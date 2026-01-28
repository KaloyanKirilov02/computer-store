package org.example.ui;

import org.example.dao.PromotionDAO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Date;
import java.util.List;

/**
 * JPanel for managing promotions.
 * Displays promotions in a table and allows CRUD operations.
 */
public class PromotionPanel extends JPanel {

    private PromotionDAO promotionDAO = new PromotionDAO();
    private JTable promotionTable;
    private DefaultTableModel promotionModel;

    /**
     * Constructs the PromotionPanel with table and buttons for CRUD operations.
     */
    public PromotionPanel() {
        setLayout(new BorderLayout(10,10));

        promotionModel = new DefaultTableModel(
                new Object[]{"ID", "Name", "Discount %", "Start Date", "End Date"}, 0);
        promotionTable = new JTable(promotionModel);
        loadPromotions();

        JScrollPane scroll = new JScrollPane(promotionTable);
        scroll.setBorder(BorderFactory.createTitledBorder("Promotions"));
        add(scroll, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1,4,5,5));
        JButton addBtn = new JButton("Add");
        JButton updateBtn = new JButton("Update");
        JButton deleteBtn = new JButton("Delete");
        JButton refreshBtn = new JButton("Refresh");

        buttonPanel.add(addBtn);
        buttonPanel.add(updateBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(refreshBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> addPromotion());
        updateBtn.addActionListener(e -> updatePromotion());
        deleteBtn.addActionListener(e -> deletePromotion());
        refreshBtn.addActionListener(e -> loadPromotions());
    }

    /**
     * Loads all promotions from the DAO into the table.
     */
    private void loadPromotions() {
        promotionModel.setRowCount(0);
        List<PromotionDAO.Promotion> promotions = promotionDAO.getAllPromotionsFull();
        for (PromotionDAO.Promotion p : promotions) {
            promotionModel.addRow(new Object[]{
                    p.id, p.name, p.discountPercent, p.startDate, p.endDate
            });
        }
    }

    /**
     * Prompts the user to add a new promotion and updates the table.
     */
    private void addPromotion() {
        try {
            int promotionId = Integer.parseInt(JOptionPane.showInputDialog("Promotion ID:"));
            String name = JOptionPane.showInputDialog("Name:");
            double discount = Double.parseDouble(JOptionPane.showInputDialog("Discount %:"));
            Date startDate = Date.valueOf(JOptionPane.showInputDialog("Start Date (YYYY-MM-DD):"));
            Date endDate = Date.valueOf(JOptionPane.showInputDialog("End Date (YYYY-MM-DD):"));

            promotionDAO.addPromotion(promotionId, name, discount, startDate, endDate);
            loadPromotions();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding promotion: " + e.getMessage());
        }
    }

    /**
     * Prompts the user to update the selected promotion and updates the table.
     */
    private void updatePromotion() {
        int row = promotionTable.getSelectedRow();
        if (row == -1) return;

        try {
            int promotionId = (int) promotionModel.getValueAt(row, 0);
            String name = JOptionPane.showInputDialog("Name:", promotionModel.getValueAt(row, 1));
            double discount = Double.parseDouble(JOptionPane.showInputDialog("Discount %:", promotionModel.getValueAt(row, 2)));
            Date startDate = Date.valueOf(JOptionPane.showInputDialog("Start Date (YYYY-MM-DD):", promotionModel.getValueAt(row, 3)));
            Date endDate = Date.valueOf(JOptionPane.showInputDialog("End Date (YYYY-MM-DD):", promotionModel.getValueAt(row, 4)));

            promotionDAO.updatePromotion(promotionId, name, discount, startDate, endDate);
            loadPromotions();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error updating promotion: " + e.getMessage());
        }
    }

    /**
     * Deletes the selected promotion and updates the table.
     */
    private void deletePromotion() {
        int row = promotionTable.getSelectedRow();
        if (row == -1) return;

        int promotionId = (int) promotionModel.getValueAt(row, 0);
        promotionDAO.deletePromotion(promotionId);
        loadPromotions();
    }
}
