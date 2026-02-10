package org.example.ui;

import org.example.dao.ReviewDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Date;
import java.util.List;

/**
 * Swing panel for managing reviews: create, update, delete, and browse.
 */
public class ReviewPanel extends JPanel {

    private final ReviewDAO reviewDAO = new ReviewDAO();

    private final JTable reviewTable;
    private final DefaultTableModel reviewModel;

    /**
     * Creates the review management panel and loads the initial data.
     */
    public ReviewPanel() {
        setLayout(new BorderLayout(10, 10));

        reviewModel = new DefaultTableModel(
                new Object[]{"Review ID", "Client ID", "Product ID", "Rating", "Text", "Date"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        reviewTable = new JTable(reviewModel);
        reviewTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(reviewTable);
        scroll.setBorder(BorderFactory.createTitledBorder("Reviews"));
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

        addBtn.addActionListener(e -> addReview());
        updateBtn.addActionListener(e -> updateReview());
        deleteBtn.addActionListener(e -> deleteReview());
        refreshBtn.addActionListener(e -> loadReviews());

        loadReviews();
    }

    /**
     * Loads all reviews into the table.
     */
    private void loadReviews() {
        reviewModel.setRowCount(0);
        try {
            List<ReviewDAO.Review> reviews = reviewDAO.getAllReviewsFull();
            for (ReviewDAO.Review r : reviews) {
                reviewModel.addRow(new Object[]{
                        r.id, r.clientId, r.productId, r.rating, r.text, r.date
                });
            }
        } catch (RuntimeException ex) {
            showError("Error while loading reviews: " + ex.getMessage());
        }
    }

    /**
     * Prompts for fields and creates a new review.
     */
    private void addReview() {
        try {
            Integer reviewId = askInt("Review ID:");
            if (reviewId == null) return;

            Integer clientId = askInt("Client ID:");
            if (clientId == null) return;

            Integer productId = askInt("Product ID:");
            if (productId == null) return;

            Integer rating = askInt("Rating (1-5):");
            if (rating == null) return;

            String text = askString("Text:");
            if (text == null) return;

            Date date = askDate("Date (YYYY-MM-DD):");
            if (date == null) return;

            reviewDAO.addReview(reviewId, clientId, productId, rating, text, date);
            loadReviews();
        } catch (RuntimeException ex) {
            showError("Error while adding review: " + ex.getMessage());
        }
    }

    /**
     * Updates the selected review after prompting for new values.
     */
    private void updateReview() {
        int row = reviewTable.getSelectedRow();
        if (row == -1) {
            showInfo("Select a review from the table first.");
            return;
        }

        try {
            int reviewId = (int) reviewModel.getValueAt(row, 0);

            Integer rating = askInt("Rating (1-5):", reviewModel.getValueAt(row, 3));
            if (rating == null) return;

            String text = askString("Text:", reviewModel.getValueAt(row, 4));
            if (text == null) return;

            Date date = askDate("Date (YYYY-MM-DD):", reviewModel.getValueAt(row, 5));
            if (date == null) return;

            reviewDAO.updateReview(reviewId, rating, text, date);
            loadReviews();
        } catch (RuntimeException ex) {
            showError("Error while updating review: " + ex.getMessage());
        }
    }

    /**
     * Deletes the selected review after confirmation.
     */
    private void deleteReview() {
        int row = reviewTable.getSelectedRow();
        if (row == -1) {
            showInfo("Select a review from the table first.");
            return;
        }

        int reviewId = (int) reviewModel.getValueAt(row, 0);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete review ID " + reviewId + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            reviewDAO.deleteReview(reviewId);
            loadReviews();
        } catch (RuntimeException ex) {
            showError("Error while deleting review: " + ex.getMessage());
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