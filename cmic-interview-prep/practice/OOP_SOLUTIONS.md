# OOP 라이브 코딩 정답 (스포일러)

⚠️ 직접 풀고 나서만 보기. 코딩보다 영어 설명이 더 중요!

---

## OOP 1 — Override

```java
public class OOP01_Override {

    static class Animal {
        String sound() { return "generic sound"; }
    }

    static class Dog extends Animal {
        @Override
        String sound() { return "woof"; }
    }

    static class Cat extends Animal {
        @Override
        String sound() { return "meow"; }
    }

    static class Cow extends Animal {
        @Override
        String sound() { return "moo"; }
    }

    public static void main(String[] args) {
        Animal[] arr = { new Dog(), new Cat(), new Cow() };
        for (Animal a : arr) {
            System.out.println(a.sound());
        }
    }
}
```

🗣️ "Each subclass overrides the sound method. The variable type is Animal but Java calls the real subclass method at runtime. To add a new animal, I just add a new subclass — no change to the loop. That is polymorphism and the open-closed principle."

---

## OOP 2 — Overload

```java
static class Calculator {
    int add(int a, int b) { return a + b; }
    double add(double a, double b) { return a + b; }
    int add(int a, int b, int c) { return a + b + c; }
}
```

🗣️ "Overloading means same method name with different parameters. The compiler picks the right one based on the arguments. It is decided at compile time."

---

## OOP 3 — Singleton

### Style A — Eager (가장 단순, 추천)

```java
static class AppConfig {
    private static final AppConfig INSTANCE = new AppConfig();
    private String dbUrl = "jdbc:oracle:thin:@host:1521/orcl";

    private AppConfig() {}   // 외부에서 new 못 하게

    public static AppConfig getInstance() { return INSTANCE; }
    public String getDbUrl() { return dbUrl; }
}

// main
AppConfig a = AppConfig.getInstance();
AppConfig b = AppConfig.getInstance();
System.out.println(a == b);   // true
```

### Style B — Double-checked Locking (lazy + thread-safe)

```java
static class AppConfig {
    private static volatile AppConfig instance;
    private AppConfig() {}

    public static AppConfig getInstance() {
        if (instance == null) {
            synchronized (AppConfig.class) {
                if (instance == null) {
                    instance = new AppConfig();
                }
            }
        }
        return instance;
    }
}
```

### Style C — Enum (가장 안전)

```java
enum AppConfig {
    INSTANCE;
    private final String dbUrl = "jdbc:oracle:...";
    public String getDbUrl() { return dbUrl; }
}
// 사용: AppConfig.INSTANCE.getDbUrl()
```

🗣️ "Singleton means only one instance. I make the constructor private and provide a static getInstance method. Eager init is the simplest. For lazy init in multi-thread code, I use double-checked locking with volatile. The cleanest version is enum."

---

## OOP 4 — Strategy

```java
public class OOP04_Strategy {

    interface PaymentStrategy {
        void pay(double amount);
    }

    static class CreditCard implements PaymentStrategy {
        public void pay(double a) { System.out.println("Paid " + a + " with credit card"); }
    }

    static class BankTransfer implements PaymentStrategy {
        public void pay(double a) { System.out.println("Paid " + a + " with bank transfer"); }
    }

    static class Crypto implements PaymentStrategy {
        public void pay(double a) { System.out.println("Paid " + a + " with crypto"); }
    }

    static class PaymentService {
        private final PaymentStrategy strategy;
        public PaymentService(PaymentStrategy s) { this.strategy = s; }
        public void checkout(double amount) { strategy.pay(amount); }
    }

    public static void main(String[] args) {
        PaymentStrategy[] strategies = { new CreditCard(), new BankTransfer(), new Crypto() };
        for (PaymentStrategy s : strategies) {
            new PaymentService(s).checkout(100);
        }
    }
}
```

🗣️ "Strategy pattern lets me change behavior at runtime. PaymentService works with the PaymentStrategy interface, not with a concrete class. To add a new payment method, I just add a new class implementing the interface — no change to PaymentService. That is the open-closed principle."

---

## OOP 5 — Encapsulation

```java
static class BankAccount {
    private double balance;

    public BankAccount(double initial) {
        if (initial < 0) throw new IllegalArgumentException("initial must be >= 0");
        this.balance = initial;
    }

    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("deposit must be > 0");
        balance += amount;
    }

    public void withdraw(double amount) {
        if (amount <= 0)        throw new IllegalArgumentException("withdraw must be > 0");
        if (amount > balance)   throw new IllegalArgumentException("insufficient balance");
        balance -= amount;
    }

    public double getBalance() { return balance; }
}
```

🗣️ "Balance is private, so callers cannot set it directly. Deposit and withdraw validate the input and throw if the rule is broken. This keeps the object always in a valid state. If the rules change, I only update the methods — callers stay the same."

---

## 🎯 5개 패턴 영어 한 줄

| 주제 | 한 줄 |
| --- | --- |
| Override | child redefines parent's method, runtime decision |
| Overload | same name, different params, compile-time decision |
| Singleton | one instance only — private constructor + static getter |
| Strategy | swap behavior at runtime via interface |
| Encapsulation | hide state, expose validated methods |
