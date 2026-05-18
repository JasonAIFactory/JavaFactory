# 02 - Caching 딥다이브 (한글 ↔ English 병기)

> 읽는 법: 한글로 개념을 잡고, 바로 아래 영어로 같은 말을 다시 본다.
> Read the Korean to get the idea, then the English for the same idea.

> 학습 전: `QUIZ.md`의 **진단 퀴즈 8문제**부터.
> Before studying: do the 8 diagnostic questions in `QUIZ.md` first.

---

## 1. 캐시는 "책상 위 메모지"다 / A cache is a "sticky note on your desk"

**KR.** 누가 전화로 "김 부장 내선번호?"를 자주 묻는다. 매번 두꺼운
전화번호부(DB)를 펴서 찾으면 느리다. 그래서 자주 묻는 번호를 **포스트잇에
적어 모니터에 붙여 둔다.** 다음엔 포스트잇만 보면 끝(캐시 히트). 없는
번호만 전화번호부를 편다(캐시 미스). 단, 그 사람 번호가 바뀌면 포스트잇은
**거짓말**을 한다 — 그래서 떼거나(무효화) 유통기한(TTL)을 둔다.

**EN.** People keep asking "what's Kim's extension?". Opening the thick
phone book (the db) every time is slow. So you write the common numbers
on a **sticky note on your monitor**. Next time you just read the note
(cache hit). Only unknown numbers need the phone book (cache miss). But
if the number changes, the note now **lies** — so you remove it
(invalidation) or give it an expiry (TTL).

| 비유 / Analogy | 시스템 / System |
| --- | --- |
| 전화번호부 / phone book | 원본 데이터 소스 / source of truth (db, API) |
| 포스트잇 / sticky note | 캐시 / cache |
| 붙어 있던 번호 발견 / note had it | 캐시 히트 / cache hit |
| 없어서 전화번호부 펴기 / not on note | 캐시 미스 / cache miss |
| 포스트잇 다 차서 오래된 것 버림 / note full | 축출 / eviction (LRU/LFU) |
| 유통기한 지나면 버림 / expiry | TTL (time to live) |
| 번호 바뀌어 떼기 / number changed | 무효화 / invalidation |
| 거짓 번호 / wrong number | 스테일 데이터 / stale data |

> **파인만 테스트:** 표를 덮고 포스트잇 비유로 90초 설명. /
> Cover the table; explain with the sticky-note story in 90s.

---

## 2. 왜 쓰는가 — 안 쓰면 뭐가 터지나 / Why — what breaks without it

**1) 느림 / Slow**
- KR: 매 요청이 무거운 쿼리. 사용자 응답이 항상 쿼리 지연만큼.
- EN: Every request runs the heavy query; latency is always the full
  query latency.

**2) DB가 같은 일을 N배 / DB does the same work N times**
- KR: 인기 상품 1개를 초당 1만 명이 보면 DB가 같은 행을 1만 번 읽는다.
- EN: 10k users/s viewing one hot product = the db reads the same row
  10k times/s. (read amplification)

**3) 비용 / Cost**
- KR: 읽기 확장을 전부 DB 증설로 막으면 돈이 기하급수. 캐시 1대가 DB
  여러 대를 대신한다.
- EN: Scaling reads purely by adding db replicas is exponentially
  expensive; one cache node replaces many db nodes.

**4) 스파이크에 DB 다운 / A spike kills the db**
- KR: 캐시가 충격을 흡수하지 못하면 트래픽 급증 시 DB가 먼저 죽는다.
- EN: Without a cache absorbing the shock, a traffic spike takes the db
  down first.

> 한 줄 / One line: 캐시는 **"같은 일을 두 번 하지 않기"**. 단, 정확성을
> 일부 포기한다. / A cache means **"don't do the same work twice"** — at
> the cost of some correctness.

---

## 3. 핵심 패턴 / Core patterns

**(1) Cache-aside (look-aside, lazy) — 기본값 / the default**
- KR: 앱이 캐시 먼저 확인 → 미스면 DB 읽고 캐시에 저장. 쓰기는 DB 후
  캐시 무효화. 대부분 여기서 시작.
- EN: App checks cache → on miss reads db and fills cache. On write,
  update db then invalidate cache. Most systems start here.

**(2) Read-through** — KR: 캐시 라이브러리가 미스 시 알아서 DB 로드. 앱
코드 단순. / EN: the cache library loads from db on miss; simpler app code.

**(3) Write-through** — KR: 쓰기를 캐시+DB 동시에. 캐시 항상 최신, 쓰기
느림. / EN: write to cache+db together; cache always fresh, slower writes.

**(4) Write-back (write-behind)** — KR: 캐시에 먼저 쓰고 DB는 나중에
비동기. 빠르지만 캐시 죽으면 데이터 유실 위험. / EN: write to cache
first, flush to db async; fast but risk data loss if cache dies.

**(5) Write-around** — KR: 쓰기는 DB로만, 캐시는 안 채움(읽힐 때 채움).
한 번 쓰고 잘 안 읽는 데이터에. / EN: write only to db, cache fills on
read; good for write-once-rarely-read data.

---

## 4. 핵심 7개념 / The 7 core concepts

**(1) 히트율 / Hit ratio** — KR: 히트/(히트+미스). 캐시 가치의 척도.
미스가 잦으면 캐시가 오히려 부담. / EN: hits/(hits+misses); the measure
of a cache's value. Low ratio = the cache is just overhead.

**(2) TTL** — KR: 자동 만료로 **스테일을 한정**. 짧으면 정확↑ 히트율↓,
길면 반대. / EN: auto-expiry **bounds staleness**. Short = fresher but
fewer hits; long = the reverse.

**(3) 축출 / Eviction** — KR: 메모리 한계 시 버릴 항목 선택. LRU(가장
오래 안 쓴 것), LFU(가장 덜 쓰인 것), FIFO. / EN: pick what to drop when
full: LRU (least recently used), LFU (least frequently), FIFO.

**(4) 무효화 / Invalidation** — KR: CS의 양대 난제. 데이터 변경 시 캐시를
지우거나 갱신. 순서 경합이 진짜 함정(S3). / EN: one of the two hard
problems. On change, delete/refresh the cache; ordering races are the
real trap (S3).

**(5) 스탬피드 / Stampede (dogpile)** — KR: 핫 키 만료 순간 미스 폭주가
DB를 친다. → single-flight(키당 로더 1개). / EN: a hot key expires and a
miss storm hits the db → single-flight (one loader per key).

**(6) 페네트레이션 & 어밸런치 / Penetration & avalanche** — KR:
페네트레이션=없는 키 반복 조회 → null도 캐시. 어밸런치=다수 키 동시 만료
→ TTL 지터. / EN: penetration = repeated missing key → cache the null;
avalanche = many keys expire together → TTL jitter.

**(7) 일관성 / Consistency** — KR: 캐시는 본질적으로 **결과적 일관성**.
강한 일관성 필요하면 캐시를 우회하거나 write-through. / EN: a cache is
inherently **eventually consistent**; for strong consistency bypass it
or use write-through.

---

## 5. 어디에 두나 / Where the cache lives

| 위치 / Location | 예 / Example | 특징 / Trait |
| --- | --- | --- |
| 로컬(프로세스 내) / local | Caffeine, Guava | ns 단위, 인스턴스마다 따로, stale 위험 / ns, per-instance, stale risk |
| 분산(공유) / distributed | Redis, Memcached | ~1ms, 모두 공유, 운영 필요 / ~1ms, shared, needs ops |
| 다계층 / multi-tier | L1 local + L2 Redis | 최고 속도, 일관성 비용↑ / fastest, more consistency cost |
| 엣지/CDN / edge | CloudFront, Cloudflare | 사용자 근처, 공개 읽기 전용에 / near user, public read-heavy |

KR: 스케일 = 분산 캐시를 샤딩(키를 노드로 분배) → 일관 해싱(05모듈)으로
리밸런싱 최소화. / EN: Scale = shard the distributed cache (spread keys
over nodes) → consistent hashing (module 05) to minimize rebalancing.

---

## 6. 빅테크 사례 / Big tech in practice

- **Redis** — KR: 사실상 표준 분산 캐시. 자료구조·TTL·원자연산. EN: the
  de facto standard distributed cache; data structures, TTL, atomics.
- **Memcached** — KR: 더 단순·순수 캐시(키-값). Facebook이 대규모 사용,
  stale 방지로 lease 기법. EN: simpler pure key-value; Facebook at huge
  scale, "leases" to fight stale/stampede.
- **Netflix EVCache** — KR: Memcached 위 다지역 복제 캐시 계층. EN: a
  multi-region replicated tier on top of Memcached.
- **CDN edge** — KR: 정적·공개 콘텐츠를 사용자 근처에서. Cache-Control,
  ETag. EN: static/public content cached near users via Cache-Control,
  ETag.

---

## 7. 트레이드오프 / Trade-offs

| 얻는 것 / Gain | 잃는 것 / Cost |
| --- | --- |
| 낮은 지연 / low latency | 스테일 가능 / possible staleness |
| DB 부하·비용↓ / less db load & cost | 무효화 복잡성 / invalidation complexity |
| 스파이크 흡수 / absorbs spikes | 새 장애 지점·운영 / new failure point & ops |
| 수평 확장 쉬움 / easy scale-out | 메모리 비용 / memory cost |

> KR: 강한 일관성·쓰기 위주·낮은 재사용이면 캐시가 손해다. 캐시는
> **읽기 많고 변경 적은** 데이터에. / EN: For strong consistency,
> write-heavy, or low-reuse data a cache is a net loss. Cache data that
> is **read-heavy and changes rarely**.

---

## 8. 면접 체크리스트 / Interview checklist

- [ ] 포스트잇 비유 90초 / sticky-note analogy in 90s
- [ ] 안 쓰면 터지는 4가지 / four failures without a cache
- [ ] cache-aside vs write-through 언제 / when each
- [ ] TTL이 정확성·히트율을 어떻게 트레이드 / TTL trades accuracy vs hit ratio
- [ ] 무효화가 왜 어려운가 (S3 경합) / why invalidation is hard (the race)
- [ ] 스탬피드와 single-flight / stampede & single-flight
- [ ] 페네트레이션·어밸런치 방어 / penetration & avalanche defenses
- [ ] 로컬 vs 분산 vs 다계층 / local vs distributed vs multi-tier
- [ ] 캐시가 손해인 경우 / when a cache is a net loss
- [ ] Redis vs Memcached vs CDN 언제 / when each

---

## 9. 다음 / Next

- 진화 트랙: `evolution/EVOLUTION.md` (S0→S5 실행)
- 실무 표준 + 코드: `production/STANDARD_SOLUTION.md` (Caffeine/Redis/Multi-tier/CDN)
- 직접 구현: `blank/` → `solution/`
