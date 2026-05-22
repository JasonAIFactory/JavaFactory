// =====================================================================
// MANUAL CACHE. A HashMap we manage by hand. Same idea as Python's dict.
// Run:  java ManualCache.java
// =====================================================================
import java.util.HashMap;
import java.util.Map;

public class ManualCache {
    static int calls = 0;
    static final Map<Integer, Map<String, Object>> cache = new HashMap<>();  // the cache. just a Map.

    static Map<String, Object> getUser(int userId) {
        // 1) check cache first
        Map<String, Object> cached = cache.get(userId);
        if (cached != null) return cached;          // HIT

        // 2) MISS: slow path
        calls++;
        sleep(100);
        Map<String, Object> user = Map.of("id", userId, "name", "user_" + userId);

        // 3) remember it
        cache.put(userId, user);
        return user;
    }

    public static void main(String[] args) {
        long startNs = System.nanoTime();
        for (int rep = 0; rep < 10; rep++)
            for (int id = 0; id < 10; id++) getUser(id);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

        System.out.println("requests sent: 100");
        System.out.println("DB calls:      " + calls + "   <-- only the FIRST time per id");
        System.out.println("wall clock:    " + elapsedMs + "ms");
        System.out.println("-> 90 of 100 requests came from memory. a Map IS a cache.");
        System.out.println();
        System.out.println("WARNING: this code is NOT thread-safe. If two threads call");
        System.out.println("getUser(7) at the exact same instant, BOTH may MISS and BOTH");
        System.out.println("may run the slow path. See ConcurrentCache.java for the fix.");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
