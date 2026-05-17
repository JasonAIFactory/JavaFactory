# 01 - Queue & Worker 퀴즈 (한글 ↔ English)

> 각 문제 한글로 생각하고, 영어 문장으로도 답해보기 (실무에선 영어로 말함).
> Think in Korean, then answer in English too (you'll speak English at work).

---

## A. 진단 퀴즈 / Diagnostic (이론 보기 전 / before theory — 8문제)

1. KR: 가입 시 이메일 2초. 사용자가 안 기다리게 하려면? 구조 이름은?
   EN: Signup email takes 2s. How to not make the user wait? Name the pattern.
2. KR: 주문 100배 폭주. 큐 있을 때/없을 때 차이?
   EN: Orders spike 100x. Difference with vs without a queue?
3. KR: "at-least-once"란? 부작용은?
   EN: What is "at-least-once"? Its side effect?
4. KR: exactly-once를 실무에서 어떻게 "사실상" 달성?
   EN: How do you "effectively" achieve exactly-once in practice?
5. KR: 워커가 처리 중 죽음. 메시지는? 관련 파라미터 이름?
   EN: A worker dies mid-processing. What of the message? Name the parameter.
6. KR: 실패를 즉시 재시도하면 왜 위험? 완화책 2개?
   EN: Why is immediate retry dangerous? Two mitigations?
7. KR: 같은 메시지 5번 실패. 큐 전체를 안 막으려면?
   EN: Same message fails 5 times. How to not block the whole queue?
8. KR: 프로듀서가 워커보다 빠르면? 어떻게 막나?
   EN: Producer faster than workers? How to stop it?

<details><summary>👉 정답 / Answers</summary>

1. KR: 이메일을 큐에 작업으로 넣고 워커가 백그라운드 처리. EN: enqueue
   the email; a worker processes it in the background. → 비동기 처리 /
   asynchronous processing (producer–consumer).
2. KR: 없으면 DB·CPU 초과로 전체 장애. 있으면 큐가 버퍼라 시스템 생존.
   EN: without → DB/CPU overload, full outage. with → the queue buffers,
   system survives (load leveling).
3. KR: 최소 1번 전달(성공 후 삭제), ack 유실 시 중복. EN: delivered at
   least once (delete after success); duplicates if the ack is lost.
4. KR: at-least-once + 멱등 컨슈머(고유 ID dedup/upsert). EN:
   at-least-once + an idempotent consumer (dedup by unique id / upsert).
5. KR: 가시성 타임아웃 만료 후 재노출 → 다른 워커가 처리. EN: after the
   **visibility timeout** it reappears for another worker.
6. KR: retry storm. → 지수 백오프 + 지터(+서킷브레이커, 최대 횟수). EN:
   retry storm → exponential backoff + jitter (+ circuit breaker, max
   attempts).
7. KR: DLQ로 격리. EN: isolate it in a **dead letter queue (DLQ)**.
8. KR: 큐 무한 증가 → 바운디드 큐 + 백프레셔(블록/429/drop). EN: queue
   grows forever → bounded queue + backpressure (block / 429 / drop).

</details>

---

## B. 확인 퀴즈 / Exit quiz (코드·구현 후 / after code & build — 10문제)

1. KR: 큐는 무엇과 무엇을 분리? EN: A queue separates what and what?
2. KR: at-most vs at-least의 코드 한 줄 차이? EN: The one-line code
   difference between at-most and at-least?
3. KR: 멱등 컨슈머 구현 2가지? EN: Two ways to implement an idempotent
   consumer?
4. KR: 가시성 30s인데 처리 45s면? 해결? EN: Visibility 30s but
   processing takes 45s — what happens, how to fix?
5. KR: 지터 없으면 무슨 현상? EN: No jitter — what phenomenon? (term)
6. KR: 워커 5배인데 DB 에러 폭증. 원인·해법? EN: 5x workers but DB
   errors explode — cause and fix?
7. KR: 결제 중복 방지: 큐 레벨/앱 레벨 각각? EN: Prevent double payment:
   what at queue level vs app level?
8. KR: 순서 필요 작업 + 병렬성 둘 다? EN: Need ordering AND some
   parallelism — how?
9. KR: SQS 대신 Kafka 골라야 하는 상황 2개? EN: Two cases to pick Kafka
   over SQS?
10. KR: 무한 큐가 "폭탄"인 이유? EN: Why is an unbounded queue a "bomb"?

<details><summary>👉 정답 / Answers</summary>

1. KR: 시간과 고장. EN: **time and failure** (fast response + failure
   isolation).
2. KR: ack/삭제를 처리 전(at-most) vs 성공 후(at-least). EN: ack/delete
   **before** processing (at-most) vs **after** success (at-least).
3. KR: (a) 처리 전 처리완료 ID 확인(Redis SETNX/DB unique) (b) 결과를
   upsert. EN: (a) check a processed-id set before work (Redis
   SETNX/DB unique) (b) write the result with an upsert.
4. KR: 45>30이라 처리 중 재노출 → 중복 실행. 해결: 타임아웃을 p99+여유로
   늘리거나 처리 중 연장(heartbeat). EN: 45>30 so it reappears while
   still running → double execution. Fix: raise timeout to p99+margin,
   or extend it mid-processing (heartbeat).
5. KR: 모든 워커가 동시 재시도 = thundering herd. EN: all workers retry
   at once = **thundering herd / retry storm**.
6. KR: 워커는 무상태라 늘었지만 공유 DB 커넥션 고갈. → 동시성 상한, 풀
   사이징, 배치. EN: workers scaled but the shared DB connection pool is
   the bottleneck. → cap concurrency, size the pool, batch.
7. KR: 큐=at-least-once만, 앱=멱등 키로 중복 무효화(Stripe). EN: queue =
   only at-least-once; app = an **idempotency key** voids duplicates.
8. KR: 키 기반 파티셔닝(같은 키=같은 파티션 순서, 다른 키 병렬). EN:
   **key-based partitioning** (same key → same partition → order;
   different keys run in parallel).
9. KR: (a) 높은 처리량 + 재처리(offset 되감기) (b) 여러 독립 소비자
   그룹 + 파티션 순서. EN: (a) high throughput + replay (rewind offset)
   (b) many independent consumer groups + per-partition order.
10. KR: 프로듀서>컨슈머가 지속되면 큐가 끝없이 커져 더 큰 장애로 지연.
    바운디드는 지금 가장자리에서(429) 드러냄. EN: if producer>consumer
    persists, the queue grows until a bigger outage — just delayed.
    Bounded surfaces it now, at the edge (429).

</details>

---

## C. 자기 채점 / Self-grading

- KR: 진단 6+/8이면 이론 빠르게, 미만이면 정독. EN: 6+/8 on diagnostic
  → skim theory; below → read closely.
- KR: 확인 8+/10이면 통과. 4·6·8 틀리면 `reference/R2,R3` 다시. EN:
  8+/10 on exit → pass. Missed 4/6/8 → revisit `reference/R2,R3`.
- KR: 9·10을 **남에게 영어로 말로** 설명되면 진짜 통과. EN: if you can
  explain 9 & 10 **out loud in English**, you truly passed.
