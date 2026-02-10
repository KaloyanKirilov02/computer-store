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
 * Data Access Object (DAO) for managing products stored as Oracle object types in the {@code products} object table.
 *
 * <p>The {@code products} table contains subtypes such as {@code computer_t} and {@code accessory_t}. Category is stored
 * as an Oracle {@code REF} to {@code categories} and is resolved by {@code category_id} when inserting/updating.</p>
 *
 * <p>This DAO provides separate read methods for each subtype and a base update for common fields.</p>
 */
public class ProductDAO {

    private static final Logger LOGGER = Logger.getLogger(ProductDAO.class.getName());

    private static final String CATEGORY_REF_SQL =
            "SELECT REF(c) FROM categories c WHERE VALUE(c).category_id = ?";

    private static final String INSERT_COMPUTER_SQL =
            "INSERT INTO products VALUES (computer_t(?, ?, ?, ?, ?, ?, ?, ?, ?, ?))";

    private static final String INSERT_ACCESSORY_SQL =
            "INSERT INTO products VALUES (accessory_t(?, ?, ?, ?, ?, ?, ?, ?))";

    private static final String SELECT_COMPUTERS_SQL =
            "SELECT " +
                    "  VALUE(p).product_id, " +
                    "  VALUE(p).name, " +
                    "  VALUE(p).description, " +
                    "  VALUE(p).price, " +
                    "  VALUE(p).quantity_in_stock, " +
                    "  CASE WHEN VALUE(p).category IS NOT NULL THEN DEREF(VALUE(p).category).category_id ELSE NULL END AS category_id, " +
                    "  TREAT(VALUE(p) AS computer_t).cpu, " +
                    "  TREAT(VALUE(p) AS computer_t).ram, " +
                    "  TREAT(VALUE(p) AS computer_t).storage, " +
                    "  TREAT(VALUE(p) AS computer_t).gpu " +
                    "FROM products p " +
                    "WHERE VALUE(p) IS OF (computer_t)";

    private static final String SELECT_ACCESSORIES_SQL =
            "SELECT " +
                    "  VALUE(p).product_id, " +
                    "  VALUE(p).name, " +
                    "  VALUE(p).description, " +
                    "  VALUE(p).price, " +
                    "  VALUE(p).quantity_in_stock, " +
                    "  CASE WHEN VALUE(p).category IS NOT NULL THEN DEREF(VALUE(p).category).category_id ELSE NULL END AS category_id, " +
                    "  TREAT(VALUE(p) AS accessory_t).accessory_type, " +
                    "  TREAT(VALUE(p) AS accessory_t).compatibility " +
                    "FROM products p " +
                    "WHERE VALUE(p) IS OF (accessory_t)";

    private static final String UPDATE_BASE_SQL =
            "UPDATE products p " +
                    "SET VALUE(p).name = ?, " +
                    "    VALUE(p).description = ?, " +
                    "    VALUE(p).price = ?, " +
                    "    VALUE(p).quantity_in_stock = ?, " +
                    "    VALUE(p).category = ? " +
                    "WHERE VALUE(p).product_id = ?";

    private static final String DELETE_SQL =
            "DELETE FROM products p WHERE VALUE(p).product_id = ?";

    /**
     * DTO for products of subtype {@code computer_t}.
     */
    public static class Computer {
        public final int id;
        public final String name;
        public final String description;
        public final double price;
        public final int quantity;
        public final int categoryId;
        public final String cpu;
        public final String ram;
        public final String storage;
        public final String gpu;

        /**
         * Creates a computer DTO.
         *
         * @param id          product identifier
         * @param name        product name
         * @param description product description
         * @param price       product price
         * @param quantity    quantity in stock
         * @param categoryId  category identifier
         * @param cpu         CPU model/spec
         * @param ram         RAM spec
         * @param storage     storage spec
         * @param gpu         GPU model/spec
         */
        public Computer(int id, String name, String description, double price,
                        int quantity, int categoryId, String cpu, String ram, String storage, String gpu) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.quantity = quantity;
            this.categoryId = categoryId;
            this.cpu = cpu;
            this.ram = ram;
            this.storage = storage;
            this.gpu = gpu;
        }
    }

    /**
     * DTO for products of subtype {@code accessory_t}.
     */
    public static class Accessory {
        public final int id;
        public final String name;
        public final String description;
        public final double price;
        public final int quantity;
        public final int categoryId;
        public final String accessoryType;
        public final String compatibility;

        /**
         * Creates an accessory DTO.
         *
         * @param id            product identifier
         * @param name          product name
         * @param description   product description
         * @param price         product price
         * @param quantity      quantity in stock
         * @param categoryId    category identifier
         * @param accessoryType accessory type
         * @param compatibility compatibility notes
         */
        public Accessory(int id, String name, String description, double price,
                         int quantity, int categoryId, String accessoryType, String compatibility) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.quantity = quantity;
            this.categoryId = categoryId;
            this.accessoryType = accessoryType;
            this.compatibility = compatibility;
        }
    }

    /**
     * Resolves and returns {@code REF(c)} for the given {@code category_id}.
     *
     * @param categoryId category identifier that must exist
     * @return Oracle REF to the category object row
     * @throws SQLException if the category does not exist or the query fails
     */
    private Object getCategoryRefOrThrow(int categoryId) throws SQLException {
        PreparedStatement ps = DBConnection.prepareStatement(CATEGORY_REF_SQL);
        ps.clearParameters();
        ps.setInt(1, categoryId);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getObject(1);
            }
        }
        throw new SQLException("No category found with ID=" + categoryId + " (REF not found)");
    }

    /**
     * Converts a nullable JDBC value to {@link Integer}.
     *
     * <p>For Oracle, values coming from {@code CASE} expressions may arrive as {@code BigDecimal} or other
     * {@link Number} implementations, so {@link Number#intValue()} is used when applicable.</p>
     *
     * @param rs       result set
     * @param colIndex column index (1-based)
     * @return integer value, or {@code null} if the database value is {@code NULL}
     * @throws SQLException if reading from the result set fails
     */
    private Integer getNullableInt(ResultSet rs, int colIndex) throws SQLException {
        Object v = rs.getObject(colIndex);
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        return Integer.parseInt(v.toString());
    }

    /**
     * Ensures the category id is present in the current row.
     *
     * @param rs        result set
     * @param colIndex  column index (1-based)
     * @param productId product identifier used in the error message
     * @return non-null category id
     * @throws SQLException if category id is {@code NULL}
     */
    private int requireCategoryId(ResultSet rs, int colIndex, int productId) throws SQLException {
        Integer cat = getNullableInt(rs, colIndex);
        if (cat == null) {
            throw new SQLException("Product ID=" + productId + " has no category (category is NULL).");
        }
        return cat;
    }

    /**
     * Inserts a new computer product into {@code products} as subtype {@code computer_t}.
     *
     * @param productId   product identifier
     * @param name        product name
     * @param description product description
     * @param price       product price
     * @param quantity    quantity in stock
     * @param categoryId  category identifier
     * @param cpu         CPU model/spec
     * @param ram         RAM spec
     * @param storage     storage spec
     * @param gpu         GPU model/spec
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         if the database operation fails
     */
    public void addComputer(int productId, String name, String description, double price,
                            int quantity, int categoryId, String cpu, String ram, String storage, String gpu) {
        Validator.validateComputer(productId, name, description, price, quantity, categoryId, cpu, ram, storage, gpu);

        try {
            PreparedStatement ps = DBConnection.prepareStatement(INSERT_COMPUTER_SQL);
            ps.clearParameters();

            ps.setInt(1, productId);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setDouble(4, price);
            ps.setInt(5, quantity);
            ps.setObject(6, getCategoryRefOrThrow(categoryId));
            ps.setString(7, cpu);
            ps.setString(8, ram);
            ps.setString(9, storage);
            ps.setString(10, gpu);

            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while adding computer: " + productId, e);
            throw new RuntimeException("Failed to add computer: " + e.getMessage(), e);
        }
    }

    /**
     * Inserts a new accessory product into {@code products} as subtype {@code accessory_t}.
     *
     * @param productId      product identifier
     * @param name           product name
     * @param description    product description
     * @param price          product price
     * @param quantity       quantity in stock
     * @param categoryId     category identifier
     * @param accessoryType  accessory type
     * @param compatibility  compatibility notes
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         if the database operation fails
     */
    public void addAccessory(int productId, String name, String description, double price,
                             int quantity, int categoryId, String accessoryType, String compatibility) {
        Validator.validateAccessory(productId, name, description, price, quantity, categoryId, accessoryType, compatibility);

        try {
            PreparedStatement ps = DBConnection.prepareStatement(INSERT_ACCESSORY_SQL);
            ps.clearParameters();

            ps.setInt(1, productId);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setDouble(4, price);
            ps.setInt(5, quantity);
            ps.setObject(6, getCategoryRefOrThrow(categoryId));
            ps.setString(7, accessoryType);
            ps.setString(8, compatibility);

            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while adding accessory: " + productId, e);
            throw new RuntimeException("Failed to add accessory: " + e.getMessage(), e);
        }
    }

    /**
     * Reads all products of subtype {@code computer_t}.
     *
     * @return list of computers (may be empty)
     * @throws RuntimeException if the database operation fails
     */
    public List<Computer> getAllComputers() {
        List<Computer> list = new ArrayList<>();

        try {
            PreparedStatement ps = DBConnection.prepareStatement(SELECT_COMPUTERS_SQL);
            ps.clearParameters();

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int productId = rs.getInt(1);
                    int categoryId = requireCategoryId(rs, 6, productId);

                    list.add(new Computer(
                            productId,
                            rs.getString(2),
                            rs.getString(3),
                            rs.getDouble(4),
                            rs.getInt(5),
                            categoryId,
                            rs.getString(7),
                            rs.getString(8),
                            rs.getString(9),
                            rs.getString(10)
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while fetching computers", e);
            throw new RuntimeException("Failed to fetch computers: " + e.getMessage(), e);
        }

        return list;
    }

    /**
     * Reads all products of subtype {@code accessory_t}.
     *
     * @return list of accessories (may be empty)
     * @throws RuntimeException if the database operation fails
     */
    public List<Accessory> getAllAccessories() {
        List<Accessory> list = new ArrayList<>();

        try {
            PreparedStatement ps = DBConnection.prepareStatement(SELECT_ACCESSORIES_SQL);
            ps.clearParameters();

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int productId = rs.getInt(1);
                    int categoryId = requireCategoryId(rs, 6, productId);

                    list.add(new Accessory(
                            productId,
                            rs.getString(2),
                            rs.getString(3),
                            rs.getDouble(4),
                            rs.getInt(5),
                            categoryId,
                            rs.getString(7),
                            rs.getString(8)
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while fetching accessories", e);
            throw new RuntimeException("Failed to fetch accessories: " + e.getMessage(), e);
        }

        return list;
    }

    /**
     * Updates the common/base product fields for any product subtype.
     *
     * @param productId   product identifier
     * @param name        new product name
     * @param description new product description
     * @param price       new product price
     * @param quantity    new quantity in stock
     * @param categoryId  new category identifier
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         if the database operation fails
     */
    public void updateBaseProduct(int productId, String name, String description, double price,
                              int quantity, int categoryId) {
    Validator.validateCategory(categoryId, "Category ID");
    Validator.positiveInt(productId, "Product ID");
    Validator.notEmpty(name, "Product name");
    Validator.notEmpty(description, "Product description");
    Validator.positiveDouble(price, "Product price");
    Validator.nonNegativeInt(quantity, "Quantity");

    String sql =
            "UPDATE products p " +
            "SET VALUE(p) = product_t(?, ?, ?, ?, ?, ?) " +
            "WHERE VALUE(p).product_id = ?";

    try {
        PreparedStatement ps = DBConnection.prepareStatement(sql);
        ps.clearParameters();

        ps.setInt(1, productId);
        ps.setString(2, name);
        ps.setString(3, description);
        ps.setDouble(4, price);
        ps.setInt(5, quantity);
        ps.setObject(6, getCategoryRefOrThrow(categoryId));

        ps.setInt(7, productId);

        ps.executeUpdate();
    } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, "Error while updating product", e);
        throw new RuntimeException("Failed to update product: " + e.getMessage(), e);
    }
}

    /**
     * Deletes a product from {@code products} by {@code product_id}.
     *
     * @param productId product identifier
     * @throws IllegalArgumentException if {@code productId} is not positive
     * @throws RuntimeException         if the database operation fails
     */
    public void deleteProduct(int productId) {
        Validator.positiveInt(productId, "Product ID");

        try {
            PreparedStatement ps = DBConnection.prepareStatement(DELETE_SQL);
            ps.clearParameters();
            ps.setInt(1, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while deleting product: " + productId, e);
            throw new RuntimeException("Failed to delete product: " + e.getMessage(), e);
        }
    }
}