"""
Currency as an Enum so the type system itself forbids "USDD" or "us-dollar".
Each currency knows its own fraction digits - this drives Money's canonical
scale so equality is well-defined (see money.py).

Why an Enum (and not a str like "USD"):
  - IDE auto-completion + mypy can check exhaustiveness.
  - No "stringly typed" bugs ("usd" vs "USD" vs " USD ").
  - One place to add a new currency.
"""
from __future__ import annotations

from enum import Enum


class Currency(Enum):
    USD = ("USD", 2)
    EUR = ("EUR", 2)
    JPY = ("JPY", 0)   # yen has no minor unit - real bug source if hardcoded "2"

    def __init__(self, code: str, fraction_digits: int) -> None:
        self.code = code
        self.fraction_digits = fraction_digits
