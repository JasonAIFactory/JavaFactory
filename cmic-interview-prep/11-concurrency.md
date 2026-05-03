# 동시성 / 멀티스레드 면접 답변

본인 경력에 "scheduler concurrency control" 명시되어 있으니 거의 확실히 물어봄.
영어는 단순 SVO, 짧은 문장.

---

## 1. Concurrency vs Parallelism

🧠 **Concurrency** = 여러 작업이 번갈아 진행. **Parallelism** = 여러 작업이 동시에 진행 (멀티 코어).

```
Concurrency:    [A][B][A][C][A][B][C]   ← 한 코어에서 시간 분할
Parallelism:    [A][A][A][A]            ← 코어 1
                [B][B][B][B]            ← 코어 2 (동시)
```

🗣️ "Concurrency means multiple tasks make progress over time. They share one CPU and switch between each other. Parallelism means multiple tasks run at the exact same time on different CPU cores. Concurrency is about structure. Parallelism is about execution."

---

## 2. Thread 기본

🧠 Thread는 작업 실행 단위. Runnable로 정의, Thread로 실행.

```java
// 방법 1: Runnable (권장)
Runnable task = () -> System.out.println("running on " + Thread.currentThread().getName());
Thread t = new Thread(task);
t.start();
t.join();   // 종료 대기

// 방법 2: Thread 상속 (덜 추천)
class MyThread extends Thread {
    public void run() { System.out.println("hi"); }
}
new MyThread().start();
```

🗣️ "Thread is a unit of execution. I prefer Runnable over extending Thread because Runnable is more flexible. Java now has ExecutorService for thread pools — that is what I use in production."

---

## 3. synchronized + volatile

🧠 `synchronized` = 한 번에 한 스레드만 진입. `volatile` = 다른 스레드가 변경한 값을 즉시 보이게.

```java
class Counter {
    private int count = 0;

    // synchronized: 한 번에 한 스레드만 메서드 진입
    public synchronized void increment() {
        count++;
    }

    // 또는 블록
    public void increment2() {
        synchronized (this) {
            count++;
        }
    }
}

// volatile: 다른 스레드의 변경을 바로 봄
class FlagHolder {
    private volatile boolean stop = false;
    public void run() {
        while (!stop) {
            // do work
        }
    }
}
```

🗣️ "Synchronized makes only one thread enter the method or block at a time. It guarantees mutual exclusion. Volatile makes a variable visible across threads — when one thread writes, other threads see the new value immediately. But volatile does not give atomicity for compound operations like count++."

---

## 4. Deadlock (단골)

🧠 두 스레드가 서로의 락을 기다리며 영원히 멈춘 상태.

```java
class DeadlockExample {
    private final Object lockA = new Object();
    private final Object lockB = new Object();

    public void method1() {
        synchronized (lockA) {
            sleep(100);
            synchronized (lockB) {  // T1이 여기서 대기
                // ...
            }
        }
    }

    public void method2() {
        synchronized (lockB) {
            sleep(100);
            synchronized (lockA) {  // T2가 여기서 대기 → 데드락
                // ...
            }
        }
    }
}
```

### 데드락 4가지 조건 (모두 만족해야 발생)

1. **Mutual exclusion** — 락은 한 번에 하나만
2. **Hold and wait** — 락 잡은 채 다른 락 기다림
3. **No preemption** — 락 강제로 뺏을 수 없음
4. **Circular wait** — A→B→A 식 순환

### 예방

- **락 순서 통일**: 항상 lockA → lockB 순서로 잡기
- **타임아웃**: `tryLock(timeout)` 사용
- **락 줄이기**: 가능하면 lock-free 자료구조 사용

🗣️ "Deadlock happens when two threads wait for each other's lock forever. To prevent it I always acquire locks in the same order. I also use tryLock with timeout so a thread can give up and retry. In my scheduler work, I solved a duplicate-job issue using SELECT FOR UPDATE with row-level locking, which avoided race conditions."

---

## 5. Race Condition

🧠 여러 스레드가 같은 데이터에 동시 접근해서 결과가 예측 불가.

```java
// 위험한 코드
class Counter {
    int count = 0;
    void increment() { count++; }   // 3단계: 읽기 → 더하기 → 쓰기
}
// 두 스레드가 동시에 호출하면 카운트 누락 가능

// 안전한 코드
class SafeCounter {
    private final AtomicInteger count = new AtomicInteger();
    void increment() { count.incrementAndGet(); }
}
```

🗣️ "Race condition is when threads access shared data and the result depends on timing. The fix is synchronization, atomic classes, or immutable data. I prefer AtomicInteger or AtomicLong for simple counters because they are lock-free and fast."

---

## 6. ExecutorService (실무 패턴)

🧠 스레드 풀. 직접 Thread 만드는 대신 사용.

```java
import java.util.concurrent.*;

ExecutorService pool = Executors.newFixedThreadPool(10);

Future<Integer> future = pool.submit(() -> {
    Thread.sleep(100);
    return 42;
});

Integer result = future.get();   // 결과 기다림

pool.shutdown();
pool.awaitTermination(5, TimeUnit.SECONDS);
```

🗣️ "In production I use ExecutorService instead of raw Threads. A thread pool reuses threads and limits concurrency. I submit Callable or Runnable and get a Future for the result. Always shutdown the pool to release resources."

---

## 7. System Crash / Application Crash

🧠 JVM이 비정상 종료. 흔한 원인 4가지.

| 원인 | 증상 | 조사 방법 |
| --- | --- | --- |
| **OutOfMemoryError (OOM)** | heap 부족 | heap dump 분석 |
| **StackOverflowError** | 무한 재귀 | thread dump 확인 |
| **Native crash** | JNI, GC 버그 | hs_err_pid.log 확인 |
| **Process kill** | OS가 종료 (OOM killer) | dmesg, /var/log/messages |

🗣️ "When a Java app crashes, I check four things. First, the application log for OutOfMemoryError or StackOverflowError. Second, the heap dump if available — that shows which objects fill the memory. Third, the thread dump for deadlocks or stuck threads. Fourth, the OS logs in case the kernel killed the process. Then I correlate the time with deployment, traffic spikes, and DB issues."

### 흔한 OOM 원인

- 메모리 누수 (Map에 객체 계속 쌓임, listener 안 제거)
- 큰 결과셋 한 번에 로드 (PreparedStatement에서 fetchSize 안 줌)
- 캐시가 무한 증가
- 파일/스트림 안 닫음

---

## 8. 본인 경험 영어 표현 (Scheduler Concurrency)

> "In my MES project I worked on a scheduler that occasionally double-processed jobs. The root cause was a race condition — two scheduler instances picked up the same job. I added a status column with a row-level lock using SELECT FOR UPDATE, so the second instance saw the lock and skipped that row. Duplicate rate dropped to zero."

핵심 단어: **race condition, row-level lock, SELECT FOR UPDATE, duplicate**.

---

## 🎯 한 줄 카드

| 주제 | 한 줄 |
| --- | --- |
| Concurrency | tasks make progress over time |
| Parallelism | tasks run at the same time on different cores |
| synchronized | one thread at a time |
| volatile | visibility across threads, not atomicity |
| Deadlock | two threads wait for each other's lock forever |
| Race condition | result depends on timing |
| AtomicInteger | lock-free counter |
| ExecutorService | thread pool, preferred over raw Thread |
| OOM | heap full, check heap dump |
| Thread dump | for deadlocks and stuck threads |
