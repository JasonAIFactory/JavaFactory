# DEBUG - B1 라우팅 레이스 (실무자 디버깅 루프)
# DEBUG - B1 routing race (the practitioner debugging loop)

> 먼저 `java B1_RoutingRace.java`를 직접 돌리고 **가설을 세운 뒤** 읽어라.
> 디버깅은 "코드를 노려보기"가 아니라 **방법**이다. 그 방법을 연습한다.
> Run it, form a hypothesis, THEN read. Debugging is a METHOD, not staring.

---

## 0. 증상 / The symptom (네가 받는 알림)

```
EXCEPTIONS in route= 143      <-- 사용자는 500을 본다
routed to UNHEALTHY= 14394    <-- 죽은 서버로 트래픽이 샌다
```
- 매 실행마다 **숫자가 다르다** → 타이밍 의존 → **동시성 결함**.
- 로컬 단건 테스트는 통과한다. **부하 + 멤버십 변동**에서만 터진다.
- 이 두 줄(① 예외 ② 비정상 라우팅)이 *서로 다른 두 증상이지만 원인은
  하나*다. 그게 핵심 통찰.
- The counts change every run → timing-dependent → a concurrency defect.
  Single-call tests pass; it only breaks under load + membership churn.
  Two symptoms, one root cause.

---

## 1. 재현을 결정론적으로 / Make it deterministic FIRST

플래키 버그를 그냥 쳐다보면 진다. **재현 확률을 1에 가깝게** 만든다:
- 스레드 수↑, 요청 수↑ (이미 32 스레드 / 200k — 거의 항상 터짐).
- 변동을 빠르게: `flapper`가 2ms마다 health 토글 (이미 그렇게 함).
- 더 확실히 하려면 `route()`의 `size()`와 `get()` **사이에**
  `Thread.yield()`를 잠깐 끼워 창을 벌린다(원인 확인용, 커밋 금지).

> 교훈: "재현이 안 돼요"는 보고가 아니다. 먼저 **압축 재현기**를 만든다.
> "Cannot reproduce" is not a report. Build a压축 repro harness first.

---

## 2. 국소화 / Localize (어디서?)

- 예외의 **스택트레이스**를 본다(잡지 말고 한 번 찍어보면):
  `IndexOutOfBoundsException ... at java.util.ArrayList.get`
  → `LoadBalancer.route()`의 `healthy.get(idx)` 줄.
- `route()`는 딱 두 줄이다:
  ```java
  int idx = (rr.getAndIncrement() & Integer.MAX_VALUE) % healthy.size();
  return healthy.get(idx);
  ```
- 누가 `healthy`를 동시에 만지나? → 헬스 스레드:
  ```java
  healthy.clear();                       // 리스트가 잠깐 size 0/줄어듦
  for (Backend b : all) if (b.healthy) healthy.add(b);
  ```

> 도구: 예외면 스택트레이스, 멈춤이면 `jstack`(스레드 덤프), 메모리면
> 힙 덤프 + `jmap`. 여기선 스택트레이스가 정답으로 직행시킨다.

---

## 3. 근본 원인 / Root cause (왜?)

`route()`는 **공유 가변 리스트를 동기화 없이 읽는다**, 그리고 동시에
헬스 스레드가 그 리스트를 `clear()` 후 다시 채운다.

두 가지가 동시에 깨진다(같은 원인):
1. **size–get 사이의 경합**: `size()`로 idx를 계산한 직후 `clear()`가
   끼면 `get(idx)`가 `IndexOutOfBoundsException`. → ① 예외.
2. **stale read**: `clear()` 직후 아직 다시 채우기 전이거나, 방금 빠진
   백엔드를 다른 스레드가 그 찰나에 읽음. → ② 죽은 노드로 라우팅.

핵심: `ArrayList`는 thread-safe가 아니다. **"읽고-쓰기를 한 구조 위에서
다른 스레드와 겹치면" 그 자체가 버그.** 락이 없으면 원자성도 없다.

---

## 4. 수정 / The fix (한 가지 아이디어: 스냅샷)

리스트를 "그 자리에서 수정"하지 말고, **불변 스냅샷을 통째로 교체**한다.
읽는 쪽은 한 번 읽은 참조로 끝까지 간다(중간에 안 바뀜).

```java
// 공유 상태를 volatile 참조 1개로. 교체는 원자적.
private volatile List<Backend> healthy = List.copyOf(allInitially);

// health 스레드: 새 리스트를 만들어 '통째로' 갈아끼움 (in-place 수정 X)
List<Backend> next = new ArrayList<>();
for (Backend b : all) if (b.healthy) next.add(b);
this.healthy = List.copyOf(next);          // atomic publish

// route(): 참조를 '한 번' 읽고 그 스냅샷으로만 계산
List<Backend> snap = this.healthy;         // immutable, won't change under us
if (snap.isEmpty()) return null;           // 경계: 전부 죽었을 때
return snap.get((rr.getAndIncrement() & Integer.MAX_VALUE) % snap.size());
```

왜 이게 맞나: 읽는 쪽은 *절대 변하지 않는* 리스트를 본다 → size/get
불일치 불가능(① 해결). 갓 빠진 노드는 다음 publish 전까지만 보이고
그 window가 원래 의도된 헬스체크 지연일 뿐(② 정상화). `volatile`이
교체의 가시성을 보장한다.

> 대안: `CopyOnWriteArrayList`(쓰기 드물고 읽기 폭주에 적합 — LB가 딱
> 그 케이스). 어느 쪽이든 **불변 스냅샷 + 원자 교체**가 패턴이다.
> 이건 `solution/`이 `healthySnapshot()`으로 매 호출 새 리스트를
> 반환하는 이유와 같은 원리다.

---

## 5. 재발 방지 / The regression test (이게 실무자다)

수정만 하고 끝내면 6개월 뒤 누가 또 깬다. **버그를 고정하는 테스트**를
남긴다. 이미 `tests/T1_LoadBalancerTests.java`에 그 모양이 있다:

- `underLoad_neverRoutesToEjectedNode` — 부하 중 ejected 노드로 절대
  안 간다는 **불변식**을 32스레드로 검증. 버그 버전이면 실패한다.
- 추가로 "route()는 어떤 동시 변동에도 예외를 던지지 않는다"를 같은
  스타일로 한 개 더 넣으면 ①까지 핀으로 고정된다.

> 좋은 동시성 테스트는 *타이밍*이 아니라 *불변식*을 검증한다:
> "무슨 레이스가 나든, X는 절대 일어나지 않는다."

---

## 6. 한 줄 회고 / One-line postmortem (영어로 써보기)

> "Under health churn, `route()` read a shared `ArrayList` while the
> health thread cleared and refilled it, causing `IndexOutOfBounds` and
> stale routing to ejected nodes. Fixed by publishing an immutable
> snapshot via a `volatile` reference and reading it once per request.
> Pinned by a 32-thread invariant test."

이 6단계(증상→결정론적 재현→국소화→근본원인→수정→재발방지+회고)가
**모든 디버깅의 형태**다. 주제가 바뀌어도 루프는 같다.
