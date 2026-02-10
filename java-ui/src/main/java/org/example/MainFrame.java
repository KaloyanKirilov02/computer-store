package org.example;

import org.example.db.DBConnection;
import org.example.ui.*;

import javax.swing.*;
import java.awt.*;

/**
 * Main application window for the Computer Store Management system.
 * Initializes and displays all management panels in separate tabs.
 */
public class MainFrame extends JFrame {

    private JTabbedPane tabbedPane;

    /**
     * Creates the main frame, initializes the UI, and shows the window.
     */
    public MainFrame() {
        setTitle("Computer Store Management");
        setSize(1000, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        initUI();

        setVisible(true);
    }

    /**
     * Builds the tabbed UI and attaches all feature panels.
     */
    private void initUI() {
        tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Clients", new ClientPanel());
        tabbedPane.addTab("Employees", new EmployeePanel());
        tabbedPane.addTab("Products", new ProductPanel());
        tabbedPane.addTab("Orders", new OrderPanel());
        tabbedPane.addTab("Payments", new PaymentPanel());
        tabbedPane.addTab("Deliveries", new DeliveryPanel());
        tabbedPane.addTab("Reviews", new ReviewPanel());
        tabbedPane.addTab("Promotions", new PromotionPanel());
        tabbedPane.addTab("Product Promotions", new ProductPromotionPanel());
        tabbedPane.addTab("Reports", new ReportsPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * Application entry point.
     * Ensures cached JDBC resources are released on shutdown and starts the Swing UI.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(DBConnection::shutdown));
        SwingUtilities.invokeLater(MainFrame::new);
    }
}