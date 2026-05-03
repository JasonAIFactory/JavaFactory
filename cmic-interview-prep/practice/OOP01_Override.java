/*
 * OOP 1 — Method Overriding (Live Coding)
 *
 * Problem:
 *   Build an Animal type and three subclasses Dog, Cat, Cow that override sound().
 *   Then loop over an Animal[] and print each sound.
 *   This is the Animal/Dog/Cat polymorphism question — a CMiC live-coding favorite.
 *
 * Why Overriding is good (must explain in interview):
 *   1. Polymorphism — one variable, many runtime forms
 *   2. Open-closed principle — add new types without changing the loop
 *   3. Cleaner than `if/else` chains on a "type" string
 *
 * English script after coding:
 *   "I have a base class Animal with a method sound. Each subclass overrides it.
 *    In the loop, the variable type is Animal, but Java calls the real subclass
 *    method at runtime. This is polymorphism. To add a new animal, I just add
 *    a new subclass — no change to the loop. That is the open-closed principle."
 */
public class OOP01_Override {

    static class Animal {
        // TODO: define sound() returning "generic sound"
    }

    // TODO: class Dog extends Animal — override sound() to return "woof"


    // TODO: class Cat extends Animal — override sound() to return "meow"


    // TODO: class Cow extends Animal — override sound() to return "moo"


    public static void main(String[] args) {
        // TODO: create Animal[] arr containing one of each type


        // TODO: loop and print sound() for each


        System.out.println("--- expected ---");
        System.out.println("woof");
        System.out.println("meow");
        System.out.println("moo");
    }
}
