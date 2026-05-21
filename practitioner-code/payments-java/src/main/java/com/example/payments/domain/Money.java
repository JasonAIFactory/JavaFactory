package com.example.payments.domain;

import com.example.payments.domain.exception.CurrencyMismatchException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Money is a VALUE OBJECT (DDD). Defining traits:
 *  - Immutable: every operation returns a new Money, never mutates.
 *  - Identity is by VALUE, not by reference (10 USD == 10 USD).
 *  - It enforces its own invariants (no double for money, no cross-currency math).
 *
 * Implemented as a record so equals/hashCode/toString are auto-generated and
 * correct - PROVIDED we canonicalize the BigDecimal scale at construction,
 * because new BigDecimal("10.00").equals(new BigDecimal("10")) is FALSE in
 * Java (a famous gotcha). We normalize on the way in, so equality is sane.
 *
 * Why BigDecimal, not double:
 *   double 0.1 + 0.2 == 0.30000000000000004. In money, that's a bug at scale.
 *   BigDecimal is exact and the production default.
 */
public record Money(BigDecimal amount, Currency currency) {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        // Canonicalize scale once, so 10 USD and 10.00 USD become equal Money.
        amount = amount.setScale(currency.fractionDigits(), RoundingMode.HALF_EVEN);
    }

    // --- factories: convenience constructors that read at the call site -----
    public static Money of(String amount, Currency currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    // --- arithmetic: returns NEW Money (immutability) -----------------------
    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isGreaterThanOrEqualTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount) >= 0;
    }

    private void requireSameCurrency(Money other) {
        if (currency != other.currency) {
            throw new CurrencyMismatchException(
                    "Cannot operate on " + currency + " and " + other.currency);
        }
    }
}
