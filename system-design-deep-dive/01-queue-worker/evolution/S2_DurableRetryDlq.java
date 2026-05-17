// =============================================================================
// STAGE 2 - Never lose work. Worker pool + retry/backoff + DLQ + idempotency.
//
// New ideas:
//   - retry with growing delay + a little randomness (do not hammer a
//     downstream that is already struggling)
//   - DLQ: after N failures, set the message aside so one bad message
//     does not block the rest
//   - idempotency: the same job processed twice has the same effect
//
// Run: java S2_DurableRetryDlq.java
// Problem after this: the queue still has no size limit. If the downstream
//   stays down long, the queue grows until memory dies.
// =============================================================================

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;

public class S2_DurableRetryDlq {

    record EmailJob(int userId, int attempts) { EmailJob retry() { return new EmailJob(userId, attempts + 1); } }

    static final int MAX_ATTEMPTS = 4;

    // SERVICE: downstream that is DOWN for the first ~300ms, then recovers.
    // (Retries outlast the outage, so most jobs eventually succeed.)
    static class EmailGateway {
        final long upAt = System.currentTimeMillis() + 300;
        void send(int userId) {
            if (System.currentTimeMillis() < upAt) throw new RuntimeException("EmailGateway DOWN");
            if (userId == 7) throw new RuntimeException("user 7 is a poison message"); // never works
            sleep(60);
        }
    }

    public static void main(String[] args) throws Exception {
        BlockingQueue<EmailJob> queue = new LinkedBlockingQueue<>();
        BlockingQueue<EmailJob> dlq = new LinkedBlockingQueue<>();
        Set<Integer> done = ConcurrentHashMap.newKeySet();        // idempotency: ids already sent
        EmailGateway email = new EmailGateway();
        AtomicInteger sent = new AtomicInteger(), dead = new AtomicInteger();
        ScheduledExecutorService delay = Executors.newScheduledThreadPool(2);

        // SERVICE: WorkerService as a POOL (3 workers, competing on one queue).
        ExecutorService pool = Executors.newFixedThreadPool(3);
        for (int w = 0; w < 3; w++) pool.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                EmailJob job;
                try { job = queue.take(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                // duplicate -> skip. NOTE: this check-then-act has a tiny race
                // (two copies in flight at once). Real systems use a DB unique
                // key. Good enough to show the idea here.
                if (done.contains(job.userId())) continue;
                try {
                    email.send(job.userId());
                    done.add(job.userId());                       // record AFTER success
                    sent.incrementAndGet();
                } catch (Exception e) {
                    if (job.attempts() + 1 >= MAX_ATTEMPTS) {
                        dlq.offer(job); dead.incrementAndGet();    // give up -> set aside
                    } else {
                        long base = 80L << job.attempts();         // 80,160,320 ms (grows)
                        long jitter = ThreadLocalRandom.current().nextLong(-base/2, base/2+1);
                        EmailJob r = job.retry();
                        delay.schedule(() -> queue.offer(r), Math.max(0, base + jitter), TimeUnit.MILLISECONDS);
                    }
                }
            }
        });

        for (int i = 1; i <= 12; i++) queue.put(new EmailJob(i, 0));
        queue.put(new EmailJob(3, 0));                            // a duplicate -> idempotency must skip it

        Thread.sleep(3000);
        pool.shutdownNow(); delay.shutdownNow();
        System.out.println("sent=" + sent.get() + " dlq=" + dead.get()
                + "  -> ~11 sent (recovered after retries) + 1 in DLQ (user 7 poison) = 12 unique."
                + " Zero loss even though the downstream was down at first.");
        System.out.println("DLQ holds: " + new ArrayList<>(dlq) + "  (user 7 = the poison message, isolated)");

        System.out.println("\nWHAT BROKE: nothing is lost now, but the queue has NO size limit."
                + " If the downstream stays down longer, the queue grows until the process dies.");
        System.out.println("NEXT (S3): keep the system alive -> bounded queue + backpressure + safe shutdown.");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
