// =============================================================================
// STAGE 0 - One server. No load balancer. (Feel the pain.)
//
// Components:
//   - AppServer : one process that handles requests. It has a LIMITED number
//                 of worker threads (like a real Tomcat/Netty thread pool).
//   - Client    : fires many requests at the same time (a traffic spike).
//
// There is only ONE server. Every request must go to it.
//
// Run:  java S0_SingleServer.java
// Watch: how many requests are REJECTED when the spike is bigger than the
//        server can handle, and what happens when the server dies.
// =============================================================================

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class S0_SingleServer {

    // SERVICE: one app server. In real life this is a separate machine.
    static class AppServer {
        private final Semaphore slots;        // how many requests it can do AT ONCE
        volatile boolean alive = true;        // we will "kill" it later

        AppServer(int concurrency) { this.slots = new Semaphore(concurrency); }

        // Returns true if served, false if rejected (server too busy / dead).
        boolean handle() {
            if (!alive) return false;                 // dead server -> every request fails
            if (!slots.tryAcquire()) return false;    // no free worker -> reject (overload)
            try {
                sleep(50);                            // the request does ~50ms of work
                return true;
            } finally {
                slots.release();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        AppServer server = new AppServer(10);         // can do 10 requests at once

        // ---- 1) A traffic spike: 200 requests arrive almost together ----
        int n = 200;
        AtomicInteger ok = new AtomicInteger(), rejected = new AtomicInteger();
        ExecutorService clients = Executors.newFixedThreadPool(64);
        CountDownLatch done = new CountDownLatch(n);
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            clients.submit(() -> {
                try { if (server.handle()) ok.incrementAndGet(); else rejected.incrementAndGet(); }
                finally { done.countDown(); }
            });
        }
        done.await();
        System.out.println("Spike of " + n + " -> served=" + ok + " rejected=" + rejected
                + " in " + (System.currentTimeMillis() - t0) + " ms");

        // ---- 2) The single server dies. There is no one else. ----
        server.alive = false;
        boolean served = server.handle();
        System.out.println("Server died -> next request served? " + served + "  (TOTAL OUTAGE)");

        clients.shutdown();
        System.out.println("\nWHAT BROKE: one server has a hard ceiling (here ~10 at once),");
        System.out.println("so a spike is REJECTED; and if it dies, EVERYTHING is down (SPOF).");
        System.out.println("NEXT (S1): add more identical servers and spread requests across them.");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
