// =============================================================================
// S2 - Distributed (shared) cache. One cache, all instances see it.
//
// STAGE STORY: to fix S1's multi-instance bug we move the cache OUT of the
// app into a shared store (think Redis). Every instance reads/writes the
// same cache, so an invalidation by one instance is seen by all.
//
// (Here a thread-safe singleton stands in for a Redis server, so the
// concept is run-verifiable without network. In production this is Redis.)
//
// WHERE IT BREAKS:
//   - a network hop is added (~1ms) - still far cheaper than the db.
//   - the cache is now a shared dependency / new failure mode (S? handles
//     it with TTL + graceful fallback to db).
//   - INVALIDATION ORDERING is still hard: see S3.
//
// Run:  javac S2_DistributedCache.java && java -ea S2_DistributedCache
// =============================================================================

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class S2_DistributedCache {

    // Stand-in for Redis: one shared, thread-safe store all instances use.
    static final class SharedCache {
        final Map<String, String> data = new ConcurrentHashMap<>();
        String get(String k) { return data.get(k); }
        void put(String k, String v) { data.put(k, v); }
        void invalidate(String k) { data.remove(k); }
    }

    static final class AppInstance {
        final SharedCache cache;
        final Map<String, String> db;
        final AtomicInteger dbCalls = new AtomicInteger();
        AppInstance(SharedCache cache, Map<String, String> db) { this.cache = cache; this.db = db; }

        String read(String key) {
            String hit = cache.get(key);
            if (hit != null) return hit;
            String v = db.get(key);
            dbCalls.incrementAndGet();
            cache.put(key, v);
            return v;
        }

        // Write path: update db, then invalidate the SHARED cache so every
        // instance refetches the fresh value on its next read.
        void write(String key, String value) {
            db.put(key, value);
            cache.invalidate(key);
        }
    }

    public static void main(String[] args) {
        Map<String, String> db = new ConcurrentHashMap<>();
        db.put("price:42", "100");
        SharedCache redis = new SharedCache();

        AppInstance a = new AppInstance(redis, db);
        AppInstance b = new AppInstance(redis, db);
        AppInstance c = new AppInstance(redis, db);

        a.read("price:42");                  // MISS on A -> db -> shared cache = 100
        assert b.read("price:42").equals("100");  // HIT for B (shared!) no db call
        assert c.read("price:42").equals("100");  // HIT for C
        assert b.dbCalls.get() == 0 && c.dbCalls.get() == 0
                : "shared cache: only the first instance pays the db cost";

        // A writes a new price and invalidates the SHARED cache.
        a.write("price:42", "200");

        // B and C now see the fresh value - the S1 multi-instance bug is gone.
        String fromB = b.read("price:42");
        String fromC = c.read("price:42");
        System.out.println("after shared invalidation: B=" + fromB + "  C=" + fromC);
        assert fromB.equals("200") && fromC.equals("200")
                : "shared cache invalidation reaches every instance";

        System.out.println("S2: one shared cache fixes staleness across instances. "
                + "Next: S3 shows invalidation ORDERING is still a trap.");
    }
}
