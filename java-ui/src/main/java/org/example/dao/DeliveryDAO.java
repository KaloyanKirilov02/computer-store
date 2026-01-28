package org.example.dao;

import org.example.db.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for managing delivery entities in the database.
 * Supports CRUD operations and maintains references to orders.
 */
public class DeliveryDAO {

    /**
     * Data holder representing a delivery entity.
     */
    public static class Delivery {

        public int id;
        public String courier;
        public String trackingNumber;
        public String deliveryAddress;
        public Integer orderId;

        /**
         * Creates a new Delivery object.
         *
         * @param id              delivery identifier
         * @param courier         name of the courier
         * @param trackingNumber  tracking number of the delivery
         * @param deliveryAddress delivery address
         * @param orderId         associated order ID (nullable)
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
     * Adds a new delivery to the database, including a REF to the associated order.
     *
     * @param deliveryId      delivery identifier
     * @param courier         courier name
     * @param trackingNumber  tracking number
     * @param deliveryAddress delivery address
     * @param orderId         associated order ID
     */
    public void addDelivery(int deliveryId, String courier, String trackingNumber, String deliveryAddress, int orderId) {
        String sql = "INSERT INTO deliveries VALUES (delivery_t(?, ?, ?, ?, ?))";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, deliveryId);
            stmt.setString(2, courier);
            stmt.setString(3, trackingNumber);
            stmt.setString(4, deliveryAddress);

            try (PreparedStatement orderStmt = conn.prepareStatement(
                    "SELECT REF(o) FROM orders o WHERE VALUE(o).order_id=?")) {
                orderStmt.setInt(1, orderId);
                ResultSet rs = orderStmt.executeQuery();
                if (rs.next()) {
                    stmt.setObject(5, rs.getObject(1));
                } else {
                    stmt.setObject(5, null);
                }
            }

            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all deliveries from the database.
     *
     * @return list of all deliveries
     */
    public List<Delivery> getAllDeliveries() {
        List<Delivery> deliveries = new ArrayList<>();
        String sql = "SELECT " +
                "VALUE(d).delivery_id, " +
                "VALUE(d).courier, " +
                "VALUE(d).tracking_number, " +
                "VALUE(d).delivery_address, " +
                "CASE WHEN VALUE(d).order_ref IS NOT NULL THEN VALUE(d).order_ref.order_id ELSE NULL END " +
                "FROM deliveries d";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                deliveries.add(new Delivery(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getObject(5) != null ? rs.getInt(5) : null
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return deliveries;
    }

    /**
     * Updates an existing delivery in the database, including its order reference.
     *
     * @param deliveryId      delivery identifier
     * @param courier         new courier name
     * @param trackingNumber  new tracking number
     * @param deliveryAddress new delivery address
     * @param orderId         new associated order ID
     */
    public void updateDelivery(int deliveryId, String courier, String trackingNumber, String deliveryAddress, int orderId) {
        String sql = "UPDATE deliveries d SET " +
                "VALUE(d).courier=?, " +
                "VALUE(d).tracking_number=?, " +
                "VALUE(d).delivery_address=?, " +
                "VALUE(d).order_ref=? " +
                "WHERE VALUE(d).delivery_id=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, courier);
            stmt.setString(2, trackingNumber);
            stmt.setString(3, deliveryAddress);

            try (PreparedStatement orderStmt = conn.prepareStatement(
                    "SELECT REF(o) FROM orders o WHERE VALUE(o).order_id=?")) {
                orderStmt.setInt(1, orderId);
                ResultSet rs = orderStmt.executeQuery();
                if (rs.next()) {
                    stmt.setObject(4, rs.getObject(1));
                } else {
                    stmt.setObject(4, null);
                }
            }

            stmt.setInt(5, deliveryId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes a delivery from the database by its identifier.
     *
     * @param deliveryId delivery identifier
     */
    public void deleteDelivery(int deliveryId) {
        String sql = "DELETE FROM deliveries d WHERE VALUE(d).delivery_id=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, deliveryId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
