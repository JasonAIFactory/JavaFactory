/*
 * Drill 10 — Strategy Pattern
 *
 * 🎯 Goal:
 *   PaymentStrategy interface with pay(double).
 *   Implementations: CreditCard, BankTransfer, Crypto.
 *   PaymentService takes a strategy and calls strategy.pay() in checkout().
 *
 * 🗣️ Say 10 times:
 *   "Strategy pattern lets me swap behavior at runtime.
 *    I define an interface with one method.
 *    Different classes implement it with different logic.
 *    The caller works with the interface, not the concrete class.
 *    To add a new strategy, I add a new class. No change to the caller.
 *    This is the open-closed principle."
 *   [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]
 *
 * Follow-up:
 *   Q: "Real example?"
 *   A: "Comparator for sorting. Discount strategy. Cost calculator for ERP."
 *
 *   Q: "Difference from inheritance?"
 *   A: "Strategy uses composition. We hold the behavior as a field, not extend it.
 *       More flexible. We can swap the strategy at runtime."
 */
public class Drill10_Strategy {

    // TODO: interface PaymentStrategy with pay(double)


    // TODO: 3 implementations — CreditCard, BankTransfer, Crypto


    static class PaymentService {
        // TODO: hold a PaymentStrategy field
        // TODO: constructor takes strategy
        // TODO: checkout(double amount) calls strategy.pay(amount)
    }

    public static void main(String[] args) {
        // TODO: create 3 PaymentService objects with different strategies, call checkout(100)


        // expected output:
        // Paid 100.0 with credit card
        // Paid 100.0 with bank transfer
        // Paid 100.0 with crypto
    }
}
