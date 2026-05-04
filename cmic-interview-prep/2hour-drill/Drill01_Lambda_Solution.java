/*
 * Drill 1 — SOLUTION
 */

import java.util.*;
import java.util.stream.*;

public class Drill01_Lambda_Solution {
    public static void main(String[] args) {
        // 1: Runnable as lambda
        Runnable r = () -> System.out.println("running");
        r.run();

        // 2: filter even, collect, print
        List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5, 6);
        List<Integer> evens = nums.stream()
            .filter(n -> n % 2 == 0)
            .collect(Collectors.toList());
        System.out.println(evens);   // [2, 4, 6]
    }
}
