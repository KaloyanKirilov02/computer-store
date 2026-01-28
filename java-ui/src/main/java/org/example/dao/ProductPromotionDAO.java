package org.example.dao;

import org.example.db.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for managing product promotions in the database.
 * Supports CRUD operations and retrieving detailed information for display.
 */
public class ProductPromotionDAO {

    /**
     * Represents a basic product-promotion relationship.
     */
    public static class ProductPromotion {
        public int id;
        public int productId;
        public int promotionId;

        public ProductPromotion(int id, int productId, int promotionId) {
            this.id = id;
            this.productId = productId;
            this.promotionId = promotionId;
        }
    }

    /**
     * Represents a product-promotion relationship with full display information.
     */
    public static class ProductPromotionFull {
        public int id;
        public String productName;
        public String promotionName;
        public double promotionDiscount;
        public double discountedPrice;

        public ProductPromotionFull(int id, String productName, String promotionName,
                                    double promotionDiscount, double discountedPrice) {
            this.id = id;
            this.productName = productName;
            this.promotionName = promotionName;
            this.promotionDiscount = promotionDiscount;
            this.discountedPrice = discountedPrice;
        }
    }

    /**
     * Adds a new product-promotion mapping.
     *
     * @param id          mapping ID
     * @param productId   product ID
     * @param promotionId promotion ID
     */
    public void addProductPromotion(int id, int productId, int promotionId) {
        String sql = "INSERT INTO product_promotions VALUES (product_promotion_t(?, ?, ?))";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.setObject(2, getRef(conn, "products", "product_id", productId));
            stmt.setObject(3, getRef(conn, "promotions", "promotion_id", promotionId));

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all product-promotion mappings (basic).
     *
     * @return list of ProductPromotion objects
     */
    public List<ProductPromotion> getAll() {
        List<ProductPromotion> list = new ArrayList<>();
        String sql = "SELECT VALUE(pp).product_promotion_id, " +
                "       DEREF(VALUE(pp).product_ref).product_id, " +
                "       DEREF(VALUE(pp).promotion_ref).promotion_id " +
                "FROM product_promotions pp";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new ProductPromotion(
                        rs.getInt(1),
                        rs.getInt(2),
                        rs.getInt(3)
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Retrieves all product-promotion mappings with full display information.
     *
     * @return list of ProductPromotionFull objects
     */
    public List<ProductPromotionFull> getAllFull() {
        List<ProductPromotionFull> list = new ArrayList<>();
        String sql = "SELECT " +
                " VALUE(pp).product_promotion_id, " +
                " DEREF(VALUE(pp).product_ref).name, " +
                " DEREF(VALUE(pp).promotion_ref).name, " +
                " DEREF(VALUE(pp).promotion_ref).discount_percent, " +
                " DEREF(VALUE(pp).product_ref).price * (1 - DEREF(VALUE(pp).promotion_ref).discount_percent/100) AS discounted_price " +
                "FROM product_promotions pp";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new ProductPromotionFull(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getDouble(4),
                        rs.getDouble(5)
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Retrieves product-promotion mappings by product ID.
     *
     * @param productId product ID
     * @return list of ProductPromotion objects
     */
    public List<ProductPromotion> getByProductId(int productId) {
        List<ProductPromotion> list = new ArrayList<>();
        String sql = "SELECT VALUE(pp).product_promotion_id, " +
                "       DEREF(VALUE(pp).product_ref).product_id, " +
                "       DEREF(VALUE(pp).promotion_ref).promotion_id " +
                "FROM product_promotions pp " +
                "WHERE VALUE(pp).product_ref = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, getRef(conn, "products", "product_id", productId));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(new ProductPromotion(
                        rs.getInt(1),
                        rs.getInt(2),
                        rs.getInt(3)
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Updates an existing product-promotion mapping.
     *
     * @param id          mapping ID
     * @param productId   product ID
     * @param promotionId promotion ID
     */
    public void updateProductPromotion(int id, int productId, int promotionId) {
        String sql = "UPDATE product_promotions pp SET VALUE(pp).product_ref=?, VALUE(pp).promotion_ref=? " +
                "WHERE VALUE(pp).product_promotion_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, getRef(conn, "products", "product_id", productId));
            stmt.setObject(2, getRef(conn, "promotions", "promotion_id", promotionId));
            stmt.setInt(3, id);

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes a product-promotion mapping by ID.
     *
     * @param id mapping ID
     */
    public void deleteProductPromotion(int id) {
        String sql = "DELETE FROM product_promotions pp WHERE VALUE(pp).product_promotion_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to get a database REF object for a row.
     *
     * @param conn       database connection
     * @param tableName  table name
     * @param idColumn   ID column name
     * @param id         row ID
     * @return REF object for the row
     * @throws SQLException if the REF is not found
     */
    private Object getRef(Connection conn, String tableName, String idColumn, int id) throws SQLException {
        String sql = "SELECT REF(t) FROM " + tableName + " t WHERE VALUE(t)." + idColumn + "=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getObject(1);
        }
        throw new SQLException("REF not found for " + tableName + " id=" + id);
    }
}
