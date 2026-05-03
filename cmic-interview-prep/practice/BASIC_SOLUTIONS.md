# Basic 1~10 정답 (스포일러)

⚠️ 직접 풀고 막혔을 때만 보기.

---

## Basic 1 — Print and Variables

```java
int age = 30;
double salary = 50000.50;
String name = "Alice";

System.out.println(age);
System.out.println(salary);
System.out.println(name);
```

---

## Basic 2 — Arrays and Loop

```java
int[] arr = {1, 2, 3, 4, 5};
int sum = 0;
for (int i = 0; i < arr.length; i++) {
    sum += arr[i];
}
System.out.println(sum);   // 15
```

또는 for-each:
```java
for (int n : arr) sum += n;
```

---

## Basic 3 — If / Else

```java
if (score >= 90) {
    System.out.println("A");
} else if (score >= 80) {
    System.out.println("B");
} else if (score >= 70) {
    System.out.println("C");
} else {
    System.out.println("F");
}
```

---

## Basic 4 — Method

```java
static int add(int a, int b) {
    return a + b;
}

static boolean isEven(int n) {
    return n % 2 == 0;
}

// main 안에서
System.out.println(add(3, 4));     // 7
System.out.println(isEven(10));    // true
```

⚠️ `main`에서 직접 호출하려면 메서드를 `static`으로 선언해야 함.

---

## Basic 5 — Strings

```java
System.out.println(s.length());                              // 12
System.out.println(s.toUpperCase());                         // INV-001-2026
System.out.println(s.startsWith("INV-"));                    // true
System.out.println(s.substring(s.indexOf('-') + 1));         // 001-2026
```

---

## Basic 6 — ArrayList

```java
List<String> list = new ArrayList<>();
list.add("apple");
list.add("banana");
list.add("cherry");

System.out.println(list.size());   // 3

for (String s : list) {
    System.out.println(s);
}

System.out.println(list.get(1));   // banana
```

---

## Basic 7 — HashMap

```java
Map<String, Integer> map = new HashMap<>();
map.put("apple", 1);
map.put("banana", 2);
map.put("cherry", 3);

System.out.println(map.get("banana"));               // 2
System.out.println(map.containsKey("apple"));        // true
System.out.println(map.getOrDefault("kiwi", 0));     // 0
```

---

## Basic 8 — Simple Class

```java
public class Basic08_SimpleClass {

    static class Person {
        private String name;
        private int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public int getAge() { return age; }

        @Override
        public String toString() {
            return "Person[" + name + ", " + age + "]";
        }
    }

    public static void main(String[] args) {
        Person p = new Person("Alice", 30);
        System.out.println(p);   // Person[Alice, 30]
    }
}
```

---

## Basic 9 — Inheritance

```java
static class Dog extends Animal {
    @Override
    String sound() { return "woof"; }
}

static class Cat extends Animal {
    @Override
    String sound() { return "meow"; }
}

public static void main(String[] args) {
    Animal[] arr = { new Dog(), new Cat(), new Animal() };
    for (Animal a : arr) {
        System.out.println(a.sound());
    }
}
```

이게 **polymorphism** 그 자체. `Animal[]` 안에 `Dog`, `Cat`이 들어있어도 `a.sound()`는 실제 객체 메서드 호출.

---

## Basic 10 — Try / Catch

```java
try {
    int x = arr[10];
} catch (ArrayIndexOutOfBoundsException e) {
    System.out.println("caught: out of bounds");
}

System.out.println("program continues");
```

🗣️ "Without try-catch, the program would crash. Catch lets us recover and keep running."
