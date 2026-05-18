// =============================================================================
// S3 - The cache-aside invalidation RACE (the real "hard part")
//
// "There are only two hard things in CS: cache invalidation and naming."
//
// THE TRAP (cache-aside read/write interleave):
//   Reader: MISS -> loads OLD value v1 from db (but pauses before storing)
//   Writer: writes v2 to db, then invalidates the cache
//   Reader: resumes and stores its STALE v1 into the cache
//   => the cache now holds v1 forever (until TTL). Invalidation "worked"
//      but the race re-poisoned the cache right after.
//
// THE PRAGMATIC FIX used in industry: a TTL as a safety net bounds how
// long that stale value can survive (eventual consistency). Stronger
// options noted at the bottom.
//
// We force the bad interleaving deterministically with latches so the
// race is REPRODUCIBLE, not flaky.
//
// Run:  javac S3_InvalidationRace.java && java -ea S3_InvalidationRace
// =============================================================================

import java.util.Map;
import java.util.concurrent.*;

public class S3_InvalidationRace {

    static final Map<String, String> db = new ConcurrentHashMap<>();
    static final Map<String, String> cache = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        db.put("k", "v1");

        CountDownLatch readerLoaded = new CountDownLatch(1);
        CountDownLatch writerDone   = new CountDownLatch(1);

        // READER: miss -> read db (gets old v1) -> WAIT -> store stale value.
        Thread reader = new Thread(() -> {
            if (cache.get("k") == null) {
                String fromDb = db.get("k");          // reads v1 (old)
                readerLoaded.countDown();             // let the writer go now
                await(writerDone);                    // writer fully finishes first
                cache.put("k", fromDb);               // stores STALE v1 -> poison
            }
        });

        // WRITER: write v2 -> invalidate cache.
        Thread writer = new Thread(() -> {
            await(readerLoaded);
            db.put("k", "v2");                        // new truth
            cache.remove("k");                        // invalidate
            writerDone.countDown();
        });

        reader.start(); writer.start();
        reader.join();  writer.join();

        String served = cache.get("k");
        System.out.println("db = " + db.get("k") + ", cache = " + served);
        assert "v2".equals(db.get("k"));
        assert "v1".equals(served) : "the race re-poisoned the cache with stale v1";
        System.out.println("BUG REPRODUCED: cache serves stale v1 while db has v2.");

        // ---- FIX (pragmatic): TTL safety net -> bounded staleness ----
        // With a TTL the poisoned entry self-heals after the TTL window;
        // the system is EVENTUALLY consistent instead of permanently wrong.
        long ttlMs = 50;
        long writtenAt = System.currentTimeMillis();
        sleep(ttlMs + 10);
        boolean expired = System.currentTimeMillis() - writtenAt >= ttlMs;
        if (expired) cache.remove("k");               // TTL eviction kicks in
        String after = cache.get("k");
        if (after == null) after = db.get("k");       // reload -> fresh
        System.out.println("after TTL safety net, served = " + after);
        assert "v2".equals(after) : "TTL bounds the staleness window -> eventual consistency";

        System.out.println("S3: invalidation races are unavoidable in cache-aside; "
                + "TTL bounds the damage. Stronger fixes: write-through, "
                + "delete-after-commit + recheck, or versioned keys. Next: S4 stampede/penetration/avalanche.");
    }

    static void await(CountDownLatch l) {
        try { l.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
