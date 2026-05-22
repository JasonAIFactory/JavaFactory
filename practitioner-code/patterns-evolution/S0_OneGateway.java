// =============================================================================
// S0 — One Gateway, Hard-Coded
//
// THE STORY (KO):
//   당신은 PayCo의 엔지니어 #2다. 첫 임무 — "Stripe로 카드결제만 되게 해.
//   배포 다음주." 다른 게이트웨이, 재시도, 메트릭, 이벤트 같은 건 *아직 아무도
//   요구하지 않았다*. 미래의 요구사항을 상상해서 설계하지 마라. 일단 작동시켜라.
//
// THE STORY (EN):
//   Engineer #2 at PayCo. First task: "Just take card payments through Stripe.
//   Ship next week." No other gateways, no retries, no metrics, no events have
//   been asked for yet. Don't invent future requirements. Make it work.
//
// RUN:    java -ea S0_OneGateway.java
// STATUS: it works. The tests below pass. We are done with S0.
//
// -----------------------------------------------------------------------------
// YOUR TASK FOR S1 (DO THIS BEFORE SCROLLING):
//
//   The product manager just walked over: "We need PayPal too. Also, finance is
//   negotiating with a Korean card switch called KCard. Probably this quarter."
//
//   YOUR TASK: change THIS file so PaymentProcessor.charge(...) can use Stripe
//   OR PayPal. The caller picks. Add a stub `PaypalClient` similar to
//   `StripeClient` below. Keep the existing Stripe test passing. Add a test
//   that a PayPal payment also succeeds.
//
//   30분 이상 버텨라. 막히면 좋다. 막히는 그 지점이 패턴이 태어나는 자리다.
//   Push for at least 30 min. The exact place you get stuck is where a pattern
//   is born.
//
//   가장 끌리는 두 가지 방법 (둘 다 일단 해봐도 좋다 - 아픔을 직접 느껴라):
//     (A) charge() 안에 `if (gateway.equals("stripe")) ... else if ...`
//     (B) 메서드를 둘로 복제: chargeViaStripe(...), chargeViaPaypal(...)
//
//   그러고 나서 스스로에게 질문해라:
//     - "KCard도 들어왔다. 어디를 고쳐야 하나? 몇 줄이나?"
//     - "5개월 뒤 다른 팀원이 새 게이트웨이를 추가하려면 어디부터 봐야 하나?"
//     - "테스트는 어떻게 짜지? 모든 분기를 한 번씩 다 거쳐야 하나?"
//
//   그 답이 *불편하게* 느껴질 때 — 그게 S1을 부를 신호다.
//   When the answers feel UNCOMFORTABLE - that's when you call for S1.
// =============================================================================

import java.util.UUID;

public class S0_OneGateway {

    // -------------------------------------------------------------------------
    // The Stripe SDK. We do NOT own this code. This is a stub that mimics the
    // real SDK's shape (token + cents + currency in; Charge object out, with
    // an error code on failure). When you write S1's PayPal stub, give it a
    // DIFFERENT shape on purpose - that's what real third-party SDKs do.
    // -------------------------------------------------------------------------
    static class StripeClient {
        Charge createCharge(String cardToken, long amountInCents, String currency) {
            // "decline" token deterministically simulates a card decline.
            if ("decline".equals(cardToken)) {
                return new Charge(null, "card_declined");
            }
            return new Charge("ch_" + UUID.randomUUID(), null);
        }
    }

    record Charge(String id, String errorCode) {
        boolean ok() { return errorCode == null; }
    }

    // -------------------------------------------------------------------------
    // Our code. One responsibility: charge a card via Stripe. Returns true on
    // success. No abstraction over the SDK, no retry, no logging, no events.
    // That is CORRECT for S0 - YAGNI ("You Aren't Gonna Need It"). Build for
    // today's requirement; let the next requirement pull the design forward.
    // -------------------------------------------------------------------------
    static class PaymentProcessor {
        private final StripeClient stripe = new StripeClient();

        public boolean charge(long amountInCents, String currency, String cardToken) {
            Charge c = stripe.createCharge(cardToken, amountInCents, currency);
            return c.ok();
        }
    }

    // -------------------------------------------------------------------------
    // Tests as a runnable main. Module 01-03 evolution style. If you change
    // the code for S1, KEEP THESE TWO PASSING. (Plus add a PayPal one.)
    // -------------------------------------------------------------------------
    public static void main(String[] args) {
        PaymentProcessor pp = new PaymentProcessor();

        // happy path: a valid token charges successfully.
        assert pp.charge(5000, "USD", "tok_visa")
                : "S0: a valid card should succeed";

        // failure path: the decline token is rejected.
        assert !pp.charge(5000, "USD", "decline")
                : "S0: the decline token must NOT succeed";

        System.out.println("S0 OK - one gateway (Stripe) works. Now read the task at the top of this file.");
    }
}
