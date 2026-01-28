package org.example.dao;

import org.example.db.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for managing client entities in the database.
 * Uses Oracle object-relational features for persistence.
 */
public class ClientDAO {

    /**
     * Data holder representing a client entity.
     */
    public static class Client {

        public int id;
        public String name;
        public String email;
        public String phone;
        public String address;
        public String type;

        /**
         * Creates a new client data object.
         *
         * @param id      client identifier
         * @param name    client name
         * @param email   client email address
         * @param phone   client phone number
         * @param address client address
         * @param type    client type
         */
        public Client(int id, String name, String email, String phone, String address, String type) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.address = address;
            this.type = type;
        }
    }

    /**
     * Adds a new client to the database.
     *
     * @param clientId   unique client identifier
     * @param name       client name
     * @param email      client email address
     * @param phone      client phone number
     * @param address    client address
     * @param clientType client type
     */
    public void addClient(int clientId, String name, String email, String phone, String address, String clientType) {
        String sql = "INSERT INTO clients VALUES (client_t(?, ?, ?, ?, ?, ?))";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, clientId);
            stmt.setString(2, name);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.setString(5, address);
            stmt.setString(6, clientType);

            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all clients from the database.
     *
     * @return list of all clients
     */
    public List<Client> getAllClients() {
        List<Client> clients = new ArrayList<>();

        String sql = "SELECT " +
                "VALUE(c).client_id, " +
                "VALUE(c).name, " +
                "VALUE(c).email, " +
                "VALUE(c).phone, " +
                "VALUE(c).address, " +
                "VALUE(c).client_type " +
                "FROM clients c";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                clients.add(new Client(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getString(6)
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return clients;
    }

    /**
     * Updates an existing client in the database.
     *
     * @param clientId   identifier of the client to update
     * @param name       new client name
     * @param email      new client email address
     * @param phone      new client phone number
     * @param address    new client address
     * @param clientType new client type
     */
    public void updateClient(int clientId, String name, String email, String phone, String address, String clientType) {
        String sql = "UPDATE clients c SET " +
                "VALUE(c).name = ?, " +
                "VALUE(c).email = ?, " +
                "VALUE(c).phone = ?, " +
                "VALUE(c).address = ?, " +
                "VALUE(c).client_type = ? " +
                "WHERE VALUE(c).client_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, phone);
            stmt.setString(4, address);
            stmt.setString(5, clientType);
            stmt.setInt(6, clientId);

            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes a client from the database by its identifier.
     *
     * @param clientId identifier of the client to delete
     */
    public void deleteClient(int clientId) {
        String sql = "DELETE FROM clients c WHERE VALUE(c).client_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, clientId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
