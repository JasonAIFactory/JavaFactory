// =============================================================================
// 백지 구현 — Queue & Worker 종합
//
// 규칙: solution/ 을 열지 말고 30분 이상 버틴다. 막힌 지점이 진짜 학습 포인트.
// 목표: R1~R3에서 본 개념을 "한 파일"에 직접 통합한다.
//
// 요구사항(TODO를 채워라):
//   1) 바운디드 큐(용량 8). 가득 차면 프로듀서를 블로킹(백프레셔).
//   2) 워커 3개(competing consumers).
//   3) 처리 실패 시: 최대 4회 재시도, 지수 백오프 + 지터.
//   4) 4회 초과 실패 시 DLQ로 격리.
//   5) 멱등성: 같은 id를 두 번 처리하지 않는다.
//   6) Graceful shutdown: 새 작업 중단 → 큐 드레인 → 워커 정상 종료(유실 0).
//   7) 메트릭: success / dlq 카운트.
//
// 검증: main 끝의 assert 블록이 모두 통과하면 성공.
// 실행: java -ea Q1_QueueWorker.java   (-ea = assert 활성화, 꼭 붙일 것)
//
// ⚠️ 일부러 안 알려주는 함정 2개 (다 짜고 -ea로 돌리면 드러난다. 스스로 디버깅하라):
//   함정 A) 드레인 완료를 "큐가 비었나"로만 판단하면? 백오프 대기 중인
//           지연 재시도가 아직 큐 밖(스케줄러)에 떠 있을 때 워커가 먼저 죽어 유실.
//           → 종료 조건을 "큐 비었다"가 아닌 "미완료 작업 수==0"으로.
//   함정 B) 재시도 재투입을 offer()로 하면 바운디드 큐가 꽉 찼을 때 조용히
//           사라진다 → 영원히 안 끝남. → 재투입은 블로킹 put()으로.
// =============================================================================

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.*;

public class Q1_QueueWorker {

    // TODO 0: attempts를 들고 다니는 Task 레코드. retryOnce()로 attempts+1 새 Task 반환.
    record Task(int id, int attempts) {
        Task retryOnce() { /* TODO */ return null; }
    }

    static final int MAX_ATTEMPTS = 4;
    static final long BASE_BACKOFF_MS = 50;

    static final AtomicInteger success = new AtomicInteger();
    static final AtomicInteger dlqCount = new AtomicInteger();
    static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) throws Exception {

        // TODO 1: 용량 8의 바운디드 BlockingQueue<Task> queue 선언.
        BlockingQueue<Task> queue = null;
        BlockingQueue<Task> dlq = new LinkedBlockingQueue<>();

        // TODO 5: 멱등성용 처리 완료 id 집합 (스레드 안전).
        Set<Integer> processedIds = null;

        AtomicBoolean accepting = new AtomicBoolean(true);
        int workerCount = 3;
        ExecutorService pool = Executors.newFixedThreadPool(workerCount);
        CountDownLatch done = new CountDownLatch(workerCount);

        for (int w = 0; w < workerCount; w++) {
            final int id = w;
            pool.submit(() -> {
                try {
                    while (true) {
                        // TODO 6: poll(타임아웃)로 꺼낸다. null이고 !accepting이고 큐 비면 break.
                        //         아직 일이 올 수 있으면 continue.
                        Task task = null; // <- 교체

                        // TODO 5: 이미 처리한 id면 skip(continue).

                        try {
                            process(id, task);
                            // TODO 5: 성공 기록 + success 증가.
                        } catch (Exception e) {
                            // TODO 3/4: attempts+1 >= MAX 이면 DLQ + dlqCount++,
                            //           아니면 backoff 후 retryOnce()를 큐로 재투입.
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        // 프로듀서: 15개. id가 4의 배수면 영원히 실패(→DLQ). 중복 1건 투입.
        for (int i = 1; i <= 15; i++) {
            // TODO 1: 백프레셔 — 큐가 차면 여기서 막히는 메서드로 넣을 것.
        }
        // 중복 메시지(멱등성 테스트)
        // TODO: queue 에 id=1 짜리 Task 한 번 더 투입.

        // TODO 6: graceful shutdown — accepting=false → done.await(...) → pool.shutdown().
        scheduler.shutdownNow();

        // ---- 검증 (건드리지 말 것) ----
        int uniqueTasks = 15;                 // id 1..15
        int expectedDlq = 15 / 4;             // 4,8,12 → 3개
        int expectedSuccess = uniqueTasks - expectedDlq;
        System.out.println("success=" + success.get() + " dlq=" + dlqCount.get());
        assert success.get() == expectedSuccess : "성공 수 불일치: " + success.get();
        assert dlqCount.get() == expectedDlq : "DLQ 수 불일치: " + dlqCount.get();
        assert success.get() + dlqCount.get() == uniqueTasks : "유실 발생!";
        System.out.println("✅ ALL ASSERTIONS PASSED — 유실 0, 멱등성/재시도/DLQ 정상");
    }

    static void process(int workerId, Task task) {
        if (task.id() % 4 == 0) throw new RuntimeException("poison " + task.id());
        sleep(60);
    }

    static long backoffWithJitter(int attempt) {
        // TODO 3: BASE * 2^attempt + ±50% 지터, 음수 방지.
        return 0;
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
