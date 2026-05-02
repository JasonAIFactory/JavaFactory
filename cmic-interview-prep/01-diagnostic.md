# 자가진단 테스트 (10문제)

영어로 답변, 각 2~4문장. 모르면 "I don't know"라고 적기.

---

## Java Basics

**Q1.** What is the difference between `String` and `StringBuilder`? When would you use each one in a real backend application?

**Q2.** What is the difference between `==` and `.equals()` in Java? What happens if you override `equals()` but forget to override `hashCode()`?

**Q3.** Look at this code. What will it print, and why?
```java
List<Integer> list = new ArrayList<>();
list.add(1);
list.add(2);
list.add(3);
for (Integer i : list) {
    if (i == 2) list.remove(i);
}
System.out.println(list);
```

---

## OOP

**Q4.** Explain the difference between an `interface` and an `abstract class` in Java. Give one situation where you would choose one over the other.

**Q5.** What does "polymorphism" mean? Give a short example from any enterprise system you have worked on.

---

## SQL / PL-SQL

**Q6.** What is the difference between `INNER JOIN` and `LEFT JOIN`? Give a one-sentence business example.

**Q7.** What is the difference between `EXISTS` and `IN` in Oracle? When would `EXISTS` perform better?

**Q8.** Write a short PL/SQL block that:
- Loops through all rows in `PROJECT_COST` where `STATUS = 'PENDING'`
- Updates each row's status to `'APPROVED'`
- Handles exceptions and rolls back if anything fails

---

## JDBC

**Q9.** Why is `PreparedStatement` preferred over `Statement` in production code? Give two reasons.

---

## Debugging

**Q10.** A user reports: "Yesterday the cost report worked, today it shows wrong totals for one project." No code was deployed. Walk me through the first 4-5 steps you would take to investigate.
