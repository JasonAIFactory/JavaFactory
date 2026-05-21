"""Application-layer tests: unit + the zero-loss concurrency invariant."""
from concurrent.futures import ThreadPoolExecutor, wait
from threading import Event

import pytest

from payments.application.wallet_service import WalletNotFoundError, WalletService
from payments.domain.currency import Currency
from payments.domain.exceptions import InsufficientFundsError, OptimisticLockError
from payments.domain.money import Money
from payments.domain.wallet_id import WalletId
from payments.infrastructure.inmemory import InMemoryWalletRepository


def test_open_wallet_returns_usable_id():
    svc = WalletService(InMemoryWalletRepository())
    wid = svc.open_wallet("100", Currency.USD)
    assert svc.balance(wid) == Money.of("100", Currency.USD)


def test_debit_and_credit_happy_paths():
    svc = WalletService(InMemoryWalletRepository())
    wid = svc.open_wallet("100", Currency.USD)
    assert svc.debit(wid, Money.of("30", Currency.USD)) == Money.of("70", Currency.USD)
    assert svc.credit(wid, Money.of("25", Currency.USD)) == Money.of("95", Currency.USD)


def test_domain_exceptions_surface_unchanged():
    svc = WalletService(InMemoryWalletRepository())
    wid = svc.open_wallet("10", Currency.USD)
    with pytest.raises(InsufficientFundsError):
        svc.debit(wid, Money.of("50", Currency.USD))


def test_missing_wallet_raises_application_exception():
    svc = WalletService(InMemoryWalletRepository())
    with pytest.raises(WalletNotFoundError):
        svc.balance(WalletId.new())


def test_concurrent_debits_zero_loss_invariant():
    """THE invariant for Part 2: under concurrent debits on the same wallet,
    OCC + retry must converge to the correct final balance with ZERO lost
    updates. Without OCC, a naive read-modify-write loses writes."""
    repo = InMemoryWalletRepository()
    # Generous retries so the test pins the property, not luck.
    svc = WalletService(repo, max_retries=100)
    wid = svc.open_wallet("100", Currency.USD)

    threads = 8
    debits_per_thread = 5
    start = Event()

    def worker() -> None:
        start.wait()
        for _ in range(debits_per_thread):
            svc.debit(wid, Money.of("1", Currency.USD))

    with ThreadPoolExecutor(max_workers=threads) as pool:
        futures = [pool.submit(worker) for _ in range(threads)]
        start.set()
        for f in futures:
            f.result()    # re-raises any exception

    # 8 threads * 5 = 40 debits of $1 from a $100 wallet -> $60.
    assert svc.balance(wid) == Money.of("60", Currency.USD), \
        "ZERO-LOSS INVARIANT broken under contention"


def test_retries_exhausted_surface_the_conflict():
    """When the retry budget is gone, OptimisticLockError MUST surface.

    We prove this with a TEST DOUBLE: a fake repo that always conflicts.
    This is THE benefit of depending on the port (an ABC) instead of a
    concrete class - we can swap in a deterministic stub for tests and
    isolate the policy under test (retry budget) from everything else.
    A flaky concurrency test would not prove the policy; this one does."""
    from payments.domain.money import Money as _Money

    class AlwaysConflictRepo:
        def find_by_id(self, wid):
            from payments.domain.wallet import Wallet
            return Wallet.rehydrate(wid, _Money.of("100", Currency.USD), version=0)

        def save(self, wallet):
            raise OptimisticLockError(wallet.id, expected_version=0, actual_version=99)

    svc = WalletService(AlwaysConflictRepo(), max_retries=3)   # type: ignore[arg-type]
    with pytest.raises(OptimisticLockError) as exc:
        svc.debit(WalletId.new(), Money.of("1", Currency.USD))
    assert exc.value.actual_version == 99   # the last conflict's info reaches the caller
