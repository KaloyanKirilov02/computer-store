package org.example.ui;

import org.example.dao.ReviewDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Date;
import java.util.List;

public class ReviewPanel extends JPanel {

    private ReviewDAO reviewDAO = new ReviewDAO();

    private JTable reviewTable;
    private DefaultTableModel reviewModel;

    public ReviewPanel() {
        setLayout(new BorderLayout(10, 10));

        // ---------------- TABLE ----------------
        reviewModel = new DefaultTableModel(
                new Object[]{"Review ID", "Client ID", "Product ID", "Rating", "Text", "Date"}, 0
        );
        reviewTable = new JTable(reviewModel);
        JScrollPane scroll = new JScrollPane(reviewTable);
        scroll.setBorder(BorderFactory.createTitledBorder("Reviews"));
        add(scroll, BorderLayout.CENTER);

        // Клик върху ред -> попълва форма за Update
        reviewTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = reviewTable.getSelectedRow();
                if (row >= 0) {
                    // може да се използва за предварително попълване на диалогите
                }
            }
        });

        // ---------------- BUTTONS ----------------
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

        // ---------------- BUTTON ACTIONS ----------------
        addBtn.addActionListener(e -> addReview());
        updateBtn.addActionListener(e -> updateReview());
        deleteBtn.addActionListener(e -> deleteReview());
        refreshBtn.addActionListener(e -> loadReviews());

        // Първоначално зареждане
        loadReviews();
    }

    private void loadReviews() {
        reviewModel.setRowCount(0); // clear table
        List<ReviewDAO.Review> reviews = reviewDAO.getAllReviewsFull(); // new method returning Review objects

        for (ReviewDAO.Review r : reviews) {
            reviewModel.addRow(new Object[]{
                    r.id,
                    r.clientId,
                    r.productId,
                    r.rating,
                    r.text,
                    r.date
            });
        }
    }

    private void addReview() {
        try {
            int reviewId = Integer.parseInt(JOptionPane.showInputDialog("Review ID:"));
            int clientId = Integer.parseInt(JOptionPane.showInputDialog("Client ID:"));
            int productId = Integer.parseInt(JOptionPane.showInputDialog("Product ID:"));
            int rating = Integer.parseInt(JOptionPane.showInputDialog("Rating:"));
            String reviewText = JOptionPane.showInputDialog("Review Text:");
            Date reviewDate = Date.valueOf(JOptionPane.showInputDialog("Review Date (YYYY-MM-DD):"));

            reviewDAO.addReview(reviewId, clientId, productId, rating, reviewText, reviewDate);
            loadReviews();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding review: " + e.getMessage());
        }
    }

    private void updateReview() {
        int row = reviewTable.getSelectedRow();
        if (row == -1) return;

        try {
            int reviewId = (int) reviewModel.getValueAt(row, 0);
            int rating = Integer.parseInt(JOptionPane.showInputDialog("Rating:", reviewModel.getValueAt(row, 3)));
            String reviewText = JOptionPane.showInputDialog("Review Text:", reviewModel.getValueAt(row, 4));
            Date reviewDate = Date.valueOf(JOptionPane.showInputDialog("Review Date (YYYY-MM-DD):", reviewModel.getValueAt(row, 5)));

            reviewDAO.updateReview(reviewId, rating, reviewText, reviewDate);
            loadReviews();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error updating review: " + e.getMessage());
        }
    }

    private void deleteReview() {
        int row = reviewTable.getSelectedRow();
        if (row == -1) return;

        int reviewId = (int) reviewModel.getValueAt(row, 0);
        reviewDAO.deleteReview(reviewId);
        loadReviews();
    }
}
