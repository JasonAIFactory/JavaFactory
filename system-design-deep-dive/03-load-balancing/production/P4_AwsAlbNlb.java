// =============================================================================
// P4 - AWS ALB (L7) vs NLB (L4) - the managed cloud choice
//
// KR: 클라우드에서 LB를 직접 운영하지 않는 가장 흔한 선택. ALB=L7(경로/호스트
//     라우팅), NLB=L4(초고처리량·고정 IP). 헬스체크·드레이닝이 설정값.
// EN: The most common choice - you operate no LB box. ALB = L7 (path/host
//     routing), NLB = L4 (very high throughput, static IP). Health checks
//     and draining are just settings.
//
// PRODUCTION REFERENCE - infra config (Terraform), nothing to run here.
//
//   ALB (L7) - choose when you need HTTP routing / TLS / cookies:
//   --------------------------------------------------------------------
//   resource "aws_lb" "app" {
//     load_balancer_type = "application"          # ALB = L7
//     enable_cross_zone_load_balancing = true     # even across AZs
//   }
//   resource "aws_lb_target_group" "app" {
//     port = 8080
//     protocol = "HTTP"
//     deregistration_delay = 30                   # <-- CONNECTION DRAINING
//     health_check {
//       path = "/healthz"
//       interval = 10
//       healthy_threshold = 2
//       unhealthy_threshold = 2                   # ACTIVE health check
//     }
//     stickiness {                                # optional sticky (cookie)
//       type = "lb_cookie"
//       cookie_duration = 3600
//       enabled = false                           # prefer stateless!
//     }
//   }
//
//   NLB (L4) - choose for raw TCP/UDP, lowest latency, static IP:
//   --------------------------------------------------------------------
//   resource "aws_lb" "edge" { load_balancer_type = "network" }  # NLB = L4
//   # No path routing, no cookie stickiness, no TLS-aware rules.
//   # Pattern: NLB (L4, anycast-ish, fixed IP) -> ALB/Envoy (L7) -> pods.
//
// Key mappings to remember:
//   - deregistration_delay  == connection draining (zero-downtime deploy)
//   - unhealthy_threshold   == active health-check ejection
//   - cross_zone            == don't let one AZ get hot
//   - target group          == the "backend pool" from THEORY.md
// =============================================================================

package production;

// AWS LB is infrastructure, not application code. The Spring app behind a
// target group only needs the same cheap, honest /healthz as P1, plus a
// graceful-shutdown hook so in-flight requests finish during the
// deregistration_delay window:
//   server.shutdown=graceful
//   spring.lifecycle.timeout-per-shutdown-phase=25s
class P4_AwsAlbNlb { }
