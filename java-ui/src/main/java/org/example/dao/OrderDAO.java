package org.example.dao;

import org.example.db.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for managing order entities in the database.
 * Supports CRUD operations and maintains references to clients, employees, payments, and deliveries.
 */
public class OrderDAO {

    /**
     * Data holder representing an order entity.
     */
    public static class Order {

        public int orderId;
        public Date orderDate;
        public String status;
        public Integer clientId;
        public Integer employeeId;
        public Integer paymentId;
        public Integer deliveryId;

        /**
         * Creates a new Order object.
         *
         * @param orderId    order identifier
         * @param orderDate  order date
         * @param status     order status
         * @param clientId   associated client ID (nullable)
         * @param employeeId associated employee ID (nullable)
         * @param paymentId  associated payment ID (nullable)
         * @param deliveryId associated delivery ID (nullable)
         */
        public Order(int orderId, Date orderDate, String status, Integer clientId, Integer employeeId,
                     Integer paymentId, Integer deliveryId) {
            this.orderId = orderId;
            this.orderDate = orderDate;
            this.status = status;
            this.clientId = clientId;
            this.employeeId = employeeId;
            this.paymentId = paymentId;
            this.deliveryId = deliveryId;
        }
    }

    /**
     * Adds a new order to the database.
     *
     * @param orderId    order identifier
     * @param orderDate  date of the order
     * @param status     order status
     * @param clientId   client ID associated with the order
     * @param employeeId employee ID associated with the order
     * @param paymentId  payment ID associated with the order
     * @param deliveryId delivery ID associated with the order
     */
    public void addOrder(int orderId, Date orderDate, String status, int clientId, int employeeId, int paymentId, int deliveryId) {
        String sql = "INSERT INTO orders VALUES (order_t(?, ?, ?, " +
                " (SELECT REF(c) FROM clients c WHERE VALUE(c).client_id=?), " +
                " (SELECT REF(e) FROM employees e WHERE VALUE(e).employee_id=?), " +
                " (SELECT REF(p) FROM payments p WHERE VALUE(p).payment_id=?), " +
                " (SELECT REF(d) FROM deliveries d WHERE VALUE(d).delivery_id=?)))";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, orderId);
            stmt.setDate(2, orderDate);
            stmt.setString(3, status);
            stmt.setInt(4, clientId);
            stmt.setInt(5, employeeId);
            stmt.setInt(6, paymentId);
            stmt.setInt(7, deliveryId);

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all orders from the database.
     *
     * @return list of all orders
     */
    public List<Order> getAllOrders() {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT VALUE(o).order_id, VALUE(o).order_date, VALUE(o).status, " +
                "CASE WHEN VALUE(o).client IS NOT NULL THEN VALUE(o).client.client_id ELSE NULL END, " +
                "CASE WHEN VALUE(o).employee IS NOT NULL THEN VALUE(o).employee.employee_id ELSE NULL END, " +
                "CASE WHEN VALUE(o).payment IS NOT NULL THEN VALUE(o).payment.payment_id ELSE NULL END, " +
                "CASE WHEN VALUE(o).delivery IS NOT NULL THEN VALUE(o).delivery.delivery_id ELSE NULL END " +
                "FROM orders o";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Order(
                        rs.getInt(1),
                        rs.getDate(2),
                        rs.getString(3),
                        rs.getObject(4) != null ? rs.getInt(4) : null,
                        rs.getObject(5) != null ? rs.getInt(5) : null,
                        rs.getObject(6) != null ? rs.getInt(6) : null,
                        rs.getObject(7) != null ? rs.getInt(7) : null
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * Updates an existing order in the database.
     *
     * @param orderId    order identifier
     * @param orderDate  new order date
     * @param status     new order status
     * @param clientId   new client ID
     * @param employeeId new employee ID
     * @param paymentId  new payment ID
     * @param deliveryId new delivery ID
     */
    public void updateOrder(int orderId, Date orderDate, String status, int clientId, int employeeId, int paymentId, int deliveryId) {
        String sql = "UPDATE orders o SET " +
                "VALUE(o).order_date=?, VALUE(o).status=?, " +
                "VALUE(o).client=(SELECT REF(c) FROM clients c WHERE VALUE(c).client_id=?), " +
                "VALUE(o).employee=(SELECT REF(e) FROM employees e WHERE VALUE(e).employee_id=?), " +
                "VALUE(o).payment=(SELECT REF(p) FROM payments p WHERE VALUE(p).payment_id=?), " +
                "VALUE(o).delivery=(SELECT REF(d) FROM deliveries d WHERE VALUE(d).delivery_id=?) " +
                "WHERE VALUE(o).order_id=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, orderDate);
            stmt.setString(2, status);
            stmt.setInt(3, clientId);
            stmt.setInt(4, employeeId);
            stmt.setInt(5, paymentId);
            stmt.setInt(6, deliveryId);
            stmt.setInt(7, orderId);

            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes an order from the database by its identifier.
     *
     * @param orderId order identifier
     */
    public void deleteOrder(int orderId) {
        String sql = "DELETE FROM orders o WHERE VALUE(o).order_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
