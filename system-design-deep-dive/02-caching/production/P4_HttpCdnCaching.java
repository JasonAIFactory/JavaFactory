// =============================================================================
// P4 - HTTP / CDN edge caching (Cache-Control, ETag)
//
// KR: 가장 싼 캐시. 공개·정적·읽기 위주 응답엔 캐시 헤더만 붙이면 CDN
//     (CloudFront/Cloudflare)이 사용자 근처에서 응답한다 → 트래픽이
//     앱·DB·Redis까지 아예 안 온다.
// EN: The cheapest cache. For public/static/read-heavy responses, just
//     set cache headers and a CDN (CloudFront/Cloudflare) serves them
//     near the user → traffic never reaches your app/db/Redis at all.
//
// PRODUCTION REFERENCE - does NOT run here (needs Maven + Spring + a CDN).
//
// build.gradle:
//   implementation "org.springframework.boot:spring-boot-starter-web"
// =============================================================================

package production;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;

@RestController
class PublicContentController {

    // ---- 1) Cache-Control: tell the CDN + browser how long to cache ----
    // public  = any shared cache (CDN) may store it
    // max-age = TTL at the edge (staleness bound, just like a cache TTL)
    // s-maxage / stale-while-revalidate are common production additions.
    @GetMapping("/api/articles/{id}")
    public ResponseEntity<String> getArticle(@PathVariable long id) {
        String body = loadArticle(id);                 // runs only on a CDN MISS
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                .body(body);
    }

    // ---- 2) ETag: revalidation without resending the body ----
    // The CDN/browser sends If-None-Match; if the ETag matches, we return
    // 304 Not Modified (tiny) instead of the full payload.
    @GetMapping("/api/articles/{id}/v2")
    public ResponseEntity<String> getArticleEtag(@PathVariable long id,
                                                 @RequestHeader(value = "If-None-Match", required = false) String inm) {
        String body = loadArticle(id);
        String etag = "\"" + Integer.toHexString(body.hashCode()) + "\"";
        if (etag.equals(inm)) {
            return ResponseEntity.status(304).eTag(etag).build();   // unchanged -> no body
        }
        return ResponseEntity.ok().eTag(etag)
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(body);
    }

    // ---- 3) Invalidation at the edge ----
    // Private/per-user data must NOT be cached at a shared CDN:
    //   .cacheControl(CacheControl.noStore())   // or cachePrivate()
    // To "invalidate" early, change the URL (content hashing, e.g.
    // /static/app.9f3a1.js) or issue a CDN purge via the CDN's API.

    private String loadArticle(long id) { return "<article " + id + ">"; }
}

// KR: 결론(= 캐시 계층의 가장 바깥) - 공개 읽기 트래픽은 여기서 끝내는
//     게 가장 싸다. 단, 사용자별/민감 데이터는 절대 공유 CDN 캐시 금지.
//     무효화는 URL 버전팅 또는 CDN purge.
// EN: Conclusion (the outermost cache tier) - end public read traffic
//     here; it's the cheapest. Never cache per-user/sensitive data in a
//     shared CDN. Invalidate via URL versioning or a CDN purge.
