"""
Wallet is an AGGREGATE ROOT (DDD): the consistency boundary that owns its
invariants. The world can only change a Wallet by calling its methods;
there are no public setters. This is the difference between an "anemic"
object (a bag of attributes a service mutates) and a real domain object
that protects itself.

Invariants enforced HERE so they cannot be bypassed by any caller:
  I1. Balance is never negative.
  I2. Balance currency never changes after creation (no cross-currency
      arithmetic on the same wallet).

`version` enables OPTIMISTIC LOCKING in the repository layer (Part 2).
Every state-changing operation bumps it so concurrent updates can be
detected and one re-tried instead of silently overwriting.

Mutability note: we use controlled mutation (no public attribute writes)
because real persistence (SQLAlchemy / Django ORM) expects entities that
can be re-hydrated and mutated.
"""
from __future__ import annotations

from payments.domain.exceptions import CurrencyMismatchError, InsufficientFundsError
from payments.domain.money import Money
from payments.domain.wallet_id import WalletId


class Wallet:
    __slots__ = ("_id", "_balance", "_version", "_persisted_version")

    def __init__(self, wallet_id: WalletId, opening_balance: Money, version: int = 0) -> None:
        if opening_balance is None or wallet_id is None:
            raise TypeError("wallet_id and opening_balance are required")
        if opening_balance.is_negative():
            raise ValueError("Opening balance must not be negative")
        self._id = wallet_id
        self._balance = opening_balance
        self._version = version
        # The version at which this wallet was last loaded or saved. Domain
        # operations do NOT touch this - only the repository, on a successful
        # save. The repo's OCC compares the stored row's version to this
        # field. This split (revision vs persisted) mirrors SQLAlchemy's
        # version_id_col and JPA's @Version.
        self._persisted_version = version

    # ---- factories ---------------------------------------------------------
    @classmethod
    def open(cls, wallet_id: WalletId, opening_balance: Money) -> "Wallet":
        """Brand-new wallet. Version starts at 0."""
        return cls(wallet_id, opening_balance, version=0)

    @classmethod
    def rehydrate(cls, wallet_id: WalletId, balance: Money, version: int) -> "Wallet":
        """Re-hydrate from persistence. Used by repositories, NOT callers."""
        return cls(wallet_id, balance, version=version)

    # ---- behavior ----------------------------------------------------------
    def debit(self, amount: Money) -> None:
        self._require_same_currency(amount)
        if not self._balance.is_greater_than_or_equal_to(amount):
            raise InsufficientFundsError(self._id, amount, self._balance)
        self._balance = self._balance.minus(amount)
        self._version += 1

    def credit(self, amount: Money) -> None:
        self._require_same_currency(amount)
        self._balance = self._balance.plus(amount)
        self._version += 1

    # ---- read-only views ---------------------------------------------------
    @property
    def id(self) -> WalletId:
        return self._id

    @property
    def balance(self) -> Money:
        return self._balance

    @property
    def version(self) -> int:
        return self._version

    @property
    def persisted_version(self) -> int:
        return self._persisted_version

    def mark_persisted(self) -> None:
        """Repository-only API: called after a successful conditional save.
        Do NOT call from application or domain code - it would defeat OCC."""
        self._persisted_version = self._version

    def _require_same_currency(self, amount: Money) -> None:
        if amount.currency is not self._balance.currency:
            raise CurrencyMismatchError(
                f"Wallet {self._id.value} holds {self._balance.currency.code} "
                f"but operation used {amount.currency.code}"
            )
