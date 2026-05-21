package com.example.payments.domain;

import com.example.payments.domain.exception.OptimisticLockException;

import java.util.Optional;

/**
 * PORT (hexagonal architecture). The domain DEFINES what it needs from
 * persistence; the infrastructure layer PROVIDES it. The application service
 * depends on this interface, NEVER on a concrete repository - this is the D
 * in SOLID (Dependency Inversion). Result:
 *
 *   - You can swap InMemory for JPA without touching the domain or the
 *     application service.
 *   - Tests inject a fake/in-memory adapter for speed.
 *   - The database schema is an INFRASTRUCTURE detail, not a domain detail.
 *
 * The interface lives in the DOMAIN package on purpose: the domain owns its
 * contract. Implementations live in infrastructure (one per technology).
 */
public interface WalletRepository {

    Optional<Wallet> findById(WalletId id);

    /**
     * Persists the wallet using optimistic concurrency control. Throws
     * {@link OptimisticLockException} if the stored version differs from
     * {@code wallet.persistedVersion()} - meaning somebody else committed
     * between our load and our save. On success, the wallet's
     * persistedVersion is updated to its current version (the repository
     * calls {@code wallet.markPersisted()}).
     */
    void save(Wallet wallet);
}
