# 03 - Load Balancing 퀴즈 (한글 ↔ English)

> 각 문제 한글로 생각하고, 영어 문장으로도 답해보기 (실무에선 영어로 말함).
> Think in Korean, then answer in English too (you'll speak English at work).

---

## A. 진단 퀴즈 / Diagnostic (이론 보기 전 / before theory — 8문제)

1. KR: 서버 1대로만 받으면 터지는 것 4가지?
   EN: Name 4 things that break with only one server.
2. KR: L4와 L7 로드밸런서의 핵심 차이 한 문장?
   EN: One-sentence difference between an L4 and an L7 LB?
3. KR: 라운드로빈이 "공평하지 않은" 대표 상황은?
   EN: When is round robin NOT fair?
4. KR: 액티브 헬스체크와 패시브 이젝션의 차이?
   EN: Active health check vs passive ejection?
5. KR: 스티키 세션의 트레이드오프 2개 + 더 나은 대안?
   EN: Two trade-offs of sticky sessions + the better alternative?
6. KR: `hash % N` 대신 일관성 해시를 쓰는 이유?
   EN: Why consistent hashing instead of `hash % N`?
7. KR: 백엔드를 무중단으로 빼려면? 그 절차 이름은?
   EN: How do you remove a backend with zero dropped requests? Name it.
8. KR: LB 자신이 SPOF가 되는 문제를 어떻게 푸나?
   EN: How do you stop the LB itself from being a SPOF?

<details><summary>👉 정답 / Answers</summary>

1. KR: 용량 천장, 단일 장애점(SPOF), 수평 확장 불가, 무중단 배포 불가.
   EN: capacity ceiling, single point of failure, no horizontal scale,
   no zero-downtime deploy.
2. KR: L4는 IP·포트만 보고 연결을 빠르게 전달; L7은 HTTP를 읽어 경로/쿠키
   라우팅·TLS 종료가 가능(약간 느림).
   EN: L4 forwards a connection by IP/port (fast, dumb); L7 reads HTTP so
   it can route by path/cookie and terminate TLS (a bit slower).
3. KR: 서버 성능이 다르거나 요청 비용이 다를 때. 같은 개수를 보내도 느린
   서버 큐가 쌓여 꼬리 지연 폭발.
   EN: when servers or request costs are uneven — equal COUNT still piles
   up the slow server (tail latency blows up).
4. KR: 액티브 = LB가 `/health`를 주기적으로 찔러 죽으면 제거; 패시브 =
   실제 요청이 연속 N번 실패하면 제거(`/health`가 거짓일 때 대비).
   EN: active = LB probes `/health` and ejects; passive = eject after N
   consecutive real-request failures (for when `/health` lies).
5. KR: 부하 불균등(핫 서버), 서버 죽으면 세션 소실, 배포/스케일 어려움 →
   더 나은 건 무상태 서버 + Redis/JWT.
   EN: uneven load, sessions lost if the node dies, harder deploys →
   better: stateless servers + shared store (Redis) / JWT.
6. KR: `hash % N`은 N이 바뀌면 거의 모든 키가 재배치(캐시 폭망). 일관성
   해시는 추가/제거된 노드의 키만 이동.
   EN: `hash % N` reshuffles almost all keys when N changes (cache
   meltdown); consistent hashing moves only the changed node's keys.
7. KR: 커넥션 드레이닝 — DRAINING(신규 차단) → in-flight 0 대기 → 제거.
   EN: connection draining — DRAINING (no new) → wait in-flight == 0 →
   remove.
8. KR: LB 다중화 + 클라이언트 페일오버, DNS 라운드로빈/anycast, 앞단 L4
   레이어(Maglev식).
   EN: multiple LBs + client failover, DNS round robin / anycast, an L4
   layer in front (Maglev-style).

</details>

---

## B. 확인 퀴즈 / Check quiz (모듈 끝 / after the module — 10문제)

1. KR: `S0`에서 200개 중 10개만 처리된 이유는? 어떤 한계인가?
   EN: In `S0`, why were only 10 of 200 served? What limit is that?
2. KR: `S1`에서 서버 1을 죽였더니 ~1/3 실패. 무엇이 문제인가?
   EN: In `S1`, killing server 1 failed ~1/3. What is the flaw?
3. KR: `S2`에서 남은 소수의 실패는 무엇 때문인가? 줄이려면?
   EN: In `S2`, what causes the few remaining failures? How to reduce?
4. KR: `S3`에서 round robin의 p99/max가 폭발한 메커니즘을 설명하라.
   EN: In `S3`, explain WHY round robin's p99/max exploded.
5. KR: `S4`에서 sticky로 0 에러가 됐다. 그래도 권장 안 하는 이유는?
   EN: `S4` got 0 errors with sticky. Why is it still not recommended?
6. KR: `R3`에서 노드 1개 제거 시 약 1/3만 이동했다. 이게 왜 중요한가?
   EN: In `R3`, removing 1 node moved only ~1/3 of keys. Why does that
   matter?
7. KR: `S5`에서 instant removal이 32개를 드롭한 이유와, draining이 0인 이유.
   EN: In `S5`, why did instant removal drop 32 and draining drop 0?
8. KR: blank의 Trap A(라운드로빈 인덱싱)는 어떤 assert를 깨뜨리나?
   EN: Which assert does blank's Trap A (round-robin indexing) break?
9. KR: blank의 Trap B(드레이닝 종료 판정)는 어떤 assert를 깨뜨리나?
   EN: Which assert does blank's Trap B (drain completion) break?
10. KR: 헬스체크 주기를 너무 짧게 하면 생기는 문제?
    EN: What goes wrong if the health-check interval is too short?

<details><summary>👉 정답 / Answers</summary>

1. KR: 동시 처리 한도(세마포어 10) = 용량 천장. 그 이상은 즉시 거절.
   EN: the concurrency cap (semaphore = 10) = the capacity ceiling;
   anything beyond is rejected immediately.
2. KR: 라운드로빈이 헬스를 모름 → 죽은 서버에도 1/N을 계속 보냄.
   EN: round robin is health-blind → keeps sending 1/N to the dead node.
3. KR: 헬스체크 주기(~100ms) 동안 죽은 서버로 샌 요청. 주기를 줄이거나
   패시브 이젝션 추가.
   EN: requests that leaked during the ~100ms detection gap; shorten the
   interval or add passive ejection.
4. KR: 느린 서버도 같은 개수를 받음 + 동시 처리 한도 → 큐가 쌓이고 그
   큐에서 대기한 시간이 응답시간에 더해져 꼬리 지연 폭발.
   EN: the slow server gets an equal count but has a concurrency cap, so
   requests wait in its queue and that wait is added to latency → tail
   blows up.
5. KR: 부하 불균등, 노드 죽으면 세션 소실, 배포/오토스케일 어려움. 무상태
   + 외부 세션이 더 견고.
   EN: uneven load, session loss on node death, harder deploy/autoscale;
   stateless + external session is more robust.
6. KR: 일관성 해시의 핵심 이점 — 멤버십 변경 시 캐시/스티키가 거의 안
   깨짐(대량 재배치 = 캐시 폭망 회피).
   EN: it is the whole point of consistent hashing — membership changes
   barely disturb cache/stickiness (avoids a mass reshuffle).
7. KR: instant는 in-flight 중인 요청 위에서 백엔드를 REMOVED로 바꿔
   끊어버림; draining은 신규를 막고 in-flight가 0이 될 때까지 기다린 뒤
   제거하므로 0.
   EN: instant flips the backend to REMOVED while requests are mid-flight
   (reset); draining blocks new and waits for in-flight to reach 0 first.
8. KR: 전체 리스트로 인덱싱하면 ejected 노드를 반환 → "routed to an
   unhealthy backend!" assert(테스트 3) 실패.
   EN: indexing the full list can return an ejected node → test 3's
   "routed to an unhealthy backend!" assert fails.
9. KR: DRAINING 되자마자 REMOVED로 바꾸면 in-flight가 드롭 → "draining
   must not drop in-flight"(테스트 4) 실패.
   EN: marking REMOVED right after DRAINING drops in-flight → test 4's
   "draining must not drop in-flight" assert fails.
10. KR: 헬스체크 자체가 백엔드에 부하가 됨(매 짧은 주기마다 N대 프로브).
    EN: the health checks themselves become load on the backends (probing
    N nodes every short interval).

</details>

---

## C. 파인만 테스트 / Feynman test (말로 설명)

표를 덮고, 친구에게 **식당 안내원 비유**로 다음을 90초에 설명하라:
Cover the page and explain to a friend, in 90 seconds, with the
restaurant-host analogy:

- 로드밸런서가 왜 필요한가 / why an LB exists
- L4 vs L7 / L4 vs L7
- 라운드로빈이 깨지는 순간 / when round robin breaks
- 헬스체크(액티브/패시브) / health checks
- 스티키의 트레이드오프 / sticky trade-off
- LB 자신을 어떻게 HA로 / how the LB itself stays available
- 커넥션 드레이닝 / connection draining

> 막힘 없이 영어로 말할 수 있을 때까지. Until you can say it in English
> without stopping.
