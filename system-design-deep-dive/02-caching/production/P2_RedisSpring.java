// =============================================================================
// P2 - Redis distributed cache with Spring Boot (@Cacheable + Redis)
//
// KR: 분산 캐시의 사실상 표준. 인스턴스 여러 대가 같은 캐시를 공유 →
//     S1의 멀티 인스턴스 스테일 버그가 사라진다. (= evolution S2)
// EN: The de facto distributed cache. Many instances share ONE cache,
//     so S1's multi-instance staleness bug disappears. (= evolution S2)
//
// PRODUCTION REFERENCE - does NOT run here (needs Maven + a Redis server).
//
// build.gradle:
//   implementation "org.springframework.boot:spring-boot-starter-cache"
//   implementation "org.springframework.boot:spring-boot-starter-data-redis"
// application.yml:
//   spring.data.redis.host: localhost
// =============================================================================

package production;

import org.springframework.cache.annotation.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import java.time.Duration;

// ---- 1) CONFIG: same annotations as P1, but the store is Redis ----
@Configuration
@EnableCaching
class RedisCacheConfig {
    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory cf) {
        RedisCacheConfiguration cfg = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))            // TTL on the shared cache
                .disableCachingNullValues();                 // (see penetration note below)
        return RedisCacheManager.builder(cf).cacheDefaults(cfg).build();
    }
}

@Service
class ProductService {

    // Identical code to P1 - only the cache manager bean changed.
    // The win: a HIT here is shared by EVERY app instance.
    @Cacheable(cacheNames = "products", key = "#id")
    public String getProduct(long id) {
        return loadFromDb(id);
    }

    // @CacheEvict now removes the key from the SHARED Redis -> every
    // instance sees the fresh value on its next read (fixes S1).
    @CacheEvict(cacheNames = "products", key = "#id")
    public void updateProduct(long id, String value) {
        saveToDb(id, value);
    }

    private String loadFromDb(long id) { return "product#" + id; }
    private void saveToDb(long id, String v) { /* ... */ }
}

// KR: 주의(= evolution S3/S4) - ① 무효화 경합은 여전히 존재: TTL이
//     안전망. ② penetration 방어가 필요하면 null도 짧게 캐시(설정에서
//     null 허용 + 짧은 TTL). ③ 스탬피드는 sync=true 또는 분산 락으로.
// EN: Caveats (= S3/S4) - ① the invalidation race still exists: TTL is
//     the safety net. ② for penetration, cache the null with a short
//     TTL. ③ for stampede use sync=true or a distributed lock.
//   @Cacheable(cacheNames="products", key="#id", sync=true)  // single-flight per JVM
