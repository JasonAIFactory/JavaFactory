# 02 - Caching 퀴즈 (한글 ↔ English)

> 한글로 생각하고 영어로도 답해보기. / Think in Korean, answer in English too.

---

## A. 진단 퀴즈 / Diagnostic (이론 전 / before theory — 8문제)

1. KR: 인기 상품 1개를 초당 1만 명이 조회. 캐시 없으면 DB는?
   EN: 10k/s view one hot product. Without a cache, what does the db do?
2. KR: cache-aside에서 읽기 미스 시 순서 3단계?
   EN: In cache-aside, the 3 steps on a read miss?
3. KR: TTL을 짧게 vs 길게 하면 각각 무엇이 좋아지고 나빠지나?
   EN: Short vs long TTL — what gets better/worse each way?
4. KR: 캐시가 가득 찼다. 무엇을 버리나? 정책 2개 이름.
   EN: Cache is full. What do you drop? Name 2 policies.
5. KR: 인스턴스 3대 각자 로컬 캐시. 한 대가 무효화하면?
   EN: 3 instances, each a local cache. One invalidates — what happens?
6. KR: 핫 키가 막 만료된 순간 동시 미스 1000개. DB는?
   EN: A hot key just expired, 1000 concurrent misses. The db?
7. KR: 존재하지 않는 키를 계속 조회하면 캐시가 막아주나?
   EN: Repeatedly query a non-existent key — does the cache protect the db?
8. KR: 캐시를 쓰면 절대 안 되는(손해인) 데이터 유형은?
   EN: What kind of data should NOT be cached (net loss)?

<details><summary>👉 정답 / Answers</summary>

1. KR: 같은 행을 초당 1만 번 읽음(읽기 증폭), 부하·비용 폭증. EN: reads
   the same row 10k/s (read amplification), load & cost explode.
2. KR: 캐시 확인(미스) → DB 읽기 → 캐시에 저장 후 반환. EN: check cache
   (miss) → read db → store in cache → return.
3. KR: 짧게=더 신선/히트율↓/DB부하↑. 길게=히트율↑/스테일 위험↑. EN:
   short = fresher / lower hit ratio / more db load; long = higher hit
   ratio / more staleness risk.
4. KR: LRU(가장 오래 안 쓴 것), LFU(가장 덜 쓰인 것) (FIFO도). EN:
   LRU (least recently used), LFU (least frequently used) (or FIFO).
5. KR: 자기 캐시만 지움. 다른 2대는 스테일 계속 → 분산 캐시 필요. EN:
   only its own cache clears; the other 2 stay stale → need a shared cache.
6. KR: 미스 폭주가 DB를 동시에 1000번 침(스탬피드/dogpile). EN: a miss
   storm hits the db 1000x at once (stampede/dogpile).
7. KR: 기본 cache-aside는 못 막음(매번 미스→DB). null도 캐시해야 함
   (penetration). EN: plain cache-aside does not (every miss → db); must
   cache the null too (penetration).
8. KR: 쓰기 위주·강한 일관성 필요·재사용 거의 없는 데이터. EN:
   write-heavy, strong-consistency, or low-reuse data.

</details>

---

## B. 확인 퀴즈 / Exit quiz (코드·구현 후 — 10문제)

1. KR: 캐시는 무엇을 무엇과 맞바꾸나? EN: A cache trades what for what?
2. KR: cache-aside vs write-through 한 줄 차이? EN: One-line difference?
3. KR: 무효화 경합(S3)이 영구 스테일을 만드는 시나리오? EN: The race
   that causes permanent staleness?
4. KR: 그 경합의 현실적 완화책 1개 + 더 강한 것 1개? EN: One pragmatic
   mitigation + one stronger fix?
5. KR: single-flight가 정확히 무엇을 보장? EN: What exactly does
   single-flight guarantee?
6. KR: 캐시 페네트레이션 방어 코드 한 줄? EN: One-line penetration defense?
7. KR: 어밸런치는 왜 생기고 지터가 왜 고치나? EN: Why does avalanche
   happen and why does jitter fix it?
8. KR: L1+L2 다계층의 이득과 그 대가? EN: Multi-tier gain and its cost?
9. KR: 히트율 99%→90%면 DB 부하는 몇 배? EN: Hit ratio 99%→90%, db load
   multiplies by? (think it through)
10. KR: Redis vs CDN 엣지 캐시를 각각 언제? EN: When Redis vs CDN edge?

<details><summary>👉 정답 / Answers</summary>

1. KR: 속도(낮은 지연·적은 DB부하)를 정확성(스테일 가능)과. EN: speed
   (low latency, less db load) for correctness (possible staleness).
2. KR: aside=미스 시 앱이 DB 읽고 채움/쓰기 시 무효화. through=쓰기를
   캐시+DB 동시. EN: aside = app fills on miss, invalidates on write;
   through = write to cache+db together.
3. KR: 리더가 옛 값 로드 후 멈춤 → 라이터가 DB갱신+무효화 → 리더가 옛
   값을 캐시에 저장. EN: reader loads old value then pauses → writer
   updates db + invalidates → reader stores the old value into cache.
4. KR: 현실적=TTL 안전망(결과적 일관성). 강한=write-through / 커밋 후
   재검증 / 버전 키. EN: pragmatic = TTL safety net (eventual
   consistency); stronger = write-through / recheck-after-commit /
   versioned keys.
5. KR: 한 키에 대해 동시에 들어온 미스를 묶어 **로더(=DB 호출)를 1회만**.
   EN: collapses concurrent misses for one key so the loader (db call)
   runs **exactly once**.
6. KR: DB가 null이어도 그 "없음"을 짧은 TTL로 캐시(`cache.put(k,
   SENTINEL)`). EN: cache the "not found" with a short TTL even when db
   returns null.
7. KR: 다수 키가 같은 TTL로 동시 만료 → 동시 미스 폭주. 지터가 만료
   시각을 흩어 cliff를 없앰. EN: many keys share a TTL and expire
   together → mass miss storm; jitter spreads expiry times, removing the
   cliff.
8. KR: 이득=ns 읽기(네트워크 회피). 대가=L1이 인스턴스별이라 잠깐
   스테일(짧은 L1 TTL로 한정). EN: gain = ns reads (no network); cost =
   per-instance L1 can be briefly stale (bounded by a short L1 TTL).
9. KR: 미스율 1%→10%이므로 DB 부하 약 **10배**. (캐시 가치는 미스율로
   결정.) EN: miss rate 1%→10% → ~**10x** db load. (a cache's value is
   set by the miss rate.)
10. KR: Redis=동적·사용자별 데이터 공유 캐시. CDN=정적·공개·읽기 위주를
    사용자 근처에서. EN: Redis = shared cache for dynamic/per-user data;
    CDN = static/public read-heavy content near the user.

</details>

---

## C. 자기 채점 / Self-grading

- KR: 진단 6+/8 → 이론 빠르게. 미만 → 정독. EN: 6+/8 → skim; below →
  read closely.
- KR: 확인 8+/10 통과. 3·4·5 틀리면 `evolution/S3,S4` 다시 실행. EN:
  8+/10 to pass; missed 3/4/5 → rerun `evolution/S3,S4`.
- KR: 9·10을 **영어로 말로** 설명되면 진짜 통과. EN: explain 9 & 10
  **out loud in English** = truly passed.
