package org.example.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import org.example.dao.EmployeeDAO;

/**
 * A JPanel that displays and manages employees in a table with a form
 * for adding, updating, and deleting employee records.
 */
public class EmployeePanel extends JPanel {

    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    private JTextField txtId, txtName, txtPosition;
    private JButton btnAdd, btnUpdate, btnDelete, btnRefresh;
    private JTable table;
    private DefaultTableModel tableModel;

    /**
     * Constructs an EmployeePanel with a form, table, and action buttons.
     */
    public EmployeePanel() {
        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        formPanel.add(new JLabel("Employee ID:"));
        txtId = new JTextField(); formPanel.add(txtId);
        formPanel.add(new JLabel("Name:"));
        txtName = new JTextField(); formPanel.add(txtName);
        formPanel.add(new JLabel("Position:"));
        txtPosition = new JTextField(); formPanel.add(txtPosition);

        add(formPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Name", "Position"}, 0
        );
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    txtId.setText(tableModel.getValueAt(row, 0).toString());
                    txtName.setText(tableModel.getValueAt(row, 1).toString());
                    txtPosition.setText(tableModel.getValueAt(row, 2).toString());
                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout());
        btnAdd = new JButton("Add"); buttonPanel.add(btnAdd);
        btnUpdate = new JButton("Update"); buttonPanel.add(btnUpdate);
        btnDelete = new JButton("Delete"); buttonPanel.add(btnDelete);
        btnRefresh = new JButton("Refresh"); buttonPanel.add(btnRefresh);
        add(buttonPanel, BorderLayout.SOUTH);

        btnAdd.addActionListener(e -> {
            try {
                int id = Integer.parseInt(txtId.getText());
                employeeDAO.addEmployee(id, txtName.getText(), txtPosition.getText());
                refreshTable();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Employee ID must be a number");
            }
        });

        btnUpdate.addActionListener(e -> {
            try {
                int id = Integer.parseInt(txtId.getText());
                employeeDAO.updateEmployee(id, txtName.getText(), txtPosition.getText());
                refreshTable();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Employee ID must be a number");
            }
        });

        btnDelete.addActionListener(e -> {
            try {
                int id = Integer.parseInt(txtId.getText());
                employeeDAO.deleteEmployee(id);
                refreshTable();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Employee ID must be a number");
            }
        });

        btnRefresh.addActionListener(e -> refreshTable());

        refreshTable();
    }

    /**
     * Refreshes the table to display all employees from the database.
     */
    private void refreshTable() {
        tableModel.setRowCount(0);
        List<EmployeeDAO.Employee> employees = employeeDAO.getAllEmployees();

        for (EmployeeDAO.Employee e : employees) {
            tableModel.addRow(new Object[]{
                    e.id, e.name, e.position
            });
        }
    }
}
