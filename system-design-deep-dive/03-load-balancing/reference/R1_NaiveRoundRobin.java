// =============================================================================
// R1 - The simplest load balancer: round robin (flaws left in ON PURPOSE)
//
// Goal: see the "skeleton" of a load balancer, and SEE why it is not enough.
// Run:  java R1_NaiveRoundRobin.java
//
// Read the code and answer for yourself:
//   - If a backend is DOWN, does round robin still send to it?  (yes -> errors)
//   - If one backend is SLOW, does round robin still send 1/N?   (yes -> pile-up)
//   - The index keeps growing forever - is that safe?           (must mask sign)
// These 3 flaws are fixed in R2 and R3. Compare them.
// =============================================================================

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class R1_NaiveRoundRobin {

    // A backend server. In real life this is a separate machine + IP:port.
    static class Backend {
        final int id;
        final long workMs;                        // how long this backend takes
        volatile boolean alive = true;            // R1 NEVER reads this -> flaw #1
        final AtomicInteger handled = new AtomicInteger();
        Backend(int id, long workMs) { this.id = id; this.workMs = workMs; }

        boolean handle() {
            if (!alive) return false;             // a dead backend just errors out
            handled.incrementAndGet();
            sleep(workMs);
            return true;
        }
    }

    // The load balancer. It only knows "the list" and a rotating counter.
    static class RoundRobinLB {
        private final List<Backend> backends;
        private final AtomicInteger cursor = new AtomicInteger();

        RoundRobinLB(List<Backend> backends) { this.backends = backends; }

        Backend next() {
            // getAndIncrement() overflows to negative after ~2 billion calls.
            // & Integer.MAX_VALUE clears the sign bit so the index stays >= 0.
            // (A real subtle production bug if you forget this.)
            int i = (cursor.getAndIncrement() & Integer.MAX_VALUE) % backends.size();
            return backends.get(i);               // flaw #1: no health filter
        }                                         // flaw #2: ignores load/speed
    }

    public static void main(String[] args) throws Exception {
        // backend 2 is 6x slower than the others (uneven fleet).
        List<Backend> backends = List.of(
                new Backend(0, 30), new Backend(1, 30), new Backend(2, 180));
        RoundRobinLB lb = new RoundRobinLB(backends);

        // ---- 1) All healthy: count per backend is even (good) but ... ----
        fire(lb, 90);
        System.out.println("All healthy, requests per backend: "
                + backends.stream().map(b -> "b" + b.id + "=" + b.handled.get()).toList());
        System.out.println("  -> equal COUNT, but b2 is 6x slower so users on b2 wait much longer.");

        // ---- 2) Kill backend 1. Round robin keeps sending to it. ----
        backends.forEach(b -> b.handled.set(0));
        backends.get(1).alive = false;
        AtomicInteger ok = new AtomicInteger(), fail = new AtomicInteger();
        fireCount(lb, 90, ok, fail);
        System.out.println("Backend 1 DOWN -> ok=" + ok + " fail=" + fail
                + "  (~1/3 still routed to the dead backend)");

        System.out.println("\nWHY THIS IS NOT ENOUGH:");
        System.out.println("  flaw #1: no health check -> dead backend keeps getting traffic.");
        System.out.println("  flaw #2: blind to load -> slow backend gets the same count.");
        System.out.println("  -> R2 adds health checks + least-connections.");
    }

    static void fire(RoundRobinLB lb, int n) throws InterruptedException {
        ExecutorService c = Executors.newFixedThreadPool(32);
        CountDownLatch d = new CountDownLatch(n);
        for (int i = 0; i < n; i++) c.submit(() -> { try { lb.next().handle(); } finally { d.countDown(); } });
        d.await(); c.shutdown();
    }

    static void fireCount(RoundRobinLB lb, int n, AtomicInteger ok, AtomicInteger fail)
            throws InterruptedException {
        ExecutorService c = Executors.newFixedThreadPool(32);
        CountDownLatch d = new CountDownLatch(n);
        for (int i = 0; i < n; i++) c.submit(() -> {
            try { if (lb.next().handle()) ok.incrementAndGet(); else fail.incrementAndGet(); }
            finally { d.countDown(); }
        });
        d.await(); c.shutdown();
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
