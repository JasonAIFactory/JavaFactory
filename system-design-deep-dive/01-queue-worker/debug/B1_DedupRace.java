// =============================================================================
// DEBUG EXERCISE - "Customers got charged TWICE. Rarely. Only under load.
//                   The dedup set is even a concurrent set. How?"
//
// This file has a REAL, planted concurrency bug - the kind that causes a
// double charge / double email in production. Your job is the practitioner
// loop: reproduce -> deterministic -> localize -> root cause -> fix ->
// regression test.
//
// Run:  java B1_DedupRace.java
// You will see (numbers vary every run = the whole point):
//   - some task ids were PROCESSED TWICE despite an idempotency guard
//
// Do NOT read DEBUG.md until you have a hypothesis. The fix is ONE idea
// and a few characters. Don't rewrite the worker.
// =============================================================================

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class B1_DedupRace {

    record Task(int id) {}

    public static void main(String[] args) throws Exception {
        // The idempotency guard. Note: it IS a thread-safe concurrent set.
        // (So "just use a concurrent set" is NOT the fix - read on.)
        Set<Integer> processed = ConcurrentHashMap.newKeySet();

        // How many times each id actually ran the side effect (the "charge").
        ConcurrentHashMap<Integer, AtomicInteger> charges = new ConcurrentHashMap<>();

        BlockingQueue<Task> queue = new LinkedBlockingQueue<>();
        int workers = 8;
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        AtomicBoolean accepting = new AtomicBoolean(true);
        CountDownLatch done = new CountDownLatch(workers);

        for (int w = 0; w < workers; w++) pool.submit(() -> {
            try {
                while (true) {
                    Task t = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (t == null) { if (!accepting.get()) break; else continue; }

                    // ---- the idempotency check (THE BUG IS IN THESE 4 LINES) ----
                    if (processed.contains(t.id())) {
                        continue;                         // already done -> skip
                    }
                    Thread.yield();                       // amplifies the real window (prod: just rarer)
                    chargeCustomer(charges, t.id());      // side effect (e.g. charge card)
                    processed.add(t.id());                // remember we did it
                    // -------------------------------------------------------------
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            finally { done.countDown(); }
        });

        // The network delivered each message a few times (at-least-once is the
        // DEFAULT in real queues). 200 ids, each enqueued 4 times. The copies
        // of one id arrive together (a real redelivery burst), so several
        // workers pick up the same id at the same moment.
        for (int id = 1; id <= 200; id++)
            for (int copy = 0; copy < 4; copy++) queue.put(new Task(id));

        accepting.set(false);
        done.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        int doubleCharged = 0, totalExtra = 0;
        for (var e : charges.entrySet()) {
            int n = e.getValue().get();
            if (n > 1) { doubleCharged++; totalExtra += (n - 1); }
        }
        System.out.println("unique ids                 = 200");
        System.out.println("ids charged MORE THAN ONCE = " + doubleCharged + "   <-- should be 0");
        System.out.println("extra (duplicate) charges  = " + totalExtra + "   <-- should be 0");
        System.out.println();
        System.out.println("Non-zero and different every run -> a race in the dedup guard,");
        System.out.println("even though the set is thread-safe. See DEBUG.md for the method.");
    }

    // The expensive, NON-idempotent side effect we must do at most once.
    static void chargeCustomer(ConcurrentHashMap<Integer, AtomicInteger> charges, int id) {
        charges.computeIfAbsent(id, k -> new AtomicInteger()).incrementAndGet();
        // a tiny bit of work widens the race window so the bug is reliably
        // visible for learning (in prod the window is just as real, rarer).
        for (int i = 0; i < 50; i++) Integer.toString(i).hashCode();
    }
}
