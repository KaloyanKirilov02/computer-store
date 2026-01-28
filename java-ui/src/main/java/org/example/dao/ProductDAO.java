package org.example.dao;

import org.example.db.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for managing products (Computers and Accessories) in the database.
 * Supports CRUD operations and maintains references to product categories.
 */
public class ProductDAO {

    /**
     * Adds a new computer product to the database.
     *
     * @param productId   product identifier
     * @param name        product name
     * @param description product description
     * @param price       product price
     * @param quantity    quantity in stock
     * @param categoryId  associated category ID
     * @param cpu         CPU specification
     * @param ram         RAM specification
     * @param storage     storage specification
     * @param gpu         GPU specification
     */
    public void addComputer(int productId, String name, String description, double price,
                            int quantity, int categoryId, String cpu, String ram, String storage, String gpu) {
        String sql = """
            INSERT INTO products
            VALUES (
                computer_t(
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                )
            )
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            stmt.setString(2, name);
            stmt.setString(3, description);
            stmt.setDouble(4, price);
            stmt.setInt(5, quantity);
            stmt.setObject(6, getCategoryRef(conn, categoryId));
            stmt.setString(7, cpu);
            stmt.setString(8, ram);
            stmt.setString(9, storage);
            stmt.setString(10, gpu);

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a new accessory product to the database.
     *
     * @param productId     product identifier
     * @param name          product name
     * @param description   product description
     * @param price         product price
     * @param quantity      quantity in stock
     * @param categoryId    associated category ID
     * @param accessoryType type of accessory
     * @param compatibility compatibility information
     */
    public void addAccessory(int productId, String name, String description, double price,
                             int quantity, int categoryId, String accessoryType, String compatibility) {
        String sql = """
            INSERT INTO products
            VALUES (
                accessory_t(
                    ?, ?, ?, ?, ?, ?, ?, ?
                )
            )
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            stmt.setString(2, name);
            stmt.setString(3, description);
            stmt.setDouble(4, price);
            stmt.setInt(5, quantity);
            stmt.setObject(6, getCategoryRef(conn, categoryId));
            stmt.setString(7, accessoryType);
            stmt.setString(8, compatibility);

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all computers from the database.
     *
     * @return list of Computer objects
     */
    public List<Computer> getAllComputers() {
        List<Computer> list = new ArrayList<>();

        String sql = """
            SELECT
                p.product_id,
                p.name,
                p.description,
                p.price,
                p.quantity_in_stock,
                DEREF(p.category).category_id,
                TREAT(VALUE(p) AS computer_t).cpu,
                TREAT(VALUE(p) AS computer_t).ram,
                TREAT(VALUE(p) AS computer_t).storage,
                TREAT(VALUE(p) AS computer_t).gpu
            FROM products p
            WHERE VALUE(p) IS OF (computer_t)
        """;

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Computer(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getDouble(4),
                        rs.getInt(5),
                        rs.getInt(6),
                        rs.getString(7),
                        rs.getString(8),
                        rs.getString(9),
                        rs.getString(10)
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Retrieves all accessories from the database.
     *
     * @return list of Accessory objects
     */
    public List<Accessory> getAllAccessories() {
        List<Accessory> list = new ArrayList<>();

        String sql = """
            SELECT
                p.product_id,
                p.name,
                p.description,
                p.price,
                p.quantity_in_stock,
                DEREF(p.category).category_id,
                TREAT(VALUE(p) AS accessory_t).accessory_type,
                TREAT(VALUE(p) AS accessory_t).compatibility
            FROM products p
            WHERE VALUE(p) IS OF (accessory_t)
        """;

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Accessory(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getDouble(4),
                        rs.getInt(5),
                        rs.getInt(6),
                        rs.getString(7),
                        rs.getString(8)
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Updates the base attributes of a product (works for both subtypes).
     *
     * @param productId   product identifier
     * @param name        new name
     * @param description new description
     * @param price       new price
     * @param quantity    new quantity in stock
     * @param categoryId  new category ID
     */
    public void updateBaseProduct(int productId, String name, String description, double price,
                                  int quantity, int categoryId) {
        String sql = """
            UPDATE products p
            SET
                p.name = ?,
                p.description = ?,
                p.price = ?,
                p.quantity_in_stock = ?,
                p.category = ?
            WHERE p.product_id = ?
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.setDouble(3, price);
            stmt.setInt(4, quantity);
            stmt.setObject(5, getCategoryRef(conn, categoryId));
            stmt.setInt(6, productId);

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes a product from the database by its ID.
     *
     * @param productId product identifier
     */
    public void deleteProduct(int productId) {
        String sql = "DELETE FROM products p WHERE p.product_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to retrieve a REF object for a category.
     *
     * @param conn       database connection
     * @param categoryId category ID
     * @return database REF object for the category, or null if not found
     * @throws SQLException if a database error occurs
     */
    private Object getCategoryRef(Connection conn, int categoryId) throws SQLException {
        String sql = "SELECT REF(c) FROM categories c WHERE c.category_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, categoryId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getObject(1);
            }
        }
        return null;
    }

    /**
     * Data holder representing a computer product.
     */
    public static class Computer {
        public int id;
        public String name;
        public String description;
        public double price;
        public int quantity;
        public int categoryId;
        public String cpu;
        public String ram;
        public String storage;
        public String gpu;

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
     * Data holder representing an accessory product.
     */
    public static class Accessory {
        public int id;
        public String name;
        public String description;
        public double price;
        public int quantity;
        public int categoryId;
        public String accessoryType;
        public String compatibility;

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
}
