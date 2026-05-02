# Java 기본 17주제

각 주제마다:
- 🧠 한 줄 정의 (한국어)
- ⌨️ 직접 타이핑할 코드
- 🗣️ 영어로 말하기 (단순 SVO, 짧은 문장)

---

## 1. Primitive vs Reference Type

🧠 Primitive은 값 자체 저장, Reference는 객체 주소 저장.

```java
int a = 10;
int b = a;          // 값 복사
b = 20;
System.out.println(a);  // 10

int[] arr1 = {1, 2, 3};
int[] arr2 = arr1;  // 주소 복사
arr2[0] = 99;
System.out.println(arr1[0]);  // 99
```

🗣️ "Primitive type stores the value. Reference type stores the address. Two references can point to the same object. So changing one affects the other."

---

## 2. Local Variable vs Field Default

🧠 멤버 변수는 자동 기본값, 지역 변수는 초기화 안 하면 컴파일 에러.

```java
public class Defaults {
    int memberInt;          // 0
    boolean memberBool;     // false
    String memberStr;       // null

    void method() {
        int localInt;
        // System.out.println(localInt);  // 컴파일 에러!
        localInt = 5;
    }
}
```

🗣️ "Field has default value. Int is zero, boolean is false, object is null. Local variable has no default. You must set it before you use it."

---

## 3. String `==` vs `.equals()`

🧠 `==`는 주소 비교, `.equals()`는 값 비교. String literal은 pool에서 공유.

```java
String a = "hello";
String b = "hello";              // pool에서 공유
String c = new String("hello");  // 새 객체

System.out.println(a == b);        // true
System.out.println(a == c);        // false
System.out.println(a.equals(c));   // true
```

🗣️ "Two equal signs check the address. The equals method checks the value. String literals share one object in the pool. New String always makes a new object."

⚠️ `equals()` 오버라이드하면 반드시 `hashCode()`도 오버라이드. 안 그러면 HashMap/HashSet 동작 깨짐.

---

## 4. final class

🧠 `final` 클래스는 상속 불가. 메서드는 오버라이드 불가. 변수는 재할당 불가.

```java
public final class ImmutablePoint {
    private final int x;
    private final int y;
    public ImmutablePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
// class SubPoint extends ImmutablePoint { }  // 컴파일 에러!
```

🗣️ "Final class cannot be extended. Final method cannot be overridden. Final variable cannot be changed. String is final for safety."

---

## 5. abstract class vs interface

🧠 abstract class는 일부 구현 + 상태 가능, interface는 계약(contract).

```java
abstract class Report {
    String title;
    Report(String t) { title = t; }
    void printHeader() { System.out.println("=== " + title + " ==="); }
    abstract void printBody();   // 서브클래스가 구현
}

interface Exportable {
    void exportToPdf();
}

class CostReport extends Report implements Exportable {
    CostReport(String t) { super(t); }
    void printBody() { System.out.println("cost data..."); }
    public void exportToPdf() { System.out.println("exporting..."); }
}
```

🗣️ "Abstract class can have fields and shared code. Interface is only a contract. A class extends one abstract class. A class can implement many interfaces."

---

## 6. Inheritance / Polymorphism

🧠 Inheritance는 `extends`로 코드 재사용, Polymorphism은 부모 타입으로 자식 객체 다루기.

```java
class Employee {
    double calculatePay() { return 3000.0; }
}
class Manager extends Employee {
    @Override
    double calculatePay() { return 5000.0; }
}
class Contractor extends Employee {
    @Override
    double calculatePay() { return 2000.0; }
}

Employee[] team = { new Employee(), new Manager(), new Contractor() };
double total = 0;
for (Employee e : team) {
    total += e.calculatePay();   // 실제 객체 타입의 메서드 호출
}
// total = 10000.0
```

🗣️ "Inheritance is when a child class reuses the parent class. Polymorphism is when a parent type holds a child object. At runtime, Java calls the real child method. So we can add new types without changing old code."

---

## 7. Access Modifiers

🧠 private → default(package) → protected → public 순으로 넓어짐.

| 제어자 | 같은 클래스 | 같은 패키지 | 서브클래스 (다른 패키지) | 외부 |
| --- | :-: | :-: | :-: | :-: |
| `private` | ✅ | ❌ | ❌ | ❌ |
| (default) | ✅ | ✅ | ❌ | ❌ |
| `protected` | ✅ | ✅ | ✅ | ❌ |
| `public` | ✅ | ✅ | ✅ | ✅ |

```java
public class AccessDemo {
    private int secret = 1;
    int packageField = 2;          // default
    protected int protField = 3;
    public int publicField = 4;
}
```

🗣️ "Private is same class only. Default is same package. Protected is package and subclasses. Public is everyone."

---

## 8. Exception Catch Order

🧠 자식 예외 먼저, 부모 예외 나중. 순서 틀리면 컴파일 에러.

```java
try {
    int[] a = {1, 2, 3};
    int x = a[10];
} catch (ArrayIndexOutOfBoundsException e) {  // 자식 먼저
    System.out.println("array bound!");
} catch (Exception e) {                        // 부모 나중
    System.out.println("generic!");
} finally {
    System.out.println("always runs");
}
```

🗣️ "Child exception comes first. Parent exception comes last. If parent is first, child catch is never reached. Compile error. Finally block always runs."

---

## 9. switch fall-through

🧠 `break` 안 쓰면 다음 case로 떨어짐.

```java
String value = "red";
switch (value) {
    case "red":
        System.out.println("FAIL");   // break 없음
    case "green":
        System.out.println("OK");
}
// 출력: FAIL\nOK
```

🗣️ "Without break, the next case also runs. This is called fall-through. It is a common bug."

---

## 10. Method Parameter Reassignment

🧠 메서드 파라미터에 재할당 가능. 단, primitive는 호출자에 영향 없음 (pass-by-value).

```java
void method(int a) {
    a = 4;   // 합법. 호출자의 변수에는 영향 없음
}
```

🗣️ "You can change the parameter inside the method. But the caller does not see the change. Java passes value, not the variable itself."

---

## 11. ArrayList / HashMap

🧠 ArrayList = 인덱스 접근 O(1). HashMap = key 조회 평균 O(1), 순서 미정의.

```java
import java.util.*;

List<String> list = new ArrayList<>();
list.add("a"); list.add("b");
for (String s : list) System.out.println(s);  // 삽입 순서

Map<String, Integer> map = new HashMap<>();
map.put("apple", 1);
map.put("banana", 2);
for (Map.Entry<String, Integer> e : map.entrySet()) {
    System.out.println(e.getKey() + "=" + e.getValue());  // 순서 보장 X
}
map.getOrDefault("kiwi", 0);   // 0
map.merge("apple", 10, Integer::sum);  // apple=11
```

🗣️ "ArrayList is a resizable array. Get by index is O(1). HashMap finds value by key in O(1) average. HashMap order is not defined. Use LinkedHashMap for insertion order."

---

## 12. Serializable

🧠 객체를 byte stream으로 변환해서 저장/전송 가능. Marker interface (메서드 없음).

```java
import java.io.*;

class User implements Serializable {
    private static final long serialVersionUID = 1L;
    String name;
    transient String password;   // 직렬화 제외
}
```

🗣️ "Serializable means an object can become bytes. We save bytes to file or send over network. Transient fields are skipped. It is a marker interface, no method to implement."

---

## 13. Anonymous Class

🧠 이름 없이 인터페이스/추상클래스를 즉석에서 구현하면서 객체 생성.

```java
import java.util.*;

List<String> list = new ArrayList<>(Arrays.asList("ccc", "a", "bb"));

// 익명 클래스
Collections.sort(list, new Comparator<String>() {
    @Override
    public int compare(String a, String b) {
        return a.length() - b.length();
    }
});

// 람다 (Java 8+)
Collections.sort(list, (a, b) -> a.length() - b.length());
```

🗣️ "Anonymous class is a class without a name. We declare and create the object at the same time. Used for one-time things like Comparator. Java 8 lambda replaces most cases."

---

## 14. JAR

🧠 Java Archive — `.class` 파일과 리소스를 ZIP 형식으로 묶은 배포 단위.

```bash
javac MyApp.java
jar cf myapp.jar MyApp.class
java -cp myapp.jar MyApp
```

🗣️ "JAR is a zip file. Inside are compiled classes and resources. We share Java apps as JAR. Manifest file tells the main class."

---

## 15. toString()

🧠 객체를 문자열로 표현. `println(obj)` 시 자동 호출.

```java
public class Rectangle {
    public int height, width;
    @Override
    public String toString() {
        return "Rectangle[" + width + "x" + height + "]";
    }
}
System.out.println(new Rectangle());  // Rectangle[0x0]
```

🗣️ "When you print an object, Java calls toString. Default is class name and hash code. We override it for readable output. Useful for logging and debugging."

---

## 16. Java 8 Streams

🧠 데이터 처리 파이프라인. 컬렉션 자체가 아님. 함수형 연산 (filter / map / reduce).

```java
List<Integer> evens = list.stream()
    .filter(n -> n % 2 == 0)
    .collect(Collectors.toList());
```

🗣️ "Stream is a pipeline for data. We use filter, map, collect. Stream is not a data structure. It is lazy. Nothing runs until terminal operation like collect."

---

## 17. Buffered I/O

🧠 한 번에 여러 byte 묶어서 읽기/쓰기. 시스템 콜 줄여서 성능 향상.

```java
try (BufferedReader br = new BufferedReader(new FileReader("file.txt"))) {
    String line;
    while ((line = br.readLine()) != null) {
        System.out.println(line);
    }
}
```

🗣️ "Buffered I/O reads or writes in blocks. Not one byte at a time. Much faster. Always wrap raw streams with buffered."

---

## 🎯 면접 직전 한 줄 카드

| 주제 | 한 줄 답변 |
| --- | --- |
| `==` vs `equals` | reference vs value |
| String pool | literals shared, `new String` always heap |
| Local var | must initialize, no default |
| Exception order | child first, parent last |
| switch | needs `break`, falls through otherwise |
| Method param | pass-by-value, reassign is local only |
| extends / implements | 1 class, many interfaces |
| protected | package + all subclasses |
| final class | cannot inherit |
| abstract method | subclass must implement |
| toString | override for readable output |
| Serializable | save/reconstruct across JVMs |
| HashMap | iterable, order undefined |
| ArrayList.add | amortized O(1) |
| Anonymous class | declare + instantiate, no name |
| Stream | pipeline, not storage |
| Buffered I/O | block reads, faster |
