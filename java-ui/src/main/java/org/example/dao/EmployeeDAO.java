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
 * Data Access Object (DAO) for managing employees stored as Oracle object types (e.g. {@code employee_t})
 * in the {@code employees} object table.
 */
public class EmployeeDAO {

    private static final Logger LOGGER = Logger.getLogger(EmployeeDAO.class.getName());

    private static final String INSERT_SQL =
            "INSERT INTO employees VALUES (employee_t(?, ?, ?))";

    private static final String SELECT_ALL_SQL =
            "SELECT VALUE(e).employee_id, VALUE(e).name, VALUE(e).position FROM employees e";

    private static final String UPDATE_SQL =
            "UPDATE employees e SET VALUE(e).name = ?, VALUE(e).position = ? " +
                    "WHERE VALUE(e).employee_id = ?";

    private static final String DELETE_SQL =
            "DELETE FROM employees e WHERE VALUE(e).employee_id = ?";

    /**
     * Simple data holder representing an employee row (object instance) read from the {@code employees} object table.
     */
    public static class Employee {
        public final int id;
        public final String name;
        public final String position;

        /**
         * Creates an employee DTO.
         *
         * @param id       employee identifier
         * @param name     employee name
         * @param position employee position/title
         */
        public Employee(int id, String name, String position) {
            this.id = id;
            this.name = name;
            this.position = position;
        }
    }

    /**
     * Inserts a new employee into the {@code employees} object table.
     *
     * @param employeeId employee identifier
     * @param name       employee name
     * @param position   employee position/title
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         if the database operation fails
     */
    public void addEmployee(int employeeId, String name, String position) {
        Validator.validateEmployee(employeeId, name, position);

        try {
            PreparedStatement stmt = DBConnection.prepareStatement(INSERT_SQL);
            stmt.clearParameters();

            stmt.setInt(1, employeeId);
            stmt.setString(2, name);
            stmt.setString(3, position);

            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while adding employee with ID: " + employeeId, e);
            throw new RuntimeException("Failed to add employee: " + e.getMessage(), e);
        }
    }

    /**
     * Reads all employees from the {@code employees} object table.
     *
     * @return list of all employees (may be empty)
     * @throws RuntimeException if the database operation fails
     */
    public List<Employee> getAllEmployees() {
        List<Employee> employees = new ArrayList<>();

        try {
            PreparedStatement stmt = DBConnection.prepareStatement(SELECT_ALL_SQL);
            stmt.clearParameters();

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    employees.add(new Employee(
                            rs.getInt(1),
                            rs.getString(2),
                            rs.getString(3)
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while fetching employees", e);
            throw new RuntimeException("Failed to fetch employees: " + e.getMessage(), e);
        }

        return employees;
    }

    /**
     * Updates an existing employee in the {@code employees} object table by {@code employee_id}.
     *
     * @param employeeId employee identifier
     * @param name       new employee name
     * @param position   new employee position/title
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         if the database operation fails
     */
public void updateEmployee(int employeeId, String name, String position) {
    Validator.validateEmployee(employeeId, name, position);

    String sql =
            "UPDATE employees e " +
            "SET VALUE(e) = employee_t(?, ?, ?) " +
            "WHERE VALUE(e).employee_id = ?";

    try {
        PreparedStatement stmt = DBConnection.prepareStatement(sql);
        stmt.clearParameters();

        stmt.setInt(1, employeeId);
        stmt.setString(2, name);
        stmt.setString(3, position);

        stmt.setInt(4, employeeId);

        stmt.executeUpdate();
    } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, "Error while updating employee", e);
        throw new RuntimeException("Failed to update employee: " + e.getMessage(), e);
    }
}

    /**
     * Deletes an employee from the {@code employees} object table by {@code employee_id}.
     *
     * @param employeeId employee identifier
     * @throws IllegalArgumentException if {@code employeeId} is not positive
     * @throws RuntimeException         if the database operation fails
     */
    public void deleteEmployee(int employeeId) {
        Validator.positiveInt(employeeId, "Employee ID");

        try {
            PreparedStatement stmt = DBConnection.prepareStatement(DELETE_SQL);
            stmt.clearParameters();

            stmt.setInt(1, employeeId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while deleting employee with ID: " + employeeId, e);
            throw new RuntimeException("Failed to delete employee: " + e.getMessage(), e);
        }
    }
}