package com.example.payments.domain;

/**
 * Currency as an enum so the type system itself forbids "USDD" or "us-dollar".
 * Each currency knows its own fraction digits - this drives Money's canonical
 * scale so equality is well-defined (see Money).
 *
 * Why an enum (and not a String like "USD"):
 *  - Compile-time exhaustiveness in switch.
 *  - No "stringly typed" bugs ("usd" vs "USD" vs " USD ").
 *  - One place to add a new currency.
 */
public enum Currency {
    USD(2),
    EUR(2),
    JPY(0);   // yen has no minor unit - a real source of bugs if you hardcode "2"

    private final int fractionDigits;

    Currency(int fractionDigits) {
        this.fractionDigits = fractionDigits;
    }

    public int fractionDigits() {
        return fractionDigits;
    }
}
