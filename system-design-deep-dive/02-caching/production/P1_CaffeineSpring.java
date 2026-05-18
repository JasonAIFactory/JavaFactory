// =============================================================================
// P1 - Caffeine in-process cache with Spring Boot (@Cacheable)
//
// KR: 자바 캐시의 Day-1 기본값. 의존성 1개, 인프라 0. 단일/소수 인스턴스
//     에서 ns 단위 읽기. (= evolution S1을 검증된 라이브러리로)
// EN: The Day-1 default for Java caching. One dependency, zero infra,
//     ns reads on one/few instances. (= evolution S1, but a proven lib)
//
// PRODUCTION REFERENCE - does NOT run here (needs Maven + Spring).
//
// build.gradle:
//   implementation "org.springframework.boot:spring-boot-starter-cache"
//   implementation "com.github.ben-manes.caffeine:caffeine"
// =============================================================================

package production;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.annotation.*;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

// ---- 1) CONFIG: TTL + max size live HERE, not in your code ----
@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    CaffeineCacheManager cacheManager() {
        CaffeineCacheManager m = new CaffeineCacheManager("products");
        m.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)                         // bounded memory (eviction)
                .expireAfterWrite(5, TimeUnit.MINUTES)       // TTL = staleness bound
                .recordStats());                             // hit-ratio metrics
        return m;
    }
}

@Service
class ProductService {

    // ---- 2) READ: @Cacheable = cache-aside, done for you ----
    // First call: MISS -> method runs -> result cached under key=id.
    // Next calls within TTL: HIT -> method body skipped entirely.
    @Cacheable(cacheNames = "products", key = "#id")
    public String getProduct(long id) {
        return loadFromDb(id);                               // the "slow" work
    }

    // ---- 3) INVALIDATION: evict on write so reads refetch ----
    @CacheEvict(cacheNames = "products", key = "#id")
    public void updateProduct(long id, String value) {
        saveToDb(id, value);
        // cache entry for id is removed -> next read is a fresh MISS.
    }

    private String loadFromDb(long id) { return "product#" + id; }
    private void saveToDb(long id, String v) { /* ... */ }
}

// KR: 트레이드오프(= evolution S1) - 인스턴스마다 캐시가 따로다. 여러
//     대로 스케일아웃하면 한 대의 @CacheEvict가 다른 대에 안 퍼진다.
//     그때 P2(Redis)로.
// EN: Trade-off (= S1) - the cache is per-instance. Once you scale to
//     many instances, one node's @CacheEvict does not reach the others.
//     That is when you move to P2 (Redis).
