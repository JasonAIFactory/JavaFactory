// =============================================================================
// SOLUTION - Queue & Worker (all-in-one)
// Run: java -ea Q1_QueueWorker_Solution.java
// Compare line by line with blank/Q1_QueueWorker.java and ask "why this way?".
// =============================================================================

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.*;

public class Q1_QueueWorker_Solution {

    record Task(int id, int attempts) {
        Task retryOnce() { return new Task(id, attempts + 1); }   // new immutable object with attempts+1
    }

    static final int MAX_ATTEMPTS = 4;
    static final long BASE_BACKOFF_MS = 50;

    static final AtomicInteger success = new AtomicInteger();
    static final AtomicInteger dlqCount = new AtomicInteger();
    // Lesson (Trap A): if "drain done" is judged only by "is the queue empty",
    //   a retry waiting on backoff (still in the scheduler, not in the queue)
    //   is lost when the worker exits first.
    //   => Exit condition is "pending == 0", not "queue empty".
    static final AtomicInteger pending = new AtomicInteger();
    // Lesson (Trap B): requeue with offer() drops the retry when the bounded
    //   queue is full (pending never drops -> never drains). Use a blocking
    //   put(). Use a pool so one put() blocking does not freeze other retries.
    static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public static void main(String[] args) throws Exception {

        BlockingQueue<Task> queue = new ArrayBlockingQueue<>(8);          // bounded = backpressure
        BlockingQueue<Task> dlq = new LinkedBlockingQueue<>();
        Set<Integer> processedIds = ConcurrentHashMap.newKeySet();        // idempotency guard

        AtomicBoolean accepting = new AtomicBoolean(true);
        int workerCount = 3;
        ExecutorService pool = Executors.newFixedThreadPool(workerCount);
        CountDownLatch done = new CountDownLatch(workerCount);

        for (int w = 0; w < workerCount; w++) {
            final int id = w;
            pool.submit(() -> {
                try {
                    while (true) {
                        Task task = queue.poll(150, TimeUnit.MILLISECONDS);
                        if (task == null) {
                            if (!accepting.get() && pending.get() == 0) break; // pending 0 -> truly drained
                            continue;                                          // a delayed retry may still be out there
                        }
                        if (processedIds.contains(task.id())) continue;        // duplicate -> skip (ack), pending unchanged
                        try {
                            process(id, task);
                            processedIds.add(task.id());                       // record AFTER success = at-least-once
                            success.incrementAndGet();
                            pending.decrementAndGet();                         // reached a terminal state
                        } catch (Exception e) {
                            if (task.attempts() + 1 >= MAX_ATTEMPTS) {
                                dlq.offer(task);                               // over the limit -> isolate
                                dlqCount.incrementAndGet();
                                pending.decrementAndGet();                     // reached a terminal state
                            } else {
                                long delay = backoffWithJitter(task.attempts());
                                Task retried = task.retryOnce();
                                scheduler.schedule(() -> {                       // retry not done yet -> keep pending
                                    try { queue.put(retried); }                 // put = no loss (waits if full)
                                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                                }, delay, TimeUnit.MILLISECONDS);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        for (int i = 1; i <= 15; i++) {
            pending.incrementAndGet();                                          // one unique task = pending +1
            queue.put(new Task(i, 0));                                          // put = blocks if full (backpressure)
        }
        queue.put(new Task(1, 0));                                              // duplicate -> idempotency guard blocks it (pending unchanged)

        accepting.set(false);                                                   // 1) stop new work
        done.await(15, TimeUnit.SECONDS);                                       // 2) wait for drain
        pool.shutdown();                                                        // 3) clean exit (NOT shutdownNow)
        scheduler.shutdownNow();

        int uniqueTasks = 15;
        int expectedDlq = 15 / 4;                 // 4,8,12
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

    static long backoffWithJitter(int attempt) {
        long base = (long) (BASE_BACKOFF_MS * Math.pow(2, attempt));
        long jitter = ThreadLocalRandom.current().nextLong(-base / 2, base / 2 + 1);
        return Math.max(0, base + jitter);
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
