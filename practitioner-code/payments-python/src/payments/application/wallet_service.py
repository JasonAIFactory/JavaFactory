"""
APPLICATION SERVICE (hexagonal architecture). Orchestrates use cases.

What this layer DOES:
  - Load aggregates by id (via the repository port).
  - Call domain methods to enforce invariants (debit/credit).
  - Persist via the repository.
  - Handle OCC conflicts with bounded retry.
  - Translate "wallet not found" into the application's language.

What this layer DOES NOT do:
  - Business rules (those live in Wallet/Money).
  - Know about HTTP, JSON, SQLAlchemy, or any framework. Inputs are domain
    types. Part 5 wires a FastAPI router on top - the router translates
    HTTP <-> these method calls, nothing more.

Why retry here (not in domain, not in repository): retry is an APPLICATION
POLICY (how hard to try). The domain is pure rules; the repository reports
facts ("conflict"); the application decides what to do.
"""
from __future__ import annotations

from decimal import Decimal
from typing import Callable, TypeVar

from payments.domain.currency import Currency
from payments.domain.exceptions import OptimisticLockError
from payments.domain.money import Money
from payments.domain.repository import WalletRepository
from payments.domain.wallet import Wallet
from payments.domain.wallet_id import WalletId

T = TypeVar("T")


class WalletNotFoundError(Exception):
    """Application-level: this id has no wallet (translate to HTTP 404 later)."""


class WalletService:
    def __init__(self, wallets: WalletRepository, max_retries: int = 3) -> None:
        # Dependency Injection via constructor: explicit, easy to test, no
        # hidden globals. Depends on the PORT (abstract), not on any
        # concrete repo (DIP).  max_retries is a POLICY, also injected.
        if max_retries < 1:
            raise ValueError("max_retries must be >= 1")
        self._wallets = wallets
        self._max_retries = max_retries

    def open_wallet(self, opening_amount: str, currency: Currency) -> WalletId:
        wid = WalletId.new()
        w = Wallet.open(wid, Money(Decimal(opening_amount), currency))
        self._wallets.save(w)
        return wid

    def debit(self, wallet_id: WalletId, amount: Money) -> Money:
        def action() -> Money:
            w = self._load_or_raise(wallet_id)
            w.debit(amount)
            self._wallets.save(w)
            return w.balance
        return self._with_optimistic_retry(action)

    def credit(self, wallet_id: WalletId, amount: Money) -> Money:
        def action() -> Money:
            w = self._load_or_raise(wallet_id)
            w.credit(amount)
            self._wallets.save(w)
            return w.balance
        return self._with_optimistic_retry(action)

    def balance(self, wallet_id: WalletId) -> Money:
        return self._load_or_raise(wallet_id).balance

    # ---- internals --------------------------------------------------------
    def _load_or_raise(self, wallet_id: WalletId) -> Wallet:
        w = self._wallets.find_by_id(wallet_id)
        if w is None:
            raise WalletNotFoundError(f"wallet not found: {wallet_id.value}")
        return w

    def _with_optimistic_retry(self, action: Callable[[], T]) -> T:
        last: OptimisticLockError | None = None
        for _ in range(self._max_retries):
            try:
                return action()
            except OptimisticLockError as e:
                last = e
        assert last is not None
        raise last   # give up; let the caller (e.g. HTTP layer) translate to 409
