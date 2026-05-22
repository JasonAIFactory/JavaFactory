// =====================================================================
// NO CACHE. Every request hits the "database".
// Run:  java NoCache.java
// =====================================================================
import java.util.Map;

public class NoCache {
    static int calls = 0;

    static Map<String, Object> getUser(int userId) {
        calls++;
        sleep(100);                                // pretend DB: 100ms per call
        return Map.of("id", userId, "name", "user_" + userId);
    }

    public static void main(String[] args) {
        long startNs = System.nanoTime();
        // 10 distinct users, each requested 10 times = 100 requests.
        for (int rep = 0; rep < 10; rep++)
            for (int id = 0; id < 10; id++) getUser(id);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

        System.out.println("requests sent: 100");
        System.out.println("DB calls:      " + calls);
        System.out.println("wall clock:    " + elapsedMs + "ms");
        System.out.println("-> EVERY request hit the database. expensive.");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
