// =============================================================================
// STAGE 4 - Scale out. Broker as its own service + many worker instances
//           + an Autoscaler that watches the backlog.
//
// Now the queue is its OWN service (the SQS/Kafka position). Workers are
// SEPARATE instances pointing at the same broker, competing for messages.
// The Autoscaler adds/removes worker instances based on the backlog.
//
// The lesson: adding workers helps ONLY until the shared database is
// saturated. After that, more workers do nothing - they just wait on the
// DB. The bottleneck did not disappear; it moved.
//
// Run: java S4_ScaleOutAutoscaler.java
// =============================================================================

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class S4_ScaleOutAutoscaler {

    // SERVICE: the Broker (think SQS/Kafka). A separate process in real life.
    static class Broker {
        final BlockingQueue<Integer> q = new LinkedBlockingQueue<>();
        void publish(int job) { q.offer(job); }
        Integer poll() { return q.poll(); }
        int backlog() { return q.size(); }
    }

    // SERVICE: shared Database with only 6 connections. The real ceiling.
    static class Database {
        final Semaphore connections = new Semaphore(6);     // hard limit on parallel writes
        final AtomicInteger inUse = new AtomicInteger(), peak = new AtomicInteger(), writes = new AtomicInteger();
        void write() throws InterruptedException {
            connections.acquire();                          // BLOCKS if all 6 are busy (no dropping)
            int now = inUse.incrementAndGet();
            peak.accumulateAndGet(now, Math::max);
            try { Thread.sleep(20); writes.incrementAndGet(); }
            finally { inUse.decrementAndGet(); connections.release(); }
        }
    }

    // SERVICE: one stateless WorkerService instance. We can just run more.
    static class WorkerInstance {
        volatile boolean stop = false; final Thread t;
        WorkerInstance(int id, Broker b, Database d) {
            this.t = new Thread(() -> {
                while (!stop) {
                    Integer job = b.poll();
                    if (job == null) { sleep(2); continue; }
                    try { d.write(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                }
            }, "worker-" + id);
            t.start();
        }
        void shutdown() { stop = true; t.interrupt(); }
    }

    public static void main(String[] args) throws Exception {
        Broker broker = new Broker();
        Database db = new Database();
        java.util.List<WorkerInstance> workers = new java.util.concurrent.CopyOnWriteArrayList<>();
        AtomicInteger nextId = new AtomicInteger();
        for (int i = 0; i < 2; i++) workers.add(new WorkerInstance(nextId.incrementAndGet(), broker, db));

        // SERVICE: Autoscaler - backlog long => add workers; short => remove.
        ScheduledExecutorService autoscaler = Executors.newSingleThreadScheduledExecutor();
        autoscaler.scheduleAtFixedRate(() -> {
            int backlog = broker.backlog(), n = workers.size();
            if (backlog > 100 && n < 12) {
                workers.add(new WorkerInstance(nextId.incrementAndGet(), broker, db));
                System.out.println("  [autoscaler] backlog=" + backlog + " -> ADD worker (now " + workers.size()
                        + "), but DB peak concurrency stays " + db.peak.get());
            } else if (backlog == 0 && n > 2) {
                workers.remove(workers.size() - 1).shutdown();
            }
        }, 0, 300, TimeUnit.MILLISECONDS);

        int TOTAL = 1500;
        for (int i = 0; i < TOTAL; i++) broker.publish(i);
        long start = System.currentTimeMillis();
        while (broker.backlog() > 0 && System.currentTimeMillis() - start < 15000) sleep(50);
        long drainMs = System.currentTimeMillis() - start;

        autoscaler.shutdownNow();
        for (WorkerInstance w : workers) w.shutdown();

        System.out.println("\ndrained " + TOTAL + " jobs in " + drainMs + " ms");
        System.out.println("workers ended at " + workers.size() + ", but DB peak parallel writes = "
                + db.peak.get() + " (capped at 6). Throughput ~= " + (TOTAL * 1000L / drainMs) + "/s");
        System.out.println("Adding workers past ~6 did NOT speed it up - they just waited on the DB.");

        System.out.println("\nWHAT BROKE: scaling workers out hit the SHARED DB limit."
                + " The bottleneck moved from workers to the database. Also, with many"
                + " parallel workers, message ORDER is not preserved.");
        System.out.println("NEXT (S5): partition by key (order where it matters) + size workers to the downstream.");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
