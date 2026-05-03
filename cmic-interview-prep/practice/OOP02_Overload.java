/*
 * OOP 2 — Method Overloading (Live Coding)
 *
 * Problem:
 *   Build a Calculator class with three overloaded `add` methods:
 *     - add(int a, int b)         returns int
 *     - add(double a, double b)   returns double
 *     - add(int a, int b, int c)  returns int
 *
 * Why Overloading is good:
 *   1. Same intent, different inputs — easier API for callers
 *   2. No need to invent different names like addInt, addDouble, addThree
 *   3. Compile-time decision — fast, no runtime cost
 *
 * Difference from Overriding:
 *   - Overload: same class, different parameters, decided at COMPILE time
 *   - Override: child redefines parent's method, decided at RUNTIME
 *
 * English script:
 *   "Overloading lets one class have multiple methods with the same name but
 *    different parameters. The compiler picks the right one based on the arguments.
 *    It is decided at compile time, unlike overriding which is at runtime."
 */
public class OOP02_Overload {

    static class Calculator {
        // TODO: int add(int a, int b)


        // TODO: double add(double a, double b)


        // TODO: int add(int a, int b, int c)
    }

    public static void main(String[] args) {
        Calculator c = new Calculator();

        // TODO: print c.add(2, 3)


        // TODO: print c.add(2.5, 3.5)


        // TODO: print c.add(1, 2, 3)


        System.out.println("--- expected ---");
        System.out.println("5");
        System.out.println("6.0");
        System.out.println("6");
    }
}
