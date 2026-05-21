package com.example.payments.domain.exception;

import com.example.payments.domain.Money;
import com.example.payments.domain.WalletId;

/**
 * Domain exception carrying CONTEXT (which wallet, requested vs available).
 * Operations teams love this - the message in the log tells them what
 * happened without grepping. Plain "Insufficient funds" wastes everyone's
 * time.
 */
public class InsufficientFundsException extends RuntimeException {
    private final WalletId walletId;
    private final Money requested;
    private final Money available;

    public InsufficientFundsException(WalletId walletId, Money requested, Money available) {
        super("Wallet " + walletId.value() + " has " + available + " but " + requested + " was requested");
        this.walletId = walletId;
        this.requested = requested;
        this.available = available;
    }

    public WalletId walletId()   { return walletId; }
    public Money    requested()  { return requested; }
    public Money    available()  { return available; }
}
