package view;

import dao.ImportReceiptDAO;
import dao.ProductDAO;
import dao.SupplierDAO;
import model.ImportLine;
import model.ImportReceipt;
import model.Product;
import model.Supplier;
import util.ErrorHandler;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Tab xem danh sach phieu nhap va loc theo khoang ngay
public class ImportHistoryPanel extends JPanel {

    private final ImportReceiptDAO importReceiptDAO = new ImportReceiptDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final ProductDAO productDAO = new ProductDAO();

    private JTextField txtFromDate;
    private JTextField txtToDate;
    private DefaultTableModel receiptModel;
    private DefaultTableModel detailModel;
    private JTable receiptTable;
    private JLabel lblCount;

    private List<ImportReceipt> displayedReceipts = new ArrayList<>();
    private Map<Integer, String> supplierNames = new HashMap<>();
    private Map<Integer, String> productNames = new HashMap<>();

    public ImportHistoryPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(createFilterPanel(), BorderLayout.NORTH);
        add(createTablesPanel(), BorderLayout.CENTER);

        loadData(null, null);
    }

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        panel.add(new JLabel("Từ ngày (yyyy-MM-dd):"));
        txtFromDate = new JTextField(LocalDate.now().minusDays(30).toString(), 10);
        panel.add(txtFromDate);

        panel.add(new JLabel("Đến ngày:"));
        txtToDate = new JTextField(LocalDate.now().toString(), 10);
        panel.add(txtToDate);

        JButton btnFilter = new JButton("Lọc theo ngày");
        btnFilter.addActionListener(e -> handleFilter());
        panel.add(btnFilter);

        JButton btnAll = new JButton("Tất cả");
        btnAll.addActionListener(e -> loadData(null, null));
        panel.add(btnAll);

        lblCount = new JLabel("  |  Tổng: 0 phiếu");
        lblCount.setFont(lblCount.getFont().deriveFont(Font.BOLD));
        panel.add(lblCount);

        return panel;
    }

    private JSplitPane createTablesPanel() {
        // Bang tren: danh sach phieu nhap
        String[] receiptCols = {"Mã phiếu", "Ngày nhập", "Mã NV", "Nhà cung cấp", "Ghi chú", "Tổng tiền"};
        receiptModel = new DefaultTableModel(receiptCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        receiptTable = new JTable(receiptModel);
        receiptTable.setRowHeight(26);
        receiptTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        receiptTable.getTableHeader().setReorderingAllowed(false);

        // Chon 1 phieu thi hien chi tiet o bang duoi
        receiptTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedDetails();
            }
        });

        // Bang duoi: chi tiet cac dong hang trong phieu dang chon
        String[] detailCols = {"Mã SP", "Tên sản phẩm", "Số lượng", "Giá nhập", "Thành tiền"};
        detailModel = new DefaultTableModel(detailCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable detailTable = new JTable(detailModel);
        detailTable.setRowHeight(26);
        detailTable.getTableHeader().setReorderingAllowed(false);

        JScrollPane topScroll = new JScrollPane(receiptTable);
        topScroll.setBorder(BorderFactory.createTitledBorder("Danh sách phiếu nhập"));

        JScrollPane bottomScroll = new JScrollPane(detailTable);
        bottomScroll.setBorder(BorderFactory.createTitledBorder("Chi tiết dòng hàng"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topScroll, bottomScroll);
        splitPane.setDividerLocation(260);
        splitPane.setResizeWeight(0.55);
        return splitPane;
    }

    private void handleFilter() {
        try {
            LocalDate from = LocalDate.parse(txtFromDate.getText().trim());
            LocalDate to = LocalDate.parse(txtToDate.getText().trim());
            if (from.isAfter(to)) {
                JOptionPane.showMessageDialog(this,
                    "Từ ngày phải nhỏ hơn hoặc bằng Đến ngày!",
                    "Ngày không hợp lệ", JOptionPane.WARNING_MESSAGE);
                return;
            }
            loadData(from, to);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng nhập ngày đúng định dạng yyyy-MM-dd (ví dụ: 2026-11-20).",
                "Sai định dạng ngày", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void loadData(LocalDate from, LocalDate to) {
        try {
            supplierNames.clear();
            for (Supplier s : supplierDAO.findAll()) {
                supplierNames.put(s.getSupplierId(), s.getName());
            }

            productNames.clear();
            for (Product p : productDAO.findAll()) {
                productNames.put(p.getProductId(), p.getName());
            }

            List<ImportReceipt> all = importReceiptDAO.findAll();
            displayedReceipts.clear();
            receiptModel.setRowCount(0);
            detailModel.setRowCount(0);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (ImportReceipt r : all) {
                LocalDate date = r.getImportDate().toLocalDate();
                if (from != null && date.isBefore(from)) continue;
                if (to != null && date.isAfter(to)) continue;

                displayedReceipts.add(r);
                String supName = supplierNames.getOrDefault(r.getSupplierId(), "NCC #" + r.getSupplierId());
                receiptModel.addRow(new Object[]{
                    r.getImportReceiptId(),
                    r.getImportDate().format(fmt),
                    r.getEmployeeId(),
                    supName,
                    r.getDescription() != null ? r.getDescription() : "",
                    String.format("%,.0f VNĐ", r.getTotal())
                });
            }

            lblCount.setText("  |  Tổng: " + displayedReceipts.size() + " phiếu");
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);
        }
    }

    private void showSelectedDetails() {
        detailModel.setRowCount(0);
        int row = receiptTable.getSelectedRow();
        if (row < 0 || row >= displayedReceipts.size()) return;

        ImportReceipt r = displayedReceipts.get(row);
        for (ImportLine line : r.getLines()) {
            String prodName = productNames.getOrDefault(line.getProductId(), "SP #" + line.getProductId());
            detailModel.addRow(new Object[]{
                line.getProductId(),
                prodName,
                line.getQuantity(),
                String.format("%,.0f", line.getImportPrice()),
                String.format("%,.0f", line.getLineTotal())
            });
        }
    }
}
