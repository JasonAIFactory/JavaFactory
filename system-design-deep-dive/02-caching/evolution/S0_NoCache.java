// =============================================================================
// S0 - No cache. Every read hits the database.
//
// STAGE STORY: day one. One service, one DB. Reads are simple and always
// correct (no staleness). This is the RIGHT starting point - do not add a
// cache until you can measure that you need one.
//
// WHERE IT BREAKS: the SAME expensive query runs again and again. Latency
// is the query latency every time, and the db load grows linearly with
// traffic. At 100x reads, the db is doing 100x identical work.
//
// Run:  javac S0_NoCache.java && java -ea S0_NoCache
// =============================================================================

import java.util.concurrent.atomic.AtomicInteger;

public class S0_NoCache {
    static final AtomicInteger dbCalls = new AtomicInteger();

    static String getUserProfile(long userId) {
        dbCalls.incrementAndGet();
        sleep(20);                       // a join-heavy profile query
        return "profile#" + userId;
    }

    public static void main(String[] args) {
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < 50; i++) getUserProfile(7);   // same user, 50 times
        long ms = System.currentTimeMillis() - t0;

        System.out.println("50 reads of the same profile: db calls = "
                + dbCalls.get() + ", time = " + ms + "ms");
        assert dbCalls.get() == 50 : "no cache -> every read is a db call";
        System.out.println("S0: correct but wasteful. Next: S1 caches it in-process.");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
