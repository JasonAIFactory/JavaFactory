/*
 * Drill 9 — Singleton (Eager)
 *
 * 🎯 Goal:
 *   AppConfig class.
 *   Only one instance ever exists.
 *   getInstance() returns the same object every time.
 *
 * 🗣️ Say 10 times:
 *   "Singleton makes sure only one instance exists.
 *    I make the constructor private. So no one else can call new.
 *    I create the instance once as static final.
 *    A static getInstance method returns it.
 *    Eager init is simplest and thread-safe by the class loader."
 *   [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]
 *
 * Follow-up:
 *   Q: "Is it thread-safe?"
 *   A: "Yes. The JVM class loader creates the static field only once."
 *
 *   Q: "Lazy version?"
 *   A: "Use double-checked locking with volatile. Or use enum, which is simplest and safest."
 *
 *   Q: "Real example?"
 *   A: "Logger, configuration, and database connection pool."
 */
public class Drill09_Singleton {

    static class AppConfig {
        // TODO: private static final INSTANCE


        // TODO: private constructor


        // TODO: public static getInstance()


        // TODO: a sample field, e.g., String dbUrl
    }

    public static void main(String[] args) {
        // TODO: get the instance twice and check they are the same with ==


        // expected output:
        // true
    }
}
