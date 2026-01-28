package org.example.ui;

import org.example.dao.ProductPromotionDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * JPanel for managing product promotions.
 * Displays product promotions in a table with CRUD operations.
 */
public class ProductPromotionPanel extends JPanel {

    private final ProductPromotionDAO dao = new ProductPromotionDAO();
    private final JTable table;
    private final DefaultTableModel model;

    /**
     * Constructs the ProductPromotionPanel with table and buttons for CRUD operations.
     */
    public ProductPromotionPanel() {
        setLayout(new BorderLayout(10, 10));

        model = new DefaultTableModel(
                new Object[]{"ID", "Product Name", "Promotion Name", "Discount %", "Price After Discount"}, 0
        );
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
     * Loads product promotion data from the DAO into the table.
     */
    private void loadData() {
        model.setRowCount(0);
        List<ProductPromotionDAO.ProductPromotionFull> list = dao.getAllFull();
        for (ProductPromotionDAO.ProductPromotionFull p : list) {
            model.addRow(new Object[]{
                    p.id,
                    p.productName,
                    p.promotionName,
                    p.promotionDiscount,
                    p.discountedPrice
            });
        }
    }

    /**
     * Prompts the user to add a new product promotion and updates the table.
     */
    private void addProductPromotion() {
        try {
            int id = Integer.parseInt(JOptionPane.showInputDialog("Product Promotion ID:"));
            int productId = Integer.parseInt(JOptionPane.showInputDialog("Product ID:"));
            int promotionId = Integer.parseInt(JOptionPane.showInputDialog("Promotion ID:"));

            dao.addProductPromotion(id, productId, promotionId);
            loadData();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding: " + e.getMessage());
        }
    }

    /**
     * Prompts the user to update the selected product promotion and updates the table.
     */
    private void updateProductPromotion() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a row first!");
            return;
        }

        try {
            int id = (int) model.getValueAt(row, 0);
            int productId = Integer.parseInt(JOptionPane.showInputDialog("Product ID:", model.getValueAt(row, 1)));
            int promotionId = Integer.parseInt(JOptionPane.showInputDialog("Promotion ID:", model.getValueAt(row, 2)));

            dao.updateProductPromotion(id, productId, promotionId);
            loadData();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error updating: " + e.getMessage());
        }
    }

    /**
     * Deletes the selected product promotion and updates the table.
     */
    private void deleteProductPromotion() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a row first!");
            return;
        }

        int id = (int) model.getValueAt(row, 0);
        dao.deleteProductPromotion(id);
        loadData();
    }
}
