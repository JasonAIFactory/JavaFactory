// =============================================================================
// S1 - In-process (local) cache. Cache-aside in a HashMap.
//
// STAGE STORY: traffic grew, the db is the bottleneck. We add the smallest
// possible cache: a map inside the app process. Reads of a hot key now
// skip the db. Cheapest possible win, zero new infrastructure.
//
// WHERE IT BREAKS:
//   (1) it grows forever (no TTL / no size limit) -> handled in S3 idea.
//   (2) MULTI-INSTANCE PROBLEM: run 3 app instances and each has its own
//       separate map. A write/invalidation on instance A does not reach
//       instances B and C -> they keep serving stale data. This is THE
//       reason teams move to a shared (distributed) cache in S2.
//
// Run:  javac S1_LocalCache.java && java -ea S1_LocalCache
// =============================================================================

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class S1_LocalCache {

    // Each instance owns its own private cache map.
    static final class AppInstance {
        final String name;
        final Map<String, String> local = new HashMap<>();
        final AtomicInteger dbCalls = new AtomicInteger();
        AppInstance(String name) { this.name = name; }

        String read(String key, Map<String, String> db) {
            String hit = local.get(key);
            if (hit != null) return hit;
            String v = db.get(key);          // "slow" db read
            dbCalls.incrementAndGet();
            local.put(key, v);
            return v;
        }
    }

    public static void main(String[] args) {
        Map<String, String> db = new HashMap<>();
        db.put("price:42", "100");

        AppInstance a = new AppInstance("A");
        AppInstance b = new AppInstance("B");

        // Single-instance win: repeated reads on A hit the db only once.
        for (int i = 0; i < 10; i++) a.read("price:42", db);
        assert a.dbCalls.get() == 1 : "local cache saves repeat db reads on the same instance";
        System.out.println("A read price 10x, db calls = " + a.dbCalls.get());

        b.read("price:42", db);              // B caches 100 too

        // Now the price changes and instance A invalidates ITS cache only.
        db.put("price:42", "200");
        a.local.remove("price:42");          // A will refetch -> sees 200

        String fromA = a.read("price:42", db);
        String fromB = b.read("price:42", db);   // B still has stale 100!
        System.out.println("after price change: A=" + fromA + "  B=" + fromB);

        assert fromA.equals("200") : "A invalidated, sees fresh value";
        assert fromB.equals("100") : "B has a SEPARATE cache -> serves stale (the multi-instance bug)";

        System.out.println("S1: great per-instance, but caches are not shared. "
                + "Next: S2 puts the cache OUT of the process (Redis-style).");
    }
}
