// =============================================================================
// STAGE 3 - Keep the system alive. Bounded queue + backpressure + safe shutdown.
//
// New ideas:
//   - the queue has a MAX size. When full: reject new requests (HTTP 429)
//     instead of growing forever and dying later.
//   - graceful shutdown (deploys happen): stop taking new work, drain what
//     is left, then exit. Zero loss.
//
// Run: java -ea S3_BoundedBackpressure.java   (-ea checks zero loss)
// Problem after this: one process has a CPU ceiling. More traffic just
//   means more rejects. We must run many worker instances -> S4.
// =============================================================================

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class S3_BoundedBackpressure {

    record EmailJob(int userId) {}

    public static void main(String[] args) throws Exception {
        // Bounded: capacity 10. This is the safety valve.
        BlockingQueue<EmailJob> queue = new ArrayBlockingQueue<>(10);
        AtomicInteger accepted = new AtomicInteger(), rejected = new AtomicInteger(), processed = new AtomicInteger();
        AtomicBoolean accepting = new AtomicBoolean(true);
        CountDownLatch workersDone = new CountDownLatch(3);

        // SERVICE: ApiService - if the queue is full, say "busy" (429) instead of waiting forever.
        class ApiService {
            String signup(int userId) {
                if (queue.offer(new EmailJob(userId))) { accepted.incrementAndGet(); return "202 accepted"; }
                rejected.incrementAndGet(); return "429 busy, retry later";  // backpressure to the caller
            }
        }
        ApiService api = new ApiService();

        // SERVICE: WorkerService pool. poll() so they can exit cleanly on shutdown.
        ExecutorService pool = Executors.newFixedThreadPool(3);
        for (int w = 0; w < 3; w++) pool.submit(() -> {
            try {
                while (true) {
                    EmailJob job = queue.poll(150, TimeUnit.MILLISECONDS);
                    if (job == null) { if (!accepting.get() && queue.isEmpty()) break; else continue; }
                    sleep(80);                                   // do the work
                    processed.incrementAndGet();
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            finally { workersDone.countDown(); }
        });

        // A burst of 200 signups much faster than 3 workers can process.
        for (int i = 1; i <= 200; i++) api.signup(i);
        System.out.println("burst of 200 -> accepted=" + accepted.get()
                + " rejected(429)=" + rejected.get() + " (queue never grew past 10)");

        // Graceful shutdown (a deploy): stop new work, drain the rest, then exit.
        accepting.set(false);
        boolean drained = workersDone.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        System.out.println("after graceful drain: processed=" + processed.get()
                + " (== accepted, so zero loss) drained=" + drained);
        assert processed.get() == accepted.get() : "LOSS! processed != accepted";

        System.out.println("\nWHAT BROKE: rejecting is safe but it means we are dropping real users."
                + " One process / 3 threads is the ceiling.");
        System.out.println("NEXT (S4): scale OUT - many worker instances + autoscale on backlog.");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
