import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import java.sql.*;
import java.util.*;

// ===========================================================
// You custom imports here...

// ===========================================================

public class Actions {

    // =====================================================================================================================================

    // ================= ROUTING =================
    public void goToPage(ActionEvent event) {
        Button b = (Button) event.getSource();
        Main.go(b.getUserData().toString());
    }

    // =====================================================
    // SQLITE QUERY FUNCTION
    // =====================================================
    /*
    |--------------------------------------------------------------------------
    | transactionalSQLiteQuery
    |--------------------------------------------------------------------------
    |
    | Usage:
    |
    | SELECT:
    |   Object result = transactionalSQLiteQuery(
    |       "SELECT * FROM users WHERE id = ?",
    |       Arrays.asList(1)
    |   );
    |
    | INSERT / UPDATE / DELETE:
    |   Object result = transactionalSQLiteQuery(
    |       "INSERT INTO users (name) VALUES (?)",
    |       Arrays.asList("John")
    |   );
    |
    | Result:
    |   SELECT → List<Map<String, Object>>
    |   Others → Boolean (true = success)
    |
    */
    public static Object transactionalSQLiteQuery(String query, List<Object> params) {

        String url = "jdbc:sqlite:database.db";
        boolean isSelect = query.trim().toUpperCase().startsWith("SELECT");

        try (Connection conn = DriverManager.getConnection(url)) {

            conn.setAutoCommit(false);

            PreparedStatement stmt = conn.prepareStatement(query);

            if (params != null) {
                for (int i = 0; i < params.size(); i++) {
                    stmt.setObject(i + 1, params.get(i));
                }
            }

            // ================= SELECT =================
            if (isSelect) {

                ResultSet rs = stmt.executeQuery();
                List<Map<String, Object>> result = new ArrayList<>();

                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= cols; i++) {
                        row.put(meta.getColumnName(i), rs.getObject(i));
                    }
                    result.add(row);
                }

                stmt.close();
                conn.commit();
                return result;
            }

            // ================= INSERT / UPDATE / DELETE / DDL =================
            stmt.executeUpdate();

            stmt.close();
            conn.commit();
            return true;

        } catch (Exception e) {
            return "SQLite error: " + e.getMessage();
        }
    }

    // =====================================================
    // MYSQL QUERY FUNCTION
    // =====================================================
    /*
    |--------------------------------------------------------------------------
    | transactionalMySQLQuery
    |--------------------------------------------------------------------------
    |
    | Usage:
    |
    | SELECT:
    |   Object result = transactionalMySQLQuery(
    |       "SELECT * FROM users WHERE username = ?",
    |       Arrays.asList("john")
    |   );
    |
    | INSERT:
    |   Object result = transactionalMySQLQuery(
    |       "INSERT INTO users (name, email) VALUES (?, ?)",
    |       Arrays.asList("John", "john@email.com")
    |   );
    |
    | Result:
    |   SELECT → List<Map<String, Object>>
    |   Others → Boolean
    |
    */
    public static Object transactionalMySQLQuery(String query, List<Object> params) {

        String url = "jdbc:mysql://localhost:3306/mydb";
        String user = "root";
        String pass = "password";

        boolean isSelect = query.trim().toUpperCase().startsWith("SELECT");

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {

            conn.setAutoCommit(false);

            PreparedStatement stmt = conn.prepareStatement(query);

            if (params != null) {
                for (int i = 0; i < params.size(); i++) {
                    stmt.setObject(i + 1, params.get(i));
                }
            }

            // ================= SELECT =================
            if (isSelect) {

                ResultSet rs = stmt.executeQuery();
                List<Map<String, Object>> result = new ArrayList<>();

                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= cols; i++) {
                        row.put(meta.getColumnName(i), rs.getObject(i));
                    }
                    result.add(row);
                }

                stmt.close();
                conn.commit();
                return result;
            }

            // ================= INSERT / UPDATE / DELETE / DDL =================
            stmt.executeUpdate();

            stmt.close();
            conn.commit();
            return true;

        } catch (Exception e) {
            return "MySQL error: " + e.getMessage();
        }
    }

    // =====================================================================================================================================

    // ---------------------------------------
    // Put your custom logic below
    // ---------------------------------------
}