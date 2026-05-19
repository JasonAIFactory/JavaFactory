# 03 - Load Balancing: 실무형 진화 (Evolution)

> 읽는 법: 위에서 아래로 **순서대로**. 각 단계마다
> ① 지금 구조 → ② 무엇이 실제로 깨지는가 → ③ 그래서 다음 단계로.
> 코드(`evolution/S0..S5`)를 실제로 돌리면 그 "깨짐"이 콘솔에 숫자로 보입니다.
> 용어 자랑 없이, 엔지니어가 현장에서 실제로 내리는 판단만.

---

## 등장하는 구성요소 (먼저 이름만 명확히)

| 이름 | 현실에서 무엇인가 | 한 줄 역할 |
| --- | --- | --- |
| **Client** | 브라우저/모바일/다른 서비스 | 요청을 보냄 |
| **Backend / App Server** | 실제 일을 하는 웹 서버 (별도 머신) | 요청 처리. 용량·속도 유한 |
| **Load Balancer** | Nginx/Envoy/ALB 같은 별도 장비 | 트래픽을 건강한 백엔드로 분산 |
| **Health Check** | LB의 백그라운드 프로브 | 죽은 백엔드를 로테이션에서 제거 |
| **Hash Ring** | 일관성 해시 자료구조 | 같은 키 → 같은 백엔드(스티키) |

> 핵심 직관: Backend는 "일하는 사람", Load Balancer는 "입구의 안내원".
> 코드에서는 스레드/세마포어로 흉내 내지만, 실제로는 **각각 별도 서버**.

---

## S0 - 로드밸런서가 없다 (서버 1대)

**구조:** Client → App Server 1대 (동시 처리 한도 10).

**무엇이 깨지나 (`java S0_SingleServer.java`):**
```
Spike of 200 -> served=10 rejected=190
Server died -> next request served? false  (TOTAL OUTAGE)
```
- 1대의 **용량 천장**(여기 동시 10)을 넘는 스파이크는 거절.
- 그 1대가 죽으면 **전면 장애**(SPOF).

**그래서 다음:** 똑같은 서버를 여러 대로 늘리고 트래픽을 나눈다.

---

## S1 - 서버 여러 대 + 클라이언트 라운드로빈 (헬스체크 없음)

**구조:** Client가 서버 목록을 들고 0→1→2→0… 순서로 직접 분산
(= DNS 라운드로빈 / 멍청한 클라이언트 리스트).

**무엇이 깨지나 (`java S1_ClientRoundRobin.java`):**
```
all healthy:   served=30 failed=270   (처리량 자체는 S0보다 좋아짐)
server 1 DOWN: served=20 failed=280   (~1/3이 죽은 서버로)
```
- 라운드로빈은 **헬스를 모른다**. 죽은 서버에도 1/N을 계속 보낸다 →
  사용자가 에러를 본다.

**그래서 다음:** 헬스체크해서 죽은 백엔드를 빼는 진짜 LB.

---

## S2 - 헬스체크하는 진짜 로드밸런서

**구조:** Client → **Load Balancer**(백그라운드 헬스 프로브) → Backends.
프로브가 죽은 백엔드를 healthy 집합에서 제거/복구.

**무엇이 좋아지나 (`java S2_HealthCheckedLB.java`):**
```
Killing server 1 ... Reviving server 1 ...
During kill+revive: served=278 failed=3
```
- 서버가 죽었다 살아나도 클라이언트는 거의 에러를 안 본다.
- 남은 실패 ~3개는 **헬스체크 주기(~100ms) 동안의 누수**뿐.

**아직 순진한 것:** 라운드로빈은 모든 서버가 똑같이 빠르다고 가정.

---

## S3 - 라운드로빈 vs 최소연결 (서버가 불균등할 때)

**구조:** 서버 0은 5배 느림, 각 서버 동시 처리 4개(나머지는 큐 대기).

**무엇이 깨지나 (`java S3_LeastConnections.java`):**
```
round robin    avg=1033ms  p99=4302ms  max=4352ms
least-conn     avg=632ms   p99=2727ms  max=2971ms
```
- 라운드로빈은 느린 서버에도 **같은 개수**를 보낸다 → 그 서버 큐가
  쌓이고 **꼬리 지연(p99/max)이 폭발**.
- 최소연결은 in-flight 적은 쪽으로 보내 느린 서버를 자연히 피한다.

**그래서 다음:** 상태(세션)를 메모리에 들고 있는 앱은 분산되면 깨진다.

---

## S4 - 상태 있는 앱: "로그아웃" 버그 → 스티키

**구조:** 각 서버가 로그인 세션을 **메모리에** 보관.

**무엇이 깨지나 (`java S4_StickySessions.java`):**
```
round robin : 'logged out' errors = 300 / 400
sticky hash : 'logged out' errors = 0 / 400
```
- 라운드로빈이면 다음 요청이 다른 서버로 → 세션 없음 → 강제 로그아웃.
- 일관성 해시 스티키(같은 세션→같은 서버)면 0 에러.

**트레이드오프(면접 필수):** 부하 불균등(핫 서버), 그 서버 죽으면 세션
어차피 소실, 배포/스케일 어려움. → **실무 정답은 무상태 + Redis/JWT.**

**그래서 다음:** 이제 LB 자신이 SPOF·병목이다.

---

## S5 - LB가 SPOF가 된다 → HA + 드레이닝

**구조:** LB 다중화(클라이언트 페일오버) + 커넥션 드레이닝.

**무엇이 좋아지나 (`java S5_LbHaAndDraining.java`):**
```
Single LB  -> lb1 killed: 0/50 ok   <- TOTAL OUTAGE
HA two LBs -> lb1 killed: 50/50 ok  <- failed over to lb2

instant removal : dropped=32   <- 배포 중 in-flight 끊김
with draining   : dropped=0    <- 무손실
```
- LB 1대면 그게 새 SPOF. 2대 + 페일오버면 한 대 죽어도 생존.
- 백엔드를 즉시 빼면 in-flight 32개 드롭. DRAINING(신규 차단)→in-flight
  0 대기→제거 = **0 드롭** = 무중단 배포.

**완성:** 이제 헬스체크(S2) + 전략(S3) + 스티키(S4) + 드레이닝(S5)을
하나로 엮는 게 `blank/` 캡스톤.

---

## 한 장 요약 / One-page recap

| 단계 | 추가한 것 | 막은 사고 |
| --- | --- | --- |
| S0 | 없음 | (사고를 본다: 천장·SPOF) |
| S1 | 서버 N대 + RR | 용량 천장 |
| S2 | 헬스체크/이젝션 | 죽은 서버로 누수 |
| S3 | 최소연결 | 느린 서버 꼬리지연 |
| S4 | 스티키(일관성 해시) | 세션 유실(차선책) |
| S5 | LB 다중화 + 드레이닝 | LB SPOF, 배포 중 드롭 |

> 다음: `reference/R1..R3` 코드를 한 줄씩 "왜 이렇게?" 설명 → `blank/`에서
> 30분 버티며 직접 구현 → `solution/` 비교.
