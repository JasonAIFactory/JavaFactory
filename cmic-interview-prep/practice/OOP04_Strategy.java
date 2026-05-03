/*
 * OOP 4 — Strategy Pattern (Live Coding)
 *
 * Problem:
 *   Build a payment system that supports multiple payment methods (CreditCard, BankTransfer, Crypto)
 *   without using if/else or switch on a payment type string.
 *
 * Why Strategy Pattern is good:
 *   1. Behavior swap at runtime — pick the algorithm based on input
 *   2. Add a new strategy without changing the caller (open-closed principle)
 *   3. Easier to test each strategy in isolation
 *
 * Real-world examples:
 *   - Sorting strategies (Comparator)
 *   - Discount strategies (loyalty, holiday, employee)
 *   - Cost calculation (labor, material, equipment) — exactly CMiC's domain
 *
 * English script:
 *   "Strategy pattern lets me change behavior at runtime. I define an interface
 *    with one method, then provide multiple implementations. The caller works
 *    with the interface and does not care about the concrete class. To add a new
 *    strategy I just add a new class — no change to the caller. This is the
 *    open-closed principle."
 */
public class OOP04_Strategy {

    // TODO: define interface PaymentStrategy with method `void pay(double amount)`


    // TODO: class CreditCard implements PaymentStrategy
    //       prints "Paid <amount> with credit card"


    // TODO: class BankTransfer implements PaymentStrategy
    //       prints "Paid <amount> with bank transfer"


    // TODO: class Crypto implements PaymentStrategy
    //       prints "Paid <amount> with crypto"


    static class PaymentService {
        private final PaymentStrategy strategy;
        public PaymentService(PaymentStrategy s) { this.strategy = s; }
        public void checkout(double amount) {
            strategy.pay(amount);
        }
    }

    public static void main(String[] args) {
        // TODO: create three PaymentService objects with different strategies
        //       and call checkout(100)


        System.out.println("--- expected ---");
        System.out.println("Paid 100.0 with credit card");
        System.out.println("Paid 100.0 with bank transfer");
        System.out.println("Paid 100.0 with crypto");
    }
}

// 컴파일 에러 안 나려면 PaymentStrategy 인터페이스가 main 클래스 위에서 정의돼야 함.
// 위에 TODO 자리에 다음처럼 작성:
//   interface PaymentStrategy { void pay(double amount); }
