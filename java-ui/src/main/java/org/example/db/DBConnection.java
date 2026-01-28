package org.example.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utility class for managing database connections.
 * Provides methods to obtain and close connections to the Oracle database.
 */
public class DBConnection {

    private static final String URL = "jdbc:oracle:thin:@localhost:1521/XEPDB1";
    private static final String USER = "shop_db";
    private static final String PASSWORD = "123456";

    /**
     * Obtains a new connection to the database.
     *
     * @return a {@link Connection} object
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Closes the given database connection if it is not null and open.
     *
     * @param conn the {@link Connection} to close
     */
    public static void close(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
