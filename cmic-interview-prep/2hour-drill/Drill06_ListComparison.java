/*
 * Drill 6 — ArrayList vs LinkedList
 *
 * 🎯 Goal:
 *   1. Make an ArrayList with 5 numbers and get the 3rd element by index
 *   2. Make a LinkedList with same numbers and add 99 at the head
 *   3. Print both lists
 *
 * 🗣️ Say 10 times:
 *   "ArrayList is backed by an array. Get by index is O(1). Good for read-heavy.
 *    LinkedList is doubly-linked. Add at head or tail is O(1). Good for write-heavy.
 *    In real code I almost always use ArrayList. It is the right default."
 *   [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]
 *
 * Follow-up:
 *   Q: "When to pick LinkedList?"
 *   A: "Rare. Maybe when I do a lot of insert/remove at the head. Otherwise ArrayList."
 *
 *   Q: "What is the cost of get(i) on LinkedList?"
 *   A: "O(N), because it walks node by node from the start."
 */

import java.util.*;

public class Drill06_ListComparison {
    public static void main(String[] args) {
        // TODO: ArrayList<Integer> with 1..5
        //       print list.get(2)


        // TODO: LinkedList<Integer> with same numbers
        //       add 99 at the head, print the list


        // expected output:
        // 3
        // [99, 1, 2, 3, 4, 5]
    }
}
