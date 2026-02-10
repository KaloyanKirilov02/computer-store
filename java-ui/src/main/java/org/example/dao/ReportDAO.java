package org.example.dao;

import org.example.db.DBConnection;
import org.example.util.Validator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Vector;

/**
 * Data Access Object (DAO) for executing ad-hoc read-only reports.
 *
 * <p>This DAO only allows {@code SELECT} queries. It intentionally does not use the global prepared-statement cache
 * (i.e. {@link DBConnection#prepareStatement(String)}) because report queries are ad-hoc and caching them could
 * unnecessarily grow the cache.</p>
 *
 * <p><b>Important:</b> The shared connection from {@link DBConnection#getConnection()} is reused and is
 * <b>NOT</b> closed here. Closing it would break the application because other DAOs reuse the same connection.</p>
 */
public class ReportDAO {

    /**
     * Executes a single ad-hoc {@code SELECT} query and returns the result as a table-like structure.
     *
     * <p>Security note: This method is designed for a local/admin report console. It does not support parameters,
     * so do not expose it to untrusted input.</p>
     *
     * @param sql SQL query text (must start with {@code SELECT})
     * @return report result containing column names and row data
     * @throws IllegalArgumentException if the query is empty or not a {@code SELECT}
     * @throws RuntimeException         if the database operation fails
     */
    public ReportResult executeQuery(String sql) {
        Validator.notEmpty(sql, "SQL query");

        // normalize input
        String trimmed = sql.trim();
        String normalized = trimmed.toLowerCase();

        // allow only SELECT
        if (!normalized.startsWith("select")) {
            throw new IllegalArgumentException("Only SELECT queries are allowed.");
        }

        // Avoid multi-statement / trailing semicolon issues in JDBC
        // (SQL Developer allows ';', JDBC often doesn't).
        if (trimmed.contains(";")) {
            throw new IllegalArgumentException("Remove ';' from the query. Only a single SELECT statement is allowed.");
        }

        try {
            // IMPORTANT: do NOT close this connection here (it's a shared singleton)
            Connection conn = DBConnection.getConnection();

            // Do NOT use DBConnection.prepareStatement(sql) here (ad-hoc queries must not be cached)
            try (PreparedStatement ps = conn.prepareStatement(trimmed)) {
                ps.setFetchSize(200);

                try (ResultSet rs = ps.executeQuery()) {
                    Vector<String> columnNames = getColumnNames(rs);
                    Vector<Vector<Object>> data = getData(rs);
                    return new ReportResult(columnNames, data);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error while executing report query: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts column names from a {@link ResultSet}.
     *
     * @param rs result set
     * @return column names in display order
     * @throws SQLException if metadata cannot be read
     */
    public static Vector<String> getColumnNames(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        Vector<String> cols = new Vector<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            cols.add(meta.getColumnName(i));
        }
        return cols;
    }

    /**
     * Reads all rows from a {@link ResultSet} into a nested vector structure.
     *
     * @param rs result set
     * @return rows, where each row is a vector of column values
     * @throws SQLException if reading data fails
     */
    public static Vector<Vector<Object>> getData(ResultSet rs) throws SQLException {
        Vector<Vector<Object>> data = new Vector<>();
        ResultSetMetaData meta = rs.getMetaData();

        while (rs.next()) {
            Vector<Object> row = new Vector<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                row.add(rs.getObject(i));
            }
            data.add(row);
        }
        return data;
    }

    /**
     * Container for a report result set: column names + row data.
     */
    public static class ReportResult {
        public final Vector<String> columnNames;
        public final Vector<Vector<Object>> data;

        /**
         * Creates a report result.
         *
         * @param columnNames column names in display order
         * @param data        rows of data
         */
        public ReportResult(Vector<String> columnNames, Vector<Vector<Object>> data) {
            this.columnNames = columnNames;
            this.data = data;
        }
    }
}