/*
 * Drill 3 — SOLUTION
 */
public class Drill03_EqualsVsEquality_Solution {
    public static void main(String[] args) {
        String a = "hello";
        String b = "hello";
        String c = new String("hello");

        System.out.println(a == b);          // true (string pool)
        System.out.println(a == c);          // false (new object)
        System.out.println(a.equals(c));     // true (value match)

        String input = null;
        // 안전한 비교: 상수를 왼쪽에
        System.out.println("OK".equals(input));   // false, no NPE

        // 또는 Objects.equals 사용 (양쪽 null 안전)
        System.out.println(java.util.Objects.equals(input, "OK"));  // false
    }
}
