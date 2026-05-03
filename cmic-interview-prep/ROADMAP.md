# CMiC 면접 8시간 로드맵 (Day 1 + Day 2)

면접까지 2일, 하루 4시간씩, 총 8시간.
**알고리즘 문제는 안 나오고 실무형 코드 + 영어 설명**이 핵심.

---

## 📌 시작 전 5분 — 환경 점검

- [ ] VS Code + Extension Pack for Java 설치됨
- [ ] `java -version` 실행됨 (Java 17+ 권장)
- [ ] `cmic-interview-prep/` 폴더 익숙함
- [ ] 헤드폰 + 조용한 공간 확보 (영어 소리내어 말해야 함)

---

# 🟢 DAY 1 (4시간) — 무조건 나오는 것부터

## ⏰ Hour 1 (60분) — REST API 🔴

**파일:** [10-rest-api.md](10-rest-api.md)

### 0~10분: 읽기 + 한 줄 카드

- 파일 통독 1회
- 마지막 한 줄 카드 표 외우기 (REST / GET / POST / PUT / PATCH / DELETE / status code 5개)

### 10~45분: Meeting API 빈 종이에 직접 설계

- 빈 메모장 또는 종이에 직접 작성 (파일 보지 말 것)
- 다음 7개 엔드포인트 다 적기:
  - `GET /meetings`
  - `GET /meetings/{id}`
  - `POST /meetings` + JSON body
  - `PUT /meetings/{id}` + JSON body
  - `PATCH /meetings/{id}` + JSON body
  - `DELETE /meetings/{id}`
  - `GET /meetings/{id}/participants`
  - `POST /meetings/{id}/participants`
- 각 엔드포인트에 status code 적기 (200/201/204/400/404/500)
- POST + 응답 JSON 예시 1개 적기

### 45~60분: 영어로 설명

소리내어 말하기. 막히면 파일 봐도 OK. 다시 안 보고 말하기.

> "First I identify resources. Meeting is the main one. Participant is a sub-resource.
> So I design nested URLs.
> For HTTP methods I use GET to read, POST to create, PUT to replace, PATCH to update part, DELETE to remove.
> POST returns 201 Created. DELETE returns 204 No Content.
> JSON in the body. Stateless — each request carries auth in the header."

✅ **체크:** Meeting API 빈 종이에 막힘없이 설계 가능 + PUT vs PATCH vs POST 5초 답변

---

## ⏰ Hour 2 (60분) — Concurrency 🔴

**파일:** [11-concurrency.md](11-concurrency.md)

### 0~15분: 읽기

- 통독 1회
- 본인 "scheduler concurrency control" 경험을 다음 문장에 채워넣기:
  > "In my MES project I worked on a scheduler that occasionally double-processed jobs.
  >  The root cause was a race condition.
  >  I added a status column with row-level lock using SELECT FOR UPDATE.
  >  Duplicate rate dropped to zero."

### 15~35분: Deadlock 코드 직접 타이핑

VS Code에서 새 파일 `DeadlockDemo.java` 만들고 직접 작성:

```java
public class DeadlockDemo {
    private static final Object lockA = new Object();
    private static final Object lockB = new Object();

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            synchronized (lockA) {
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                synchronized (lockB) {
                    System.out.println("T1 got both");
                }
            }
        });

        Thread t2 = new Thread(() -> {
            synchronized (lockB) {
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                synchronized (lockA) {
                    System.out.println("T2 got both");
                }
            }
        });

        t1.start(); t2.start();
        t1.join(); t2.join();
    }
}
```

실행해서 멈추는 거 확인 → Ctrl+C 종료. **이 코드를 안 보고 다시 작성**.

### 35~45분: ExecutorService 코드 직접 타이핑

```java
import java.util.concurrent.*;

public class PoolDemo {
    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(3);

        Future<Integer> future = pool.submit(() -> {
            Thread.sleep(100);
            return 42;
        });

        System.out.println(future.get());

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
    }
}
```

### 45~60분: 영어로 답변 연습

3개 질문에 답하기:

1. **"What is concurrency vs parallelism?"**
   > "Concurrency means tasks make progress over time, sharing one CPU. Parallelism means tasks run at the same time on different cores. Concurrency is structure. Parallelism is execution."

2. **"What is a deadlock and how do you prevent it?"**
   > "Deadlock is when two threads wait for each other's lock forever. To prevent it I always acquire locks in the same order. I also use tryLock with timeout."

3. **"Tell me about a concurrency issue you solved."**
   > (위에 본인 scheduler 답변)

✅ **체크:** Deadlock 코드 안 보고 작성 가능 + 3개 질문 영어 답변

---

## ⏰ Hour 3 (60분) — OOP Live Coding ⭐ 핵심

**파일:** `practice/OOP01_*.java` ~ `OOP05_*.java`, 정답은 [practice/OOP_SOLUTIONS.md](practice/OOP_SOLUTIONS.md)

각 파일 12분씩. **타이핑 + 영어로 "Why this is good" 1번 말하기**.

### 0~12분: OOP01_Override

- Animal/Dog/Cat/Cow + sound() override
- 영어: "Each subclass overrides sound. The variable type is Animal but Java calls the real subclass method at runtime. To add a new animal I just add a new subclass — no change to the loop."

### 12~24분: OOP02_Overload

- Calculator + 3가지 add()
- 영어: "Same name, different parameters. Compiler picks at compile time."

### 24~36분: OOP03_Singleton

- AppConfig — Eager 방식
- 영어: "One instance only. Private constructor + static getInstance. Eager init is simplest and thread-safe."

### 36~48분: OOP04_Strategy

- PaymentStrategy + CreditCard / BankTransfer / Crypto
- 영어: "Behavior swap at runtime via interface. Add a new strategy without changing the caller. Open-closed principle."

### 48~60분: OOP05_Encapsulation

- BankAccount + private balance + deposit/withdraw + 예외
- 영어: "Balance is private. Methods validate input and throw if rule broken. Object stays in valid state."

✅ **체크:** 5개 다 코드 + 1줄 이점 영어로 가능

---

## ⏰ Hour 4 (60분) — 실무 패턴 4문제

**파일:** `practice/Problem02_*.java` ~ `Problem05_*.java`, 정답은 [practice/SOLUTIONS.md](practice/SOLUTIONS.md)

각 15분씩. **알고리즘 PrefixCount는 스킵**.

| 시간 | 문제 | 영어 한 줄 |
|---|---|---|
| 0~15분 | Problem02_GroupByKey | "HashMap with merge accumulates sums per key. O(N)." |
| 15~30분 | Problem03_ValidateOrders | "Validation in a private isValid method. Easier to test, easier to extend." |
| 30~45분 | Problem04_FindDuplicates | "Two sets — one for seen, one for already reported. One pass, O(N)." |
| 45~60분 | Problem05_SimpleCache | "LinkedHashMap with accessOrder true. Override removeEldestEntry. Standard Java LRU." |

✅ **Day 1 끝 체크리스트:**
- [ ] Meeting API 빈 종이 설계 가능
- [ ] Deadlock 코드 안 보고 작성 가능
- [ ] OOP 5개 코드 + 영어 가능
- [ ] 실무 패턴 4개 코드 가능

---

# 🟡 DAY 2 (4시간) — 단골 + SQL + 모의면접

## ⏰ Hour 1 (60분) — 단골 Java 질문 ⭐

**파일:** [09-java-classic-questions.md](09-java-classic-questions.md)

각 주제 8분: **읽기 → 파일 닫기 → 소리내어 답변**.

| 시간 | 주제 | 핵심 한 줄 |
|---|---|---|
| 0~8분 | Java 8 features | Lambda, Stream, Optional, Default method |
| 8~16분 | Stream API | Data pipeline, lazy, terminal triggers execution |
| 16~24분 | StringBuffer vs StringBuilder | Thread-safe vs faster |
| 24~32분 | Override vs Overload | Runtime vs compile-time |
| 32~40분 | ArrayList vs LinkedList | Read-heavy vs write-heavy |
| 40~48분 | HashMap vs ConcurrentHashMap | Not safe vs bucket-level lock |
| 48~56분 | Singleton (Eager / DCL / Enum) | Private constructor, static getInstance |
| 56~60분 | Tree (TreeMap) | Red-Black tree, sorted, O(log N) |

✅ **체크:** 8개 주제 자료 안 보고 영어 답변 가능

---

## ⏰ Hour 2 (60분) — SQL 실제 쿼리 작성

**파일:** [06-sql-basics.md](06-sql-basics.md), [07-plsql.md](07-plsql.md)

빈 텍스트 파일 열고 직접 타이핑 (자료 보지 말기).

### 0~20분: Highest Salary 4가지 방식 (필수)

```sql
-- 방식 1: FETCH (Oracle 12c+)
SELECT * FROM employee
ORDER BY salary DESC
FETCH FIRST 1 ROW ONLY;

-- 방식 2: ROWNUM
SELECT * FROM (
    SELECT * FROM employee ORDER BY salary DESC
)
WHERE ROWNUM = 1;

-- 방식 3: Window function (동률 처리)
SELECT * FROM (
    SELECT e.*, DENSE_RANK() OVER (ORDER BY salary DESC) AS rnk
    FROM employee e
)
WHERE rnk = 1;

-- 방식 4: Subquery
SELECT * FROM employee
WHERE salary = (SELECT MAX(salary) FROM employee);
```

### 20~30분: JOIN + EXISTS

LEFT JOIN으로 비용 없는 프로젝트 찾기 + EXISTS로 PENDING 비용 있는 프로젝트 찾기.

### 30~45분: Cursor FOR loop

```sql
DECLARE
    CURSOR c_projects IS SELECT * FROM projects WHERE status = 'OPEN';
BEGIN
    FOR rec IN c_projects LOOP
        DBMS_OUTPUT.PUT_LINE(rec.project_id);
    END LOOP;
END;
/
```

### 45~60분: Bulk update + EXCEPTION (자가진단 Q8)

```sql
BEGIN
    UPDATE project_cost
       SET status = 'APPROVED'
     WHERE status = 'PENDING';

    COMMIT;
    DBMS_OUTPUT.PUT_LINE(SQL%ROWCOUNT || ' rows approved');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END;
/
```

영어: "I use one bulk UPDATE, not a cursor loop. Commit on success. Rollback in exception. Re-raise so the caller knows."

✅ **체크:** 4가지 쿼리 자료 안 보고 작성 가능

---

## ⏰ Hour 3 (60분) — 약점 재훈련

Day 1 + Hour 1~2에서 안 외워진 것 다시.

### 0~30분: REST Meeting API 빈 종이에 다시

타이머 30분. 한 번 더 처음부터.

### 30~45분: OOP 1개 백지 재코딩

OOP04_Strategy 또는 OOP03_Singleton 중 본인이 약한 것 골라서 빈 파일에 다시 작성.

### 45~60분: 단골 질문 막힌 거 다시

본인이 Hour 1에서 막혔던 주제 2~3개 골라서 영어 답변 재연습.

✅ **체크:** 막힌 거 한 번씩 더 짚음

---

## ⏰ Hour 4 (60분) — 자가 모의면접

### 0~40분: [01-diagnostic.md](01-diagnostic.md) 10문제 영어로 답변

자료 닫고 큰 소리로 답변. 끝나고 [02-java-basics.md](02-java-basics.md), [07-plsql.md](07-plsql.md), [09-java-classic-questions.md](09-java-classic-questions.md) 참고해서 자가 채점.

### 40~50분: Reverse Questions 3개 외우기

> 1. "Could you tell me a bit about the team I'd be working with?"
> 2. "How is the responsibility split between new feature development and production support?"
> 3. "What does success look like in the first 90 days for someone in this role?"

### 50~60분: 자기소개 90초

본인이 따로 정리한 자기소개 영어로 3번 반복.

✅ **Day 2 끝 체크리스트:**
- [ ] 단골 8개 자료 안 보고 답 가능
- [ ] SQL 4가지 쿼리 자료 안 보고 작성
- [ ] REST Meeting API 빈 종이에 빠르게 설계
- [ ] 자가진단 10문제 답변 가능
- [ ] 자기소개 + reverse questions 외움

---

# 🎯 면접 당일 아침 5분 — Final Cheatsheet

## Java 한 줄

| 주제 | 한 줄 |
|---|---|
| `==` vs equals | reference vs value |
| StringBuilder vs StringBuffer | not safe vs thread-safe |
| Override vs Overload | runtime vs compile-time |
| ArrayList vs LinkedList | read-heavy vs write-heavy |
| HashMap vs ConcurrentHashMap | not safe vs bucket lock |
| final class | cannot inherit |
| Stream | data pipeline, lazy |
| Singleton | private constructor + static getInstance |

## OOP 한 줄

| 주제 | 한 줄 |
|---|---|
| Inheritance | reuse parent via extends |
| Polymorphism | one type, many runtime forms |
| Encapsulation | hide state, validated methods |
| Abstraction | hide complexity behind contract |

## REST 한 줄

| 메서드 | 용도 | 코드 |
|---|---|---|
| GET | read | 200 |
| POST | create | 201 |
| PUT | replace whole | 200 |
| PATCH | update part | 200 |
| DELETE | remove | 204 |

## SQL 한 줄

| 주제 | 한 줄 |
|---|---|
| WHERE vs HAVING | row vs group filter |
| INNER vs LEFT JOIN | matches only vs keep all left |
| EXISTS vs IN | early exit vs full set |
| Window function | per-row aggregate, no collapse |
| Highest salary | `ORDER BY salary DESC FETCH FIRST 1 ROW ONLY` |

## Concurrency 한 줄

| 주제 | 한 줄 |
|---|---|
| Concurrency | progress over time |
| Parallelism | same time, multiple cores |
| Deadlock | two threads wait for each other forever |
| Race condition | result depends on timing |
| ExecutorService | thread pool, preferred over raw Thread |

---

# ⚠️ 면접 진행 표현 (필수 영어)

## 코딩 문제 받았을 때

1. **"OK so the problem is... Let me make sure I understand..."**
2. **"My idea is... I will use..."**
3. (코딩 중) **"Here I declare... Then I loop..."**
4. **"Let me test. With X equals Y..."**
5. **"Time complexity is O(N). Space is O(1)."**

## 막혔을 때 (절대 침묵 금지)

- "Let me think for a moment."
- "Could you give me a hint on the expected approach?"
- "I'm not 100% sure but my best guess is..."
- "I would normally check the documentation, but conceptually..."

## 모를 때 솔직히

- "I don't have hands-on experience with that, but based on what I know..."
- "I'd need to check, but I believe..."

## 마무리

- **"Do you have any questions for us?"** → reverse questions 3개

---

# 🔥 면접 직전 1시간 안 봐도 되는 것

- 자료 다시 안 봐도 됨 (오히려 혼란)
- 깊은 호흡 + 자기소개 한 번 더 + 차 한 잔
- "I'm here to learn and contribute" 마인드

면접 잘 보세요.
