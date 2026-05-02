# PL/SQL 기본 (Oracle)

CMiC 같은 Oracle ERP는 PL/SQL이 매일 나옴.
영어는 단순 SVO, 짧은 문장.

---

## 1. PL/SQL 블록 구조

🧠 4개 섹션: `DECLARE`, `BEGIN`, `EXCEPTION`, `END`.

```sql
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM projects;
    DBMS_OUTPUT.PUT_LINE('Project count: ' || v_count);
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('No data');
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error: ' || SQLERRM);
END;
/
```

🗣️ "PL/SQL block has four parts. DECLARE for variables. BEGIN for code. EXCEPTION for errors. END to close."

---

## 2. 변수 / 데이터 타입

🧠 Oracle 컬럼 타입을 그대로 받는 `%TYPE`, 행 전체는 `%ROWTYPE`.

```sql
DECLARE
    v_id   projects.project_id%TYPE;     -- 컬럼 타입 그대로
    v_row  projects%ROWTYPE;             -- 행 전체 타입
BEGIN
    SELECT * INTO v_row FROM projects WHERE project_id = 100;
    DBMS_OUTPUT.PUT_LINE(v_row.project_name);
END;
/
```

🗣️ "%TYPE matches column type. %ROWTYPE matches the whole row. If table changes, my code still works."

---

## 3. IF / LOOP / FOR

🧠 제어 흐름.

```sql
IF v_status = 'OPEN' THEN
    DBMS_OUTPUT.PUT_LINE('open');
ELSIF v_status = 'CLOSED' THEN
    DBMS_OUTPUT.PUT_LINE('closed');
ELSE
    DBMS_OUTPUT.PUT_LINE('other');
END IF;

FOR i IN 1..5 LOOP
    DBMS_OUTPUT.PUT_LINE(i);
END LOOP;
```

🗣️ "IF is for conditions. FOR loop runs fixed number of times. WHILE loop runs until condition is false."

---

## 4. Cursor (커서)

🧠 SELECT 결과를 한 행씩 처리. **Cursor FOR loop**가 가장 깔끔 (자동 OPEN/FETCH/CLOSE).

```sql
DECLARE
    CURSOR c_projects IS SELECT * FROM projects WHERE status = 'OPEN';
BEGIN
    FOR rec IN c_projects LOOP
        DBMS_OUTPUT.PUT_LINE(rec.project_id || ' - ' || rec.project_name);
    END LOOP;
END;
/
```

🗣️ "Cursor reads SELECT result row by row. Cursor FOR loop is the easiest. It opens, fetches, and closes automatically."

---

## 5. Exception Handling

🧠 흔한 예외: `NO_DATA_FOUND, TOO_MANY_ROWS, DUP_VAL_ON_INDEX, OTHERS`.

```sql
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM projects WHERE project_id = 9999;
    IF v_count = 0 THEN
        RAISE_APPLICATION_ERROR(-20001, 'Invalid project');
    END IF;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('No data');
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Unknown: ' || SQLCODE || ' / ' || SQLERRM);
        ROLLBACK;
        RAISE;
END;
/
```

🗣️ "Built-in exceptions are NO_DATA_FOUND, TOO_MANY_ROWS, OTHERS. Custom exception uses RAISE_APPLICATION_ERROR. Always log error and re-raise. Do not catch silently."

---

## 6. Stored Procedure

🧠 재사용 가능한 PL/SQL 블록. 파라미터 모드: `IN, OUT, IN OUT`.

```sql
CREATE OR REPLACE PROCEDURE approve_project_costs (
    p_project_id IN  projects.project_id%TYPE,
    p_count_out  OUT NUMBER
) AS
BEGIN
    UPDATE project_cost
       SET status = 'APPROVED', approved_date = SYSDATE
     WHERE project_id = p_project_id
       AND status = 'PENDING';

    p_count_out := SQL%ROWCOUNT;
    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END;
/
```

🗣️ "Procedure is reusable PL/SQL block. IN parameter is input. OUT parameter is output. SQL%ROWCOUNT shows how many rows changed."

---

## 7. Function

🧠 Procedure는 값 반환 안 함. Function은 `RETURN` 사용. SQL 안에서도 호출 가능.

```sql
CREATE OR REPLACE FUNCTION get_total_cost (
    p_project_id projects.project_id%TYPE
) RETURN NUMBER AS
    v_total NUMBER;
BEGIN
    SELECT NVL(SUM(amount), 0)
      INTO v_total
      FROM project_cost
     WHERE project_id = p_project_id
       AND status = 'APPROVED';
    RETURN v_total;
END;
/

SELECT project_id, get_total_cost(project_id) AS total FROM projects;
```

🗣️ "Function returns a value. Procedure does not. Function can be used in SELECT. Procedure cannot."

---

## 8. Trigger

🧠 INSERT/UPDATE/DELETE 시 자동 실행.

```sql
CREATE OR REPLACE TRIGGER trg_audit_cost
AFTER INSERT OR UPDATE ON project_cost
FOR EACH ROW
BEGIN
    INSERT INTO cost_audit (cost_id, action, change_date, user_name)
    VALUES (:NEW.cost_id,
            CASE WHEN INSERTING THEN 'INSERT' ELSE 'UPDATE' END,
            SYSDATE,
            USER);
END;
/
```

🗣️ "Trigger runs automatically on INSERT, UPDATE, or DELETE. Good for audit log. But too many triggers make debugging hard."

---

## 9. Transaction — COMMIT / ROLLBACK / SAVEPOINT

🧠 여러 DML을 하나의 단위로 묶음. 일부분만 롤백하려면 `SAVEPOINT`.

```sql
BEGIN
    UPDATE accounts SET balance = balance - 100 WHERE id = 1;
    SAVEPOINT after_debit;

    UPDATE accounts SET balance = balance + 100 WHERE id = 2;
    -- ROLLBACK TO after_debit;   -- 일부만 롤백 가능

    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END;
/
```

🗣️ "Transaction is one unit. All succeed or all rollback. COMMIT saves changes. ROLLBACK undoes them. SAVEPOINT lets you rollback only part."

---

## 10. 자가진단 Q8 정답 — Approve Pending Costs

문제: `PROJECT_COST.STATUS = 'PENDING'`인 행을 모두 `'APPROVED'`로 변경, 예외 시 롤백.

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
        DBMS_OUTPUT.PUT_LINE('Failed: ' || SQLERRM);
        RAISE;
END;
/
```

🗣️ "I use one bulk UPDATE, not a cursor loop. Cursor is slow. Commit if success. Rollback if error. Re-raise so the caller knows."

⚠️ **주의:** 면접에서 cursor loop로 한 행씩 update하는 코드를 쓰지 마세요. **bulk update가 압도적으로 빠릅니다**.

---

## 🎯 PL/SQL 한 줄 카드

| 주제 | 한 줄 |
| --- | --- |
| 블록 구조 | DECLARE / BEGIN / EXCEPTION / END |
| %TYPE / %ROWTYPE | column / row 타입 자동 매칭 |
| Cursor FOR loop | 자동 OPEN/FETCH/CLOSE |
| 흔한 예외 | NO_DATA_FOUND, TOO_MANY_ROWS, OTHERS |
| Custom 예외 | RAISE_APPLICATION_ERROR(-20001, ...) |
| Procedure vs Function | no return vs RETURN value |
| SQL%ROWCOUNT | 마지막 DML의 영향 행 수 |
| Bulk vs cursor loop | bulk이 빠름 |
| Trigger | 행 이벤트 시 자동 실행 |
| SAVEPOINT | 트랜잭션 일부 롤백 |
