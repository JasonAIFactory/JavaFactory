// =============================================================================
// S5 - Multi-tier cache: L1 (local, in-process) + L2 (shared, Redis-like)
//
// STAGE STORY: even a Redis hop (~1ms) is too slow for the hottest keys
// read millions of times. Add an L1 cache INSIDE the process in front of
// the shared L2. Read order: L1 -> L2 -> db, and populate backwards.
//
//   L1: nanoseconds, per-instance, tiny + SHORT ttl (this is the catch)
//   L2: ~1ms, shared by all instances, larger + longer ttl
//   db: slow, the source of truth
//
// THE TRADE-OFF: L1 is per-instance and is NOT invalidated by another
// instance's write. So L1 can be briefly stale - you accept up to "L1 ttl"
// of staleness in exchange for nanosecond reads. Keep L1 ttl small.
//
// Run:  javac S5_MultiTierCache.java && java -ea S5_MultiTierCache
// =============================================================================

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class S5_MultiTierCache {

    static final AtomicInteger dbCalls = new AtomicInteger();
    static final AtomicInteger l2Calls = new AtomicInteger();

    static final Map<String, String> db = new ConcurrentHashMap<>();
    static final Map<String, String> l2 = new ConcurrentHashMap<>();   // shared (Redis stand-in)

    static final class AppInstance {
        final Map<String, String> l1 = new ConcurrentHashMap<>();      // per-instance

        String get(String key) {
            String v = l1.get(key);
            if (v != null) return v;                 // L1 HIT: fastest, no network

            v = l2.get(key); l2Calls.incrementAndGet();
            if (v != null) { l1.put(key, v); return v; }   // L2 HIT: fill L1

            v = db.get(key); dbCalls.incrementAndGet();    // MISS: slow path
            l2.put(key, v);                                // fill L2 (shared)
            l1.put(key, v);                                // fill L1 (local)
            return v;
        }
    }

    public static void main(String[] args) {
        db.put("hot", "v1");
        AppInstance a = new AppInstance();
        AppInstance b = new AppInstance();

        a.get("hot");                                 // A: L1 miss, L2 miss -> db
        assert dbCalls.get() == 1 && l2Calls.get() == 1;

        for (int i = 0; i < 1000; i++) a.get("hot");  // all L1 hits on A
        assert dbCalls.get() == 1 : "L1 absorbs repeat reads with zero db/L2 traffic";
        assert l2Calls.get() == 1 : "L1 hit must not even touch L2";
        System.out.println("A read hot 1001x -> db=" + dbCalls.get() + " L2=" + l2Calls.get());

        b.get("hot");                                 // B: L1 miss, but L2 HIT (shared) -> no db
        assert dbCalls.get() == 1 : "B benefits from the shared L2, no extra db call";
        System.out.println("B first read -> L2 hit, db still " + dbCalls.get());

        // THE TRADE-OFF demonstrated: a write updates db + shared L2, but
        // A's L1 still holds the old value until A's L1 ttl expires.
        db.put("hot", "v2");
        l2.put("hot", "v2");                          // shared L2 updated
        String fromA = a.get("hot");                  // A's L1 still "v1"
        String fromB = b.get("hot");                  // B's L1 also cached "v1" earlier
        System.out.println("after write: A(L1)=" + fromA + "  B(L1)=" + fromB + "  L2=" + l2.get("hot"));
        assert fromA.equals("v1") : "A's L1 is briefly stale - the accepted cost of an L1 tier";
        assert l2.get("hot").equals("v2") : "L2 is correct; L1 self-heals when its short ttl expires";

        System.out.println("S5: L1+L2 gives ns reads; cost = bounded L1 staleness. "
                + "This is the EVCache/Facebook-style shape. End of evolution track.");
    }
}
