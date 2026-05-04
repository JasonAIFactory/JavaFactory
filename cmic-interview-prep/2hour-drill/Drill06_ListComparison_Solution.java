/*
 * Drill 6 — SOLUTION
 */

import java.util.*;

public class Drill06_ListComparison_Solution {
    public static void main(String[] args) {
        // ArrayList — fast index access
        ArrayList<Integer> al = new ArrayList<>();
        for (int i = 1; i <= 5; i++) al.add(i);
        System.out.println(al.get(2));   // 3 (O(1))

        // LinkedList — fast head insert
        LinkedList<Integer> ll = new LinkedList<>();
        for (int i = 1; i <= 5; i++) ll.add(i);
        ll.addFirst(99);                 // O(1) at head
        System.out.println(ll);          // [99, 1, 2, 3, 4, 5]
    }
}
