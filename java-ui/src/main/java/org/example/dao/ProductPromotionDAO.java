package org.example.dao;

import org.example.db.DBConnection;
import org.example.util.Validator;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object (DAO) for managing product-promotion associations stored as Oracle object types
 * (e.g. {@code product_promotion_t}) in the {@code product_promotions} object table.
 *
 * <p>Each association references a product and a promotion via Oracle {@code REF}s. This DAO resolves those
 * references by IDs before insert/update and extracts IDs via {@code DEREF(...).<id>} when reading. A "full"
 * query is also provided that returns product/promotion names, discount percent, and a calculated discounted price.</p>
 */
public class ProductPromotionDAO {

    private static final Logger LOGGER = Logger.getLogger(ProductPromotionDAO.class.getName());

    private static final String INSERT_SQL =
            "INSERT INTO product_promotions VALUES (product_promotion_t(?, ?, ?))";

    private static final String UPDATE_SQL =
            "UPDATE product_promotions pp " +
                    "SET VALUE(pp).product_ref=?, VALUE(pp).promotion_ref=? " +
                    "WHERE VALUE(pp).product_promotion_id=?";

    private static final String DELETE_SQL =
            "DELETE FROM product_promotions pp WHERE VALUE(pp).product_promotion_id=?";

    private static final String SELECT_ALL_SQL =
            "SELECT VALUE(pp).product_promotion_id, " +
                    "       CASE WHEN VALUE(pp).product_ref   IS NOT NULL THEN DEREF(VALUE(pp).product_ref).product_id ELSE NULL END AS product_id, " +
                    "       CASE WHEN VALUE(pp).promotion_ref IS NOT NULL THEN DEREF(VALUE(pp).promotion_ref).promotion_id ELSE NULL END AS promotion_id " +
                    "FROM product_promotions pp";

    private static final String SELECT_ALL_FULL_SQL =
            "SELECT " +
                    "  VALUE(pp).product_promotion_id, " +
                    "  CASE WHEN VALUE(pp).product_ref   IS NOT NULL THEN DEREF(VALUE(pp).product_ref).product_id ELSE NULL END AS product_id, " +
                    "  CASE WHEN VALUE(pp).promotion_ref IS NOT NULL THEN DEREF(VALUE(pp).promotion_ref).promotion_id ELSE NULL END AS promotion_id, " +
                    "  CASE WHEN VALUE(pp).product_ref   IS NOT NULL THEN DEREF(VALUE(pp).product_ref).name ELSE NULL END AS product_name, " +
                    "  CASE WHEN VALUE(pp).promotion_ref IS NOT NULL THEN DEREF(VALUE(pp).promotion_ref).name ELSE NULL END AS promotion_name, " +
                    "  CASE WHEN VALUE(pp).promotion_ref IS NOT NULL THEN DEREF(VALUE(pp).promotion_ref).discount_percent ELSE NULL END AS discount_percent, " +
                    "  CASE " +
                    "    WHEN VALUE(pp).product_ref IS NOT NULL AND VALUE(pp).promotion_ref IS NOT NULL " +
                    "    THEN DEREF(VALUE(pp).product_ref).price * (1 - DEREF(VALUE(pp).promotion_ref).discount_percent/100) " +
                    "    ELSE NULL " +
                    "  END AS discounted_price " +
                    "FROM product_promotions pp";

    private static final String SELECT_BY_PRODUCT_SQL =
            "SELECT VALUE(pp).product_promotion_id, " +
                    "       CASE WHEN VALUE(pp).product_ref   IS NOT NULL THEN DEREF(VALUE(pp).product_ref).product_id ELSE NULL END AS product_id, " +
                    "       CASE WHEN VALUE(pp).promotion_ref IS NOT NULL THEN DEREF(VALUE(pp).promotion_ref).promotion_id ELSE NULL END AS promotion_id " +
                    "FROM product_promotions pp " +
                    "WHERE VALUE(pp).product_ref = ?";

    private static final String PRODUCT_REF_SQL =
            "SELECT REF(p) FROM products p WHERE VALUE(p).product_id=?";

    private static final String PROMOTION_REF_SQL =
            "SELECT REF(pr) FROM promotions pr WHERE VALUE(pr).promotion_id=?";

    /**
     * Basic DTO for a product-promotion association.
     */
    public static class ProductPromotion {
        public final int id;
        public final int productId;
        public final int promotionId;

        /**
         * Creates a product-promotion DTO.
         *
         * @param id          association identifier
         * @param productId   referenced product id
         * @param promotionId referenced promotion id
         */
        public ProductPromotion(int id, int productId, int promotionId) {
            this.id = id;
            this.productId = productId;
            this.promotionId = promotionId;
        }
    }

    /**
     * Extended DTO for a product-promotion association including human-readable details and calculated price.
     */
    public static class ProductPromotionFull {
        public final int id;
        public final int productId;
        public final int promotionId;

        public final String productName;
        public final String promotionName;
        public final double promotionDiscount;
        public final double discountedPrice;

        /**
         * Creates a detailed product-promotion DTO.
         *
         * @param id                association identifier
         * @param productId         referenced product id
         * @param promotionId       referenced promotion id
         * @param productName       product name (may be {@code null})
         * @param promotionName     promotion name (may be {@code null})
         * @param promotionDiscount discount percent (defaults can be applied by caller)
         * @param discountedPrice   calculated discounted price (defaults can be applied by caller)
         */
        public ProductPromotionFull(int id, int productId, int promotionId,
                                    String productName, String promotionName,
                                    double promotionDiscount, double discountedPrice) {
            this.id = id;
            this.productId = productId;
            this.promotionId = promotionId;
            this.productName = productName;
            this.promotionName = promotionName;
            this.promotionDiscount = promotionDiscount;
            this.discountedPrice = discountedPrice;
        }
    }

    /**
     * Resolves and returns {@code REF(p)} for the given {@code product_id}.
     *
     * @param productId product identifier that must exist
     * @return Oracle REF to the product object row
     * @throws SQLException if the product does not exist or the query fails
     */
    private Object getProductRefOrThrow(int productId) throws SQLException {
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

    /**
     * Resolves and returns {@code REF(pr)} for the given {@code promotion_id}.
     *
     * @param promotionId promotion identifier that must exist
     * @return Oracle REF to the promotion object row
     * @throws SQLException if the promotion does not exist or the query fails
     */
    private Object getPromotionRefOrThrow(int promotionId) throws SQLException {
        PreparedStatement ps = DBConnection.prepareStatement(PROMOTION_REF_SQL);
        ps.clearParameters();
        ps.setInt(1, promotionId);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getObject(1);
            }
        }
        throw new SQLException("No promotion found with ID=" + promotionId + " (REF not found)");
    }

    /**
     * Converts a nullable JDBC value to {@link Integer}.
     *
     * <p>Oracle {@code NUMBER} values commonly arrive as {@code BigDecimal} or other {@link Number} implementations.</p>
     *
     * @param rs  result set
     * @param col column index (1-based)
     * @return integer value, or {@code null} if the database value is {@code NULL}
     * @throws SQLException if reading from the result set fails
     */
    private Integer getNullableInt(ResultSet rs, int col) throws SQLException {
        Object v = rs.getObject(col);
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        return Integer.parseInt(v.toString());
    }

    /**
     * Converts a nullable JDBC value to {@link Double}.
     *
     * @param rs  result set
     * @param col column index (1-based)
     * @return double value, or {@code null} if the database value is {@code NULL}
     * @throws SQLException if reading from the result set fails
     */
    private Double getNullableDouble(ResultSet rs, int col) throws SQLException {
        Object v = rs.getObject(col);
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        return Double.parseDouble(v.toString());
    }

    /**
     * Inserts a new product-promotion association into {@code product_promotions}.
     *
     * @param id          association identifier
     * @param productId   referenced product id
     * @param promotionId referenced promotion id
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         if the database operation fails
     */
    public void addProductPromotion(int id, int productId, int promotionId) {
        Validator.validateProductPromotion(id, productId, promotionId);

        try {
            PreparedStatement ps = DBConnection.prepareStatement(INSERT_SQL);
            ps.clearParameters();

            ps.setInt(1, id);
            ps.setObject(2, getProductRefOrThrow(productId));
            ps.setObject(3, getPromotionRefOrThrow(promotionId));

            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while adding product_promotion id=" + id, e);
            throw new RuntimeException("Failed to add promotion to product: " + e.getMessage(), e);
        }
    }

    /**
     * Reads all product-promotion associations from {@code product_promotions}.
     *
     * @return list of all associations (may be empty)
     * @throws RuntimeException if the database operation fails or invalid rows are encountered
     */
    public List<ProductPromotion> getAll() {
        List<ProductPromotion> list = new ArrayList<>();

        try {
            PreparedStatement ps = DBConnection.prepareStatement(SELECT_ALL_SQL);
            ps.clearParameters();

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = getNullableInt(rs, 1);
                    Integer productId = getNullableInt(rs, 2);
                    Integer promotionId = getNullableInt(rs, 3);

                    if (productId == null || promotionId == null) {
                        throw new SQLException("Invalid row in product_promotions: NULL refs (id=" + id + ")");
                    }

                    list.add(new ProductPromotion(id, productId, promotionId));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while fetching product_promotions", e);
            throw new RuntimeException("Failed to fetch promotions: " + e.getMessage(), e);
        }

        return list;
    }

    /**
     * Reads all product-promotion associations with additional details and calculated discounted price.
     *
     * @return list of detailed associations (may be empty)
     * @throws RuntimeException if the database operation fails or invalid rows are encountered
     */
    public List<ProductPromotionFull> getAllFull() {
        List<ProductPromotionFull> list = new ArrayList<>();

        try {
            PreparedStatement ps = DBConnection.prepareStatement(SELECT_ALL_FULL_SQL);
            ps.clearParameters();

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = getNullableInt(rs, 1);
                    Integer productId = getNullableInt(rs, 2);
                    Integer promotionId = getNullableInt(rs, 3);

                    String productName = rs.getString(4);
                    String promotionName = rs.getString(5);

                    Double discount = getNullableDouble(rs, 6);
                    Double discountedPrice = getNullableDouble(rs, 7);

                    if (productId == null || promotionId == null) {
                        throw new SQLException("Invalid row in product_promotions: NULL refs (id=" + id + ")");
                    }

                    list.add(new ProductPromotionFull(
                            id,
                            productId,
                            promotionId,
                            productName,
                            promotionName,
                            discount != null ? discount : 0.0,
                            discountedPrice != null ? discountedPrice : 0.0
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while fetching product_promotions full", e);
            throw new RuntimeException("Failed to fetch full promotions: " + e.getMessage(), e);
        }

        return list;
    }

    /**
     * Reads product-promotion associations for a specific product.
     *
     * @param productId product identifier
     * @return list of associations for the product (may be empty)
     * @throws IllegalArgumentException if {@code productId} is not positive
     * @throws RuntimeException         if the database operation fails
     */
    public List<ProductPromotion> getByProductId(int productId) {
        Validator.positiveInt(productId, "Product ID");

        List<ProductPromotion> list = new ArrayList<>();

        try {
            PreparedStatement ps = DBConnection.prepareStatement(SELECT_BY_PRODUCT_SQL);
            ps.clearParameters();
            ps.setObject(1, getProductRefOrThrow(productId));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = getNullableInt(rs, 1);
                    Integer pid = getNullableInt(rs, 2);
                    Integer prId = getNullableInt(rs, 3);

                    if (pid == null || prId == null) {
                        throw new SQLException("Invalid row in product_promotions: NULL refs (id=" + id + ")");
                    }

                    list.add(new ProductPromotion(id, pid, prId));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while fetching by productId=" + productId, e);
            throw new RuntimeException("Failed to fetch by Product ID: " + e.getMessage(), e);
        }

        return list;
    }

    /**
     * Updates an existing product-promotion association by {@code product_promotion_id}.
     *
     * @param id          association identifier
     * @param productId   new referenced product id
     * @param promotionId new referenced promotion id
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         if the database operation fails
     */
    public void updateProductPromotion(int id, int productId, int promotionId) {
    Validator.validateProductPromotion(id, productId, promotionId);

    String sql =
            "UPDATE product_promotions pp " +
            "SET VALUE(pp) = product_promotion_t(?, ?, ?) " +
            "WHERE VALUE(pp).product_promotion_id = ?";

    try {
        PreparedStatement ps = DBConnection.prepareStatement(sql);
        ps.clearParameters();

        ps.setInt(1, id);
        ps.setObject(2, getProductRefOrThrow(productId));
        ps.setObject(3, getPromotionRefOrThrow(promotionId));
        ps.setInt(4, id);

        ps.executeUpdate();
    } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, "Error while updating product_promotion id=" + id, e);
        throw new RuntimeException("Failed to update: " + e.getMessage(), e);
    }
}

    /**
     * Deletes a product-promotion association by {@code product_promotion_id}.
     *
     * @param id association identifier
     * @throws IllegalArgumentException if {@code id} is not positive
     * @throws RuntimeException         if the database operation fails
     */
    public void deleteProductPromotion(int id) {
        Validator.positiveInt(id, "ProductPromotion ID");

        try {
            PreparedStatement ps = DBConnection.prepareStatement(DELETE_SQL);
            ps.clearParameters();
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while deleting product_promotion id=" + id, e);
            throw new RuntimeException("Failed to delete: " + e.getMessage(), e);
        }
    }
}