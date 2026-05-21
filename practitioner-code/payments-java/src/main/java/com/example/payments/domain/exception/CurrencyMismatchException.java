package com.example.payments.domain.exception;

/**
 * Domain exception: code that catches this knows exactly what went wrong.
 * Generic RuntimeException would be lossy. Type-meaningful errors are part
 * of a good domain language.
 *
 * Unchecked (extends RuntimeException) because programmer error / contract
 * violation - the caller passed wrong currencies. Checked exceptions are
 * for recoverable conditions; this one is "you misused the API".
 */
public class CurrencyMismatchException extends RuntimeException {
    public CurrencyMismatchException(String message) {
        super(message);
    }
}
