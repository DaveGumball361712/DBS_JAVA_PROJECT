package view;

import dao.ReportDAO;
import util.ErrorHandler;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

// Tab canh bao san pham sap het hang (doc tu view CATALOG.V_LOW_STOCK)
public class LowStockPanel extends JPanel {

    private final ReportDAO reportDAO = new ReportDAO();
    private DefaultTableModel tableModel;
    private JLabel lblSummary;

    public LowStockPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(createTopPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);

        loadLowStock();
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton btnReload = new JButton("Tải lại");
        btnReload.addActionListener(e -> loadLowStock());
        panel.add(btnReload);

        lblSummary = new JLabel("");
        lblSummary.setFont(lblSummary.getFont().deriveFont(Font.BOLD));
        panel.add(lblSummary);

        return panel;
    }

    private JScrollPane createTablePanel() {
        String[] cols = {"Mã SP", "Tên sản phẩm", "Loại hàng", "Tồn hiện tại", "Tồn tối thiểu", "Cần nhập thêm"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        JTable table = new JTable(tableModel);
        table.setRowHeight(28);
        table.getTableHeader().setReorderingAllowed(false);

        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(280);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);

        return new JScrollPane(table);
    }

    public void loadLowStock() {
        try {
            List<Object[]> rows = reportDAO.lowStock();
            tableModel.setRowCount(0);
            for (Object[] row : rows) {
                tableModel.addRow(row);
            }

            if (rows.isEmpty()) {
                lblSummary.setText("  Tồn kho ổn định, không có sản phẩm nào dưới mức tối thiểu.");
                lblSummary.setForeground(new Color(0, 130, 0));
            } else {
                lblSummary.setText("  Cảnh báo: có " + rows.size() + " sản phẩm sắp hết hàng!");
                lblSummary.setForeground(Color.RED);
            }
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);
        }
    }
}
