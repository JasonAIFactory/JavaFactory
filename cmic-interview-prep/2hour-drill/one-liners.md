# 30개 한 줄 영어 + 팔로우업 (Hour 2)

각 항목 2분: 읽고 → 닫고 → **소리내어 10번** 말하기.
팔로우업도 한 번씩.

---

## 1. Java 8

🗣️ **"Java 8 introduced lambda expressions, Stream API, Optional, and functional interfaces."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "Which feature do you use most?"
- A: "Stream and lambda. Every day."

---

## 2. Stream

🗣️ **"Stream is a data processing pipeline, not a data structure."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "Lazy or eager?"
- A: "Lazy. Nothing runs until terminal operation like collect."

---

## 3. StringBuilder

🗣️ **"StringBuilder is faster but not thread-safe."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "Why faster than String plus?"
- A: "String is immutable. Each plus makes a new object. StringBuilder reuses one buffer."

---

## 4. StringBuffer

🗣️ **"StringBuffer is synchronized and thread-safe at the method level, but slower."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "When use StringBuffer?"
- A: "Only multi-thread. In modern code, very rare."

---

## 5. ==

🗣️ **"== compares references."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "What about for int?"
- A: "For primitive, == compares value. For object, it compares the address."

---

## 6. equals

🗣️ **"equals compares values."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "Default behavior?"
- A: "Default equals checks reference. We override for content match."

---

## 7. null safety

🗣️ **"If null is possible, I use Objects.equals or constant.equals(variable)."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "Why?"
- A: "If variable is null, calling .equals on it throws NullPointerException."

---

## 8. boolean

🗣️ **"boolean is primitive and cannot be null."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "Default value?"
- A: "false."

---

## 9. Boolean

🗣️ **"Boolean is a wrapper class and can be null."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "When use Boolean over boolean?"
- A: "When I need null, like in a HashMap value, or to mean 'unknown'."

---

## 10. Class

🗣️ **"A class is a blueprint for creating objects."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "Where in memory?"
- A: "Class definition is in metaspace. Object instances are in heap."

---

## 11. Object

🗣️ **"An object is an instance of a class."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "How is it created?"
- A: "With the new keyword. Java allocates heap memory and runs the constructor."

---

## 12. Constructor

🗣️ **"A constructor initializes an object."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "Can constructor be overloaded?"
- A: "Yes. Different parameters. Caller picks which one."

---

## 13. Inheritance

🗣️ **"Inheritance allows code reuse and supports polymorphism."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "Risk of inheritance?"
- A: "Tight coupling. Change in parent breaks all children. Prefer composition when possible."

---

## 14. Interface

🗣️ **"An interface defines a contract and reduces coupling."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "Difference from abstract class?"
- A: "Abstract class can have state. Interface has no fields. Class can implement many interfaces."

---

## 15. Polymorphism

🗣️ **"Polymorphism means the same method call can behave differently depending on the actual object."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "Real example?"
- A: "Animal type holds Dog or Cat. Calling sound gives different result."

---

## 16. Overriding

🗣️ **"Overriding means a child class changes a parent method."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "When decided?"
- A: "Runtime. Java looks at the real object."

---

## 17. Overloading

🗣️ **"Overloading means the same method name with different parameters."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "When decided?"
- A: "Compile time. The compiler picks based on the argument types."

---

## 18. ArrayList

🗣️ **"ArrayList is good for index-based access."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "Cost of get(i)?"
- A: "O(1). It is a real array inside."

---

## 19. HashMap

🗣️ **"HashMap is not thread-safe."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "What can break?"
- A: "Multi-thread put can corrupt the buckets. In old Java even infinite loop."

---

## 20. ConcurrentHashMap

🗣️ **"ConcurrentHashMap is thread-safe and designed for concurrent access."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "How is it different from Hashtable?"
- A: "Hashtable locks the whole map. ConcurrentHashMap locks one bucket. Much faster."

---

## 21. Thread-safe

🗣️ **"Thread-safe means code works correctly when multiple threads access it at the same time."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "How to make code thread-safe?"
- A: "Use synchronized, or atomic classes, or thread-safe collections, or immutable data."

---

## 22. Singleton

🗣️ **"Singleton ensures only one instance of a class."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "How to write it?"
- A: "Private constructor. Static field for the instance. Static getInstance method."

---

## 23. Strategy Pattern

🗣️ **"Strategy Pattern makes different behaviors interchangeable."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "Real example?"
- A: "Comparator for sorting. Or different cost calculators in an ERP."

---

## 24. REST API

🗣️ **"REST API uses resource-based URLs and HTTP methods."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "Stateful or stateless?"
- A: "Stateless. Each request carries all needed info."

---

## 25. POST / PUT / PATCH

🗣️ **"POST creates, PUT replaces, PATCH partially updates."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "Which is idempotent?"
- A: "PUT and PATCH. POST is not — every call makes a new resource."

---

## 26. Function vs Procedure

🗣️ **"Function returns a value; procedure performs an operation."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "Which can be used in SELECT?"
- A: "Function. Procedure cannot be called inside a SELECT."

---

## 27. Deadlock

🗣️ **"A deadlock happens when threads wait for each other forever."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "How to prevent?"
- A: "Always acquire locks in the same order. Or use tryLock with timeout."

---

## 28. Transaction

🗣️ **"A transaction should either fully succeed or fully fail."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "What does ACID mean?"
- A: "Atomicity, Consistency, Isolation, Durability. The four properties of a transaction."

---

## 29. Index

🗣️ **"An index improves read performance but adds write overhead."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "Why slower writes?"
- A: "Every insert and update must also update the index."

---

## 30. PreparedStatement

🗣️ **"PreparedStatement helps prevent SQL injection."**
[ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]

**Follow-up:**
- Q: "How does it stop injection?"
- A: "It binds values separately. The driver does not parse them as SQL."

- Q: "Other benefits?"
- A: "The database caches the parsed plan. Queries are faster on repeat."

---

# ✅ 끝

30개 다 10번씩 = 약 60분.
중간에 끊지 말고 한 번에 가세요.

---

# 🚨 면접 직전 1분 — 가장 자주 나오는 4가지

크게 한 번씩 더 말하고 들어가세요:

1. **"Double equals checks reference. Equals method checks value."**
2. **"StringBuilder is fast but not thread-safe. StringBuffer is synchronized."**
3. **"Override is runtime. Overload is compile time."**
4. **"PreparedStatement prevents SQL injection by binding values separately."**
