/*
 * Problem 1 — Codility Task 2 (실제 출제 문제)
 *
 * Given two integers X, Y and a non-empty array A of N integers,
 * return the largest P (0 <= P < N) such that the prefix A[0..P]
 * contains the same number of occurrences of X and Y.
 * If no such P exists, return -1.
 *
 * Constraints:
 *   - N is in [1..100,000]
 *   - X, Y are in [1..1,000,000,000]
 *   - each element of A is in [1..1,000,000,000]
 *   - X may equal Y (edge case!)
 *
 * Examples:
 *   X=7, Y=42, A=[6,42,11,7,1,42] -> 4
 *   X=6, Y=13, A=[13,13,1,6]      -> -1
 *   X=100, Y=63, A=[100,63,1,6,2,13] -> 5
 *   X=5, Y=5, A=[5,5,5]           -> 2  (X==Y edge case)
 *
 * Hint: scan once with two counters. Be careful with else.
 */
public class Problem01_PrefixCount {

    public int solution(int X, int Y, int[] A) {
        // TODO: implement
        return -1;
    }

    public static void main(String[] args) {
        Problem01_PrefixCount p = new Problem01_PrefixCount();

        test(p, 7, 42, new int[]{6, 42, 11, 7, 1, 42}, 4);
        test(p, 6, 13, new int[]{13, 13, 1, 6}, -1);
        test(p, 100, 63, new int[]{100, 63, 1, 6, 2, 13}, 5);
        test(p, 5, 5, new int[]{5, 5, 5}, 2);              // X == Y
        test(p, 1, 2, new int[]{3, 3, 3}, 0);              // neither -> 0=0 at i=0
        test(p, 1, 2, new int[]{1, 1, 2}, 2);              // counts equal at end
    }

    static void test(Problem01_PrefixCount p, int X, int Y, int[] A, int expected) {
        int actual = p.solution(X, Y, A);
        String status = (actual == expected) ? "PASS" : "FAIL";
        System.out.println(status + " | X=" + X + " Y=" + Y
                + " expected=" + expected + " actual=" + actual);
    }
}
