/*
 * Drill 4 — SOLUTION
 */
public class Drill04_Override_Solution {

    static class Animal {
        String sound() { return "generic"; }
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
