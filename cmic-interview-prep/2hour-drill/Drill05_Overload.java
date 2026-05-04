/*
 * Drill 5 — Method Overloading
 *
 * 🎯 Goal:
 *   Calculator with three add methods:
 *     add(int, int) returns int
 *     add(double, double) returns double
 *     add(int, int, int) returns int
 *
 * 🗣️ Say 10 times:
 *   "Overloading means same name, different parameters in the same class.
 *    The compiler picks the right method based on the argument types.
 *    It is decided at compile time, not runtime."
 *   [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]
 *
 * Follow-up:
 *   Q: "Why use overloading instead of different names like addInt, addDouble?"
 *   A: "Same intent, different inputs. Cleaner API for the caller."
 *
 *   Q: "Can return type alone overload?"
 *   A: "No. Parameters must differ. Return type alone is not enough."
 */
public class Drill05_Overload {

    static class Calculator {
        // TODO: int add(int, int)


        // TODO: double add(double, double)


        // TODO: int add(int, int, int)
    }

    public static void main(String[] args) {
        Calculator c = new Calculator();

        // TODO: call all three and print


        // expected output:
        // 5
        // 6.0
        // 6
    }
}
