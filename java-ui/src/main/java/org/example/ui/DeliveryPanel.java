package org.example.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import org.example.dao.DeliveryDAO;

/**
 * A JPanel that displays and manages deliveries in a table with a form for adding, updating, and deleting deliveries.
 */
public class DeliveryPanel extends JPanel {

    private final DeliveryDAO deliveryDAO = new DeliveryDAO();

    private JTextField txtId, txtCourier, txtTracking, txtAddress, txtOrderId;
    private JButton btnAdd, btnUpdate, btnDelete, btnRefresh;
    private JTable table;
    private DefaultTableModel tableModel;

    /**
     * Constructs a DeliveryPanel with a form, table, and action buttons.
     */
    public DeliveryPanel() {
        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        formPanel.add(new JLabel("Delivery ID:"));
        txtId = new JTextField(); formPanel.add(txtId);
        formPanel.add(new JLabel("Courier:"));
        txtCourier = new JTextField(); formPanel.add(txtCourier);
        formPanel.add(new JLabel("Tracking Number:"));
        txtTracking = new JTextField(); formPanel.add(txtTracking);
        formPanel.add(new JLabel("Delivery Address:"));
        txtAddress = new JTextField(); formPanel.add(txtAddress);
        formPanel.add(new JLabel("Order ID:"));
        txtOrderId = new JTextField(); formPanel.add(txtOrderId);

        add(formPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Courier", "Tracking Number", "Delivery Address", "Order ID"}, 0
        );
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    txtId.setText(tableModel.getValueAt(row, 0).toString());
                    txtCourier.setText(tableModel.getValueAt(row, 1).toString());
                    txtTracking.setText(tableModel.getValueAt(row, 2).toString());
                    txtAddress.setText(tableModel.getValueAt(row, 3).toString());
                    txtOrderId.setText(tableModel.getValueAt(row, 4) != null ?
                            tableModel.getValueAt(row, 4).toString() : "");
                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout());
        btnAdd = new JButton("Add"); buttonPanel.add(btnAdd);
        btnUpdate = new JButton("Update"); buttonPanel.add(btnUpdate);
        btnDelete = new JButton("Delete"); buttonPanel.add(btnDelete);
        btnRefresh = new JButton("Refresh"); buttonPanel.add(btnRefresh);
        add(buttonPanel, BorderLayout.SOUTH);

        btnAdd.addActionListener(e -> {
            try {
                int id = Integer.parseInt(txtId.getText());
                int orderId = Integer.parseInt(txtOrderId.getText());
                deliveryDAO.addDelivery(id, txtCourier.getText(), txtTracking.getText(),
                        txtAddress.getText(), orderId);
                refreshTable();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "ID and Order ID must be numbers");
            }
        });

        btnUpdate.addActionListener(e -> {
            try {
                int id = Integer.parseInt(txtId.getText());
                int orderId = Integer.parseInt(txtOrderId.getText());
                deliveryDAO.updateDelivery(id, txtCourier.getText(), txtTracking.getText(),
                        txtAddress.getText(), orderId);
                refreshTable();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "ID and Order ID must be numbers");
            }
        });

        btnDelete.addActionListener(e -> {
            try {
                int id = Integer.parseInt(txtId.getText());
                deliveryDAO.deleteDelivery(id);
                refreshTable();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Delivery ID must be a number");
            }
        });

        btnRefresh.addActionListener(e -> refreshTable());

        refreshTable();
    }

    /**
     * Refreshes the table to display all deliveries from the database.
     */
    private void refreshTable() {
        tableModel.setRowCount(0);
        List<DeliveryDAO.Delivery> deliveries = deliveryDAO.getAllDeliveries();

        for (DeliveryDAO.Delivery d : deliveries) {
            tableModel.addRow(new Object[]{
                    d.id, d.courier, d.trackingNumber, d.deliveryAddress, d.orderId
            });
        }
    }
}
