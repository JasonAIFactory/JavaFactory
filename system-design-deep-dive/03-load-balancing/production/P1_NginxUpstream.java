// =============================================================================
// P1 - Nginx as the L7 load balancer in front of Spring Boot
//
// KR: 가장 흔한 "그냥 앞에 한 대" 선택. 설정 파일 한 장으로 라운드로빈/
//     least_conn/ip_hash + 패시브 헬스체크 + 드레이닝을 다 한다.
// EN: The most common "just put one in front" choice. One config file gives
//     round robin / least_conn / ip_hash + passive health + draining.
//
// PRODUCTION REFERENCE - does NOT run in this repo (needs Nginx + Maven).
// The internals are already run-verified in reference/ and evolution/.
//
// ---- nginx.conf (the part that matters) -------------------------------
//
//   upstream app_pool {
//       least_conn;                       # algorithm (default: round robin)
//       # ip_hash;                        # OR sticky by client IP
//
//       server 10.0.0.11:8080 max_fails=3 fail_timeout=10s;  # passive eject
//       server 10.0.0.12:8080 max_fails=3 fail_timeout=10s;
//       server 10.0.0.13:8080 max_fails=3 fail_timeout=10s weight=2; # weighted
//       server 10.0.0.14:8080 backup;     # only used if all others are down
//   }
//
//   server {
//       listen 443 ssl;                   # TLS termination here (L7)
//       location / {
//           proxy_pass http://app_pool;
//           proxy_next_upstream error timeout http_502 http_503; # retry next
//           proxy_connect_timeout 2s;
//       }
//       location = /healthz { access_log off; proxy_pass http://app_pool; }
//   }
//
//   # Active health checks (proactive ejection) are an Nginx Plus feature:
//   #   health_check uri=/healthz interval=2s fails=2 passes=2;
//   # Open-source Nginx only has PASSIVE checks (max_fails/fail_timeout).
//
// ---- Draining on deploy ------------------------------------------------
//   Mark a node down WITHOUT killing in-flight requests:
//     1) `server 10.0.0.13:8080 down;`  then `nginx -s reload`
//        (reload finishes existing requests on old workers = graceful)
//     2) deploy the node, then remove `down` and reload again.
//   In Kubernetes this is the pod `preStop` hook + `terminationGracePeriod`.
// =============================================================================

package production;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// The backend that sits behind Nginx. Many identical copies of THIS run.
// The only LB-specific thing the app must provide is a cheap health endpoint.
@SpringBootApplication
@RestController
public class P1_NginxUpstream {

    public static void main(String[] args) {
        SpringApplication.run(P1_NginxUpstream.class, args);
    }

    @GetMapping("/healthz")
    public String health() {
        // Keep this CHEAP and HONEST. Check the things that make THIS node
        // unable to serve (DB pool, critical downstream), not the whole world.
        // A lying /health (always 200) defeats the load balancer.
        return "ok";
    }

    @GetMapping("/work")
    public String work() {
        return "served by " + System.getenv().getOrDefault("HOSTNAME", "node");
    }
}
