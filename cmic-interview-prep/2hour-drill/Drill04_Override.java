/*
 * Drill 4 — Method Overriding (polymorphism)
 *
 * 🎯 Goal:
 *   Animal class with sound() returning "generic".
 *   Dog, Cat, Cow extend Animal and override sound().
 *   In main, hold them all in Animal[] and print each sound.
 *
 * 🗣️ Say 10 times:
 *   "Overriding means the child class redefines the parent's method.
 *    The variable type is parent. The real object decides the method at runtime.
 *    This is polymorphism. To add a new type, I add a new subclass.
 *    No change to the loop. Open-closed principle."
 *   [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]
 *
 * Follow-up:
 *   Q: "What is the benefit?"
 *   A: "I can add a new animal class without changing existing code."
 *
 *   Q: "Difference from overloading?"
 *   A: "Overriding is runtime. Overloading is compile time. Overriding uses inheritance."
 */
public class Drill04_Override {

    // TODO: define Animal class with sound() returning "generic"


    // TODO: Dog, Cat, Cow extending Animal, each override sound()


    public static void main(String[] args) {
        // TODO: Animal[] arr = { new Dog(), new Cat(), new Cow() }
        //       loop and print each sound()


        // expected output:
        // woof
        // meow
        // moo
    }
}
