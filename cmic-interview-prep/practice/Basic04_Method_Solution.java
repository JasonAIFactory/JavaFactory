public class Basic04_Method_Solution {

    static int add(int a, int b) {
        return a + b;
    }

    static boolean isEven(int n) {
        return n % 2 == 0;
    }

    public static void main(String[] args) {
        System.out.println(add(3, 4));      // 7
        System.out.println(isEven(10));     // true
    }
}
