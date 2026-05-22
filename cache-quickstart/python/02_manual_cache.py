"""
02 — MANUAL CACHE. A plain Python dict that we manage by hand.
The "tool" is literally a dict. That's all a cache is.
Run:  python3 02_manual_cache.py
"""
import time

CALLS = 0
_cache: dict[int, dict] = {}    # <-- the cache. it's just a dict.


def get_user(user_id: int) -> dict:
    # 1) check the cache first
    if user_id in _cache:
        return _cache[user_id]          # HIT - return remembered value

    # 2) MISS: run the slow path
    global CALLS
    CALLS += 1
    time.sleep(0.1)
    result = {"id": user_id, "name": f"user_{user_id}"}

    # 3) remember for next time
    _cache[user_id] = result
    return result


def main() -> None:
    start = time.perf_counter()
    for _ in range(10):
        for user_id in range(10):
            get_user(user_id)
    elapsed = time.perf_counter() - start

    print(f"requests sent: 100")
    print(f"DB calls:      {CALLS}   <-- only the FIRST time per id")
    print(f"wall clock:    {elapsed:.2f}s")
    print(f"-> 90 of 100 requests came from memory. a dict IS a cache.")


if __name__ == "__main__":
    main()
