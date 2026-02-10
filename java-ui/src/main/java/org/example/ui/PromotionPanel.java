package org.example.ui;

import org.example.dao.PromotionDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Date;
import java.util.List;

/**
 * Swing panel for managing promotions: create, update, delete, and browse.
 */
public class PromotionPanel extends JPanel {

    private final PromotionDAO promotionDAO = new PromotionDAO();
    private final JTable promotionTable;
    private final DefaultTableModel promotionModel;

    /**
     * Creates the promotion management panel and loads the initial data.
     */
    public PromotionPanel() {
        setLayout(new BorderLayout(10, 10));

        promotionModel = new DefaultTableModel(
                new Object[]{"ID", "Name", "Discount %", "Start Date", "End Date"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        promotionTable = new JTable(promotionModel);
        promotionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(promotionTable);
        scroll.setBorder(BorderFactory.createTitledBorder("Promotions"));
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

        addBtn.addActionListener(e -> addPromotion());
        updateBtn.addActionListener(e -> updatePromotion());
        deleteBtn.addActionListener(e -> deletePromotion());
        refreshBtn.addActionListener(e -> loadPromotions());

        loadPromotions();
    }

    /**
     * Loads all promotions into the table.
     */
    private void loadPromotions() {
        promotionModel.setRowCount(0);
        try {
            List<PromotionDAO.Promotion> promotions = promotionDAO.getAllPromotionsFull();
            for (PromotionDAO.Promotion p : promotions) {
                promotionModel.addRow(new Object[]{
                        p.id, p.name, p.discountPercent, p.startDate, p.endDate
                });
            }
        } catch (RuntimeException ex) {
            showError("Error while loading promotions: " + ex.getMessage());
        }
    }

    /**
     * Prompts for fields and creates a new promotion.
     */
    private void addPromotion() {
        try {
            Integer id = askInt("Promotion ID:");
            if (id == null) return;

            String name = askString("Name:");
            if (name == null) return;

            Double discount = askDouble("Discount %:");
            if (discount == null) return;

            Date startDate = askDate("Start Date (YYYY-MM-DD):");
            if (startDate == null) return;

            Date endDate = askDate("End Date (YYYY-MM-DD):");
            if (endDate == null) return;

            promotionDAO.addPromotion(id, name, discount, startDate, endDate);
            loadPromotions();
        } catch (RuntimeException e) {
            showError("Error while adding promotion: " + e.getMessage());
        }
    }

    /**
     * Updates the selected promotion after prompting for new values.
     */
    private void updatePromotion() {
        int row = promotionTable.getSelectedRow();
        if (row == -1) {
            showInfo("Select a promotion from the table first.");
            return;
        }

        try {
            int id = Integer.parseInt(String.valueOf(promotionModel.getValueAt(row, 0)));

            String name = askString("Name:", promotionModel.getValueAt(row, 1));
            if (name == null) return;

            Double discount = askDouble("Discount %:", promotionModel.getValueAt(row, 2));
            if (discount == null) return;

            Date startDate = askDate("Start Date (YYYY-MM-DD):", promotionModel.getValueAt(row, 3));
            if (startDate == null) return;

            Date endDate = askDate("End Date (YYYY-MM-DD):", promotionModel.getValueAt(row, 4));
            if (endDate == null) return;

            promotionDAO.updatePromotion(id, name, discount, startDate, endDate);
            loadPromotions();
        } catch (RuntimeException e) {
            showError("Error while updating promotion: " + e.getMessage());
        }
    }

    /**
     * Deletes the selected promotion after confirmation.
     */
    private void deletePromotion() {
        int row = promotionTable.getSelectedRow();
        if (row == -1) {
            showInfo("Select a promotion from the table first.");
            return;
        }

        int id = Integer.parseInt(String.valueOf(promotionModel.getValueAt(row, 0)));

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete promotion ID " + id + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            promotionDAO.deletePromotion(id);
            loadPromotions();
        } catch (RuntimeException e) {
            showError("Error while deleting promotion: " + e.getMessage());
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