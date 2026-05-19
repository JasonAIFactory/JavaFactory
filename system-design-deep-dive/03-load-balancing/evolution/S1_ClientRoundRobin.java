// =============================================================================
// STAGE 1 - Many servers. Client picks with round robin. (No health check.)
//
// We added more identical servers (horizontal scale-out). The CLIENT keeps
// a static list and rotates: server 0, 1, 2, 0, 1, 2 ... (round robin).
// This is basically "DNS round robin" / a dumb client-side list.
//
// Run:  java S1_ClientRoundRobin.java
// Watch: throughput is much better than S0. But when ONE server is down,
//        round robin is BLIND to that, so ~1/N of all requests still fail.
// =============================================================================

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class S1_ClientRoundRobin {

    static class AppServer {
        final int id;
        final Semaphore slots = new Semaphore(10);
        volatile boolean alive = true;
        AppServer(int id) { this.id = id; }
        boolean handle() {
            if (!alive) return false;
            if (!slots.tryAcquire()) return false;
            try { sleep(50); return true; } finally { slots.release(); }
        }
    }

    public static void main(String[] args) throws Exception {
        List<AppServer> servers = List.of(new AppServer(0), new AppServer(1), new AppServer(2));

        // Round robin index. Atomic so many client threads can rotate safely.
        AtomicInteger rr = new AtomicInteger();

        int n = 300;
        AtomicInteger ok = new AtomicInteger(), failed = new AtomicInteger();
        ExecutorService clients = Executors.newFixedThreadPool(64);

        // ---- 1) All servers healthy: spread load -> much higher throughput ----
        runSpike("all healthy", servers, rr, clients, n, ok, failed);

        // ---- 2) One server goes down. Round robin does NOT know. ----
        servers.get(1).alive = false;
        ok.set(0); failed.set(0);
        runSpike("server 1 DOWN", servers, rr, clients, n, ok, failed);
        System.out.println("  ~1/3 of requests failed because round robin still sent them to the dead server.");

        clients.shutdown();
        System.out.println("\nWHAT BROKE: round robin is blind to health. A dead backend keeps");
        System.out.println("getting its share of traffic -> users see errors.");
        System.out.println("NEXT (S2): a real load balancer that HEALTH-CHECKS and ejects dead backends.");
    }

    static void runSpike(String label, List<AppServer> servers, AtomicInteger rr,
                          ExecutorService clients, int n, AtomicInteger ok, AtomicInteger failed)
            throws InterruptedException {
        CountDownLatch done = new CountDownLatch(n);
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            clients.submit(() -> {
                try {
                    // pick next server by round robin (& mask keeps index non-negative)
                    AppServer s = servers.get((rr.getAndIncrement() & Integer.MAX_VALUE) % servers.size());
                    if (s.handle()) ok.incrementAndGet(); else failed.incrementAndGet();
                } finally { done.countDown(); }
            });
        }
        done.await();
        System.out.println(label + ": served=" + ok + " failed=" + failed
                + " in " + (System.currentTimeMillis() - t0) + " ms");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
