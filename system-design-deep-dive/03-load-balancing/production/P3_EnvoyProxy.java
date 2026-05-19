// =============================================================================
// P3 - Envoy as the L7 proxy (the modern service-mesh data plane)
//
// KR: Istio/Consul mesh의 데이터플레인. 활성+수동 헬스(outlier detection),
//     일관성 해시(ring hash), panic threshold 등 LB의 "교과서"가 다 있음.
// EN: The data plane of Istio/Consul mesh. It has the full LB textbook:
//     active + passive (outlier detection) health, consistent hashing
//     (ring hash), and a panic threshold.
//
// PRODUCTION REFERENCE - config only; nothing to run here.
//
// ---- envoy.yaml (the cluster that matters) ----------------------------
//
//   clusters:
//   - name: app_pool
//     connect_timeout: 1s
//     lb_policy: LEAST_REQUEST          # or ROUND_ROBIN / RING_HASH / RANDOM
//     # RING_HASH = consistent hashing (sticky + minimal reshuffle):
//     # lb_policy: RING_HASH
//     # ring_hash_lb_config: { minimum_ring_size: 1024 }
//
//     load_assignment:
//       cluster_name: app_pool
//       endpoints:
//       - lb_endpoints:
//         - endpoint: { address: { socket_address: { address: 10.0.0.11, port_value: 8080 }}}
//         - endpoint: { address: { socket_address: { address: 10.0.0.12, port_value: 8080 }}}
//
//     health_checks:                    # ACTIVE health check
//     - timeout: 1s
//       interval: 2s
//       unhealthy_threshold: 2
//       healthy_threshold: 2
//       http_health_check: { path: "/healthz" }
//
//     outlier_detection:                # PASSIVE ejection (auto)
//       consecutive_5xx: 5              # 5 bad responses -> eject
//       base_ejection_time: 30s         # for at least 30s
//       max_ejection_percent: 50        # never eject more than half
//
//     # PANIC THRESHOLD: if fewer than 50% of hosts are healthy, Envoy
//     # sends to ALL hosts (healthy or not). The logic: a half-broken
//     # cluster is better than hammering the few survivors to death.
//     common_lb_config: { healthy_panic_threshold: { value: 50 } }
//
// ---- Draining ----------------------------------------------------------
//   Envoy supports graceful drain via the admin endpoint
//   (POST /drain_listeners?graceful) and DRAINING endpoint health status,
//   so a rollout finishes in-flight requests before the host is removed.
// =============================================================================

package production;

// No Java needed - Envoy is configured by YAML/xDS, not code. The Spring
// app behind it only needs the same cheap /healthz as in P1.
// Key idea to remember for interviews: Envoy's "outlier detection" is
// passive ejection done automatically, and "panic threshold" is the
// safety valve when too much of the pool is unhealthy.
class P3_EnvoyProxy { }
