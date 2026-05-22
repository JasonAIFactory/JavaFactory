"""
03 — REAL TOOL: functools.lru_cache. Built into Python.
Same behavior as the manual cache, but in ONE LINE, and:
   - bounded (won't grow forever and OOM)
   - thread-safe
   - battle-tested
Run:  python3 03_lru_cache.py
"""
import time
from functools import lru_cache

CALLS = 0


@lru_cache(maxsize=128)         # <-- the entire cache, in one decorator
def get_user(user_id: int) -> dict:
    global CALLS
    CALLS += 1
    time.sleep(0.1)
    # NOTE: lru_cache needs HASHABLE args. user_id is int -> fine.
    # NOTE: it returns the SAME object for cache hits - if the caller mutates
    # it, the cached copy mutates too. Treat cached returns as read-only.
    return {"id": user_id, "name": f"user_{user_id}"}


def main() -> None:
    start = time.perf_counter()
    for _ in range(10):
        for user_id in range(10):
            get_user(user_id)
    elapsed = time.perf_counter() - start

    print(f"requests sent: 100")
    print(f"DB calls:      {CALLS}")
    print(f"wall clock:    {elapsed:.2f}s")
    print(f"cache info:    {get_user.cache_info()}")
    print(f"-> identical to manual cache, but production-grade, in 1 line.")


if __name__ == "__main__":
    main()
