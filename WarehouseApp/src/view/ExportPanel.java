package view;

import dao.ExportReceiptDAO;
import dao.ProductDAO;
import dao.SupplierDAO;
import model.ExportLine;
import model.ExportReason;
import model.ExportReceipt;
import model.Product;
import model.Supplier;
import util.DBConnection;
import util.ErrorHandler;
import util.Session;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Tab lap phieu xuat kho (noi DB that qua DAO)
public class ExportPanel extends JPanel {

    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final ExportReceiptDAO exportReceiptDAO = new ExportReceiptDAO();

    private DefaultTableModel tableModel;
    private JTable exportTable;
    private JComboBox<ExportReason> reasonCombo;
    private List<Supplier> suppliers = new ArrayList<>();
    private List<Integer> rowProductIds = new ArrayList<>();
    private JComboBox<String> supplierCombo;
    private boolean refreshingSuppliers = false;

    public ExportPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(createInfoPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    // Chon ly do xuat va NCC nhan (neu tra hang)
    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        panel.add(new JLabel("Ngày lập: " + LocalDate.now().format(fmt) + "  |"));

        panel.add(new JLabel("Lý do xuất:"));
        reasonCombo = new JComboBox<>(ExportReason.values());
        panel.add(reasonCombo);

        panel.add(new JLabel("  NCC nhận:"));
        supplierCombo = new JComboBox<>();
        loadSuppliers();
        supplierCombo.setEnabled(false); // chi bat khi ly do la RETURN

        reasonCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                boolean isReturn = reasonCombo.getSelectedItem() == ExportReason.RETURN;
                supplierCombo.setEnabled(isReturn);
                if (!isReturn && supplierCombo.getItemCount() > 0) {
                    supplierCombo.setSelectedIndex(0);
                } else if (isReturn && tableModel != null && tableModel.getRowCount() > 0) {
                    // Chuyen sang Tra NCC thi xoa dong cu de tranh san pham khong thuoc NCC
                    clearTable();
                }
            }
        });

        // Khi doi NCC trong che do Tra hang thi xoa bang de chon lai dung SP cua NCC moi
        supplierCombo.addItemListener(e -> {
            if (!refreshingSuppliers
                    && e.getStateChange() == ItemEvent.SELECTED
                    && reasonCombo.getSelectedItem() == ExportReason.RETURN
                    && tableModel != null && tableModel.getRowCount() > 0) {
                clearTable();
            }
        });

        panel.add(supplierCombo);

        return panel;
    }

    private void loadSuppliers() {
        try {
            suppliers = supplierDAO.findAll();
            supplierCombo.removeAllItems();
            supplierCombo.addItem("-- Chọn nhà cung cấp --");
            for (Supplier s : suppliers) {
                supplierCombo.addItem(s.getName());
            }
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);
        }
    }

    // Tai lai danh sach NCC nhung khong xoa bang dang lap
    public void refreshSuppliers() {
        Supplier current = getSelectedSupplier();
        int currentSupId = current != null ? current.getSupplierId() : -1;
        refreshingSuppliers = true;
        try {
            loadSuppliers();
            if (currentSupId != -1) {
                for (int i = 0; i < suppliers.size(); i++) {
                    if (suppliers.get(i).getSupplierId() == currentSupId) {
                        supplierCombo.setSelectedIndex(i + 1);
                        break;
                    }
                }
            }
        } finally {
            refreshingSuppliers = false;
        }
    }

    private Supplier getSelectedSupplier() {
        int supIdx = supplierCombo.getSelectedIndex() - 1; // tru 1 dong placeholder dau tien
        return (supIdx >= 0 && supIdx < suppliers.size()) ? suppliers.get(supIdx) : null;
    }

    private void clearTable() {
        rowProductIds.clear();
        tableModel.setRowCount(0);
    }

    // Lay tap PRODUCT_ID ma nha cung cap nay cung cap tu bang WAREHOUSE.PRODUCT_SUPPLY
    private Set<Integer> getSuppliedProductIds(int supplierId) throws SQLException {
        Set<Integer> ids = new HashSet<>();
        String sql = "SELECT PRODUCT_ID FROM WAREHOUSE.PRODUCT_SUPPLY WHERE SUPPLIER_ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, supplierId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("PRODUCT_ID"));
                }
            }
        }
        return ids;
    }

    // Bang chi tiet hang xuat
    private JScrollPane createTablePanel() {
        String[] columns = {"Sản phẩm", "Tồn hiện tại", "Số lượng xuất"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                // Chi cho sua so luong xuat
                return col == 2;
            }
        };

        exportTable = new JTable(tableModel);
        exportTable.setRowHeight(28);
        exportTable.getTableHeader().setReorderingAllowed(false);

        return new JScrollPane(exportTable);
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));

        JButton btnAdd = new JButton("Thêm dòng");
        JButton btnRemove = new JButton("Xóa dòng");
        JButton btnSave = new JButton("Lưu - Chờ duyệt");

        btnAdd.addActionListener(e -> addRow());
        btnRemove.addActionListener(e -> removeRow());
        btnSave.addActionListener(e -> saveReceipt());

        panel.add(btnAdd);
        panel.add(btnRemove);
        panel.add(Box.createHorizontalStrut(30));
        panel.add(btnSave);

        return panel;
    }

    private void addRow() {
        boolean isReturn = (reasonCombo.getSelectedItem() == ExportReason.RETURN);
        Supplier selectedSupplier = getSelectedSupplier();

        if (isReturn && selectedSupplier == null) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng chọn nhà cung cấp nhận hàng trước khi thêm sản phẩm trả!",
                "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Product> availableProducts;
        try {
            List<Product> allProducts = productDAO.findAll();
            if (isReturn) {
                // Chi loc cac san pham ma NCC dang chon co cung cap
                Set<Integer> suppliedIds = getSuppliedProductIds(selectedSupplier.getSupplierId());
                availableProducts = new ArrayList<>();
                for (Product p : allProducts) {
                    if (suppliedIds.contains(p.getProductId())) {
                        availableProducts.add(p);
                    }
                }
            } else {
                availableProducts = allProducts;
            }
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);
            return;
        }

        if (availableProducts.isEmpty()) {
            String msg = isReturn
                ? "Nhà cung cấp \"" + selectedSupplier.getName() + "\" chưa có sản phẩm nào trong danh mục cung cấp!"
                : "Không có sản phẩm nào trong kho!";
            JOptionPane.showMessageDialog(this, msg, "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String[] options = new String[availableProducts.size()];
        for (int i = 0; i < availableProducts.size(); i++) {
            Product p = availableProducts.get(i);
            options[i] = "[" + p.getProductId() + "] " + p.getName() + " (Tồn: " + p.getCurrentStock() + ")";
        }

        String selectedOption = (String) JOptionPane.showInputDialog(
            this,
            "Chọn sản phẩm cần xuất:",
            "Thêm dòng xuất kho",
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            options[0]
        );

        if (selectedOption == null) return;

        int pickedIndex = -1;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(selectedOption)) {
                pickedIndex = i;
                break;
            }
        }
        if (pickedIndex < 0) return;

        Product p = availableProducts.get(pickedIndex);

        if (p.getCurrentStock() <= 0) {
            JOptionPane.showMessageDialog(this,
                "Sản phẩm \"" + p.getName() + "\" hiện đã hết tồn kho (tồn = 0), không thể xuất!",
                "Hết hàng", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Kiem tra trung san pham theo PRODUCT_ID
        if (rowProductIds.contains(p.getProductId())) {
            JOptionPane.showMessageDialog(this,
                "Sản phẩm \"" + p.getName() + "\" đã có trong phiếu!",
                "Trùng sản phẩm", JOptionPane.WARNING_MESSAGE);
            return;
        }

        rowProductIds.add(p.getProductId());
        tableModel.addRow(new Object[]{p.getName(), p.getCurrentStock(), 1});
    }

    private void removeRow() {
        if (exportTable.isEditing()) {
            exportTable.getCellEditor().stopCellEditing();
        }
        int selectedRow = exportTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng chọn 1 dòng trong bảng để xóa.",
                "Chưa chọn dòng", JOptionPane.WARNING_MESSAGE);
            return;
        }
        rowProductIds.remove(selectedRow);
        tableModel.removeRow(selectedRow);
    }

    private void saveReceipt() {
        if (exportTable.isEditing()) {
            exportTable.getCellEditor().stopCellEditing();
        }

        ExportReason reason = (ExportReason) reasonCombo.getSelectedItem();
        boolean isReturn = (reason == ExportReason.RETURN);
        Supplier selectedSupplier = getSelectedSupplier();

        if (isReturn && selectedSupplier == null) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng chọn nhà cung cấp nhận hàng cho phiếu trả hàng!",
                "Thiếu thông tin", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                "Phiếu xuất phải có ít nhất 1 dòng sản phẩm!",
                "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int employeeId = Session.currentUser != null ? Session.currentUser.getEmployeeId() : 101;

        // Phieu xuat moi lap chua co nguoi duyet (managerId = null)
        ExportReceipt receipt = new ExportReceipt();
        receipt.setEmployeeId(employeeId);
        receipt.setManagerId(null);
        receipt.setExportDate(LocalDate.now());
        receipt.setReason(reason);
        receipt.setApproveTime(null);

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                String productName = tableModel.getValueAt(i, 0).toString();
                int stock = Integer.parseInt(tableModel.getValueAt(i, 1).toString().trim());
                Object qtyObj = tableModel.getValueAt(i, 2);
                if (qtyObj == null) {
                    throw new NumberFormatException();
                }
                int qty = Integer.parseInt(qtyObj.toString().trim());

                if (qty <= 0) {
                    JOptionPane.showMessageDialog(this,
                        "Dòng " + (i + 1) + ": Số lượng xuất phải lớn hơn 0!",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (qty > stock) {
                    JOptionPane.showMessageDialog(this,
                        "Dòng " + (i + 1) + " (" + productName + "):\n"
                        + "Số lượng xuất (" + qty + ") vượt quá tồn kho (" + stock + ")!",
                        "Xuất vượt tồn kho", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int productId = rowProductIds.get(i);
                receipt.getLines().add(new ExportLine(0, productId, qty));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                    "Dòng " + (i + 1) + ": Vui lòng nhập số hợp lệ!",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        Integer returnSupplierId = isReturn ? selectedSupplier.getSupplierId() : null;

        // Luu phieu xuat xuong SQL Server o trang thai cho duyet
        try {
            int newId = exportReceiptDAO.insert(receipt, returnSupplierId);

            StringBuilder summary = new StringBuilder();
            summary.append("PHIẾU XUẤT KHO #").append(newId).append("\n");
            summary.append("─────────────────────────\n");
            summary.append("Lý do: ").append(receipt.getReason().getDisplayName()).append("\n");
            if (isReturn) {
                summary.append("NCC nhận: ").append(selectedSupplier.getName()).append("\n");
            }
            summary.append("Số dòng: ").append(receipt.getLines().size()).append("\n");
            summary.append("─────────────────────────\n");
            summary.append("Trạng thái: Chờ duyệt\n");
            summary.append("Phiếu xuất đã được lưu và đang chờ quản lý duyệt.");

            JOptionPane.showMessageDialog(this, summary.toString(),
                "Lưu phiếu xuất thành công", JOptionPane.INFORMATION_MESSAGE);

            clearTable();
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);
        }
    }
}
