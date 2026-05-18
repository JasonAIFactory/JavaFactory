# 캐시 진화 트랙 S0→S5 / Caching evolution track S0→S5

> KR: 캐시를 "한 번에 완성"으로 배우지 말 것. 실무는 **문제가 터질 때마다
> 한 단계씩** 진화한다. 각 단계는 실행되는 `.java`이고, 그 단계가 **어디서
> 깨지는지**를 assert로 증명한다. 다음 단계는 그 깨짐을 고친다.
> EN: Do not learn caching as "done in one shot". Real systems evolve
> **one step per failure**. Each stage is a runnable `.java` that proves
> with asserts **where it breaks**; the next stage fixes that break.

실행 / Run:
```
cd 02-caching/evolution
javac S*.java
java -ea S0_NoCache   # ... S1 ... S5
```

| 단계 / Stage | 한 줄 / One line | 깨지는 지점 / Breaks at |
| --- | --- | --- |
| **S0** `NoCache` | 캐시 없음, 매 읽기 DB / no cache, every read hits db | 같은 쿼리 반복, DB 부하 선형 증가 / same query repeats, db load grows linearly |
| **S1** `LocalCache` | 프로세스 내 맵 캐시 / in-process map | 인스턴스마다 캐시 따로 → 무효화 안 퍼짐 / per-instance caches → invalidation doesn't propagate |
| **S2** `DistributedCache` | 공유 캐시(Redis형) / shared cache | 네트워크 홉 추가, 무효화 **순서** 함정 남음 / network hop, invalidation ordering trap remains |
| **S3** `InvalidationRace` | cache-aside 읽기/쓰기 경합 / read-write race | 경합이 stale 값을 캐시에 재주입 → TTL로 한정 / race re-poisons cache → bound with TTL |
| **S4** `StampedePenetrationAvalanche` | 부하 3대 실패모드 / 3 load failure modes | 스탬피드·페네트레이션·어밸런치 → single-flight·null캐시·지터 / fixed by single-flight, null-cache, jitter |
| **S5** `MultiTierCache` | L1(로컬)+L2(공유) / L1 local + L2 shared | ns 읽기 얻고 L1이 잠깐 stale (수용 비용) / ns reads, cost = bounded L1 staleness |

## 핵심 교훈 / Key lessons

- KR: 캐시는 **정확성을 속도와 맞바꾸는** 장치다. 공짜가 아니다.
  EN: A cache **trades correctness for speed**. It is never free.
- KR: 가장 어려운 건 저장이 아니라 **무효화**다(S3). TTL은 만능이 아니라
  "피해를 한정"하는 안전망이다.
  EN: The hard part is not storing, it's **invalidation** (S3). TTL is not
  a cure; it is a safety net that **bounds the damage**.
- KR: 캐시는 DB를 보호하려고 둔다. 그런데 미스 폭주(S4)는 오히려 DB를 더
  죽인다. 그래서 single-flight가 필수.
  EN: A cache exists to protect the db, yet a miss storm (S4) amplifies db
  load. That is why single-flight is mandatory.
- KR: 더 빠르게(S5) = 더 많은 계층 = 더 많은 stale 위험. 계층마다
  일관성 비용을 의식적으로 받아들여라.
  EN: Faster (S5) = more tiers = more staleness risk. Accept the
  consistency cost of each tier deliberately.

> 다음 / Next: `../production/STANDARD_SOLUTION.md` — 실무에선 이걸 직접
> 안 만들고 Caffeine/Redis/CDN을 쓴다. / In production you don't build
> this; you use Caffeine/Redis/CDN.
