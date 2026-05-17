// =============================================================================
// R3 — 바운디드 큐 + 백프레셔 + Graceful Shutdown + 메트릭
//
// 목적: R1의 결함 #2(무한 큐)와 #3(종료 시 유실)을 고치고,
//       실무에서 반드시 보는 "큐 깊이" 메트릭을 노출한다.
//   - 바운디드 큐: 가득 차면 프로듀서를 블로킹(가장 단순한 백프레셔)
//     또는 거절(reject) — 둘 다 보여줌
//   - Graceful shutdown: 새 작업 수신 중단 → 큐 드레인 → 워커 정상 종료
//   - 메트릭: enqueued / processed / rejected / queueDepth (오토스케일 트리거의 근거)
//
// 실행: java R3_BackpressureGracefulShutdown.java
//
// 분석 포인트:
//   - 큐 용량 5에 프로듀서가 빠르면 어떤 줄에서 막히나? (offer vs put)
//   - shutdown()과 shutdownNow()의 차이가 왜 "유실 0"의 핵심인가?
//   - queueDepth가 계속 커지면 오토스케일러는 무엇을 하나? (워커 증설)
// =============================================================================

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

public class R3_BackpressureGracefulShutdown {

    record Task(int id) {}

    // ---- 메트릭: 실무에선 Prometheus gauge/counter. 여기선 원자 카운터. ----
    static final AtomicInteger enqueued  = new AtomicInteger();
    static final AtomicInteger processed = new AtomicInteger();
    static final AtomicInteger rejected  = new AtomicInteger();

    public static void main(String[] args) throws Exception {

        // WHY: 용량 5로 제한. 이게 백프레셔의 핵심 — 큐가 무한히 못 자란다.
        BlockingQueue<Task> queue = new ArrayBlockingQueue<>(5);

        AtomicBoolean acceptingNewWork = new AtomicBoolean(true);  // WHY: graceful shutdown 1단계 스위치.
        int workerCount = 2;
        ExecutorService pool = Executors.newFixedThreadPool(workerCount);
        CountDownLatch workersDone = new CountDownLatch(workerCount);

        for (int w = 0; w < workerCount; w++) {
            final int workerId = w;
            pool.submit(() -> {
                try {
                    while (true) {
                        // WHY: poll(타임아웃) — take()와 달리 영원히 안 막힌다.
                        //      shutdown 신호 후 큐가 비면 빠져나와 워커가 깔끔히 종료.
                        Task task = queue.poll(200, TimeUnit.MILLISECONDS);
                        if (task == null) {
                            // 큐가 비었고 더 이상 새 작업도 안 받는다면 → 드레인 완료, 종료.
                            if (!acceptingNewWork.get() && queue.isEmpty()) break;
                            continue;                              // WHY: 아직 일이 올 수 있으니 계속 폴링.
                        }
                        process(workerId, task);
                        processed.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    workersDone.countDown();                       // WHY: 이 워커가 끝났음을 알림.
                }
            });
        }

        // ---- 메트릭 리포터: 0.5초마다 큐 깊이 출력 (오토스케일러가 보는 신호) ----
        ScheduledExecutorService reporter = Executors.newSingleThreadScheduledExecutor();
        reporter.scheduleAtFixedRate(() ->
                System.out.println("  [metric] queueDepth=" + queue.size()
                        + " enq=" + enqueued.get() + " proc=" + processed.get()
                        + " rej=" + rejected.get()),
                0, 500, TimeUnit.MILLISECONDS);

        // ---- 프로듀서: 워커보다 빠르게 30개 투입 → 백프레셔 두 방식 시연 ----
        for (int i = 1; i <= 30; i++) {
            Task t = new Task(i);
            if (i <= 20) {
                // 방식 A) 블로킹 백프레셔: 큐가 차면 프로듀서가 여기서 대기.
                //         => 프로듀서가 자연히 느려진다(요청을 천천히 받게 됨).
                queue.put(t);
                enqueued.incrementAndGet();
            } else {
                // 방식 B) 거절 백프레셔: 50ms 안에 못 넣으면 reject(실무의 HTTP 429).
                //         => 프로듀서를 안 막는 대신 호출자에게 "지금 바쁨"을 알림.
                if (queue.offer(t, 50, TimeUnit.MILLISECONDS)) {
                    enqueued.incrementAndGet();
                } else {
                    rejected.incrementAndGet();
                    System.out.println("  [producer] REJECTED task " + i + " (429-style)");
                }
            }
        }

        // ---- Graceful shutdown: 유실 0의 핵심 ----
        acceptingNewWork.set(false);          // 1) 새 작업 수신 중단.
        // 2) 큐에 남은 것을 워커가 다 빼갈 때까지 기다린다(드레인).
        boolean drained = workersDone.await(10, TimeUnit.SECONDS);
        pool.shutdown();                       // 3) 그 다음에야 풀 종료. shutdownNow()가 아님에 주의!
        reporter.shutdownNow();

        System.out.println("\n드레인 완료=" + drained
                + " | enqueued=" + enqueued.get()
                + " processed=" + processed.get()
                + " rejected=" + rejected.get());
        System.out.println("검증: enqueued == processed 여야 유실 0. "
                + "rejected는 백프레셔로 '의도적으로' 안 받은 것(유실 아님).");
    }

    static void process(int workerId, Task task) {
        sleep(120);                            // WHY: 워커를 일부러 느리게 → 큐가 차서 백프레셔가 보이게.
        System.out.println("  [w" + workerId + "] done " + task.id());
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
