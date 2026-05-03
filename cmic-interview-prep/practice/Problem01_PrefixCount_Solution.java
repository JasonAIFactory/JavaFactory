/*
 * Problem 1 — SOLUTION
 *
 * Bug: original code used `else if`, so when X == Y, only nX was incremented.
 * Fix: remove `else` so both counters grow independently.
 *
 * Time: O(N)   Space: O(1)
 */
public class Problem01_PrefixCount_Solution {

    public int solution(int X, int Y, int[] A) {
        int nX = 0, nY = 0, result = -1;
        for (int i = 0; i < A.length; i++) {
            if (A[i] == X) nX++;
            if (A[i] == Y) nY++;
            if (nX == nY) result = i;
        }
        return result;
    }

    public static void main(String[] args) {
        Problem01_PrefixCount_Solution p = new Problem01_PrefixCount_Solution();

        test(p, 7, 42, new int[]{6, 42, 11, 7, 1, 42}, 4);
        test(p, 6, 13, new int[]{13, 13, 1, 6}, -1);
        test(p, 100, 63, new int[]{100, 63, 1, 6, 2, 13}, 5);
        test(p, 5, 5, new int[]{5, 5, 5}, 2);
        test(p, 1, 2, new int[]{3, 3, 3}, 0);
        test(p, 1, 2, new int[]{1, 1, 2}, 2);
    }

    static void test(Problem01_PrefixCount_Solution p, int X, int Y, int[] A, int expected) {
        int actual = p.solution(X, Y, A);
        String status = (actual == expected) ? "PASS" : "FAIL";
        System.out.println(status + " | X=" + X + " Y=" + Y
                + " expected=" + expected + " actual=" + actual);
    }
}
