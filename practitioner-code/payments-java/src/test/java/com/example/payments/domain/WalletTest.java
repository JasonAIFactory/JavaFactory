package com.example.payments.domain;

import com.example.payments.domain.exception.CurrencyMismatchException;
import com.example.payments.domain.exception.InsufficientFundsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WalletTest {

    @Test
    @DisplayName("a newly opened wallet has the opening balance and version 0")
    void open_setsState() {
        Wallet w = Wallet.open(WalletId.newId(), Money.of("100.00", Currency.USD));
        assertEquals(Money.of("100.00", Currency.USD), w.balance());
        assertEquals(0L, w.version());
    }

    @Test
    @DisplayName("opening with a negative balance is rejected")
    void open_withNegative_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> Wallet.open(WalletId.newId(), Money.of("-1", Currency.USD)));
    }

    @Test
    @DisplayName("debit reduces balance and bumps version")
    void debit_happyPath() {
        Wallet w = Wallet.open(WalletId.newId(), Money.of("100", Currency.USD));
        w.debit(Money.of("30", Currency.USD));
        assertEquals(Money.of("70", Currency.USD), w.balance());
        assertEquals(1L, w.version(), "every state change must bump the version");
    }

    @Test
    @DisplayName("debit beyond balance throws InsufficientFunds with full context")
    void debit_insufficient_throws() {
        WalletId id = WalletId.newId();
        Wallet w = Wallet.open(id, Money.of("10", Currency.USD));
        InsufficientFundsException ex = assertThrows(InsufficientFundsException.class,
                () -> w.debit(Money.of("50", Currency.USD)));
        assertEquals(id, ex.walletId());
        assertEquals(Money.of("50", Currency.USD), ex.requested());
        assertEquals(Money.of("10", Currency.USD), ex.available());
        // and balance must NOT have changed (invariant preserved on failure)
        assertEquals(Money.of("10", Currency.USD), w.balance());
        assertEquals(0L, w.version(), "failed operations must not bump version");
    }

    @Test
    @DisplayName("debit in the wrong currency is rejected by the aggregate")
    void debit_wrongCurrency_throws() {
        Wallet w = Wallet.open(WalletId.newId(), Money.of("100", Currency.USD));
        assertThrows(CurrencyMismatchException.class,
                () -> w.debit(Money.of("10", Currency.EUR)));
    }

    @Test
    @DisplayName("credit adds funds and bumps version")
    void credit_happyPath() {
        Wallet w = Wallet.open(WalletId.newId(), Money.of("0", Currency.USD));
        w.credit(Money.of("25", Currency.USD));
        assertEquals(Money.of("25", Currency.USD), w.balance());
        assertEquals(1L, w.version());
    }

    @Test
    @DisplayName("rehydrate restores state from persistence (repository's job, not callers)")
    void rehydrate_preservesVersion() {
        WalletId id = WalletId.newId();
        Wallet w = Wallet.rehydrate(id, Money.of("42", Currency.USD), 7L);
        assertEquals(7L, w.version());
        w.credit(Money.of("8", Currency.USD));
        assertEquals(8L, w.version(), "version must continue from the rehydrated value");
    }
}
