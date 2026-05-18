// =============================================================================
// R1 - Naive cache-aside (look-aside) cache
//
// WHY THIS FILE: the simplest possible cache. It already shows the core
// idea (skip slow work by remembering the answer) AND the two problems
// every later file fixes: (1) it grows forever, (2) it can serve stale data.
//
// Run:  javac R1_NaiveCache.java && java -ea R1_NaiveCache
// =============================================================================

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class R1_NaiveCache {

    // Pretend this is a slow database. We count how often it is hit so we
    // can PROVE the cache actually saves work.
    static final AtomicInteger dbCalls = new AtomicInteger();

    static String loadFromDb(String key) {
        dbCalls.incrementAndGet();       // a real query is slow; we count
        return "value-of-" + key;        // calls instead of sleeping so the
    }                                    // 100k-key test stays fast

    // The cache itself is just a map. "cache-aside": the caller checks the
    // cache first, and only on a MISS does it call the db and store it.
    static final Map<String, String> cache = new HashMap<>();

    static String get(String key) {
        String hit = cache.get(key);
        if (hit != null) return hit;     // HIT: fast path, no db
        String value = loadFromDb(key);  // MISS: do the slow work once
        cache.put(key, value);           // remember it for next time
        return value;
    }

    public static void main(String[] args) {
        // First read of "a": MISS -> db called.
        get("a");
        // Next 999 reads of "a": HIT -> db NOT called again.
        for (int i = 0; i < 999; i++) get("a");

        System.out.println("db calls for 1000 reads of 'a' = " + dbCalls.get());
        assert dbCalls.get() == 1 : "cache should make the db be called only once";

        // ---- PROBLEM 1: it grows forever (no size limit, no TTL) ----
        for (int i = 0; i < 100_000; i++) get("key-" + i);
        System.out.println("cache size after 100k distinct keys = " + cache.size());
        assert cache.size() == 100_001 : "naive cache never evicts -> OOM risk";

        // ---- PROBLEM 2: it can serve STALE data forever ----
        // The db value for "a" changed, but the cache still has the old one.
        // A naive cache has no TTL and no invalidation, so it lies forever.
        String served = get("a");
        System.out.println("still serving = " + served + "  (even if db changed!)");
        assert served.equals("value-of-a") : "stale read: cache never expires";

        System.out.println("R1 OK - fast, but unbounded and can go stale. "
                + "R2 adds TTL+eviction, R3 adds stampede protection.");
    }
}
