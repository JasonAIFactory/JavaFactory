package com.example.payments.application;

import com.example.payments.domain.Currency;
import com.example.payments.domain.Money;
import com.example.payments.domain.WalletId;
import com.example.payments.domain.exception.InsufficientFundsException;
import com.example.payments.infrastructure.inmemory.InMemoryWalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class WalletServiceTest {

    @Test
    @DisplayName("openWallet returns an id and balance reflects the opening amount")
    void openWallet_returnsUsableId() {
        var svc = new WalletService(new InMemoryWalletRepository());
        WalletId id = svc.openWallet("100", Currency.USD);
        assertEquals(Money.of("100", Currency.USD), svc.balance(id));
    }

    @Test
    @DisplayName("debit + credit happy paths return the new balance")
    void debit_credit_happyPaths() {
        var svc = new WalletService(new InMemoryWalletRepository());
        WalletId id = svc.openWallet("100", Currency.USD);
        assertEquals(Money.of("70", Currency.USD), svc.debit(id, Money.of("30", Currency.USD)));
        assertEquals(Money.of("95", Currency.USD), svc.credit(id, Money.of("25", Currency.USD)));
    }

    @Test
    @DisplayName("domain exceptions surface through the service unchanged")
    void debit_insufficient_throwsDomainException() {
        var svc = new WalletService(new InMemoryWalletRepository());
        WalletId id = svc.openWallet("10", Currency.USD);
        assertThrows(InsufficientFundsException.class,
                () -> svc.debit(id, Money.of("50", Currency.USD)));
    }

    @Test
    @DisplayName("missing wallet -> NoSuchElementException (the application's language)")
    void balance_missingId_throws() {
        var svc = new WalletService(new InMemoryWalletRepository());
        assertThrows(NoSuchElementException.class, () -> svc.balance(WalletId.newId()));
    }

    /**
     * THE invariant test for Part 2: under concurrent debits on the same
     * wallet, OCC + retry must converge to the correct final balance with
     * ZERO lost updates. This is the property that justifies the whole
     * pattern - without OCC, a naive read-modify-write loses writes.
     */
    @Test
    @DisplayName("INVARIANT: under concurrent debits with OCC retry, no debit is lost")
    void concurrentDebits_zeroLoss() throws Exception {
        var repo = new InMemoryWalletRepository();
        // Generous retries so the test pins the property, not luck. Real
        // services usually use ~3-5 + backoff; here we want determinism.
        var svc = new WalletService(repo, 100);
        WalletId id = svc.openWallet("100", Currency.USD);

        int threads = 8;
        int debitsPerThread = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger conflicts = new AtomicInteger();

        var futures = new java.util.ArrayList<Future<?>>();
        for (int t = 0; t < threads; t++) futures.add(pool.submit(() -> {
            start.await();
            for (int i = 0; i < debitsPerThread; i++) {
                try { svc.debit(id, Money.of("1", Currency.USD)); }
                catch (Exception e) { conflicts.incrementAndGet(); throw e; }
            }
            return null;
        }));
        start.countDown();
        for (Future<?> f : futures) f.get();
        pool.shutdown();

        // 8 threads * 5 debits = 40 debits of $1 from a $100 wallet -> $60.
        Money expected = Money.of("60", Currency.USD);
        assertEquals(expected, svc.balance(id),
                "ZERO-LOSS INVARIANT broken under contention (conflicts that retried: any)");
        assertEquals(0, conflicts.get(), "no debit should have exhausted its retries");
    }

    @Test
    @DisplayName("when retries are EXHAUSTED, the OptimisticLockException surfaces (correct give-up)")
    void retriesExhausted_throws() throws Exception {
        var repo = new InMemoryWalletRepository();
        // maxRetries=1 -> any contention will fail. We force contention by
        // running many concurrent debits; at least one MUST hit the cap.
        var svc = new WalletService(repo, 1);
        WalletId id = svc.openWallet("100", Currency.USD);

        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger conflicts = new AtomicInteger();
        var futures = new java.util.ArrayList<Future<?>>();
        for (int t = 0; t < 16; t++) futures.add(pool.submit(() -> {
            start.await();
            try { svc.debit(id, Money.of("1", Currency.USD)); }
            catch (com.example.payments.domain.exception.OptimisticLockException e) {
                conflicts.incrementAndGet();
            }
            return null;
        }));
        start.countDown();
        for (Future<?> f : futures) f.get();
        pool.shutdown();

        // At least one conflict must have surfaced (give-up is honest).
        assertTrue(conflicts.get() >= 1, "with maxRetries=1, contention MUST produce visible give-ups");
        // And the final balance must equal initial - successful-debits (no phantom debits).
        long successes = 16 - conflicts.get();
        Money expectedBalance = Money.of(String.valueOf(100 - successes), Currency.USD);
        assertEquals(expectedBalance, svc.balance(id),
                "final balance must reflect ONLY the successful debits");
    }
}
