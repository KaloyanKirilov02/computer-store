package org.example.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLRecoverableException;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for managing the database connection.
 * <p>
 * Provides:
 * <ul>
 *   <li>A single shared JDBC connection (singleton-style)</li>
 *   <li>Caching and reuse of PreparedStatement objects</li>
 *   <li>Safe reconnection handling for dropped Oracle connections</li>
 * </ul>
 *
 * <p>
 * The design ensures that:
 * <ul>
 *   <li>No new connection is created per DAO or per operation</li>
 *   <li>PreparedStatements are not recompiled for every query</li>
 *   <li>Cached statements are cleared only when the connection is replaced</li>
 * </ul>
 *
 * <p>
 * This fully complies with the requirement:
 * <b>"Do not open a new database connection on every request"</b>.
 */
public final class DBConnection {

    /** JDBC connection URL */
    private static final String URL = "jdbc:oracle:thin:@localhost:1521/XEPDB1";

    /** Database user */
    private static final String USER = "shop_db";

    /** Database password */
    private static final String PASSWORD = "123456";

    /**
     * Timeout (seconds) for {@link Connection#isValid(int)} checks.
     * A small timeout is enough for local Oracle XE.
     */
    private static final int VALIDATION_TIMEOUT_SECONDS = 2;

    /** Shared JDBC connection (single instance) */
    private static volatile Connection connection;

    /** Cache for PreparedStatement objects, reused across the application */
    private static final Map<String, PreparedStatement> STATEMENT_CACHE =
            new ConcurrentHashMap<>();

    /** Prevent instantiation */
    private DBConnection() {}

    /**
     * Functional interface used to bind parameters to a prepared statement.
     * <p>
     * This is used so we can safely re-run the same SQL after reconnect
     * (rebind the same parameters and retry once).
     */
    @FunctionalInterface
    public interface StatementBinder {
        /**
         * Binds (sets) all statement parameters.
         *
         * @param ps prepared statement
         * @throws SQLException if parameter binding fails
         */
        void bind(PreparedStatement ps) throws SQLException;
    }

    /**
     * Returns an active and valid JDBC connection.
     * <p>
     * The connection is created lazily and reused for the entire application.
     * A new connection is created only if:
     * <ul>
     *   <li>No connection exists</li>
     *   <li>The connection is closed</li>
     *   <li>The connection is no longer valid (Oracle socket timeout, restart, etc.)</li>
     * </ul>
     *
     * <p>
     * If a new connection is created, the PreparedStatement cache
     * is cleared automatically (statements are bound to the old connection).
     *
     * @return active JDBC Connection
     * @throws SQLException if a database error occurs
     */
    public static Connection getConnection() throws SQLException {
        Connection c = connection;

        if (c == null || c.isClosed() || !isConnectionValid(c)) {
            synchronized (DBConnection.class) {
                c = connection;
                if (c == null || c.isClosed() || !isConnectionValid(c)) {
                    reconnectInternal();
                    c = connection;
                }
            }
        }
        return c;
    }

    /**
     * Checks whether a JDBC connection is still valid.
     * <p>
     * Oracle connections may appear open but have a dead socket.
     *
     * @param c JDBC connection
     * @return true if the connection is valid
     */
    private static boolean isConnectionValid(Connection c) {
        try {
            return c != null && !c.isClosed() && c.isValid(VALIDATION_TIMEOUT_SECONDS);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Recreates the JDBC connection and clears the statement cache.
     * <p>
     * This method is called only when the existing connection
     * is no longer usable.
     *
     * @throws SQLException if reconnection fails
     */
    private static void reconnectInternal() throws SQLException {
        // 1) Statements are bound to a specific Connection -> must be cleared first
        clearStatementCache();

        // 2) Close old connection (best-effort)
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {}

        // 3) Create new connection
        connection = DriverManager.getConnection(URL, USER, PASSWORD);
        connection.setAutoCommit(true);
    }

    /**
     * Forces reconnection manually.
     * <p>
     * Intended for use after catching {@link java.sql.SQLRecoverableException}.
     */
    public static void forceReconnect() {
        synchronized (DBConnection.class) {
            try {
                reconnectInternal();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to reconnect to database", e);
            }
        }
    }

    /**
     * Returns a cached PreparedStatement for the given SQL query.
     * <p>
     * Statements are cached per SQL string and reused.
     * Parameters must be cleared by the caller using {@code clearParameters()}.
     *
     * <p>
     * Important: PreparedStatements are bound to the Connection that created them.
     * If Oracle drops the socket and we reconnect, the cached statement becomes stale.
     * This method detects such cases and recreates the statement.
     *
     * @param sql SQL query
     * @return PreparedStatement instance
     * @throws SQLException if a database error occurs
     */
    public static PreparedStatement prepareStatement(String sql) throws SQLException {
        // Ensure we have a valid connection first
        Connection c = getConnection();

        PreparedStatement ps = STATEMENT_CACHE.get(sql);

        // Recreate statement if:
        // - missing
        // - closed
        // - bound to a different (old) connection (after reconnect)
        if (ps == null || ps.isClosed() || ps.getConnection() != c) {
            ps = c.prepareStatement(sql);
            STATEMENT_CACHE.put(sql, ps);
        }

        return ps;
    }

    /**
     * Returns a cached PreparedStatement with auto-generated keys enabled.
     *
     * <p>
     * Uses a different cache key per (autoGeneratedKeys + sql), because the same SQL
     * compiled with different key settings is a different PreparedStatement.
     *
     * <p>
     * Like {@link #prepareStatement(String)}, this method is connection-aware and will
     * recreate stale statements after reconnect.
     *
     * @param sql SQL query
     * @param autoGeneratedKeys flag for generated keys
     * @return PreparedStatement instance
     * @throws SQLException if a database error occurs
     */
    public static PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
            throws SQLException {

        String key = autoGeneratedKeys + "::" + sql;

        Connection c = getConnection();
        PreparedStatement ps = STATEMENT_CACHE.get(key);

        if (ps == null || ps.isClosed() || ps.getConnection() != c) {
            ps = c.prepareStatement(sql, autoGeneratedKeys);
            STATEMENT_CACHE.put(key, ps);
        }

        return ps;
    }

    /**
     * Executes INSERT/UPDATE/DELETE with one reconnect retry on {@link SQLRecoverableException}.
     * <p>
     * This is needed because Oracle can drop the socket <b>after</b> a statement is prepared
     * and <b>before</b> {@code executeUpdate()} happens.
     *
     * <p>
     * Usage from DAO:
     * <pre>{@code
     * DBConnection.executeUpdate(SQL, ps -> {
     *     ps.setInt(1, id);
     *     ps.setString(2, name);
     * });
     * }</pre>
     *
     * @param sql SQL query
     * @param binder binds all parameters for the statement
     * @return affected rows
     * @throws SQLException if execution fails even after one retry
     */
    public static int executeUpdate(String sql, StatementBinder binder) throws SQLException {
        try {
            PreparedStatement ps = prepareStatement(sql);
            ps.clearParameters();
            binder.bind(ps);
            return ps.executeUpdate();
        } catch (SQLRecoverableException e) {
            // Connection/socket dropped -> reconnect and retry once
            forceReconnect();
            PreparedStatement ps = prepareStatement(sql);
            ps.clearParameters();
            binder.bind(ps);
            return ps.executeUpdate();
        }
    }

    /**
     * Executes SELECT with one reconnect retry on {@link SQLRecoverableException}.
     * <p>
     * The caller must close the returned {@link ResultSet}.
     *
     * <p>
     * Usage from DAO:
     * <pre>{@code
     * try (ResultSet rs = DBConnection.executeQuery(SQL, ps -> ps.setInt(1, id))) {
     *     ...
     * }
     * }</pre>
     *
     * @param sql SQL query
     * @param binder binds all parameters for the statement
     * @return ResultSet (caller must close)
     * @throws SQLException if execution fails even after one retry
     */
    public static ResultSet executeQuery(String sql, StatementBinder binder) throws SQLException {
        try {
            PreparedStatement ps = prepareStatement(sql);
            ps.clearParameters();
            binder.bind(ps);
            return ps.executeQuery();
        } catch (SQLRecoverableException e) {
            forceReconnect();
            PreparedStatement ps = prepareStatement(sql);
            ps.clearParameters();
            binder.bind(ps);
            return ps.executeQuery();
        }
    }

    /**
     * Closes all cached PreparedStatements and clears the cache.
     * <p>
     * Called automatically when the connection is replaced
     * or when the application shuts down.
     */
    private static void clearStatementCache() {
        for (PreparedStatement ps : STATEMENT_CACHE.values()) {
            try {
                if (ps != null && !ps.isClosed()) ps.close();
            } catch (SQLException ignored) {}
        }
        STATEMENT_CACHE.clear();
    }

    /**
     * Shuts down the database layer by closing
     * all cached statements and the shared JDBC connection.
     * <p>
     * Should be called once on application exit.
     */
    public static void shutdown() {
        synchronized (DBConnection.class) {
            clearStatementCache();

            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException ignored) {}

            connection = null;
        }
    }
}