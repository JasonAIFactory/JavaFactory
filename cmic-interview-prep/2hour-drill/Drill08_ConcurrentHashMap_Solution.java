/*
 * Drill 8 — SOLUTION
 */

import java.util.*;
import java.util.concurrent.*;

public class Drill08_ConcurrentHashMap_Solution {
    public static void main(String[] args) throws Exception {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(4);

        for (int i = 0; i < 10; i++) {
            pool.submit(() -> map.merge("hello", 1, Integer::sum));
        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println(map.get("hello"));   // 10
    }
}
