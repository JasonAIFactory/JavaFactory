# CHANGE - 낯선 코드에 안전하게 기능 추가하기
# CHANGE - add a feature to unfamiliar code, safely

> 목표 #1("어떤 팀에 가도 바로 기여")의 실제 근육. 새 기능을 *최소
> 변경*으로 넣고, *그걸 증명하는 테스트*를 달고, *영어 PR 설명*을 쓴다.
> 코드는 다시 짜지 않는다. **읽고, 가장 작은 변경점을 찾는다.**

---

## 티켓 / The ticket (실제 이렇게 들어온다)

> **[FEAT-214] Weighted round robin**
> 새 장비가 들어왔다. node-3는 node-0보다 3배 빠르다. 빠른 노드가 더
> 많은 트래픽을 받게 하고 싶다. 헬스체크/스티키/드레이닝 동작은 **그대로
> 유지**. 기존 `ROUND_ROBIN`도 **그대로 남겨라**(다른 서비스가 쓴다).
>
> A new box is faster. We want a backend to take traffic proportional to
> its weight. Do not change health/sticky/draining. Keep the existing
> `ROUND_ROBIN` strategy intact (other services depend on it).

---

## 작업 규칙 / The rules (실무 그대로)

1. **읽기 먼저.** `solution/Q1_LoadBalancer_Solution.java`에서 `Strategy`가
   어떻게 끼워지는지, `ROUND_ROBIN`/`LEAST_CONN`이 어디서 쓰이는지 찾아라.
   변경점이 **한 곳**임을 확인하라(그게 잘 설계된 코드의 신호).
2. **최소 변경.** `Strategy`는 인터페이스다. 새 구현 `WEIGHTED_RR` 하나를
   추가한다. 기존 함수 시그니처·기존 전략·테스트를 건드리지 마라.
3. **계약 유지.** weighted도 **healthy 스냅샷 위에서만** 골라야 한다
   (Trap A 재발 금지). null 안전.
4. **증명.** 아래 인수 테스트를 통과시켜라. 추가로 `tests/`에 같은 스타일
   테스트 1개를 더해라.
5. **영어 PR 설명**을 아래 골격으로 작성하라(이게 "인정"의 통로).

---

## 인수 기준 / Acceptance (이 테스트가 통과해야 done)

`tests/`에 `T2_WeightedRrTest.java`로 추가하고 `java`로 돌려 초록:

```java
// weight: b0=1, b1=1, b2=3  -> over 5000 picks, b2 ~= 3x of b0 (±15%),
// and an UNHEALTHY weighted backend is never returned.
static void weightedSplitsByWeightAndSkipsUnhealthy() {
    List<Backend> all = List.of(new Backend(0), new Backend(1), new Backend(2));
    int[] w = {1, 1, 3};
    Strategy s = weightedRoundRobin(w);          // <- your new factory
    int[] hits = new int[3];
    AtomicInteger rr = new AtomicInteger();
    for (int i = 0; i < 5000; i++) hits[s.pick(all, rr).id]++;
    check(hits[2] > hits[0] * 2.5 && hits[2] < hits[0] * 3.5,
          "b2 should get ~3x b0, got " + Arrays.toString(hits));

    all.get(2).healthy = false;                   // ejected
    // caller passes only the healthy snapshot, exactly like route() does:
    List<Backend> healthy = all.stream().filter(Backend::acceptsNew).toList();
    for (int i = 0; i < 2000; i++)
        check(s.pick(healthy, rr).id != 2, "weighted returned an ejected node");
}
```

> 힌트(보기 전에 30분 버텨라): 가장 단순한 정답은 weight만큼 가상
> 엔트리를 펼친 리스트에 라운드로빈, 또는 smooth weighted round robin
> (Nginx가 쓰는 방식). 둘 다 healthy 리스트 위에서 동작해야 한다.

---

## PR 설명 골격 / PR description skeleton (영어로 채워라)

```md
## What
Add a `WEIGHTED_RR` strategy so faster backends take proportionally more
traffic. Existing `ROUND_ROBIN`/`LEAST_CONN` and health/sticky/drain
behavior are unchanged.

## Why
[FEAT-214] New hardware: node-3 is ~3x faster; equal-count round robin
under-uses it.

## How
- New `Strategy` implementation only; one new factory method.
- Operates on the healthy snapshot passed by `route()` (no Trap-A regress).
- No change to existing signatures or callers.

## Testing
- New unit test: weight ratio within ±15% over 5000 picks.
- New invariant test: never returns an ejected backend under churn.
- Full existing suite still green (`java tests/T1_LoadBalancerTests.java`).

## Risk / rollback
Additive only; revert is deleting the new strategy + test. Existing
services keep using `ROUND_ROBIN` unchanged.
```

---

## 왜 이게 실무자 훈련인가 / Why this is the practitioner rep

- **읽기→최소변경**: 큰 코드에서 "어디 한 곳"을 찾는 능력(목표 #1).
- **계약 유지·하위호환**: 남의 코드를 안 깨고 더하기.
- **증명 가능한 변경**: 테스트 없는 변경은 실무에서 변경이 아니다.
- **영어 글쓰기**: PR 설명이 곧 당신이 "인정"받는 인터페이스다.

> 이 3종(`tests/` · `debug/` · `CHANGE.md`)이 매 모듈의 **Day B**.
> Day A에서 만든 같은 코드 위에서, 공부한 사람이 아니라 실무자가 된다.
