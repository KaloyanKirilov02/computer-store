package org.example.ui;

import org.example.dao.ProductPromotionDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Swing panel for managing product promotions (linking products to promotions)
 * and displaying the computed discounted price.
 */
public class ProductPromotionPanel extends JPanel {

    private final ProductPromotionDAO dao = new ProductPromotionDAO();
    private final JTable table;
    private final DefaultTableModel model;

    /**
     * Creates the product-promotion management panel and loads the initial data.
     */
    public ProductPromotionPanel() {
        setLayout(new BorderLayout(10, 10));

        model = new DefaultTableModel(
                new Object[]{
                        "ID",
                        "Product ID", "Product Name",
                        "Promotion ID", "Promotion Name",
                        "Discount %", "Price After Discount"
                },
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder("Product Promotions"));
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

        addBtn.addActionListener(e -> addProductPromotion());
        updateBtn.addActionListener(e -> updateProductPromotion());
        deleteBtn.addActionListener(e -> deleteProductPromotion());
        refreshBtn.addActionListener(e -> loadData());

        loadData();
    }

    /**
     * Loads the full product-promotion view (including names and discounted price) into the table.
     */
    private void loadData() {
        model.setRowCount(0);

        List<ProductPromotionDAO.ProductPromotionFull> list = dao.getAllFull();
        for (ProductPromotionDAO.ProductPromotionFull p : list) {
            model.addRow(new Object[]{
                    p.id,
                    p.productId, p.productName,
                    p.promotionId, p.promotionName,
                    p.promotionDiscount,
                    p.discountedPrice
            });
        }
    }

    /**
     * Prompts for IDs and creates a new product-promotion link.
     */
    private void addProductPromotion() {
        try {
            Integer id = askInt("Product Promotion ID:");
            if (id == null) return;

            Integer productId = askInt("Product ID:");
            if (productId == null) return;

            Integer promotionId = askInt("Promotion ID:");
            if (promotionId == null) return;

            dao.addProductPromotion(id, productId, promotionId);
            loadData();
        } catch (RuntimeException e) {
            showError("Error while adding: " + e.getMessage());
        }
    }

    /**
     * Updates the selected product-promotion link.
     */
    private void updateProductPromotion() {
        int row = table.getSelectedRow();
        if (row == -1) {
            showInfo("Select a row from the table first.");
            return;
        }

        try {
            int id = Integer.parseInt(String.valueOf(model.getValueAt(row, 0)));

            Integer productId = askInt("Product ID:", model.getValueAt(row, 1));
            if (productId == null) return;

            Integer promotionId = askInt("Promotion ID:", model.getValueAt(row, 3));
            if (promotionId == null) return;

            dao.updateProductPromotion(id, productId, promotionId);
            loadData();
        } catch (RuntimeException e) {
            showError("Error while updating: " + e.getMessage());
        }
    }

    /**
     * Deletes the selected product-promotion link after confirmation.
     */
    private void deleteProductPromotion() {
        int row = table.getSelectedRow();
        if (row == -1) {
            showInfo("Select a row from the table first.");
            return;
        }

        int id = Integer.parseInt(String.valueOf(model.getValueAt(row, 0)));

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete record ID " + id + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            dao.deleteProductPromotion(id);
            loadData();
        } catch (RuntimeException e) {
            showError("Error while deleting: " + e.getMessage());
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