package com.example.payments.domain.exception;

import com.example.payments.domain.WalletId;

/**
 * Optimistic concurrency control failure: somebody else updated this wallet
 * between our load and our save. The CORRECT response is usually to reload
 * and retry the operation - the application service does that (see
 * WalletService). We carry the ids/versions so logs say what conflicted.
 *
 * "Optimistic" because we did NOT take a lock at load time; we BET no one
 * else would write. When the bet loses, this is the signal. Compare with
 * "pessimistic" locks that serialize all access (correct but slower).
 */
public class OptimisticLockException extends RuntimeException {
    private final WalletId walletId;
    private final long expectedVersion;
    private final long actualVersion;

    public OptimisticLockException(WalletId walletId, long expectedVersion, long actualVersion) {
        super("Optimistic lock failure on wallet " + walletId.value()
              + ": expected version " + expectedVersion + " but stored is " + actualVersion);
        this.walletId = walletId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }

    public WalletId walletId()        { return walletId; }
    public long     expectedVersion() { return expectedVersion; }
    public long     actualVersion()   { return actualVersion; }
}
