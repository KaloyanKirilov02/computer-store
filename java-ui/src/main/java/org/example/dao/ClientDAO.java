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
 * Data Access Object (DAO) for managing {@code clients} stored as Oracle object types (e.g. {@code client_t})
 * in the {@code clients} object table.
 *
 * <p>This DAO relies on {@link DBConnection} for prepared statement reuse and a shared connection strategy.</p>
 */
public class ClientDAO {

    private static final Logger LOGGER = Logger.getLogger(ClientDAO.class.getName());

    /**
     * Simple data holder representing a client row (object instance) read from the {@code clients} object table.
     */
    public static class Client {
        public final int id;
        public final String name;
        public final String email;
        public final String phone;
        public final String address;
        public final String type;

        /**
         * Creates a client DTO.
         *
         * @param id      client identifier
         * @param name    client name
         * @param email   client email
         * @param phone   client phone
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
     * Inserts a new client into the {@code clients} object table.
     *
     * @param clientId   client identifier
     * @param name       client name
     * @param email      client email
     * @param phone      client phone
     * @param address    client address
     * @param clientType client type
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         if the database operation fails
     */
    public void addClient(int clientId, String name, String email, String phone, String address, String clientType) {
        Validator.validateClient(clientId, name, email, phone);
        Validator.notEmpty(address, "Client address");
        Validator.notEmpty(clientType, "Client type");

        String sql = "INSERT INTO clients VALUES (client_t(?, ?, ?, ?, ?, ?))";

        try {
            PreparedStatement stmt = DBConnection.prepareStatement(sql);
            stmt.clearParameters();

            stmt.setInt(1, clientId);
            stmt.setString(2, name);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.setString(5, address);
            stmt.setString(6, clientType);

            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while adding client", e);
            throw new RuntimeException("Failed to add client: " + e.getMessage(), e);
        }
    }

    /**
     * Reads all clients from the {@code clients} object table.
     *
     * @return list of all clients (may be empty)
     * @throws RuntimeException if the database operation fails
     */
    public List<Client> getAllClients() {
        List<Client> clients = new ArrayList<>();
        String sql =
                "SELECT VALUE(c).client_id, VALUE(c).name, VALUE(c).email, " +
                        "VALUE(c).phone, VALUE(c).address, VALUE(c).client_type " +
                        "FROM clients c";

        try {
            PreparedStatement stmt = DBConnection.prepareStatement(sql);
            stmt.clearParameters();

            try (ResultSet rs = stmt.executeQuery()) {
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
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while fetching clients", e);
            throw new RuntimeException("Failed to fetch clients: " + e.getMessage(), e);
        }

        return clients;
    }

    /**
     * Updates an existing client in the {@code clients} object table by {@code client_id}.
     *
     * @param clientId   client identifier
     * @param name       new client name
     * @param email      new client email
     * @param phone      new client phone
     * @param address    new client address
     * @param clientType new client type
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         if the database operation fails
     */
    public void updateClient(int clientId, String name, String email, String phone, String address, String clientType) {
        Validator.validateClient(clientId, name, email, phone);
        Validator.notEmpty(address, "Client address");
        Validator.notEmpty(clientType, "Client type");

        String sql =
                "UPDATE clients c " +
                        "SET VALUE(c) = client_t(?, ?, ?, ?, ?, ?) " +
                        "WHERE VALUE(c).client_id = ?";

        try {
            PreparedStatement stmt = DBConnection.prepareStatement(sql);
            stmt.clearParameters();

            stmt.setInt(1, clientId);
            stmt.setString(2, name);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.setString(5, address);
            stmt.setString(6, clientType);

            stmt.setInt(7, clientId);

            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while updating client", e);
            throw new RuntimeException("Failed to update client: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a client from the {@code clients} object table by {@code client_id}.
     *
     * @param clientId client identifier
     * @throws IllegalArgumentException if {@code clientId} is not positive
     * @throws RuntimeException         if the database operation fails
     */
    public void deleteClient(int clientId) {
        Validator.positiveInt(clientId, "Client ID");

        String sql = "DELETE FROM clients c WHERE VALUE(c).client_id = ?";

        try {
            PreparedStatement stmt = DBConnection.prepareStatement(sql);
            stmt.clearParameters();

            stmt.setInt(1, clientId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while deleting client", e);
            throw new RuntimeException("Failed to delete client: " + e.getMessage(), e);
        }
    }
}