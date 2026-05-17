# 실무 표준 솔루션 — 창업하면 실제로 뭘 쓰나
# Real-world standard solutions — what you actually use at a startup

> KR: 앞의 `reference/`, `evolution/` 코드는 **내부 원리를 이해하려고
> 직접 만든 것**이다. 실무에선 큐를 직접 만들지 않는다 — **검증된 제품을
> 쓴다.** 여기 그 제품 4개와, 자바/스프링으로 쓰는 실제 코드가 있다.
> EN: The `reference/` and `evolution/` code was built **to understand
> the internals**. In production you do NOT build a queue — you **use a
> proven product**. Here are the four standard products and the actual
> Java/Spring code to use them.

> ⚠️ KR: 이 폴더의 `.java`는 **실무 참조용**이다. Maven 의존성 + 실제
> 서버(또는 클라우드)가 필요해서 이 레포에서 바로 실행되지 않는다.
> 원리 검증은 `reference/`·`evolution/`에서 이미 실행으로 끝냈다.
> EN: The `.java` here is **production reference**. It needs Maven
> dependencies + a real server/cloud, so it does not run inside this
> repo. The internals are already run-verified in `reference/`/`evolution/`.

---

## 4개 표준 제품 비교 / The four standard products

| 제품 / Product | 한 줄 / One line | 언제 / When | 운영 부담 / Ops |
| --- | --- | --- | --- |
| **AWS SQS** | 완전관리 큐. 서버 0대 운영 / fully managed queue, zero servers | 클라우드(AWS) + "그냥 큐" / cloud + "just a queue" | 거의 없음 / almost none |
| **RabbitMQ** | 전통적 메시지 브로커. 라우팅 강력 / classic broker, rich routing | 복잡한 라우팅·온프레미스 / complex routing, on-prem | 중간 / medium |
| **Redis (Streams/List)** | 이미 Redis 쓰면 추가 0. 가볍고 빠름 / free if you already run Redis | 단순 잡, 낮은 내구성 허용 / simple jobs, low durability OK | 낮음 / low |
| **Apache Kafka** | 분산 로그. 초고처리량 + 재처리 / distributed log, huge throughput + replay | 이벤트 스트리밍·대규모 / event streaming, large scale | 높음 / high |

---

## 창업자 의사결정 가이드 / Founder decision guide

**KR.** 단계별로 이렇게 가면 거의 안 틀린다:

1. **Day 1 (MVP, 팀 작음)** → **AWS SQS** (AWS면) 또는 **Redis 큐**
   (이미 Redis 쓰면). 이유: 운영 인력 0. 인프라에 시간 쓰지 말 것.
2. **트래픽·기능 늘고 라우팅 복잡** → 그래도 SQS로 버틸 수 있으면 버틴다.
   온프레미스이거나 정교한 라우팅이 필요하면 **RabbitMQ**.
3. **이벤트가 핵심 자산(분석·여러 소비자·재처리)** → **Kafka**.
   단, Kafka는 운영이 무겁다. **필요하기 전에 도입하지 말 것.**

> 한 줄 원칙: **"가장 지루한 기술을 골라라."** 큐는 차별화 포인트가
> 아니다. SQS/Redis로 시작해 정말 필요할 때만 Kafka로.

**EN.** A near-foolproof path by stage:

1. **Day 1 (MVP, tiny team)** → **AWS SQS** (if on AWS) or a **Redis
   queue** (if you already run Redis). Reason: zero ops people. Do not
   spend time on infrastructure.
2. **More traffic/features, routing gets complex** → stay on SQS if you
   can. If on-prem or you need rich routing → **RabbitMQ**.
3. **Events are a core asset (analytics, many consumers, replay)** →
   **Kafka**. But Kafka is heavy to run. **Do not adopt it before you
   need it.**

> One rule: **"Choose the most boring technology."** A queue is not your
> differentiator. Start with SQS/Redis; move to Kafka only when truly
> needed.

---

## 코드 파일 / Code files (Spring Boot)

| 파일 / File | 제품 / Product |
| --- | --- |
| `P1_SqsSpring.java` | AWS SQS (Spring Cloud AWS) |
| `P2_RabbitSpring.java` | RabbitMQ (Spring AMQP) |
| `P3_RedisStreamSpring.java` | Redis Streams (Spring Data Redis) |
| `P4_KafkaSpring.java` | Apache Kafka (Spring for Apache Kafka) |

> KR: 네 파일 모두 **같은 4가지**를 보여준다: ① 프로듀서가 메시지 전송
> ② 컨슈머가 수신 ③ 재시도/DLQ 설정 ④ 멱등 처리 위치. 제품이 바뀌어도
> **개념은 그대로**임을 비교로 확인하라.
> EN: All four files show the **same four things**: ① producer sends
> ② consumer receives ③ retry/DLQ config ④ where idempotency goes.
> Compare them and see the **concepts stay the same** across products.

> KR: 의존성/실행법은 각 파일 상단 주석에. EN: Dependencies and how to
> run are in the comment header of each file.
