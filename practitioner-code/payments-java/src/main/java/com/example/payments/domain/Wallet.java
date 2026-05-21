package com.example.payments.domain;

import com.example.payments.domain.exception.CurrencyMismatchException;
import com.example.payments.domain.exception.InsufficientFundsException;

import java.util.Objects;

/**
 * Wallet is an AGGREGATE ROOT (DDD): the consistency boundary that owns
 * its invariants. The world can only change a Wallet by calling its
 * methods; there are no public setters. This is the difference between an
 * "anemic" object (a bag of public fields a service mutates) and a real
 * domain object that protects itself.
 *
 * Invariants enforced HERE so they cannot be bypassed by any caller:
 *   I1. Balance is never negative.
 *   I2. Balance currency never changes after creation (no cross-currency
 *       arithmetic on the same wallet).
 *
 * `version` enables OPTIMISTIC LOCKING in the repository layer (Part 2).
 * Every state-changing operation bumps it so concurrent updates can be
 * detected and one of them re-tried instead of silently overwriting.
 *
 * Mutability note: we use controlled mutation (no setters) because real
 * persistence layers like JPA expect entities they can re-hydrate and
 * mutate. An immutable functional style ("debit returns a new Wallet") is
 * also valid; we pick the mainstream one so the leap to Part 5 is small.
 */
public final class Wallet {

    private final WalletId id;
    private Money balance;
    private long version;
    // The version at which this wallet was last loaded or saved. Domain
    // operations do NOT touch this - only the repository, on a successful
    // save. The repository's OCC compares stored.version to this field to
    // detect concurrent writers. This split (revision vs persisted) is how
    // JPA's @Version and SQLAlchemy's version_id_col actually work.
    private long persistedVersion;

    private Wallet(WalletId id, Money openingBalance, long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.balance = Objects.requireNonNull(openingBalance, "openingBalance");
        if (openingBalance.isNegative()) {
            throw new IllegalArgumentException("Opening balance must not be negative");
        }
        this.version = version;
        this.persistedVersion = version;
    }

    /** Factory for a brand-new wallet (version starts at 0). */
    public static Wallet open(WalletId id, Money openingBalance) {
        return new Wallet(id, openingBalance, 0L);
    }

    /** Re-hydrate from persistence. Used by repositories, NOT by callers. */
    public static Wallet rehydrate(WalletId id, Money balance, long version) {
        return new Wallet(id, balance, version);
    }

    // ---- behavior: methods named in the domain's language ------------------
    public void debit(Money amount) {
        requireSameCurrency(amount);
        if (!balance.isGreaterThanOrEqualTo(amount)) {
            throw new InsufficientFundsException(id, amount, balance);
        }
        this.balance = balance.minus(amount);
        this.version++;
    }

    public void credit(Money amount) {
        requireSameCurrency(amount);
        this.balance = balance.plus(amount);
        this.version++;
    }

    // ---- accessors (read-only views) ---------------------------------------
    public WalletId id()               { return id; }
    public Money    balance()          { return balance; }
    public long     version()          { return version; }
    public long     persistedVersion() { return persistedVersion; }

    /**
     * Repository-only API: called after a successful conditional save so the
     * next save uses the new version as its "expected prior". DO NOT call
     * from application or domain code - that would defeat OCC.
     */
    public void markPersisted() {
        this.persistedVersion = this.version;
    }

    private void requireSameCurrency(Money amount) {
        if (amount.currency() != balance.currency()) {
            throw new CurrencyMismatchException(
                    "Wallet " + id.value() + " holds " + balance.currency()
                  + " but operation used " + amount.currency());
        }
    }
}
