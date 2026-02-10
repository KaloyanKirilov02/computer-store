package org.example.dao;

import org.example.db.DBConnection;
import org.example.util.Validator;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object (DAO) for managing payments stored as Oracle object types (e.g. {@code payment_t})
 * in the {@code payments} object table.
 *
 * <p>Payments reference orders through {@code order_ref} (an Oracle {@code REF} to {@code orders}). This DAO
 * resolves the order reference by {@code order_id} before insert/update, and extracts {@code order_id}
 * via {@code DEREF(...).order_id} when reading.</p>
 */
public class PaymentDAO {

    private static final Logger LOGGER = Logger.getLogger(PaymentDAO.class.getName());

    private static final String INSERT_SQL =
            "INSERT INTO payments VALUES (payment_t(?, ?, ?, ?, ?))";

    private static final String UPDATE_SQL =
            "UPDATE payments p " +
                    "SET VALUE(p).amount=?, VALUE(p).pay_date=?, VALUE(p).status=?, VALUE(p).order_ref=? " +
                    "WHERE VALUE(p).payment_id=?";

    private static final String DELETE_SQL =
            "DELETE FROM payments p WHERE VALUE(p).payment_id=?";

    private static final String ORDER_REF_SQL =
            "SELECT REF(o) FROM orders o WHERE VALUE(o).order_id=?";

    private static final String SELECT_ALL_INFO_SQL =
            "SELECT VALUE(p).payment_id, VALUE(p).amount, VALUE(p).pay_date, VALUE(p).status, " +
                    "CASE WHEN VALUE(p).order_ref IS NOT NULL THEN DEREF(VALUE(p).order_ref).order_id ELSE NULL END " +
                    "FROM payments p";

    private static final String SELECT_BY_ID_SQL =
            "SELECT VALUE(p).amount, VALUE(p).pay_date, VALUE(p).status, " +
                    "CASE WHEN VALUE(p).order_ref IS NOT NULL THEN DEREF(VALUE(p).order_ref).order_id ELSE NULL END " +
                    "FROM payments p WHERE VALUE(p).payment_id=?";

    /**
     * Simple data holder representing a payment row (object instance) read from the {@code payments} object table.
     */
    public static class PaymentInfo {
        public final int paymentId;
        public final double amount;
        public final Date payDate;
        public final String status;
        public final Integer orderId;

        /**
         * Creates a payment DTO.
         *
         * @param paymentId payment identifier
         * @param amount    payment amount
         * @param payDate   payment date
         * @param status    payment status
         * @param orderId   referenced order id (may be {@code null} if the record has no reference)
         */
        public PaymentInfo(int paymentId, double amount, Date payDate, String status, Integer orderId) {
            this.paymentId = paymentId;
            this.amount = amount;
            this.payDate = payDate;
            this.status = status;
            this.orderId = orderId;
        }
    }

    /**
     * Resolves and returns {@code REF(o)} for the given {@code order_id}.
     *
     * @param orderId order identifier that must exist
     * @return Oracle REF to the order object row
     * @throws SQLException if the order does not exist or the query fails
     */
    private Object getOrderRefOrThrow(int orderId) throws SQLException {
        PreparedStatement ps = DBConnection.prepareStatement(ORDER_REF_SQL);
        ps.clearParameters();
        ps.setInt(1, orderId);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getObject(1);
            }
        }
        throw new SQLException("No order found with ID=" + orderId + " (REF not found)");
    }

    /**
     * Inserts a new payment into the {@code payments} object table.
     *
     * @param paymentId payment identifier
     * @param amount    payment amount
     * @param payDate   payment date
     * @param status    payment status
     * @param orderId   referenced order id (required)
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         if the database operation fails
     */
    public void addPayment(int paymentId, double amount, Date payDate, String status, int orderId) {
        Validator.validatePayment(paymentId, amount, payDate, status, orderId);

        try {
            PreparedStatement ps = DBConnection.prepareStatement(INSERT_SQL);
            ps.clearParameters();

            ps.setInt(1, paymentId);
            ps.setDouble(2, amount);
            ps.setDate(3, payDate);
            ps.setString(4, status);
            ps.setObject(5, getOrderRefOrThrow(orderId));

            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while adding payment with ID: " + paymentId, e);
            throw new RuntimeException("Failed to add payment: " + e.getMessage(), e);
        }
    }

    /**
     * Reads all payments from the {@code payments} object table.
     *
     * @return list of all payments (may be empty)
     * @throws RuntimeException if the database operation fails
     */
    public List<PaymentInfo> getAllPaymentsInfo() {
        List<PaymentInfo> list = new ArrayList<>();

        try {
            PreparedStatement ps = DBConnection.prepareStatement(SELECT_ALL_INFO_SQL);
            ps.clearParameters();

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PaymentInfo(
                            rs.getInt(1),
                            rs.getDouble(2),
                            rs.getDate(3),
                            rs.getString(4),
                            rs.getObject(5) != null ? rs.getInt(5) : null
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while fetching payments", e);
            throw new RuntimeException("Failed to fetch payments: " + e.getMessage(), e);
        }

        return list;
    }

    /**
     * Reads a single payment by {@code payment_id}.
     *
     * @param paymentId payment identifier
     * @return payment info, or {@code null} if not found
     * @throws IllegalArgumentException if {@code paymentId} is not positive
     * @throws RuntimeException         if the database operation fails
     */
    public PaymentInfo getPaymentInfo(int paymentId) {
        Validator.positiveInt(paymentId, "Payment ID");

        try {
            PreparedStatement ps = DBConnection.prepareStatement(SELECT_BY_ID_SQL);
            ps.clearParameters();
            ps.setInt(1, paymentId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PaymentInfo(
                            paymentId,
                            rs.getDouble(1),
                            rs.getDate(2),
                            rs.getString(3),
                            rs.getObject(4) != null ? rs.getInt(4) : null
                    );
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while fetching payment with ID: " + paymentId, e);
            throw new RuntimeException("Failed to fetch payment: " + e.getMessage(), e);
        }

        return null;
    }

    /**
     * Updates an existing payment in the {@code payments} object table by {@code payment_id}.
     *
     * @param paymentId payment identifier
     * @param amount    new payment amount
     * @param payDate   new payment date
     * @param status    new payment status
     * @param orderId   referenced order id (required)
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         if the database operation fails
     */
    public void updatePayment(int paymentId, double amount, Date payDate, String status, int orderId) {
    Validator.validatePayment(paymentId, amount, payDate, status, orderId);

    String sql =
            "UPDATE payments p " +
            "SET VALUE(p) = payment_t(?, ?, ?, ?, ?) " +
            "WHERE VALUE(p).payment_id = ?";

    try {
        PreparedStatement ps = DBConnection.prepareStatement(sql);
        ps.clearParameters();

        ps.setInt(1, paymentId);
        ps.setDouble(2, amount);
        ps.setDate(3, payDate);
        ps.setString(4, status);
        ps.setObject(5, getOrderRefOrThrow(orderId));

        ps.setInt(6, paymentId);

        ps.executeUpdate();
    } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, "Error while updating payment", e);
        throw new RuntimeException("Failed to update payment: " + e.getMessage(), e);
    }
}

    /**
     * Deletes a payment from the {@code payments} object table by {@code payment_id}.
     *
     * @param paymentId payment identifier
     * @throws IllegalArgumentException if {@code paymentId} is not positive
     * @throws RuntimeException         if the database operation fails
     */
    public void deletePayment(int paymentId) {
        Validator.positiveInt(paymentId, "Payment ID");

        try {
            PreparedStatement ps = DBConnection.prepareStatement(DELETE_SQL);
            ps.clearParameters();
            ps.setInt(1, paymentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while deleting payment with ID: " + paymentId, e);
            throw new RuntimeException("Failed to delete payment: " + e.getMessage(), e);
        }
    }
}