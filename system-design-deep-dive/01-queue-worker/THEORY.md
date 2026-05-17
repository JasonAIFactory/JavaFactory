# 01 - Queue & Worker 딥다이브 (한글 ↔ English 병기)

> 읽는 법: **한글로 개념을 잡고, 바로 아래 영어로 같은 말을 다시 본다.**
> 영어는 면접/실무에서 쓰는 표현 그대로. 모국어를 영어로 "바꿔" 가는 연습.
> Read the Korean to get the idea, then read the English right below for
> the same idea in interview/work words.

> 학습 전: `QUIZ.md`의 **진단 퀴즈 8문제**부터.
> Before studying: do the 8 diagnostic questions in `QUIZ.md` first.

---

## 1. 큐는 "주문 전표 꽂이"다 / A queue is an "order ticket rail"

**KR.** 식당을 생각하자. 손님이 "김치찌개 하나요"라고 주문(요청)한다.
요리사가 직접 주문을 받으면, 손님이 몰릴 때 요리를 못 하고 줄이 밖까지
늘어선다(동기, 블로킹). 그래서 캐셔가 주문을 **전표에 적어 꽂이에
꽂기만** 하고 "번호 부르면 오세요"라고 한다(비동기). 요리사들(워커)이
꽂이에서 전표를 한 장씩 떼어 요리한다. 요리사가 3명이면 3개를 동시에.

**EN.** Think of a restaurant. A customer orders ("one kimchi stew").
If the cook takes orders directly, a rush blocks the kitchen and the
line grows outside (synchronous, blocking). So the cashier just **writes
the order on a ticket and puts it on a rail**, and says "we'll call your
number" (asynchronous). The cooks (workers) take tickets one by one and
cook. Three cooks cook three orders at once.

| 식당 / Restaurant | 시스템 / System |
| --- | --- |
| 손님 주문 / customer order | 메시지·작업 / message, task (job) |
| 캐셔 / cashier | 프로듀서 / producer (puts work in) |
| 전표 꽂이 / ticket rail | 큐 / queue (buffer) |
| 요리사 / cook | 워커 / worker (consumer) |
| 요리사 여러 명 / many cooks | 워커 풀 / worker pool (parallel) |
| 전표가 쌓임 / tickets pile up | 백로그 / backlog (queue depth) |
| 다시 요리 / re-cook | 재시도 / retry |
| 못 만드는 주문 / impossible order | 데드레터 큐 / dead letter queue (DLQ) |

> **파인만 테스트 / Feynman test:** 표를 덮고 친구에게 식당 비유로 90초
> 설명할 수 있는가? / Cover the table and explain it with the restaurant
> story in 90 seconds.

---

## 2. 왜 쓰는가 — 안 쓰면 뭐가 터지나 / Why use it — what breaks without it

**KR.** 큐가 없으면(요청 받은 스레드가 그 자리에서 끝까지 처리하면)
네 가지가 터진다.
**EN.** Without a queue (the request thread does all the work inline),
four things break.

**1) 느린 응답 / Slow response**
- KR: 회원가입에 이메일 2초 + 썸네일 3초를 동기로 하면 사용자는 5초를
  기다린다. 큐에 "보내라"만 넣으면 응답은 50ms.
- EN: If signup does email (2s) + thumbnail (3s) synchronously, the user
  waits 5s. Enqueue "do it later" and the response is 50ms.

**2) 스파이크에 서버가 죽음 / A spike kills the server**
- KR: 트래픽 100배면 동기 처리는 DB·CPU 한계 초과로 전체 장애. 큐가
  충격을 흡수하고 워커는 자기 속도대로 천천히 뺀다.
- EN: At 100x traffic, sync processing exceeds DB/CPU limits and the
  whole system fails. The queue absorbs the shock; workers drain at
  their own safe rate. (load leveling)

**3) 한 부품 고장이 전체로 / One failure spreads**
- KR: 주문 API가 알림 서비스를 직접 호출하면 알림이 죽을 때 주문도
  실패. 큐로 분리하면 알림이 죽어도 메시지는 쌓였다가 나중에 처리.
- EN: If the order API calls the notification service directly, orders
  fail when notifications are down. A queue **decouples** them, so the
  message waits and is processed after recovery.

**4) 실패한 작업이 증발 / Failed work disappears**
- KR: 동기 호출은 에러 나면 그 작업이 사라진다. 큐는 ack 전엔 메시지를
  안 지운다 → 재시도 가능.
- EN: A sync call loses the work on error. A queue keeps the message
  until it is acked, so it can be retried. (durability)

> **한 줄 / One line:** 큐는 "시간"과 "고장"을 분리하는 장치다. /
> A queue separates **time** and **failure**.

---

## 3. 핵심 7개념 / The 7 core concepts

**(1) 전달 보장 / Delivery guarantee**
- KR: `at-most-once`=처리 전 삭제(죽으면 유실, 로그용). `at-least-once`
  =성공 후 삭제(중복 가능, **현실의 기본값**). `exactly-once`=전송만으론
  사실상 불가 → at-least-once + 멱등성으로 "사실상 한 번".
- EN: at-most-once = delete before processing (lost on crash; OK for
  logs). at-least-once = delete after success (duplicates possible;
  **the real default**). exactly-once = not really possible in transport
  → at-least-once + idempotency gives "effectively once".

**(2) 가시성 타임아웃 / Visibility timeout**
- KR: 워커가 메시지를 꺼내면 큐가 잠깐 숨긴다. 그 시간 안에 ack 못 하면
  다시 보여 다른 워커가 처리. 너무 짧으면 정상 처리 중 중복, 너무 길면
  복구 지연.
- EN: When a worker takes a message, the queue hides it for a while. If
  not acked in time, it reappears for another worker. Too short = a
  still-running job is duplicated; too long = slow recovery.

**(3) 백오프 + 지터 / Backoff + jitter**
- KR: 즉시 재시도하면 죽은 다운스트림을 더 죽인다(retry storm). 간격을
  1s→2s→4s로 늘리고 무작위 지터를 더해 동시 재시도를 흩뿌린다.
- EN: Immediate retry hammers a struggling downstream (retry storm).
  Grow the gap (1s→2s→4s) and add random jitter so retries do not all
  fire at once.

**(4) 데드레터 큐 / Dead letter queue (DLQ)**
- KR: 같은 메시지가 N번 실패하면 메인 큐에서 빼서 격리. 독성 메시지
  하나가 줄 전체를 막지 않게.
- EN: After N failures, move the message aside. One poison message must
  not block the whole queue.

**(5) 순서 보장 / Ordering**
- KR: 기본 큐는 순서 보장 없음(워커 병렬). 필요하면 같은 키를 같은
  파티션에 보내 **그 안에서만** 순서 보장. 전역 순서는 병렬성 포기.
- EN: A plain queue does not keep order (parallel workers). If needed,
  route the same key to the same partition so order holds **within
  that partition**. Global order means giving up parallelism.

**(6) 백프레셔 / Backpressure**
- KR: 프로듀서가 워커보다 빠르면 큐가 무한히 자란다. 바운디드 큐로 막고
  가득 차면 ① 프로듀서 블로킹 ② 거절(429) ③ 버림. 무한 큐는 폭탄.
- EN: If producers outrun workers, the queue grows forever. Use a
  bounded queue; when full ① block the producer ② reject (429) ③ drop.
  An unbounded queue is a bomb.

**(7) 멱등성 / Idempotency**
- KR: 같은 메시지를 두 번 처리해도 결과가 같아야 한다. 메시지 고유 ID로
  처리 전 중복 확인(Redis SETNX / DB unique / upsert).
- EN: Processing the same message twice must give the same result. Use
  a unique message id to detect duplicates before doing the work
  (Redis SETNX / DB unique key / upsert).

---

## 4. 스케일업 vs 스케일아웃 / Scale-up vs scale-out

**KR.** 스케일업 = 한 대를 키운다(CPU·스레드↑). 빠르지만 천장이 낮고
단일 장애점. 스케일아웃 = 똑같은 워커를 여러 대. 큐가 빛나는 지점.
워커들이 같은 큐를 경쟁적으로 소비(competing consumers). 워커는
무상태여야 복제 가능. 큐에 쌓인 양(backlog)을 보고 자동 증감
(autoscaling). 큐 자체도 병목이면 파티션으로 쪼갠다.

**EN.** Scale-up = make one machine bigger (more CPU/threads). Fast but
low ceiling and a single point of failure. Scale-out = run many
identical workers — where a queue shines. Workers compete on the same
queue (competing consumers). Workers must be stateless to be cloned.
Autoscale on backlog. If the queue itself is the bottleneck, split it
into partitions.

**깨지는 지점 / Where it breaks**
- KR: ① 큐는 비는데 DB가 죽음(워커 늘리니 DB 커넥션 고갈) ② 핫 파티션
  (키 쏠림) ③ 순서 vs 병렬 충돌 ④ 리밸런싱 멈춤 ⑤ 재시도 폭풍.
- EN: ① queue empties but DB dies (more workers exhaust DB connections)
  ② hot partition (skewed key) ③ ordering vs parallelism conflict
  ④ rebalancing pause ⑤ retry storm.

---

## 5. 빅테크 사례 / Big tech in practice

- **SQS** — KR: 매니지드 큐, at-least-once, 가시성·DLQ 내장. "그냥 큐
  필요해"의 기본. EN: managed queue, at-least-once, visibility/DLQ
  built in. The default "I just need a queue".
- **Kafka** — KR: 분산 로그, 파티션 순서, 높은 처리량, offset 되감기로
  재처리. EN: distributed log, per-partition order, high throughput,
  replay by rewinding the offset.
- **Celery / Sidekiq** — KR: 백그라운드 잡 워커(Instagram, GitHub).
  무거운 일을 응답 경로에서 제거. EN: background job workers; move
  heavy work out of the request path.
- **Stripe** — KR: at-least-once + 멱등 키로 결제 중복 방지. EN:
  at-least-once + an idempotency key so a payment is never double-charged.

---

## 6. 트레이드오프 / Trade-offs

| 얻는 것 / Gain | 잃는 것 / Cost |
| --- | --- |
| 빠른 응답 / fast response | 결과가 즉시 없음 / no immediate result |
| 장애 격리 / failure isolation | 운영 복잡도↑ / more ops complexity |
| 부하 평탄화 / load leveling | 지연 증가 / added latency |
| 수평 확장 / easy scale-out | 순서·정확히한번 어려움 / hard ordering & exactly-once |
| 내구성 / durability | 중복 가능 / duplicates possible |

> KR: 강한 일관성·즉시 결과·단순함이 필요하면 동기 호출이 낫다. 큐는
> 비동기로 해도 되는 일에만. / EN: If you need strong consistency, an
> immediate result, or simplicity, a synchronous call is better. Use a
> queue only for work that can be asynchronous.

---

## 7. 면접 체크리스트 / Interview checklist

- [ ] 식당 비유 90초 / restaurant analogy in 90s
- [ ] 안 쓰면 터지는 4가지 / the four failures without a queue
- [ ] at-least-once + 멱등성으로 exactly-once / exactly-once via idempotency
- [ ] 가시성 타임아웃 짧을 때/길 때 / visibility timeout too short/long
- [ ] 지터를 왜 / why jitter (retry storm)
- [ ] DLQ는 왜 / why DLQ (poison pill)
- [ ] competing consumers 한계 / its limit (DB, hot partition)
- [ ] 순서 vs 병렬 / ordering vs parallelism
- [ ] 무한 큐 폭탄 / the unbounded-queue bomb
- [ ] SQS vs Kafka 언제 / when each

---

## 8. 다음 / Next

- 진화 트랙: `evolution/EVOLUTION.md` (S0→S5 실행) /
  Evolution track: `evolution/EVOLUTION.md` (run S0→S5)
- 영어 말하기 스크립트: `EXPLAIN_EN.md` /
  Spoken English scripts: `EXPLAIN_EN.md`
- **실무 표준 솔루션 + 코드**: `production/STANDARD_SOLUTION.md` /
  **Real-world standard solutions + code**: `production/STANDARD_SOLUTION.md`
- 직접 구현: `blank/` → `solution/` / Build it: `blank/` → `solution/`
