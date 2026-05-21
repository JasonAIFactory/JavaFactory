"""
ADAPTER (hexagonal architecture). One concrete implementation of the
WalletRepository port. Lives in infrastructure - the only place that knows
we are using an in-memory dict. Tomorrow's SQLAlchemy adapter goes here too;
nothing in domain or application changes.

Thread-safety: a single Lock guards the check-and-swap. We read the stored
version, compare to wallet.persisted_version (set when the wallet was last
loaded or saved), and only swap the row if they match - otherwise raise.
Real SQL ORMs emit  UPDATE ... WHERE id=? AND version=?  and check the
affected row count. Same semantics, different mechanism.

We store an immutable Snapshot (a dataclass), not the Wallet itself, so two
concurrent holders of the same id cannot accidentally share mutable state.
"""
from __future__ import annotations

import threading
from dataclasses import dataclass
from typing import Dict, Optional

from payments.domain.exceptions import OptimisticLockError
from payments.domain.money import Money
from payments.domain.repository import WalletRepository
from payments.domain.wallet import Wallet
from payments.domain.wallet_id import WalletId


@dataclass(frozen=True, slots=True)
class _Snapshot:
    balance: Money
    version: int


class InMemoryWalletRepository(WalletRepository):
    def __init__(self) -> None:
        self._store: Dict[WalletId, _Snapshot] = {}
        self._lock = threading.Lock()

    def find_by_id(self, wallet_id: WalletId) -> Optional[Wallet]:
        with self._lock:
            snap = self._store.get(wallet_id)
        if snap is None:
            return None
        # Re-hydrate as a fresh Wallet - callers must not share mutable state.
        return Wallet.rehydrate(wallet_id, snap.balance, snap.version)

    def save(self, wallet: Wallet) -> None:
        with self._lock:
            existing = self._store.get(wallet.id)
            expected = wallet.persisted_version
            actual = existing.version if existing is not None else 0
            is_create = existing is None and expected == 0 and wallet.version == 0
            is_update = existing is not None and actual == expected
            if not (is_create or is_update):
                raise OptimisticLockError(wallet.id, expected, actual)
            self._store[wallet.id] = _Snapshot(wallet.balance, wallet.version)
        # Update the in-memory aggregate so the NEXT save uses the new
        # version as its expected prior. Real ORMs do this on flush.
        wallet.mark_persisted()
