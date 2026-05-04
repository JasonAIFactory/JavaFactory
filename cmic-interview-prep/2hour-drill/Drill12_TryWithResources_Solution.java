/*
 * Drill 12 — SOLUTION
 */
public class Drill12_TryWithResources_Solution {

    static class FakeResource implements AutoCloseable {
        String name;
        FakeResource(String n) { name = n; System.out.println("opened " + name); }
        @Override
        public void close() { System.out.println("closed " + name); }
        public void use() { System.out.println("using " + name); }
    }

    public static void main(String[] args) {
        try (FakeResource r = new FakeResource("connection")) {
            r.use();
        }
        // close() runs automatically at end of try block
    }
}
