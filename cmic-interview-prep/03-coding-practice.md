# 코딩 실전 문제

각 문제: 본인이 먼저 풀고 → 정답 비교 → 영어 설명 외우기.
영어는 단순 SVO, 짧은 문장.

---

## Problem 1 — Task 2 (Codility 실제 출제)

### 문제

배열 A에서 X와 Y의 출현 횟수가 같은 가장 긴 prefix의 마지막 인덱스 P를 반환. 없으면 -1.

**예시:**
- X=7, Y=42, A=[6,42,11,7,1,42] → 4
- X=6, Y=13, A=[13,13,1,6] → -1
- X=100, Y=63, A=[100,63,1,6,2,13] → 5

### 원본 코드 (버그 있음)

```java
class Solution {
    public int solution(int X, int Y, int[] A) {
        int N = A.length;
        int result = -1;
        int nX = 0;
        int nY = 0;
        for (int i = 0; i < N; i++) {
            if (A[i] == X)
                nX += 1;
            else if (A[i] == Y)   // ← BUG
                nY += 1;
            if (nX == nY)
                result = i;
        }
        return result;
    }
}
```

### 버그

`X == Y`일 때 `else if`가 스킵되어 `nY`가 절대 안 증가. 두 카운터가 독립적이어야 함.

### 정답 (2줄 수정)

```java
class Solution {
    public int solution(int X, int Y, int[] A) {
        int nX = 0, nY = 0, result = -1;
        for (int i = 0; i < A.length; i++) {
            if (A[i] == X) nX++;
            if (A[i] == Y) nY++;        // else 제거
            if (nX == nY) result = i;
        }
        return result;
    }
}
```

### 🗣️ 영어

> "The bug is `else if`. When X is equal to Y, only nX increases. nY stays zero. So the counts never match. I remove `else`, so both counters grow independently. This fixes the case when X equals Y. Time is O(N). Space is O(1)."

---

## Problem 2 — Group By Key

### 문제

프로젝트 비용 데이터의 프로젝트별 총액 계산.

```java
class CostEntry {
    String projectId;
    double amount;
    CostEntry(String p, double a) { projectId = p; amount = a; }
}
// 입력: [(P1, 100), (P2, 50), (P1, 200), (P3, 30), (P2, 70)]
// 출력: {P1=300, P2=120, P3=30}
```

### 풀이 1: HashMap + merge (전통)

```java
public Map<String, Double> totalByProject(List<CostEntry> entries) {
    Map<String, Double> totals = new HashMap<>();
    for (CostEntry e : entries) {
        totals.merge(e.projectId, e.amount, Double::sum);
    }
    return totals;
}
```

### 풀이 2: Stream

```java
public Map<String, Double> totalByProjectStream(List<CostEntry> entries) {
    return entries.stream()
        .collect(Collectors.groupingBy(
            e -> e.projectId,
            Collectors.summingDouble(e -> e.amount)
        ));
}
```

### 🗣️ 영어

> "I have two ways. First way uses HashMap and merge. I add the amount to each project. Easy to debug. Second way uses Stream with groupingBy. Shorter code. I usually pick the first way for clarity."

CMiC가 cost management ERP라서 정확히 이런 패턴이 매일 나옴.

---

## Problem 3 — Validate + Detect Invalid

### 문제

주문 리스트에서 유효한 것과 그렇지 않은 것을 분리.
- `quantity > 0`
- `productId`는 null이나 빈 문자열이 아님
- `price >= 0`

```java
class Order {
    String productId;
    int quantity;
    double price;
    Order(String p, int q, double pr) { productId = p; quantity = q; price = pr; }
}

class ValidationResult {
    List<Order> valid = new ArrayList<>();
    List<Order> invalid = new ArrayList<>();
}
```

### 정답

```java
public ValidationResult validate(List<Order> orders) {
    ValidationResult result = new ValidationResult();
    for (Order o : orders) {
        if (isValid(o)) {
            result.valid.add(o);
        } else {
            result.invalid.add(o);
        }
    }
    return result;
}

private boolean isValid(Order o) {
    if (o.productId == null || o.productId.isEmpty()) return false;
    if (o.quantity <= 0) return false;
    if (o.price < 0) return false;
    return true;
}
```

### 🗣️ 영어

> "I put the validation in a separate method called isValid. Easier to test. Easier to add new rules. In a real ERP system, I would make a separate Validator class. Each rule becomes a method. Then I can unit test each rule alone."

---

## ✅ 코딩 면접 진행 6단계

면접에서 코딩 문제를 받으면 항상 이 순서:

1. **문제 다시 말하기**: "OK so the problem is... We have... and we need to..."
2. **접근 설명**: "My idea is... I will use... The plan is..."
3. **코드 작성** (변수명 의미있게)
4. **테스트**: "Let me test. With input X=Y, the result is... With empty array, it returns..."
5. **복잡도**: "Time is O(N). Space is O(1)."
6. **개선점**: "If the array is very large, I could split the work and run in parallel."
