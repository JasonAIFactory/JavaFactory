# 03 - Load Balancing 딥다이브 (한글 ↔ English 병기)

> 읽는 법: **한글로 개념을 잡고, 바로 아래 영어로 같은 말을 다시 본다.**
> 영어는 면접/실무에서 쓰는 표현 그대로.
> Read the Korean to get the idea, then read the English for the same idea
> in interview/work words.

> 학습 전: `QUIZ.md`의 **진단 퀴즈 8문제**부터.
> Before studying: do the 8 diagnostic questions in `QUIZ.md` first.

---

## 1. 로드밸런서는 "식당 입구의 안내원"이다 / A load balancer is the "host at the door"

**KR.** 인기 식당에 손님이 몰린다. 홀이 하나뿐이면 줄이 밖까지 늘어선다.
그래서 똑같은 홀을 여러 개 두고, **입구에 안내원**을 세운다. 안내원은
"2번 홀로 가세요", "3번 홀로 가세요" 하고 손님을 **빈 홀로 분산**한다.
홀 하나가 불나면(서버 다운) 안내원은 그 홀로 손님을 안 보낸다(헬스체크).
같은 일행은 같은 홀로 보내야 할 때도 있다(스티키 세션).

**EN.** A popular restaurant gets a rush. With one dining room the line
goes out the door. So we build several identical rooms and put a **host
at the door**. The host says "room 2", "room 3" and **spreads guests to
free rooms**. If a room catches fire (a server is down) the host stops
sending people there (health check). Sometimes the same party must go
to the same room (sticky session).

| 식당 / Restaurant | 시스템 / System |
| --- | --- |
| 안내원 / host at the door | 로드밸런서 / load balancer (LB) |
| 똑같은 홀 여러 개 / identical rooms | 백엔드 서버 풀 / backend server pool |
| "어느 홀?" 규칙 / which-room rule | 분산 알고리즘 / balancing algorithm |
| 불난 홀 안 보냄 / skip the burning room | 헬스체크 + 이젝션 / health check + ejection |
| 같은 일행 같은 홀 / same party, same room | 스티키 세션 / session affinity |
| 홀 닫기 전 마무리 / let a room finish first | 커넥션 드레이닝 / connection draining |
| 안내원이 한 명뿐 / only one host | LB가 SPOF / LB is a single point of failure |

> **파인만 테스트 / Feynman test:** 표를 덮고 친구에게 식당 안내원
> 비유로 LB·헬스체크·스티키·드레이닝을 90초에 설명할 수 있는가?

---

## 2. 왜 쓰는가 — 안 쓰면 뭐가 터지나 / Why use it — what breaks without it

**KR.** LB가 없으면(서버 1대로 다 받으면) 네 가지가 터진다.
**EN.** Without an LB (one server takes everything), four things break.

1. **용량 천장 / Hard capacity ceiling.**
   - KR: 서버 1대의 CPU·메모리·커넥션 수는 유한하다. 트래픽이 그 천장을
     넘으면 요청이 거절되거나 응답이 폭발한다. (`S0` 실행: 200개 중 10개만
     처리, 190개 거절.)
   - EN: one box has finite CPU/memory/connections. Past that ceiling,
     requests are rejected or latency explodes. (`S0`: 10 served, 190
     rejected out of 200.)
2. **단일 장애점 / Single point of failure.**
   - KR: 그 1대가 죽으면 서비스 전체가 죽는다(전면 장애).
   - EN: if that one box dies, the whole service is down.
3. **수평 확장 불가 / No horizontal scale.**
   - KR: 더 큰 장비로 키우는(스케일업) 데는 한계가 있고 비싸다. 여러 대로
     늘리려면(스케일아웃) 그 앞에 트래픽을 나눠줄 무언가가 필요하다.
   - EN: scaling up one box has a hard, expensive limit. To scale out you
     need something in front that splits traffic.
4. **무중단 배포 불가 / No zero-downtime deploy.**
   - KR: 1대뿐이면 그 1대를 재시작하는 순간 서비스가 끊긴다.
   - EN: with one box, restarting it = an outage.

> 한 줄 정의 / One-line definition:
> **"로드밸런서는 동일한 서버 풀 앞에 서서, 트래픽을 건강한 서버들로
> 나눠 보내 용량·가용성·무중단 배포를 만든다."**
> "A load balancer sits in front of an identical pool and spreads traffic
> across the healthy ones to give capacity, availability, and zero-downtime
> deploys."

---

## 3. L4 vs L7 — 가장 많이 묻는 질문 / The question you WILL be asked

**KR.**

| | L4 (전송 계층) | L7 (애플리케이션 계층) |
| --- | --- | --- |
| 보는 것 | IP·포트·TCP/UDP만 | HTTP 경로·헤더·쿠키·TLS |
| 속도 | 매우 빠름 (그냥 패킷 전달) | 약간 느림 (요청을 해석) |
| 할 수 있는 것 | "이 연결을 서버 B로" | `/api`→A, `/img`→B, 쿠키 스티키, 재시도, TLS 종료 |
| 제품 | AWS NLB, LVS/IPVS, Maglev | Nginx, Envoy, HAProxy, AWS ALB |
| 언제 | 초저지연·초고처리량(게임·DB 앞) | 일반 웹/HTTP API (대부분 여기) |

**EN.**
- **L4** balances by IP/port only. It does not read HTTP. Fast, dumb,
  great for raw TCP throughput. It cannot route by URL or do cookie
  stickiness.
- **L7** terminates the connection, reads HTTP, and can route by path,
  header, or cookie, do TLS termination, retries, and content-based
  rules. Slightly more work per request.
- Real big systems often do **both**: an L4 layer (anycast/ECMP) spreads
  raw traffic across many L7 proxies, and the L7 proxies do the smart
  per-request routing. (`S5` shows why you need a layer in front of the LB.)

> 면접 한 문장 / Interview one-liner: *"L4 routes a connection by IP and
> port and is very fast; L7 reads the HTTP request so it can route by
> path or cookie and terminate TLS, at a small CPU cost."*

---

## 4. 분산 알고리즘 — 트레이드오프 / Balancing algorithms — trade-offs

**KR.**

| 알고리즘 | 한 줄 | 약점 |
| --- | --- | --- |
| Round Robin | 순서대로 1대씩 | 서버/요청이 불균등하면 느린 서버에 쌓임 (`S3`) |
| Weighted RR | 성능 좋은 서버에 가중치 | 가중치를 수동/정적으로 줘야 함 |
| Least Connections | in-flight 가장 적은 서버 | 짧은 요청엔 과민; 상태 공유 필요 |
| Least Response Time | 평균 응답 빠른 서버 | 측정/노이즈에 민감 |
| Hash / Consistent Hash | 키→항상 같은 서버 (스티키, 캐시 지역성) | 키 분포 치우치면 핫스팟 |
| Power of Two Choices | 무작위 2개 중 덜 바쁜 쪽 | 거의 최적인데 전역 상태 불필요 (실무 인기) |

**EN.** Round robin is the default and fine when servers are uniform.
When they are **not** (old vs new box, uneven request cost), round
robin keeps the slow one saturated while fast ones idle — `S3` shows the
tail latency blow up. **Least-connections** steers around the slow node.
**Consistent hashing** (module 05) gives stickiness and cache locality
with minimal key movement when membership changes — `R3` shows only the
removed node's keys move. **Power of two choices** (pick 2 at random,
send to the less busy) is a famous near-optimal trick that needs no
global state — very common in real proxies.

---

## 5. 헬스체크 — LB의 핵심 / Health checks — the heart of an LB

**KR.**
- **액티브(active):** LB가 주기적으로 `GET /health`를 찔러본다. 실패하면
  로테이션에서 **빼고(eject)**, 회복하면 다시 넣는다. (`S2`)
- **패시브(passive):** 실제 요청이 연속 N번 실패하면 그 서버를 뺀다.
  `/health`는 OK인데 실제로는 고장난 경우를 잡는다. (`R2`)
- **슬로 스타트(slow start):** 회복한 서버에 갑자기 트래픽을 다 몰면
  콜드 캐시·JIT 미가동으로 또 죽는다. 천천히 올린다.
- **이상치 탐지(outlier detection):** Envoy 용어. 에러율이 튀는 서버를
  자동으로 일시 추방.

**EN.** Active = the LB probes `/health` and ejects a failing backend,
re-adding it on recovery. Passive = eject after N consecutive real
request failures (catches "health says OK but it's actually broken").
Slow start = ease a recovered node back in, or a cold cache / un-JITed
node falls over again. Outlier detection (Envoy's name) = auto-eject a
node whose error rate spikes.

> 함정 / The trap: 헬스체크 간격이 길면 죽은 서버로 그 간격만큼 트래픽이
> 샌다(`S2`의 ~100ms gap). 너무 짧으면 헬스체크 자체가 부하가 된다.
> Long interval = traffic leaks to a dead node for that long; too short
> = the health checks themselves become load.

---

## 6. 스티키 세션 — 편하지만 비싸다 / Sticky sessions — easy but costly

**KR.** 서버가 세션을 **메모리에** 들고 있으면, 다음 요청이 다른 서버로
가면 "로그아웃"된다(`S4`: round robin 시 300/400 에러). 스티키(같은
세션→같은 서버)로 막을 수 있지만 트레이드오프:
- 부하가 **불균등**해진다(핫 유저/핫 서버).
- 그 서버가 죽으면 그 세션들은 어차피 **다 날아간다**.
- 배포·오토스케일이 어려워진다(유저를 옮겨야 함).

→ **실무 정답: 서버를 무상태(stateless)로 만들고 세션을 외부
저장소(Redis)나 토큰(JWT)에 둔다.** 그러면 아무 서버나 처리 가능.
스티키는 그게 불가능할 때의 *차선책*.

**EN.** If a server keeps the session **in memory**, the next request on
another server says "who are you?" (`S4`: 300/400 errors with round
robin). Stickiness fixes it but: load gets uneven (hot server); if that
server dies the sessions are gone anyway; deploys/autoscaling get harder.
The real answer is **stateless servers + shared session store (Redis) or
a signed token (JWT)** so any server can serve any request. Stickiness is
the fallback when you can't do that.

---

## 7. 스케일아웃 vs 스케일업 — LB 자신은? / What scales the LB itself?

**KR.** 백엔드는 LB로 수평 확장했다. 그런데 **LB 자신이 새 SPOF·병목**이
된다(`S5`). 해결:
- **LB 다중화:** LB를 2대 이상. 한 대 죽어도 다른 LB로 페일오버.
- **DNS 라운드로빈 / Anycast:** 클라이언트가 여러 LB IP 중 하나로. 죽은
  LB는 DNS/헬스에서 빠지거나 클라이언트가 재시도.
- **L4 앞단:** ECMP/anycast L4가 raw 트래픽을 여러 L7 LB로 분산
  (Google Maglev 방식).
- **커넥션 드레이닝:** 배포·스케일다운 시 백엔드를 즉시 빼면 in-flight가
  끊긴다(`S5`: 32개 드롭). DRAINING(신규 차단)→in-flight 0 대기→제거 →
  **0개 드롭**.

**EN.** Backends scale out behind the LB — but now the **LB is the new
SPOF/bottleneck** (`S5`). Fixes: run multiple LBs with client failover;
DNS round robin / anycast so clients reach any LB; an L4 layer
(ECMP/anycast, Maglev-style) spreading raw traffic across many L7
proxies; and connection draining so deploys don't reset in-flight
requests (`S5`: 32 dropped instant vs 0 with draining).

---

## 8. 빅테크는 실제로 / How big tech actually does it

**KR./EN.**
- **AWS:** NLB = L4 (초고처리량, 고정 IP), ALB = L7 (경로/호스트 라우팅,
  타깃그룹 헬스체크, `deregistration_delay` = 드레이닝). Cross-zone LB로
  AZ 간 균등.
- **Nginx / HAProxy:** `upstream` 블록, `least_conn`, `ip_hash`,
  `max_fails`/`fail_timeout`(패시브), `slow_start`(상용).
- **Envoy:** 클러스터 + outlier detection + ring hash(일관성 해시) +
  panic threshold(건강한 노드가 너무 적으면 전부에 보냄) + zone-aware.
- **Google Maglev:** L4 소프트웨어 LB, 일관성 해시로 연결 고정, ECMP로
  여러 Maglev에 anycast.
- **Netflix:** Eureka(서비스 디스커버리) + Ribbon/클라이언트 사이드 LB +
  resilience4j(모듈 12)로 재시도/서킷브레이커.

---

## 9. 흔한 오해 / Common misconceptions

**KR.**
- "LB 두면 무조건 빨라진다" → 아니다. 단일 요청은 더 빨라지지 않는다.
  **처리량과 가용성**이 좋아지는 것.
- "라운드로빈이면 균등하다" → 요청 비용이 다르면 전혀 아니다(`S3`).
- "스티키 쓰면 세션 안전" → 그 서버 죽으면 그대로 날아간다(`S4`).
- "헬스체크 OK면 정상" → `/health`는 거짓말할 수 있다(패시브 필요).
- "LB 하나면 충분" → 그게 새 SPOF다(`S5`).

**EN.** An LB does not speed up a single request — it adds throughput and
availability. Round robin is not "even" when request cost varies. Sticky
does not make sessions safe (node death loses them). A green `/health`
can lie (need passive ejection). One LB is itself a SPOF.

---

## 10. 면접 체크리스트 / Interview checklist

- [ ] LB가 **왜** 필요한가(용량 천장·SPOF·수평확장·무중단배포)를 한
      문장으로.
- [ ] **L4 vs L7** 차이와 각각 언제 쓰는지.
- [ ] 알고리즘 3개 이상 + 각 **트레이드오프** (특히 RR이 깨지는 경우).
- [ ] 액티브 vs 패시브 헬스체크, 슬로 스타트가 왜 필요한지.
- [ ] 스티키 세션의 트레이드오프 + **무상태가 더 낫다**는 결론.
- [ ] LB 자신을 어떻게 HA로 만드나(다중 LB, DNS/anycast, L4 앞단).
- [ ] 커넥션 드레이닝이 무엇이고 왜 무중단 배포에 필수인지.
- [ ] 일관성 해시가 `hash % N`보다 나은 이유(모듈 05 연결).

> 다음 / Next: `EVOLUTION.md` → `evolution/S0..S5` 실행 → `reference/`
> 읽기 → `blank/` 직접 구현 → `solution/` 비교 → `QUIZ.md` 확인 퀴즈.
