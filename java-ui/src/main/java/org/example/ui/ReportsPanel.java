package org.example.ui;

import org.example.dao.ReportDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Swing panel for running ad-hoc SQL reports (SELECT-only) and displaying the result in a table.
 */
public class ReportsPanel extends JPanel {

    private final JTextArea queryArea;
    private final JTable resultTable;
    private final DefaultTableModel tableModel;
    private final ReportDAO reportDAO = new ReportDAO();

    /**
     * Creates the reports panel with a query editor and a result table.
     */
    public ReportsPanel() {
        setLayout(new BorderLayout(10, 10));

        queryArea = new JTextArea(10, 80);
        queryArea.setBorder(BorderFactory.createTitledBorder("SQL Report (SELECT only)"));
        queryArea.setText("""
                SELECT
                    VALUE(o).order_id,
                    VALUE(o).order_date,
                    VALUE(o).status AS order_status,

                    DEREF(VALUE(o).client).name        AS client_name,
                    DEREF(VALUE(o).client).client_type AS client_type,

                    DEREF(VALUE(o).employee).name      AS employee_name,
                    DEREF(VALUE(o).employee).position  AS employee_position,

                    DEREF(VALUE(o).payment).amount     AS payment_amount,
                    DEREF(VALUE(o).payment).status     AS payment_status,

                    DEREF(VALUE(o).delivery).courier   AS courier,
                    DEREF(VALUE(o).delivery).tracking_number,
                    DEREF(VALUE(o).delivery).delivery_address

                FROM orders o
                ORDER BY VALUE(o).order_id
                """);

        add(new JScrollPane(queryArea), BorderLayout.NORTH);

        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        resultTable = new JTable(tableModel);
        add(new JScrollPane(resultTable), BorderLayout.CENTER);

        JButton executeBtn = new JButton("Execute");
        executeBtn.addActionListener(e -> executeQuery());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(executeBtn);
        add(bottom, BorderLayout.SOUTH);
    }

    /**
     * Executes the SQL from the text area and populates the result table.
     */
    private void executeQuery() {
        try {
            String sql = queryArea.getText().trim();
            ReportDAO.ReportResult result = reportDAO.executeQuery(sql);
            tableModel.setDataVector(result.data, result.columnNames);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}