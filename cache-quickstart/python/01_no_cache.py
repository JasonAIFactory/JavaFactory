"""
01 — NO CACHE. Every request hits the "database".
Run:  python3 01_no_cache.py
"""
import time

CALLS = 0

def get_user(user_id: int) -> dict:
    """Pretend this is a slow DB query. 100 ms per call."""
    global CALLS
    CALLS += 1
    time.sleep(0.1)
    return {"id": user_id, "name": f"user_{user_id}"}


def main() -> None:
    # We ask for 10 distinct users, each one 10 times = 100 requests.
    start = time.perf_counter()
    for _ in range(10):
        for user_id in range(10):
            get_user(user_id)
    elapsed = time.perf_counter() - start

    print(f"requests sent: 100")
    print(f"DB calls:      {CALLS}")
    print(f"wall clock:    {elapsed:.2f}s")
    print(f"-> EVERY request hit the database. expensive.")


if __name__ == "__main__":
    main()
