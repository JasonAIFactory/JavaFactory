# OOP 영어 답변 5주제

영어는 단순 SVO, 짧은 문장. 하나만 외우면 됨.

---

## 1. Inheritance (상속)

🧠 자식 클래스가 부모 클래스의 필드/메서드를 재사용 (`extends`).

🗣️ "Inheritance is when a child class reuses the parent class. We use `extends`. For example, Manager extends Employee. The child gets all the parent's fields and methods. We use it for code reuse and 'is-a' relationships."

---

## 2. Polymorphism (다형성)

🧠 부모 타입 변수가 자식 객체를 가리키고, 실제 호출은 자식 메서드.

🗣️ "Polymorphism is when a parent type holds a child object. At runtime, Java calls the real child method. So one variable can have many forms. We can add new types without changing old code."

---

## 3. Encapsulation (캡슐화)

🧠 데이터를 클래스 안에 숨기고, 메서드로만 접근.

🗣️ "Encapsulation hides the data inside the class. Fields are private. Outside code uses getter and setter. This protects the object from invalid state. It also lets us change the inside without breaking callers."

---

## 4. Abstraction (추상화)

🧠 무엇을 하는지만 보여주고, 어떻게 하는지는 숨김.

🗣️ "Abstraction shows only what an object does. It hides how. We use interface or abstract class for this. The caller does not need to know the inside details. We can change the implementation without changing the caller."

---

## 5. Interface vs Abstract Class

🧠 abstract class는 일부 구현 + 상태 가능, interface는 계약(contract)만.

🗣️ "Abstract class can have fields and shared code. Interface is only a contract. A class extends one abstract class. A class can implement many interfaces. I use abstract class for shared code, interface for capability."

---

## 보너스: Composition vs Inheritance

🧠 Composition은 다른 클래스를 필드로 가짐. Inheritance는 extends로 상속.

🗣️ "Composition is when a class has another class as a field. Inheritance is when a class extends another. Composition is more flexible. Inheritance is rigid. The general rule: prefer composition over inheritance."

---

## 🎯 5주제 한 줄 요약

| 주제 | 한 줄 정의 |
| --- | --- |
| Inheritance | reuse parent's fields/methods via `extends` |
| Polymorphism | one reference type, multiple runtime types |
| Encapsulation | hide state, expose through methods |
| Abstraction | hide complexity behind a contract |
| Interface vs Abstract | contract vs shared code |
