package org.example;

import org.example.ui.*;

import javax.swing.*;
import java.awt.*;

/**
 * Main application window for the Computer Store Management system.
 * <p>
 * This frame initializes and displays all functional panels
 * using a tab-based layout.
 */
public class MainFrame extends JFrame {

    private JTabbedPane tabbedPane;

    /**
     * Constructs the main application frame and initializes the UI.
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
     * Initializes the user interface components and tabs.
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
        tabbedPane.addTab("Product Promotion", new ProductPromotionPanel());
        tabbedPane.addTab("Reports", new ReportsPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * Application entry point.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainFrame::new);
    }
}
