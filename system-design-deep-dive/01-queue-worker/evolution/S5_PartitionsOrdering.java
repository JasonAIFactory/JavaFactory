// =============================================================================
// STAGE 5 - The realistic shape. Broker with partitions for ordering.
//
// Idea: some work MUST be ordered (same bank account: deposit then
// withdraw). Many parallel workers break order. Fix: split the broker
// into PARTITIONS. Route by key (accountId) so the same account always
// goes to the same partition, and ONE worker owns each partition. Inside
// a partition order is kept; different accounts run in parallel.
//
// Also shown: a HOT partition (one popular key floods one partition) and
// why the key choice matters. We stop here on purpose - deeper is Kafka
// itself (module 09).
//
// Run: java S5_PartitionsOrdering.java
// =============================================================================

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class S5_PartitionsOrdering {

    record Event(int accountId, String op, int seq) {}   // seq = the order it was produced

    public static void main(String[] args) throws Exception {
        int PARTITIONS = 4;

        // SERVICE: Broker split into partitions. Same key -> same partition.
        @SuppressWarnings("unchecked")
        BlockingQueue<Event>[] partitions = new BlockingQueue[PARTITIONS];
        for (int i = 0; i < PARTITIONS; i++) partitions[i] = new LinkedBlockingQueue<>();

        // record the processing order PER account, to prove ordering is kept
        Map<Integer, List<Integer>> processedSeq = new ConcurrentHashMap<>();
        AtomicIntegerArray partitionCount = new AtomicIntegerArray(PARTITIONS);

        // SERVICE: one WorkerService per partition (this is what guarantees order).
        ExecutorService pool = Executors.newFixedThreadPool(PARTITIONS);
        CountDownLatch done = new CountDownLatch(PARTITIONS);
        for (int p = 0; p < PARTITIONS; p++) {
            final int pid = p;
            pool.submit(() -> {
                try {
                    while (true) {
                        Event e = partitions[pid].poll(300, TimeUnit.MILLISECONDS);
                        if (e == null) break;                       // partition drained
                        sleep(10);                                   // process (in order, single worker)
                        processedSeq.computeIfAbsent(e.accountId(), k -> new ArrayList<>()).add(e.seq());
                        partitionCount.incrementAndGet(pid);
                    }
                } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                finally { done.countDown(); }
            });
        }

        // Producer: accounts 0..9, 6 ordered events each. Route by key.
        // Account 0 is a "hot" key: it gets 10x more events -> hot partition.
        int seq = 0;
        for (int round = 0; round < 6; round++)
            for (int acc = 0; acc < 10; acc++)
                partitions[acc % PARTITIONS].put(new Event(acc, "op", seq++));
        for (int extra = 0; extra < 50; extra++)                     // flood account 0
            partitions[0 % PARTITIONS].put(new Event(0, "hot", seq++));

        done.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        // 1) Ordering check: for each account, the seq numbers must be increasing.
        boolean ordered = true;
        for (var entry : processedSeq.entrySet()) {
            List<Integer> s = entry.getValue();
            for (int i = 1; i < s.size(); i++) if (s.get(i) < s.get(i - 1)) ordered = false;
        }
        System.out.println("per-account ordering preserved = " + ordered
                + "  (same account always went to one partition, one worker, in order)");

        // 2) Hot partition: partition that owns account 0 did far more work.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < PARTITIONS; i++) sb.append("P").append(i).append("=").append(partitionCount.get(i)).append(" ");
        System.out.println("work per partition: " + sb + " <- one is HOT (account 0 flooded it)");

        System.out.println("\nTAKEAWAY: partitioning buys per-key order AND parallelism between keys."
                + " Throughput = partitions x per-partition speed.");
        System.out.println("COST: a skewed key makes one partition hot -> key choice is a real design decision.");
        System.out.println("\nThis is the realistic shape. Going deeper = Kafka itself (module 09),"
                + " exactly-once/Saga (modules 10-11).");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
