// =====================================================================
// REAL TOOL: ConcurrentHashMap.computeIfAbsent. Built into Java.
// Same idea as ManualCache, but:
//   - thread-safe (atomic check-and-fill per key)
//   - one line of cache code
//   - the production-default way to cache in Java
// Run:  java ConcurrentCache.java
// =====================================================================
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentCache {
    static final AtomicInteger calls = new AtomicInteger();
    static final ConcurrentHashMap<Integer, Map<String, Object>> cache = new ConcurrentHashMap<>();

    static Map<String, Object> getUser(int userId) {
        // computeIfAbsent runs the lambda ATOMICALLY per key.
        // If two threads call getUser(7) at the same time, the lambda runs
        // EXACTLY ONCE; the other thread waits and gets the same result.
        // This is the fix to ManualCache's race.
        return cache.computeIfAbsent(userId, id -> {
            calls.incrementAndGet();
            sleep(100);
            return Map.of("id", id, "name", "user_" + id);
        });
    }

    public static void main(String[] args) {
        long startNs = System.nanoTime();
        for (int rep = 0; rep < 10; rep++)
            for (int id = 0; id < 10; id++) getUser(id);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

        System.out.println("requests sent: 100");
        System.out.println("DB calls:      " + calls.get());
        System.out.println("wall clock:    " + elapsedMs + "ms");
        System.out.println("cache size:    " + cache.size());
        System.out.println("-> identical numbers to ManualCache, but thread-safe + 1 line.");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
