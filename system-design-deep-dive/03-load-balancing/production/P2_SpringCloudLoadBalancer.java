// =============================================================================
// P2 - Client-side load balancing (Spring Cloud LoadBalancer)
//
// KR: LB 장비 없이 클라이언트가 직접 인스턴스 목록을 받아 분산. MSA에서
//     서비스→서비스 호출에 흔함(Netflix Ribbon의 후계자).
// EN: No LB box - the CLIENT gets the instance list and balances itself.
//     Common for service-to-service calls in MSA (the successor to Ribbon).
//
// PRODUCTION REFERENCE - does NOT run in this repo (needs Maven + discovery).
//
// build.gradle:
//   implementation "org.springframework.cloud:spring-cloud-starter-loadbalancer"
//   implementation "org.springframework.cloud:spring-cloud-starter-netflix-eureka-client"
//
// Pros: no extra network hop, no LB to operate.
// Cons: every client must embed the LB logic; consistency across languages
//       is hard; you still need service discovery (Eureka/Consul) for the
//       live instance list. (Service discovery = module 14.)
// =============================================================================

package production;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Configuration
class LbConfig {

    // @LoadBalanced makes RestClient resolve "http://order-service" via the
    // discovery client and pick a healthy instance per call (round robin by
    // default; swap the strategy with a LoadBalancerClient config).
    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClient() {
        return RestClient.builder();
    }
}

@Service
class OrderClient {
    private final RestClient http;

    OrderClient(RestClient.Builder lb) {
        this.http = lb.build();
    }

    public String placeOrder(String body) {
        // "order-service" is a LOGICAL name, not a host. The client-side LB
        // turns it into a concrete healthy instance for THIS request.
        return http.post()
                .uri("http://order-service/orders")
                .body(body)
                .retrieve()
                .body(String.class);
    }
}

// To use least-loaded instead of round robin, define a per-service config:
//   @LoadBalancerClient(name = "order-service", configuration = LeastConn.class)
// and provide a ReactorLoadBalancer<ServiceInstance> bean there.
// Health-aware instance lists come from the discovery client + actuator
// /health, so dead instances are dropped from the list automatically.
class P2_SpringCloudLoadBalancer { }
