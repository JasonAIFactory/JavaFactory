// =============================================================================
// DEBUG EXERCISE - "The cache works. But every cold key still hammers the
//                   DB with hundreds of identical queries for a moment.
//                   We DO have single-flight. Don't we?"
//
// This file has a REAL, planted concurrency bug: single-flight that does
// not actually collapse the stampede. The kind that takes the DB down at
// the exact worst moment (cold cache after a deploy). Practitioner loop:
// reproduce -> deterministic -> localize -> root cause -> fix -> test.
//
// Run:  java B1_StampedeRace.java
// You will see (numbers vary every run = the point):
//   - one cold key caused MANY db loads, not 1
//
// Do NOT read DEBUG.md until you have a hypothesis.
// =============================================================================

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class B1_StampedeRace {

    static final AtomicInteger dbCalls = new AtomicInteger();
    static String db(String key) {
        dbCalls.incrementAndGet();
        sleep(40);                                  // a slow query
        return "val-" + key;
    }

    static final class Cache {
        final Map<String, String> values = new ConcurrentHashMap<>();
        // "single-flight": only one loader per key should run.
        final Map<String, CompletableFuture<String>> inFlight = new ConcurrentHashMap<>();

        String get(String key) {
            String v = values.get(key);
            if (v != null) return v;                       // fast hit

            // ---- single-flight (THE BUG IS IN THESE LINES) --------------
            CompletableFuture<String> f;
            if (!inFlight.containsKey(key)) {               // (A) check
                Thread.yield();                             // amplifies the real window
                f = CompletableFuture.supplyAsync(() -> {
                    String loaded = db(key);
                    values.put(key, loaded);
                    return loaded;
                });
                inFlight.put(key, f);                       // (C) register
            } else {
                f = inFlight.get(key);
            }
            // -------------------------------------------------------------
            try { return f.join(); } finally { inFlight.remove(key, f); }
        }
    }

    public static void main(String[] args) throws Exception {
        Cache cache = new Cache();
        int n = 300;
        ExecutorService pool = Executors.newFixedThreadPool(64);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> fs = new ArrayList<>();
        for (int i = 0; i < n; i++)
            fs.add(pool.submit(() -> { await(start); cache.get("hot"); }));
        start.countDown();                                  // release the herd at once
        for (Future<?> x : fs) x.get();
        pool.shutdown();

        System.out.println("concurrent readers of 1 cold key = " + n);
        System.out.println("db loads for that ONE key         = " + dbCalls.get() + "   <-- should be 1");
        System.out.println();
        System.out.println("If this is >> 1 and changes every run, single-flight is NOT");
        System.out.println("collapsing the herd. See DEBUG.md for the method and one-line fix.");
    }

    static void await(CountDownLatch l) { try { l.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
}
