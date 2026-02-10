package org.example.dao;

import org.example.db.DBConnection;
import org.example.util.Validator;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object (DAO) for managing deliveries stored as Oracle object types (e.g. {@code delivery_t})
 * in the {@code deliveries} object table.
 *
 * <p>Deliveries reference orders through {@code order_ref} (a REF to {@code orders}). This DAO resolves
 * order references by {@code order_id} before insert/update.</p>
 */
public class DeliveryDAO {

    private static final Logger LOGGER = Logger.getLogger(DeliveryDAO.class.getName());

    /**
     * Simple data holder representing a delivery row (object instance) read from the {@code deliveries} object table.
     */
    public static class Delivery {
        public final int id;
        public final String courier;
        public final String trackingNumber;
        public final String deliveryAddress;
        public final Integer orderId;

        /**
         * Creates a delivery DTO.
         *
         * @param id              delivery identifier
         * @param courier         courier name
         * @param trackingNumber  tracking number
         * @param deliveryAddress delivery address
         * @param orderId         referenced order id (may be {@code null} when not available in query)
         */
        public Delivery(int id, String courier, String trackingNumber, String deliveryAddress, Integer orderId) {
            this.id = id;
            this.courier = courier;
            this.trackingNumber = trackingNumber;
            this.deliveryAddress = deliveryAddress;
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
    private Object getOrderRefRequired(int orderId) throws SQLException {
        String sql = "SELECT REF(o) FROM orders o WHERE VALUE(o).order_id = ?";
        PreparedStatement ps = DBConnection.prepareStatement(sql);
        ps.clearParameters();
        ps.setInt(1, orderId);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getObject(1);
            }
        }
        throw new SQLException("No order found with order_id=" + orderId);
    }

    /**
     * Inserts a new delivery into the {@code deliveries} object table.
     *
     * @param deliveryId       delivery identifier
     * @param courier          courier name
     * @param trackingNumber   tracking number
     * @param deliveryAddress  delivery address
     * @param orderId          referenced order id (required)
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         if the database operation fails
     */
    public void addDelivery(int deliveryId, String courier, String trackingNumber, String deliveryAddress, int orderId) {
        Validator.validateDelivery(deliveryId, courier, trackingNumber, deliveryAddress, orderId);

        String sql = "INSERT INTO deliveries VALUES (delivery_t(?, ?, ?, ?, ?))";

        try {
            PreparedStatement ps = DBConnection.prepareStatement(sql);
            ps.clearParameters();

            ps.setInt(1, deliveryId);
            ps.setString(2, courier);
            ps.setString(3, trackingNumber);
            ps.setString(4, deliveryAddress);
            ps.setObject(5, getOrderRefRequired(orderId));

            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while adding delivery", e);
            throw new RuntimeException("Failed to add delivery: " + e.getMessage(), e);
        }
    }

    /**
     * Reads all deliveries from the {@code deliveries} object table.
     *
     * @return list of all deliveries (may be empty)
     * @throws RuntimeException if the database operation fails
     */
    public List<Delivery> getAllDeliveries() {
        List<Delivery> deliveries = new ArrayList<>();

        String sql =
                "SELECT VALUE(d).delivery_id, VALUE(d).courier, VALUE(d).tracking_number, " +
                        "VALUE(d).delivery_address, " +
                        "CASE WHEN VALUE(d).order_ref IS NOT NULL THEN VALUE(d).order_ref.order_id ELSE NULL END " +
                        "FROM deliveries d";

        try {
            PreparedStatement ps = DBConnection.prepareStatement(sql);
            ps.clearParameters();

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    deliveries.add(new Delivery(
                            rs.getInt(1),
                            rs.getString(2),
                            rs.getString(3),
                            rs.getString(4),
                            rs.getObject(5) != null ? rs.getInt(5) : null
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while fetching deliveries", e);
            throw new RuntimeException("Failed to fetch deliveries: " + e.getMessage(), e);
        }

        return deliveries;
    }

    /**
     * Updates an existing delivery in the {@code deliveries} object table by {@code delivery_id}.
     *
     * @param deliveryId       delivery identifier
     * @param courier          new courier name
     * @param trackingNumber   new tracking number
     * @param deliveryAddress  new delivery address
     * @param orderId          referenced order id (required)
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         if the database operation fails
     */
    public void updateDelivery(int deliveryId, String courier,
                           String trackingNumber, String deliveryAddress,
                           int orderId) {
    Validator.validateDelivery(deliveryId, courier, trackingNumber, deliveryAddress, orderId);

    String sql =
            "UPDATE deliveries d " +
            "SET VALUE(d) = delivery_t(?, ?, ?, ?, ?) " +
            "WHERE VALUE(d).delivery_id = ?";

    try {
        PreparedStatement ps = DBConnection.prepareStatement(sql);
        ps.clearParameters();

        ps.setInt(1, deliveryId);
        ps.setString(2, courier);
        ps.setString(3, trackingNumber);
        ps.setString(4, deliveryAddress);
        ps.setObject(5, getOrderRefRequired(orderId));

        ps.setInt(6, deliveryId);

        ps.executeUpdate();
    } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, "Error while updating delivery", e);
        throw new RuntimeException("Failed to update delivery: " + e.getMessage(), e);
    }
}
    /**
     * Deletes a delivery from the {@code deliveries} object table by {@code delivery_id}.
     *
     * @param deliveryId delivery identifier
     * @throws IllegalArgumentException if {@code deliveryId} is not positive
     * @throws RuntimeException         if the database operation fails
     */
    public void deleteDelivery(int deliveryId) {
        Validator.positiveInt(deliveryId, "Delivery ID");

        String sql = "DELETE FROM deliveries d WHERE VALUE(d).delivery_id = ?";

        try {
            PreparedStatement ps = DBConnection.prepareStatement(sql);
            ps.clearParameters();

            ps.setInt(1, deliveryId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while deleting delivery", e);
            throw new RuntimeException("Failed to delete delivery: " + e.getMessage(), e);
        }
    }
}