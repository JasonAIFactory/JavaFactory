/*
 * OOP 5 — Encapsulation (Live Coding)
 *
 * Problem:
 *   Build a BankAccount class with:
 *     - private balance field (cannot be accessed directly from outside)
 *     - public deposit(amount) — adds amount, must be > 0
 *     - public withdraw(amount) — subtracts, must be > 0 and <= balance
 *     - public getBalance() — returns current balance
 *   If a rule is broken, throw IllegalArgumentException with a message.
 *
 * Why Encapsulation is good:
 *   1. Protects invariants — balance can never go negative
 *   2. Centralizes business rules — easy to add audit log, validation, etc.
 *   3. Safe refactor — change internal storage without breaking callers
 *
 * Without encapsulation:
 *     account.balance = -1000;   // disaster
 *
 * With encapsulation:
 *     account.withdraw(1000);    // throws if not enough balance
 *
 * English script:
 *   "Encapsulation hides the internal state. I make balance private. The only way
 *    to change it is through deposit and withdraw, which validate the input.
 *    This protects the object from invalid state. If business rules change later,
 *    I update the methods — callers do not change."
 */
public class OOP05_Encapsulation {

    static class BankAccount {
        // TODO: private double balance


        // TODO: constructor BankAccount(double initial) — stores initial


        // TODO: public void deposit(double amount)
        //       throw IllegalArgumentException if amount <= 0


        // TODO: public void withdraw(double amount)
        //       throw IllegalArgumentException if amount <= 0 OR amount > balance


        // TODO: public double getBalance() — returns balance
    }

    public static void main(String[] args) {
        BankAccount a = new BankAccount(1000);

        a.deposit(500);
        a.withdraw(200);
        System.out.println(a.getBalance());   // expected 1300.0

        // Invalid withdraw should throw
        try {
            a.withdraw(99999);
            System.out.println("should not reach here");
        } catch (IllegalArgumentException e) {
            System.out.println("blocked: " + e.getMessage());
        }

        // Invalid deposit should throw
        try {
            a.deposit(-10);
            System.out.println("should not reach here");
        } catch (IllegalArgumentException e) {
            System.out.println("blocked: " + e.getMessage());
        }

        System.out.println("--- expected ---");
        System.out.println("1300.0");
        System.out.println("blocked: <some message>");
        System.out.println("blocked: <some message>");
    }
}
