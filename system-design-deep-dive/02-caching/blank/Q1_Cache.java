// =============================================================================
// BLANK exercise - Caching (all-in-one)
//
// Rule: do NOT open solution/. Sit with it for 30+ minutes.
//       The point where you get stuck is the real learning.
// Goal: combine R1~R3 + evolution S4 into ONE thread-safe cache yourself.
//
// Requirements (fill the TODOs):
//   1) Cache-aside read: check cache -> on miss load from db -> store.
//   2) TTL: an entry expires after ttlMs (bounds staleness).
//   3) LRU + maxSize: evict the least-recently-used when over capacity.
//   4) Single-flight: concurrent misses on ONE key cause ONE db call.
//   5) Penetration: cache the "not found" (null) too.
//
// Check: all asserts at the end of main must pass.
// Run:   javac Q1_Cache.java && java -ea Q1_Cache   (-ea is REQUIRED)
//
// Two traps left ON PURPOSE (they appear when you run with -ea, debug them):
//   Trap A) LinkedHashMap is NOT thread-safe. If you read/write it from
//           many worker threads without synchronizing, the stampede test
//           corrupts it or miscounts. => guard map access with a lock.
//   Trap B) If single-flight removes the in-flight future BEFORE the
//           loader has stored the value (or you check cache then create a
//           future non-atomically with an instant db), many futures are
//           created and the db is called many times. => use an atomic
//           computeIfAbsent on the in-flight map, and a SLOW db so the
//           coalescing window is real (mirrors R3).
// =============================================================================

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Q1_Cache {

    static final AtomicInteger dbCalls = new AtomicInteger();

    static String db(String key) {
        dbCalls.incrementAndGet();
        sleep(40);                                   // slow on purpose (see Trap B)
        return key.startsWith("ghost") ? null : "val-" + key;
    }

    static final String NULL_SENTINEL = " NULL";

    // TODO 2: an Entry holding value + an expiresAt; expired() returns
    //         true once the ttl has passed.
    static final class Entry {
        // TODO
        Entry(String v, long ttlMs) { /* TODO */ }
        boolean expired() { /* TODO */ return false; }
    }

    final int maxSize;
    final long ttlMs;
    // TODO 3: an access-order LinkedHashMap with removeEldestEntry that
    //         evicts when size() > maxSize.
    final LinkedHashMap<String, Entry> map = null;
    // TODO 4: an in-flight map String -> CompletableFuture<String>.
    final ConcurrentHashMap<String, CompletableFuture<String>> inFlight = new ConcurrentHashMap<>();

    Q1_Cache(int maxSize, long ttlMs) {
        this.maxSize = maxSize;
        this.ttlMs = ttlMs;
        // TODO 3: build the LRU map here.
    }

    String get(String key) {
        // TODO 1+2: look up; if present and not expired, return it
        //           (a cached NULL_SENTINEL means "return null"). Guard
        //           the map with a lock (Trap A).
        // TODO 4+5: on miss, use inFlight.computeIfAbsent so only ONE
        //           loader runs; the loader calls db, stores the value
        //           (store NULL_SENTINEL when db returns null), returns.
        //           Everyone joins the same future. Remove it in finally.
        return null;
    }

    int size() { /* TODO: return map size under the lock */ return 0; }

    public static void main(String[] args) throws Exception {
        // 1) basic hit
        Q1_Cache c = new Q1_Cache(3, 10_000);
        for (int i = 0; i < 50; i++) c.get("a");
        assert dbCalls.get() == 1 : "repeat reads must hit the cache";

        // 2) LRU bound
        c.get("a"); c.get("b"); c.get("c"); c.get("a"); c.get("d");
        assert c.size() == 3 : "LRU must bound size, got " + c.size();
        dbCalls.set(0);
        c.get("b");
        assert dbCalls.get() == 1 : "evicted key must be refetched";

        // 3) penetration
        dbCalls.set(0);
        for (int i = 0; i < 100; i++) assert c.get("ghost") == null;
        assert dbCalls.get() == 1 : "the 'not found' must be cached";

        // 4) stampede
        for (int round = 1; round <= 4; round++) {
            Q1_Cache s = new Q1_Cache(1000, 10_000);
            dbCalls.set(0);
            ExecutorService pool = Executors.newFixedThreadPool(64);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> fs = new ArrayList<>();
            for (int i = 0; i < 300; i++) fs.add(pool.submit(() -> { await(start); s.get("hot"); }));
            start.countDown();
            for (Future<?> x : fs) x.get();
            pool.shutdown();
            assert dbCalls.get() == 1 : "single-flight must collapse the stampede, got " + dbCalls.get();
        }

        // 5) TTL self-heal
        Q1_Cache t = new Q1_Cache(10, 50);
        t.get("k");
        int before = dbCalls.get();
        t.get("k");
        assert dbCalls.get() == before;
        sleep(70);
        t.get("k");
        assert dbCalls.get() == before + 1 : "TTL must bound staleness";

        System.out.println("ALL ASSERTIONS PASSED");
    }

    static void await(CountDownLatch l) {
        try { l.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
