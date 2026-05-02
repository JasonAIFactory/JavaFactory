# 정답 (스포일러 — 본인이 풀고 나서만 보기)

⚠️ **먼저 직접 풀고 막혔을 때만 펼치기**

---

## Problem 1 — PrefixCount

```java
public int solution(int X, int Y, int[] A) {
    int nX = 0, nY = 0, result = -1;
    for (int i = 0; i < A.length; i++) {
        if (A[i] == X) nX++;
        if (A[i] == Y) nY++;     // else 가 있으면 X==Y 케이스 깨짐
        if (nX == nY) result = i;
    }
    return result;
}
```

🗣️ "I scan once with two counters. When the counts match, I update the result. The key trick is to NOT use `else if` — that breaks when X equals Y."

---

## Problem 2 — GroupByKey

```java
public Map<String, Double> totalByProject(List<CostEntry> entries) {
    Map<String, Double> totals = new HashMap<>();
    for (CostEntry e : entries) {
        totals.merge(e.projectId, e.amount, Double::sum);
    }
    return totals;
}
```

또는 Stream 버전:

```java
return entries.stream()
    .collect(Collectors.groupingBy(
        e -> e.projectId,
        Collectors.summingDouble(e -> e.amount)
    ));
```

🗣️ "I use HashMap and merge. Merge takes the key, the value, and a function. If key exists, it sums. If not, it inserts. Clean and O(N)."

---

## Problem 3 — ValidateOrders

```java
public ValidationResult validate(List<Order> orders) {
    ValidationResult result = new ValidationResult();
    for (Order o : orders) {
        if (isValid(o)) result.valid.add(o);
        else            result.invalid.add(o);
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

🗣️ "I put validation in a separate isValid method. Easier to test. Easier to add new rules later."

---

## Problem 4 — FindDuplicates

```java
public List<String> findDuplicates(List<String> input) {
    Set<String> seen = new HashSet<>();
    Set<String> reported = new HashSet<>();
    List<String> result = new ArrayList<>();
    for (String s : input) {
        if (!seen.add(s) && reported.add(s)) {
            result.add(s);
        }
    }
    return result;
}
```

설명:
- `seen.add(s)` → false면 이미 있던 것 → 중복
- `reported.add(s)` → 결과 리스트에 한 번만 추가하기 위한 가드

🗣️ "I use two sets. The first tracks what I have seen. The second tracks what I have already reported. So each duplicate goes to the result only once. One pass, O(N)."

---

## Problem 5 — SimpleCache (LRU)

```java
import java.util.*;

static class SimpleCache<K, V> {
    private final int capacity;
    private final LinkedHashMap<K, V> map;

    public SimpleCache(int capacity) {
        this.capacity = capacity;
        // accessOrder=true 가 LRU의 핵심
        this.map = new LinkedHashMap<K, V>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > SimpleCache.this.capacity;
            }
        };
    }

    public V get(K key) { return map.get(key); }
    public void put(K key, V value) { map.put(key, value); }
    public int size() { return map.size(); }
}
```

🗣️ "I use LinkedHashMap with access-order true. That makes the map track access time. Then I override removeEldestEntry to evict when size goes over capacity. This is the standard Java LRU pattern."

---

## 💡 면접에서의 팁

1. **무조건 `else if` 조심** — Problem 1처럼 두 조건이 독립적이어야 할 때 버그
2. **`merge` 외워두기** — Problem 2, 6, 7 등 집계 패턴에 반복 등장
3. **별도 helper 메서드** — Problem 3처럼 로직 분리 = 테스트 가능 + 영어로 설명하기 쉬움
4. **Set/Map 적재적소** — Problem 4, 5처럼 자료구조 한 번 잘 고르면 코드 절반
5. **표준 라이브러리 우선** — Problem 5의 LinkedHashMap처럼, 이미 있는 거 다시 만들지 마세요
