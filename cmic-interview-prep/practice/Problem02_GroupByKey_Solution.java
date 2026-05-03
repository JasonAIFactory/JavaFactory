/*
 * Problem 2 — SOLUTION
 *
 * Two approaches: HashMap.merge (traditional) and Stream (declarative).
 * In production I prefer the merge version for clarity in mixed-skill teams.
 */

import java.util.*;
import java.util.stream.*;

public class Problem02_GroupByKey_Solution {

    static class CostEntry {
        String projectId;
        double amount;
        CostEntry(String p, double a) { projectId = p; amount = a; }
    }

    // Traditional: HashMap + merge
    public Map<String, Double> totalByProject(List<CostEntry> entries) {
        Map<String, Double> totals = new HashMap<>();
        for (CostEntry e : entries) {
            totals.merge(e.projectId, e.amount, Double::sum);
        }
        return totals;
    }

    // Stream version (alternative)
    public Map<String, Double> totalByProjectStream(List<CostEntry> entries) {
        return entries.stream()
            .collect(Collectors.groupingBy(
                e -> e.projectId,
                Collectors.summingDouble(e -> e.amount)
            ));
    }

    public static void main(String[] args) {
        Problem02_GroupByKey_Solution p = new Problem02_GroupByKey_Solution();

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

        Map<String, Double> r1 = p.totalByProject(input);
        Map<String, Double> r2 = p.totalByProjectStream(input);

        System.out.println((expected.equals(r1) ? "PASS" : "FAIL") + " | merge result=" + r1);
        System.out.println((expected.equals(r2) ? "PASS" : "FAIL") + " | stream result=" + r2);
        System.out.println((p.totalByProject(new ArrayList<>()).isEmpty() ? "PASS" : "FAIL") + " | empty input");
    }
}
