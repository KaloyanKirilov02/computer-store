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
 * Data Access Object (DAO) for managing promotions stored as Oracle object types (e.g. {@code promotion_t})
 * in the {@code promotions} object table.
 */
public class PromotionDAO {

    private static final String INSERT_SQL =
            "INSERT INTO promotions VALUES (promotion_t(?, ?, ?, ?, ?))";

    private static final String SELECT_ALL_SQL =
            "SELECT VALUE(p).promotion_id, VALUE(p).name, VALUE(p).discount_percent, VALUE(p).start_date, VALUE(p).end_date " +
                    "FROM promotions p";

    private static final String UPDATE_SQL =
            "UPDATE promotions p " +
                    "SET VALUE(p).name=?, VALUE(p).discount_percent=?, VALUE(p).start_date=?, VALUE(p).end_date=? " +
                    "WHERE VALUE(p).promotion_id=?";

    private static final String DELETE_SQL =
            "DELETE FROM promotions p WHERE VALUE(p).promotion_id=?";

    /**
     * Simple data holder representing a promotion row (object instance) read from the {@code promotions} object table.
     */
    public static class Promotion {
        public final int id;
        public final String name;
        public final double discountPercent;
        public final Date startDate;
        public final Date endDate;

        /**
         * Creates a promotion DTO.
         *
         * @param id              promotion identifier
         * @param name            promotion name
         * @param discountPercent discount percentage
         * @param startDate       promotion start date
         * @param endDate         promotion end date
         */
        public Promotion(int id, String name, double discountPercent, Date startDate, Date endDate) {
            this.id = id;
            this.name = name;
            this.discountPercent = discountPercent;
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }

    /**
     * Inserts a new promotion into the {@code promotions} object table.
     *
     * @param promotionId     promotion identifier
     * @param name            promotion name
     * @param discountPercent discount percentage
     * @param startDate       promotion start date
     * @param endDate         promotion end date
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         if the database operation fails
     */
    public void addPromotion(int promotionId, String name, double discountPercent, Date startDate, Date endDate) {
        Validator.validatePromotion(promotionId, name, discountPercent, startDate, endDate);

        try {
            PreparedStatement ps = DBConnection.prepareStatement(INSERT_SQL);
            ps.clearParameters();

            ps.setInt(1, promotionId);
            ps.setString(2, name);
            ps.setDouble(3, discountPercent);
            ps.setDate(4, startDate);
            ps.setDate(5, endDate);

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add promotion: " + e.getMessage(), e);
        }
    }

    /**
     * Reads all promotions from the {@code promotions} object table.
     *
     * @return list of all promotions (may be empty)
     * @throws RuntimeException if the database operation fails
     */
    public List<Promotion> getAllPromotionsFull() {
        List<Promotion> list = new ArrayList<>();

        try {
            PreparedStatement ps = DBConnection.prepareStatement(SELECT_ALL_SQL);
            ps.clearParameters();

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Promotion(
                            rs.getInt(1),
                            rs.getString(2),
                            rs.getDouble(3),
                            rs.getDate(4),
                            rs.getDate(5)
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch promotions: " + e.getMessage(), e);
        }

        return list;
    }

    /**
     * Updates an existing promotion in the {@code promotions} object table by {@code promotion_id}.
     *
     * @param promotionId     promotion identifier
     * @param name            new promotion name
     * @param discountPercent new discount percentage
     * @param startDate       new promotion start date
     * @param endDate         new promotion end date
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         if the database operation fails
     */
    public void updatePromotion(int promotionId, String name, double discountPercent, Date startDate, Date endDate) {
    Validator.validatePromotion(promotionId, name, discountPercent, startDate, endDate);

    String sql =
            "UPDATE promotions p " +
            "SET VALUE(p) = promotion_t(?, ?, ?, ?, ?) " +
            "WHERE VALUE(p).promotion_id = ?";

    try {
        PreparedStatement ps = DBConnection.prepareStatement(sql);
        ps.clearParameters();

        ps.setInt(1, promotionId);
        ps.setString(2, name);
        ps.setDouble(3, discountPercent);
        ps.setDate(4, startDate);
        ps.setDate(5, endDate);
        ps.setInt(6, promotionId);

        ps.executeUpdate();
    } catch (SQLException e) {
        throw new RuntimeException("Failed to update promotion: " + e.getMessage(), e);
    }
}

    /**
     * Deletes a promotion from the {@code promotions} object table by {@code promotion_id}.
     *
     * @param promotionId promotion identifier
     * @throws IllegalArgumentException if {@code promotionId} is not positive
     * @throws RuntimeException         if the database operation fails
     */
    public void deletePromotion(int promotionId) {
        Validator.positiveInt(promotionId, "Promotion ID");

        try {
            PreparedStatement ps = DBConnection.prepareStatement(DELETE_SQL);
            ps.clearParameters();
            ps.setInt(1, promotionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete promotion: " + e.getMessage(), e);
        }
    }
}