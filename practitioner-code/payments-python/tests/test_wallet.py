"""Executable specification for the Wallet aggregate."""
import pytest

from payments.domain.currency import Currency
from payments.domain.exceptions import CurrencyMismatchError, InsufficientFundsError
from payments.domain.money import Money
from payments.domain.wallet import Wallet
from payments.domain.wallet_id import WalletId


def test_open_sets_state():
    w = Wallet.open(WalletId.new(), Money.of("100", Currency.USD))
    assert w.balance == Money.of("100", Currency.USD)
    assert w.version == 0


def test_open_with_negative_is_rejected():
    with pytest.raises(ValueError):
        Wallet.open(WalletId.new(), Money.of("-1", Currency.USD))


def test_debit_happy_path():
    w = Wallet.open(WalletId.new(), Money.of("100", Currency.USD))
    w.debit(Money.of("30", Currency.USD))
    assert w.balance == Money.of("70", Currency.USD)
    assert w.version == 1, "every state change must bump the version"


def test_debit_insufficient_funds_carries_context_and_preserves_state():
    wid = WalletId.new()
    w = Wallet.open(wid, Money.of("10", Currency.USD))
    with pytest.raises(InsufficientFundsError) as exc:
        w.debit(Money.of("50", Currency.USD))
    assert exc.value.wallet_id == wid
    assert exc.value.requested == Money.of("50", Currency.USD)
    assert exc.value.available == Money.of("10", Currency.USD)
    # invariant: failed operation must NOT mutate the wallet
    assert w.balance == Money.of("10", Currency.USD)
    assert w.version == 0, "failed operations must not bump version"


def test_debit_wrong_currency_is_rejected_by_aggregate():
    w = Wallet.open(WalletId.new(), Money.of("100", Currency.USD))
    with pytest.raises(CurrencyMismatchError):
        w.debit(Money.of("10", Currency.EUR))


def test_credit_adds_funds_and_bumps_version():
    w = Wallet.open(WalletId.new(), Money.of("0", Currency.USD))
    w.credit(Money.of("25", Currency.USD))
    assert w.balance == Money.of("25", Currency.USD)
    assert w.version == 1


def test_rehydrate_preserves_version_for_persistence_roundtrip():
    wid = WalletId.new()
    w = Wallet.rehydrate(wid, Money.of("42", Currency.USD), version=7)
    assert w.version == 7
    w.credit(Money.of("8", Currency.USD))
    assert w.version == 8, "version must continue from the rehydrated value"
