# 시스템 디자인 딥다이브 (System Design Deep Dive)

> 목표: "면접용 키워드 암기"가 아니라 **실무에서 직접 설계하고 코드로 짤 수 있는** 수준.
> 각 모듈은 **2시간 집중 학습** 단위. 한 주제를 파인만식으로 끝까지 판다.

---

## 🎯 이 트랙의 철학

대부분의 시스템 디자인 공부는 "캐시 쓰면 빨라져요" 수준에서 멈춘다.
여기서는 **하나의 주제를 골라 끝까지 판다**:

1. **파인만식 설명** — 비유로 직관을 잡고, 내가 남에게 설명할 수 있을 때까지
2. **왜 쓰는가 / 안 쓰면 어떻게 되는가** — 문제가 먼저, 솔루션은 그 다음
3. **장단점·트레이드오프** — 공짜 점심은 없다. 무엇을 포기하는가?
4. **이론 + 빅테크 실제 사례** — SQS / Kafka / Redis / Cassandra가 *실제로* 어떻게 쓰는지
5. **스케일아웃 vs 스케일업** — 트래픽이 10배, 1000배 되면 이 설계는 어디서 깨지는가
6. **분석용 레퍼런스 코드** — 동작하는 Java 구현을 한 줄씩 "왜 이렇게 짰는지" 설명
7. **백지 직접 구현** — 스켈레톤만 보고 직접 짜고, 정답과 비교

---

## 📁 모듈 폴더 구조 (공통 규약)

각 모듈 `NN-topic/` 안에는:

| 파일/폴더 | 내용 |
| --- | --- |
| `THEORY.md` | 파인만식 이론 + 왜/장단점 + 빅테크 사례 + 스케일 전략 + 면접 체크리스트 |
| `EXPLAIN_EN.md` | **미국 실무/면접용 영어 설명 + 말하기 스크립트** (30초/60초, 트레이드오프, 빅테크) |
| `QUIZ.md` | 시작 전 **진단 퀴즈** + 모듈 끝 **확인 퀴즈** (정답·해설 포함) |
| `reference/` | 동작하는 Java 레퍼런스 코드. **모든 주석은 심플한 영어** |
| `blank/` | TODO만 있는 백지 스켈레톤 (직접 구현). 주석은 영어 |
| `solution/` | `blank/`의 정답. 주석은 영어 |

> 코드 규약: 학습자가 미국에서 일하므로 **모든 코드 주석은 단순하고 명확한 영어**로 작성한다.
> 코드는 작성 후 반드시 컴파일·실행·검증(`java -ea`)하고, 동시성 코드는 여러 번 반복 실행해 안정성을 확인한다.

학습 규칙: **레퍼런스를 읽기 전에 진단 퀴즈를 먼저 풀고**, **정답을 보기 전에 백지에서 30분 이상 버틴다.**

---

## 🗺️ 전체 커리큘럼 (14 모듈 + 캡스톤)

순서는 "백엔드 스케일링의 뼈대부터" 쌓는 순서. 위에서부터 내려갈수록 앞 모듈을 전제로 한다.

### Part 1 — 비동기와 부하 분산의 뼈대

| # | 모듈 | 한 줄 요약 | 핵심 빅테크 |
| --- | --- | --- | --- |
| **01** | **Queue & Worker** ✅ | 비동기·디커플링·부하 평탄화의 시작점 | SQS, Celery, Sidekiq |
| 02 | Caching | 같은 일을 두 번 하지 않기. 무효화가 진짜 어려움 | Redis, Memcached, CDN edge |
| 03 | Load Balancing | 트래픽을 여러 서버로. L4 vs L7, health check | Nginx, Envoy, AWS ALB |
| 04 | Rate Limiting | 남용·폭주 막기. token bucket vs sliding window | Stripe, Cloudflare |
| 05 | Consistent Hashing | 노드가 늘고 줄어도 키가 안 흩어지게 | Cassandra, DynamoDB ring |

### Part 2 — 데이터를 키우기

| # | 모듈 | 한 줄 요약 | 핵심 빅테크 |
| --- | --- | --- | --- |
| 06 | DB Replication | 읽기 확장 + 가용성. 복제 지연이라는 함정 | MySQL replica, Aurora |
| 07 | Sharding & Partitioning | 한 DB에 안 들어갈 때. 샤드 키 잘못 고르면 지옥 | Vitess, MongoDB, Citus |
| 08 | Indexing & Search | 풀스캔 탈출. inverted index, B-tree | Elasticsearch, Postgres |

### Part 3 — 분산 시스템의 진실

| # | 모듈 | 한 줄 요약 | 핵심 빅테크 |
| --- | --- | --- | --- |
| 09 | Pub/Sub & Event-Driven | 큐의 진화형. 로그 기반 아키텍처 | Kafka, Pulsar |
| 10 | Idempotency & Exactly-once | 네트워크는 거짓말한다. 중복은 기본값 | Stripe API, payment systems |
| 11 | Distributed Transactions | 2PC는 왜 안 쓰나. Saga / Outbox 패턴 | Uber, banking |
| 12 | Resiliency Patterns | circuit breaker, retry, bulkhead, backpressure | Netflix Hystrix/resilience4j |

### Part 4 — 운영과 종합

| # | 모듈 | 한 줄 요약 | 핵심 빅테크 |
| --- | --- | --- | --- |
| 13 | Observability | 안 보이면 못 고친다. log/metric/trace | Prometheus, OpenTelemetry |
| 14 | API Gateway & Service Discovery | MSA의 현관문 | Kong, Consul, Eureka |
| **CAP** | Capstone | 위 14개를 엮어 시스템 1개를 끝까지 설계 | — |

---

## ⏱️ 2시간 모듈 표준 타임박스

| 시간 | 활동 | 산출물 |
| --- | --- | --- |
| 0:00–0:10 | **진단 퀴즈** (`QUIZ.md` 앞부분) — 모르는 걸 먼저 자각 | 채점 결과 |
| 0:10–0:40 | `THEORY.md` 정독 + 비유를 **내 말로** 다시 쓰기 | 요약 노트 |
| 0:40–1:10 | `reference/` 코드 정독 → 한 줄씩 "왜?" 설명해보기 | 주석 이해 |
| 1:10–1:50 | `blank/` 백지 구현 (정답 안 봄) | 동작하는 코드 |
| 1:50–2:00 | `solution/`과 비교 + **확인 퀴즈** | 갭 메모 |

> 백지에서 막혀도 30분은 버틴다. 막힌 지점이 곧 진짜 학습 포인트.

---

## ✅ 모듈 완료 체크리스트

- [ ] 진단 퀴즈를 **먼저** 풀었다 (이론 보기 전)
- [ ] 핵심 비유를 안 보고 내 말로 설명할 수 있다 (파인만 테스트)
- [ ] "왜 쓰는가"와 "안 쓰면 뭐가 터지는가"를 둘 다 말할 수 있다
- [ ] 최소 2개의 트레이드오프를 댈 수 있다
- [ ] 트래픽 1000배 시나리오에서 이 설계가 깨지는 지점을 안다
- [ ] 레퍼런스 코드 없이 백지에서 핵심을 구현했다
- [ ] 빅테크가 실제로 쓰는 제품 이름 + 이유를 1개 이상 안다

---

## 🚀 시작하기

```
cd 01-queue-worker
# 1) QUIZ.md 의 "진단 퀴즈" 먼저
# 2) THEORY.md 정독
# 3) reference/ 코드 분석
# 4) blank/ 직접 구현
# 5) solution/ 비교 + 확인 퀴즈
```

👉 **[01-queue-worker/THEORY.md](01-queue-worker/THEORY.md)** 부터 시작.
