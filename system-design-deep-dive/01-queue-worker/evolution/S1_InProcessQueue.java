// =============================================================================
// STAGE 1 - First queue. In-process queue + one background worker.
//
// New component:
//   - Queue (in memory)     : ApiService drops work here and returns fast
//   - WorkerService (1)     : a background thread that drains the queue
//
// Win: response time drops from ~300ms to ~0ms.
// Problem: the queue lives in memory; a crash loses everything, and a
//          failed task is just dropped (no retry).
//
// Run: java S1_InProcessQueue.java
// =============================================================================

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class S1_InProcessQueue {

    record EmailJob(int userId) {}

    static class EmailGateway {
        void send(int userId) {
            if (userId % 9 == 0) throw new RuntimeException("transient send error for " + userId);
            sleep(300);
        }
    }

    static class ApiService {
        final BlockingQueue<EmailJob> queue;
        ApiService(BlockingQueue<EmailJob> q) { this.queue = q; }
        String signup(int userId) {
            queue.offer(new EmailJob(userId));   // just drop the work and return. Fast.
            return "OK user " + userId;          // user does NOT wait for the email
        }
    }

    public static void main(String[] args) throws Exception {
        BlockingQueue<EmailJob> queue = new LinkedBlockingQueue<>(); // in memory, unbounded
        EmailGateway email = new EmailGateway();
        ApiService api = new ApiService(queue);
        AtomicInteger sent = new AtomicInteger(), lost = new AtomicInteger();

        // SERVICE: WorkerService - one background thread (same process for now).
        Thread worker = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    EmailJob job = queue.take();
                    try {
                        email.send(job.userId());
                        sent.incrementAndGet();
                    } catch (Exception e) {
                        // FLAW: failed -> just dropped. No retry. The email never goes.
                        lost.incrementAndGet();
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        });
        worker.start();

        // 50 signups: measure user-facing latency now.
        long t0 = System.currentTimeMillis();
        for (int i = 1; i <= 50; i++) api.signup(i);
        System.out.println("50 signups returned in " + (System.currentTimeMillis() - t0)
                + " ms total (user no longer waits for email)");

        Thread.sleep(3000);                       // let the single worker drain
        worker.interrupt();
        System.out.println("emails sent=" + sent.get() + " lost (failed, dropped)=" + lost.get()
                + " | queue still holding=" + queue.size());

        System.out.println("\nWHAT BROKE: queue is in memory (a crash loses all of it);"
                + " a failed send is dropped with no retry; one worker is the speed limit.");
        System.out.println("NEXT (S2): never lose work -> retry + DLQ + idempotency, worker pool.");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
