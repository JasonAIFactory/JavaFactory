"""
Typed identifier (a "tiny type"). NEVER pass raw UUID/str across the domain -
it lets you swap a customer_id for a wallet_id silently. A WalletId can ONLY
be a WalletId; mypy and runtime equality both stop the bug.

(typing.NewType would help static checkers but not runtime - a frozen
dataclass is a real distinct type that also enables structured comparison.)
"""
from __future__ import annotations

from dataclasses import dataclass
from uuid import UUID, uuid4


@dataclass(frozen=True, slots=True)
class WalletId:
    value: UUID

    @classmethod
    def new(cls) -> "WalletId":
        return cls(uuid4())

    def __str__(self) -> str:
        return str(self.value)
