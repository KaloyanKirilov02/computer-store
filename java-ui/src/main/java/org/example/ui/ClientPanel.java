package org.example.ui;

import org.example.dao.ClientDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Swing panel for managing clients: create, update, delete, and browse.
 */
public class ClientPanel extends JPanel {

    private final ClientDAO clientDAO = new ClientDAO();

    private final JTextField txtId = new JTextField();
    private final JTextField txtName = new JTextField();
    private final JTextField txtEmail = new JTextField();
    private final JTextField txtPhone = new JTextField();
    private final JTextField txtAddress = new JTextField();
    private final JTextField txtType = new JTextField();

    private final JButton btnAdd = new JButton("Add");
    private final JButton btnUpdate = new JButton("Update");
    private final JButton btnDelete = new JButton("Delete");
    private final JButton btnRefresh = new JButton("Refresh");

    private final JTable table;
    private final DefaultTableModel tableModel;

    /**
     * Creates the client management panel and loads the initial data.
     */
    public ClientPanel() {
        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(6, 2, 5, 5));
        formPanel.add(new JLabel("Client ID:"));
        formPanel.add(txtId);
        formPanel.add(new JLabel("Name:"));
        formPanel.add(txtName);
        formPanel.add(new JLabel("Email:"));
        formPanel.add(txtEmail);
        formPanel.add(new JLabel("Phone:"));
        formPanel.add(txtPhone);
        formPanel.add(new JLabel("Address:"));
        formPanel.add(txtAddress);
        formPanel.add(new JLabel("Type:"));
        formPanel.add(txtType);

        add(formPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"ID", "Name", "Email", "Phone", "Address", "Type"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
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

        btnAdd.addActionListener(e -> addClient());
        btnUpdate.addActionListener(e -> updateClient());
        btnDelete.addActionListener(e -> deleteClient());
        btnRefresh.addActionListener(e -> refreshTable());

        refreshTable();
    }

    /**
     * Populates the form fields from the selected table row.
     *
     * @param row selected table row index
     */
    private void fillFormFromTable(int row) {
        txtId.setText(tableModel.getValueAt(row, 0).toString());
        txtName.setText(tableModel.getValueAt(row, 1).toString());
        txtEmail.setText(tableModel.getValueAt(row, 2).toString());
        txtPhone.setText(tableModel.getValueAt(row, 3).toString());
        txtAddress.setText(tableModel.getValueAt(row, 4).toString());
        txtType.setText(tableModel.getValueAt(row, 5).toString());
    }

    /**
     * Clears all form fields and resets table selection.
     */
    private void clearForm() {
        txtId.setText("");
        txtName.setText("");
        txtEmail.setText("");
        txtPhone.setText("");
        txtAddress.setText("");
        txtType.setText("");
        table.clearSelection();
    }

    /**
     * Reads the form fields and creates a new client.
     */
    private void addClient() {
        try {
            int id = Integer.parseInt(txtId.getText());
            clientDAO.addClient(
                    id,
                    txtName.getText(),
                    txtEmail.getText(),
                    txtPhone.getText(),
                    txtAddress.getText(),
                    txtType.getText()
            );
            refreshTable();
            selectRowById(id);
            clearForm();
        } catch (NumberFormatException ex) {
            showError("Client ID must be a number");
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    /**
     * Reads the form fields and updates the selected client.
     */
    private void updateClient() {
        try {
            int id = Integer.parseInt(txtId.getText());
            clientDAO.updateClient(
                    id,
                    txtName.getText(),
                    txtEmail.getText(),
                    txtPhone.getText(),
                    txtAddress.getText(),
                    txtType.getText()
            );
            refreshTable();
            selectRowById(id);
            clearForm();
        } catch (NumberFormatException ex) {
            showError("Client ID must be a number");
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    /**
     * Deletes the client specified in the form after confirmation.
     */
    private void deleteClient() {
        try {
            int id = Integer.parseInt(txtId.getText());
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to delete client ID " + id + "?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                clientDAO.deleteClient(id);
                refreshTable();
                clearForm();
            }
        } catch (NumberFormatException ex) {
            showError("Client ID must be a number");
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    /**
     * Reloads the table data from the database.
     */
    private void refreshTable() {
        tableModel.setRowCount(0);
        List<ClientDAO.Client> clients = clientDAO.getAllClients();
        for (ClientDAO.Client c : clients) {
            tableModel.addRow(new Object[]{c.id, c.name, c.email, c.phone, c.address, c.type});
        }
    }

    /**
     * Selects a row in the table by client ID and scrolls it into view.
     *
     * @param id client identifier to locate
     */
    private void selectRowById(int id) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if ((int) tableModel.getValueAt(i, 0) == id) {
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