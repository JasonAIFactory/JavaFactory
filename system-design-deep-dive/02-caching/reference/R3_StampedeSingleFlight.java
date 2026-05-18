// =============================================================================
// R3 - Cache stampede defense via single-flight (request coalescing)
//
// THE PROBLEM (a.k.a. dogpile / thundering herd):
//   A hot key expires. In the same millisecond, 1000 requests all MISS,
//   and all 1000 hit the database at once -> the db falls over. The cache
//   was supposed to PROTECT the db; on a miss it instead amplifies load.
//
// THE FIX (single-flight): on a miss, only the FIRST caller computes the
//   value; everyone else waits for that one in-flight computation and
//   shares its result. The db is called exactly once per key.
//
// Run:  javac R3_StampedeSingleFlight.java && java -ea R3_StampedeSingleFlight
// =============================================================================

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class R3_StampedeSingleFlight {

    final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
    // The key idea: a map of IN-FLIGHT loads. One Future per key.
    final ConcurrentHashMap<String, CompletableFuture<String>> inFlight = new ConcurrentHashMap<>();
    final AtomicInteger dbCalls = new AtomicInteger();

    String loadFromDb(String key) {
        dbCalls.incrementAndGet();
        sleep(50);                       // slow query; the window where a stampede happens
        return "value-of-" + key;
    }

    String get(String key) {
        String hit = cache.get(key);
        if (hit != null) return hit;     // fast path

        // computeIfAbsent is atomic per key: only ONE thread creates the
        // future; all other concurrent callers get the SAME future back.
        CompletableFuture<String> f = inFlight.computeIfAbsent(key, k ->
                CompletableFuture.supplyAsync(() -> {
                    String v = loadFromDb(k);   // executed by exactly one thread
                    cache.put(k, v);
                    return v;
                }));
        try {
            return f.join();             // everyone waits on the single in-flight load
        } finally {
            inFlight.remove(key, f);     // clear so the next miss can reload later
        }
    }

    public static void main(String[] args) throws Exception {
        // Run several rounds so a flaky race would show up across repeats.
        for (int round = 1; round <= 5; round++) {
            R3_StampedeSingleFlight c = new R3_StampedeSingleFlight();
            int threads = 200;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);

            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        start.await();                 // line everyone up...
                        String v = c.get("hot");       // ...then all miss "hot" at once
                        assert v.equals("value-of-hot");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();                         // release the stampede
            done.await();
            pool.shutdown();

            System.out.println("round " + round + ": " + threads
                    + " concurrent misses -> db calls = " + c.dbCalls.get());
            assert c.dbCalls.get() == 1
                    : "single-flight must collapse the stampede to ONE db call, got " + c.dbCalls.get();
        }
        System.out.println("R3 OK - stampede collapsed to 1 db call every round. "
                + "Related defenses: TTL jitter (avalanche), null-caching (penetration).");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
