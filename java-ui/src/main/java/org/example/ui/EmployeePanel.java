package org.example.ui;

import org.example.dao.EmployeeDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Swing panel for managing employees: create, update, delete, and browse.
 */
public class EmployeePanel extends JPanel {

    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    private final JTextField txtId = new JTextField();
    private final JTextField txtName = new JTextField();
    private final JTextField txtPosition = new JTextField();

    private final JButton btnAdd = new JButton("Add");
    private final JButton btnUpdate = new JButton("Update");
    private final JButton btnDelete = new JButton("Delete");
    private final JButton btnRefresh = new JButton("Refresh");

    private final JTable table;
    private final DefaultTableModel tableModel;

    /**
     * Creates the employee management panel and loads the initial data.
     */
    public EmployeePanel() {
        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        formPanel.add(new JLabel("Employee ID:"));
        formPanel.add(txtId);

        formPanel.add(new JLabel("Name:"));
        formPanel.add(txtName);

        formPanel.add(new JLabel("Position:"));
        formPanel.add(txtPosition);

        add(formPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"ID", "Name", "Position"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    fillFormFromTable(row);
                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnRefresh);
        add(buttonPanel, BorderLayout.SOUTH);

        btnAdd.addActionListener(e -> addEmployee());
        btnUpdate.addActionListener(e -> updateEmployee());
        btnDelete.addActionListener(e -> deleteEmployee());
        btnRefresh.addActionListener(e -> refreshTable());

        refreshTable();
    }

    /**
     * Reads the form fields and creates a new employee.
     */
    private void addEmployee() {
        try {
            int id = parseRequiredInt(txtId, "Employee ID");
            String name = txtName.getText() == null ? "" : txtName.getText().trim();
            String position = txtPosition.getText() == null ? "" : txtPosition.getText().trim();

            employeeDAO.addEmployee(id, name, position);

            refreshTable();
            selectRowById(id);
            clearForm();
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    /**
     * Reads the form fields and updates an existing employee.
     */
    private void updateEmployee() {
        try {
            int id = parseRequiredInt(txtId, "Employee ID");
            String name = txtName.getText() == null ? "" : txtName.getText().trim();
            String position = txtPosition.getText() == null ? "" : txtPosition.getText().trim();

            employeeDAO.updateEmployee(id, name, position);

            refreshTable();
            selectRowById(id);
            clearForm();
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    /**
     * Deletes the employee specified in the form after confirmation.
     */
    private void deleteEmployee() {
        try {
            int id = parseRequiredInt(txtId, "Employee ID");

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to delete employee ID " + id + "?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                employeeDAO.deleteEmployee(id);
                refreshTable();
                clearForm();
            }
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    /**
     * Reloads the table data from the database.
     */
    private void refreshTable() {
        tableModel.setRowCount(0);
        try {
            List<EmployeeDAO.Employee> employees = employeeDAO.getAllEmployees();
            for (EmployeeDAO.Employee e : employees) {
                tableModel.addRow(new Object[]{e.id, e.name, e.position});
            }
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    /**
     * Populates the form fields from the selected table row.
     *
     * @param row selected table row index
     */
    private void fillFormFromTable(int row) {
        txtId.setText(String.valueOf(tableModel.getValueAt(row, 0)));
        txtName.setText(String.valueOf(tableModel.getValueAt(row, 1)));
        txtPosition.setText(String.valueOf(tableModel.getValueAt(row, 2)));
    }

    /**
     * Clears all form fields and resets table selection.
     */
    private void clearForm() {
        txtId.setText("");
        txtName.setText("");
        txtPosition.setText("");
        table.clearSelection();
    }

    /**
     * Parses a required integer value from a text field.
     *
     * @param field text field to read from
     * @param name  logical field name used in error messages
     * @return parsed integer value
     * @throws IllegalArgumentException if the field is empty or not a valid integer
     */
    private int parseRequiredInt(JTextField field, String name) {
        String s = field.getText();
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be empty.");
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be an integer.");
        }
    }

    /**
     * Selects a row in the table by employee ID and scrolls it into view.
     *
     * @param id employee identifier to locate
     */
    private void selectRowById(int id) {
        String target = String.valueOf(id);
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Object v = tableModel.getValueAt(i, 0);
            if (v != null && target.equals(String.valueOf(v))) {
                table.setRowSelectionInterval(i, i);
                table.scrollRectToVisible(table.getCellRect(i, 0, true));
                break;
            }
        }
    }

    /**
     * Shows an error dialog with the provided message.
     *
     * @param msg message to display
     */
    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}