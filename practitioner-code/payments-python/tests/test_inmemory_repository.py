"""Contract tests for the InMemoryWalletRepository adapter.

Anything here also needs to pass for the SQLAlchemy adapter we add in Part 5.
"""
import pytest

from payments.domain.currency import Currency
from payments.domain.exceptions import OptimisticLockError
from payments.domain.money import Money
from payments.domain.wallet import Wallet
from payments.domain.wallet_id import WalletId
from payments.infrastructure.inmemory import InMemoryWalletRepository


def test_find_missing_returns_none():
    repo = InMemoryWalletRepository()
    assert repo.find_by_id(WalletId.new()) is None


def test_save_then_load_back_equal():
    repo = InMemoryWalletRepository()
    wid = WalletId.new()
    repo.save(Wallet.open(wid, Money.of("100", Currency.USD)))

    loaded = repo.find_by_id(wid)
    assert loaded is not None
    assert loaded.id == wid
    assert loaded.balance == Money.of("100", Currency.USD)
    assert loaded.version == 0
    assert loaded.persisted_version == 0


def test_update_round_trip():
    repo = InMemoryWalletRepository()
    wid = WalletId.new()
    repo.save(Wallet.open(wid, Money.of("100", Currency.USD)))

    w = repo.find_by_id(wid)
    assert w is not None
    w.debit(Money.of("30", Currency.USD))
    repo.save(w)

    again = repo.find_by_id(wid)
    assert again is not None
    assert again.balance == Money.of("70", Currency.USD)
    assert again.version == 1


def test_stale_version_save_raises_optimistic_lock():
    repo = InMemoryWalletRepository()
    wid = WalletId.new()
    repo.save(Wallet.open(wid, Money.of("100", Currency.USD)))

    # Two callers load the same wallet at the same version (the race).
    a = repo.find_by_id(wid)
    b = repo.find_by_id(wid)
    assert a is not None and b is not None

    a.debit(Money.of("10", Currency.USD))
    repo.save(a)                                  # 'a' commits first

    b.debit(Money.of("20", Currency.USD))
    with pytest.raises(OptimisticLockError) as exc:
        repo.save(b)                              # 'b' is stale -> reject
    assert exc.value.wallet_id == wid
    assert exc.value.expected_version == 0
    assert exc.value.actual_version == 1

    # And the stored state reflects 'a's debit, NOT 'b's lost update.
    after = repo.find_by_id(wid)
    assert after is not None
    assert after.balance == Money.of("90", Currency.USD)


def test_find_returns_fresh_instances_no_shared_state():
    repo = InMemoryWalletRepository()
    wid = WalletId.new()
    repo.save(Wallet.open(wid, Money.of("100", Currency.USD)))
    a = repo.find_by_id(wid)
    b = repo.find_by_id(wid)
    assert a is not None and b is not None
    assert a is not b
    a.debit(Money.of("10", Currency.USD))
    assert b.balance == Money.of("100", Currency.USD), "mutation on 'a' must not leak into 'b'"
