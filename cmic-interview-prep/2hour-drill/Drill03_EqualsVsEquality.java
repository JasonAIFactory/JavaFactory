/*
 * Drill 3 — == vs equals + null safety
 *
 * 🎯 Goal:
 *   1. Show that "hello" == "hello" is true (string pool)
 *   2. Show that new String("hello") == "hello" is false
 *   3. Compare a possibly-null variable to "OK" safely
 *      (call .equals on the constant, not the variable)
 *
 * 🗣️ Say 10 times:
 *   "Double equals checks the reference. Equals method checks the value.
 *    String literals share one object in the pool.
 *    new String always makes a new object.
 *    If a variable can be null, I put the constant first to avoid NullPointerException."
 *   [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]
 *
 * Follow-up:
 *   Q: "Why constant first?"
 *   A: "Because if the variable is null, calling .equals on it throws NPE.
 *       Constant is never null, so it is safe."
 *
 *   Q: "Can I use Objects.equals?"
 *   A: "Yes. Objects.equals handles null on both sides. Cleaner."
 */
public class Drill03_EqualsVsEquality {
    public static void main(String[] args) {
        String a = "hello";
        String b = "hello";
        String c = new String("hello");

        // TODO: print a == b, a == c, a.equals(c)


        // TODO: variable might be null, compare to "OK"
        String input = null;
        // print true if input equals "OK", false otherwise — and don't crash on null


        // expected output:
        // true
        // false
        // true
        // false
    }
}
