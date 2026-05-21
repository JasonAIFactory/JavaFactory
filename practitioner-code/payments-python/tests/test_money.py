"""Executable specification for Money - pins the contract."""
from decimal import Decimal

import pytest

from payments.domain.currency import Currency
from payments.domain.exceptions import CurrencyMismatchError
from payments.domain.money import Money


def test_equality_normalizes_scale():
    a = Money.of("10",    Currency.USD)
    b = Money.of("10.00", Currency.USD)
    assert a == b
    assert hash(a) == hash(b)


def test_plus_in_same_currency():
    total = Money.of("3.50", Currency.USD).plus(Money.of("1.49", Currency.USD))
    assert total == Money.of("4.99", Currency.USD)


def test_plus_across_currencies_is_rejected():
    with pytest.raises(CurrencyMismatchError):
        Money.of("10", Currency.USD).plus(Money.of("10", Currency.EUR))


def test_jpy_has_zero_fraction_digits():
    y = Money.of("100", Currency.JPY)
    assert y.amount.as_tuple().exponent == 0
    assert y.amount == Decimal("100")


def test_immutability_plus_does_not_mutate_original():
    a = Money.of("5", Currency.USD)
    b = a.plus(Money.of("3", Currency.USD))
    assert a == Money.of("5", Currency.USD)
    assert b == Money.of("8", Currency.USD)
    assert a is not b


def test_str_carries_currency_code():
    assert str(Money.of("1.23", Currency.USD)) == "1.23 USD"
