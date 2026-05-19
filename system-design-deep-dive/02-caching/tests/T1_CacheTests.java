// =============================================================================
// TESTS - Caching (the practitioner layer)
//
// Pins the four properties so nobody silently regresses them:
//   TTL self-heal, LRU bound+order, single-flight (stampede), null-cache.
//
// Run:  java T1_CacheTests.java     (no -ea needed; checks throw)
// Exit 0 = all green, 1 = a regression.
//
// What this teaches:
//   - UNIT: deterministic single-threaded behavior (hit, LRU, TTL, null).
//   - INVARIANT under concurrency: "N concurrent misses => exactly ONE
//     loader call" no matter the race (single-flight), repeated rounds.
//   - BOUNDARY: we test OUR cache, with a counting fake "db" - we do not
//     test a real database.
// =============================================================================

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class T1_CacheTests {

    // ---- counting fake datasource (a test double) -----------------------
    static final AtomicInteger dbCalls = new AtomicInteger();
    static String db(String key) {
        dbCalls.incrementAndGet();
        sleep(30);
        return key.startsWith("ghost") ? null : "val-" + key;
    }

    // ---- system under test (same design as solution/) -------------------
    static final String NULL_SENTINEL = " NULL";
    static final class Entry {
        final String value; final long expiresAt;
        Entry(String v, long ttl) { value = v; expiresAt = System.currentTimeMillis() + ttl; }
        boolean expired() { return System.currentTimeMillis() >= expiresAt; }
    }
    static final class Cache {
        final int maxSize; final long ttlMs;
        final LinkedHashMap<String, Entry> map;
        final ConcurrentHashMap<String, CompletableFuture<String>> inFlight = new ConcurrentHashMap<>();
        Cache(int maxSize, long ttlMs) {
            this.maxSize = maxSize; this.ttlMs = ttlMs;
            this.map = new LinkedHashMap<>(16, 0.75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, Entry> e) {
                    return size() > Cache.this.maxSize;
                }
            };
        }
        Entry lookup(String k) {
            synchronized (map) {
                Entry e = map.get(k);
                if (e == null) return null;
                if (e.expired()) { map.remove(k); return null; }
                return e;
            }
        }
        void store(String k, String v) { synchronized (map) { map.put(k, new Entry(v, ttlMs)); } }
        String get(String key) {
            Entry e = lookup(key);
            if (e != null) return e.value.equals(NULL_SENTINEL) ? null : e.value;
            CompletableFuture<String> f = inFlight.computeIfAbsent(key, k ->
                    CompletableFuture.supplyAsync(() -> {
                        String v = db(k);
                        store(k, v == null ? NULL_SENTINEL : v);
                        return v;
                    }));
            try { return f.join(); } finally { inFlight.remove(key, f); }
        }
        int size() { synchronized (map) { return map.size(); } }
    }

    // ---- tiny framework -------------------------------------------------
    static int passed = 0, failed = 0;
    static void test(String name, ThrowingRunnable b) {
        try { b.run(); System.out.println("PASS  " + name); passed++; }
        catch (Throwable t) { System.out.println("FAIL  " + name + "  -> " + t); failed++; }
    }
    interface ThrowingRunnable { void run() throws Exception; }
    static void check(boolean c, String m) { if (!c) throw new AssertionError(m); }
    static void eq(long a, long b, String m) { if (a != b) throw new AssertionError(m + " (got " + a + ", want " + b + ")"); }

    // ---- UNIT -----------------------------------------------------------
    static void repeatReadsHitTheCache() {
        dbCalls.set(0);
        Cache c = new Cache(3, 10_000);
        for (int i = 0; i < 50; i++) c.get("a");
        eq(dbCalls.get(), 1, "repeat reads must hit cache, not the db");
    }
    static void lruBoundsSizeAndEvictsEldest() {
        dbCalls.set(0);
        Cache c = new Cache(3, 10_000);
        c.get("a"); c.get("b"); c.get("c");
        c.get("a");                       // touch a -> b is now eldest
        c.get("d");                       // insert d -> evict b
        eq(c.size(), 3, "LRU must bound size");
        dbCalls.set(0);
        c.get("a"); eq(dbCalls.get(), 0, "recently-used 'a' must still be cached");
        c.get("b"); eq(dbCalls.get(), 1, "evicted 'b' must be refetched");
    }
    static void penetrationCachesTheMiss() {
        dbCalls.set(0);
        Cache c = new Cache(10, 10_000);
        for (int i = 0; i < 100; i++) check(c.get("ghost") == null, "absent key must read as null");
        eq(dbCalls.get(), 1, "the 'not found' must be cached (penetration)");
    }
    static void ttlSelfHeals() {
        dbCalls.set(0);
        Cache c = new Cache(10, 50);
        c.get("k");
        int before = dbCalls.get();
        c.get("k"); eq(dbCalls.get(), before, "within ttl must be a hit");
        sleep(75);
        c.get("k"); eq(dbCalls.get(), before + 1, "after ttl must reload (bounded staleness)");
    }

    // ---- INVARIANT under concurrency ------------------------------------
    static void stampedeCollapsesToOneLoader() throws Exception {
        for (int round = 1; round <= 6; round++) {
            Cache s = new Cache(1000, 10_000);
            dbCalls.set(0);
            ExecutorService pool = Executors.newFixedThreadPool(64);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> fs = new ArrayList<>();
            for (int i = 0; i < 300; i++)
                fs.add(pool.submit(() -> { awaitL(start); s.get("hot"); }));
            start.countDown();
            for (Future<?> x : fs) x.get();
            pool.shutdown();
            eq(dbCalls.get(), 1, "SINGLE-FLIGHT INVARIANT broken in round " + round);
        }
    }
    static void sizeNeverExceedsMaxUnderConcurrency() throws Exception {
        Cache s = new Cache(50, 10_000);
        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> fs = new ArrayList<>();
        AtomicBoolean over = new AtomicBoolean(false);
        for (int i = 0; i < 2000; i++) {
            final int k = i;
            fs.add(pool.submit(() -> { awaitL(start); s.get("k" + k); if (s.size() > 50) over.set(true); }));
        }
        start.countDown();
        for (Future<?> x : fs) x.get();
        pool.shutdown();
        check(!over.get(), "LRU bound violated under concurrency (size > max)");
        check(s.size() <= 50, "final size exceeds max: " + s.size());
    }

    public static void main(String[] args) {
        System.out.println("== UNIT ==");
        test("repeat reads hit the cache",        T1_CacheTests::repeatReadsHitTheCache);
        test("LRU bounds size + evicts eldest",   T1_CacheTests::lruBoundsSizeAndEvictsEldest);
        test("penetration caches the miss",       T1_CacheTests::penetrationCachesTheMiss);
        test("TTL self-heals after expiry",       T1_CacheTests::ttlSelfHeals);
        System.out.println("== INVARIANTS (concurrency) ==");
        test("stampede collapses to one loader",  T1_CacheTests::stampedeCollapsesToOneLoader);
        test("size never exceeds max under load", T1_CacheTests::sizeNeverExceedsMaxUnderConcurrency);
        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    static void awaitL(CountDownLatch l) { try { l.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
}
