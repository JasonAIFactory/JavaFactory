// =============================================================================
// R2 - Retry + exponential backoff + jitter + DLQ + idempotency
//
// Goal: fix R1 flaw #1 (lost on failure) the right way.
//   - On failure: do not drop. Retry (up to N times).
//   - Retry delay grows: exponential backoff + jitter (avoid retry storm).
//   - After N failures: move to DLQ (one poison message must not block the queue).
//   - Safe to process the same message twice (idempotency).
//
// Run: java R2_RetryDlqQueue.java
//
// Read and answer:
//   - Where are "attempts" stored? Why inside the message?
//   - What happens if backoff has no jitter? (THEORY 3-(3))
//   - What bad effect of at-least-once does the processedIds check stop?
// =============================================================================

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;

public class R2_RetryDlqQueue {

    // We keep "attempts" inside the message. The queue does not remember state,
    // so "which try is this" must travel with the message.
    record Task(int id, String payload, int attempts) {
        Task retryOnce() { return new Task(id, payload, attempts + 1); }
    }

    static final int MAX_ATTEMPTS = 4;          // try up to 4 times, then DLQ. Never retry forever.
    static final long BASE_BACKOFF_MS = 100;     // 1st 100ms, 2nd 200ms, 3rd 400ms ... exponential

    public static void main(String[] args) throws Exception {
        BlockingQueue<Task> queue = new LinkedBlockingQueue<>();
        BlockingQueue<Task> dlq   = new LinkedBlockingQueue<>();   // jail for dead messages

        // Idempotency: remember ids we already processed successfully.
        // In a distributed system this Set becomes Redis SETNX or a DB unique key.
        Set<Integer> processedIds = ConcurrentHashMap.newKeySet();

        AtomicInteger success = new AtomicInteger();
        AtomicInteger toDlq   = new AtomicInteger();

        int workerCount = 3;
        ExecutorService pool = Executors.newFixedThreadPool(workerCount);

        for (int w = 0; w < workerCount; w++) {
            final int workerId = w;
            pool.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    Task task;
                    try {
                        task = queue.take();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    // ---- Idempotency guard: at-least-once means the same msg can come again ----
                    if (processedIds.contains(task.id())) {
                        // already done -> do not process again, just skip (ack)
                        System.out.println("  [w" + workerId + "] DUP skip task " + task.id());
                        continue;
                    }

                    try {
                        process(workerId, task);
                        processedIds.add(task.id());            // record AFTER success = at-least-once
                        success.incrementAndGet();
                    } catch (Exception e) {
                        // ---- Failure handling: do NOT drop ----
                        if (task.attempts() + 1 >= MAX_ATTEMPTS) {
                            dlq.offer(task);                     // over the limit -> isolate in DLQ
                            toDlq.incrementAndGet();
                            System.out.println("  [w" + workerId + "] -> DLQ task " + task.id()
                                    + " after " + (task.attempts() + 1) + " attempts");
                        } else {
                            long delay = backoffWithJitter(task.attempts());
                            // do not requeue now; put it back after 'delay'.
                            // a separate scheduler returns it to the queue (delayed retry).
                            scheduleRequeue(queue, task.retryOnce(), delay);
                            System.out.println("  [w" + workerId + "] retry task " + task.id()
                                    + " (attempt " + (task.attempts() + 1) + ") in " + delay + "ms");
                        }
                    }
                }
            });
        }

        // Producer: 12 tasks. id multiple of 3 always fails -> watch them go to DLQ.
        for (int i = 1; i <= 12; i++) queue.put(new Task(i, "job-" + i, 0));
        // Push one duplicate on purpose -> watch the idempotency guard block it.
        queue.put(new Task(1, "job-1-DUPLICATE", 0));

        Thread.sleep(5000);
        pool.shutdownNow();
        SCHEDULER.shutdownNow();
        System.out.println("\nsuccess=" + success.get() + " DLQ=" + toDlq.get()
                + " (success + DLQ must equal the number of unique tasks -> zero loss)");
        System.out.println("DLQ content: " + new ArrayList<>(dlq));
    }

    // Exponential backoff = BASE * 2^attempt, plus +/-50% random jitter.
    // Without jitter all workers retry at the same time -> thundering herd.
    static long backoffWithJitter(int attempt) {
        long base = (long) (BASE_BACKOFF_MS * Math.pow(2, attempt));
        long jitter = ThreadLocalRandom.current().nextLong(-base / 2, base / 2 + 1);
        return Math.max(0, base + jitter);
    }

    // One shared scheduler for delayed retry. After 'delay', put the task back.
    static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    static void scheduleRequeue(BlockingQueue<Task> queue, Task task, long delayMs) {
        SCHEDULER.schedule(() -> queue.offer(task), delayMs, TimeUnit.MILLISECONDS);
    }

    static void process(int workerId, Task task) {
        if (task.id() % 3 == 0) {                              // multiples of 3 fail forever -> destined for DLQ
            throw new RuntimeException("poison: task " + task.id());
        }
        sleep(80);
        System.out.println("  [w" + workerId + "] OK task " + task.id());
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
