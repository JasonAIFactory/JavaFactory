// =============================================================================
// R2 - Bounded cache with TTL + LRU eviction (fixes R1's two problems)
//
// WHY THIS FILE: a real cache cannot grow forever and cannot lie forever.
//   - TTL (time to live): an entry auto-expires -> bounds STALENESS.
//   - LRU eviction + max size: drop the least-recently-used entry when
//     full -> bounds MEMORY.
// These are the two knobs every cache product (Redis, Caffeine) exposes.
//
// Run:  javac R2_TtlLruCache.java && java -ea R2_TtlLruCache
// =============================================================================

import java.util.LinkedHashMap;
import java.util.Map;

public class R2_TtlLruCache {

    static final class Entry {
        final String value;
        final long expiresAt;          // epoch millis when this entry dies
        Entry(String value, long ttlMs) { this.value = value; this.expiresAt = now() + ttlMs; }
        boolean expired() { return now() >= expiresAt; }
    }

    // LinkedHashMap in access-order = a ready-made LRU list. Override
    // removeEldestEntry so it auto-drops the least-recently-used entry
    // once we exceed maxSize. (Not thread-safe; R3 handles concurrency.)
    static final class LruCache extends LinkedHashMap<String, Entry> {
        private final int maxSize;
        LruCache(int maxSize) { super(16, 0.75f, true /* access order */); this.maxSize = maxSize; }
        @Override protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
            return size() > maxSize;   // evict the LRU when over capacity
        }
    }

    static long mockNow = 1_000_000L;          // controllable clock for the TTL test
    static long now() { return mockNow; }

    final LruCache map;
    final long ttlMs;
    int dbCalls = 0;

    R2_TtlLruCache(int maxSize, long ttlMs) { this.map = new LruCache(maxSize); this.ttlMs = ttlMs; }

    String loadFromDb(String key) { dbCalls++; return "v1-" + key; }

    String get(String key) {
        Entry e = map.get(key);
        if (e != null && !e.expired()) return e.value;     // fresh HIT
        // MISS or EXPIRED: reload and overwrite.
        String value = loadFromDb(key);
        map.put(key, new Entry(value, ttlMs));
        return value;
    }

    public static void main(String[] args) {
        // ---- TTL bounds staleness ----
        R2_TtlLruCache c = new R2_TtlLruCache(1000, /*ttl*/ 100);
        c.get("a");                                  // MISS -> 1 db call
        c.get("a");                                  // fresh HIT -> still 1
        assert c.dbCalls == 1 : "second read within TTL must be a hit";

        mockNow += 101;                              // time passes, entry expires
        c.get("a");                                  // EXPIRED -> reload -> 2
        assert c.dbCalls == 2 : "after TTL the entry must be refetched (not stale forever)";
        System.out.println("TTL OK: stale read is bounded by ttl, db calls = " + c.dbCalls);

        // ---- LRU + max size bounds memory ----
        R2_TtlLruCache lru = new R2_TtlLruCache(/*maxSize*/ 3, 10_000);
        lru.get("a"); lru.get("b"); lru.get("c");    // cache = [a,b,c]
        lru.get("a");                                // touch a -> a is now most recent
        lru.get("d");                                // over capacity -> evict LRU = "b"
        assert lru.map.containsKey("a") : "a was recently used, must stay";
        assert !lru.map.containsKey("b") : "b was least-recently-used, must be evicted";
        assert lru.map.size() == 3 : "size must stay bounded at maxSize";
        System.out.println("LRU OK: evicted 'b', cache keys = " + lru.map.keySet());

        System.out.println("R2 OK - bounded memory + bounded staleness. "
                + "But under load, many threads can all MISS the same key at once -> R3.");
    }
}
