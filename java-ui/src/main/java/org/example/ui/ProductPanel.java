package org.example.ui;

import org.example.dao.ProductDAO;
import org.example.dao.ProductDAO.Accessory;
import org.example.dao.ProductDAO.Computer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Swing panel for managing products (Computers and Accessories).
 */
public class ProductPanel extends JPanel {

    private final ProductDAO dao = new ProductDAO();

    private DefaultTableModel computerModel;
    private DefaultTableModel accessoryModel;

    private JTable computerTable;
    private JTable accessoryTable;

    private final JTabbedPane tabs = new JTabbedPane();

    /**
     * Creates the product management panel and loads the initial data.
     */
    public ProductPanel() {
        setLayout(new BorderLayout(10, 10));

        tabs.add("Computers", createComputerPanel());
        tabs.add("Accessories", createAccessoryPanel());

        add(tabs, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        refreshTables();
    }

    /**
     * Builds the tab panel that lists computers.
     *
     * @return computer tab panel
     */
    private JPanel createComputerPanel() {
        computerModel = new DefaultTableModel(
                new Object[]{"ID", "Name", "Description", "Price", "Qty", "Category", "CPU", "RAM", "Storage", "GPU"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        computerTable = new JTable(computerModel);
        computerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(computerTable), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Builds the tab panel that lists accessories.
     *
     * @return accessory tab panel
     */
    private JPanel createAccessoryPanel() {
        accessoryModel = new DefaultTableModel(
                new Object[]{"ID", "Name", "Description", "Price", "Qty", "Category", "Type", "Compatibility"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        accessoryTable = new JTable(accessoryModel);
        accessoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(accessoryTable), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Builds the bottom action bar.
     *
     * @return button panel
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 10, 10));

        JButton add = new JButton("Add");
        JButton update = new JButton("Update");
        JButton delete = new JButton("Delete");
        JButton refresh = new JButton("Refresh");

        add.addActionListener(e -> addProduct());
        update.addActionListener(e -> updateProduct());
        delete.addActionListener(e -> deleteProduct());
        refresh.addActionListener(e -> refreshTables());

        panel.add(add);
        panel.add(update);
        panel.add(delete);
        panel.add(refresh);

        return panel;
    }

    /**
     * Reloads data for both tabs.
     */
    private void refreshTables() {
        try {
            loadComputers();
            loadAccessories();
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    /**
     * Loads computer products into the computer table.
     */
    private void loadComputers() {
        computerModel.setRowCount(0);
        List<Computer> list = dao.getAllComputers();
        for (Computer c : list) {
            computerModel.addRow(new Object[]{
                    c.id, c.name, c.description, c.price,
                    c.quantity, c.categoryId,
                    c.cpu, c.ram, c.storage, c.gpu
            });
        }
    }

    /**
     * Loads accessory products into the accessory table.
     */
    private void loadAccessories() {
        accessoryModel.setRowCount(0);
        List<Accessory> list = dao.getAllAccessories();
        for (Accessory a : list) {
            accessoryModel.addRow(new Object[]{
                    a.id, a.name, a.description, a.price,
                    a.quantity, a.categoryId,
                    a.accessoryType, a.compatibility
            });
        }
    }

    /**
     * Prompts for fields and creates a new product (computer or accessory).
     */
    private void addProduct() {
        String[] options = {"Computer", "Accessory"};
        String type = (String) JOptionPane.showInputDialog(
                this,
                "Select product type:",
                "Product Type",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (type == null) {
            return;
        }

        try {
            Integer id = askInt("Product ID:");
            if (id == null) return;

            String name = askString("Name:");
            if (name == null) return;

            String desc = askString("Description:");
            if (desc == null) return;

            Double price = askDouble("Price:");
            if (price == null) return;

            Integer qty = askInt("Quantity:");
            if (qty == null) return;

            Integer category = askInt("Category ID:");
            if (category == null) return;

            if ("Computer".equals(type)) {
                String cpu = askString("CPU:");
                if (cpu == null) return;

                String ram = askString("RAM:");
                if (ram == null) return;

                String storage = askString("Storage:");
                if (storage == null) return;

                String gpu = askString("GPU:");
                if (gpu == null) return;

                dao.addComputer(id, name, desc, price, qty, category, cpu, ram, storage, gpu);
            } else {
                String accType = askString("Accessory type:");
                if (accType == null) return;

                String comp = askString("Compatibility:");
                if (comp == null) return;

                dao.addAccessory(id, name, desc, price, qty, category, accType, comp);
            }

            refreshTables();
        } catch (RuntimeException ex) {
            showError("Error: " + ex.getMessage());
        }
    }

    /**
     * Updates common (base) product fields for the selected row in the active tab.
     */
    private void updateProduct() {
        int tab = tabs.getSelectedIndex();
        JTable table = (tab == 0) ? computerTable : accessoryTable;
        DefaultTableModel model = (tab == 0) ? computerModel : accessoryModel;

        int row = table.getSelectedRow();
        if (row == -1) {
            showInfo("Select a product from the table first.");
            return;
        }

        try {
            int id = (int) model.getValueAt(row, 0);

            String name = askString("Name:", model.getValueAt(row, 1));
            if (name == null) return;

            String desc = askString("Description:", model.getValueAt(row, 2));
            if (desc == null) return;

            Double price = askDouble("Price:", model.getValueAt(row, 3));
            if (price == null) return;

            Integer qty = askInt("Quantity:", model.getValueAt(row, 4));
            if (qty == null) return;

            Integer cat = askInt("Category ID:", model.getValueAt(row, 5));
            if (cat == null) return;

            dao.updateBaseProduct(id, name, desc, price, qty, cat);
            refreshTables();
        } catch (RuntimeException ex) {
            showError("Error while updating: " + ex.getMessage());
        }
    }

    /**
     * Deletes the selected product in the active tab after confirmation.
     */
    private void deleteProduct() {
        int tab = tabs.getSelectedIndex();
        JTable table = (tab == 0) ? computerTable : accessoryTable;

        int row = table.getSelectedRow();
        if (row == -1) {
            showInfo("Select a product from the table first.");
            return;
        }

        int id = (int) table.getValueAt(row, 0);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete product ID " + id + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            dao.deleteProduct(id);
            refreshTables();
        } catch (RuntimeException ex) {
            showError("Error while deleting: " + ex.getMessage());
        }
    }

    /**
     * Shows an input dialog for an integer value.
     *
     * @param label message shown to the user
     * @return parsed integer value, or {@code null} if cancelled
     */
    private Integer askInt(String label) {
        String s = JOptionPane.showInputDialog(this, label);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) throw new RuntimeException(label + " cannot be empty.");
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new RuntimeException(label + " must be a number.");
        }
    }

    /**
     * Shows an input dialog for an integer value, pre-filled with a default value.
     *
     * @param label        message shown to the user
     * @param defaultValue default value shown in the input field
     * @return parsed integer value, or {@code null} if cancelled
     */
    private Integer askInt(String label, Object defaultValue) {
        String s = JOptionPane.showInputDialog(this, label, defaultValue);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) throw new RuntimeException(label + " cannot be empty.");
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new RuntimeException(label + " must be a number.");
        }
    }

    /**
     * Shows an input dialog for a double value.
     *
     * @param label message shown to the user
     * @return parsed double value, or {@code null} if cancelled
     */
    private Double askDouble(String label) {
        String s = JOptionPane.showInputDialog(this, label);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) throw new RuntimeException(label + " cannot be empty.");
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new RuntimeException(label + " must be a number (double).");
        }
    }

    /**
     * Shows an input dialog for a double value, pre-filled with a default value.
     *
     * @param label        message shown to the user
     * @param defaultValue default value shown in the input field
     * @return parsed double value, or {@code null} if cancelled
     */
    private Double askDouble(String label, Object defaultValue) {
        String s = JOptionPane.showInputDialog(this, label, defaultValue);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) throw new RuntimeException(label + " cannot be empty.");
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new RuntimeException(label + " must be a number (double).");
        }
    }

    /**
     * Shows an input dialog for a non-empty string value.
     *
     * @param label message shown to the user
     * @return string value, or {@code null} if cancelled
     */
    private String askString(String label) {
        String s = JOptionPane.showInputDialog(this, label);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) throw new RuntimeException(label + " cannot be empty.");
        return s;
    }

    /**
     * Shows an input dialog for a non-empty string value, pre-filled with a default value.
     *
     * @param label        message shown to the user
     * @param defaultValue default value shown in the input field
     * @return string value, or {@code null} if cancelled
     */
    private String askString(String label, Object defaultValue) {
        String s = JOptionPane.showInputDialog(this, label, defaultValue);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) throw new RuntimeException(label + " cannot be empty.");
        return s;
    }

    /**
     * Shows an error dialog with the provided message.
     *
     * @param msg message to display
     */
    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Shows an information dialog with the provided message.
     *
     * @param msg message to display
     */
    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }
}