// =============================================================================
// TESTS - Load Balancer (the practitioner layer: how you make a change
//         nobody can break)
//
// This is NOT a second copy of the solution to admire. It is the thing a
// real practitioner adds: a suite that PINS the behavior so the next
// person (or you, in 3 months) cannot regress it silently.
//
// Run:  java T1_LoadBalancerTests.java     (no -ea needed; checks throw)
// Exit code 0 = all green, 1 = something regressed.
//
// What this teaches (read the section headers):
//   - UNIT tests: pure logic, deterministic, single-threaded, fast.
//   - CONCURRENCY tests: assert an INVARIANT that must hold under load,
//     not a timing ("never routes to an ejected node, no matter the race").
//   - BOUNDARY: we test OUR logic (ring, strategy, drain). We do NOT test
//     the JDK or Thread.sleep. Test what you own, at its edge.
//   - DETERMINISTIC concurrency: use a latch to force "in-flight at the
//     moment of removal" so the test is not flaky. Flaky test = no test.
// =============================================================================

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class T1_LoadBalancerTests {

    // ---- system under test (same shapes as solution/) -------------------
    static class Backend {
        final int id;
        enum State { LIVE, DRAINING, REMOVED }
        volatile State state = State.LIVE;
        volatile boolean healthy = true;
        final AtomicInteger inflight = new AtomicInteger();
        Backend(int id) { this.id = id; }
        boolean acceptsNew() { return state == State.LIVE && healthy; }
        boolean handle(Runnable hook) {
            inflight.incrementAndGet();
            try {
                if (hook != null) hook.run();
                for (int i = 0; i < 5; i++) { if (state == State.REMOVED) return false; sleep(6); }
                return true;
            } finally { inflight.decrementAndGet(); }
        }
    }
    interface Strategy { Backend pick(List<Backend> healthy, AtomicInteger rr); }
    static final Strategy ROUND_ROBIN = (h, rr) ->
            h.get((rr.getAndIncrement() & Integer.MAX_VALUE) % h.size());
    static final Strategy LEAST_CONN = (h, rr) ->
            h.stream().min(Comparator.comparingInt(b -> b.inflight.get())).orElseThrow();

    static class HashRing {
        final TreeMap<Integer, Backend> ring = new TreeMap<>();
        final int vnodes;
        HashRing(int v) { vnodes = v; }
        void add(Backend b)    { for (int v=0;v<vnodes;v++) ring.put(h("b"+b.id+"#"+v), b); }
        void remove(Backend b) { for (int v=0;v<vnodes;v++) ring.remove(h("b"+b.id+"#"+v)); }
        Backend route(String k) {
            if (ring.isEmpty()) return null;
            var e = ring.ceilingEntry(h(k));
            return (e == null ? ring.firstEntry() : e).getValue();
        }
        static int h(String s) {
            int x = s.hashCode();
            x ^= x>>>16; x *= 0x85ebca6b; x ^= x>>>13; x *= 0xc2b2ae35; x ^= x>>>16;
            return x & Integer.MAX_VALUE;
        }
    }

    static List<Backend> healthySnapshot(List<Backend> all) {
        List<Backend> out = new ArrayList<>();
        for (Backend b : all) if (b.acceptsNew()) out.add(b);
        return out;
    }

    // ---- tiny test framework (no dependency; checks throw) ---------------
    static int passed = 0, failed = 0;
    static void test(String name, ThrowingRunnable body) {
        try { body.run(); System.out.println("PASS  " + name); passed++; }
        catch (Throwable t) { System.out.println("FAIL  " + name + "  -> " + t); failed++; }
    }
    interface ThrowingRunnable { void run() throws Exception; }
    static void check(boolean c, String msg) { if (!c) throw new AssertionError(msg); }
    static void eq(long a, long b, String m) { if (a != b) throw new AssertionError(m + " (got " + a + ", want " + b + ")"); }

    // ---- UNIT TESTS -----------------------------------------------------
    static void stickyIsDeterministic() {
        HashRing ring = new HashRing(150);
        List.of(new Backend(0), new Backend(1), new Backend(2)).forEach(ring::add);
        for (int u = 0; u < 1000; u++) {
            String s = "sess-" + u;
            int first = ring.route(s).id;
            for (int r = 0; r < 8; r++) eq(ring.route(s).id, first, "sticky drifted for " + s);
        }
    }
    static void consistentHashMovesOnlyRemovedNodesKeys() {
        List<Backend> bs = new ArrayList<>(List.of(new Backend(0), new Backend(1), new Backend(2)));
        HashRing ring = new HashRing(150);
        bs.forEach(ring::add);
        Map<String,Integer> before = new HashMap<>();
        for (int k = 0; k < 5000; k++) before.put("k"+k, ring.route("k"+k).id);
        ring.remove(bs.get(2));
        int moved = 0, offNodeMoved = 0;
        for (int k = 0; k < 5000; k++) {
            int now = ring.route("k"+k).id;
            if (now != before.get("k"+k)) { moved++; if (before.get("k"+k) != 2) offNodeMoved++; }
        }
        eq(offNodeMoved, 0, "removing a node disturbed UNRELATED keys");
        check(moved > 0 && moved < 2500, "moved fraction unreasonable: " + moved + "/5000");
    }
    static void roundRobinNeverPicksUnhealthy() {
        List<Backend> all = List.of(new Backend(0), new Backend(1), new Backend(2));
        all.get(1).healthy = false;                       // ejected
        AtomicInteger rr = new AtomicInteger();
        for (int i = 0; i < 1000; i++) {
            Backend b = ROUND_ROBIN.pick(healthySnapshot(all), rr);
            check(b.id != 1, "round robin returned an ejected backend");
        }
    }
    static void leastConnPicksTheLeastBusy() {
        List<Backend> all = List.of(new Backend(0), new Backend(1), new Backend(2));
        all.get(0).inflight.set(7); all.get(1).inflight.set(2); all.get(2).inflight.set(5);
        eq(LEAST_CONN.pick(all, new AtomicInteger()).id, 1, "least-conn did not pick min inflight");
    }
    static void drainingBackendRefusesNewTraffic() {
        Backend b = new Backend(9);
        check(b.acceptsNew(), "LIVE backend should accept");
        b.state = Backend.State.DRAINING;
        check(!b.acceptsNew(), "DRAINING backend must NOT accept new traffic");
    }

    // ---- CONCURRENCY TESTS (assert an invariant under load) -------------
    static void underLoad_neverRoutesToEjectedNode() throws Exception {
        List<Backend> all = List.of(new Backend(0), new Backend(1), new Backend(2));
        all.get(1).healthy = false;
        AtomicInteger rr = new AtomicInteger();
        AtomicBoolean violated = new AtomicBoolean(false);
        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch done = new CountDownLatch(4000);
        for (int i = 0; i < 4000; i++) pool.submit(() -> {
            try {
                Backend b = ROUND_ROBIN.pick(healthySnapshot(all), rr);
                if (b.id == 1) violated.set(true);        // the invariant
            } finally { done.countDown(); }
        });
        done.await(); pool.shutdown();
        check(!violated.get(), "INVARIANT BROKEN: routed to ejected node under concurrency");
    }
    static void drainingDropsZero_instantDropsSome() throws Exception {
        Backend target = new Backend(5);
        eq(removalRun(target, true), 0, "draining must drop ZERO in-flight");
        target.state = Backend.State.LIVE;
        check(removalRun(target, false) >= 1, "instant removal should drop in-flight (control)");
    }
    static int removalRun(Backend target, boolean drain) throws Exception {
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
        inFlight.await();                                 // force deterministic in-flight
        if (drain) {
            target.state = Backend.State.DRAINING;
            while (target.inflight.get() > 0) Thread.sleep(2);
            target.state = Backend.State.REMOVED;
        } else target.state = Backend.State.REMOVED;
        finished.await(); c.shutdown();
        return dropped.get();
    }

    public static void main(String[] args) {
        System.out.println("== UNIT ==");
        test("sticky is deterministic",                 T1_LoadBalancerTests::stickyIsDeterministic);
        test("consistent hash moves only removed keys", T1_LoadBalancerTests::consistentHashMovesOnlyRemovedNodesKeys);
        test("round robin never picks unhealthy",       T1_LoadBalancerTests::roundRobinNeverPicksUnhealthy);
        test("least-conn picks the least busy",         T1_LoadBalancerTests::leastConnPicksTheLeastBusy);
        test("draining backend refuses new traffic",    T1_LoadBalancerTests::drainingBackendRefusesNewTraffic);
        System.out.println("== CONCURRENCY ==");
        test("never routes to ejected under load",      T1_LoadBalancerTests::underLoad_neverRoutesToEjectedNode);
        test("draining zero loss, instant loses",       T1_LoadBalancerTests::drainingDropsZero_instantDropsSome);

        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
