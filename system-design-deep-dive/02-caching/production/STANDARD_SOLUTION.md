# 실무 표준 솔루션 — 캐시는 직접 만들지 않는다
# Real-world standard solutions — you do NOT build a cache

> KR: `reference/`·`evolution/` 코드는 **내부 원리 이해용**이다. 실무에선
> 캐시를 직접 안 짠다 — Caffeine/Redis/CDN을 쓴다. 여기 그 표준 4개와
> 자바/스프링 실제 코드가 있다.
> EN: The `reference/`/`evolution/` code is **to understand internals**.
> In production you use Caffeine/Redis/CDN. Here are the four standards
> with real Java/Spring code.

> ⚠️ KR: 이 폴더의 `.java`는 **실무 참조용**. Maven 의존성 + Redis/CDN이
> 필요해 이 레포에서 바로 실행되지 않는다. 원리 검증은 `reference/`·
> `evolution/`에서 실행으로 끝냈다.
> EN: The `.java` here is **production reference**; needs Maven + Redis/CDN
> so it does not run in this repo. Internals are run-verified in
> `reference/`/`evolution/`.

---

## 4개 표준 / The four standards

| 제품 / Product | 한 줄 / One line | 언제 / When |
| --- | --- | --- |
| **Caffeine** | 프로세스 내 L1 캐시(고성능 LRU/TTL) / in-process L1 | 단일·소수 인스턴스, ns 지연 / few instances, ns latency |
| **Redis** | 분산 공유 캐시 사실상 표준 / de facto distributed cache | 다중 인스턴스 공유 필요 / shared across instances |
| **Multi-tier** | Caffeine(L1)+Redis(L2) / both tiers | 초핫키 + 공유 둘 다 / hottest keys + sharing |
| **CDN / HTTP cache** | 엣지에서 캐시(Cache-Control/ETag) / cache at the edge | 정적·공개·읽기 위주 / static, public, read-heavy |

---

## 창업자 의사결정 가이드 / Founder decision guide

**KR.**
1. **Day 1 (단일/소수 인스턴스)** → **Caffeine**. 의존성 1개, 인프라 0,
   `@Cacheable`만 붙이면 끝. 가장 지루하고 가장 옳다.
2. **인스턴스 여러 대로 스케일아웃** → **Redis** 추가. 이제 캐시를
   공유해야 S1의 멀티 인스턴스 스테일 버그가 안 생긴다.
3. **공개·정적·읽기 폭주(이미지/목록/문서)** → 그 경로는 **CDN**으로.
   앱·DB까지 트래픽이 아예 안 온다(가장 싼 캐시).
4. **특정 키가 너무 뜨거워 Redis 홉도 아까움** → **Multi-tier**
   (Caffeine L1 + Redis L2). 단, L1 스테일 비용을 받아들일 때만.

> 원칙: **"필요해지기 전에 계층을 늘리지 마라."** Caffeine→Redis→(필요 시)
> Multi-tier. CDN은 공개 읽기엔 거의 항상 정답.

**EN.**
1. **Day 1 (one/few instances)** → **Caffeine**. One dependency, zero
   infra, just add `@Cacheable`. The most boring and most correct choice.
2. **Scale out to many instances** → add **Redis**. You now must share
   the cache or you hit the S1 multi-instance staleness bug.
3. **Public/static read-heavy (images, lists, docs)** → put that path on
   a **CDN**. Traffic never even reaches your app/db (the cheapest cache).
4. **A key is so hot even the Redis hop hurts** → **Multi-tier**
   (Caffeine L1 + Redis L2) — only if you accept the L1 staleness cost.

> Rule: **"Don't add a tier before you need it."**
> Caffeine → Redis → (only if needed) Multi-tier. CDN is almost always
> right for public reads.

---

## 코드 파일 / Code files (Spring Boot)

| 파일 / File | 표준 / Standard |
| --- | --- |
| `P1_CaffeineSpring.java` | Caffeine in-process cache (`@Cacheable`) |
| `P2_RedisSpring.java` | Redis distributed cache (`@Cacheable` + RedisCacheManager) |
| `P3_MultiTierSpring.java` | Caffeine L1 + Redis L2 composite |
| `P4_HttpCdnCaching.java` | HTTP/CDN edge caching (Cache-Control, ETag) |

> KR: 네 파일 모두 **같은 4가지**: ① 무엇을 캐시하나 ② 어떻게 무효화하나
> ③ TTL/축출 설정 위치 ④ 스테일 트레이드오프. 제품이 바뀌어도 **개념은
> 동일**.
> EN: All four show the **same four things**: ① what is cached ② how it
> is invalidated ③ where TTL/eviction is configured ④ the staleness
> trade-off. The **concepts stay the same** across products.
