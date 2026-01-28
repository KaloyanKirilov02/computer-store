package org.example.ui;

import org.example.dao.ProductDAO;
import org.example.dao.ProductDAO.Accessory;
import org.example.dao.ProductDAO.Computer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * JPanel for managing products.
 * Supports Computers and Accessories using a tabbed interface.
 * Provides CRUD operations for both product types.
 */
public class ProductPanel extends JPanel {

    private final ProductDAO dao = new ProductDAO();

    private DefaultTableModel computerModel;
    private DefaultTableModel accessoryModel;

    private JTable computerTable;
    private JTable accessoryTable;

    /**
     * Constructs the ProductPanel with tabs for Computers and Accessories
     * and buttons for CRUD operations.
     */
    public ProductPanel() {
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Computers", createComputerPanel());
        tabs.add("Accessories", createAccessoryPanel());

        add(tabs, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        refreshTables();
    }

    /**
     * Creates the panel containing the computers table.
     * @return JPanel containing the computer JTable
     */
    private JPanel createComputerPanel() {
        computerModel = new DefaultTableModel(
                new Object[]{"ID", "Name", "Description", "Price", "Qty", "Category", "CPU", "RAM", "Storage", "GPU"}, 0
        );
        computerTable = new JTable(computerModel);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(computerTable), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Creates the panel containing the accessories table.
     * @return JPanel containing the accessory JTable
     */
    private JPanel createAccessoryPanel() {
        accessoryModel = new DefaultTableModel(
                new Object[]{"ID", "Name", "Description", "Price", "Qty", "Category", "Type", "Compatibility"}, 0
        );
        accessoryTable = new JTable(accessoryModel);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(accessoryTable), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Creates the panel with CRUD buttons and attaches listeners.
     * @return JPanel with buttons
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
     * Refreshes both computers and accessories tables.
     */
    private void refreshTables() {
        loadComputers();
        loadAccessories();
    }

    /**
     * Loads all computers from the DAO into the computer table.
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
     * Loads all accessories from the DAO into the accessory table.
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
     * Prompts the user to add a new product (Computer or Accessory) and refreshes the tables.
     */
    private void addProduct() {
        String[] options = {"Computer", "Accessory"};
        String type = (String) JOptionPane.showInputDialog(
                this, "Select product type:",
                "Product Type",
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]
        );

        if (type == null) return;

        try {
            int id = Integer.parseInt(JOptionPane.showInputDialog("Product ID:"));
            String name = JOptionPane.showInputDialog("Name:");
            String desc = JOptionPane.showInputDialog("Description:");
            double price = Double.parseDouble(JOptionPane.showInputDialog("Price:"));
            int qty = Integer.parseInt(JOptionPane.showInputDialog("Quantity:"));
            int category = Integer.parseInt(JOptionPane.showInputDialog("Category ID:"));

            if (type.equals("Computer")) {
                String cpu = JOptionPane.showInputDialog("CPU:");
                String ram = JOptionPane.showInputDialog("RAM:");
                String storage = JOptionPane.showInputDialog("Storage:");
                String gpu = JOptionPane.showInputDialog("GPU:");

                dao.addComputer(id, name, desc, price, qty, category, cpu, ram, storage, gpu);
            } else {
                String accType = JOptionPane.showInputDialog("Accessory type:");
                String comp = JOptionPane.showInputDialog("Compatibility:");

                dao.addAccessory(id, name, desc, price, qty, category, accType, comp);
            }

            refreshTables();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    /**
     * Prompts the user to update the selected product's base fields and refreshes the tables.
     */
    private void updateProduct() {
        int tab = ((JTabbedPane) getComponent(0)).getSelectedIndex();
        JTable table = (tab == 0) ? computerTable : accessoryTable;
        DefaultTableModel model = (tab == 0) ? computerModel : accessoryModel;

        int row = table.getSelectedRow();
        if (row == -1) return;

        try {
            int id = (int) model.getValueAt(row, 0);

            String name = JOptionPane.showInputDialog("Name:", model.getValueAt(row, 1));
            String desc = JOptionPane.showInputDialog("Description:", model.getValueAt(row, 2));
            double price = Double.parseDouble(JOptionPane.showInputDialog("Price:", model.getValueAt(row, 3)));
            int qty = Integer.parseInt(JOptionPane.showInputDialog("Quantity:", model.getValueAt(row, 4)));
            int cat = Integer.parseInt(JOptionPane.showInputDialog("Category ID:", model.getValueAt(row, 5)));

            dao.updateBaseProduct(id, name, desc, price, qty, cat);
            refreshTables();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Update error: " + ex.getMessage());
        }
    }

    /**
     * Deletes the selected product from the database and refreshes the tables.
     */
    private void deleteProduct() {
        int tab = ((JTabbedPane) getComponent(0)).getSelectedIndex();
        JTable table = (tab == 0) ? computerTable : accessoryTable;

        int row = table.getSelectedRow();
        if (row == -1) return;

        int id = (int) table.getValueAt(row, 0);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete product " + id + "?",
                "Confirm",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            dao.deleteProduct(id);
            refreshTables();
        }
    }
}
