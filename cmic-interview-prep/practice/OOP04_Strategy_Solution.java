/*
 * OOP 4 — SOLUTION
 *
 * Strategy pattern: PaymentService works with PaymentStrategy interface.
 * Add a new payment method by adding a new class — no change to PaymentService.
 */
public class OOP04_Strategy_Solution {

    interface PaymentStrategy {
        void pay(double amount);
    }

    static class CreditCard implements PaymentStrategy {
        public void pay(double a) { System.out.println("Paid " + a + " with credit card"); }
    }

    static class BankTransfer implements PaymentStrategy {
        public void pay(double a) { System.out.println("Paid " + a + " with bank transfer"); }
    }

    static class Crypto implements PaymentStrategy {
        public void pay(double a) { System.out.println("Paid " + a + " with crypto"); }
    }

    static class PaymentService {
        private final PaymentStrategy strategy;
        public PaymentService(PaymentStrategy s) { this.strategy = s; }
        public void checkout(double amount) { strategy.pay(amount); }
    }

    public static void main(String[] args) {
        PaymentStrategy[] strategies = { new CreditCard(), new BankTransfer(), new Crypto() };
        for (PaymentStrategy s : strategies) {
            new PaymentService(s).checkout(100);
        }
    }
}
