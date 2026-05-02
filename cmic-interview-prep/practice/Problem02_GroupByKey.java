/*
 * Problem 2 — Group By Key (Enterprise pattern)
 *
 * Given a list of CostEntry objects, return a map of projectId -> total amount.
 *
 * Why this matters: CMiC is a cost management ERP, so this pattern
 * (group/aggregate by key) shows up every day.
 *
 * Example input:
 *   [(P1, 100), (P2, 50), (P1, 200), (P3, 30), (P2, 70)]
 * Expected output:
 *   {P1=300.0, P2=120.0, P3=30.0}
 *
 * Hints:
 *   - HashMap with merge() is the cleanest traditional approach
 *   - Or use Stream.collect(Collectors.groupingBy(..., summingDouble(...)))
 */

import java.util.*;
import java.util.stream.*;

public class Problem02_GroupByKey {

    static class CostEntry {
        String projectId;
        double amount;
        CostEntry(String p, double a) { projectId = p; amount = a; }
    }

    public Map<String, Double> totalByProject(List<CostEntry> entries) {
        // TODO: implement (HashMap + merge, or Stream)
        return new HashMap<>();
    }

    public static void main(String[] args) {
        Problem02_GroupByKey p = new Problem02_GroupByKey();

        List<CostEntry> input = Arrays.asList(
            new CostEntry("P1", 100),
            new CostEntry("P2", 50),
            new CostEntry("P1", 200),
            new CostEntry("P3", 30),
            new CostEntry("P2", 70)
        );

        Map<String, Double> expected = new HashMap<>();
        expected.put("P1", 300.0);
        expected.put("P2", 120.0);
        expected.put("P3", 30.0);

        Map<String, Double> actual = p.totalByProject(input);

        boolean pass = expected.equals(actual);
        System.out.println((pass ? "PASS" : "FAIL") + " | expected=" + expected + " actual=" + actual);

        // Edge cases
        Map<String, Double> empty = p.totalByProject(new ArrayList<>());
        System.out.println((empty.isEmpty() ? "PASS" : "FAIL") + " | empty input -> empty map");
    }
}
