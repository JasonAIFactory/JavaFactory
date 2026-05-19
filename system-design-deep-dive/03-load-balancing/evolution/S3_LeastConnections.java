// =============================================================================
// STAGE 3 - Round robin vs least-connections, when servers are UNEVEN.
//
// Reality: servers are not identical (old box vs new box), and each server
// can only do a FEW requests at once - the rest WAIT in its queue. Round
// robin sends an equal COUNT to each, so the slow server's queue and tail
// latency explode while fast servers sit idle.
//
// least-connections = send the next request to the backend with the
// FEWEST in-flight requests. It naturally drains away from the slow one.
//
// Run:  java S3_LeastConnections.java
// Watch: avg AND tail (p99/max) latency. Round robin's tail blows up
//        because requests sit in the slow server's queue.
// =============================================================================

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class S3_LeastConnections {

    static class AppServer {
        final int id;
        final long workMs;                          // how slow THIS server is
        final Semaphore slots;                      // it can only do `slots` at once
        final AtomicInteger inflight = new AtomicInteger();   // queued + running
        AppServer(int id, long workMs, int concurrency) {
            this.id = id; this.workMs = workMs; this.slots = new Semaphore(concurrency);
        }
        // End-to-end latency = time spent WAITING for a slot + the work itself.
        long handle() {
            long start = System.currentTimeMillis();
            inflight.incrementAndGet();
            try {
                slots.acquire();                    // wait here if the server is full (the queue)
                try { sleep(workMs); } finally { slots.release(); }
                return System.currentTimeMillis() - start;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); return -1;
            } finally {
                inflight.decrementAndGet();
            }
        }
    }

    interface Strategy { AppServer pick(List<AppServer> s, AtomicInteger rr); }

    public static void main(String[] args) throws Exception {
        Strategy roundRobin = (s, rr) ->
                s.get((rr.getAndIncrement() & Integer.MAX_VALUE) % s.size());
        Strategy leastConn = (s, rr) ->
                s.stream().min(Comparator.comparingInt(x -> x.inflight.get())).orElseThrow();

        bench("round robin   ", roundRobin);
        bench("least-conn    ", leastConn);

        System.out.println("\nWHAT YOU SEE: same servers, same load. Round robin keeps feeding the");
        System.out.println("slow server an equal count -> its queue and TAIL latency blow up.");
        System.out.println("Least-connections steers around it, so p99/max stay low.");
        System.out.println("NEXT (S4): apps that keep SESSION state break when each request hits a different server.");
    }

    static void bench(String label, Strategy strat) throws InterruptedException {
        // server 0 is 5x slower; every server can do only 4 requests at once.
        List<AppServer> servers = List.of(
                new AppServer(0, 250, 4), new AppServer(1, 50, 4), new AppServer(2, 50, 4));
        AtomicInteger rr = new AtomicInteger();
        int n = 240;
        ExecutorService clients = Executors.newFixedThreadPool(120);
        CountDownLatch done = new CountDownLatch(n);
        List<Long> lat = Collections.synchronizedList(new ArrayList<>());
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            clients.submit(() -> {
                try { lat.add(strat.pick(servers, rr).handle()); }
                finally { done.countDown(); }
            });
        }
        done.await();
        long wall = System.currentTimeMillis() - t0;
        clients.shutdown();
        Collections.sort(lat);
        long avg = (long) lat.stream().mapToLong(Long::longValue).average().orElse(0);
        long p99 = lat.get((int) Math.ceil(lat.size() * 0.99) - 1);
        long max = lat.get(lat.size() - 1);
        System.out.println(label + " wall=" + wall + "ms  avg=" + avg
                + "ms  p99=" + p99 + "ms  max=" + max + "ms");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
