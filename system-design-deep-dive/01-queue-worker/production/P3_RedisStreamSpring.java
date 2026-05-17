// =============================================================================
// P3 - Redis Streams with Spring Boot (Spring Data Redis)
//
// KR: 이미 Redis를 캐시로 쓰고 있으면 추가 인프라 0. 가볍고 빠르다.
//     단, RabbitMQ/Kafka보다 기능이 단순하고 내구성 보장이 약하다.
// EN: If you already run Redis (as a cache), this adds zero new infra.
//     Light and fast - but fewer features and weaker durability than
//     RabbitMQ/Kafka.
//
// PRODUCTION REFERENCE - does NOT run here (needs Maven + a Redis server).
//
// build.gradle:
//   implementation "org.springframework.boot:spring-boot-starter-data-redis"
//
// Redis Streams gives consumer GROUPS (competing consumers) and a
// pending list (PEL) for messages not yet acked. DLQ is manual: after N
// failed deliveries you XADD to a "dead" stream and XACK the original.
// =============================================================================

package production;

import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// ---- 1) PRODUCER: XADD to the stream ----
@Service
class SignupService {
    private final StringRedisTemplate redis;
    SignupService(StringRedisTemplate redis) { this.redis = redis; }

    public String signup(long userId) {
        redis.opsForStream().add("email.stream",
                Map.of("userId", String.valueOf(userId), "template", "welcome"));
        return "202 accepted";
    }
}

// ---- 2) CONSUMER: part of a consumer group (one of many app instances) ----
@Component
class EmailWorker implements StreamListener<String, MapRecord<String, String, String>> {

    private final StringRedisTemplate redis;
    private final Set<Long> processed = ConcurrentHashMap.newKeySet(); // -> DB unique key in real life
    EmailWorker(StringRedisTemplate redis) { this.redis = redis; }

    @Override
    public void onMessage(MapRecord<String, String, String> msg) {
        long userId = Long.parseLong(msg.getValue().get("userId"));
        try {
            if (processed.add(userId)) sendEmail(userId);            // idempotent
            redis.opsForStream().acknowledge("email.group", msg);    // ack: remove from pending list
        } catch (Exception e) {
            // manual retry/DLQ: count deliveries; after N, move to a dead stream + ack.
            redis.opsForStream().add("email.dead", msg.getValue());
            redis.opsForStream().acknowledge("email.group", msg);
        }
    }

    private void sendEmail(long userId) { /* call email gateway */ }
}

// A StreamMessageListenerContainer + a consumer group "email.group" is
// configured in a @Bean (omitted for brevity). Scale = more app
// instances joining the same group.
//
// KR: 비교 - 멱등성은 똑같이 네 책임. 재시도/DLQ는 SQS/Rabbit과 달리
//     "거의 수동"이다. 그래서 단순 잡에만 권장.
// EN: Comparison - idempotency is still your job. Unlike SQS/Rabbit,
//     retry/DLQ is mostly MANUAL here. So use it only for simple jobs.
