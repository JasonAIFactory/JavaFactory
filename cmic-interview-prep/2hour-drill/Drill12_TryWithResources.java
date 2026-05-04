/*
 * Drill 12 — try-with-resources
 *
 * 🎯 Goal:
 *   Open a file (or any AutoCloseable) inside try-with-resources.
 *   No explicit close needed. Java closes it for you.
 *
 *   For this drill, use a fake AutoCloseable to show the pattern.
 *
 * 🗣️ Say 10 times:
 *   "Try-with-resources auto-closes the resource at the end of the block.
 *    The class must implement AutoCloseable.
 *    No need for finally with close.
 *    It also handles exceptions correctly — no resource leak even if the body throws.
 *    I use it for JDBC Connection, Statement, ResultSet, file streams."
 *   [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]
 *
 * Follow-up:
 *   Q: "Why prefer it over finally?"
 *   A: "Less code. Safer. The compiler enforces close. No forgotten close()."
 *
 *   Q: "Multiple resources?"
 *   A: "Separate them with semicolons inside the parentheses. They close in reverse order."
 */
public class Drill12_TryWithResources {

    static class FakeResource implements AutoCloseable {
        String name;
        FakeResource(String n) { name = n; System.out.println("opened " + name); }
        @Override
        public void close() { System.out.println("closed " + name); }
        public void use() { System.out.println("using " + name); }
    }

    public static void main(String[] args) {
        // TODO: try (FakeResource r = new FakeResource("connection")) {
        //           r.use();
        //       }
        // — close() runs automatically


        // expected output:
        // opened connection
        // using connection
        // closed connection
    }
}
