/*
 * Drill 9 — SOLUTION (Eager)
 */
public class Drill09_Singleton_Solution {

    static class AppConfig {
        private static final AppConfig INSTANCE = new AppConfig();
        private final String dbUrl = "jdbc:oracle:thin:@host:1521/orcl";

        private AppConfig() {}

        public static AppConfig getInstance() { return INSTANCE; }
        public String getDbUrl() { return dbUrl; }
    }

    public static void main(String[] args) {
        AppConfig a = AppConfig.getInstance();
        AppConfig b = AppConfig.getInstance();
        System.out.println(a == b);      // true
    }
}
