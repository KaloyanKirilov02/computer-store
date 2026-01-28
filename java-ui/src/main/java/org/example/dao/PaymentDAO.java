package org.example.dao;

import org.example.db.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for managing payment entities in the database.
 * Supports CRUD operations and maintains references to orders.
 */
public class PaymentDAO {

    /**
     * Adds a new payment to the database.
     *
     * @param paymentId payment identifier
     * @param amount    payment amount
     * @param payDate   payment date
     * @param status    payment status
     * @param orderId   associated order ID
     */
    public void addPayment(int paymentId, double amount, Date payDate, String status, int orderId) {
        String sql = "INSERT INTO payments VALUES (payment_t(?, ?, ?, ?, ?))";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, paymentId);
            stmt.setDouble(2, amount);
            stmt.setDate(3, payDate);
            stmt.setString(4, status);
            stmt.setObject(5, getRef(conn, "orders", "order_id", orderId));

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all payment IDs from the database.
     *
     * @return list of payment IDs
     */
    public List<Integer> getAllPayments() {
        List<Integer> payments = new ArrayList<>();
        String sql = "SELECT VALUE(p).payment_id FROM payments p";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                payments.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return payments;
    }

    /**
     * Retrieves full information for a specific payment, including the associated order ID.
     *
     * @param paymentId payment identifier
     * @return PaymentInfo object containing full payment details, or null if not found
     */
    public PaymentInfo getPaymentInfo(int paymentId) {
        String sql = "SELECT VALUE(p).amount, VALUE(p).pay_date, VALUE(p).status, " +
                "(SELECT VALUE(o).order_id FROM orders o WHERE REF(o) = VALUE(p).order_ref) " +
                "FROM payments p WHERE VALUE(p).payment_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, paymentId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                double amount = rs.getDouble(1);
                Date payDate = rs.getDate(2);
                String status = rs.getString(3);
                int orderId = rs.getInt(4);

                return new PaymentInfo(paymentId, amount, payDate, status, orderId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Updates an existing payment in the database.
     *
     * @param paymentId payment identifier
     * @param amount    new payment amount
     * @param payDate   new payment date
     * @param status    new payment status
     * @param orderId   new associated order ID
     */
    public void updatePayment(int paymentId, double amount, Date payDate, String status, int orderId) {
        String sql = "UPDATE payments p SET VALUE(p).amount=?, VALUE(p).pay_date=?, VALUE(p).status=?, VALUE(p).order_ref=? " +
                "WHERE VALUE(p).payment_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, amount);
            stmt.setDate(2, payDate);
            stmt.setString(3, status);
            stmt.setObject(4, getRef(conn, "orders", "order_id", orderId));
            stmt.setInt(5, paymentId);

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes a payment from the database by its identifier.
     *
     * @param paymentId payment identifier
     */
    public void deletePayment(int paymentId) {
        String sql = "DELETE FROM payments p WHERE VALUE(p).payment_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, paymentId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to retrieve a REF object for a given table and ID.
     *
     * @param conn       database connection
     * @param tableName  table name
     * @param idColumn   column name of the ID
     * @param id         value of the ID
     * @return database REF object, or null if not found
     * @throws SQLException if a database error occurs
     */
    private Object getRef(Connection conn, String tableName, String idColumn, int id) throws SQLException {
        String sql = "SELECT REF(t) FROM " + tableName + " t WHERE VALUE(t)." + idColumn + "=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getObject(1);
        }
        return null;
    }

    /**
     * Data holder for full payment information including associated order.
     */
    public static class PaymentInfo {
        public int paymentId;
        public double amount;
        public Date payDate;
        public String status;
        public int orderId;

        /**
         * Creates a new PaymentInfo object.
         *
         * @param paymentId payment identifier
         * @param amount    payment amount
         * @param payDate   payment date
         * @param status    payment status
         * @param orderId   associated order ID
         */
        public PaymentInfo(int paymentId, double amount, Date payDate, String status, int orderId) {
            this.paymentId = paymentId;
            this.amount = amount;
            this.payDate = payDate;
            this.status = status;
            this.orderId = orderId;
        }
    }
}
