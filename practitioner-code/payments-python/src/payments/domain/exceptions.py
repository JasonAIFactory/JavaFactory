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
