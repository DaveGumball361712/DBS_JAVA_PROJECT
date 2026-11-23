package view;

import dao.ExportReceiptDAO;
import dao.ProductDAO;
import dao.SupplierDAO;
import model.ExportLine;
import model.ExportReason;
import model.ExportReceipt;
import model.Product;
import model.Supplier;
import util.ErrorHandler;
import util.Session;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Tab lap phieu xuat kho (noi DB that qua DAO)
public class ExportPanel extends JPanel {

    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final ExportReceiptDAO exportReceiptDAO = new ExportReceiptDAO();

    private DefaultTableModel tableModel;
    private JTable exportTable;
    private JComboBox<ExportReason> reasonCombo;
    private List<Supplier> suppliers = new ArrayList<>();
    private List<Product> products = new ArrayList<>();
    private JComboBox<String> supplierCombo;

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
            }
        });

        panel.add(supplierCombo);

        return panel;
    }

    private void loadSuppliers() {
        try {
            suppliers = supplierDAO.findAll();
            supplierCombo.removeAllItems();
            for (Supplier s : suppliers) {
                supplierCombo.addItem(s.getName());
            }
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);
        }
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

    private Product findProductByName(String name) {
        for (Product p : products) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    private void addRow() {
        try {
            products = productDAO.findAll();
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);
            return;
        }

        if (products.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có sản phẩm nào trong kho!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String[] productNames = new String[products.size()];
        for (int i = 0; i < products.size(); i++) {
            productNames[i] = products.get(i).getName();
        }

        String selectedName = (String) JOptionPane.showInputDialog(
            this,
            "Chọn sản phẩm cần xuất:",
            "Thêm dòng xuất kho",
            JOptionPane.PLAIN_MESSAGE,
            null,
            productNames,
            productNames[0]
        );

        if (selectedName == null) return;

        // Kiem tra trung san pham
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (selectedName.equals(tableModel.getValueAt(i, 0))) {
                JOptionPane.showMessageDialog(this,
                    "Sản phẩm \"" + selectedName + "\" đã có trong phiếu!",
                    "Trùng sản phẩm", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        Product p = findProductByName(selectedName);
        if (p == null) return;

        tableModel.addRow(new Object[]{p.getName(), p.getCurrentStock(), 1});
    }

    private void removeRow() {
        int selectedRow = exportTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng chọn 1 dòng trong bảng để xóa.",
                "Chưa chọn dòng", JOptionPane.WARNING_MESSAGE);
            return;
        }
        tableModel.removeRow(selectedRow);
    }

    private void saveReceipt() {
        if (exportTable.isEditing()) {
            exportTable.getCellEditor().stopCellEditing();
        }

        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                "Phiếu xuất phải có ít nhất 1 dòng sản phẩm!",
                "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ExportReason reason = (ExportReason) reasonCombo.getSelectedItem();
        boolean isReturn = (reason == ExportReason.RETURN);
        int supIdx = supplierCombo.getSelectedIndex();
        Supplier selectedSupplier = (supIdx >= 0 && supIdx < suppliers.size()) ? suppliers.get(supIdx) : null;

        if (isReturn && selectedSupplier == null) {
            JOptionPane.showMessageDialog(this,
                "Lý do Trả NCC phải chọn nhà cung cấp nhận hàng!",
                "Lỗi R15", JOptionPane.ERROR_MESSAGE);
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
                int qty = Integer.parseInt(tableModel.getValueAt(i, 2).toString().trim());

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

                Product p = findProductByName(productName);
                if (p != null) {
                    receipt.getLines().add(new ExportLine(0, p.getProductId(), qty));
                }
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
            summary.append("Trạng thái: CHỜ DUYỆT\n");
            summary.append("(Đã lưu vào cơ sở dữ liệu, chờ Quản lý duyệt)");

            JOptionPane.showMessageDialog(this, summary.toString(),
                "Lưu phiếu xuất thành công", JOptionPane.INFORMATION_MESSAGE);

            tableModel.setRowCount(0);
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);
        }
    }
}
