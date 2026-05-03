public class Basic09_Inheritance_Solution {

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

    public static void main(String[] args) {
        Animal[] arr = { new Dog(), new Cat(), new Animal() };
        for (Animal a : arr) {
            System.out.println(a.sound());
        }
    }
}
