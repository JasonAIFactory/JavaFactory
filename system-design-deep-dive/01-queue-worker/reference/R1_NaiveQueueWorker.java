// =============================================================================
// R1 - The simplest queue + worker (flaws left in ON PURPOSE)
//
// Goal: see the "skeleton" of queue+worker, and SEE why it is not enough.
// Run:  java R1_NaiveQueueWorker.java
//
// Read the code and answer for yourself:
//   - If processing fails, what happens to that task?  (hint: it is lost)
//   - If the producer is faster than workers, queue size?  (hint: grows forever)
//   - If we stop the program, the task being processed?  (hint: lost)
// These 3 flaws are fixed in R2 and R3. Compare them.
// =============================================================================

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class R1_NaiveQueueWorker {

    // A task (message). In real life this is JSON, but it is just "work + id".
    record Task(int id, String payload) {}

    public static void main(String[] args) throws Exception {

        // BlockingQueue = thread-safe, and take() waits automatically when empty.
        // LinkedBlockingQueue default capacity is Integer.MAX_VALUE,
        // so it is basically an "infinite queue". This is flaw #2
        // (no backpressure). R3 fixes it with a bounded queue.
        BlockingQueue<Task> queue = new LinkedBlockingQueue<>();

        AtomicInteger processed = new AtomicInteger();   // many workers add at once -> atomic counter
        int workerCount = 3;                              // 3 cooks. The start of scale-out.
        ExecutorService pool = Executors.newFixedThreadPool(workerCount);

        // ---- Worker pool: competing consumers (all read the same queue) ----
        for (int w = 0; w < workerCount; w++) {
            final int workerId = w;
            pool.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Task task = queue.take();            // if queue is empty, block here (no busy-wait)
                        process(workerId, task);              // do the real work. What if this throws?
                        processed.incrementAndGet();          // only count success
                        // flaw #1: if process() throws, we never reach the line above;
                        //          we jump to catch. The task is already removed from
                        //          the queue and there is no retry -> "lost".
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();   // restore interrupt flag, then exit loop
                        break;
                    } catch (Exception e) {
                        // flaw #1 shown directly: drop the failed task, move on.
                        // No retry, no DLQ.
                        System.out.println("  [worker " + workerId + "] FAILED, task lost: " + e.getMessage());
                    }
                }
            });
        }

        // ---- Producer: push 20 tasks into the queue ----
        for (int i = 1; i <= 20; i++) {
            queue.put(new Task(i, "job-" + i));               // put = add to queue (here always instant: infinite queue)
        }

        Thread.sleep(3000);                                   // demo only: roughly wait for work (R3 does real shutdown)
        pool.shutdownNow();                                   // force-stop workers -> task in progress is lost (flaw #3)
        System.out.println("\nSubmitted 20, processed OK: " + processed.get()
                + " (the rest were lost on failure or cut off at shutdown)");
    }

    // If id is a multiple of 7, fail on purpose so we can SEE the "lost on failure" flaw.
    static void process(int workerId, Task task) {
        if (task.id() % 7 == 0) {
            throw new RuntimeException("simulated failure for task " + task.id());
        }
        sleep(100);                                           // pretend the work takes time
        System.out.println("  [worker " + workerId + "] done task " + task.id());
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
