// =============================================================================
// STAGE 4 - Stateful app behind an LB: the "lost session" bug, then sticky.
//
// Many apps keep per-user state IN MEMORY on the server (a login session).
// Behind an LB, request 1 lands on server A (login), request 2 lands on
// server B -> "who are you?" -> the user is logged out randomly.
//
// Fix shown here: SESSION AFFINITY (sticky). Route the same sessionId to
// the same server, using a hash. We use consistent hashing so that
// adding/removing a server moves as few sessions as possible.
//
// Run:  java S4_StickySessions.java
// Watch: error count with plain round robin vs sticky hashing.
//        Read the TRADEOFF note at the end - sticky is not free.
// =============================================================================

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class S4_StickySessions {

    // A server that remembers which sessions logged in HERE (in memory).
    static class AppServer {
        final int id;
        final Set<String> localSessions = ConcurrentHashMap.newKeySet();
        AppServer(int id) { this.id = id; }
        void login(String session) { localSessions.add(session); }
        boolean isLoggedIn(String session) { return localSessions.contains(session); }
    }

    public static void main(String[] args) throws Exception {
        List<AppServer> servers = List.of(new AppServer(0), new AppServer(1), new AppServer(2));
        int users = 100, requestsPerUser = 5;

        // ---- 1) Plain round robin: session is lost on most requests ----
        AtomicInteger rr = new AtomicInteger();
        int rrErrors = simulate(servers, users, requestsPerUser,
                (session) -> servers.get((rr.getAndIncrement() & Integer.MAX_VALUE) % servers.size()));
        System.out.println("round robin : 'logged out' errors = " + rrErrors
                + " / " + (users * (requestsPerUser - 1)));

        // ---- 2) Sticky via consistent hashing: same session -> same server ----
        servers.forEach(s -> s.localSessions.clear());
        ConsistentHashRing ring = new ConsistentHashRing(servers, 100);
        int stickyErrors = simulate(servers, users, requestsPerUser, ring::serverFor);
        System.out.println("sticky hash : 'logged out' errors = " + stickyErrors
                + " / " + (users * (requestsPerUser - 1)));

        System.out.println("\nWHAT IMPROVED: sticky routing keeps a session on its server -> no logout.");
        System.out.println("TRADEOFF (must say in an interview):");
        System.out.println("  - load can get UNEVEN (a 'hot' user/server).");
        System.out.println("  - if that server dies, those sessions are GONE anyway.");
        System.out.println("  - it makes deploys/scaling harder (you move users around).");
        System.out.println("BETTER in practice: keep servers STATELESS; put sessions in shared");
        System.out.println("  storage (Redis/JWT). Then any server can serve any request.");
        System.out.println("NEXT (S5): the LB itself is now the single point of failure / bottleneck.");
    }

    interface Router { AppServer route(String session); }

    static int simulate(List<AppServer> servers, int users, int rpu, Router router)
            throws InterruptedException {
        AtomicInteger errors = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch done = new CountDownLatch(users);
        for (int u = 0; u < users; u++) {
            final String session = "sess-" + u;
            pool.submit(() -> {
                try {
                    router.route(session).login(session);                 // request 1: login
                    for (int r = 1; r < rpu; r++) {                       // later requests
                        if (!router.route(session).isLoggedIn(session)) errors.incrementAndGet();
                    }
                } finally { done.countDown(); }
            });
        }
        done.await();
        pool.shutdown();
        return errors.get();
    }

    // Minimal consistent hash ring (preview of module 05). Virtual nodes for
    // smoother spread; a key maps to the first node clockwise.
    static class ConsistentHashRing {
        private final TreeMap<Integer, AppServer> ring = new TreeMap<>();
        ConsistentHashRing(List<AppServer> servers, int vnodes) {
            for (AppServer s : servers)
                for (int v = 0; v < vnodes; v++)
                    ring.put(hash("srv-" + s.id + "-" + v), s);
        }
        AppServer serverFor(String key) {
            int h = hash(key);
            Map.Entry<Integer, AppServer> e = ring.ceilingEntry(h);
            return (e == null ? ring.firstEntry() : e).getValue();
        }
        // Bit-mixing finalizer (MurmurHash3 fmix32) so short, similar keys
        // spread across the ring instead of clustering on one node.
        static int hash(String s) {
            int h = s.hashCode();
            h ^= h >>> 16; h *= 0x85ebca6b;
            h ^= h >>> 13; h *= 0xc2b2ae35;
            h ^= h >>> 16;
            return h & Integer.MAX_VALUE;
        }
    }
}
