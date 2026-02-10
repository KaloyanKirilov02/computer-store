package org.example.ui;

import org.example.dao.DeliveryDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Swing panel for managing deliveries: create, update, delete, and browse.
 */
public class DeliveryPanel extends JPanel {

    private final DeliveryDAO deliveryDAO = new DeliveryDAO();

    private final JTextField txtId = new JTextField();
    private final JTextField txtCourier = new JTextField();
    private final JTextField txtTracking = new JTextField();
    private final JTextField txtAddress = new JTextField();
    private final JTextField txtOrderId = new JTextField();

    private final JButton btnAdd = new JButton("Add");
    private final JButton btnUpdate = new JButton("Update");
    private final JButton btnDelete = new JButton("Delete");
    private final JButton btnRefresh = new JButton("Refresh");

    private final JTable table;
    private final DefaultTableModel tableModel;

    /**
     * Creates the delivery management panel and loads the initial data.
     */
    public DeliveryPanel() {
        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        formPanel.add(new JLabel("Delivery ID:"));
        formPanel.add(txtId);
        formPanel.add(new JLabel("Courier:"));
        formPanel.add(txtCourier);
        formPanel.add(new JLabel("Tracking Number:"));
        formPanel.add(txtTracking);
        formPanel.add(new JLabel("Delivery Address:"));
        formPanel.add(txtAddress);
        formPanel.add(new JLabel("Order ID:"));
        formPanel.add(txtOrderId);

        add(formPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Courier", "Tracking Number", "Delivery Address", "Order ID"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    fillFormFromTable(row);
                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnRefresh);
        add(buttonPanel, BorderLayout.SOUTH);

        btnAdd.addActionListener(e -> addDelivery());
        btnUpdate.addActionListener(e -> updateDelivery());
        btnDelete.addActionListener(e -> deleteDelivery());
        btnRefresh.addActionListener(e -> refreshTable());

        refreshTable();
    }

    /**
     * Reads the form fields and creates a new delivery.
     */
    private void addDelivery() {
        try {
            int id = parseRequiredInt(txtId, "Delivery ID");
            int orderId = parseRequiredInt(txtOrderId, "Order ID");

            deliveryDAO.addDelivery(
                    id,
                    txtCourier.getText(),
                    txtTracking.getText(),
                    txtAddress.getText(),
                    orderId
            );

            refreshTable();
            selectRowById(id);
            clearForm();
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    /**
     * Reads the form fields and updates an existing delivery.
     */
    private void updateDelivery() {
        try {
            int id = parseRequiredInt(txtId, "Delivery ID");
            int orderId = parseRequiredInt(txtOrderId, "Order ID");

            deliveryDAO.updateDelivery(
                    id,
                    txtCourier.getText(),
                    txtTracking.getText(),
                    txtAddress.getText(),
                    orderId
            );

            refreshTable();
            selectRowById(id);
            clearForm();
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    /**
     * Deletes the delivery specified in the form after confirmation.
     */
    private void deleteDelivery() {
        try {
            int id = parseRequiredInt(txtId, "Delivery ID");

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to delete delivery ID " + id + "?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                deliveryDAO.deleteDelivery(id);
                refreshTable();
                clearForm();
            }
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    /**
     * Reloads the table data from the database.
     */
    private void refreshTable() {
        tableModel.setRowCount(0);
        List<DeliveryDAO.Delivery> deliveries = deliveryDAO.getAllDeliveries();
        for (DeliveryDAO.Delivery d : deliveries) {
            tableModel.addRow(new Object[]{d.id, d.courier, d.trackingNumber, d.deliveryAddress, d.orderId});
        }
    }

    /**
     * Populates the form fields from the selected table row.
     *
     * @param row selected table row index
     */
    private void fillFormFromTable(int row) {
        txtId.setText(String.valueOf(tableModel.getValueAt(row, 0)));
        txtCourier.setText(String.valueOf(tableModel.getValueAt(row, 1)));
        txtTracking.setText(String.valueOf(tableModel.getValueAt(row, 2)));
        txtAddress.setText(String.valueOf(tableModel.getValueAt(row, 3)));
        Object orderVal = tableModel.getValueAt(row, 4);
        txtOrderId.setText(orderVal != null ? String.valueOf(orderVal) : "");
    }

    /**
     * Clears all form fields and resets table selection.
     */
    private void clearForm() {
        txtId.setText("");
        txtCourier.setText("");
        txtTracking.setText("");
        txtAddress.setText("");
        txtOrderId.setText("");
        table.clearSelection();
    }

    /**
     * Parses a required integer value from a text field.
     *
     * @param field text field to read from
     * @param name  logical field name used in error messages
     * @return parsed integer value
     * @throws IllegalArgumentException if the field is empty or not a valid integer
     */
    private int parseRequiredInt(JTextField field, String name) {
        String s = field.getText();
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be empty.");
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a number.");
        }
    }

    /**
     * Selects a row in the table by delivery ID and scrolls it into view.
     *
     * @param id delivery identifier to locate
     */
    private void selectRowById(int id) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Object v = tableModel.getValueAt(i, 0);
            if (v != null && String.valueOf(v).equals(String.valueOf(id))) {
                table.setRowSelectionInterval(i, i);
                table.scrollRectToVisible(table.getCellRect(i, 0, true));
                break;
            }
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
}