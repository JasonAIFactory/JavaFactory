/*
 * Problem 5 — Simple LRU Cache (Map-based)
 *
 * Implement a fixed-size cache that:
 *   - put(key, value): stores a value
 *   - get(key): returns value or null if not present
 *   - When capacity is exceeded, evicts the LEAST RECENTLY USED entry
 *
 * Why this matters: caching is everywhere in enterprise apps —
 * lookup tables, configuration, computed totals.
 *
 * Hint: Java's LinkedHashMap with accessOrder=true does this for you.
 *       Override removeEldestEntry to enforce capacity.
 *
 * Example:
 *   cache = new SimpleCache(2)
 *   cache.put("A", 1)        // {A=1}
 *   cache.put("B", 2)        // {A=1, B=2}
 *   cache.get("A")           // returns 1; A is now most recent
 *   cache.put("C", 3)        // {A=1, C=3}  -- B was evicted (least recent)
 *   cache.get("B")           // returns null
 */

import java.util.*;

public class Problem05_SimpleCache {

    static class SimpleCache<K, V> {
        private final int capacity;
        // TODO: declare your storage (LinkedHashMap recommended)

        public SimpleCache(int capacity) {
            this.capacity = capacity;
            // TODO: initialize storage
        }

        public V get(K key) {
            // TODO
            return null;
        }

        public void put(K key, V value) {
            // TODO
        }

        public int size() {
            // TODO
            return 0;
        }
    }

    public static void main(String[] args) {
        SimpleCache<String, Integer> cache = new SimpleCache<>(2);

        cache.put("A", 1);
        cache.put("B", 2);
        check("get A", 1, cache.get("A"));     // makes A most recent
        cache.put("C", 3);                      // should evict B
        check("get B (evicted)", null, cache.get("B"));
        check("get A (still here)", 1, cache.get("A"));
        check("get C", 3, cache.get("C"));
        check("size", 2, cache.size());

        // Update existing key
        cache.put("A", 99);
        check("update A", 99, cache.get("A"));
        check("size after update", 2, cache.size());
    }

    static void check(String label, Object expected, Object actual) {
        String status = Objects.equals(expected, actual) ? "PASS" : "FAIL";
        System.out.println(status + " | " + label + " expected=" + expected + " actual=" + actual);
    }
}
