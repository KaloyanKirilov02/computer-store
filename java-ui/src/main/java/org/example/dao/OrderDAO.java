package org.example.dao;

import org.example.db.DBConnection;
import org.example.util.Validator;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO for orders stored as Oracle object type ORDER_T in the ORDERS object table.
 *
 * Orders reference other entities (client, employee, payment, delivery) through Oracle REFs.
 * When binding NULL for REF attributes, Oracle JDBC requires typed NULL:
 * setNull(paramIndex, Types.REF, "<TYPE_NAME>").
 */
public class OrderDAO {

    private static final Logger LOGGER = Logger.getLogger(OrderDAO.class.getName());

    private static final String CLIENT_T = "CLIENT_T";
    private static final String EMPLOYEE_T = "EMPLOYEE_T";
    private static final String PAYMENT_T = "PAYMENT_T";
    private static final String DELIVERY_T = "DELIVERY_T";

    private static final String INSERT_SQL =
            "INSERT INTO orders VALUES (order_t(?, ?, ?, ?, ?, ?, ?))";

    private static final String DELETE_SQL =
            "DELETE FROM orders o WHERE VALUE(o).order_id=?";

    private static final String SELECT_ALL_SQL =
            "SELECT VALUE(o).order_id, VALUE(o).order_date, VALUE(o).status, " +
                    "CASE WHEN VALUE(o).client   IS NOT NULL THEN DEREF(VALUE(o).client).client_id     ELSE NULL END, " +
                    "CASE WHEN VALUE(o).employee IS NOT NULL THEN DEREF(VALUE(o).employee).employee_id ELSE NULL END, " +
                    "CASE WHEN VALUE(o).payment  IS NOT NULL THEN DEREF(VALUE(o).payment).payment_id   ELSE NULL END, " +
                    "CASE WHEN VALUE(o).delivery IS NOT NULL THEN DEREF(VALUE(o).delivery).delivery_id ELSE NULL END " +
                    "FROM orders o";

    private static final String UPDATE_SQL =
            "UPDATE orders o " +
                    "SET VALUE(o) = order_t(?, ?, ?, ?, ?, ?, ?) " +
                    "WHERE VALUE(o).order_id = ?";

    private static final String CLIENT_REF_SQL =
            "SELECT REF(c) FROM clients c WHERE VALUE(c).client_id=?";

    private static final String EMPLOYEE_REF_SQL =
            "SELECT REF(e) FROM employees e WHERE VALUE(e).employee_id=?";

    private static final String PAYMENT_REF_SQL =
            "SELECT REF(p) FROM payments p WHERE VALUE(p).payment_id=?";

    private static final String DELIVERY_REF_SQL =
            "SELECT REF(d) FROM deliveries d WHERE VALUE(d).delivery_id=?";

    public static class Order {
        public final int orderId;
        public final Date orderDate;
        public final String status;
        public final Integer clientId;
        public final Integer employeeId;
        public final Integer paymentId;
        public final Integer deliveryId;

        public Order(int orderId, Date orderDate, String status,
                     Integer clientId, Integer employeeId, Integer paymentId, Integer deliveryId) {
            this.orderId = orderId;
            this.orderDate = orderDate;
            this.status = status;
            this.clientId = clientId;
            this.employeeId = employeeId;
            this.paymentId = paymentId;
            this.deliveryId = deliveryId;
        }
    }

    private Object getRefOrThrow(String refSql, int id, String entityName) throws SQLException {
        PreparedStatement ps = DBConnection.prepareStatement(refSql);
        ps.clearParameters();
        ps.setInt(1, id);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getObject(1);
        }
        throw new SQLException("No " + entityName + " found with ID=" + id + " (REF not found)");
    }

    private Object getRefOrNull(String refSql, Integer id, String entityName) throws SQLException {
        if (id == null) return null;
        return getRefOrThrow(refSql, id, entityName);
    }

    public void addOrder(int orderId, Date orderDate, String status,
                         int clientId, int employeeId, Integer paymentId, Integer deliveryId) {

        Validator.validateOrderCreate(orderId, orderDate, status, clientId, employeeId);

        try {
            PreparedStatement ps = DBConnection.prepareStatement(INSERT_SQL);
            ps.clearParameters();

            ps.setInt(1, orderId);
            ps.setDate(2, orderDate);
            ps.setString(3, status);

            ps.setObject(4, getRefOrThrow(CLIENT_REF_SQL, clientId, "client"));
            ps.setObject(5, getRefOrThrow(EMPLOYEE_REF_SQL, employeeId, "employee"));

            Object paymentRef = getRefOrNull(PAYMENT_REF_SQL, paymentId, "payment");
            if (paymentRef != null) {
                ps.setObject(6, paymentRef);
            } else {
                ps.setNull(6, Types.REF, PAYMENT_T);
            }

            Object deliveryRef = getRefOrNull(DELIVERY_REF_SQL, deliveryId, "delivery");
            if (deliveryRef != null) {
                ps.setObject(7, deliveryRef);
            } else {
                ps.setNull(7, Types.REF, DELIVERY_T);
            }

            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while adding order with ID: " + orderId, e);
            throw new RuntimeException("Failed to add order: " + e.getMessage(), e);
        }
    }

    public List<Order> getAllOrders() {
        List<Order> list = new ArrayList<>();

        try {
            PreparedStatement ps = DBConnection.prepareStatement(SELECT_ALL_SQL);
            ps.clearParameters();

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer cId = (rs.getObject(4) != null) ? rs.getInt(4) : null;
                    Integer eId = (rs.getObject(5) != null) ? rs.getInt(5) : null;
                    Integer pId = (rs.getObject(6) != null) ? rs.getInt(6) : null;
                    Integer dId = (rs.getObject(7) != null) ? rs.getInt(7) : null;

                    list.add(new Order(
                            rs.getInt(1),
                            rs.getDate(2),
                            rs.getString(3),
                            cId, eId, pId, dId
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while fetching orders", e);
            throw new RuntimeException("Failed to fetch orders: " + e.getMessage(), e);
        }

        return list;
    }

    public void updateOrder(int orderId, Date orderDate, String status,
                            int clientId, int employeeId, Integer paymentId, Integer deliveryId) {

        Validator.validateOrderUpdate(orderId, orderDate, status, clientId, employeeId, paymentId, deliveryId);

        try {
            PreparedStatement ps = DBConnection.prepareStatement(UPDATE_SQL);
            ps.clearParameters();

            ps.setInt(1, orderId);
            ps.setDate(2, orderDate);
            ps.setString(3, status);

            ps.setObject(4, getRefOrThrow(CLIENT_REF_SQL, clientId, "client"));
            ps.setObject(5, getRefOrThrow(EMPLOYEE_REF_SQL, employeeId, "employee"));

            Object paymentRef = getRefOrNull(PAYMENT_REF_SQL, paymentId, "payment");
            if (paymentRef != null) {
                ps.setObject(6, paymentRef);
            } else {
                ps.setNull(6, Types.REF, PAYMENT_T);
            }

            Object deliveryRef = getRefOrNull(DELIVERY_REF_SQL, deliveryId, "delivery");
            if (deliveryRef != null) {
                ps.setObject(7, deliveryRef);
            } else {
                ps.setNull(7, Types.REF, DELIVERY_T);
            }

            ps.setInt(8, orderId);

            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while updating order", e);
            throw new RuntimeException("Failed to update order: " + e.getMessage(), e);
        }
    }

    public void deleteOrder(int orderId) {
        Validator.positiveInt(orderId, "Order ID");

        try {
            PreparedStatement ps = DBConnection.prepareStatement(DELETE_SQL);
            ps.clearParameters();
            ps.setInt(1, orderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while deleting order with ID: " + orderId, e);
            throw new RuntimeException("Failed to delete order: " + e.getMessage(), e);
        }
    }
}