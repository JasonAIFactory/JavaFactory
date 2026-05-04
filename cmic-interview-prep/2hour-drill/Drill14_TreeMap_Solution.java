/*
 * Drill 14 — SOLUTION
 */

import java.util.*;

public class Drill14_TreeMap_Solution {
    public static void main(String[] args) {
        TreeMap<String, Integer> tm = new TreeMap<>();
        tm.put("banana", 2);
        tm.put("apple", 1);
        tm.put("cherry", 3);

        // Iteration is sorted by key automatically
        for (Map.Entry<String, Integer> e : tm.entrySet()) {
            System.out.println(e.getKey() + "=" + e.getValue());
        }

        System.out.println(tm.firstKey());   // apple
        System.out.println(tm.lastKey());    // cherry
    }
}
