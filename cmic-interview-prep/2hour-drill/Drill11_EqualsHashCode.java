/*
 * Drill 11 — equals + hashCode (always together)
 *
 * 🎯 Goal:
 *   Person class with name and age.
 *   Override equals so two Persons are equal when both fields match.
 *   Override hashCode using both fields.
 *   Test by putting two equal Persons in a HashSet and check size is 1.
 *
 * 🗣️ Say 10 times:
 *   "If I override equals, I must also override hashCode.
 *    HashSet and HashMap use hashCode to find the bucket.
 *    Then they use equals to confirm.
 *    If equals says they are equal but hashCode is different, the collection breaks."
 *   [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]
 *
 * Follow-up:
 *   Q: "What if I forget hashCode?"
 *   A: "The HashSet may store two equal objects. The HashMap may not find the key."
 *
 *   Q: "What is a good hashCode?"
 *   A: "Use Objects.hash(field1, field2). Combines fields with a known formula."
 */

import java.util.*;

public class Drill11_EqualsHashCode {

    static class Person {
        String name;
        int age;
        Person(String n, int a) { name = n; age = a; }

        // TODO: override equals


        // TODO: override hashCode (use Objects.hash)
    }

    public static void main(String[] args) {
        Person p1 = new Person("Alice", 30);
        Person p2 = new Person("Alice", 30);

        System.out.println(p1.equals(p2));   // true after override

        Set<Person> set = new HashSet<>();
        set.add(p1);
        set.add(p2);
        System.out.println(set.size());      // 1 if both equals and hashCode are correct
    }
}
