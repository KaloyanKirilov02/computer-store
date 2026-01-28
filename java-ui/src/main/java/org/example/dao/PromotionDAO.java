package org.example.dao;

import org.example.db.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for managing promotions in the database.
 * Supports CRUD operations and retrieving promotion details.
 */
public class PromotionDAO {

    /**
     * Represents a promotion entity.
     */
    public static class Promotion {
        public int id;
        public String name;
        public double discountPercent;
        public Date startDate;
        public Date endDate;

        public Promotion(int id, String name, double discountPercent, Date startDate, Date endDate) {
            this.id = id;
            this.name = name;
            this.discountPercent = discountPercent;
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }

    /**
     * Adds a new promotion to the database.
     *
     * @param promotionId     ID of the promotion
     * @param name            promotion name
     * @param discountPercent discount percentage
     * @param startDate       start date of the promotion
     * @param endDate         end date of the promotion
     */
    public void addPromotion(int promotionId, String name, double discountPercent, Date startDate, Date endDate) {
        String sql = "INSERT INTO promotions VALUES (promotion_t(?, ?, ?, ?, ?))";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, promotionId);
            stmt.setString(2, name);
            stmt.setDouble(3, discountPercent);
            stmt.setDate(4, startDate);
            stmt.setDate(5, endDate);

            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all promotions as full objects.
     *
     * @return list of Promotion objects
     */
    public List<Promotion> getAllPromotionsFull() {
        List<Promotion> list = new ArrayList<>();
        String sql = "SELECT VALUE(p).promotion_id, VALUE(p).name, VALUE(p).discount_percent, VALUE(p).start_date, VALUE(p).end_date FROM promotions p";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Promotion(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getDouble(3),
                        rs.getDate(4),
                        rs.getDate(5)
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Updates an existing promotion in the database.
     *
     * @param promotionId     ID of the promotion
     * @param name            promotion name
     * @param discountPercent discount percentage
     * @param startDate       start date of the promotion
     * @param endDate         end date of the promotion
     */
    public void updatePromotion(int promotionId, String name, double discountPercent, Date startDate, Date endDate) {
        String sql = "UPDATE promotions p SET VALUE(p).name=?, VALUE(p).discount_percent=?, VALUE(p).start_date=?, VALUE(p).end_date=? WHERE VALUE(p).promotion_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            stmt.setDouble(2, discountPercent);
            stmt.setDate(3, startDate);
            stmt.setDate(4, endDate);
            stmt.setInt(5, promotionId);

            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes a promotion from the database.
     *
     * @param promotionId ID of the promotion to delete
     */
    public void deletePromotion(int promotionId) {
        String sql = "DELETE FROM promotions p WHERE VALUE(p).promotion_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, promotionId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
