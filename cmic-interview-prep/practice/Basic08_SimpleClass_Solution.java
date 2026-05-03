public class Basic08_SimpleClass_Solution {

    static class Person {
        private String name;
        private int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public int getAge() { return age; }

        @Override
        public String toString() {
            return "Person[" + name + ", " + age + "]";
        }
    }

    public static void main(String[] args) {
        Person p = new Person("Alice", 30);
        System.out.println(p);   // Person[Alice, 30]
    }
}
