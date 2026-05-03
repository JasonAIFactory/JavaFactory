/*
 * OOP 3 — SOLUTION
 *
 * Eager Singleton: simplest form, thread-safe by class loader.
 * Constructor is private so no one outside can create another instance.
 */
public class OOP03_Singleton_Solution {

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
        System.out.println(a == b);              // true
        System.out.println(a.getDbUrl());
    }
}
