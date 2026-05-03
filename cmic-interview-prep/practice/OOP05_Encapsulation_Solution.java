/*
 * OOP 5 — SOLUTION
 *
 * BankAccount with private balance.
 * Methods validate input and throw if rules are violated.
 * Object stays in valid state at all times.
 */
public class OOP05_Encapsulation_Solution {

    static class BankAccount {
        private double balance;

        public BankAccount(double initial) {
            if (initial < 0) throw new IllegalArgumentException("initial must be >= 0");
            this.balance = initial;
        }

        public void deposit(double amount) {
            if (amount <= 0) throw new IllegalArgumentException("deposit must be > 0");
            balance += amount;
        }

        public void withdraw(double amount) {
            if (amount <= 0)      throw new IllegalArgumentException("withdraw must be > 0");
            if (amount > balance) throw new IllegalArgumentException("insufficient balance");
            balance -= amount;
        }

        public double getBalance() { return balance; }
    }

    public static void main(String[] args) {
        BankAccount a = new BankAccount(1000);

        a.deposit(500);
        a.withdraw(200);
        System.out.println(a.getBalance());      // 1300.0

        try {
            a.withdraw(99999);
            System.out.println("should not reach here");
        } catch (IllegalArgumentException e) {
            System.out.println("blocked: " + e.getMessage());
        }

        try {
            a.deposit(-10);
            System.out.println("should not reach here");
        } catch (IllegalArgumentException e) {
            System.out.println("blocked: " + e.getMessage());
        }
    }
}
