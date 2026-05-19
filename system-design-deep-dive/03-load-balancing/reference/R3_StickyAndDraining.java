// =============================================================================
// R3 - Sticky sessions (consistent hashing) + connection draining
//
// Adds the last two real-world pieces on top of R2's ideas:
//   - STICKY routing: the same sessionId always maps to the same backend,
//     using a consistent-hash RING. Adding/removing a backend only moves
//     the keys near that backend, not all of them (vs plain hash % N).
//   - CONNECTION DRAINING: to remove a backend (deploy/scale-down) we set
//     it DRAINING (no NEW sessions), let in-flight finish, then REMOVE.
//     Zero dropped requests.
//
// Run:  java R3_StickyAndDraining.java
// See:  sessions stay on one backend; rehash moves only a small fraction;
//       draining drops zero in-flight while instant removal drops many.
// =============================================================================

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class R3_StickyAndDraining {

    static class Backend {
        final int id;
        enum State { LIVE, DRAINING, REMOVED }
        volatile State state = State.LIVE;
        final AtomicInteger inflight = new AtomicInteger();
        Backend(int id) { this.id = id; }
        boolean acceptsNew() { return state == State.LIVE; }

        boolean handle() {                                 // false = dropped mid-flight
            inflight.incrementAndGet();
            try {
                for (int i = 0; i < 6; i++) {              // ~60ms of work
                    if (state == State.REMOVED) return false;
                    sleep(10);
                }
                return true;
            } finally { inflight.decrementAndGet(); }
        }
    }

    // Consistent hash ring with virtual nodes (smoother key spread).
    static class HashRing {
        private final TreeMap<Integer, Backend> ring = new TreeMap<>();
        private final int vnodes;
        HashRing(int vnodes) { this.vnodes = vnodes; }

        void add(Backend b) {
            for (int v = 0; v < vnodes; v++) ring.put(hash("b" + b.id + "#" + v), b);
        }
        void remove(Backend b) {
            for (int v = 0; v < vnodes; v++) ring.remove(hash("b" + b.id + "#" + v));
        }
        // first backend clockwise from the key's hash
        Backend route(String key) {
            if (ring.isEmpty()) return null;
            Map.Entry<Integer, Backend> e = ring.ceilingEntry(hash(key));
            return (e == null ? ring.firstEntry() : e).getValue();
        }
        // A plain polynomial hash clusters short similar strings onto one
        // node. Consistent hashing needs a well-spread hash, so we run the
        // key through a bit-mixing finalizer (MurmurHash3 fmix32).
        static int hash(String s) {
            int h = s.hashCode();
            h ^= h >>> 16; h *= 0x85ebca6b;
            h ^= h >>> 13; h *= 0xc2b2ae35;
            h ^= h >>> 16;
            return h & Integer.MAX_VALUE;
        }
    }

    public static void main(String[] args) throws Exception {
        List<Backend> backends = new ArrayList<>(List.of(
                new Backend(0), new Backend(1), new Backend(2)));
        HashRing ring = new HashRing(150);
        backends.forEach(ring::add);

        // ---- 1) Stickiness: same session -> same backend, every time ----
        boolean sticky = true;
        for (int u = 0; u < 200; u++) {
            String s = "sess-" + u;
            int first = ring.route(s).id;
            for (int r = 0; r < 5; r++) if (ring.route(s).id != first) sticky = false;
        }
        System.out.println("Every session stuck to one backend across 5 requests? " + sticky);

        // ---- 2) Consistent hashing: removing 1 of 3 backends moves few keys ----
        Map<String, Integer> before = new HashMap<>();
        for (int u = 0; u < 3000; u++) before.put("k" + u, ring.route("k" + u).id);
        ring.remove(backends.get(2));
        int moved = 0;
        for (int u = 0; u < 3000; u++) if (ring.route("k" + u).id != before.get("k" + u)) moved++;
        System.out.printf("Removed 1 of 3 backends -> keys moved: %d / 3000 (%.1f%%)%n",
                moved, moved * 100.0 / 3000);
        System.out.println("  (plain hash % N would reshuffle ~2/3 of ALL keys.)");
        ring.add(backends.get(2));                          // restore for next test

        // ---- 3) Connection draining vs instant removal ----
        System.out.println("\n-- Removing backend 1 under live traffic --");
        System.out.println("  instant : dropped=" + drainTest(backends.get(1), false));
        backends.get(1).state = Backend.State.LIVE;
        System.out.println("  drained : dropped=" + drainTest(backends.get(1), true));

        System.out.println("\nNOW YOU CAN: combine R1 round robin skeleton + R2 health/least-conn");
        System.out.println("+ R3 sticky/draining into the blank/ exercise yourself.");
    }

    static int drainTest(Backend target, boolean drain) throws InterruptedException {
        AtomicInteger dropped = new AtomicInteger();
        ExecutorService c = Executors.newFixedThreadPool(32);
        CountDownLatch d = new CountDownLatch(60);
        for (int i = 0; i < 60; i++) c.submit(() -> {
            try {
                if (!target.acceptsNew()) return;
                if (!target.handle()) dropped.incrementAndGet();
            } finally { d.countDown(); }
        });
        Thread.sleep(25);
        if (drain) {
            target.state = Backend.State.DRAINING;
            while (target.inflight.get() > 0) Thread.sleep(5);
            target.state = Backend.State.REMOVED;
        } else {
            target.state = Backend.State.REMOVED;
        }
        d.await(); c.shutdown();
        return dropped.get();
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
