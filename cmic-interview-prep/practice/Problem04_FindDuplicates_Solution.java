/*
 * Problem 4 — SOLUTION
 *
 * Two sets:
 *   - "seen" tracks what we have observed.
 *   - "reported" prevents adding the same duplicate twice.
 * Single pass, O(N) time.
 */

import java.util.*;

public class Problem04_FindDuplicates_Solution {

    public List<String> findDuplicates(List<String> input) {
        Set<String> seen = new HashSet<>();
        Set<String> reported = new HashSet<>();
        List<String> result = new ArrayList<>();
        for (String s : input) {
            if (!seen.add(s) && reported.add(s)) {
                result.add(s);
            }
        }
        return result;
    }

    public static void main(String[] args) {
        Problem04_FindDuplicates_Solution p = new Problem04_FindDuplicates_Solution();

        check("test1",
            Arrays.asList("INV-1", "INV-2"),
            p.findDuplicates(Arrays.asList("INV-1", "INV-2", "INV-1", "INV-3", "INV-2", "INV-1")));

        check("no dups",
            new ArrayList<>(),
            p.findDuplicates(Arrays.asList("A", "B", "C")));

        check("all same",
            Arrays.asList("X"),
            p.findDuplicates(Arrays.asList("X", "X", "X")));

        check("empty",
            new ArrayList<>(),
            p.findDuplicates(new ArrayList<>()));
    }

    static void check(String label, List<String> expected, List<String> actual) {
        System.out.println((expected.equals(actual) ? "PASS" : "FAIL")
            + " | " + label + " expected=" + expected + " actual=" + actual);
    }
}
