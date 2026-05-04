/*
 * Drill 14 — TreeMap (sorted by key)
 *
 * 🎯 Goal:
 *   Put fruits in a TreeMap in random order. Iterate.
 *   The output will be alphabetical because TreeMap is sorted.
 *
 * 🗣️ Say 10 times:
 *   "TreeMap is based on a Red-Black tree.
 *    Keys are sorted. Operations are O(log N).
 *    HashMap is O(1) average but order is undefined.
 *    I use TreeMap when I need sorted iteration or range queries."
 *   [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]
 *
 * Follow-up:
 *   Q: "When TreeMap over HashMap?"
 *   A: "When I need sorted order, or queries like firstKey, lastKey, headMap, tailMap."
 *
 *   Q: "TreeSet?"
 *   A: "Same idea but only keys. A sorted set. Useful for unique sorted values."
 */

import java.util.*;

public class Drill14_TreeMap {
    public static void main(String[] args) {
        // TODO: TreeMap<String, Integer>, put banana=2, apple=1, cherry=3 (in this order)


        // TODO: iterate and print key=value


        // TODO: print firstKey() and lastKey()


        // expected output:
        // apple=1
        // banana=2
        // cherry=3
        // apple
        // cherry
    }
}
