// =============================================================================
// R3 - Bounded queue + backpressure + graceful shutdown + metrics
//
// Goal: fix R1 flaw #2 (infinite queue) and #3 (loss on shutdown),
//       and expose the "queue depth" metric every real system watches.
//   - Bounded queue: when full, block the producer (simplest backpressure)
//     OR reject (HTTP 429 style) - both shown.
//   - Graceful shutdown: stop taking new work -> drain queue -> exit cleanly.
//   - Metrics: enqueued / processed / rejected / queueDepth (autoscale signal).
//
// Run: java R3_BackpressureGracefulShutdown.java
//
// Read and answer:
//   - With capacity 5 and a fast producer, which line blocks? (offer vs put)
//   - Why is shutdown() vs shutdownNow() the key to "zero loss"?
//   - If queueDepth keeps growing, what does the autoscaler do? (add workers)
// =============================================================================

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

public class R3_BackpressureGracefulShutdown {

    record Task(int id) {}

    // ---- Metrics: in real life Prometheus gauge/counter. Here atomic counters. ----
    static final AtomicInteger enqueued  = new AtomicInteger();
    static final AtomicInteger processed = new AtomicInteger();
    static final AtomicInteger rejected  = new AtomicInteger();

    public static void main(String[] args) throws Exception {

        // Capacity 5. This is the heart of backpressure - the queue cannot grow forever.
        BlockingQueue<Task> queue = new ArrayBlockingQueue<>(5);

        AtomicBoolean acceptingNewWork = new AtomicBoolean(true);  // graceful shutdown step 1 switch
        int workerCount = 2;
        ExecutorService pool = Executors.newFixedThreadPool(workerCount);
        CountDownLatch workersDone = new CountDownLatch(workerCount);

        for (int w = 0; w < workerCount; w++) {
            final int workerId = w;
            pool.submit(() -> {
                try {
                    while (true) {
                        // poll(timeout) - unlike take(), it does not block forever.
                        // After the shutdown signal, when the queue is empty the
                        // worker can leave and exit cleanly.
                        Task task = queue.poll(200, TimeUnit.MILLISECONDS);
                        if (task == null) {
                            // queue empty AND not accepting new work -> drain done, exit.
                            if (!acceptingNewWork.get() && queue.isEmpty()) break;
                            continue;                              // work may still arrive, keep polling
                        }
                        process(workerId, task);
                        processed.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    workersDone.countDown();                       // tell main this worker finished
                }
            });
        }

        // ---- Metrics reporter: print queue depth every 0.5s (autoscaler signal) ----
        ScheduledExecutorService reporter = Executors.newSingleThreadScheduledExecutor();
        reporter.scheduleAtFixedRate(() ->
                System.out.println("  [metric] queueDepth=" + queue.size()
                        + " enq=" + enqueued.get() + " proc=" + processed.get()
                        + " rej=" + rejected.get()),
                0, 500, TimeUnit.MILLISECONDS);

        // ---- Producer: push 30 tasks faster than workers -> show both backpressure ways ----
        for (int i = 1; i <= 30; i++) {
            Task t = new Task(i);
            if (i <= 20) {
                // Way A) Blocking backpressure: if full, the producer waits here.
                //        => the producer naturally slows down (accepts requests slower).
                queue.put(t);
                enqueued.incrementAndGet();
            } else {
                // Way B) Reject backpressure: if no space within 50ms, reject (HTTP 429).
                //        => do not block the producer; tell the caller "busy now".
                if (queue.offer(t, 50, TimeUnit.MILLISECONDS)) {
                    enqueued.incrementAndGet();
                } else {
                    rejected.incrementAndGet();
                    System.out.println("  [producer] REJECTED task " + i + " (429-style)");
                }
            }
        }

        // ---- Graceful shutdown: the key to zero loss ----
        acceptingNewWork.set(false);          // 1) stop accepting new work
        // 2) wait until workers drain everything left in the queue
        boolean drained = workersDone.await(10, TimeUnit.SECONDS);
        pool.shutdown();                       // 3) only now shut the pool. NOT shutdownNow()!
        reporter.shutdownNow();

        System.out.println("\ndrained=" + drained
                + " | enqueued=" + enqueued.get()
                + " processed=" + processed.get()
                + " rejected=" + rejected.get());
        System.out.println("Check: enqueued == processed means zero loss. "
                + "rejected was refused ON PURPOSE by backpressure (not a loss).");
    }

    static void process(int workerId, Task task) {
        sleep(120);                            // make workers slow on purpose so the queue fills and backpressure shows
        System.out.println("  [w" + workerId + "] done " + task.id());
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
