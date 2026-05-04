/*
 * Drill 1 — Lambda + Stream
 *
 * 🎯 Goal:
 *   1. Use a lambda for Runnable
 *   2. Filter even numbers from a list and collect to a new list using Stream
 *
 * 🗣️ Say 10 times after coding:
 *   "Java 8 added lambda for short functions and Stream for data pipelines.
 *    Lambda makes the code shorter than anonymous class.
 *    Stream is lazy until collect or forEach."
 *   [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]
 *
 * Follow-up:
 *   Q: "What is the benefit of Stream?"
 *   A: "Less code. Easy to read. Can run in parallel with parallelStream."
 *
 *   Q: "Is Stream a data structure?"
 *   A: "No. Stream is a pipeline. The data is in the collection."
 */

import java.util.*;
import java.util.stream.*;

public class Drill01_Lambda {
    public static void main(String[] args) {
        // TODO 1: Runnable as lambda — print "running"
        Runnable r = null;
        r.run();


        // TODO 2: Given list, filter even numbers, collect, print
        List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5, 6);

        // expected output: [2, 4, 6]
    }
}

