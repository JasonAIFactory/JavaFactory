// =============================================================================
// P1 - AWS SQS with Spring Boot (Spring Cloud AWS)
//
// KR: 자바 창업의 가장 흔한 "그냥 큐" 선택. 서버를 직접 운영하지 않는다.
// EN: The most common "just a queue" choice for a Java startup. You run
//     no broker servers yourself.
//
// PRODUCTION REFERENCE - does NOT run in this repo (needs Maven + AWS).
//
// build.gradle:
//   implementation platform("io.awspring.cloud:spring-cloud-aws-dependencies:3.1.1")
//   implementation "io.awspring.cloud:spring-cloud-aws-starter-sqs"
//
// DLQ + retry: configured ON the SQS queue (a redrive policy: after N
// receives, AWS moves the message to a dead-letter queue). You do NOT
// code the retry loop - the broker does it. Visibility timeout is also
// a queue setting.
// =============================================================================

package production;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// ---- 1) PRODUCER: send a message (the API service does this) ----
@Service
class SignupService {
    private final SqsTemplate sqs;
    SignupService(SqsTemplate sqs) { this.sqs = sqs; }

    public String signup(long userId) {
        // save user (fast) ... then just enqueue the slow work and return.
        sqs.send(to -> to.queue("email-queue")
                .payload(new EmailJob(userId, "welcome")));
        return "202 accepted"; // user does NOT wait for the email
    }
}

record EmailJob(long userId, String template) {}

// ---- 2) CONSUMER: a worker. Spring runs a pool of these listeners ----
@Component
class EmailWorker {

    // idempotency lives HERE (at-least-once delivery can deliver twice).
    // In real life this is a DB unique key or Redis SETNX, not a Set.
    private final Set<Long> processed = ConcurrentHashMap.newKeySet();

    @SqsListener("email-queue")            // competing consumers: scale by running more app instances
    public void handle(EmailJob job) {
        if (!processed.add(job.userId())) return;   // duplicate -> skip (idempotent)
        sendEmail(job);                              // if this throws, SQS will redeliver,
                                                     // and after N tries the redrive policy
                                                     // moves it to the DLQ automatically.
    }

    private void sendEmail(EmailJob job) {
        // call the email gateway ...
    }
}

// application.yml (key settings, not Java):
//   spring.cloud.aws.region.static: us-east-1
//   # On the queue itself (Terraform/console):
//   #   VisibilityTimeout: 60
//   #   RedrivePolicy: maxReceiveCount=5 -> email-queue-dlq
//
// KR: 핵심 - 재시도/DLQ/가시성은 "코드"가 아니라 "큐 설정"이다.
//     너는 멱등성만 책임진다.
// EN: Key point - retry/DLQ/visibility are QUEUE SETTINGS, not code.
//     You are only responsible for idempotency.
