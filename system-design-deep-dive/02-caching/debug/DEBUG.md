# DEBUG - B1 스탬피드 레이스 (실무자 디버깅 루프)
# DEBUG - B1 stampede race (the practitioner debugging loop)

> 먼저 `java B1_StampedeRace.java`를 돌리고 **가설을 세운 뒤** 읽어라.
> Run it, form a hypothesis, THEN read.

---

## 0. 증상 / The symptom

```
concurrent readers of 1 cold key = 300
db loads for that ONE key         = 13   <-- should be 1
```
- 콜드 키 하나에 DB가 **수~수십 번** 동시에 맞는다. 매 실행 숫자가
  다르다 → 타이밍 의존 → **동시성 결함**.
- 단건/순차 테스트는 통과(1회). 배포 직후 **콜드 캐시 + 동시 폭주**에서만
  터진다 — 정확히 가장 위험한 순간.
- single-flight 코드가 *있는데* 안 먹는다.

---

## 1. 재현을 결정론적으로 / Make it deterministic FIRST

- 같은 콜드 키를 64스레드 300요청이 **동시에**(latch로 일제히 release).
- `containsKey` 체크와 `put` 사이에 `Thread.yield()`로 창을 넓힘
  (프로덕션에선 창이 좁을 뿐 *똑같이 실재*).
- → 거의 항상 재현.

---

## 2. 국소화 / Localize

증상은 "한 키에 db 여러 번". db는 loader 안에서만 호출된다. loader를
만드는 곳은 single-flight 블록 하나:

```java
if (!inFlight.containsKey(key)) {   // (A) check
    f = supplyAsync(() -> db(key))  // (B) create loader
    inFlight.put(key, f);           // (C) register
} else {
    f = inFlight.get(key);
}
```

범인은 이 블록.

---

## 3. 근본 원인 / Root cause

**check-then-act가 원자적이지 않다.** 여러 스레드가 같은 콜드 키로:

```
T1: (A) containsKey? -> false
T2: (A) containsKey? -> false   <-- T1이 아직 (C) put 안 함
T3: (A) containsKey? -> false
T1: (B) supplyAsync(db)  -> db 호출 #1
T2: (B) supplyAsync(db)  -> db 호출 #2   (herd!)
T3: (B) supplyAsync(db)  -> db 호출 #3
T1: (C) put ; T2: (C) put(덮어씀) ; T3: (C) put(덮어씀)
```

`ConcurrentHashMap`이 thread-safe라는 건 `containsKey` 하나, `put`
하나가 안전하다는 뜻이지, **`containsKey`→`put` 사이가 보호된다는 뜻이
아니다.** 그 틈에 N개 스레드가 모두 "내가 첫 번째"라고 착각하고 각자
loader를 만든다. single-flight가 사실상 없는 것과 같다.

> B1(큐 모듈)의 dedup 레이스와 **완전히 같은 형태**(check-then-act /
> TOCTOU). 주제만 다르고 결함의 본질은 하나다.

---

## 4. 수정 / The fix (한 가지 아이디어: 원자적 compute)

"확인 후 등록"을 **"원자적으로 없으면-만들고-등록"** 한 연산으로 합친다.
`ConcurrentHashMap.computeIfAbsent`는 키별로 매핑 함수를 **정확히 한 번**
실행한다(원자적 test-and-set).

```java
CompletableFuture<String> f = inFlight.computeIfAbsent(key, k ->
        CompletableFuture.supplyAsync(() -> {
            String loaded = db(k);
            values.put(k, loaded);
            return loaded;
        }));
try { return f.join(); } finally { inFlight.remove(key, f); }
```

이제 콜드 키에 300개가 동시에 와도 **loader는 단 하나**, 나머지는 같은
future를 `join()`으로 기다린다. db 호출 = 1. (이게 `solution/`이 처음부터
`computeIfAbsent`를 쓰는 이유다.)

> 주의: 매핑 함수 안에서 *블로킹*하지 마라(여기선 `supplyAsync`로 즉시
> future만 반환하므로 안전). `computeIfAbsent` 함수는 짧아야 한다.

---

## 5. 재발 방지 / The regression test

`tests/T1_CacheTests.java`의 `stampedeCollapsesToOneLoader`가 이 버그를
고정한다: 300 동시 미스 → `dbCalls == 1`이라는 **불변식**을 6라운드
반복. 버그 버전이면 실패, 수정 후 영원히 초록.

> 좋은 동시성 테스트는 타이밍이 아니라 불변식을 검증한다:
> "어떤 레이스가 나도, 콜드 키 1개의 loader는 정확히 1회."

---

## 6. 한 줄 회고 / One-line postmortem (영어로)

> "Single-flight used check-then-act (`containsKey` then `put`) on a
> concurrent map; the compound is not atomic, so a herd of readers each
> started its own loader and hammered the DB on every cold key. Fixed with
> `computeIfAbsent`, which runs the mapping exactly once per key. Pinned by
> a 300-thread, 6-round single-flight invariant test."

이 6단계가 모든 디버깅의 형태다. B1(큐)와 비교해보면, 주제가 달라도
**check-then-act 레이스**의 진단·수정 루프는 동일함을 알 수 있다.
