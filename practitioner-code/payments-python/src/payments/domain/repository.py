"""
PORT (hexagonal architecture). The domain DEFINES what it needs from
persistence; the infrastructure layer PROVIDES it. The application service
depends on this ABC, NEVER on a concrete repository - this is the D in
SOLID (Dependency Inversion). Result:

  - You can swap InMemory for SQLAlchemy without touching the domain or the
    application service.
  - Tests inject a fake/in-memory adapter for speed.
  - The database schema is an INFRASTRUCTURE detail, not a domain detail.

The ABC lives in the DOMAIN package on purpose: the domain owns its
contract. Implementations live in infrastructure (one per technology).
"""
from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Optional

from payments.domain.wallet import Wallet
from payments.domain.wallet_id import WalletId


class WalletRepository(ABC):
    @abstractmethod
    def find_by_id(self, wallet_id: WalletId) -> Optional[Wallet]: ...

    @abstractmethod
    def save(self, wallet: Wallet) -> None:
        """Persist the wallet using optimistic concurrency control.

        Raises OptimisticLockError if the stored version differs from
        wallet.persisted_version - meaning somebody else committed between
        our load and our save. On success, the wallet's persisted_version is
        updated to its current version (the repo calls wallet.mark_persisted()).
        """
