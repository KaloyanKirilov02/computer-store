package org.example.ui;

import org.example.dao.OrderDAO;
import org.example.dao.OrderProductDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Date;
import java.util.List;

/**
 * Swing panel for managing orders and the products within each order.
 *
 * <p>The left table shows orders. Selecting an order loads its related order-products
 * into the right table.</p>
 */
public class OrderPanel extends JPanel {

    private final OrderDAO orderDAO = new OrderDAO();
    private final OrderProductDAO orderProductDAO = new OrderProductDAO();

    private final JTable orderTable;
    private final DefaultTableModel orderModel;

    private final JTable productTable;
    private final DefaultTableModel productModel;

    /**
     * Creates the order management panel and loads the initial data.
     */
    public OrderPanel() {
        setLayout(new BorderLayout(10, 10));

        orderModel = new DefaultTableModel(
                new Object[]{"Order ID", "Status", "Date", "Client ID", "Employee ID", "Payment ID", "Delivery ID"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        orderTable = new JTable(orderModel);
        orderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane orderScroll = new JScrollPane(orderTable);
        orderScroll.setBorder(BorderFactory.createTitledBorder("Orders"));

        productModel = new DefaultTableModel(
                new Object[]{"OrderProduct ID", "Product ID", "Quantity", "Price"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        productTable = new JTable(productModel);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane productScroll = new JScrollPane(productTable);
        productScroll.setBorder(BorderFactory.createTitledBorder("Products in Order"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, orderScroll, productScroll);
        splitPane.setDividerLocation(650);
        add(splitPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 4, 5, 5));

        JButton addOrderBtn = new JButton("Add Order");
        JButton updateOrderBtn = new JButton("Update Order");
        JButton deleteOrderBtn = new JButton("Delete Order");
        JButton refreshBtn = new JButton("Refresh Orders");

        JButton addProductBtn = new JButton("Add Product");
        JButton updateProductBtn = new JButton("Update Product");
        JButton deleteProductBtn = new JButton("Delete Product");

        buttonPanel.add(addOrderBtn);
        buttonPanel.add(updateOrderBtn);
        buttonPanel.add(deleteOrderBtn);
        buttonPanel.add(refreshBtn);
        buttonPanel.add(addProductBtn);
        buttonPanel.add(updateProductBtn);
        buttonPanel.add(deleteProductBtn);

        add(buttonPanel, BorderLayout.SOUTH);

        refreshBtn.addActionListener(e -> loadOrders());
        addOrderBtn.addActionListener(e -> addOrder());
        updateOrderBtn.addActionListener(e -> updateOrder());
        deleteOrderBtn.addActionListener(e -> deleteOrder());

        addProductBtn.addActionListener(e -> addProductToOrder());
        updateProductBtn.addActionListener(e -> updateProductInOrder());
        deleteProductBtn.addActionListener(e -> deleteProductFromOrder());

        orderTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadProductsForSelectedOrder();
            }
        });

        loadOrders();
    }

    /**
     * Loads all orders into the order table and clears the product table.
     */
    private void loadOrders() {
        orderModel.setRowCount(0);
        productModel.setRowCount(0);

        try {
            List<OrderDAO.Order> orders = orderDAO.getAllOrders();
            for (OrderDAO.Order o : orders) {
                orderModel.addRow(new Object[]{
                        o.orderId,
                        o.status,
                        o.orderDate,
                        o.clientId,
                        o.employeeId,
                        o.paymentId,
                        o.deliveryId
                });
            }
        } catch (RuntimeException ex) {
            showError("Error while loading orders: " + ex.getMessage());
        }
    }

    /**
     * Loads order-products for the currently selected order.
     */
    private void loadProductsForSelectedOrder() {
        productModel.setRowCount(0);

        int row = orderTable.getSelectedRow();
        if (row == -1) {
            return;
        }

        int orderId = (int) orderModel.getValueAt(row, 0);

        try {
            List<OrderProductDAO.OrderProduct> products = orderProductDAO.getByOrderId(orderId);
            for (OrderProductDAO.OrderProduct p : products) {
                productModel.addRow(new Object[]{
                        p.orderProductId,
                        p.productId,
                        p.quantity,
                        p.price
                });
            }
        } catch (RuntimeException ex) {
            showError("Error while loading products for order: " + ex.getMessage());
        }
    }

    /**
     * Prompts for fields and creates a new order.
     */
    private void addOrder() {
        try {
            Integer id = askInt("Order ID:");
            if (id == null) return;

            String status = askString("Status:");
            if (status == null) return;

            Date date = askDate("Order Date (YYYY-MM-DD):");
            if (date == null) return;

            Integer clientId = askInt("Client ID:");
            if (clientId == null) return;

            Integer employeeId = askInt("Employee ID:");
            if (employeeId == null) return;

            Integer paymentId = askIntOptional("Payment ID (optional):");
            Integer deliveryId = askIntOptional("Delivery ID (optional):");

            orderDAO.addOrder(id, date, status, clientId, employeeId, paymentId, deliveryId);
            loadOrders();
        } catch (RuntimeException ex) {
            showError("Error while adding order: " + ex.getMessage());
        }
    }

    /**
     * Updates the selected order after prompting for new values.
     */
    private void updateOrder() {
        int row = orderTable.getSelectedRow();
        if (row == -1) {
            showInfo("Select an order from the table first.");
            return;
        }

        try {
            int id = (int) orderModel.getValueAt(row, 0);

            String status = askString("Status:", orderModel.getValueAt(row, 1));
            if (status == null) return;

            Date date = askDate("Date (YYYY-MM-DD):", orderModel.getValueAt(row, 2));
            if (date == null) return;

            Integer clientId = askInt("Client ID:", orderModel.getValueAt(row, 3));
            if (clientId == null) return;

            Integer employeeId = askInt("Employee ID:", orderModel.getValueAt(row, 4));
            if (employeeId == null) return;

            Integer paymentId = askIntOptional("Payment ID (optional):", orderModel.getValueAt(row, 5));
            Integer deliveryId = askIntOptional("Delivery ID (optional):", orderModel.getValueAt(row, 6));

            orderDAO.updateOrder(id, date, status, clientId, employeeId, paymentId, deliveryId);
            loadOrders();
        } catch (RuntimeException ex) {
            showError("Error while updating order: " + ex.getMessage());
        }
    }

    /**
     * Deletes the selected order after confirmation.
     */
    private void deleteOrder() {
        int row = orderTable.getSelectedRow();
        if (row == -1) {
            showInfo("Select an order from the table first.");
            return;
        }

        int id = (int) orderModel.getValueAt(row, 0);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete order ID " + id + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            orderDAO.deleteOrder(id);
            loadOrders();
        } catch (RuntimeException ex) {
            showError("Error while deleting order: " + ex.getMessage());
        }
    }

    /**
     * Adds a product to the currently selected order.
     */
    private void addProductToOrder() {
        int orderRow = orderTable.getSelectedRow();
        if (orderRow == -1) {
            showInfo("Select an order first.");
            return;
        }

        int orderId = (int) orderModel.getValueAt(orderRow, 0);

        try {
            Integer orderProductId = askInt("OrderProduct ID:");
            if (orderProductId == null) return;

            Integer productId = askInt("Product ID:");
            if (productId == null) return;

            Integer quantity = askInt("Quantity:");
            if (quantity == null) return;

            Double price = askDouble("Price at time:");
            if (price == null) return;

            orderProductDAO.addOrderProduct(orderProductId, orderId, productId, quantity, price);
            loadProductsForSelectedOrder();
        } catch (RuntimeException ex) {
            showError("Error while adding product to order: " + ex.getMessage());
        }
    }

    /**
     * Updates the selected order-product entry.
     */
    private void updateProductInOrder() {
        int row = productTable.getSelectedRow();
        if (row == -1) {
            showInfo("Select a product from the table first.");
            return;
        }

        try {
            int orderProductId = (int) productModel.getValueAt(row, 0);

            Integer productId = askInt("Product ID:", productModel.getValueAt(row, 1));
            if (productId == null) return;

            Integer quantity = askInt("Quantity:", productModel.getValueAt(row, 2));
            if (quantity == null) return;

            Double price = askDouble("Price:", productModel.getValueAt(row, 3));
            if (price == null) return;

            orderProductDAO.updateOrderProduct(orderProductId, productId, quantity, price);
            loadProductsForSelectedOrder();
        } catch (RuntimeException ex) {
            showError("Error while updating product in order: " + ex.getMessage());
        }
    }

    /**
     * Deletes the selected order-product entry after confirmation.
     */
    private void deleteProductFromOrder() {
        int row = productTable.getSelectedRow();
        if (row == -1) {
            showInfo("Select a product from the table first.");
            return;
        }

        int id = (int) productModel.getValueAt(row, 0);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to remove this product from the order?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            orderProductDAO.deleteById(id);
            loadProductsForSelectedOrder();
        } catch (RuntimeException ex) {
            showError("Error while deleting product from order: " + ex.getMessage());
        }
    }

    /**
     * Shows an input dialog for an integer value.
     *
     * @param label message shown to the user
     * @return parsed integer value, or {@code null} if cancelled
     */
    private Integer askInt(String label) {
        String s = JOptionPane.showInputDialog(this, label);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) throw new RuntimeException(label + " cannot be empty.");
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new RuntimeException(label + " must be a number.");
        }
    }

    /**
     * Shows an input dialog for an integer value, pre-filled with a default value.
     *
     * @param label        message shown to the user
     * @param defaultValue default value shown in the input field
     * @return parsed integer value, or {@code null} if cancelled
     */
    private Integer askInt(String label, Object defaultValue) {
        String s = JOptionPane.showInputDialog(this, label, defaultValue);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) throw new RuntimeException(label + " cannot be empty.");
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new RuntimeException(label + " must be a number.");
        }
    }

    /**
     * Shows an input dialog for an optional integer value.
     *
     * @param label message shown to the user
     * @return parsed integer value, or {@code null} if cancelled or left blank
     */
    private Integer askIntOptional(String label) {
        String s = JOptionPane.showInputDialog(this, label);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new RuntimeException(label + " must be a number.");
        }
    }

    /**
     * Shows an input dialog for an optional integer value, pre-filled with a default value.
     *
     * @param label        message shown to the user
     * @param defaultValue default value shown in the input field
     * @return parsed integer value, or {@code null} if cancelled or left blank
     */
    private Integer askIntOptional(String label, Object defaultValue) {
        String s = JOptionPane.showInputDialog(this, label, defaultValue);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new RuntimeException(label + " must be a number.");
        }
    }

    /**
     * Shows an input dialog for a double value.
     *
     * @param label message shown to the user
     * @return parsed double value, or {@code null} if cancelled
     */
    private Double askDouble(String label) {
        String s = JOptionPane.showInputDialog(this, label);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) throw new RuntimeException(label + " cannot be empty.");
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new RuntimeException(label + " must be a number (double).");
        }
    }

    /**
     * Shows an input dialog for a double value, pre-filled with a default value.
     *
     * @param label        message shown to the user
     * @param defaultValue default value shown in the input field
     * @return parsed double value, or {@code null} if cancelled
     */
    private Double askDouble(String label, Object defaultValue) {
        String s = JOptionPane.showInputDialog(this, label, defaultValue);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) throw new RuntimeException(label + " cannot be empty.");
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new RuntimeException(label + " must be a number (double).");
        }
    }

    /**
     * Shows an input dialog for a non-empty string value.
     *
     * @param label message shown to the user
     * @return string value, or {@code null} if cancelled
     */
    private String askString(String label) {
        String s = JOptionPane.showInputDialog(this, label);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) throw new RuntimeException(label + " cannot be empty.");
        return s;
    }

    /**
     * Shows an input dialog for a non-empty string value, pre-filled with a default value.
     *
     * @param label        message shown to the user
     * @param defaultValue default value shown in the input field
     * @return string value, or {@code null} if cancelled
     */
    private String askString(String label, Object defaultValue) {
        String s = JOptionPane.showInputDialog(this, label, defaultValue);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) throw new RuntimeException(label + " cannot be empty.");
        return s;
    }

    /**
     * Shows an input dialog for a {@link Date} value in {@code YYYY-MM-DD} format.
     *
     * @param label message shown to the user
     * @return parsed date value, or {@code null} if cancelled
     */
    private Date askDate(String label) {
        String s = JOptionPane.showInputDialog(this, label);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) throw new RuntimeException(label + " cannot be empty.");
        try {
            return Date.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid date. Expected format is YYYY-MM-DD.");
        }
    }

    /**
     * Shows an input dialog for a {@link Date} value in {@code YYYY-MM-DD} format, pre-filled with a default value.
     *
     * @param label        message shown to the user
     * @param defaultValue default value shown in the input field
     * @return parsed date value, or {@code null} if cancelled
     */
    private Date askDate(String label, Object defaultValue) {
        String s = JOptionPane.showInputDialog(this, label, defaultValue);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) throw new RuntimeException(label + " cannot be empty.");
        try {
            return Date.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid date. Expected format is YYYY-MM-DD.");
        }
    }

    /**
     * Shows an error dialog with the provided message.
     *
     * @param msg message to display
     */
    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Shows an information dialog with the provided message.
     *
     * @param msg message to display
     */
    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }
}