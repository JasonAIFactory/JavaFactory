// =============================================================================
// R1 — 가장 단순한 큐 + 워커 (일부러 결함을 남겨둔 버전)
//
// 목적: "큐+워커의 뼈대"를 보고, 동시에 "왜 이걸론 부족한지"를 눈으로 본다.
// 실행: java R1_NaiveQueueWorker.java
//
// 분석 포인트(코드 읽으며 스스로 답해보기):
//   - 작업 처리가 실패하면 그 작업은 어떻게 되는가?  (힌트: 사라진다)
//   - 프로듀서가 워커보다 빠르면 큐 크기는?            (힌트: 무한정 증가)
//   - 프로그램을 멈추면 처리 중이던 작업은?            (힌트: 유실)
// 이 3개 결함이 R2, R3에서 어떻게 고쳐지는지 비교하라.
// =============================================================================

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class R1_NaiveQueueWorker {

    // WHY: 작업(메시지) 1건. 실무에선 JSON 메시지지만 본질은 "할 일 + 식별자".
    record Task(int id, String payload) {}

    public static void main(String[] args) throws Exception {

        // WHY: BlockingQueue = 스레드 안전 + take()가 비었을 때 자동 대기.
        //      LinkedBlockingQueue 기본 생성자는 용량이 Integer.MAX_VALUE
        //      => 사실상 "무한 큐". 결함 #2(백프레셔 없음)의 원인. R3에서 바운디드로 고침.
        BlockingQueue<Task> queue = new LinkedBlockingQueue<>();

        AtomicInteger processed = new AtomicInteger();   // WHY: 여러 워커가 동시 증가 → 원자적 카운터.
        int workerCount = 3;                              // WHY: 요리사 3명. 스케일아웃의 시작점.
        ExecutorService pool = Executors.newFixedThreadPool(workerCount);

        // ---- 워커 풀: competing consumers (모두 같은 큐를 경쟁적으로 소비) ----
        for (int w = 0; w < workerCount; w++) {
            final int workerId = w;
            pool.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Task task = queue.take();            // WHY: 큐가 비면 여기서 블록(busy-wait 방지).
                        process(workerId, task);              // WHY: 실제 처리. 여기서 예외가 나면?
                        processed.incrementAndGet();          // WHY: 성공한 것만 카운트.
                        // 결함 #1: 만약 process()가 예외를 던지면 위 줄에 도달 못 하고
                        //          catch로 빠진다 → 그 Task는 큐에서 이미 빠졌고 재시도 없음 → "유실".
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();   // WHY: 인터럽트 플래그 복구 후 루프 종료.
                        break;
                    } catch (Exception e) {
                        // 결함 #1 그대로 노출: 실패한 task를 버리고 다음으로. 재시도/DLQ 없음.
                        System.out.println("  [worker " + workerId + "] FAILED, task lost: " + e.getMessage());
                    }
                }
            });
        }

        // ---- 프로듀서: 20개 작업을 큐에 투입 ----
        for (int i = 1; i <= 20; i++) {
            queue.put(new Task(i, "job-" + i));               // WHY: put = 큐에 넣기(여기선 무한 큐라 항상 즉시 성공).
        }

        Thread.sleep(3000);                                   // WHY: 데모용으로 처리 끝나길 대충 기다림(R3에서 제대로 된 종료로 대체).
        pool.shutdownNow();                                   // WHY: 워커 강제 중단 → 처리 중이던 작업 유실(결함 #3).
        System.out.println("\n총 투입 20개, 성공 처리: " + processed.get()
                + " (나머지는 실패로 유실되었거나 종료 시 잘림)");
    }

    // WHY: id가 7의 배수면 일부러 실패시켜 "실패 시 유실" 결함을 눈으로 보게 함.
    static void process(int workerId, Task task) {
        if (task.id() % 7 == 0) {
            throw new RuntimeException("simulated failure for task " + task.id());
        }
        sleep(100);                                           // WHY: 실제 작업처럼 시간이 걸리는 척.
        System.out.println("  [worker " + workerId + "] done task " + task.id());
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
