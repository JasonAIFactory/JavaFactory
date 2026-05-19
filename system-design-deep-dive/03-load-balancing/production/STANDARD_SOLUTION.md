# 실무 표준 솔루션 — 창업하면 실제로 뭘 쓰나
# Real-world standard solutions — what you actually use at a startup

> KR: 앞의 `reference/`, `evolution/` 코드는 **LB 내부 원리를 이해하려고
> 직접 만든 것**이다. 실무에선 로드밸런서를 직접 구현하지 않는다 —
> **검증된 제품을 쓴다.** 여기 그 제품들과 실제 설정/코드가 있다.
> EN: The `reference/`/`evolution/` code was built **to understand the
> internals**. In production you do NOT build an LB — you **use a proven
> product**. Here are those products and the actual config/code.

> ⚠️ KR: 이 폴더의 `.java`는 **실무 참조용**이다. Maven + 실제 LB/클라우드가
> 필요해 이 레포에서 바로 실행되지 않는다. 원리 검증은 `reference/`·
> `evolution/`에서 이미 실행으로 끝냈다.
> EN: The `.java` here is **production reference**. It needs Maven + a real
> LB/cloud, so it does not run in this repo. The internals are already
> run-verified in `reference/`/`evolution/`.

---

## 4개 표준 선택 비교 / The four standard choices

| 제품 / Product | 계층 | 한 줄 / One line | 언제 / When | 운영 부담 |
| --- | --- | --- | --- | --- |
| **AWS ALB** | L7 | 완전관리 HTTP LB / managed HTTP LB | 클라우드 + HTTP API/웹 | 거의 없음 |
| **AWS NLB** | L4 | 완전관리 초고처리량 / managed, ultra-fast | TCP·고정 IP·최저 지연 | 거의 없음 |
| **Nginx / HAProxy** | L7(/L4) | 자체 운영 표준 / self-run standard | 온프렘·VM, 세밀 제어 | 중간 (직접 운영) |
| **Envoy** | L7 | 메시 데이터플레인 / mesh data plane | k8s/서비스 메시, 고급 기능 | 중간~높음 |
| (Spring Cloud LB) | client | LB 없이 클라 분산 / client-side | MSA 서비스 간 호출 | 낮음 (코드에 내장) |

---

## 결정 가이드 / Decision guide

- **클라우드(AWS)에서 HTTP 서비스 시작** → **ALB**. 경로/호스트 라우팅,
  TLS 종료, 헬스체크, 드레이닝(`deregistration_delay`)이 다 설정값.
  *Starting an HTTP service on AWS → ALB. Everything is a setting.*
- **최저 지연·초고처리량·고정 IP(게임 서버, DB 프록시, gRPC 엣지)** →
  **NLB(L4)**, 보통 그 뒤에 ALB/Envoy(L7)를 둔다.
  *Lowest latency / static IP → NLB (L4), usually with L7 behind it.*
- **온프렘·VM, 세밀한 제어가 필요** → **Nginx**(또는 HAProxy).
  *On-prem / fine control → Nginx or HAProxy.*
- **Kubernetes·서비스 메시, 자동 이상치 추방·일관성 해시·zone-aware** →
  **Envoy**(Istio/Consul가 관리).
  *k8s / mesh, advanced features → Envoy via the mesh.*
- **서비스→서비스 내부 호출, LB 홉을 없애고 싶음** → **클라이언트 사이드
  LB**(Spring Cloud LoadBalancer) + 서비스 디스커버리(모듈 14).
  *Service-to-service, skip the hop → client-side LB + discovery.*

---

## 어디서나 똑같이 챙겨야 하는 5가지 / The 5 things that are always on you

제품이 무엇이든 **앱과 운영 쪽에서** 반드시 해야 하는 것:

1. **싸고 정직한 `/healthz`.** 전 세계가 아니라 *이 노드가 못 버티는*
   것만 확인. 항상 200 주는 헬스는 LB를 무력화한다.
   *A cheap, honest health endpoint. A lying /health defeats the LB.*
2. **Graceful shutdown.** 종료 신호 → 신규 거부 → in-flight 마무리 →
   종료. (`server.shutdown=graceful`, k8s `preStop` + grace period.)
   드레이닝은 LB만이 아니라 **앱도** 협조해야 무손실(`S5` 참고).
3. **무상태 + 외부 세션(Redis/JWT).** 스티키는 차선책일 뿐(`S4`).
   *Stateless + shared session. Stickiness is only a fallback.*
4. **타임아웃 + 재시도(다음 노드로)** 설정. 단, 재시도는 멱등할 때만
   (모듈 10 idempotency와 연결). 무지성 재시도는 장애를 증폭한다.
5. **LB 자신을 HA로.** 단일 LB는 새 SPOF(`S5`). 매니지드 LB는 AWS가
   다중 AZ로 해주고, 자체 운영이면 LB 2대+VRRP/keepalived 또는 anycast.

---

## 한 줄 결론 / One-line conclusion

> KR: **"클라우드면 ALB(필요시 앞에 NLB), 자체 운영이면 Nginx/Envoy. LB는
> 사지만, `/healthz`·graceful shutdown·무상태·HA는 내가 책임진다."**
> EN: **"On cloud use ALB (NLB in front if needed); self-run use
> Nginx/Envoy. You buy the LB, but the honest health endpoint, graceful
> shutdown, statelessness, and LB high-availability are on you."**

> 다음 모듈: **04 Rate Limiting** — LB로 분산해도, 한 클라이언트가
> 폭주하면 풀 전체가 죽는다. 그걸 막는 게 다음 부품.
