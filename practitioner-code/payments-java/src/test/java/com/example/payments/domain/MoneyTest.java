package com.example.payments.domain;

import com.example.payments.domain.exception.CurrencyMismatchException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests are EXECUTABLE SPECIFICATION: they pin Money's contract so it can
 * never silently drift. Names read like sentences a product person can
 * follow.
 */
class MoneyTest {

    @Test
    @DisplayName("equality treats 10 USD and 10.00 USD as the same value")
    void equality_normalizesScale() {
        Money a = Money.of("10",    Currency.USD);
        Money b = Money.of("10.00", Currency.USD);
        assertEquals(a, b, "Money's record equals must work across BigDecimal scales");
        assertEquals(a.hashCode(), b.hashCode(), "equal Money must share hashCode");
    }

    @Test
    @DisplayName("addition stays in the same currency")
    void plus_inSameCurrency() {
        Money sum = Money.of("3.50", Currency.USD).plus(Money.of("1.49", Currency.USD));
        assertEquals(Money.of("4.99", Currency.USD), sum);
    }

    @Test
    @DisplayName("cross-currency arithmetic is rejected at the type/value boundary")
    void plus_acrossCurrencies_throws() {
        Money usd = Money.of("10", Currency.USD);
        Money eur = Money.of("10", Currency.EUR);
        assertThrows(CurrencyMismatchException.class, () -> usd.plus(eur));
    }

    @Test
    @DisplayName("JPY has 0 fraction digits - no minor unit")
    void jpy_hasZeroFractionDigits() {
        Money y = Money.of("100", Currency.JPY);
        assertEquals(0, y.amount().scale(), "JPY must canonicalize to scale 0");
        assertEquals(new BigDecimal("100"), y.amount());
    }

    @Test
    @DisplayName("Money is immutable - plus returns a new instance, original unchanged")
    void immutability_plusDoesNotMutate() {
        Money a = Money.of("5", Currency.USD);
        Money b = a.plus(Money.of("3", Currency.USD));
        assertEquals(Money.of("5", Currency.USD), a);   // original intact
        assertEquals(Money.of("8", Currency.USD), b);   // new value
        assertNotSame(a, b);
    }

    @Test
    @DisplayName("nulls fail loudly at construction, not later")
    void nulls_failFast() {
        assertThrows(NullPointerException.class, () -> new Money(null, Currency.USD));
        assertThrows(NullPointerException.class, () -> new Money(BigDecimal.ONE, null));
    }
}
