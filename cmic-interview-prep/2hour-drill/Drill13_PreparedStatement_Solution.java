/*
 * Drill 13 — SOLUTION
 *
 * No real DB connection — just the correct JDBC pattern.
 * In production this connects to Oracle via DriverManager or DataSource.
 */

import java.sql.*;

public class Drill13_PreparedStatement_Solution {

    public static String findUser(Connection conn, int id) throws SQLException {
        String sql = "SELECT name FROM users WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("name");
                return null;
            }
        }
    }

    public static void main(String[] args) {
        // No real connection here.
        // The point is to write the method correctly with PreparedStatement,
        // ? placeholder, setInt binding, and try-with-resources.
        System.out.println("Pattern OK — see findUser()");
    }
}
