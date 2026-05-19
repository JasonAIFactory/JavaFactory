// =============================================================================
// BLANK exercise - Queue & Worker (all-in-one)
//
// Rule: do NOT open solution/. Sit with it for 30+ minutes.
//       The point where you get stuck is the real learning.
// Goal: combine R1~R3 ideas into ONE file by yourself.
//
// Requirements (fill the TODOs):
//   1) Bounded queue (capacity 8). When full, block the producer (backpressure).
//   2) 3 workers (competing consumers).
//   3) On failure: retry up to 4 times, exponential backoff + jitter.
//   4) After 4 failures: move to DLQ.
//   5) Idempotency: never process the same id twice.
//   6) Graceful shutdown: stop new work -> drain queue -> exit (zero loss).
//   7) Metrics: success / dlq counts.
//
// Check: all asserts at the end of main must pass.
// Run:   java -ea Q1_QueueWorker.java   (-ea enables assert - you MUST add it)
//
// Two traps left ON PURPOSE (they appear when you run with -ea, debug them):
//   Trap A) If you decide "drain done" only by "is the queue empty?", a delayed
//           retry still waiting on backoff (outside the queue, in the scheduler)
//           is lost when the worker exits first.
//           => Exit condition must be "in-flight count == 0", not "queue empty".
//   Trap B) If you requeue a retry with offer(), a full bounded queue drops it
//           silently -> it never finishes. => Requeue with a blocking put().
// =============================================================================

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.*;

public class Q1_QueueWorker {

    // TODO 0: a Task record that carries "attempts". retryOnce() returns a new Task with attempts+1.
    record Task(int id, int attempts) {
        Task retryOnce() { /* TODO */ return null; }
    }

    static final int MAX_ATTEMPTS = 4;
    static final long BASE_BACKOFF_MS = 50;

    static final AtomicInteger success = new AtomicInteger();
    static final AtomicInteger dlqCount = new AtomicInteger();
    static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) throws Exception {

        // TODO 1: declare a bounded BlockingQueue<Task> queue with capacity 8.
        BlockingQueue<Task> queue = null;
        BlockingQueue<Task> dlq = new LinkedBlockingQueue<>();

        // TODO 5: a thread-safe Set of ids already processed (for idempotency).
        Set<Integer> processedIds = null;

        AtomicBoolean accepting = new AtomicBoolean(true);
        int workerCount = 3;
        ExecutorService pool = Executors.newFixedThreadPool(workerCount);
        CountDownLatch done = new CountDownLatch(workerCount);

        for (int w = 0; w < workerCount; w++) {
            final int id = w;
            pool.submit(() -> {
                try {
                    while (true) {
                        // TODO 6: poll(timeout). If null and !accepting and no work left -> break.
                        //         If work may still arrive -> continue. (See Trap A.)
                        Task task = todo6_pollWithDrain(queue, accepting); // <- replace this call

                        // TODO 5: if this id was already processed -> skip (continue).

                        try {
                            process(id, task);
                            // TODO 5: record success + increment success.
                        } catch (Exception e) {
                            // TODO 3/4: if attempts+1 >= MAX -> DLQ + dlqCount++,
                            //           else schedule a retry of retryOnce() back to the queue
                            //           after backoff. (See Trap B for how to requeue.)
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        // Producer: 15 tasks. id multiple of 4 always fails (-> DLQ). One duplicate.
        for (int i = 1; i <= 15; i++) {
            // TODO 1: backpressure - use the method that BLOCKS when the queue is full.
        }
        // duplicate message (idempotency test)
        // TODO: put one more Task with id=1 into the queue.

        // TODO 6: graceful shutdown - accepting=false -> done.await(...) -> pool.shutdown().
        scheduler.shutdownNow();

        // ---- Verification (do not touch) ----
        int uniqueTasks = 15;                 // ids 1..15
        int expectedDlq = 15 / 4;             // 4,8,12 -> 3
        int expectedSuccess = uniqueTasks - expectedDlq;
        System.out.println("success=" + success.get() + " dlq=" + dlqCount.get());
        assert success.get() == expectedSuccess : "wrong success count: " + success.get();
        assert dlqCount.get() == expectedDlq : "wrong DLQ count: " + dlqCount.get();
        assert success.get() + dlqCount.get() == uniqueTasks : "task loss!";
        System.out.println("ALL ASSERTIONS PASSED - zero loss, idempotency/retry/DLQ OK");
    }

    static void process(int workerId, Task task) {
        if (task.id() % 4 == 0) throw new RuntimeException("poison " + task.id());
        sleep(60);
    }

    // Skeleton stub so the file COMPILES (it declares InterruptedException so
    // the worker's catch is reachable). It fails fast until you implement it.
    // Replace this whole method body with the real poll(timeout) + drain
    // decision (Trap A): return a task, or null when there is no more work.
    static Task todo6_pollWithDrain(BlockingQueue<Task> queue, AtomicBoolean accepting)
            throws InterruptedException {
        throw new UnsupportedOperationException("TODO 6: implement poll(timeout) + drain/break");
    }

    static long backoffWithJitter(int attempt) {
        // TODO 3: BASE * 2^attempt + (+/-50% jitter), never negative.
        return 0;
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
