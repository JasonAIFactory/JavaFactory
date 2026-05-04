/*
 * Drill 15 — SOLUTION
 */

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Drill15_RaceCondition_Solution {

    static int unsafeCount = 0;
    static final Object lock = new Object();
    static int safeCount = 0;
    static AtomicInteger atomicCount = new AtomicInteger();

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 1000; i++) {
            pool.submit(() -> {
                unsafeCount++;                          // race
                synchronized (lock) { safeCount++; }   // safe via lock
                atomicCount.incrementAndGet();         // safe lock-free
            });
        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("unsafe = " + unsafeCount);   // often < 1000
        System.out.println("safe   = " + safeCount);     // 1000
        System.out.println("atomic = " + atomicCount.get()); // 1000
    }
}
