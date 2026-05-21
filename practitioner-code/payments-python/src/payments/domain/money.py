"""
Money is a VALUE OBJECT (DDD). Defining traits:
  - Immutable: every operation returns a new Money, never mutates.
  - Identity is by VALUE, not by reference (10 USD == 10 USD).
  - It enforces its own invariants (no float for money, no cross-currency math).

Implemented as a frozen dataclass so __eq__/__hash__ are auto-generated and
correct - PROVIDED we canonicalize the Decimal scale at construction, because
Decimal("10.00") == Decimal("10") is True but their str() differs and they
hash the same; we still normalize for predictable output and arithmetic.

Why Decimal, not float:
  0.1 + 0.2 == 0.30000000000000004 in float. In money that's a bug at scale.
  Decimal is exact and the production default.
"""
from __future__ import annotations

from dataclasses import dataclass
from decimal import ROUND_HALF_EVEN, Decimal
from typing import Union

from payments.domain.currency import Currency
from payments.domain.exceptions import CurrencyMismatchError


@dataclass(frozen=True, slots=True)
class Money:
    amount: Decimal
    currency: Currency

    def __post_init__(self) -> None:
        if self.amount is None or self.currency is None:           # fail fast at the boundary
            raise TypeError("amount and currency are required")
        # Canonicalize to the currency's scale so 10 USD and 10.00 USD print
        # the same and round-trip through serialization predictably.
        q = Decimal(10) ** -self.currency.fraction_digits
        normalized = self.amount.quantize(q, rounding=ROUND_HALF_EVEN)
        # frozen dataclass: bypass __setattr__ via object.__setattr__
        object.__setattr__(self, "amount", normalized)

    # ---- factories: convenience constructors that read at the call site ----
    @classmethod
    def of(cls, amount: Union[str, int, Decimal], currency: Currency) -> "Money":
        return cls(Decimal(str(amount)), currency)

    @classmethod
    def zero(cls, currency: Currency) -> "Money":
        return cls(Decimal(0), currency)

    # ---- arithmetic: returns NEW Money (immutability) ----------------------
    def plus(self, other: "Money") -> "Money":
        self._require_same_currency(other)
        return Money(self.amount + other.amount, self.currency)

    def minus(self, other: "Money") -> "Money":
        self._require_same_currency(other)
        return Money(self.amount - other.amount, self.currency)

    def is_negative(self) -> bool:
        return self.amount < 0

    def is_greater_than_or_equal_to(self, other: "Money") -> bool:
        self._require_same_currency(other)
        return self.amount >= other.amount

    def _require_same_currency(self, other: "Money") -> None:
        if self.currency is not other.currency:
            raise CurrencyMismatchError(
                f"Cannot operate on {self.currency.code} and {other.currency.code}"
            )

    def __str__(self) -> str:
        return f"{self.amount} {self.currency.code}"
