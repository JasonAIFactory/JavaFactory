package com.example.payments.infrastructure;

import com.example.payments.domain.Currency;
import com.example.payments.domain.Money;
import com.example.payments.domain.Wallet;
import com.example.payments.domain.WalletId;
import com.example.payments.domain.exception.OptimisticLockException;
import com.example.payments.infrastructure.inmemory.InMemoryWalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for the InMemoryWalletRepository adapter. Anything we add
 * here also needs to pass when we later swap in a JPA adapter (Part 5).
 */
class InMemoryWalletRepositoryTest {

    @Test
    @DisplayName("missing id returns empty Optional, not null")
    void findById_missing_returnsEmpty() {
        var repo = new InMemoryWalletRepository();
        assertEquals(Optional.empty(), repo.findById(WalletId.newId()));
    }

    @Test
    @DisplayName("save a new wallet, then load it back equal")
    void save_thenLoadBack() {
        var repo = new InMemoryWalletRepository();
        WalletId id = WalletId.newId();
        Wallet w = Wallet.open(id, Money.of("100", Currency.USD));
        repo.save(w);

        Wallet loaded = repo.findById(id).orElseThrow();
        assertEquals(id, loaded.id());
        assertEquals(Money.of("100", Currency.USD), loaded.balance());
        assertEquals(0L, loaded.version());
        assertEquals(0L, loaded.persistedVersion(), "loaded wallet's persistedVersion equals stored version");
    }

    @Test
    @DisplayName("load -> mutate -> save -> reload shows the new state")
    void update_roundTrip() {
        var repo = new InMemoryWalletRepository();
        WalletId id = WalletId.newId();
        repo.save(Wallet.open(id, Money.of("100", Currency.USD)));

        Wallet w = repo.findById(id).orElseThrow();
        w.debit(Money.of("30", Currency.USD));
        repo.save(w);

        Wallet again = repo.findById(id).orElseThrow();
        assertEquals(Money.of("70", Currency.USD), again.balance());
        assertEquals(1L, again.version());
    }

    @Test
    @DisplayName("concurrent modification detected: 2nd save against stale version throws OptimisticLockException")
    void save_staleVersion_throws() {
        var repo = new InMemoryWalletRepository();
        WalletId id = WalletId.newId();
        repo.save(Wallet.open(id, Money.of("100", Currency.USD)));

        // Two callers load the SAME wallet at the same version (simulating a race).
        Wallet a = repo.findById(id).orElseThrow();
        Wallet b = repo.findById(id).orElseThrow();

        // 'a' commits first - succeeds, stored version becomes 1.
        a.debit(Money.of("10", Currency.USD));
        repo.save(a);

        // 'b' still thinks the world is at version 0 - its save must be rejected.
        b.debit(Money.of("20", Currency.USD));
        OptimisticLockException ex = assertThrows(OptimisticLockException.class, () -> repo.save(b));
        assertEquals(id, ex.walletId());
        assertEquals(0L, ex.expectedVersion());
        assertEquals(1L, ex.actualVersion());

        // and the stored state must reflect 'a's debit, NOT 'b's lost update.
        assertEquals(Money.of("90", Currency.USD), repo.findById(id).orElseThrow().balance());
    }

    @Test
    @DisplayName("repository hands out fresh Wallet instances - no shared mutable state")
    void findById_returnsFreshInstance() {
        var repo = new InMemoryWalletRepository();
        WalletId id = WalletId.newId();
        repo.save(Wallet.open(id, Money.of("100", Currency.USD)));
        Wallet a = repo.findById(id).orElseThrow();
        Wallet b = repo.findById(id).orElseThrow();
        assertNotSame(a, b, "callers must not accidentally share mutable Wallet state");
        a.debit(Money.of("10", Currency.USD));
        assertEquals(Money.of("100", Currency.USD), b.balance(), "mutation on 'a' must not leak into 'b'");
    }
}
