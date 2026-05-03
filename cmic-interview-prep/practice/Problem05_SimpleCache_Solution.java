/*
 * Problem 5 — SOLUTION
 *
 * LinkedHashMap with accessOrder=true tracks LRU automatically.
 * Override removeEldestEntry to evict when size exceeds capacity.
 * This is the standard Java LRU cache pattern.
 */

import java.util.*;

public class Problem05_SimpleCache_Solution {

    static class SimpleCache<K, V> {
        private final int capacity;
        private final LinkedHashMap<K, V> map;

        public SimpleCache(int capacity) {
            this.capacity = capacity;
            this.map = new LinkedHashMap<K, V>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                    return size() > SimpleCache.this.capacity;
                }
            };
        }

        public V get(K key) { return map.get(key); }
        public void put(K key, V value) { map.put(key, value); }
        public int size() { return map.size(); }
    }

    public static void main(String[] args) {
        SimpleCache<String, Integer> cache = new SimpleCache<>(2);

        cache.put("A", 1);
        cache.put("B", 2);
        check("get A", 1, cache.get("A"));
        cache.put("C", 3);
        check("get B (evicted)", null, cache.get("B"));
        check("get A (still here)", 1, cache.get("A"));
        check("get C", 3, cache.get("C"));
        check("size", 2, cache.size());

        cache.put("A", 99);
        check("update A", 99, cache.get("A"));
        check("size after update", 2, cache.size());
    }

    static void check(String label, Object expected, Object actual) {
        System.out.println((Objects.equals(expected, actual) ? "PASS" : "FAIL")
            + " | " + label + " expected=" + expected + " actual=" + actual);
    }
}
