/*
 * Basic 9 — Inheritance + Polymorphism
 *
 * Goal: extend a class and override a method.
 *
 * Tasks:
 *   1. The base class Animal has a method sound() returning "generic sound"
 *   2. Define Dog extends Animal, override sound() to return "woof"
 *   3. Define Cat extends Animal, override sound() to return "meow"
 *   4. In main, create Animal[] arr = { new Dog(), new Cat(), new Animal() }
 *      and print each sound()
 */
public class Basic09_Inheritance {

    static class Animal {
        String sound() {
            return "generic sound";
        }
    }

    // TODO: define class Dog extends Animal


    // TODO: define class Cat extends Animal


    public static void main(String[] args) {
        // TODO: create Animal[] with Dog, Cat, Animal


        // TODO: loop and print each sound()


        System.out.println("--- expected ---");
        System.out.println("woof");
        System.out.println("meow");
        System.out.println("generic sound");
    }
}
