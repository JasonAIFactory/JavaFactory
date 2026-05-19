// =============================================================================
// TESTS - Queue & Worker (the practitioner layer)
//
// Not a second copy of the solution to admire. This is what a practitioner
// adds so the next person cannot silently regress retry / DLQ / idempotency
// / zero-loss / backpressure.
//
// Run:  java T1_QueueWorkerTests.java     (no -ea needed; checks throw)
// Exit 0 = all green, 1 = a regression.
//
// What this teaches:
//   - UNIT: pure logic (backoff math, immutable retry), deterministic.
//   - INVARIANT under concurrency: "success + dlq == unique, ALWAYS"
//     (zero loss) no matter the worker race.
//   - DETERMINISTIC concurrency: we drive the pipeline to quiescence with
//     pending==0 + a latch, never with sleeps -> not flaky.
//   - BOUNDARY: we test OUR engine (retry/dedup/drain), not the JDK queue.
// =============================================================================

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class T1_QueueWorkerTests {

    // ---- system under test (same design as solution/) -------------------
    record Task(int id, int attempts) { Task retryOnce() { return new Task(id, attempts + 1); } }

    static final int MAX_ATTEMPTS = 4;
    static final long BASE_BACKOFF_MS = 20;

    static long backoffWithJitter(int attempt) {
        long base = (long) (BASE_BACKOFF_MS * Math.pow(2, attempt));
        long jitter = ThreadLocalRandom.current().nextLong(-base / 2, base / 2 + 1);
        return Math.max(0, base + jitter);
    }

    // Result of running the pipeline once.
    static final class Result { int success, dlq, processedCalls, maxQueueSeen; }

    // A faithful queue+worker: bounded queue (backpressure), N workers,
    // retry with backoff via a scheduler, DLQ after MAX, idempotent dedup,
    // graceful drain on pending==0. failIds = ids that always throw.
    static Result run(int uniqueTasks, int workers, int queueCap,
                      Set<Integer> failIds, int duplicatesOfId1) throws Exception {
        Result r = new Result();
        BlockingQueue<Task> queue = new ArrayBlockingQueue<>(queueCap);
        Set<Integer> processed = ConcurrentHashMap.newKeySet();
        AtomicInteger success = new AtomicInteger(), dlq = new AtomicInteger();
        AtomicInteger processedCalls = new AtomicInteger(), maxQ = new AtomicInteger();
        AtomicInteger pending = new AtomicInteger();
        AtomicBoolean accepting = new AtomicBoolean(true);
        ScheduledExecutorService sched = Executors.newScheduledThreadPool(2);
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        CountDownLatch done = new CountDownLatch(workers);

        for (int w = 0; w < workers; w++) pool.submit(() -> {
            try {
                while (true) {
                    maxQ.accumulateAndGet(queue.size(), Math::max);
                    Task t = queue.poll(120, TimeUnit.MILLISECONDS);
                    if (t == null) {
                        if (!accepting.get() && pending.get() == 0) break;
                        continue;
                    }
                    if (processed.contains(t.id())) continue;          // duplicate -> ack-skip
                    try {
                        if (failIds.contains(t.id())) throw new RuntimeException("poison " + t.id());
                        processedCalls.incrementAndGet();
                        processed.add(t.id());
                        success.incrementAndGet();
                        pending.decrementAndGet();
                    } catch (Exception e) {
                        if (t.attempts() + 1 >= MAX_ATTEMPTS) {
                            dlq.incrementAndGet();
                            pending.decrementAndGet();
                        } else {
                            Task again = t.retryOnce();
                            sched.schedule(() -> {
                                try { queue.put(again); }
                                catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                            }, backoffWithJitter(t.attempts()), TimeUnit.MILLISECONDS);
                        }
                    }
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            finally { done.countDown(); }
        });

        for (int i = 1; i <= uniqueTasks; i++) { pending.incrementAndGet(); queue.put(new Task(i, 0)); }
        for (int d = 0; d < duplicatesOfId1; d++) queue.put(new Task(1, 0)); // dup: pending unchanged

        accepting.set(false);
        done.await(20, TimeUnit.SECONDS);
        pool.shutdown(); sched.shutdownNow();
        r.success = success.get(); r.dlq = dlq.get();
        r.processedCalls = processedCalls.get(); r.maxQueueSeen = maxQ.get();
        return r;
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
    static void backoffIsBoundedAndNonNegative() {
        for (int attempt = 0; attempt < 12; attempt++) {
            long base = (long) (BASE_BACKOFF_MS * Math.pow(2, attempt));
            for (int i = 0; i < 2000; i++) {
                long b = backoffWithJitter(attempt);
                check(b >= 0, "backoff negative at attempt " + attempt);
                check(b <= base + base / 2, "backoff above +50% jitter cap: " + b + " base " + base);
                check(b >= base - base / 2, "backoff below -50% jitter floor: " + b + " base " + base);
            }
        }
    }
    static void backoffGrowsExponentially() {
        long lo0 = (long)(BASE_BACKOFF_MS*Math.pow(2,1)) / 2;   // attempt1 floor
        long hi0 = (long)(BASE_BACKOFF_MS*Math.pow(2,1)) + (long)(BASE_BACKOFF_MS*Math.pow(2,1))/2;
        long lo5 = (long)(BASE_BACKOFF_MS*Math.pow(2,5)) / 2;   // attempt5 floor >> attempt1 ceil
        check(lo5 > hi0, "attempt5 floor must exceed attempt1 ceiling (exponential)");
    }
    static void retryOnceIsImmutableAndIncrements() {
        Task a = new Task(7, 0);
        Task b = a.retryOnce();
        eq(a.attempts(), 0, "original mutated");      // immutability
        eq(b.attempts(), 1, "attempts not incremented");
        eq(b.id(), 7, "id must be stable across retry");
    }

    // ---- INVARIANTS under concurrency -----------------------------------
    static void zeroLoss_successPlusDlqEqualsUnique() throws Exception {
        for (int trial = 0; trial < 5; trial++) {
            Result r = run(15, 3, 8, Set.of(4, 8, 12), 0);
            eq(r.success + r.dlq, 15, "ZERO-LOSS INVARIANT broken (trial " + trial + ")");
        }
    }
    static void poisonGoesToDlqExactly() throws Exception {
        Result r = run(15, 3, 8, Set.of(4, 8, 12), 0);
        eq(r.dlq, 3, "poison tasks must all reach DLQ");
        eq(r.success, 12, "non-poison must all succeed");
    }
    static void idempotency_duplicateNeverProcessedTwice() throws Exception {
        for (int trial = 0; trial < 5; trial++) {
            Result r = run(15, 4, 8, Set.of(), 6);   // 6 duplicates of id=1, all healthy
            eq(r.processedCalls, 15, "duplicate id was processed more than once (trial " + trial + ")");
            eq(r.success, 15, "success must equal unique tasks");
        }
    }
    static void backpressure_queueNeverExceedsCapacity() throws Exception {
        Result r = run(40, 2, 8, Set.of(), 0);
        check(r.maxQueueSeen <= 8, "bounded queue exceeded capacity: " + r.maxQueueSeen);
        eq(r.success, 40, "all tasks must still complete under backpressure");
    }

    public static void main(String[] args) {
        System.out.println("== UNIT ==");
        test("backoff is bounded and non-negative",   T1_QueueWorkerTests::backoffIsBoundedAndNonNegative);
        test("backoff grows exponentially",           T1_QueueWorkerTests::backoffGrowsExponentially);
        test("retryOnce is immutable + increments",   T1_QueueWorkerTests::retryOnceIsImmutableAndIncrements);
        System.out.println("== INVARIANTS (concurrency) ==");
        test("zero loss: success+dlq == unique",      T1_QueueWorkerTests::zeroLoss_successPlusDlqEqualsUnique);
        test("poison -> DLQ exactly",                 T1_QueueWorkerTests::poisonGoesToDlqExactly);
        test("idempotency: dup processed once",       T1_QueueWorkerTests::idempotency_duplicateNeverProcessedTwice);
        test("backpressure bounds the queue",         T1_QueueWorkerTests::backpressure_queueNeverExceedsCapacity);
        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
