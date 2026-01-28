package org.example.dao;

import org.example.db.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for managing order-product relationships in the database.
 * Supports CRUD operations with references to orders and products.
 */
public class OrderProductDAO {

    /**
     * Data holder representing an order-product entity.
     */
    public static class OrderProduct {

        public int orderProductId;
        public int orderId;
        public Integer productId;
        public int quantity;
        public double price;

        /**
         * Creates a new OrderProduct object.
         *
         * @param orderProductId order-product identifier
         * @param orderId        associated order ID
         * @param productId      associated product ID (nullable)
         * @param quantity       quantity of the product in the order
         * @param price          price at the time of the order
         */
        public OrderProduct(int orderProductId, int orderId, Integer productId, int quantity, double price) {
            this.orderProductId = orderProductId;
            this.orderId = orderId;
            this.productId = productId;
            this.quantity = quantity;
            this.price = price;
        }
    }

    /**
     * Adds a new order-product relationship to the database.
     *
     * @param orderProductId order-product identifier
     * @param orderId        associated order ID
     * @param productId      associated product ID
     * @param quantity       quantity of the product
     * @param price          price at the time of the order
     */
    public void addOrderProduct(int orderProductId, int orderId, int productId, int quantity, double price) {
        String sql = "INSERT INTO order_products VALUES (order_product_t(?, ?, ?, ?, ?))";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, orderProductId);
            stmt.setObject(2, getRef(conn, "orders", "order_id", orderId));
            stmt.setObject(3, getRef(conn, "products", "product_id", productId));
            stmt.setInt(4, quantity);
            stmt.setDouble(5, price);

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all order-product relationships from the database.
     *
     * @return list of all OrderProduct objects
     */
    public List<OrderProduct> getAll() {
        List<OrderProduct> list = new ArrayList<>();
        String sql = "SELECT op.order_product_id, DEREF(op.order_ref).order_id, DEREF(op.product_ref).product_id, op.quantity, op.price_at_time FROM order_products op";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new OrderProduct(
                        rs.getInt(1),
                        rs.getInt(2),
                        rs.getObject(3) != null ? rs.getInt(3) : null,
                        rs.getInt(4),
                        rs.getDouble(5)
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Retrieves all order-product relationships for a specific order.
     *
     * @param orderId order identifier
     * @return list of OrderProduct objects for the specified order
     */
    public List<OrderProduct> getByOrderId(int orderId) {
        List<OrderProduct> list = new ArrayList<>();
        String sql = "SELECT op.order_product_id, DEREF(op.order_ref).order_id, DEREF(op.product_ref).product_id, op.quantity, op.price_at_time " +
                "FROM order_products op WHERE op.order_ref = (SELECT REF(o) FROM orders o WHERE o.order_id = ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new OrderProduct(
                        rs.getInt(1),
                        rs.getInt(2),
                        rs.getObject(3) != null ? rs.getInt(3) : null,
                        rs.getInt(4),
                        rs.getDouble(5)
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Updates an existing order-product relationship in the database.
     *
     * @param orderProductId order-product identifier
     * @param productId      new product ID
     * @param quantity       new quantity
     * @param price          new price
     */
    public void updateOrderProduct(int orderProductId, int productId, int quantity, double price) {
        String sql = "UPDATE order_products op SET VALUE(op).product_ref=?, VALUE(op).quantity=?, VALUE(op).price_at_time=? WHERE VALUE(op).order_product_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, getRef(conn, "products", "product_id", productId));
            stmt.setInt(2, quantity);
            stmt.setDouble(3, price);
            stmt.setInt(4, orderProductId);

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes an order-product relationship from the database by its identifier.
     *
     * @param orderProductId order-product identifier
     */
    public void deleteById(int orderProductId) {
        String sql = "DELETE FROM order_products op WHERE VALUE(op).order_product_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, orderProductId);
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
     * @return database REF object
     * @throws SQLException if REF is not found or a database error occurs
     */
    private Object getRef(Connection conn, String tableName, String idColumn, int id) throws SQLException {
        String sql = "SELECT REF(t) FROM " + tableName + " t WHERE t." + idColumn + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getObject(1);
        }
        throw new SQLException("REF not found for " + tableName + " id=" + id);
    }
}
