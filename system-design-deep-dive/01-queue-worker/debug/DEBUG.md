# DEBUG - B1 dedup 레이스 (실무자 디버깅 루프)
# DEBUG - B1 dedup race (the practitioner debugging loop)

> 먼저 `java B1_DedupRace.java`를 돌리고 **가설을 세운 뒤** 읽어라.
> Run it, form a hypothesis, THEN read.

---

## 0. 증상 / The symptom (네가 받는 알림)

```
ids charged MORE THAN ONCE = 196   <-- should be 0
extra (duplicate) charges  = 210   <-- should be 0
```
- 고객이 **두 번 결제**됐다. 매 실행 숫자가 다르다 → 타이밍 의존 →
  **동시성 결함**.
- "멱등성 가드"가 있고, 심지어 `ConcurrentHashMap.newKeySet()`
  **thread-safe 집합**이다. 그런데도 샌다. → "동시성 자료구조 쓰면
  안전"이라는 흔한 착각을 깨는 버그.
- A customer was charged twice. Counts differ each run → concurrency
  defect. The guard is even a thread-safe set, yet it leaks — this kills
  the "just use a concurrent collection" misconception.

---

## 1. 재현을 결정론적으로 / Make it deterministic FIRST

플래키를 노려보면 진다. 재현 확률을 1로:
- 같은 id의 중복본을 **연달아** 큐에 넣어 여러 워커가 *동시에* 같은 id를
  집게 함(실제 redelivery 버스트와 동일).
- `contains` 체크와 `add` 사이에 `Thread.yield()`로 **창을 넓힘**
  (프로덕션에선 그 창이 더 좁을 뿐 *똑같이 실재*한다 — 학습 증폭기).
- 워커 8 + id당 4중복 → 거의 항상 재현.

> "재현이 안 돼요"는 보고가 아니다. 먼저 압축 재현기를 만든다.

---

## 2. 국소화 / Localize (어디서?)

증상은 "한 id가 두 번 결제". 결제는 `chargeCustomer()` 한 곳.
그 호출을 감싼 가드는 딱 네 줄:

```java
if (processed.contains(t.id())) { continue; }   // (A) check
Thread.yield();
chargeCustomer(charges, t.id());                 // (B) side effect
processed.add(t.id());                           // (C) record
```

여기 말고 결제를 부르는 곳은 없다. 범인은 이 블록.

---

## 3. 근본 원인 / Root cause (왜?)

**check-then-act가 원자적이지 않다.** 두 워커가 같은 id를 거의 동시에
처리할 때:

```
worker-1: (A) contains(7)? -> false
worker-2: (A) contains(7)? -> false   <-- 1번이 아직 (C) 안 함
worker-1: (B) charge(7)               <-- 1회차 결제
worker-2: (B) charge(7)               <-- 2회차 결제 (중복!)
worker-1: (C) add(7)
worker-2: (C) add(7)
```

`Set`이 thread-safe라는 건 **개별 연산**(`contains` 하나, `add` 하나)이
안전하다는 뜻이지, **`contains`→`add` 사이가 보호된다는 뜻이 아니다.**
그 사이에 다른 스레드가 끼어든다. 동시성 자료구조는 원자성을
*복합 연산까지* 주지 않는다.

> 이게 모든 "check-then-act" 레이스의 본질이다(체크 후 행동 사이에 상태가
> 바뀜). TOCTOU라고도 부른다.

---

## 4. 수정 / The fix (한 가지 아이디어: 원자적 "선점")

"확인 후 처리"가 아니라 **"원자적으로 자리를 선점한 사람만 처리"**로
뒤집는다. `Set.add()`는 *새로 추가됐는지 boolean을 반환*한다 — 이게
원자적 test-and-set이다.

```java
// 처리하기 전에, 원자적으로 '내가 이 id를 맡았다'를 선언.
if (!processed.add(t.id())) {
    continue;                 // add가 false = 이미 누가 선점함 -> 건너뜀
}
chargeCustomer(charges, t.id());   // 선점에 성공한 단 한 명만 여기 도달
```

`ConcurrentHashMap.newKeySet().add()`는 내부적으로 `putIfAbsent`라서
**정확히 한 스레드만 true**를 받는다. 경합 끝.

> 주의(원래 모듈의 미묘함): 솔루션은 "성공 *후* add"로 at-least-once를
> 의도한다(처리 실패 시 재시도 가능하게). 진짜 정확히-한-번이 필요하면
> **선점(add-before) + 실패 시 명시적 해제(remove)** 또는 멱등한
> 다운스트림(upsert/유니크 키)으로 가야 한다. 트레이드오프를 알고 골라라.

---

## 5. 재발 방지 / The regression test

`tests/T1_QueueWorkerTests.java`의
`idempotency_duplicateNeverProcessedTwice`가 바로 이 버그를 고정한다:
중복을 주입하고 `processedCalls == uniqueTasks`라는 **불변식**을 5회
반복 검증. 버그 버전이면 실패한다. 수정 없이 테스트만 있어도 회귀를
잡고, 수정 후엔 영원히 초록.

> 좋은 동시성 테스트는 타이밍이 아니라 불변식을 검증한다:
> "어떤 레이스가 나도, 한 id의 부작용은 정확히 1회."

---

## 6. 한 줄 회고 / One-line postmortem (영어로)

> "Idempotency used a check-then-act (`contains` then `add`) on a
> concurrent set. Concurrent set ops are individually atomic but the
> compound is not, so two workers both passed the check and double-charged.
> Fixed by claiming the id atomically with `set.add()`'s boolean (only one
> thread wins). Pinned by a duplicate-injection invariant test."

이 6단계가 모든 디버깅의 형태다. 주제가 바뀌어도 루프는 같다.
