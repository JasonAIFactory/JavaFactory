// =============================================================================
// R2 — 재시도 + 지수 백오프 + 지터 + DLQ + 멱등성
//
// 목적: R1의 결함 #1(실패 시 유실)을 제대로 고친다.
//   - 실패하면 버리지 않고 재시도(최대 N회)
//   - 재시도 간격은 지수 백오프 + 지터 (retry storm 방지)
//   - N회 초과하면 DLQ로 격리 (poison pill이 큐를 막지 않게)
//   - 같은 메시지를 두 번 처리해도 안전하게 (멱등성)
//
// 실행: java R2_RetryDlqQueue.java
//
// 분석 포인트:
//   - attempts(재시도 횟수)는 어디에 저장되나? 왜 메시지에 같이 넣나?
//   - backoff에 jitter가 없으면 무슨 일? (THEORY 3-(3))
//   - processedIds 체크는 at-least-once의 어떤 부작용을 막나? (중복 처리)
// =============================================================================

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;

public class R2_RetryDlqQueue {

    // WHY: attempts를 메시지에 함께 둔다. 큐는 상태를 기억하지 않으므로
    //      "이 메시지가 몇 번째 시도인지"는 메시지가 들고 다녀야 한다.
    record Task(int id, String payload, int attempts) {
        Task retryOnce() { return new Task(id, payload, attempts + 1); }
    }

    static final int MAX_ATTEMPTS = 4;          // WHY: 4회까지 시도, 그 후 DLQ. 무한 재시도는 금지.
    static final long BASE_BACKOFF_MS = 100;     // WHY: 1차 100ms, 2차 200ms, 3차 400ms ... 지수.

    public static void main(String[] args) throws Exception {
        BlockingQueue<Task> queue = new LinkedBlockingQueue<>();
        BlockingQueue<Task> dlq   = new LinkedBlockingQueue<>();   // WHY: 죽은 메시지 격리소.

        // WHY: 멱등성 — 이미 성공 처리한 메시지 id 기록.
        //      분산 환경이면 이 Set이 Redis SETNX 또는 DB unique 제약이 된다.
        Set<Integer> processedIds = ConcurrentHashMap.newKeySet();

        AtomicInteger success = new AtomicInteger();
        AtomicInteger toDlq   = new AtomicInteger();

        int workerCount = 3;
        ExecutorService pool = Executors.newFixedThreadPool(workerCount);

        for (int w = 0; w < workerCount; w++) {
            final int workerId = w;
            pool.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    Task task;
                    try {
                        task = queue.take();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    // ---- 멱등성 가드: at-least-once라 같은 메시지가 또 올 수 있다 ----
                    if (processedIds.contains(task.id())) {
                        // WHY: 이미 처리됨 → 다시 처리하지 않고 조용히 ack(skip).
                        System.out.println("  [w" + workerId + "] DUP skip task " + task.id());
                        continue;
                    }

                    try {
                        process(workerId, task);
                        processedIds.add(task.id());            // WHY: 성공 "후" 기록 = at-least-once.
                        success.incrementAndGet();
                    } catch (Exception e) {
                        // ---- 실패 처리: 버리지 않는다 ----
                        if (task.attempts() + 1 >= MAX_ATTEMPTS) {
                            dlq.offer(task);                     // WHY: 한도 초과 → DLQ로 격리.
                            toDlq.incrementAndGet();
                            System.out.println("  [w" + workerId + "] -> DLQ task " + task.id()
                                    + " after " + (task.attempts() + 1) + " attempts");
                        } else {
                            long delay = backoffWithJitter(task.attempts());
                            // WHY: 즉시 큐에 넣지 않고 delay 뒤에 재투입.
                            //      별도 스케줄러가 delay 후 큐로 돌려보낸다(지연 재시도).
                            scheduleRequeue(queue, task.retryOnce(), delay);
                            System.out.println("  [w" + workerId + "] retry task " + task.id()
                                    + " (attempt " + (task.attempts() + 1) + ") in " + delay + "ms");
                        }
                    }
                }
            });
        }

        // 프로듀서: 12개 작업. id가 3의 배수면 항상 실패하도록 만들어 DLQ를 관찰.
        for (int i = 1; i <= 12; i++) queue.put(new Task(i, "job-" + i, 0));
        // 일부러 중복 메시지 1건 투입 → 멱등성 가드가 막는지 관찰.
        queue.put(new Task(1, "job-1-DUPLICATE", 0));

        Thread.sleep(5000);
        pool.shutdownNow();
        SCHEDULER.shutdownNow();
        System.out.println("\n성공=" + success.get() + " DLQ=" + toDlq.get()
                + " (성공+DLQ 합이 고유 작업 수와 맞아야 한다 — 유실 0)");
        System.out.println("DLQ 내용: " + new ArrayList<>(dlq));
    }

    // WHY: 지수 백오프 = BASE * 2^attempt. 거기에 ±50% 랜덤 지터.
    //      지터가 없으면 모든 워커가 같은 시각 재시도 → thundering herd.
    static long backoffWithJitter(int attempt) {
        long base = (long) (BASE_BACKOFF_MS * Math.pow(2, attempt));
        long jitter = ThreadLocalRandom.current().nextLong(-base / 2, base / 2 + 1);
        return Math.max(0, base + jitter);
    }

    // WHY: 지연 재시도용 단일 스케줄러. delay 후 큐로 task를 되돌린다.
    static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    static void scheduleRequeue(BlockingQueue<Task> queue, Task task, long delayMs) {
        SCHEDULER.schedule(() -> queue.offer(task), delayMs, TimeUnit.MILLISECONDS);
    }

    static void process(int workerId, Task task) {
        if (task.id() % 3 == 0) {                              // WHY: 3의 배수는 영원히 실패 → DLQ로 갈 운명.
            throw new RuntimeException("poison: task " + task.id());
        }
        sleep(80);
        System.out.println("  [w" + workerId + "] OK task " + task.id());
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
