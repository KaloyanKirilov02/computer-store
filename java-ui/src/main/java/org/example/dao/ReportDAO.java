package org.example.dao;

import org.example.db.DBConnection;

import java.sql.*;
import java.util.Vector;

/**
 * Data Access Object for executing arbitrary SQL queries and retrieving results for reports.
 */
public class ReportDAO {

    /**
     * Executes a SQL query and returns the ResultSet.
     * The ResultSet is scroll-insensitive and read-only.
     *
     * @param sql SQL query string to execute
     * @return ResultSet of the query
     * @throws SQLException if a database access error occurs
     */
    public ResultSet executeQuery(String sql) throws SQLException {
        Connection conn = DBConnection.getConnection();
        Statement stmt = conn.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY
        );
        return stmt.executeQuery(sql);
    }

    /**
     * Retrieves the column names from a ResultSet.
     *
     * @param rs ResultSet to extract column names from
     * @return a Vector of column names
     * @throws SQLException if a database access error occurs
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
     * Retrieves all data from a ResultSet as a Vector of rows.
     * Each row is represented as a Vector of Objects.
     *
     * @param rs ResultSet to extract data from
     * @return a Vector of rows, where each row is a Vector of Objects
     * @throws SQLException if a database access error occurs
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
}
