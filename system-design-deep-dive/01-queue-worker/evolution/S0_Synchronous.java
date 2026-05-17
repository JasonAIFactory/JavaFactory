// =============================================================================
// STAGE 0 - No queue. Synchronous monolith. (Feel the pain.)
//
// Components:
//   - ApiService    : receives a user request, must answer fast
//   - EmailGateway  : the downstream. Slow (~300ms) and sometimes down.
// Here ApiService calls EmailGateway INLINE and waits. Nothing else exists.
//
// Run: java S0_Synchronous.java
// Watch: response time per request, and what happens during a spike and
//        when the downstream is down.
// =============================================================================

import java.util.concurrent.*;

public class S0_Synchronous {

    // SERVICE: downstream dependency. In real life this is a separate server.
    static class EmailGateway {
        volatile boolean down = false;                 // we will turn this off for a while
        void send(int userId) {
            if (down) throw new RuntimeException("EmailGateway is DOWN");
            sleep(300);                                // real work takes time
        }
    }

    // SERVICE: the web server the user hits.
    static class ApiService {
        final EmailGateway email;
        ApiService(EmailGateway e) { this.email = e; }

        // The user waits for THIS whole method to return.
        String signup(int userId) {
            // ... save user (fast, ignore) ...
            email.send(userId);                        // <-- inline, blocking. User waits here.
            return "OK user " + userId;
        }
    }

    public static void main(String[] args) throws Exception {
        EmailGateway email = new EmailGateway();
        ApiService api = new ApiService(email);

        // 1) One normal request: measure how long the user waits.
        long t0 = System.currentTimeMillis();
        api.signup(1);
        System.out.println("1 request took " + (System.currentTimeMillis() - t0) + " ms (user waited this long)");

        // 2) A traffic spike: 50 users at once, only 10 threads (like a real server pool).
        ExecutorService server = Executors.newFixedThreadPool(10);
        long s0 = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(50);
        for (int i = 0; i < 50; i++) {
            final int id = i;
            server.submit(() -> { try { api.signup(id); } catch (Exception ignored) {} finally { latch.countDown(); } });
        }
        latch.await();
        System.out.println("50 requests under a 10-thread server took "
                + (System.currentTimeMillis() - s0) + " ms total (users queued behind each other)");

        // 3) Downstream goes down for a bit: user-facing requests FAIL too.
        email.down = true;
        try {
            api.signup(999);
        } catch (Exception e) {
            System.out.println("Downstream down -> the USER request also failed: " + e.getMessage());
        }
        email.down = false;
        server.shutdown();

        System.out.println("\nWHAT BROKE: user waits for slow work; a spike queues users behind"
                + " each other; a downstream outage fails the user too.");
        System.out.println("NEXT (S1): do the work later, somewhere else -> introduce a queue.");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
