// =============================================================================
// STAGE 2 - A real load balancer with ACTIVE health checks.
//
// New component:
//   - LoadBalancer : sits in front of the servers. A background thread
//                    probes each server's /health. Unhealthy servers are
//                    EJECTED from rotation; when they recover they are
//                    added back (this is what Nginx/Envoy/ALB do).
//
// Run:  java S2_HealthCheckedLB.java
// Watch: a server can die and come back, and clients see ~ZERO errors,
//        because the LB only routes to healthy backends.
// =============================================================================

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class S2_HealthCheckedLB {

    static class AppServer {
        final int id;
        final Semaphore slots = new Semaphore(10);
        volatile boolean alive = true;            // real liveness (the box is up or not)
        AppServer(int id) { this.id = id; }
        boolean health() { return alive; }        // what /health returns
        boolean handle() {
            if (!alive) return false;
            if (!slots.tryAcquire()) return false;
            try { sleep(50); return true; } finally { slots.release(); }
        }
    }

    static class LoadBalancer implements AutoCloseable {
        final List<AppServer> all;
        // healthy set is what we actually route to. Updated by the health thread.
        final Set<Integer> healthy = ConcurrentHashMap.newKeySet();
        final AtomicInteger rr = new AtomicInteger();
        final ScheduledExecutorService probe = Executors.newSingleThreadScheduledExecutor();

        LoadBalancer(List<AppServer> all) {
            this.all = all;
            all.forEach(s -> healthy.add(s.id));
            // ACTIVE health check: probe every 100ms, eject/restore.
            probe.scheduleAtFixedRate(() -> {
                for (AppServer s : all) {
                    if (s.health()) healthy.add(s.id);
                    else healthy.remove(s.id);
                }
            }, 0, 100, TimeUnit.MILLISECONDS);
        }

        // Route only to a healthy backend. Returns false if none available.
        boolean route() {
            // snapshot the healthy ids so the list does not change mid-pick
            Integer[] ids = healthy.toArray(new Integer[0]);
            if (ids.length == 0) return false;
            int idx = (rr.getAndIncrement() & Integer.MAX_VALUE) % ids.length;
            AppServer s = all.get(ids[idx]);
            return s.handle();
        }

        public void close() { probe.shutdownNow(); }
    }

    public static void main(String[] args) throws Exception {
        List<AppServer> servers = new ArrayList<>(List.of(
                new AppServer(0), new AppServer(1), new AppServer(2)));
        try (LoadBalancer lb = new LoadBalancer(servers)) {
            Thread.sleep(150);                       // let first health probe run

            ExecutorService clients = Executors.newFixedThreadPool(64);
            AtomicInteger ok = new AtomicInteger(), failed = new AtomicInteger();

            // Steady traffic for ~1.5s while we kill and revive a server.
            ScheduledExecutorService load = Executors.newScheduledThreadPool(8);
            load.scheduleAtFixedRate(() -> clients.submit(() -> {
                if (lb.route()) ok.incrementAndGet(); else failed.incrementAndGet();
            }), 0, 5, TimeUnit.MILLISECONDS);

            Thread.sleep(400);
            System.out.println("Killing server 1 ...");
            servers.get(1).alive = false;            // LB will eject it within ~100ms
            Thread.sleep(500);
            System.out.println("Reviving server 1 ...");
            servers.get(1).alive = true;             // LB will add it back
            Thread.sleep(500);

            load.shutdownNow();
            clients.shutdown();
            clients.awaitTermination(2, TimeUnit.SECONDS);
            System.out.println("During kill+revive: served=" + ok + " failed=" + failed);
            System.out.println("(A few failures are only the ~100ms detection gap; ~all succeed.)");
        }

        System.out.println("\nWHAT IMPROVED: the LB ejects dead backends, so clients are shielded.");
        System.out.println("WHAT IS STILL NAIVE: round robin assumes every server is equally fast.");
        System.out.println("NEXT (S3): when servers/requests are UNEVEN, round robin piles up the slow one.");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
