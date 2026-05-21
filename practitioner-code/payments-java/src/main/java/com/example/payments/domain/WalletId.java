package com.example.payments.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identifier (a "tiny type"). NEVER pass raw UUID/String/long for ids
 * across the domain - it lets you swap a customerId for a walletId silently.
 * A WalletId can ONLY be a WalletId; the compiler stops the bug.
 *
 * Cost: a class. Benefit: a whole bug class becomes impossible.
 */
public record WalletId(UUID value) {
    public WalletId {
        Objects.requireNonNull(value, "value");
    }

    public static WalletId newId() {
        return new WalletId(UUID.randomUUID());
    }
}
