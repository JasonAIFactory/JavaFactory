/*
 * Drill 8 — ConcurrentHashMap (thread-safe counter)
 *
 * 🎯 Goal:
 *   Count word occurrences across multiple threads.
 *   Use ConcurrentHashMap so the count is correct.
 *   Submit 10 tasks that each increment the same key.
 *
 * 🗣️ Say 10 times:
 *   "HashMap is not thread-safe. Multiple threads can corrupt it.
 *    ConcurrentHashMap is thread-safe. It uses bucket-level locking.
 *    Many threads can read and write at the same time without blocking everything.
 *    For a counter I use compute or merge."
 *   [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]
 *
 * Follow-up:
 *   Q: "Difference from Hashtable?"
 *   A: "Hashtable locks the whole map. ConcurrentHashMap locks only one bucket.
 *       ConcurrentHashMap is much faster."
 *
 *   Q: "Can I use HashMap with synchronized?"
 *   A: "Yes but every call locks the whole map. ConcurrentHashMap is better."
 */

import java.util.*;
import java.util.concurrent.*;

public class Drill08_ConcurrentHashMap {
    public static void main(String[] args) throws Exception {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(4);

        // TODO: submit 10 tasks that each do:
        //       map.merge("hello", 1, Integer::sum);


        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println(map.get("hello"));
        // expected: 10
    }
}
