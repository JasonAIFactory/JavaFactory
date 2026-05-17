// =============================================================================
// P2 - RabbitMQ with Spring Boot (Spring AMQP)
//
// KR: 전통적 메시지 브로커. 라우팅(어떤 메시지를 어느 큐로)이 강력하다.
//     온프레미스이거나 정교한 라우팅이 필요할 때.
// EN: Classic message broker. Powerful routing (which message goes to
//     which queue). Use it on-prem or when routing is complex.
//
// PRODUCTION REFERENCE - does NOT run here (needs Maven + a RabbitMQ server).
//
// build.gradle:
//   implementation "org.springframework.boot:spring-boot-starter-amqp"
//
// DLQ + retry: declared as queue arguments (x-dead-letter-exchange) and
// listener retry properties. The broker moves dead messages for you.
// =============================================================================

package production;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// ---- 0) TOPOLOGY: main queue + DLQ, declared once at startup ----
@Configuration
class RabbitTopology {
    @Bean Queue emailDlq() { return new Queue("email.dlq"); }

    @Bean Queue emailQueue() {
        return QueueBuilder.durable("email.queue")
                .withArgument("x-dead-letter-exchange", "")        // default exchange
                .withArgument("x-dead-letter-routing-key", "email.dlq") // -> DLQ after retries exhausted
                .build();
    }
}

record EmailJob(long userId, String template) {}

// ---- 1) PRODUCER ----
@Service
class SignupService {
    private final RabbitTemplate rabbit;
    SignupService(RabbitTemplate rabbit) { this.rabbit = rabbit; }

    public String signup(long userId) {
        rabbit.convertAndSend("email.queue", new EmailJob(userId, "welcome"));
        return "202 accepted";
    }
}

// ---- 2) CONSUMER (worker) ----
@Component
class EmailWorker {
    private final Set<Long> processed = ConcurrentHashMap.newKeySet(); // -> DB unique key in real life

    @RabbitListener(queues = "email.queue", concurrency = "3-10") // 3..10 workers, competing consumers
    public void handle(EmailJob job) {
        if (!processed.add(job.userId())) return;   // idempotent
        sendEmail(job);                              // throw -> retried; after limit -> email.dlq
    }

    private void sendEmail(EmailJob job) { /* call email gateway */ }
}

// application.yml:
//   spring.rabbitmq.listener.simple.retry.enabled: true
//   spring.rabbitmq.listener.simple.retry.max-attempts: 5
//   spring.rabbitmq.listener.simple.retry.initial-interval: 1000ms
//   spring.rabbitmq.listener.simple.retry.multiplier: 2      # exponential backoff
//   spring.rabbitmq.listener.simple.default-requeue-rejected: false  # send to DLQ, not back
//
// KR: SQS와 비교 - 개념(프로듀서/컨슈머/재시도/DLQ/멱등) 동일, 다만
//     라우팅을 네가 직접 선언한다.
// EN: Compared to SQS - same concepts; the difference is you declare the
//     routing/topology yourself.
