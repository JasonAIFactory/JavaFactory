// =============================================================================
// STAGE 5 - The LB is now the single point of failure. Make it HA + drain.
//
// Two real problems at scale:
//   (A) If there is ONE load balancer and it dies, everything is down again
//       - we just moved the SPOF. Fix: MANY LBs. Clients (via DNS/anycast)
//       fail over to another LB, so one LB dying loses ~nothing.
//   (B) Deploy/scale-down: if you yank a backend instantly, requests that
//       are IN-FLIGHT on it are DROPPED (connection reset). Fix: connection
//       DRAINING - mark it "draining" (no NEW requests), let in-flight
//       finish, THEN remove. Zero dropped.
//
// Run:  java S5_LbHaAndDraining.java
// Watch: single-LB outage vs HA failover; dropped requests with instant
//        removal vs zero with draining.
// =============================================================================

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class S5_LbHaAndDraining {

    static class Backend {
        final int id;
        enum State { LIVE, DRAINING, REMOVED }
        volatile State state = State.LIVE;
        final AtomicInteger inflight = new AtomicInteger();
        Backend(int id) { this.id = id; }
        boolean accepts() { return state == State.LIVE; }   // new traffic only if LIVE

        // Returns true if the request COMPLETED, false if it was DROPPED
        // because the backend was removed mid-flight.
        boolean handle() {
            inflight.incrementAndGet();
            try {
                for (int step = 0; step < 8; step++) {       // 8 x 10ms = ~80ms of work
                    if (state == State.REMOVED) return false; // yanked under us -> dropped
                    sleep(10);
                }
                return true;
            } finally {
                inflight.decrementAndGet();
            }
        }
    }

    static class LoadBalancer {
        final int id; volatile boolean up = true;
        final List<Backend> backends;
        final AtomicInteger rr = new AtomicInteger();
        LoadBalancer(int id, List<Backend> b) { this.id = id; this.backends = b; }
        boolean isUp() { return up; }
        Boolean route() {
            if (!up) return null;                           // null = this LB is dead, fail over
            List<Backend> live = backends.stream().filter(Backend::accepts).toList();
            if (live.isEmpty()) return Boolean.FALSE;
            return live.get((rr.getAndIncrement() & Integer.MAX_VALUE) % live.size()).handle();
        }
    }

    public static void main(String[] args) throws Exception {
        // ---- (A) SPOF vs HA failover ----
        List<Backend> backends = List.of(new Backend(0), new Backend(1), new Backend(2));
        LoadBalancer lb1 = new LoadBalancer(1, backends);
        LoadBalancer lb2 = new LoadBalancer(2, backends);

        System.out.println("-- Single LB (no backup) --");
        System.out.println("  before kill: " + hit(List.of(lb1), 50) + "/50 ok");
        lb1.up = false;
        System.out.println("  lb1 killed : " + hit(List.of(lb1), 50) + "/50 ok  <- TOTAL OUTAGE");

        System.out.println("-- HA: two LBs, client fails over (DNS/anycast) --");
        lb1.up = true;
        System.out.println("  both up    : " + hit(List.of(lb1, lb2), 50) + "/50 ok");
        lb1.up = false;
        System.out.println("  lb1 killed : " + hit(List.of(lb1, lb2), 50) + "/50 ok  <- failed over to lb2");
        lb1.up = true;

        // ---- (B) instant removal vs connection draining ----
        System.out.println("\n-- Removing backend 2 for a deploy --");
        System.out.println("  instant removal : dropped=" + removeBackend(backends.get(2), false) + "  <- in-flight killed");
        backends.get(2).state = Backend.State.LIVE;         // reset for the second run
        System.out.println("  with draining   : dropped=" + removeBackend(backends.get(2), true)  + "  <- zero loss");

        System.out.println("\nWHAT IMPROVED: many LBs => no single point of failure; draining =>");
        System.out.println("zero dropped requests on deploy/scale-down.");
        System.out.println("DONE: you now have the full real-world load balancing picture.");
        System.out.println("CAPSTONE: combine S2 health checks + S3 strategy + S4 sticky + S5 draining.");
    }

    // Client picks an LB; if it is down, FAIL OVER to the next one.
    static int hit(List<LoadBalancer> lbs, int n) throws InterruptedException {
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger lbRr = new AtomicInteger();
        ExecutorService c = Executors.newFixedThreadPool(16);
        CountDownLatch d = new CountDownLatch(n);
        for (int i = 0; i < n; i++) c.submit(() -> {
            try {
                for (int attempt = 0; attempt < lbs.size(); attempt++) {
                    LoadBalancer lb = lbs.get((lbRr.getAndIncrement() & Integer.MAX_VALUE) % lbs.size());
                    Boolean r = lb.route();
                    if (r == null) continue;                // LB dead -> try the next LB
                    if (Boolean.TRUE.equals(r)) ok.incrementAndGet();
                    break;
                }
            } finally { d.countDown(); }
        });
        d.await(); c.shutdown();
        return ok.get();
    }

    // Send 60 requests to `target`, then remove it. Returns how many in-flight
    // requests were DROPPED because the backend disappeared mid-request.
    static int removeBackend(Backend target, boolean drain) throws InterruptedException {
        AtomicInteger dropped = new AtomicInteger();
        ExecutorService c = Executors.newFixedThreadPool(32);
        CountDownLatch d = new CountDownLatch(60);
        for (int i = 0; i < 60; i++) c.submit(() -> {
            try {
                if (!target.accepts()) return;              // not routed here anymore (fine)
                if (!target.handle()) dropped.incrementAndGet();   // false = killed mid-flight
            } finally { d.countDown(); }
        });
        Thread.sleep(30);                                   // let many requests be in-flight
        if (drain) {
            target.state = Backend.State.DRAINING;          // stop NEW traffic to it
            while (target.inflight.get() > 0) Thread.sleep(5);  // wait for in-flight to finish
            target.state = Backend.State.REMOVED;           // now it is safe to remove
        } else {
            target.state = Backend.State.REMOVED;           // yank it now -> in-flight die
        }
        d.await(); c.shutdown();
        return dropped.get();
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
