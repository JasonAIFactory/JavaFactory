package com.example.payments.application;

import com.example.payments.domain.Currency;
import com.example.payments.domain.Money;
import com.example.payments.domain.Wallet;
import com.example.payments.domain.WalletId;
import com.example.payments.domain.WalletRepository;
import com.example.payments.domain.exception.OptimisticLockException;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

/**
 * APPLICATION SERVICE (hexagonal architecture). Orchestrates use cases.
 *
 * What this layer DOES:
 *   - Load aggregates by id (via the repository port).
 *   - Call domain methods to enforce invariants (debit/credit).
 *   - Persist via the repository.
 *   - Handle OCC conflicts with bounded retry.
 *   - Translate "wallet not found" into the application's language.
 *
 * What this layer DOES NOT do:
 *   - Business rules (those live in Wallet/Money).
 *   - Know about HTTP, JSON, JPA, or any framework. Inputs are domain
 *     types. Part 5 wires a Spring controller on top - controllers translate
 *     HTTP <-> these method calls, nothing more.
 *
 * Why retry here (not in domain, not in repository): retry is an
 * APPLICATION POLICY (how hard to try). The domain is pure rules; the
 * repository reports facts ("conflict"); the application decides what to do.
 */
public class WalletService {

    private final WalletRepository wallets;
    private final int maxRetries;

    public WalletService(WalletRepository wallets) {
        this(wallets, 3);   // sane production default
    }

    public WalletService(WalletRepository wallets, int maxRetries) {
        // Dependency Injection by constructor: explicit, easy to test, no
        // hidden globals. The application service depends on the PORT, not
        // on any concrete repository (DIP, the D in SOLID). maxRetries is
        // a POLICY, also injected - tests can use a higher value to ride
        // out heavy contention deterministically.
        this.wallets = wallets;
        if (maxRetries < 1) throw new IllegalArgumentException("maxRetries must be >= 1");
        this.maxRetries = maxRetries;
    }

    public WalletId openWallet(String openingAmount, Currency currency) {
        WalletId id = WalletId.newId();
        Wallet w = Wallet.open(id, new Money(new BigDecimal(openingAmount), currency));
        wallets.save(w);
        return id;
    }

    /** Debit the wallet. Retries up to MAX_RETRIES times on OCC conflicts. */
    public Money debit(WalletId id, Money amount) {
        return withOptimisticRetry(() -> {
            Wallet w = loadOrThrow(id);
            w.debit(amount);
            wallets.save(w);
            return w.balance();
        });
    }

    public Money credit(WalletId id, Money amount) {
        return withOptimisticRetry(() -> {
            Wallet w = loadOrThrow(id);
            w.credit(amount);
            wallets.save(w);
            return w.balance();
        });
    }

    public Money balance(WalletId id) {
        return loadOrThrow(id).balance();
    }

    // ---- internals --------------------------------------------------------
    private Wallet loadOrThrow(WalletId id) {
        return wallets.findById(id).orElseThrow(
                () -> new NoSuchElementException("wallet not found: " + id.value()));
    }

    private <T> T withOptimisticRetry(java.util.function.Supplier<T> action) {
        OptimisticLockException last = null;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try { return action.get(); }
            catch (OptimisticLockException e) { last = e; }
        }
        throw last;   // give up; let the caller (e.g. an HTTP layer) translate to 409
    }
}
