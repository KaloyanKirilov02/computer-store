package org.example.ui;

import org.example.dao.PaymentDAO;
import org.example.dao.PaymentDAO.PaymentInfo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Date;
import java.util.List;

/**
 * JPanel for managing payments.
 * Provides a table to display payments and buttons to perform CRUD operations.
 */
public class PaymentPanel extends JPanel {

    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final JTable paymentTable;
    private final DefaultTableModel paymentModel;

    /**
     * Constructs a PaymentPanel with a table and CRUD buttons.
     */
    public PaymentPanel() {
        setLayout(new BorderLayout(10, 10));

        paymentModel = new DefaultTableModel(
                new Object[]{"Payment ID", "Amount", "Pay Date", "Status", "Order ID"}, 0
        );
        paymentTable = new JTable(paymentModel);
        loadPayments();

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
    }

    /**
     * Loads all payments from the database into the table.
     */
    private void loadPayments() {
        paymentModel.setRowCount(0);
        List<Integer> payments = paymentDAO.getAllPayments();
        for (Integer id : payments) {
            PaymentInfo info = paymentDAO.getPaymentInfo(id);
            if (info != null) {
                paymentModel.addRow(new Object[]{
                        info.paymentId,
                        info.amount,
                        info.payDate,
                        info.status,
                        info.orderId
                });
            }
        }
    }

    /**
     * Prompts the user to add a new payment and updates the table.
     */
    private void addPayment() {
        try {
            int paymentId = Integer.parseInt(JOptionPane.showInputDialog("Payment ID:"));
            double amount = Double.parseDouble(JOptionPane.showInputDialog("Amount:"));
            Date payDate = Date.valueOf(JOptionPane.showInputDialog("Pay Date (yyyy-mm-dd):"));
            String status = JOptionPane.showInputDialog("Status:");
            int orderId = Integer.parseInt(JOptionPane.showInputDialog("Order ID:"));

            paymentDAO.addPayment(paymentId, amount, payDate, status, orderId);
            loadPayments();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding payment: " + e.getMessage());
        }
    }

    /**
     * Prompts the user to update the selected payment and refreshes the table.
     */
    private void updatePayment() {
        int row = paymentTable.getSelectedRow();
        if (row == -1) return;

        try {
            int paymentId = (int) paymentModel.getValueAt(row, 0);
            double amount = Double.parseDouble(JOptionPane.showInputDialog("Amount:", paymentModel.getValueAt(row, 1)));
            Date payDate = Date.valueOf(JOptionPane.showInputDialog("Pay Date (yyyy-mm-dd):", paymentModel.getValueAt(row, 2)));
            String status = JOptionPane.showInputDialog("Status:", paymentModel.getValueAt(row, 3));
            int orderId = Integer.parseInt(JOptionPane.showInputDialog("Order ID:", paymentModel.getValueAt(row, 4)));

            paymentDAO.updatePayment(paymentId, amount, payDate, status, orderId);
            loadPayments();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error updating payment: " + e.getMessage());
        }
    }

    /**
     * Deletes the selected payment from the database and refreshes the table.
     */
    private void deletePayment() {
        int row = paymentTable.getSelectedRow();
        if (row == -1) return;

        int paymentId = (int) paymentModel.getValueAt(row, 0);
        paymentDAO.deletePayment(paymentId);
        loadPayments();
    }
}
