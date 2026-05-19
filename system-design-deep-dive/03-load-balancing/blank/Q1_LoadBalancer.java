// =============================================================================
// BLANK exercise - Load Balancer (all-in-one)
//
// Rule: do NOT open solution/. Sit with it for 30+ minutes.
//       The point where you get stuck is the real learning.
// Goal: combine R1~R3 ideas into ONE balancer by yourself.
//
// Requirements (fill the TODOs):
//   1) Pluggable strategy: ROUND_ROBIN and LEAST_CONN.
//   2) Health: only ever route to a HEALTHY backend. Active probe flips
//      healthy; N consecutive failures also eject (passive); recover.
//   3) Sticky: a sessionId always maps to the same backend via a
//      consistent-hash ring (use the given fmix32 hash).
//   4) Connection draining: removing a backend = DRAINING (no new) ->
//      wait until in-flight == 0 -> REMOVED, then take it off the ring.
//   5) Metrics: success count.
//
// Check: all asserts in main must pass.
// Run:   java -ea Q1_LoadBalancer.java   (-ea enables assert - you MUST add it)
//
// Two traps left ON PURPOSE (they appear when you run with -ea, debug them):
//   Trap A) ROUND_ROBIN: if you do `index % backends.size()` over the FULL
//           list, you can return an EJECTED backend. The "no unhealthy"
//           assert (test 3) will fail. => index into the HEALTHY snapshot.
//   Trap B) drainAndRemove: if you mark REMOVED as soon as "no new traffic"
//           (state == DRAINING) without waiting for in-flight to reach 0,
//           in-flight requests are dropped. Test 4 (draining dropped==0)
//           will fail. => busy-wait until inflight == 0, THEN REMOVED.
// =============================================================================

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Q1_LoadBalancer {

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

        boolean handle(Runnable inFlightHook) {
            inflight.incrementAndGet();
            try {
                if (inFlightHook != null) inFlightHook.run();
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

    interface Strategy { Backend pick(List<Backend> healthy, AtomicInteger rr); }

    // TODO 1: ROUND_ROBIN over the HEALTHY list (see Trap A). Non-negative index.
    static final Strategy ROUND_ROBIN = (healthy, rr) -> { /* TODO */ return null; };

    // TODO 1: LEAST_CONN = healthy backend with the smallest inflight.
    static final Strategy LEAST_CONN = (healthy, rr) -> { /* TODO */ return null; };

    static class HashRing {
        private final TreeMap<Integer, Backend> ring = new TreeMap<>();
        private final int vnodes;
        HashRing(int vnodes) { this.vnodes = vnodes; }

        // TODO 3: add/remove `vnodes` virtual nodes per backend (key "b<id>#<v>").
        void add(Backend b)    { /* TODO */ }
        void remove(Backend b) { /* TODO */ }

        // TODO 3: first backend clockwise from h(key); wrap to first if past the end.
        Backend route(String key) { /* TODO */ return null; }

        static int h(String s) {
            int x = s.hashCode();
            x ^= x >>> 16; x *= 0x85ebca6b;
            x ^= x >>> 13; x *= 0xc2b2ae35;
            x ^= x >>> 16;
            return x & Integer.MAX_VALUE;
        }
    }

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

        // TODO 2: if b.alive -> healthy=true (clear fail streak on recovery);
        //         else healthy=false. Skip REMOVED backends.
        void probe() { /* TODO */ }

        // TODO 2: passive ejection (consecutiveFails >= PASSIVE_EJECT_AFTER ->
        //         healthy=false), then return only backends that acceptsNew().
        List<Backend> healthySnapshot() { /* TODO */ return List.of(); }

        // TODO: sessionId != null -> sticky via ring (reject if not acceptsNew);
        //       else -> strategy.pick over healthySnapshot(). Count success.
        boolean route(String sessionId, Runnable inFlightHook) { /* TODO */ return false; }
        boolean route(String sessionId) { return route(sessionId, null); }

        // TODO 4: DRAINING -> wait inflight==0 (Trap B) -> REMOVED -> ring.remove.
        void drainAndRemove(Backend b) throws InterruptedException { /* TODO */ }

        public void close() { health.shutdownNow(); }
    }

    // ---- Verification (do not touch) ------------------------------------
    public static void main(String[] args) throws Exception {
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
                if (now != before.get("k"+k)) { moved++; if (before.get("k"+k) != 2) movedNotOn2++; }
            }
            System.out.println("consistent-hash: moved=" + moved + "/3000, off-node moved=" + movedNotOn2);
            assert movedNotOn2 == 0 : "removing a node disturbed unrelated keys!";
            assert moved > 0 && moved < 1500 : "moved fraction looks wrong: " + moved;
        }
        {
            List<Backend> bs = List.of(new Backend(0), new Backend(1), new Backend(2));
            try (LoadBalancer lb = new LoadBalancer(bs, ROUND_ROBIN)) {
                bs.get(1).alive = false;
                Thread.sleep(120);
                for (int i = 0; i < 1000; i++) {
                    List<Backend> h = lb.healthySnapshot();
                    Backend picked = ROUND_ROBIN.pick(h, lb.rr);
                    assert picked.id != 1 : "routed to an unhealthy backend!";
                }
            }
        }
        {
            List<Backend> bs = List.of(new Backend(0), new Backend(1));
            try (LoadBalancer lb = new LoadBalancer(bs, LEAST_CONN)) {
                int dropDrained = removalRun(lb, bs.get(1), true);
                bs.get(1).state = Backend.State.LIVE;
                int dropInstant = removalRun(lb, bs.get(1), false);
                System.out.println("draining dropped=" + dropDrained + "  instant dropped=" + dropInstant);
                assert dropDrained == 0 : "draining must not drop in-flight: " + dropDrained;
                assert dropInstant >= 1 : "instant removal should drop in-flight";
            }
        }
        System.out.println("ALL ASSERTIONS PASSED - strategy + health + sticky + draining OK");
    }

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
        inFlight.await();
        if (drain) {
            target.state = Backend.State.DRAINING;
            while (target.inflight.get() > 0) Thread.sleep(2);
            target.state = Backend.State.REMOVED;
        } else {
            target.state = Backend.State.REMOVED;
        }
        finished.await();
        c.shutdown();
        return dropped.get();
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
