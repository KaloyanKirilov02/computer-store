package org.example.ui;

import org.example.dao.ReportDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.ResultSet;
import java.util.Vector;

/**
 * JPanel for executing custom SELECT SQL queries and displaying the results.
 * Only allows SELECT queries.
 */
public class ReportsPanel extends JPanel {

    private JTextArea queryArea;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private ReportDAO reportDAO = new ReportDAO();

    /**
     * Constructs the ReportsPanel with a text area for SQL input,
     * a table for results, and a button to execute the query.
     */
    public ReportsPanel() {
        setLayout(new BorderLayout(10, 10));

        queryArea = new JTextArea(6, 80);
        queryArea.setBorder(BorderFactory.createTitledBorder("JOIN SQL Query (SELECT only)"));
        queryArea.setText("""
                SELECT
                    o.order_id,
                    o.order_date,
                    o.status                        AS order_status,
                
                    DEREF(o.client).name            AS client_name,
                    DEREF(o.client).client_type     AS client_type,
                
                    DEREF(o.employee).name          AS employee_name,
                    DEREF(o.employee).position      AS employee_position,
                
                    DEREF(o.payment).amount         AS payment_amount,
                    DEREF(o.payment).status         AS payment_status,
                
                    DEREF(o.delivery).courier       AS courier,
                    DEREF(o.delivery).tracking_number,
                    DEREF(o.delivery).delivery_address
                
                FROM orders o
                ORDER BY o.order_id
                """);

        add(new JScrollPane(queryArea), BorderLayout.NORTH);

        tableModel = new DefaultTableModel();
        resultTable = new JTable(tableModel);
        add(new JScrollPane(resultTable), BorderLayout.CENTER);

        JButton executeBtn = new JButton("Execute Query");
        executeBtn.addActionListener(e -> executeQuery());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(executeBtn);
        add(bottom, BorderLayout.SOUTH);
    }

    /**
     * Executes the SQL query entered in the text area and displays the results in the table.
     * Only SELECT queries are allowed.
     */
    private void executeQuery() {
        try {
            String sql = queryArea.getText().trim();

            if (!sql.toLowerCase().startsWith("select")) {
                JOptionPane.showMessageDialog(this, "Only SELECT queries are allowed!");
                return;
            }

            ResultSet rs = reportDAO.executeQuery(sql);

            Vector<String> columns = ReportDAO.getColumnNames(rs);
            Vector<Vector<Object>> data = ReportDAO.getData(rs);

            tableModel.setDataVector(data, columns);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}
