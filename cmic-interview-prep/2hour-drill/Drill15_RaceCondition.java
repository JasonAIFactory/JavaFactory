/*
 * Drill 15 — Race Condition + synchronized
 *
 * 🎯 Goal:
 *   Show a counter that breaks when many threads increment without sync.
 *   Then fix it with synchronized.
 *
 * 🗣️ Say 10 times:
 *   "Race condition is when threads access shared data and the result depends on timing.
 *    The counter plus-plus is three steps: read, add, write.
 *    Two threads can read the same value and overwrite each other.
 *    The fix is synchronized, or AtomicInteger which is lock-free.
 *    AtomicInteger is faster for simple counters."
 *   [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]
 *
 * Follow-up:
 *   Q: "Difference between synchronized and AtomicInteger?"
 *   A: "Synchronized uses a lock. AtomicInteger uses CPU compare-and-swap. No lock.
 *       For simple counters, AtomicInteger is faster."
 *
 *   Q: "What about volatile?"
 *   A: "Volatile gives visibility but NOT atomicity. count++ is still broken with only volatile."
 */

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Drill15_RaceCondition {

    static int unsafeCount = 0;
    static final Object lock = new Object();
    static int safeCount = 0;
    static AtomicInteger atomicCount = new AtomicInteger();

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(10);

        // TODO: submit 1000 tasks. Each task does:
        //         unsafeCount++;
        //         synchronized (lock) { safeCount++; }
        //         atomicCount.incrementAndGet();


        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("unsafe = " + unsafeCount);
        System.out.println("safe   = " + safeCount);
        System.out.println("atomic = " + atomicCount.get());

        // expected:
        // unsafe might be < 1000 (race)
        // safe   == 1000
        // atomic == 1000
    }
}
