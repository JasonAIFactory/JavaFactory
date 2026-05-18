// =============================================================================
// P3 - Multi-tier cache: Caffeine L1 + Redis L2 (= evolution S5)
//
// KR: 특정 키가 너무 뜨거워 Redis 홉(~1ms)도 아까울 때. 프로세스 내
//     Caffeine(L1) 앞에 두고, 미스만 공유 Redis(L2)로. 가장 빠르지만
//     L1이 인스턴스별이라 잠깐 스테일 — 그 비용을 의식적으로 수용.
// EN: When a key is so hot even the ~1ms Redis hop hurts. Put Caffeine
//     (L1) in front, fall back to shared Redis (L2) on miss. Fastest,
//     but L1 is per-instance so it can be briefly stale - accept it.
//
// PRODUCTION REFERENCE - does NOT run here (needs Maven + Redis).
//
// build.gradle: caffeine + data-redis + cache starters (see P1/P2).
// =============================================================================

package production;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.*;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
class MultiTierConfig {

    // L1: tiny + SHORT ttl. Short ttl is the knob that BOUNDS L1 staleness.
    @Bean CaffeineCacheManager l1() {
        CaffeineCacheManager m = new CaffeineCacheManager("products");
        m.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(10, TimeUnit.SECONDS));     // keep L1 staleness window small
        return m;
    }

    // L2: shared, larger, longer ttl.
    @Bean RedisCacheManager l2(RedisConnectionFactory cf) {
        return RedisCacheManager.builder(cf)
                .cacheDefaults(org.springframework.data.redis.cache.RedisCacheConfiguration
                        .defaultCacheConfig().entryTtl(Duration.ofMinutes(10)))
                .build();
    }

    // Composite: Spring checks L1 first, then L2. (A custom two-level
    // CacheManager can also write-through L1<-L2; kept simple here.)
    @Bean @Primary
    CacheManager cacheManager(CaffeineCacheManager l1, RedisCacheManager l2) {
        CompositeCacheManager c = new CompositeCacheManager(l1, l2);
        c.setFallbackToNoOpCache(false);
        return c;
    }
}

@Service
class ProductService {
    // Same annotation. Read path: L1 -> L2 -> method(db). Populate back.
    @Cacheable(cacheNames = "products", key = "#id")
    public String getProduct(long id) { return loadFromDb(id); }

    // Evict must clear BOTH tiers; L1 of OTHER instances self-heals when
    // its short ttl expires (the accepted S5 trade-off).
    @CacheEvict(cacheNames = "products", key = "#id", allEntries = false)
    public void updateProduct(long id, String value) { saveToDb(id, value); }

    private String loadFromDb(long id) { return "product#" + id; }
    private void saveToDb(long id, String v) { /* ... */ }
}

// KR: 결론 - 계층이 늘수록 빨라지지만 스테일/복잡도 비용↑. 정말 핫한
//     키에만, L1 TTL을 짧게. 필요해지기 전엔 P1/P2로 충분.
// EN: More tiers = faster but more staleness/complexity. Use only for
//     truly hot keys, keep L1 TTL short. Before you need it, P1/P2 is plenty.
