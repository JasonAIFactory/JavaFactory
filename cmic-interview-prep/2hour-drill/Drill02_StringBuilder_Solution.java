/*
 * Drill 2 — SOLUTION
 */
public class Drill02_StringBuilder_Solution {
    public static void main(String[] args) {
        int[] arr = {1, 2, 3, 4, 5};

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            sb.append(arr[i]);
            if (i < arr.length - 1) sb.append(",");
        }

        System.out.println(sb.toString());   // 1,2,3,4,5
    }
}
