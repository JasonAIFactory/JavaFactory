package com.example.payments.infrastructure.inmemory;

import com.example.payments.domain.Money;
import com.example.payments.domain.Wallet;
import com.example.payments.domain.WalletId;
import com.example.payments.domain.WalletRepository;
import com.example.payments.domain.exception.OptimisticLockException;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ADAPTER (hexagonal architecture). One concrete implementation of the
 * WalletRepository port. Lives in infrastructure - the only place that
 * knows we are using an in-memory map. Tomorrow's JPA adapter goes here
 * too; nothing in the domain or application changes.
 *
 * Thread-safety: {@code store.compute} is atomic per-key. Inside it we
 * read the current stored version, compare to the wallet's persistedVersion
 * (set when the wallet was last loaded or saved), and only swap the value
 * if they match - otherwise throw. Real JPA emits
 *   UPDATE ... WHERE id = ? AND version = ?
 * and checks the affected row count. Same semantics, different mechanism.
 *
 * We store an immutable Snapshot (not the Wallet itself) so two concurrent
 * holders of the same id cannot accidentally share mutable state.
 */
public class InMemoryWalletRepository implements WalletRepository {

    private record Snapshot(Money balance, long version) {}

    private final ConcurrentHashMap<WalletId, Snapshot> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Wallet> findById(WalletId id) {
        Snapshot snap = store.get(id);
        if (snap == null) return Optional.empty();
        // Re-hydrate as a fresh Wallet instance - callers must not share mutable state.
        return Optional.of(Wallet.rehydrate(id, snap.balance(), snap.version()));
    }

    @Override
    public void save(Wallet wallet) {
        long expected = wallet.persistedVersion();
        store.compute(wallet.id(), (id, existing) -> {
            long actual = (existing == null) ? 0L : existing.version();
            boolean isCreate = (existing == null) && (expected == 0L) && (wallet.version() == 0L);
            boolean isUpdate = (existing != null) && (actual == expected);
            if (!isCreate && !isUpdate) {
                throw new OptimisticLockException(wallet.id(), expected, actual);
            }
            return new Snapshot(wallet.balance(), wallet.version());
        });
        // Successfully persisted at wallet.version() - reflect that in the
        // in-memory aggregate so the NEXT save uses the new version as its
        // expected prior. Real ORMs do this on flush.
        wallet.markPersisted();
    }
}
