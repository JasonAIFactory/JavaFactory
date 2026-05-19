// =============================================================================
// DEBUG EXERCISE - "Sometimes prod throws, and sometimes a dead node gets
//                   traffic. Only under load. Can't reproduce locally."
//
// This file has a REAL, planted concurrency bug (the kind that pages you at
// 2am, not a compile error). Your job is the practitioner loop:
//   reproduce -> make it deterministic -> localize -> root cause -> fix ->
//   add a regression test that would have caught it.
//
// Run:  java B1_RoutingRace.java
// You will see (numbers vary run to run - that is the whole point):
//   - exceptions thrown from inside route()
//   - requests "served" by a backend that was NOT healthy at the time
//
// Do NOT read DEBUG.md until you have a hypothesis. Then check yourself.
// The fix is ONE idea (snapshot), a few lines. Don't rewrite everything.
// =============================================================================

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class B1_RoutingRace {

    static class Backend {
        final int id;
        volatile boolean healthy = true;
        Backend(int id) { this.id = id; }
    }

    static class LoadBalancer implements AutoCloseable {
        final List<Backend> all;
        // The rotation pool. A background health thread rebuilds this list
        // as backends flap up/down. Client threads read it to pick a target.
        final List<Backend> healthy = new ArrayList<>();
        final AtomicInteger rr = new AtomicInteger();
        final ScheduledExecutorService probe = Executors.newSingleThreadScheduledExecutor();

        LoadBalancer(List<Backend> all) {
            this.all = all;
            healthy.addAll(all);
            // Every 3ms: recompute the healthy pool from scratch.
            probe.scheduleAtFixedRate(() -> {
                healthy.clear();
                for (Backend b : all) if (b.healthy) healthy.add(b);
            }, 0, 3, TimeUnit.MILLISECONDS);
        }

        // Returns the backend chosen for this request.
        Backend route() {
            int idx = (rr.getAndIncrement() & Integer.MAX_VALUE) % healthy.size();
            return healthy.get(idx);
        }

        public void close() { probe.shutdownNow(); }
    }

    public static void main(String[] args) throws Exception {
        List<Backend> backends = List.of(new Backend(0), new Backend(1), new Backend(2), new Backend(3));
        try (LoadBalancer lb = new LoadBalancer(backends)) {

            // A backend flaps health constantly (deploys, GC pauses, blips).
            ScheduledExecutorService flapper = Executors.newSingleThreadScheduledExecutor();
            flapper.scheduleAtFixedRate(
                    () -> backends.get(2).healthy = !backends.get(2).healthy,
                    0, 2, TimeUnit.MILLISECONDS);

            AtomicInteger exceptions = new AtomicInteger();
            AtomicInteger routedToUnhealthy = new AtomicInteger();
            AtomicInteger ok = new AtomicInteger();

            int n = 200_000;
            ExecutorService clients = Executors.newFixedThreadPool(32);
            CountDownLatch done = new CountDownLatch(n);
            for (int i = 0; i < n; i++) clients.submit(() -> {
                try {
                    Backend b = lb.route();
                    if (b == null || !b.healthy) routedToUnhealthy.incrementAndGet();
                    else ok.incrementAndGet();
                } catch (Exception e) {
                    exceptions.incrementAndGet();         // <-- prod sees 500s here
                } finally { done.countDown(); }
            });
            done.await();
            clients.shutdown();
            flapper.shutdownNow();

            System.out.println("requests           = " + n);
            System.out.println("ok                 = " + ok);
            System.out.println("EXCEPTIONS in route= " + exceptions + "   <-- should be 0");
            System.out.println("routed to UNHEALTHY= " + routedToUnhealthy + "   <-- should be ~0");
            System.out.println();
            System.out.println("Both non-zero numbers are the bug. They change every run");
            System.out.println("(nondeterministic) -> classic concurrency defect. See DEBUG.md");
            System.out.println("for the method to localize and the one-idea fix.");
        }
    }
}
