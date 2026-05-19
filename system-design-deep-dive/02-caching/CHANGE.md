# CHANGE - 낯선 캐시 코드에 안전하게 기능 추가
# CHANGE - add a feature to unfamiliar cache code, safely

> 목표 #1("어떤 팀에 가도 바로 기여")의 실제 근육. 최소 변경 + 증명
> 테스트 + 영어 PR. 코드 다시 안 짠다. **읽고, 가장 작은 변경점을 찾는다.**

---

## 티켓 / The ticket

> **[FEAT-402] TTL jitter to prevent a cache avalanche**
> 인시던트: 트래픽 급증 때 수만 개 키를 **거의 동시에** 캐싱했더니, TTL이
> 모두 같아 **정확히 같은 순간 한꺼번에 만료** → 동시에 전부 DB로 →
> 애벌랜치(`evolution/S4` 참고). 각 엔트리의 TTL에 **±10% 지터**를 줘서
> 만료 시각을 흩뜨려라. hit/LRU/single-flight/null-cache 동작은 **그대로**.
> 기존 테스트도 **그대로 통과**.
>
> Add ±10% jitter to each entry's TTL so a batch cached together does not
> all expire at the same instant (avalanche). Keep all other behavior.

---

## 작업 규칙 / The rules

1. **읽기 먼저.** `solution/Q1_Cache_Solution.java`에서 TTL이 정해지는
   유일한 지점을 찾아라 — `Entry` 생성자의 `expiresAt`. 변경점이 거기
   하나임을 확인(잘 설계된 코드는 변경점이 하나다).
2. **최소 변경.** `Entry`의 만료 계산에만 지터를 더한다. `get`/`lookup`/
   `store`/single-flight/`removeEldestEntry` 불변.
3. **계약 유지.** 지터는 **±10%**, TTL은 항상 **양수**. 평균은 대략
   기존 TTL 유지(편향 금지). 만료 자체는 여전히 동작(`expired()` 그대로).
4. **증명.** 아래 인수 테스트를 `tests/`에 `T2_TtlJitterTest.java`로
   추가, `java`로 초록. `T1`도 여전히 초록.
5. **영어 PR 설명**을 골격대로.

---

## 인수 기준 / Acceptance

```java
// 1) jittered expiry spreads (NOT all equal) for a batch cached together
// 2) every ttl stays within [0.9*ttl, 1.1*ttl] and > 0
// 3) mean is ~ ttl (no systematic bias)
static void ttlIsJitteredWithinTenPercent() {
    long ttl = 1000;
    long[] samples = new long[5000];
    long sum = 0;
    for (int i = 0; i < samples.length; i++) {
        long eff = effectiveTtl(ttl);            // your jittered ttl calc
        check(eff > 0, "ttl must stay positive");
        check(eff >= 900 && eff <= 1100, "ttl outside +/-10%: " + eff);
        samples[i] = eff; sum += eff;
    }
    double mean = sum / (double) samples.length;
    check(Math.abs(mean - ttl) < ttl * 0.03, "mean drifted from ttl: " + mean);
    long first = samples[0]; boolean spread = false;
    for (long s : samples) if (s != first) { spread = true; break; }
    check(spread, "jitter produced NO spread (avalanche not prevented)");
}
```

> 힌트(30분 버틴 뒤): 지터를 `Entry` 생성 시점에 한 번 계산해 고정해야
> 한다(매 `expired()` 호출마다 새로 뽑으면 만료 시각이 흔들려 버그).
> "언제 무작위를 뽑느냐"가 핵심 — 이게 사양 해석 연습이다.

---

## PR 설명 골격 / PR description skeleton (영어로 채워라)

```md
## What
Add +/-10% jitter to each cache entry's TTL so entries cached together do
not all expire at the same instant.

## Why
[FEAT-402] Incident: a burst cached ~tens of thousands of keys with an
identical TTL; they expired simultaneously and stampeded the DB
(cache avalanche, see evolution/S4).

## How
- Single change: jitter computed once in the Entry constructor when
  expiresAt is set. No change to get/lookup/store/single-flight/LRU.

## Testing
- New test T2: per-entry ttl within +/-10%, positive, mean ~ ttl, spread
  is non-zero.
- Existing T1 suite still green (TTL self-heal / LRU / single-flight /
  null-cache intact).

## Risk / rollback
Localized to expiry calc; rollback = remove the jitter term. Mean TTL
unchanged so cache hit ratio is not materially affected.
```

---

## 왜 이게 실무자 훈련인가

- **읽기→단일 변경점**: 큰 코드에서 "딱 한 곳"(Entry.expiresAt)을 찾기.
- **사양 해석**: "무작위를 *언제* 뽑나"(생성 시 1회 vs 매 체크) 모호함을
  인수 기준으로 푼다 — 틀리면 미묘한 버그.
- **회귀 안전**: 기존 T1이 계속 초록 = 남의 동작 안 깸.
- **영어 글쓰기**: PR 설명이 곧 인정받는 인터페이스.

> `tests/` · `debug/` · `CHANGE.md` = 이 모듈의 **Day B**. Day A에서 만든
> 같은 코드 위에서, 공부한 사람이 아니라 실무자가 된다.
