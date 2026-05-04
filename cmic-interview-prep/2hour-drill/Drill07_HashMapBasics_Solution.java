/*
 * Drill 7 — SOLUTION
 */

import java.util.*;

public class Drill07_HashMapBasics_Solution {
    public static void main(String[] args) {
        Map<String, Integer> map = new HashMap<>();
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);

        System.out.println(map.get("banana"));               // 2
        System.out.println(map.getOrDefault("kiwi", 0));     // 0

        for (Map.Entry<String, Integer> e : map.entrySet()) {
            System.out.println(e.getKey() + "=" + e.getValue());
        }
    }
}
