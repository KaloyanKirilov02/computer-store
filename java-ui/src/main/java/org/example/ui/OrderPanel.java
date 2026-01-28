package org.example.ui;

import org.example.dao.OrderDAO;
import org.example.dao.OrderProductDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Date;
import java.util.List;

/**
 * JPanel that manages orders and their associated products.
 * Provides tables and CRUD operations for both orders and order products.
 */
public class OrderPanel extends JPanel {

    private final OrderDAO orderDAO = new OrderDAO();
    private final OrderProductDAO orderProductDAO = new OrderProductDAO();

    private final JTable orderTable;
    private final DefaultTableModel orderModel;

    private final JTable productTable;
    private final DefaultTableModel productModel;

    /**
     * Constructs an OrderPanel with tables, buttons, and listeners for CRUD operations.
     */
    public OrderPanel() {
        setLayout(new BorderLayout(10, 10));

        orderModel = new DefaultTableModel(
                new Object[]{"Order ID", "Status", "Date", "Client ID", "Employee ID", "Payment ID", "Delivery ID"}, 0
        );
        orderTable = new JTable(orderModel);
        orderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane orderScroll = new JScrollPane(orderTable);
        orderScroll.setBorder(BorderFactory.createTitledBorder("Orders"));

        productModel = new DefaultTableModel(
                new Object[]{"OrderProduct ID", "Product ID", "Quantity", "Price"}, 0
        );
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
            if (!e.getValueIsAdjusting()) loadProductsForSelectedOrder();
        });

        loadOrders();
    }

    /**
     * Loads all orders from the database and displays them in the orders table.
     */
    private void loadOrders() {
        orderModel.setRowCount(0);
        productModel.setRowCount(0);

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
    }

    /**
     * Loads products for the currently selected order into the products table.
     */
    private void loadProductsForSelectedOrder() {
        productModel.setRowCount(0);

        int row = orderTable.getSelectedRow();
        if (row == -1) return;

        int orderId = (int) orderModel.getValueAt(row, 0);
        List<OrderProductDAO.OrderProduct> products = orderProductDAO.getByOrderId(orderId);

        for (OrderProductDAO.OrderProduct p : products) {
            productModel.addRow(new Object[]{
                    p.orderProductId,
                    p.productId,
                    p.quantity,
                    p.price
            });
        }
    }

    /**
     * Prompts the user to add a new order and updates the orders table.
     */
    private void addOrder() {
        try {
            int id = Integer.parseInt(JOptionPane.showInputDialog("Order ID:"));
            String status = JOptionPane.showInputDialog("Status:");
            Date date = Date.valueOf(JOptionPane.showInputDialog("Order Date (YYYY-MM-DD):"));
            int clientId = Integer.parseInt(JOptionPane.showInputDialog("Client ID:"));
            int employeeId = Integer.parseInt(JOptionPane.showInputDialog("Employee ID:"));
            int paymentId = Integer.parseInt(JOptionPane.showInputDialog("Payment ID:"));
            int deliveryId = Integer.parseInt(JOptionPane.showInputDialog("Delivery ID:"));

            orderDAO.addOrder(id, date, status, clientId, employeeId, paymentId, deliveryId);
            loadOrders();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding order: " + e.getMessage());
        }
    }

    /**
     * Prompts the user to update the selected order and refreshes the table.
     */
    private void updateOrder() {
        int row = orderTable.getSelectedRow();
        if (row == -1) return;

        try {
            int id = (int) orderModel.getValueAt(row, 0);
            String status = JOptionPane.showInputDialog("Status:", orderModel.getValueAt(row, 1));
            Date date = Date.valueOf(JOptionPane.showInputDialog("Date:", orderModel.getValueAt(row, 2)));
            int clientId = Integer.parseInt(JOptionPane.showInputDialog("Client ID:", orderModel.getValueAt(row, 3)));
            int employeeId = Integer.parseInt(JOptionPane.showInputDialog("Employee ID:", orderModel.getValueAt(row, 4)));
            int paymentId = Integer.parseInt(JOptionPane.showInputDialog("Payment ID:", orderModel.getValueAt(row, 5)));
            int deliveryId = Integer.parseInt(JOptionPane.showInputDialog("Delivery ID:", orderModel.getValueAt(row, 6)));

            orderDAO.updateOrder(id, date, status, clientId, employeeId, paymentId, deliveryId);
            loadOrders();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error updating order: " + e.getMessage());
        }
    }

    /**
     * Deletes the selected order from the database and refreshes the table.
     */
    private void deleteOrder() {
        int row = orderTable.getSelectedRow();
        if (row == -1) return;

        int id = (int) orderModel.getValueAt(row, 0);
        orderDAO.deleteOrder(id);
        loadOrders();
    }

    /**
     * Prompts the user to add a product to the selected order.
     */
    private void addProductToOrder() {
        int orderRow = orderTable.getSelectedRow();
        if (orderRow == -1) {
            JOptionPane.showMessageDialog(this, "Select an order first!");
            return;
        }

        try {
            int orderProductId = Integer.parseInt(JOptionPane.showInputDialog("OrderProduct ID:"));
            int orderId = (int) orderModel.getValueAt(orderRow, 0);
            int productId = Integer.parseInt(JOptionPane.showInputDialog("Product ID:"));
            int quantity = Integer.parseInt(JOptionPane.showInputDialog("Quantity:"));
            double price = Double.parseDouble(JOptionPane.showInputDialog("Price at time:"));

            orderProductDAO.addOrderProduct(orderProductId, orderId, productId, quantity, price);
            loadProductsForSelectedOrder();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding product: " + e.getMessage());
        }
    }

    /**
     * Prompts the user to update the selected product in the order.
     */
    private void updateProductInOrder() {
        int row = productTable.getSelectedRow();
        if (row == -1) return;

        try {
            int orderProductId = (int) productModel.getValueAt(row, 0);
            int productId = Integer.parseInt(JOptionPane.showInputDialog("Product ID:", productModel.getValueAt(row, 1)));
            int quantity = Integer.parseInt(JOptionPane.showInputDialog("Quantity:", productModel.getValueAt(row, 2)));
            double price = Double.parseDouble(JOptionPane.showInputDialog("Price:", productModel.getValueAt(row, 3)));

            orderProductDAO.updateOrderProduct(orderProductId, productId, quantity, price);
            loadProductsForSelectedOrder();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error updating product: " + e.getMessage());
        }
    }

    /**
     * Deletes the selected product from the currently selected order.
     */
    private void deleteProductFromOrder() {
        int row = productTable.getSelectedRow();
        if (row == -1) return;

        int id = (int) productModel.getValueAt(row, 0);
        orderProductDAO.deleteById(id);
        loadProductsForSelectedOrder();
    }
}
