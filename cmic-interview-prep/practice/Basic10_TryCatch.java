/*
 * Basic 10 — Try / Catch
 *
 * Goal: catch a runtime exception so the program does not crash.
 *
 * Tasks:
 *   1. Inside try, access arr[10] of an int[] of length 3
 *      (this throws ArrayIndexOutOfBoundsException)
 *   2. Catch that exception and print "caught: out of bounds"
 *   3. After the try-catch, print "program continues"
 */
public class Basic10_TryCatch {
    public static void main(String[] args) {
        int[] arr = {1, 2, 3};

        // TODO: try { ... } catch (...) { ... }


        // TODO: print "program continues"


        System.out.println("--- expected ---");
        System.out.println("caught: out of bounds");
        System.out.println("program continues");
    }
}
