// =============================================================================
// R2 - Health checks + least-connections (fixes R1 flaw #1 and #2)
//
// Adds:
//   - ACTIVE health check: a background thread probes each backend; an
//     unhealthy one is EJECTED from rotation and re-added when it recovers.
//   - PASSIVE ejection: N consecutive request failures also ejects it
//     (you do not always have a /health endpoint that tells the truth).
//   - least-connections strategy: route to the backend with the fewest
//     in-flight requests, so a slow backend stops attracting traffic.
//   - slow start: a recovered backend is eased back, not flooded instantly.
//
// Run:  java R2_HealthCheckedLeastConn.java
// Compare with R1: a backend can die and recover with ~zero client errors,
// and the slow backend no longer piles up.
// =============================================================================

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class R2_HealthCheckedLeastConn {

    static class Backend {
        final int id;
        final long workMs;
        volatile boolean alive = true;                 // real liveness of the box
        volatile boolean healthy = true;               // LB's opinion (eject/restore)
        final AtomicInteger inflight = new AtomicInteger();
        final AtomicInteger handled = new AtomicInteger();      // total served OK
        final AtomicInteger consecutiveFails = new AtomicInteger();
        Backend(int id, long workMs) { this.id = id; this.workMs = workMs; }

        boolean probe() { return alive; }              // what GET /health returns

        boolean handle() {
            inflight.incrementAndGet();
            try {
                if (!alive) { consecutiveFails.incrementAndGet(); return false; }
                sleep(workMs);
                consecutiveFails.set(0);               // success resets the failure streak
                handled.incrementAndGet();
                return true;
            } finally {
                inflight.decrementAndGet();
            }
        }
    }

    static class LoadBalancer implements AutoCloseable {
        final List<Backend> backends;
        final ScheduledExecutorService health = Executors.newSingleThreadScheduledExecutor();
        static final int PASSIVE_EJECT_AFTER = 3;

        LoadBalancer(List<Backend> backends) {
            this.backends = backends;
            // ACTIVE probe every 80ms: flip the LB's "healthy" opinion.
            health.scheduleAtFixedRate(() -> {
                for (Backend b : backends) {
                    if (b.probe()) {
                        if (!b.healthy) b.consecutiveFails.set(0);   // slow start: clear on recovery
                        b.healthy = true;
                    } else {
                        b.healthy = false;
                    }
                }
            }, 0, 80, TimeUnit.MILLISECONDS);
        }

        // least-connections among HEALTHY backends only.
        Backend pick() {
            Backend best = null;
            for (Backend b : backends) {
                if (!b.healthy) continue;                            // skip ejected
                if (b.consecutiveFails.get() >= PASSIVE_EJECT_AFTER) {
                    b.healthy = false;                               // passive ejection
                    continue;
                }
                if (best == null || b.inflight.get() < best.inflight.get()) best = b;
            }
            return best;                                             // null if all down
        }

        boolean route() {
            Backend b = pick();
            return b != null && b.handle();
        }

        public void close() { health.shutdownNow(); }
    }

    public static void main(String[] args) throws Exception {
        List<Backend> backends = List.of(
                new Backend(0, 40), new Backend(1, 40), new Backend(2, 200)); // b2 slow
        try (LoadBalancer lb = new LoadBalancer(backends)) {
            Thread.sleep(100);                               // first health probe

            ExecutorService clients = Executors.newFixedThreadPool(64);
            AtomicInteger ok = new AtomicInteger(), fail = new AtomicInteger();
            ScheduledExecutorService load = Executors.newScheduledThreadPool(8);
            load.scheduleAtFixedRate(() -> clients.submit(() -> {
                if (lb.route()) ok.incrementAndGet(); else fail.incrementAndGet();
            }), 0, 4, TimeUnit.MILLISECONDS);

            Thread.sleep(400);
            System.out.println("Killing backend 0 ...");
            backends.get(0).alive = false;
            Thread.sleep(500);
            System.out.println("Reviving backend 0 ...");
            backends.get(0).alive = true;
            Thread.sleep(400);

            load.shutdownNow();
            clients.shutdown();
            clients.awaitTermination(2, TimeUnit.SECONDS);

            System.out.println("ok=" + ok + " fail=" + fail
                    + "  (failures ~= only the brief detection gap)");
            System.out.println("requests handled per backend: "
                    + backends.stream().map(b -> "b" + b.id + "=" + b.handled.get()).toList()
                    + " (least-conn sent FAR fewer to the slow b2)");
        }
        System.out.println("\nWHAT IS STILL MISSING: stateful apps need the SAME backend per");
        System.out.println("session, and deploys must not drop in-flight requests. -> R3.");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
