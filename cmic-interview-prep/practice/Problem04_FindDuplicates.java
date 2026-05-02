/*
 * Problem 4 — Find Duplicate Entries
 *
 * Given a list of strings (e.g., invoice numbers), return a list of
 * values that appear more than once. The order of the result follows
 * the first time the duplicate was detected.
 *
 * Example:
 *   input  = ["INV-1", "INV-2", "INV-1", "INV-3", "INV-2", "INV-1"]
 *   output = ["INV-1", "INV-2"]   (INV-1 detected as dup at index 2,
 *                                   INV-2 detected as dup at index 4)
 *
 * Why this matters: detecting duplicate invoices, duplicate cost entries,
 * or duplicate part numbers is a common ERP integrity check.
 *
 * Hints:
 *   - One pass with a "seen" Set and a "duplicates" Set (and a result list)
 *   - Or use a HashMap<String, Integer> to count, then collect those with count > 1
 */

import java.util.*;

public class Problem04_FindDuplicates {

    public List<String> findDuplicates(List<String> input) {
        // TODO: implement
        return new ArrayList<>();
    }

    public static void main(String[] args) {
        Problem04_FindDuplicates p = new Problem04_FindDuplicates();

        // Test 1: typical
        List<String> result1 = p.findDuplicates(Arrays.asList(
            "INV-1", "INV-2", "INV-1", "INV-3", "INV-2", "INV-1"
        ));
        check("test1", Arrays.asList("INV-1", "INV-2"), result1);

        // Test 2: no duplicates
        List<String> result2 = p.findDuplicates(Arrays.asList("A", "B", "C"));
        check("no dups", new ArrayList<>(), result2);

        // Test 3: all same
        List<String> result3 = p.findDuplicates(Arrays.asList("X", "X", "X"));
        check("all same", Arrays.asList("X"), result3);

        // Test 4: empty
        List<String> result4 = p.findDuplicates(new ArrayList<>());
        check("empty", new ArrayList<>(), result4);
    }

    static void check(String label, List<String> expected, List<String> actual) {
        String status = expected.equals(actual) ? "PASS" : "FAIL";
        System.out.println(status + " | " + label + " expected=" + expected + " actual=" + actual);
    }
}
