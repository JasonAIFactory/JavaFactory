"""
Domain exceptions - type-meaningful errors are part of a good domain language.
A caller can `except InsufficientFunds as e` and use e.requested / e.available
without parsing a message string. Generic Exception is lossy and untestable.
"""
from __future__ import annotations

from typing import TYPE_CHECKING

if TYPE_CHECKING:  # avoid circular imports while preserving type info
    from payments.domain.money import Money
    from payments.domain.wallet_id import WalletId


class CurrencyMismatchError(Exception):
    """Raised when an operation mixes two different currencies."""


class OptimisticLockError(Exception):
    """OCC failure: somebody updated this wallet between our load and save.

    The CORRECT response is usually to reload and retry (see WalletService).
    We carry ids/versions so logs say what conflicted.

    "Optimistic" because we did NOT take a lock at load time; we BET no one
    else would write. When the bet loses, this is the signal.
    """

    def __init__(self, wallet_id: "WalletId", expected_version: int, actual_version: int) -> None:
        super().__init__(
            f"Optimistic lock failure on wallet {wallet_id.value}: "
            f"expected version {expected_version} but stored is {actual_version}"
        )
        self.wallet_id = wallet_id
        self.expected_version = expected_version
        self.actual_version = actual_version


class InsufficientFundsError(Exception):
    """Raised by Wallet.debit when the requested amount exceeds the balance.

    Carries context so logs/ops can diagnose without grepping prose.
    """

    def __init__(self, wallet_id: "WalletId", requested: "Money", available: "Money") -> None:
        super().__init__(
            f"Wallet {wallet_id.value} has {available} but {requested} was requested"
        )
        self.wallet_id = wallet_id
        self.requested = requested
        self.available = available
