/*
 * Drill 13 — PreparedStatement (JDBC)
 *
 * 🎯 Goal:
 *   Write a method findUser(Connection, int) that returns a user name by id.
 *   Use PreparedStatement with ? placeholder, NOT string concatenation.
 *   Use try-with-resources for PreparedStatement and ResultSet.
 *
 *   We mock Connection with a comment so the file compiles, but you SAY out loud
 *   what each line does — that is the point.
 *
 * 🗣️ Say 10 times:
 *   "PreparedStatement uses question marks as placeholders.
 *    I bind values with setInt or setString.
 *    The driver escapes the values, so SQL injection is blocked.
 *    The database also caches the parsed plan, so the query is faster on repeat.
 *    I always wrap PreparedStatement and ResultSet in try-with-resources."
 *   [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]
 *
 * Follow-up:
 *   Q: "Why not Statement?"
 *   A: "Statement uses string concatenation. SQL injection risk. Plan is not cached.
 *       I never use Statement in production code."
 *
 *   Q: "How does it stop SQL injection?"
 *   A: "The driver sends the SQL and the values separately. The values are never
 *       parsed as SQL, so 'or 1=1' just becomes a literal string."
 */

import java.sql.*;

public class Drill13_PreparedStatement {

    public static String findUser(Connection conn, int id) throws SQLException {
        // TODO: write the JDBC code
        //   String sql = "SELECT name FROM users WHERE id = ?";
        //   try (PreparedStatement ps = conn.prepareStatement(sql)) {
        //       ps.setInt(1, id);
        //       try (ResultSet rs = ps.executeQuery()) {
        //           if (rs.next()) return rs.getString("name");
        //           return null;
        //       }
        //   }
        return null;
    }

    public static void main(String[] args) {
        // No real DB here. Just write the method correctly.
        // Practice: SAY each line out loud as you type.
    }
}
