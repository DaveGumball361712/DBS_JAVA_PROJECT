package view;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import dao.ReportDAO;
import util.ErrorHandler;

public class ReportPanel extends JPanel {

    private final ReportDAO dao = new ReportDAO();

    private final JTextField fromField = new JTextField(10);
    private final JTextField toField = new JTextField(10);
    private final JLabel revenueLabel = new JLabel();
    private final JLabel orderCountLabel = new JLabel();

    private final DefaultTableModel dailyModel =
        createModel("Ngay", "So don", "Doanh thu (VND)");
    private final DefaultTableModel cashierModel =
        createModel("Thu ngan", "So don", "Doanh thu (VND)");
    private final DefaultTableModel topProductModel =
        createModel("Hang", "Ma SP", "Ten san pham", "So luong ban", "Doanh thu (VND)");
    private final DefaultTableModel lowStockModel =
        createModel("Ma SP", "Ten san pham", "Loai hang", "Ton kho", "Ton toi thieu", "Can nhap them");
    private final DefaultTableModel supplierModel =
        createModel("Nha cung cap", "So phieu nhap", "Tong so luong", "Tong tien nhap (VND)");

    public ReportPanel() {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(createTopArea(), BorderLayout.NORTH);
        add(createReportTabs(), BorderLayout.CENTER);

        LocalDate today = LocalDate.now();
        fromField.setText(today.withDayOfYear(1).toString());   // tu dau nam
        toField.setText(today.toString());
        loadReport();
    }

    private JPanel createTopArea() {
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filter.add(new JLabel("Tu ngay"));
        filter.add(fromField);
        filter.add(new JLabel("Den ngay"));
        filter.add(toField);

        JButton viewButton = new JButton("Xem bao cao");
        viewButton.addActionListener(e -> loadReport());
        filter.add(viewButton);
        filter.add(new JLabel("(dinh dang yyyy-MM-dd)"));

        JPanel summary = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 0));
        Font bold = revenueLabel.getFont().deriveFont(Font.BOLD, 15f);
        revenueLabel.setFont(bold);
        orderCountLabel.setFont(bold);
        summary.add(revenueLabel);
        summary.add(orderCountLabel);

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.add(filter, BorderLayout.NORTH);
        top.add(summary, BorderLayout.SOUTH);
        return top;
    }

    private JTabbedPane createReportTabs() {
        JTabbedPane reportTabs = new JTabbedPane();
        reportTabs.addTab("Doanh thu theo ngay", wrapInTable(dailyModel));
        reportTabs.addTab("Theo thu ngan (tat ca)", wrapInTable(cashierModel));
        reportTabs.addTab("San pham ban chay (tat ca)", wrapInTable(topProductModel));
        reportTabs.addTab("Hang sap het", wrapInTable(lowStockModel));
        reportTabs.addTab("Nhap theo nha cung cap", wrapInTable(supplierModel));
        return reportTabs;
    }

    private static DefaultTableModel createModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static JScrollPane wrapInTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(24);
        return new JScrollPane(table);
    }

    private void loadReport() {
        LocalDate from;
        LocalDate to;
        try {
            from = LocalDate.parse(fromField.getText().trim());
            to = LocalDate.parse(toField.getText().trim());
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this,
                "Ngay khong hop le. Hay nhap theo dang yyyy-MM-dd, vi du 2026-11-15",
                "Sai dinh dang ngay", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (from.isAfter(to)) {
            JOptionPane.showMessageDialog(this, "Tu ngay phai truoc hoac bang Den ngay",
                "Khoang ngay sai", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            List<Object[]> daily = dao.revenueByDay(from, to);
            fill(dailyModel, daily);

            // Tong doanh thu va so don trong khoang ngay
            BigDecimal totalRevenue = BigDecimal.ZERO;
            int totalOrders = 0;
            for (Object[] row : daily) {
                totalOrders += (Integer) row[1];
                totalRevenue = totalRevenue.add((BigDecimal) row[2]);
            }
            revenueLabel.setText("Tong doanh thu: " + money(totalRevenue) + " VND");
            orderCountLabel.setText("So don: " + totalOrders);

            fill(cashierModel, dao.revenueByCashier());
            fill(topProductModel, dao.topProducts());
            fill(lowStockModel, dao.lowStock());
            fill(supplierModel, dao.importBySupplier());
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);
        }
    }

    // Do danh sach dong vao bang
    private static void fill(DefaultTableModel model, List<Object[]> rows) {
        model.setRowCount(0);
        for (Object[] row : rows) {
            Object[] display = row.clone();
            for (int i = 0; i < display.length; i++) {
                if (display[i] instanceof BigDecimal) {
                    display[i] = money((BigDecimal) display[i]);
                }
            }
            model.addRow(display);
        }
    }

    private static String money(BigDecimal value) {
        return String.format("%,.0f", value);
    }
}