/*
 * Drill 7 — HashMap Basics
 *
 * 🎯 Goal:
 *   1. Put three entries
 *   2. Get one value
 *   3. Use getOrDefault for a missing key
 *   4. Iterate entries and print key=value
 *
 * 🗣️ Say 10 times:
 *   "HashMap stores key-value pairs.
 *    Get by key is O(1) average.
 *    Iteration order is not defined. Use LinkedHashMap for insertion order.
 *    HashMap is not thread-safe. For multi-thread, use ConcurrentHashMap."
 *   [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]
 *
 * Follow-up:
 *   Q: "What if the key is not in the map?"
 *   A: "get returns null. Use getOrDefault to give a fallback."
 *
 *   Q: "What if two keys hash to the same bucket?"
 *   A: "It becomes a linked list, then a tree if too many. Still O(1) average."
 */

import java.util.*;

public class Drill07_HashMapBasics {
    public static void main(String[] args) {
        // TODO: HashMap<String, Integer>, put apple=1, banana=2, cherry=3


        // TODO: print value of "banana"


        // TODO: print getOrDefault for "kiwi", default 0


        // TODO: iterate entrySet and print key=value


        // expected output:
        // 2
        // 0
        // (3 lines like apple=1, banana=2, cherry=3 — order may vary)
    }
}
