# SQL 튜닝 / 인덱스 / 실행계획

CMiC가 운영 지원도 시키는 회사라 **튜닝 경험을 영어로 설명할 수 있어야** 합니다.
영어는 단순 SVO, 짧은 문장.

---

## 1. Index

🧠 책의 색인 같은 것. 검색 속도 ↑, INSERT/UPDATE/DELETE 속도 ↓.

```sql
-- B-tree (기본)
CREATE INDEX idx_cost_project ON project_cost(project_id);

-- Composite (복합)
CREATE INDEX idx_cost_status_date ON project_cost(status, entry_date);

-- Function-based
CREATE INDEX idx_proj_upper_name ON projects(UPPER(project_name));

-- Unique
CREATE UNIQUE INDEX idx_proj_code ON projects(project_code);
```

🗣️ "Index is like a book index. It makes search fast. But INSERT and UPDATE become slower. I add index on columns used in WHERE or JOIN. For composite index, column order matters left to right."

⚠️ **인덱스 안 타는 흔한 실수:**
- `WHERE TRUNC(entry_date) = ...` → 함수가 컬럼에 적용되면 인덱스 못 탐
- `WHERE col = :value OR col IS NULL` → OR + NULL은 인덱스 회피
- 복합 인덱스 `(A, B)`인데 WHERE에 B만 있을 때 → 못 탐

---

## 2. Execution Plan (실행계획)

🧠 Oracle이 쿼리를 어떻게 실행할지 계획. 튜닝의 시작점.

```sql
EXPLAIN PLAN FOR
SELECT p.project_name, SUM(c.amount)
FROM projects p
JOIN project_cost c ON p.project_id = c.project_id
WHERE p.status = 'OPEN'
GROUP BY p.project_name;

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);
```

### 봐야 할 항목

| 항목 | 의미 | 좋은가? |
| --- | --- | --- |
| TABLE ACCESS FULL | 테이블 전체 스캔 | 작은 테이블 OK, 큰 테이블 ❌ |
| INDEX RANGE SCAN | 인덱스 사용 | ✅ |
| INDEX UNIQUE SCAN | PK/Unique 조회 | ✅ 최고 |
| NESTED LOOPS | 작은 결과 + 인덱스 | 상황에 따라 |
| HASH JOIN | 큰 테이블끼리 | 상황에 따라 |
| Cost (숫자) | 옵티마이저 추정 비용 | 낮을수록 좋음 |

🗣️ "Execution plan shows how Oracle runs the query. First I check FULL TABLE SCAN on big tables. That is bad. Then I check the cost number. Lower cost is better. I look for missing index, stale stats, or wrong join."

---

## 3. SQL 튜닝 핵심 패턴

### 3-1. SELECT * 피하기

```sql
-- ❌
SELECT * FROM project_cost WHERE project_id = 100;
-- ✅
SELECT cost_id, amount, status FROM project_cost WHERE project_id = 100;
```

🗣️ "Do not use SELECT star. Pick only the columns you need. Faster and uses less memory."

### 3-2. 바인드 변수 사용

```sql
-- ❌ 매번 새 SQL → hard parse
"SELECT * FROM projects WHERE id = " + projectId

-- ✅ PreparedStatement / 바인드 변수
"SELECT * FROM projects WHERE id = ?"
SELECT * FROM projects WHERE id = :p_id;
```

🗣️ "Use bind variable, not string concatenation. Oracle reuses the parsed plan. Faster. Also blocks SQL injection."

### 3-3. EXISTS over IN

[06-sql-basics.md](06-sql-basics.md) 참조.

### 3-4. 함수는 컬럼이 아니라 값에

```sql
-- ❌ 인덱스 회피
WHERE TRUNC(entry_date) = DATE '2026-01-15'
-- ✅ 인덱스 사용
WHERE entry_date >= DATE '2026-01-15'
  AND entry_date <  DATE '2026-01-16'
```

🗣️ "Do not put function on the indexed column. WHERE TRUNC(date) breaks the index. Use a range instead."

### 3-5. 통계 정보 갱신

```sql
EXEC DBMS_STATS.GATHER_TABLE_STATS('SCHEMA', 'PROJECT_COST');
```

🗣️ "If query is suddenly slow, check statistics. Stale stats make optimizer choose bad plan. Gather stats again."

### 3-6. Hint (마지막 수단)

```sql
SELECT /*+ INDEX(c idx_cost_project) */ *
FROM project_cost c
WHERE project_id = 100;
```

🗣️ "Hint forces a plan. I use it only as last option. It hides the real problem."

---

## 4. Transaction / Isolation Level

🧠 Oracle 기본은 `READ COMMITTED`.

| 격리 수준 | 막아주는 것 |
| --- | --- |
| READ COMMITTED | dirty read |
| REPEATABLE READ | dirty + non-repeatable read |
| SERIALIZABLE | 모두 |

```sql
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;

-- 행 잠금
SELECT * FROM project_cost WHERE cost_id = 100 FOR UPDATE;
```

🗣️ "Oracle default is READ COMMITTED. You see only committed data. SELECT FOR UPDATE locks the row. Other sessions cannot change it until I commit."

---

## 5. Production Issue 답변 (자가진단 Q10)

**Situation:** "Yesterday the cost report worked, today wrong totals. No deploy."

🗣️ 답변 (5단계):

> "First, I reproduce the bug with the user. I get the exact project ID and date.
>
> Second, I check if the data changed. No code deploy means data probably changed. I check audit logs.
>
> Third, I run the query manually. I compare with expected result. Maybe a filter is missing or join is duplicating rows.
>
> Fourth, I check execution plan and statistics. Volume change can cause a different plan.
>
> Fifth, I check the overnight batch job. Maybe it failed.
>
> The main idea is: do not guess. Check what changed first."

---

## 🎯 튜닝 한 줄 카드

| 주제 | 한 줄 |
| --- | --- |
| Index | book index — fast read, slow write |
| Composite index | left-to-right column order matters |
| Function in WHERE | breaks index unless function-based index |
| EXPLAIN PLAN | first thing to check on slow query |
| FULL SCAN on big table | usually a problem |
| Bind variable | parse once, reuse plan |
| Stale stats | optimizer picks bad plan |
| `FOR UPDATE` | row lock for race-free ops |
| READ COMMITTED | Oracle default isolation |
| Hint | last resort, hides root cause |
