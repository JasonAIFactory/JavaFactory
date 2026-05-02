# SQL 기본 (Oracle)

CMiC는 Oracle ERP라서 SQL이 합격선. 모든 예시는 Oracle 문법.
영어는 단순 SVO, 짧은 문장.

---

## 1. SELECT / WHERE / ORDER BY

🧠 데이터 조회의 기본. WHERE는 필터, ORDER BY는 정렬.

```sql
SELECT project_id, project_name, status
FROM projects
WHERE status = 'IN_PROGRESS'
ORDER BY project_name ASC;
```

🗣️ "SELECT picks columns. WHERE filters rows. ORDER BY sorts the result."

---

## 2. Aggregate + GROUP BY + HAVING

🧠 집계 함수: `COUNT, SUM, AVG, MIN, MAX`. WHERE는 행 필터, HAVING은 그룹 필터.

```sql
SELECT project_id, SUM(amount) AS total_cost
FROM project_cost
WHERE status = 'APPROVED'
GROUP BY project_id
HAVING SUM(amount) > 100000
ORDER BY total_cost DESC;
```

🗣️ "GROUP BY makes groups. WHERE filters rows before group. HAVING filters groups after group. Aggregate functions are SUM, COUNT, AVG, MIN, MAX."

---

## 3. JOIN — INNER / LEFT / RIGHT

🧠 INNER는 양쪽 매칭만, LEFT는 왼쪽 다 살림, RIGHT는 그 반대.

```sql
-- 모든 프로젝트 + 비용 (없는 프로젝트도 표시)
SELECT p.project_id, p.project_name, NVL(SUM(c.amount), 0) AS total
FROM projects p
LEFT JOIN project_cost c ON p.project_id = c.project_id
GROUP BY p.project_id, p.project_name;
```

🗣️ "INNER JOIN keeps only matching rows. LEFT JOIN keeps all left rows. If no match on right, right columns are null. We use LEFT JOIN to find missing data."

---

## 4. Subquery

🧠 쿼리 안의 쿼리. SELECT, FROM, WHERE 어디서든 사용 가능.

```sql
SELECT project_id, total_cost
FROM (
    SELECT project_id, SUM(amount) AS total_cost
    FROM project_cost
    GROUP BY project_id
)
WHERE total_cost > (SELECT AVG(amount) FROM project_cost);
```

🗣️ "Subquery is a query inside another query. We use it to compute a value and use it in the outer query. Inline view in FROM is common in Oracle reports."

---

## 5. EXISTS vs IN

🧠 IN은 결과 전체를 펼친 후 비교. EXISTS는 한 행만 발견되면 즉시 true.

```sql
-- IN
SELECT * FROM projects p
WHERE p.project_id IN (
    SELECT c.project_id FROM project_cost c WHERE c.status = 'PENDING'
);

-- EXISTS (큰 데이터셋에서 보통 더 빠름)
SELECT * FROM projects p
WHERE EXISTS (
    SELECT 1 FROM project_cost c
    WHERE c.project_id = p.project_id AND c.status = 'PENDING'
);
```

🗣️ "IN checks the full list. EXISTS stops at the first match. EXISTS is faster for big tables. EXISTS is also safer with NULL."

---

## 6. UNION vs UNION ALL

🧠 UNION은 중복 제거 (느림), UNION ALL은 중복 유지 (빠름).

```sql
SELECT project_id FROM active_projects
UNION ALL
SELECT project_id FROM archived_projects;
```

🗣️ "UNION removes duplicates. UNION ALL keeps duplicates. UNION ALL is faster. I use UNION ALL by default."

---

## 7. Window Functions

🧠 GROUP BY 없이 행 단위 집계 + 순위/누적. `OVER (PARTITION BY ... ORDER BY ...)`

```sql
SELECT project_id, amount,
       ROW_NUMBER() OVER (PARTITION BY project_id ORDER BY amount DESC) AS rn,
       SUM(amount)  OVER (PARTITION BY project_id) AS project_total,
       SUM(amount)  OVER (PARTITION BY project_id ORDER BY entry_date) AS running_total
FROM project_cost;
```

🗣️ "Window function makes a value per row. It does not group rows. ROW_NUMBER gives a rank. SUM OVER gives a running total. We use it for top-N and running totals."

---

## 8. Views

🧠 저장된 SELECT 쿼리. 진짜 데이터는 없음.

```sql
CREATE OR REPLACE VIEW active_project_summary AS
SELECT p.project_id, p.project_name, SUM(c.amount) AS total
FROM projects p
LEFT JOIN project_cost c ON p.project_id = c.project_id
WHERE p.status = 'IN_PROGRESS'
GROUP BY p.project_id, p.project_name;
```

🗣️ "A view is a saved query. It has no data. It runs every time. Materialized view stores the result."

---

## 9. Primary Key / Foreign Key / Constraints

🧠 PK는 행 고유 식별. FK는 다른 테이블 PK 참조. 그 외 NOT NULL / UNIQUE / CHECK.

```sql
CREATE TABLE projects (
    project_id   NUMBER PRIMARY KEY,
    project_name VARCHAR2(100) NOT NULL,
    status       VARCHAR2(20) CHECK (status IN ('OPEN','IN_PROGRESS','CLOSED'))
);

CREATE TABLE project_cost (
    cost_id    NUMBER PRIMARY KEY,
    project_id NUMBER NOT NULL,
    amount     NUMBER(12,2) NOT NULL,
    CONSTRAINT fk_cost_project
        FOREIGN KEY (project_id) REFERENCES projects(project_id)
);
```

🗣️ "Primary key is unique row ID. Foreign key points to another table's primary key. Constraint blocks bad data. We push business rules into the database."

---

## 10. Normalization (정규화)

🧠 중복 줄이고 일관성 보장. 1NF → 2NF → 3NF.

🗣️ "Normalization removes duplicate data. We split tables to reduce redundancy. 3NF is the common target. For reports and data warehouse, we sometimes denormalize for speed."

---

## 🎯 SQL 한 줄 카드

| 주제 | 한 줄 |
| --- | --- |
| WHERE vs HAVING | row filter vs group filter |
| INNER vs LEFT JOIN | only matches vs keep all left |
| EXISTS vs IN | early exit vs full set |
| UNION vs UNION ALL | distinct vs include duplicates |
| Window function | aggregate per row, no collapse |
| View | saved query, no data |
| PK | unique row identifier |
| FK | referential integrity |
| 3NF | no transitive dependency |
