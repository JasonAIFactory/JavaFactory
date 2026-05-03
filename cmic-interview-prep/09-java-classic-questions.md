# Java 단골 면접 질문

영어는 단순 SVO, 짧은 문장.

---

## 1. Java 8 핵심 기능

🧠 4가지 큰 변화: Lambda, Stream, Optional, Default method.

```java
// Lambda
Runnable r = () -> System.out.println("hi");

// Stream
List<Integer> evens = list.stream()
    .filter(n -> n % 2 == 0)
    .collect(Collectors.toList());

// Optional
Optional<User> user = findUser(id);
user.ifPresent(u -> System.out.println(u.getName()));

// Default method in interface
interface Greeter {
    default String hello() { return "Hello"; }
}

// Method reference
list.forEach(System.out::println);

// Date/Time API
LocalDate today = LocalDate.now();
```

🗣️ "Java 8 added four big things. Lambda for short functions. Stream for data pipelines. Optional for null safety. Default methods so interfaces can have implementation. Also a new Date and Time API to replace the old Calendar class."

---

## 2. Stream API

🧠 데이터 처리 파이프라인. 컬렉션이 아님. Lazy.

```java
List<Order> orders = ...;

// 중간 연산 (lazy) + 종단 연산 (collect)
double total = orders.stream()
    .filter(o -> o.status.equals("PAID"))
    .mapToDouble(o -> o.amount)
    .sum();

// 그룹화
Map<String, List<Order>> byStatus = orders.stream()
    .collect(Collectors.groupingBy(o -> o.status));

// 병렬 처리
long count = orders.parallelStream()
    .filter(o -> o.amount > 1000)
    .count();
```

🗣️ "Stream is a data pipeline. It is not a data structure. Operations like filter and map are lazy. Nothing runs until a terminal operation like collect or sum. Streams support parallel execution with parallelStream."

---

## 3. StringBuffer vs StringBuilder

🧠 둘 다 가변 문자열. 차이는 thread-safe 여부.

```java
// StringBuilder — 단일 스레드, 빠름
StringBuilder sb = new StringBuilder();
sb.append("Hello").append(" ").append("World");

// StringBuffer — 멀티 스레드 안전, 느림 (synchronized)
StringBuffer sbf = new StringBuffer();
sbf.append("data");
```

🗣️ "StringBuffer is thread-safe. Its methods are synchronized. StringBuilder is not thread-safe but faster. I use StringBuilder for single thread. I use StringBuffer for multi-thread environment. String itself is immutable — every concatenation makes a new object, so for loops should use StringBuilder."

| 비교 | String | StringBuilder | StringBuffer |
| --- | --- | --- | --- |
| 변경 가능? | ❌ immutable | ✅ | ✅ |
| Thread-safe? | (immutable이라 OK) | ❌ | ✅ |
| 속도 | 느림 (매번 새 객체) | 빠름 | 중간 |

---

## 4. Method Overriding vs Overloading

🧠 **Overriding** = 상속에서 메서드 재정의 (runtime polymorphism). **Overloading** = 같은 이름, 다른 파라미터 (compile-time).

```java
// Overriding
class Animal {
    String sound() { return "generic"; }
}
class Dog extends Animal {
    @Override
    String sound() { return "woof"; }
}
class Cat extends Animal {
    @Override
    String sound() { return "meow"; }
}

Animal a1 = new Dog();
Animal a2 = new Cat();
System.out.println(a1.sound());   // woof  (런타임 결정)
System.out.println(a2.sound());   // meow

// Overloading
class Calculator {
    int add(int a, int b) { return a + b; }
    double add(double a, double b) { return a + b; }
    int add(int a, int b, int c) { return a + b + c; }
}
```

🗣️ "Overriding is when a child class replaces a parent's method. Same name, same parameters. Decided at runtime. Overloading is when one class has multiple methods with the same name but different parameters. Decided at compile time. Overriding is the heart of polymorphism."

### 보너스: Strategy Pattern

🗣️ "Animal example shows Strategy Pattern. We hold the parent type but call the child method. To add a new strategy, just create a new subclass — no change to the caller."

---

## 5. LinkedList vs ArrayList

🧠 ArrayList = 배열 기반, **read-heavy**. LinkedList = 양방향 연결, **write-heavy** (insert/delete).

```java
// ArrayList — 인덱스 접근 빠름
List<Integer> al = new ArrayList<>();
al.add(1); al.add(2); al.add(3);
al.get(1);   // O(1)

// LinkedList — 중간 삽입/삭제 빠름
List<Integer> ll = new LinkedList<>();
ll.add(0, 99);   // 앞에 삽입 O(1)
```

| 작업 | ArrayList | LinkedList |
| --- | --- | --- |
| `get(i)` | O(1) | O(N) |
| `add()` (끝) | O(1) amortized | O(1) |
| `add(i, x)` (중간) | O(N) | O(N) — 노드 찾는 비용 |
| 메모리 | 적음 (배열) | 많음 (각 노드에 prev/next 포인터) |

🗣️ "ArrayList is array-based. Get by index is O(1). Good for read-heavy code. LinkedList is doubly-linked. It is good for frequent insert and delete at the head or middle. In practice, ArrayList is the right default. Most production code uses ArrayList."

---

## 6. HashMap vs ConcurrentHashMap

🧠 둘 다 key-value. 차이는 thread-safe 여부와 동시성 성능.

```java
// HashMap — 단일 스레드. 멀티 스레드에서는 위험
Map<String, Integer> map = new HashMap<>();

// ConcurrentHashMap — 멀티 스레드 안전. 버킷 단위 락 (성능 좋음)
Map<String, Integer> safe = new ConcurrentHashMap<>();
safe.compute("counter", (k, v) -> v == null ? 1 : v + 1);

// Hashtable — 옛날 방식. 전체 락 (느림). 거의 안 씀.
```

🗣️ "HashMap is not thread-safe. In multi-thread environment, it can corrupt data or even cause infinite loop in old Java versions. ConcurrentHashMap is thread-safe. It uses bucket-level locking, so multiple threads can read and write at the same time. Hashtable is the old version with full-table locking — slow and rarely used now."

---

## 7. Tree — TreeMap / TreeSet

🧠 Red-Black tree (자기 균형 이진 트리). 키 기준 정렬됨. O(log N).

```java
TreeMap<String, Integer> tm = new TreeMap<>();
tm.put("banana", 2);
tm.put("apple", 1);
tm.put("cherry", 3);
// 자동으로 키 알파벳 순
for (var e : tm.entrySet()) System.out.println(e);
// apple=1, banana=2, cherry=3

tm.firstKey();      // apple
tm.lastKey();       // cherry
tm.headMap("c");    // apple, banana
```

🗣️ "TreeMap and TreeSet are based on a Red-Black tree. Operations are O(log N). The keys are automatically sorted. We use them when we need ordered traversal or range queries. For simple lookup, HashMap is faster."

---

## 8. Singleton Pattern (Live Coding)

🧠 한 클래스에서 인스턴스 하나만. 4가지 방식.

### Eager (초기화 즉시 — 단순, thread-safe)

```java
public class Config {
    private static final Config INSTANCE = new Config();
    private Config() {}
    public static Config getInstance() { return INSTANCE; }
}
```

### Lazy + Double-checked Locking (지연, thread-safe)

```java
public class Config {
    private static volatile Config instance;
    private Config() {}

    public static Config getInstance() {
        if (instance == null) {
            synchronized (Config.class) {
                if (instance == null) {
                    instance = new Config();
                }
            }
        }
        return instance;
    }
}
```

### Enum (가장 안전 — 추천)

```java
public enum Config {
    INSTANCE;
    public void doSomething() { ... }
}
// 사용
Config.INSTANCE.doSomething();
```

🗣️ "Singleton makes sure only one instance exists. The simplest version uses a static final field — eager init, thread-safe. For lazy init we need double-checked locking with volatile. The cleanest way is to use enum with a single value — it is thread-safe and prevents reflection attacks. I prefer enum unless I really need lazy."

### 실제 사용 예

- DB connection pool
- 로깅 객체 (Logger)
- 애플리케이션 설정 (Config)
- 캐시

---

## 🎯 한 줄 카드

| 주제 | 한 줄 |
| --- | --- |
| Java 8 | Lambda, Stream, Optional, Default method |
| Stream | data pipeline, lazy |
| StringBuilder vs StringBuffer | not safe vs thread-safe |
| Override | subclass redefines (runtime) |
| Overload | same name, different params (compile) |
| ArrayList | read-heavy, O(1) get |
| LinkedList | write-heavy, O(1) head insert |
| HashMap | not thread-safe |
| ConcurrentHashMap | thread-safe, bucket lock |
| TreeMap | sorted by key, O(log N) |
| Singleton | one instance — enum is best |
