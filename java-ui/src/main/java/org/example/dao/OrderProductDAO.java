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
 * Data Access Object (DAO) for managing order-product associations stored as Oracle object types
 * (e.g. {@code order_product_t}) in the {@code order_products} object table.
 *
 * <p>Each row references an order and a product via Oracle {@code REF}s. This DAO resolves those references
 * by IDs before insert/update, and extracts IDs via {@code DEREF(...).<id>} when reading.</p>
 */
public class OrderProductDAO {

    private static final Logger LOGGER = Logger.getLogger(OrderProductDAO.class.getName());

    private static final String INSERT_SQL =
            "INSERT INTO order_products VALUES (order_product_t(?, ?, ?, ?, ?))";

    private static final String UPDATE_SQL =
            "UPDATE order_products op SET " +
                    "VALUE(op).product_ref=?, VALUE(op).quantity=?, VALUE(op).price_at_time=? " +
                    "WHERE VALUE(op).order_product_id=?";

    private static final String DELETE_SQL =
            "DELETE FROM order_products op WHERE VALUE(op).order_product_id=?";

    private static final String SELECT_ALL_SQL =
            "SELECT VALUE(op).order_product_id, " +
                    "DEREF(VALUE(op).order_ref).order_id, " +
                    "DEREF(VALUE(op).product_ref).product_id, " +
                    "VALUE(op).quantity, VALUE(op).price_at_time " +
                    "FROM order_products op";

    private static final String SELECT_BY_ORDER_SQL =
            "SELECT VALUE(op).order_product_id, " +
                    "DEREF(VALUE(op).order_ref).order_id, " +
                    "DEREF(VALUE(op).product_ref).product_id, " +
                    "VALUE(op).quantity, VALUE(op).price_at_time " +
                    "FROM order_products op " +
                    "WHERE VALUE(op).order_ref = (SELECT REF(o) FROM orders o WHERE VALUE(o).order_id=?)";

    private static final String PRODUCT_REF_SQL =
            "SELECT REF(p) FROM products p WHERE VALUE(p).product_id=?";

    private static final String ORDER_REF_SQL =
            "SELECT REF(o) FROM orders o WHERE VALUE(o).order_id=?";

    /**
     * Simple data holder representing an order-product row (object instance) read from {@code order_products}.
     */
    public static class OrderProduct {
        public final int orderProductId;
        public final int orderId;
        public final Integer productId;
        public final int quantity;
        public final double price;

        /**
         * Creates an order-product DTO.
         *
         * @param orderProductId association identifier
         * @param orderId        referenced order id
         * @param productId      referenced product id (may be {@code null} depending on query/data)
         * @param quantity       product quantity
         * @param price          price at the time of ordering
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
     * Resolves a required Oracle {@code REF} using the provided "SELECT REF(x) ..." query and identifier.
     *
     * @param refSql     SQL query that returns a single REF in the first column
     * @param id         identifier value to bind as parameter 1
     * @param entityName entity name used in error messages
     * @return Oracle REF object for the requested entity
     * @throws SQLException if the REF cannot be found or the query fails
     */
    private Object getRefOrThrow(String refSql, int id, String entityName) throws SQLException {
        PreparedStatement ps = DBConnection.prepareStatement(refSql);
        ps.clearParameters();
        ps.setInt(1, id);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getObject(1);
            }
        }
        throw new SQLException("No " + entityName + " found with ID=" + id + " (REF not found)");
    }

    /**
     * Inserts a new order-product association into {@code order_products}.
     *
     * @param orderProductId association identifier
     * @param orderId        referenced order id
     * @param productId      referenced product id
     * @param quantity       quantity
     * @param price          price at the time of ordering
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         if the database operation fails
     */
    public void addOrderProduct(int orderProductId, int orderId, int productId, int quantity, double price) {
        Validator.positiveInt(orderProductId, "OrderProduct ID");
        Validator.positiveInt(orderId, "Order ID");
        Validator.positiveInt(productId, "Product ID");
        Validator.positiveInt(quantity, "Quantity");
        Validator.positiveDouble(price, "Price");

        try {
            PreparedStatement ps = DBConnection.prepareStatement(INSERT_SQL);
            ps.clearParameters();

            ps.setInt(1, orderProductId);
            ps.setObject(2, getRefOrThrow(ORDER_REF_SQL, orderId, "order"));
            ps.setObject(3, getRefOrThrow(PRODUCT_REF_SQL, productId, "product"));
            ps.setInt(4, quantity);
            ps.setDouble(5, price);

            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while adding order_product with ID: " + orderProductId, e);
            throw new RuntimeException("Failed to add product to order: " + e.getMessage(), e);
        }
    }

    /**
     * Reads all order-product associations from {@code order_products}.
     *
     * @return list of all order-product associations (may be empty)
     * @throws RuntimeException if the database operation fails
     */
    public List<OrderProduct> getAll() {
        List<OrderProduct> list = new ArrayList<>();

        try {
            PreparedStatement ps = DBConnection.prepareStatement(SELECT_ALL_SQL);
            ps.clearParameters();

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new OrderProduct(
                            rs.getInt(1),
                            rs.getInt(2),
                            rs.getObject(3) != null ? rs.getInt(3) : null,
                            rs.getInt(4),
                            rs.getDouble(5)
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while fetching order_products", e);
            throw new RuntimeException("Failed to fetch products by orders: " + e.getMessage(), e);
        }

        return list;
    }

    /**
     * Reads all order-product associations for a specific order.
     *
     * @param orderId order identifier
     * @return list of products for the given order (may be empty)
     * @throws IllegalArgumentException if {@code orderId} is not positive
     * @throws RuntimeException         if the database operation fails
     */
    public List<OrderProduct> getByOrderId(int orderId) {
        Validator.positiveInt(orderId, "Order ID");

        List<OrderProduct> list = new ArrayList<>();

        try {
            PreparedStatement ps = DBConnection.prepareStatement(SELECT_BY_ORDER_SQL);
            ps.clearParameters();
            ps.setInt(1, orderId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new OrderProduct(
                            rs.getInt(1),
                            rs.getInt(2),
                            rs.getObject(3) != null ? rs.getInt(3) : null,
                            rs.getInt(4),
                            rs.getDouble(5)
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while fetching products for orderId=" + orderId, e);
            throw new RuntimeException("Failed to fetch products for order: " + e.getMessage(), e);
        }

        return list;
    }

    /**
     * Updates an existing order-product association by {@code order_product_id}.
     *
     * @param orderProductId association identifier
     * @param productId      new referenced product id
     * @param quantity       new quantity
     * @param price          new price at the time of ordering
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         if the database operation fails
     */
public void updateOrderProduct(int orderProductId, int productId, int quantity, double price) {
    Validator.positiveInt(orderProductId, "OrderProduct ID");
    Validator.positiveInt(productId, "Product ID");
    Validator.positiveInt(quantity, "Quantity");
    Validator.positiveDouble(price, "Price");

    String sql =
            "UPDATE order_products op " +
            "SET VALUE(op) = order_product_t(?, ?, ?, ?, ?) " +
            "WHERE VALUE(op).order_product_id = ?";

    try {
        PreparedStatement ps = DBConnection.prepareStatement(sql);
        ps.clearParameters();

        Object orderRef;
        PreparedStatement psRef = DBConnection.prepareStatement(
                "SELECT VALUE(op).order_ref FROM order_products op WHERE VALUE(op).order_product_id = ?"
        );
        psRef.clearParameters();
        psRef.setInt(1, orderProductId);

        try (ResultSet rs = psRef.executeQuery()) {
            if (!rs.next()) {
                throw new SQLException("No order_product found with ID=" + orderProductId);
            }
            orderRef = rs.getObject(1);
        }

        ps.setInt(1, orderProductId);
        ps.setObject(2, orderRef);
        ps.setObject(3, getRefOrThrow(PRODUCT_REF_SQL, productId, "product"));
        ps.setInt(4, quantity);
        ps.setDouble(5, price);

        ps.setInt(6, orderProductId);

        ps.executeUpdate();
    } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, "Error while updating order_product", e);
        throw new RuntimeException("Failed to update product in order: " + e.getMessage(), e);
    }
}

    /**
     * Deletes an order-product association by {@code order_product_id}.
     *
     * @param orderProductId association identifier
     * @throws IllegalArgumentException if {@code orderProductId} is not positive
     * @throws RuntimeException         if the database operation fails
     */
    public void deleteById(int orderProductId) {
        Validator.positiveInt(orderProductId, "OrderProduct ID");

        try {
            PreparedStatement ps = DBConnection.prepareStatement(DELETE_SQL);
            ps.clearParameters();
            ps.setInt(1, orderProductId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while deleting order_product with ID=" + orderProductId, e);
            throw new RuntimeException("Failed to delete product from order: " + e.getMessage(), e);
        }
    }
}