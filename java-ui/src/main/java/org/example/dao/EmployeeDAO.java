package org.example.dao;

import org.example.db.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for managing employee entities in the database.
 * Supports CRUD operations for employees stored as Oracle object types.
 */
public class EmployeeDAO {

    /**
     * Data holder representing an employee entity.
     */
    public static class Employee {

        public int id;
        public String name;
        public String position;

        /**
         * Creates a new Employee object.
         *
         * @param id       employee identifier
         * @param name     employee name
         * @param position employee position
         */
        public Employee(int id, String name, String position) {
            this.id = id;
            this.name = name;
            this.position = position;
        }
    }

    /**
     * Adds a new employee to the database.
     *
     * @param employeeId employee identifier
     * @param name       employee name
     * @param position   employee position
     */
    public void addEmployee(int employeeId, String name, String position) {
        String sql = "INSERT INTO employees VALUES (employee_t(?, ?, ?))";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setString(2, name);
            stmt.setString(3, position);

            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all employees from the database.
     *
     * @return list of all employees
     */
    public List<Employee> getAllEmployees() {
        List<Employee> employees = new ArrayList<>();

        String sql = "SELECT " +
                "VALUE(e).employee_id, " +
                "VALUE(e).name, " +
                "VALUE(e).position " +
                "FROM employees e";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                employees.add(new Employee(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3)
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return employees;
    }

    /**
     * Updates an existing employee in the database.
     *
     * @param employeeId employee identifier
     * @param name       new employee name
     * @param position   new employee position
     */
    public void updateEmployee(int employeeId, String name, String position) {
        String sql = "UPDATE employees e SET " +
                "VALUE(e).name = ?, " +
                "VALUE(e).position = ? " +
                "WHERE VALUE(e).employee_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            stmt.setString(2, position);
            stmt.setInt(3, employeeId);

            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes an employee from the database by its identifier.
     *
     * @param employeeId employee identifier
     */
    public void deleteEmployee(int employeeId) {
        String sql = "DELETE FROM employees e WHERE VALUE(e).employee_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
