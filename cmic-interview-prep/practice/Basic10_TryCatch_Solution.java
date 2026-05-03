public class Basic10_TryCatch_Solution {
    public static void main(String[] args) {
        int[] arr = {1, 2, 3};

        try {
            int x = arr[10];
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("caught: out of bounds");
        }

        System.out.println("program continues");
    }
}
