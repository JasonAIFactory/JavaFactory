// =============================================================================
// 정답 — Queue & Worker 종합
// 실행: java -ea Q1_QueueWorker_Solution.java
// blank/Q1_QueueWorker.java 와 한 줄씩 비교하며 "왜 이렇게 했는지" 확인할 것.
// =============================================================================

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.*;

public class Q1_QueueWorker_Solution {

    record Task(int id, int attempts) {
        Task retryOnce() { return new Task(id, attempts + 1); }   // 새 불변 객체로 attempts+1
    }

    static final int MAX_ATTEMPTS = 4;
    static final long BASE_BACKOFF_MS = 50;

    static final AtomicInteger success = new AtomicInteger();
    static final AtomicInteger dlqCount = new AtomicInteger();
    // 교훈: 드레인 완료를 "큐가 비었나"로만 판단하면, 백오프 대기 중인
    //       지연 재시도가 아직 스케줄러에 떠 있을 때 워커가 먼저 종료해 유실된다.
    //       => 종료 조건은 "큐 비었다"가 아니라 "미완료 작업 수(pending)==0".
    static final AtomicInteger pending = new AtomicInteger();
    // 교훈: 재시도 재투입을 offer()로 하면 큐가 꽉 찼을 때 조용히 사라진다
    //       (pending이 안 줄어 영원히 드레인 안 됨). 블로킹 put()으로 넣고,
    //       단일 스레드 스케줄러가 put에서 막혀 다른 재시도를 못 돌리지 않게 풀로 둔다.
    static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public static void main(String[] args) throws Exception {

        BlockingQueue<Task> queue = new ArrayBlockingQueue<>(8);          // 바운디드 = 백프레셔
        BlockingQueue<Task> dlq = new LinkedBlockingQueue<>();
        Set<Integer> processedIds = ConcurrentHashMap.newKeySet();        // 멱등성 가드

        AtomicBoolean accepting = new AtomicBoolean(true);
        int workerCount = 3;
        ExecutorService pool = Executors.newFixedThreadPool(workerCount);
        CountDownLatch done = new CountDownLatch(workerCount);

        for (int w = 0; w < workerCount; w++) {
            final int id = w;
            pool.submit(() -> {
                try {
                    while (true) {
                        Task task = queue.poll(150, TimeUnit.MILLISECONDS);
                        if (task == null) {
                            if (!accepting.get() && pending.get() == 0) break; // 미완료 0 → 진짜 드레인 완료
                            continue;                                          // 지연 재시도가 아직 떠 있을 수 있음
                        }
                        if (processedIds.contains(task.id())) continue;        // 중복 → skip(ack), pending 미변동
                        try {
                            process(id, task);
                            processedIds.add(task.id());                       // 성공 "후" 기록 = at-least-once
                            success.incrementAndGet();
                            pending.decrementAndGet();                         // 터미널 상태 도달
                        } catch (Exception e) {
                            if (task.attempts() + 1 >= MAX_ATTEMPTS) {
                                dlq.offer(task);                               // 한도 초과 → 격리
                                dlqCount.incrementAndGet();
                                pending.decrementAndGet();                     // 터미널 상태 도달
                            } else {
                                long delay = backoffWithJitter(task.attempts());
                                Task retried = task.retryOnce();
                                scheduler.schedule(() -> {                       // 재시도는 아직 미완료 → pending 유지
                                    try { queue.put(retried); }                 // put = 유실 없음(꽉 차면 대기)
                                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                                }, delay, TimeUnit.MILLISECONDS);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        for (int i = 1; i <= 15; i++) {
            pending.incrementAndGet();                                          // 고유 작업 1건 = 미완료 +1
            queue.put(new Task(i, 0));                                          // put = 가득 차면 블로킹(백프레셔)
        }
        queue.put(new Task(1, 0));                                              // 중복 → 멱등성 가드가 막음(pending 미변동)

        accepting.set(false);                                                   // 1) 새 작업 중단
        done.await(15, TimeUnit.SECONDS);                                       // 2) 드레인 대기
        pool.shutdown();                                                        // 3) 정상 종료(shutdownNow 아님)
        scheduler.shutdownNow();

        int uniqueTasks = 15;
        int expectedDlq = 15 / 4;                 // 4,8,12
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
        long base = (long) (BASE_BACKOFF_MS * Math.pow(2, attempt));
        long jitter = ThreadLocalRandom.current().nextLong(-base / 2, base / 2 + 1);
        return Math.max(0, base + jitter);
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
