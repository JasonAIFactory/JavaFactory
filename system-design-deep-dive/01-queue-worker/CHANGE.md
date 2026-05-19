# CHANGE - 낯선 큐 코드에 안전하게 기능 추가
# CHANGE - add a feature to unfamiliar queue code, safely

> 목표 #1("어떤 팀에 가도 바로 기여")의 실제 근육. 최소 변경 + 증명
> 테스트 + 영어 PR. 코드 다시 안 짠다. **읽고, 가장 작은 변경점을 찾는다.**

---

## 티켓 / The ticket

> **[FEAT-318] Cap the retry backoff**
> 인시던트 회고에서: 다운스트림이 10분간 죽었을 때 attempt가 커지며
> 백오프가 **수십 초~분 단위**로 뛰었다(`50 * 2^attempt`). 한 번 죽었던
> 작업이 너무 늦게 재시도돼 SLA를 깼다. 백오프에 **상한(cap)**을 둬라.
> 예: `min(base*2^attempt, MAX_BACKOFF_MS)`. 지터·재시도 횟수·DLQ 동작은
> **그대로**. 기존 테스트도 **그대로 통과**해야 한다.
>
> Add an upper bound to the exponential backoff so a long outage does not
> push retries minutes out. Keep jitter / max-attempts / DLQ unchanged.

---

## 작업 규칙 / The rules

1. **읽기 먼저.** `solution/Q1_QueueWorker_Solution.java`에서
   `backoffWithJitter(int attempt)`가 유일한 변경점임을 확인하라
   (잘 설계된 코드는 변경점이 하나다).
2. **최소 변경.** 그 메서드 안에서만. 시그니처·호출부·다른 로직 불변.
3. **계약 유지.** 여전히 `>= 0`, 여전히 ±50% 지터. 단 **절대 `MAX` 초과
   금지**. `MAX`는 상수로(예: `2_000`ms).
4. **증명.** 아래 인수 테스트를 `tests/`에 `T2_BackoffCapTest.java`로
   추가하고 `java`로 돌려 초록. `T1`도 여전히 초록이어야 한다.
5. **영어 PR 설명**을 골격대로.

---

## 인수 기준 / Acceptance

```java
// 1) never exceeds the cap, even at huge attempt numbers
// 2) still non-negative
// 3) low attempts are UNCHANGED (cap must not lower small backoffs)
static final long MAX_BACKOFF_MS = 2_000;
static void backoffIsCapped() {
    for (int attempt = 0; attempt < 30; attempt++)
        for (int i = 0; i < 1000; i++) {
            long b = backoffWithJitter(attempt);          // your capped version
            check(b >= 0, "negative");
            check(b <= MAX_BACKOFF_MS + MAX_BACKOFF_MS/2,  // cap + its own jitter ceiling
                  "exceeded cap at attempt " + attempt + ": " + b);
        }
    // attempt 0 (base 50, well under cap) must still be in the old window
    long base0 = 50;
    for (int i = 0; i < 1000; i++) {
        long b = backoffWithJitter(0);
        check(b >= base0/2 && b <= base0 + base0/2, "small backoff was distorted: " + b);
    }
}
```

> 힌트(30분 버틴 뒤): 캡을 *지터 전에* 적용할지 *후에* 적용할지 결정해야
> 한다. "캡한 base에 지터" vs "지터한 값에 캡" — 결과 분포가 다르다.
> 인수 기준이 어느 쪽을 요구하는지 읽고 골라라(이게 사양 해석 연습이다).

---

## PR 설명 골격 / PR description skeleton (영어로 채워라)

```md
## What
Cap exponential backoff at MAX_BACKOFF_MS so retries during a long
downstream outage do not get pushed minutes into the future.

## Why
[FEAT-318] Incident: a ~10min outage pushed retry delays to tens of
seconds, breaking the processing SLA for previously-failed tasks.

## How
- Single change in `backoffWithJitter`: bound the base by MAX_BACKOFF_MS
  before applying jitter. No signature/caller/DLQ/attempt changes.

## Testing
- New test T2: capped at all attempts, non-negative, small backoffs
  unchanged.
- Existing T1 suite still green (zero-loss / DLQ / idempotency intact).

## Risk / rollback
Pure function change, additive constant. Rollback = remove the min().
Behavior for small attempts is provably unchanged.
```

---

## 왜 이게 실무자 훈련인가

- **읽기→단일 변경점**: 큰 코드에서 "딱 한 곳"을 찾는 능력(목표 #1).
- **사양 해석**: "캡을 지터 전/후?" 모호함을 인수 기준으로 푼다.
- **회귀 안전**: 기존 T1이 계속 초록 = 남의 동작 안 깸.
- **영어 글쓰기**: PR 설명이 곧 인정받는 인터페이스.

> `tests/` · `debug/` · `CHANGE.md` = 이 모듈의 **Day B**. Day A에서 만든
> 같은 코드 위에서, 공부한 사람이 아니라 실무자가 된다.
