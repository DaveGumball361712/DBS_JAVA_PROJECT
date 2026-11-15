package view;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ReportPanel extends JPanel {

    private final JTextField fromField = new JTextField(10);
    private final JTextField toField = new JTextField(10);
    private final JLabel revenueLabel = new JLabel();
    private final JLabel orderCountLabel = new JLabel();

    private final DefaultTableModel cashierModel =
        createModel("Thu ngan", "So don", "Doanh thu (VND)");
    private final DefaultTableModel topProductModel =
        createModel("Ma SP", "Ten san pham", "So luong ban", "Doanh thu (VND)");
    private final DefaultTableModel lowStockModel =
        createModel("Ma SP", "Ten san pham", "Ton kho", "Ton toi thieu");
    private final DefaultTableModel supplierModel =
        createModel("Nha cung cap", "So phieu nhap", "Tong tien nhap (VND)");

    public ReportPanel() {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(createTopArea(), BorderLayout.NORTH);
        add(createReportTabs(), BorderLayout.CENTER);

        LocalDate today = LocalDate.now();
        fromField.setText(today.withDayOfMonth(1).toString());
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
        reportTabs.addTab("Doanh thu theo thu ngan", wrapInTable(cashierModel));
        reportTabs.addTab("San pham ban chay", wrapInTable(topProductModel));
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

        loadFakeData();
    }

    private void loadFakeData() {
        cashierModel.setRowCount(0);
        cashierModel.addRow(new Object[] {"Nguyen Thi Lan", 42, 12_450_000L});
        cashierModel.addRow(new Object[] {"Tran Van Minh", 35, 9_870_000L});
        cashierModel.addRow(new Object[] {"Le Thi Hoa", 28, 7_320_000L});

        long totalRevenue = 0;
        int totalOrders = 0;
        for (int row = 0; row < cashierModel.getRowCount(); row++) {
            totalOrders += (Integer) cashierModel.getValueAt(row, 1);
            totalRevenue += (Long) cashierModel.getValueAt(row, 2);
        }
        revenueLabel.setText("Tong doanh thu: " + String.format("%,d", totalRevenue) + " VND");
        orderCountLabel.setText("So don: " + totalOrders);

        topProductModel.setRowCount(0);
        topProductModel.addRow(new Object[] {12, "Sua tuoi Vinamilk 1L", 120, 3_840_000L});
        topProductModel.addRow(new Object[] {7, "Mi Hao Hao", 300, 1_350_000L});
        topProductModel.addRow(new Object[] {25, "Tai nghe Bluetooth", 8, 2_400_000L});

        lowStockModel.setRowCount(0);
        lowStockModel.addRow(new Object[] {31, "Nuoc suoi 500ml", 4, 20});
        lowStockModel.addRow(new Object[] {18, "Pin AA", 2, 10});

        supplierModel.setRowCount(0);
        supplierModel.addRow(new Object[] {"Cong ty Sua Viet", 5, 25_000_000L});
        supplierModel.addRow(new Object[] {"Dien May Sai Gon", 2, 18_500_000L});
    }
}