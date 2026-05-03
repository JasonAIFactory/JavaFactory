/*
 * OOP 3 — Singleton Pattern (Live Coding)
 *
 * Problem:
 *   Implement an AppConfig class so that only ONE instance ever exists.
 *   getInstance() always returns the same object.
 *
 * Why Singleton is good:
 *   1. Single point of access — global config, logger, connection pool
 *   2. Resource control — only one DB connection pool, one cache
 *   3. Lazy initialization possible — created only when first needed
 *
 * Watch out:
 *   - Bad singleton can hide dependencies and make testing hard
 *   - In multi-thread environment, naive lazy init is broken
 *
 * Three styles to know:
 *   A) Eager (simplest, thread-safe by class loader)
 *   B) Double-checked locking (lazy + thread-safe)
 *   C) Enum (cleanest, recommended in Effective Java)
 *
 * English script:
 *   "Singleton means only one instance of a class exists. I make the constructor
 *    private and provide a static getInstance method. Eager init is simplest and
 *    thread-safe. For lazy init in multi-thread code I use double-checked locking
 *    with volatile. The cleanest version is the enum singleton — it is thread-safe
 *    and prevents reflection attacks."
 */
public class OOP03_Singleton {

    static class AppConfig {
        // TODO (Style A — Eager): private static final AppConfig INSTANCE = new AppConfig();
        // TODO: private constructor
        // TODO: public static AppConfig getInstance()
        // TODO: a sample field like String dbUrl = "jdbc:oracle:thin:@host:1521/orcl";
    }

    public static void main(String[] args) {
        // TODO: get two references to AppConfig and print whether they are the same object
        // (use ==, expected true)


        System.out.println("--- expected ---");
        System.out.println("true");
    }
}
