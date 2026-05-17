// =============================================================================
// P4 - Apache Kafka with Spring Boot (Spring for Apache Kafka)
//
// KR: 분산 로그. 초고처리량 + 파티션 순서 + 재처리(offset 되감기) +
//     여러 독립 소비자 그룹. 강력하지만 운영이 무겁다 - 필요해지기
//     전에는 쓰지 말 것.
// EN: A distributed log. Huge throughput + per-partition order + replay
//     (rewind offset) + many independent consumer groups. Powerful but
//     heavy to operate - do NOT adopt before you need it.
//
// PRODUCTION REFERENCE - does NOT run here (needs Maven + a Kafka cluster).
//
// build.gradle:
//   implementation "org.springframework.kafka:spring-kafka"
//
// Retry + DLQ: a DefaultErrorHandler with exponential backoff + a
// DeadLetterPublishingRecoverer (auto-publishes failures to <topic>.DLT).
// Ordering: messages with the same KEY go to the same partition.
// =============================================================================

package production;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.backoff.ExponentialBackOff;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// ---- 1) PRODUCER: key = userId so the same user keeps order ----
@Service
class SignupService {
    private final KafkaTemplate<String, EmailJob> kafka;
    SignupService(KafkaTemplate<String, EmailJob> kafka) { this.kafka = kafka; }

    public String signup(long userId) {
        // same key -> same partition -> ordered per user
        kafka.send(new ProducerRecord<>("email.topic", String.valueOf(userId),
                new EmailJob(userId, "welcome")));
        return "202 accepted";
    }
}

record EmailJob(long userId, String template) {}

// ---- 2) ERROR HANDLING: backoff + auto DLQ (topic "email.topic.DLT") ----
@Configuration
class KafkaErrorConfig {
    @Bean
    DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
        var backoff = new ExponentialBackOff(1000L, 2.0); // 1s, 2s, 4s ...
        backoff.setMaxElapsedTime(30000L);
        return new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(template), backoff);
    }
}

// ---- 3) CONSUMER: a consumer group; partitions split across instances ----
@Component
class EmailWorker {
    private final Set<Long> processed = ConcurrentHashMap.newKeySet(); // -> DB unique key in real life

    @KafkaListener(topics = "email.topic", groupId = "email-workers")
    public void handle(EmailJob job) {
        if (!processed.add(job.userId())) return;   // idempotent (at-least-once delivery)
        sendEmail(job);                              // throw -> backoff retries -> email.topic.DLT
    }

    private void sendEmail(EmailJob job) { /* call email gateway */ }
}

// application.yml:
//   spring.kafka.consumer.group-id: email-workers
//   spring.kafka.consumer.enable-auto-commit: false   # commit after success
//   # scale = more partitions + more app instances in the same group
//
// KR: 4개 비교 결론 - 프로듀서/컨슈머/재시도/DLQ/멱등이라는 "개념"은
//     SQS·Rabbit·Redis·Kafka 모두 동일. 바뀌는 건 운영 부담과 추가 기능
//     (Kafka: 순서·재처리·다중 소비자 그룹)뿐. 그래서 가장 단순한 걸로
//     시작하는 게 옳다.
// EN: Conclusion across the four - the CONCEPTS (producer/consumer/
//     retry/DLQ/idempotency) are identical in SQS, Rabbit, Redis, Kafka.
//     Only the ops burden and extra features differ (Kafka adds order,
//     replay, many consumer groups). That is why starting with the
//     simplest is the right call.
