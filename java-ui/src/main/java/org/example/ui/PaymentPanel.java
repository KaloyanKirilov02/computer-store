package org.example.ui;

import org.example.dao.PaymentDAO;
import org.example.dao.PaymentDAO.PaymentInfo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Date;
import java.util.List;

/**
 * Swing panel for managing payments: create, update, delete, and browse.
 */
public class PaymentPanel extends JPanel {

    private final PaymentDAO paymentDAO = new PaymentDAO();

    private final JTable paymentTable;
    private final DefaultTableModel paymentModel;

    /**
     * Creates the payment management panel and loads the initial data.
     */
    public PaymentPanel() {
        setLayout(new BorderLayout(10, 10));

        paymentModel = new DefaultTableModel(
                new Object[]{"Payment ID", "Amount", "Pay Date", "Status", "Order ID"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        paymentTable = new JTable(paymentModel);
        paymentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(paymentTable);
        scroll.setBorder(BorderFactory.createTitledBorder("Payments"));
        add(scroll, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        JButton addBtn = new JButton("Add");
        JButton updateBtn = new JButton("Update");
        JButton deleteBtn = new JButton("Delete");
        JButton refreshBtn = new JButton("Refresh");

        buttonPanel.add(addBtn);
        buttonPanel.add(updateBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(refreshBtn);

        add(buttonPanel, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> addPayment());
        updateBtn.addActionListener(e -> updatePayment());
        deleteBtn.addActionListener(e -> deletePayment());
        refreshBtn.addActionListener(e -> loadPayments());

        loadPayments();
    }

    /**
     * Loads all payments into the table.
     */
    private void loadPayments() {
        paymentModel.setRowCount(0);

        try {
            List<PaymentInfo> payments = paymentDAO.getAllPaymentsInfo();
            for (PaymentInfo info : payments) {
                paymentModel.addRow(new Object[]{
                        info.paymentId,
                        info.amount,
                        info.payDate,
                        info.status,
                        info.orderId
                });
            }
        } catch (RuntimeException ex) {
            showError("Error while loading payments: " + ex.getMessage());
        }
    }

    /**
     * Prompts for fields and creates a new payment.
     */
    private void addPayment() {
        try {
            Integer paymentId = askInt("Payment ID:");
            if (paymentId == null) return;

            Double amount = askDouble("Amount:");
            if (amount == null) return;

            Date payDate = askDate("Pay Date (YYYY-MM-DD):");
            if (payDate == null) return;

            String status = askString("Status:");
            if (status == null) return;

            Integer orderId = askInt("Order ID:");
            if (orderId == null) return;

            paymentDAO.addPayment(paymentId, amount, payDate, status, orderId);
            loadPayments();
        } catch (RuntimeException ex) {
            showError("Error while adding payment: " + ex.getMessage());
        }
    }

    /**
     * Updates the selected payment after prompting for new values.
     */
    private void updatePayment() {
        int row = paymentTable.getSelectedRow();
        if (row == -1) {
            showInfo("Select a payment from the table first.");
            return;
        }

        try {
            int paymentId = (int) paymentModel.getValueAt(row, 0);

            Double amount = askDouble("Amount:", paymentModel.getValueAt(row, 1));
            if (amount == null) return;

            Date payDate = askDate("Pay Date (YYYY-MM-DD):", paymentModel.getValueAt(row, 2));
            if (payDate == null) return;

            String status = askString("Status:", paymentModel.getValueAt(row, 3));
            if (status == null) return;

            Integer orderId = askInt("Order ID:", paymentModel.getValueAt(row, 4));
            if (orderId == null) return;

            paymentDAO.updatePayment(paymentId, amount, payDate, status, orderId);
            loadPayments();
        } catch (RuntimeException ex) {
            showError("Error while updating payment: " + ex.getMessage());
        }
    }

    /**
     * Deletes the selected payment after confirmation.
     */
    private void deletePayment() {
        int row = paymentTable.getSelectedRow();
        if (row == -1) {
            showInfo("Select a payment from the table first.");
            return;
        }

        int paymentId = (int) paymentModel.getValueAt(row, 0);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete payment ID " + paymentId + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            paymentDAO.deletePayment(paymentId);
            loadPayments();
        } catch (RuntimeException ex) {
            showError("Error while deleting payment: " + ex.getMessage());
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