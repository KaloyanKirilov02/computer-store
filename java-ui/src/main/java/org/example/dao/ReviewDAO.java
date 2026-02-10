package org.example.dao;

import org.example.db.DBConnection;
import org.example.util.Validator;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for managing reviews stored as Oracle object types (e.g. {@code review_t})
 * in the {@code reviews} object table.
 *
 * <p>Reviews reference clients and products via Oracle {@code REF}s. This DAO resolves those references
 * by IDs before insert and uses {@code DEREF(...).<id>} when reading. Filtering by product/client is done
 * via subqueries returning the appropriate {@code REF}.</p>
 */
public class ReviewDAO {

    private static final String INSERT_SQL =
            "INSERT INTO reviews VALUES (review_t(?, ?, ?, ?, ?, ?))";

    private static final String UPDATE_SQL =
            "UPDATE reviews r " +
                    "SET VALUE(r).rating=?, VALUE(r).review_text=?, VALUE(r).review_date=? " +
                    "WHERE VALUE(r).review_id=?";

    private static final String DELETE_SQL =
            "DELETE FROM reviews r WHERE VALUE(r).review_id=?";

    private static final String SELECT_ALL_SQL =
            "SELECT " +
                    "  VALUE(r).review_id, " +
                    "  DEREF(VALUE(r).client_ref).client_id, " +
                    "  DEREF(VALUE(r).product_ref).product_id, " +
                    "  VALUE(r).rating, " +
                    "  VALUE(r).review_text, " +
                    "  VALUE(r).review_date " +
                    "FROM reviews r";

    private static final String SELECT_BY_PRODUCT_SQL =
            "SELECT " +
                    "  VALUE(r).review_id, " +
                    "  DEREF(VALUE(r).client_ref).client_id, " +
                    "  DEREF(VALUE(r).product_ref).product_id, " +
                    "  VALUE(r).rating, " +
                    "  VALUE(r).review_text, " +
                    "  VALUE(r).review_date " +
                    "FROM reviews r " +
                    "WHERE VALUE(r).product_ref = (SELECT REF(p) FROM products p WHERE VALUE(p).product_id=?)";

    private static final String SELECT_BY_CLIENT_SQL =
            "SELECT " +
                    "  VALUE(r).review_id, " +
                    "  DEREF(VALUE(r).client_ref).client_id, " +
                    "  DEREF(VALUE(r).product_ref).product_id, " +
                    "  VALUE(r).rating, " +
                    "  VALUE(r).review_text, " +
                    "  VALUE(r).review_date " +
                    "FROM reviews r " +
                    "WHERE VALUE(r).client_ref = (SELECT REF(c) FROM clients c WHERE VALUE(c).client_id=?)";

    private static final String CLIENT_REF_SQL =
            "SELECT REF(c) FROM clients c WHERE VALUE(c).client_id=?";

    private static final String PRODUCT_REF_SQL =
            "SELECT REF(p) FROM products p WHERE VALUE(p).product_id=?";

    /**
     * Simple data holder representing a review row (object instance) read from the {@code reviews} object table.
     */
    public static class Review {
        public final int id;
        public final int clientId;
        public final int productId;
        public final int rating;
        public final String text;
        public final Date date;

        /**
         * Creates a review DTO.
         *
         * @param id       review identifier
         * @param clientId referenced client id
         * @param productId referenced product id
         * @param rating   rating value (expected 1..5)
         * @param text     review text
         * @param date     review date
         */
        public Review(int id, int clientId, int productId, int rating, String text, Date date) {
            this.id = id;
            this.clientId = clientId;
            this.productId = productId;
            this.rating = rating;
            this.text = text;
            this.date = date;
        }
    }

    /**
     * Inserts a new review into the {@code reviews} object table.
     *
     * @param reviewId    review identifier
     * @param clientId    referenced client id
     * @param productId   referenced product id
     * @param rating      rating (1..5)
     * @param reviewText  review text
     * @param reviewDate  review date
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         if the database operation fails
     */
    public void addReview(int reviewId, int clientId, int productId, int rating, String reviewText, Date reviewDate) {
        Validator.validateReview(reviewId, clientId, productId, rating, reviewText, reviewDate);

        try {
            PreparedStatement ps = DBConnection.prepareStatement(INSERT_SQL);
            ps.clearParameters();

            ps.setInt(1, reviewId);
            ps.setObject(2, getClientRefOrThrow(clientId));
            ps.setObject(3, getProductRefOrThrow(productId));
            ps.setInt(4, rating);
            ps.setString(5, reviewText);
            ps.setDate(6, reviewDate);

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add review: " + e.getMessage(), e);
        }
    }

    /**
     * Reads all reviews from the {@code reviews} object table.
     *
     * @return list of all reviews (may be empty)
     * @throws RuntimeException if the database operation fails
     */
    public List<Review> getAllReviewsFull() {
        List<Review> list = new ArrayList<>();

        try {
            PreparedStatement ps = DBConnection.prepareStatement(SELECT_ALL_SQL);
            ps.clearParameters();

            try (ResultSet rs = ps.executeQuery()) {
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
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reviews: " + e.getMessage(), e);
        }

        return list;
    }

    /**
     * Reads all reviews for a specific product.
     *
     * @param productId product identifier
     * @return list of reviews for the given product (may be empty)
     * @throws IllegalArgumentException if {@code productId} is not positive
     * @throws RuntimeException         if the database operation fails
     */
    public List<Review> getReviewsByProduct(int productId) {
        Validator.positiveInt(productId, "Product ID");

        List<Review> list = new ArrayList<>();

        try {
            PreparedStatement ps = DBConnection.prepareStatement(SELECT_BY_PRODUCT_SQL);
            ps.clearParameters();
            ps.setInt(1, productId);

            try (ResultSet rs = ps.executeQuery()) {
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
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reviews by product: " + e.getMessage(), e);
        }

        return list;
    }

    /**
     * Reads all reviews written by a specific client.
     *
     * @param clientId client identifier
     * @return list of reviews for the given client (may be empty)
     * @throws IllegalArgumentException if {@code clientId} is not positive
     * @throws RuntimeException         if the database operation fails
     */
    public List<Review> getReviewsByClient(int clientId) {
        Validator.positiveInt(clientId, "Client ID");

        List<Review> list = new ArrayList<>();

        try {
            PreparedStatement ps = DBConnection.prepareStatement(SELECT_BY_CLIENT_SQL);
            ps.clearParameters();
            ps.setInt(1, clientId);

            try (ResultSet rs = ps.executeQuery()) {
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
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reviews by client: " + e.getMessage(), e);
        }

        return list;
    }

    /**
     * Updates the rating/text/date of an existing review by {@code review_id}.
     *
     * @param reviewId   review identifier
     * @param rating     new rating (1..5)
     * @param reviewText new review text
     * @param reviewDate new review date
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         if the database operation fails
     */
    public void updateReview(int reviewId, int rating, String reviewText, Date reviewDate) {
    Validator.positiveInt(reviewId, "Review ID");
    if (rating < 1 || rating > 5) {
        throw new IllegalArgumentException("Rating must be between 1 and 5.");
    }
    Validator.notEmpty(reviewText, "Review text");
    Validator.validDate(reviewDate, "Review date");

    String sql =
            "UPDATE reviews r " +
            "SET VALUE(r) = review_t(" +
            "  ?, " +
            "  VALUE(r).client_ref, " +
            "  VALUE(r).product_ref, " +
            "  ?, ?, ?" +
            ") " +
            "WHERE VALUE(r).review_id = ?";

    try {
        PreparedStatement ps = DBConnection.prepareStatement(sql);
        ps.clearParameters();

        ps.setInt(1, reviewId);
        ps.setInt(2, rating);
        ps.setString(3, reviewText);
        ps.setDate(4, reviewDate);
        ps.setInt(5, reviewId);

        ps.executeUpdate();
    } catch (SQLException e) {
        throw new RuntimeException("Failed to update review: " + e.getMessage(), e);
    }
}

    /**
     * Deletes a review from the {@code reviews} object table by {@code review_id}.
     *
     * @param reviewId review identifier
     * @throws IllegalArgumentException if {@code reviewId} is not positive
     * @throws RuntimeException         if the database operation fails
     */
    public void deleteReview(int reviewId) {
        Validator.positiveInt(reviewId, "Review ID");

        try {
            PreparedStatement ps = DBConnection.prepareStatement(DELETE_SQL);
            ps.clearParameters();
            ps.setInt(1, reviewId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete review: " + e.getMessage(), e);
        }
    }

    /**
     * Resolves and returns {@code REF(c)} for the given {@code client_id}.
     *
     * @param clientId client identifier that must exist
     * @return Oracle REF to the client object row
     * @throws SQLException if the client does not exist or the query fails
     */
    private Object getClientRefOrThrow(int clientId) throws SQLException {
        Validator.positiveInt(clientId, "Client ID");

        PreparedStatement ps = DBConnection.prepareStatement(CLIENT_REF_SQL);
        ps.clearParameters();
        ps.setInt(1, clientId);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getObject(1);
            }
        }
        throw new SQLException("No client found with ID=" + clientId + " (REF not found)");
    }

    /**
     * Resolves and returns {@code REF(p)} for the given {@code product_id}.
     *
     * @param productId product identifier that must exist
     * @return Oracle REF to the product object row
     * @throws SQLException if the product does not exist or the query fails
     */
    private Object getProductRefOrThrow(int productId) throws SQLException {
        Validator.positiveInt(productId, "Product ID");

        PreparedStatement ps = DBConnection.prepareStatement(PRODUCT_REF_SQL);
        ps.clearParameters();
        ps.setInt(1, productId);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getObject(1);
            }
        }
        throw new SQLException("No product found with ID=" + productId + " (REF not found)");
    }
}