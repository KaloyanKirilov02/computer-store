package org.example.dao;

import org.example.db.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for managing reviews in the database.
 * Supports creating, reading, updating, and deleting reviews.
 */
public class ReviewDAO {

    /**
     * Adds a new review to the database.
     *
     * @param reviewId   Unique ID of the review
     * @param clientId   ID of the client writing the review
     * @param productId  ID of the product being reviewed
     * @param rating     Rating value
     * @param reviewText Text of the review
     * @param reviewDate Date of the review
     */
    public void addReview(int reviewId, int clientId, int productId, int rating, String reviewText, Date reviewDate) {
        String sql = "INSERT INTO reviews VALUES (review_t(?, ?, ?, ?, ?, ?))";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reviewId);
            stmt.setObject(2, getRef(conn, "clients", "client_id", clientId));
            stmt.setObject(3, getRef(conn, "products", "product_id", productId));
            stmt.setInt(4, rating);
            stmt.setString(5, reviewText);
            stmt.setDate(6, reviewDate);

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all reviews as full objects.
     *
     * @return List of all reviews
     */
    public List<Review> getAllReviewsFull() {
        List<Review> list = new ArrayList<>();
        String sql = "SELECT VALUE(r).review_id, VALUE(r).client_ref.client_id, VALUE(r).product_ref.product_id, " +
                "VALUE(r).rating, VALUE(r).review_text, VALUE(r).review_date FROM reviews r";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Review(
                        rs.getInt(1),
                        rs.getInt(2),
                        rs.getInt(3),
                        rs.getInt(4),
                        rs.getString(5),
                        rs.getDate(6)
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Retrieves reviews for a specific product.
     *
     * @param productId ID of the product
     * @return List of reviews for the product
     */
    public List<Review> getReviewsByProduct(int productId) {
        List<Review> list = new ArrayList<>();
        String sql = "SELECT VALUE(r).review_id, VALUE(r).client_ref.client_id, VALUE(r).product_ref.product_id, " +
                "VALUE(r).rating, VALUE(r).review_text, VALUE(r).review_date FROM reviews r WHERE REF(r.product_ref)=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, getRef(conn, "products", "product_id", productId));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new Review(
                        rs.getInt(1),
                        rs.getInt(2),
                        rs.getInt(3),
                        rs.getInt(4),
                        rs.getString(5),
                        rs.getDate(6)
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Retrieves reviews written by a specific client.
     *
     * @param clientId ID of the client
     * @return List of reviews by the client
     */
    public List<Review> getReviewsByClient(int clientId) {
        List<Review> list = new ArrayList<>();
        String sql = "SELECT VALUE(r).review_id, VALUE(r).client_ref.client_id, VALUE(r).product_ref.product_id, " +
                "VALUE(r).rating, VALUE(r).review_text, VALUE(r).review_date FROM reviews r WHERE REF(r.client_ref)=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, getRef(conn, "clients", "client_id", clientId));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new Review(
                        rs.getInt(1),
                        rs.getInt(2),
                        rs.getInt(3),
                        rs.getInt(4),
                        rs.getString(5),
                        rs.getDate(6)
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Updates a review.
     *
     * @param reviewId   ID of the review to update
     * @param rating     New rating
     * @param reviewText New review text
     * @param reviewDate New review date
     */
    public void updateReview(int reviewId, int rating, String reviewText, Date reviewDate) {
        String sql = "UPDATE reviews r SET VALUE(r).rating=?, VALUE(r).review_text=?, VALUE(r).review_date=? WHERE VALUE(r).review_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, rating);
            stmt.setString(2, reviewText);
            stmt.setDate(3, reviewDate);
            stmt.setInt(4, reviewId);

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes a review by its ID.
     *
     * @param reviewId ID of the review to delete
     */
    public void deleteReview(int reviewId) {
        String sql = "DELETE FROM reviews r WHERE VALUE(r).review_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reviewId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to get a REF object for a table row.
     *
     * @param conn      Database connection
     * @param tableName Table name
     * @param idColumn  Column name of the ID
     * @param id        ID value
     * @return REF object to the row, or null if not found
     * @throws SQLException if a database access error occurs
     */
    private Object getRef(Connection conn, String tableName, String idColumn, int id) throws SQLException {
        String sql = "SELECT REF(t) FROM " + tableName + " t WHERE VALUE(t)." + idColumn + "=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getObject(1);
        }
        return null;
    }

    /**
     * Represents a review object.
     */
    public static class Review {
        public int id;
        public int clientId;
        public int productId;
        public int rating;
        public String text;
        public Date date;

        public Review(int id, int clientId, int productId, int rating, String text, Date date) {
            this.id = id;
            this.clientId = clientId;
            this.productId = productId;
            this.rating = rating;
            this.text = text;
            this.date = date;
        }
    }
}
