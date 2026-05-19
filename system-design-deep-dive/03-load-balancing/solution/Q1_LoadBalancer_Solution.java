// =============================================================================
// SOLUTION - Load Balancer (all-in-one)
//
// Combines R1+R2+R3 ideas into ONE balancer:
//   1) Pluggable strategy: RoundRobin and LeastConnections.
//   2) Health: only ever route to a HEALTHY backend (active probe + passive
//      ejection after N consecutive failures, and recovery).
//   3) Sticky: a sessionId always maps to the same backend via a
//      consistent-hash ring (few keys move when membership changes).
//   4) Connection draining: removing a backend = DRAINING (no new) ->
//      wait until in-flight == 0 -> REMOVED. Zero dropped.
//   5) Metrics: success count, and per-backend handled count.
//
// Run: java -ea Q1_LoadBalancer_Solution.java
// =============================================================================

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Q1_LoadBalancer_Solution {

    // ---- Backend ---------------------------------------------------------
    static class Backend {
        final int id;
        enum State { LIVE, DRAINING, REMOVED }
        volatile State state = State.LIVE;
        volatile boolean alive = true;                 // real box liveness
        volatile boolean healthy = true;               // LB's opinion
        final AtomicInteger inflight = new AtomicInteger();
        final AtomicInteger handled  = new AtomicInteger();
        final AtomicInteger consecutiveFails = new AtomicInteger();
        Backend(int id) { this.id = id; }
        boolean acceptsNew() { return state == State.LIVE && healthy; }

        // false = request was dropped because the backend was REMOVED mid-flight
        boolean handle(Runnable inFlightHook) {
            inflight.incrementAndGet();
            try {
                if (inFlightHook != null) inFlightHook.run();   // test sync point
                for (int i = 0; i < 6; i++) {
                    if (state == State.REMOVED) return false;
                    if (!alive) { consecutiveFails.incrementAndGet(); return false; }
                    sleep(8);
                }
                consecutiveFails.set(0);
                handled.incrementAndGet();
                return true;
            } finally {
                inflight.decrementAndGet();
            }
        }
    }

    // ---- Strategy (pluggable) -------------------------------------------
    interface Strategy { Backend pick(List<Backend> healthy, AtomicInteger rr); }

    static final Strategy ROUND_ROBIN = (healthy, rr) ->
            // Trap A is avoided: we index into the HEALTHY snapshot, not the
            // full list. Indexing the full list could return an ejected node.
            healthy.get((rr.getAndIncrement() & Integer.MAX_VALUE) % healthy.size());

    static final Strategy LEAST_CONN = (healthy, rr) ->
            healthy.stream().min(Comparator.comparingInt(b -> b.inflight.get())).orElseThrow();

    // ---- Consistent hash ring (sticky) ----------------------------------
    static class HashRing {
        private final TreeMap<Integer, Backend> ring = new TreeMap<>();
        private final int vnodes;
        HashRing(int vnodes) { this.vnodes = vnodes; }
        void add(Backend b)    { for (int v=0; v<vnodes; v++) ring.put(h("b"+b.id+"#"+v), b); }
        void remove(Backend b) { for (int v=0; v<vnodes; v++) ring.remove(h("b"+b.id+"#"+v)); }
        Backend route(String key) {
            if (ring.isEmpty()) return null;
            Map.Entry<Integer, Backend> e = ring.ceilingEntry(h(key));
            return (e == null ? ring.firstEntry() : e).getValue();
        }
        // MurmurHash3 fmix32 so short similar keys spread across the ring.
        static int h(String s) {
            int x = s.hashCode();
            x ^= x >>> 16; x *= 0x85ebca6b;
            x ^= x >>> 13; x *= 0xc2b2ae35;
            x ^= x >>> 16;
            return x & Integer.MAX_VALUE;
        }
    }

    // ---- Load balancer ---------------------------------------------------
    static class LoadBalancer implements AutoCloseable {
        final List<Backend> backends;
        final Strategy strategy;
        final HashRing ring = new HashRing(150);
        final AtomicInteger rr = new AtomicInteger();
        final AtomicInteger success = new AtomicInteger();
        final ScheduledExecutorService health = Executors.newSingleThreadScheduledExecutor();
        static final int PASSIVE_EJECT_AFTER = 3;

        LoadBalancer(List<Backend> backends, Strategy strategy) {
            this.backends = backends;
            this.strategy = strategy;
            backends.forEach(ring::add);
            health.scheduleAtFixedRate(this::probe, 0, 50, TimeUnit.MILLISECONDS);
        }

        void probe() {
            for (Backend b : backends) {
                if (b.state == Backend.State.REMOVED) continue;
                if (b.alive) {
                    if (!b.healthy) b.consecutiveFails.set(0);   // slow start on recovery
                    b.healthy = true;
                } else {
                    b.healthy = false;
                }
            }
        }

        List<Backend> healthySnapshot() {
            List<Backend> out = new ArrayList<>();
            for (Backend b : backends) {
                if (b.consecutiveFails.get() >= PASSIVE_EJECT_AFTER) b.healthy = false;
                if (b.acceptsNew()) out.add(b);
            }
            return out;
        }

        // sessionId == null -> stateless (use strategy). else -> sticky.
        boolean route(String sessionId, Runnable inFlightHook) {
            Backend b;
            if (sessionId != null) {
                b = ring.route(sessionId);
                if (b == null || !b.acceptsNew()) return false;
            } else {
                List<Backend> healthy = healthySnapshot();
                if (healthy.isEmpty()) return false;
                b = strategy.pick(healthy, rr);
            }
            boolean ok = b.handle(inFlightHook);
            if (ok) success.incrementAndGet();
            return ok;
        }
        boolean route(String sessionId) { return route(sessionId, null); }

        // Graceful removal: stop new traffic, wait for in-flight, then remove.
        void drainAndRemove(Backend b) throws InterruptedException {
            b.state = Backend.State.DRAINING;                 // no NEW requests
            while (b.inflight.get() > 0) Thread.sleep(2);      // Trap B avoided
            b.state = Backend.State.REMOVED;
            ring.remove(b);                                   // off the sticky ring too
        }

        public void close() { health.shutdownNow(); }
    }

    // ---- Verification ----------------------------------------------------
    public static void main(String[] args) throws Exception {
        // (1) Sticky determinism: same session -> same backend, always.
        {
            List<Backend> bs = List.of(new Backend(0), new Backend(1), new Backend(2));
            try (LoadBalancer lb = new LoadBalancer(bs, ROUND_ROBIN)) {
                for (int u = 0; u < 500; u++) {
                    String s = "sess-" + u;
                    int first = lb.ring.route(s).id;
                    for (int r = 0; r < 10; r++)
                        assert lb.ring.route(s).id == first : "sticky not stable for " + s;
                }
            }
        }

        // (2) Consistent hashing: removing 1 backend moves ONLY its own keys.
        {
            List<Backend> bs = new ArrayList<>(List.of(new Backend(0), new Backend(1), new Backend(2)));
            HashRing ring = new HashRing(150);
            bs.forEach(ring::add);
            Map<String,Integer> before = new HashMap<>();
            for (int k = 0; k < 3000; k++) before.put("k"+k, ring.route("k"+k).id);
            ring.remove(bs.get(2));
            int moved = 0, movedNotOn2 = 0;
            for (int k = 0; k < 3000; k++) {
                int now = ring.route("k"+k).id;
                if (now != before.get("k"+k)) {
                    moved++;
                    if (before.get("k"+k) != 2) movedNotOn2++;
                }
            }
            System.out.println("consistent-hash: moved=" + moved + "/3000, "
                    + "keys NOT owned by removed node that moved=" + movedNotOn2);
            assert movedNotOn2 == 0 : "removing a node disturbed unrelated keys!";
            assert moved > 0 && moved < 1500 : "moved fraction looks wrong: " + moved;
        }

        // (3) Health: route() must NEVER return an unhealthy backend.
        {
            List<Backend> bs = List.of(new Backend(0), new Backend(1), new Backend(2));
            try (LoadBalancer lb = new LoadBalancer(bs, ROUND_ROBIN)) {
                bs.get(1).alive = false;
                Thread.sleep(120);                            // let the probe eject it
                for (int i = 0; i < 1000; i++) {
                    List<Backend> h = lb.healthySnapshot();
                    Backend picked = ROUND_ROBIN.pick(h, lb.rr);
                    assert picked.id != 1 : "routed to an unhealthy backend!";
                }
            }
        }

        // (4) Draining = zero loss, instant removal = real loss.
        {
            List<Backend> bs = List.of(new Backend(0), new Backend(1));
            try (LoadBalancer lb = new LoadBalancer(bs, LEAST_CONN)) {
                int dropDrained = removalRun(lb, bs.get(1), true);
                bs.get(1).state = Backend.State.LIVE;          // reset
                int dropInstant = removalRun(lb, bs.get(1), false);
                System.out.println("draining dropped=" + dropDrained
                        + "  instant dropped=" + dropInstant);
                assert dropDrained == 0 : "draining must not drop in-flight: " + dropDrained;
                assert dropInstant >= 1 : "instant removal should drop in-flight";
            }
        }

        System.out.println("ALL ASSERTIONS PASSED - strategy + health + sticky + draining OK");
    }

    // Fire requests at one backend; remove it mid-flight either by draining
    // or instantly. Returns dropped count. A latch makes "in-flight at removal"
    // deterministic so the test is not flaky.
    static int removalRun(LoadBalancer lb, Backend target, boolean drain) throws InterruptedException {
        AtomicInteger dropped = new AtomicInteger();
        CountDownLatch inFlight = new CountDownLatch(4);
        CountDownLatch finished = new CountDownLatch(16);
        ExecutorService c = Executors.newFixedThreadPool(16);
        for (int i = 0; i < 16; i++) c.submit(() -> {
            try {
                if (!target.acceptsNew()) return;
                if (!target.handle(inFlight::countDown)) dropped.incrementAndGet();
            } finally { finished.countDown(); }
        });
        inFlight.await();                                      // >=4 requests are in-flight now
        if (drain) {
            target.state = Backend.State.DRAINING;
            while (target.inflight.get() > 0) Thread.sleep(2); // wait in-flight to finish
            target.state = Backend.State.REMOVED;
        } else {
            target.state = Backend.State.REMOVED;              // yank now -> in-flight die
        }
        finished.await();
        c.shutdown();
        return dropped.get();
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
