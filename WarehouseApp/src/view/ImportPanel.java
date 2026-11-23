package view;

import dao.ImportReceiptDAO;
import dao.ProductDAO;
import dao.SupplierDAO;
import model.ImportLine;
import model.ImportReceipt;
import model.Product;
import model.Supplier;
import util.ErrorHandler;
import util.Session;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Tab lap phieu nhap kho (noi DB that qua DAO)
public class ImportPanel extends JPanel {

    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final ImportReceiptDAO importReceiptDAO = new ImportReceiptDAO();

    private DefaultTableModel tableModel;
    private TableModelListener tableListener;
    private JTable importTable;
    private JLabel lblTotal;
    private List<Supplier> suppliers = new ArrayList<>();
    private List<Integer> rowProductIds = new ArrayList<>();
    private JComboBox<String> supplierCombo;
    private JTextField txtNote;

    public ImportPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(createInfoPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    // Thong tin chung phieu nhap (NCC, ngay lap, ghi chu)
    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        panel.add(new JLabel("Nhà cung cấp:"));
        supplierCombo = new JComboBox<>();
        loadSuppliers();
        panel.add(supplierCombo);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        panel.add(new JLabel("  |  Ngày lập: " + LocalDateTime.now().format(fmt)));

        panel.add(new JLabel("  |  Ghi chú:"));
        txtNote = new JTextField(15);
        panel.add(txtNote);

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

    // Tai lai danh sach NCC nhung giu nguyen NCC dang chon
    public void refreshSuppliers() {
        int currentSupId = -1;
        int curIdx = supplierCombo.getSelectedIndex();
        if (curIdx >= 0 && curIdx < suppliers.size()) {
            currentSupId = suppliers.get(curIdx).getSupplierId();
        }
        loadSuppliers();
        if (currentSupId != -1) {
            for (int i = 0; i < suppliers.size(); i++) {
                if (suppliers.get(i).getSupplierId() == currentSupId) {
                    supplierCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    // Bang danh sach mat hang nhap
    private JScrollPane createTablePanel() {
        String[] columns = {"Sản phẩm", "Số lượng", "Giá nhập", "Thành tiền"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                // Chi cho sua so luong va gia nhap
                return col == 1 || col == 2;
            }
        };

        importTable = new JTable(tableModel);
        importTable.setRowHeight(28);
        importTable.getTableHeader().setReorderingAllowed(false);

        // Tu tinh lai thanh tien va tong tien khi sua o
        tableListener = e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                int col = e.getColumn();
                if (col == 1 || col == 2) {
                    recalculateRow(e.getFirstRow());
                    updateTotal();
                }
            }
        };
        tableModel.addTableModelListener(tableListener);

        return new JScrollPane(importTable);
    }

    // Thanh nut bam va tong tien
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));

        JButton btnAdd = new JButton("Thêm dòng");
        JButton btnRemove = new JButton("Xóa dòng");
        JButton btnSave = new JButton("Lưu Phiếu Nhập");
        lblTotal = new JLabel("Tổng tiền: 0 VNĐ");
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD));

        btnAdd.addActionListener(e -> addRow());
        btnRemove.addActionListener(e -> removeRow());
        btnSave.addActionListener(e -> saveReceipt());

        panel.add(btnAdd);
        panel.add(btnRemove);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(lblTotal);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(btnSave);

        return panel;
    }

    private void addRow() {
        List<Product> products;
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

        String[] options = new String[products.size()];
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            options[i] = "[" + p.getProductId() + "] " + p.getName();
        }

        String selectedOption = (String) JOptionPane.showInputDialog(
            this,
            "Chọn sản phẩm cần nhập:",
            "Thêm dòng nhập kho",
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

        Product p = products.get(pickedIndex);

        // Khong cho them trung san pham (kiem tra theo PRODUCT_ID)
        int dupRow = rowProductIds.indexOf(p.getProductId());
        if (dupRow != -1) {
            JOptionPane.showMessageDialog(this,
                "Sản phẩm \"" + p.getName() + "\" đã có trong phiếu!\n"
                + "Hãy sửa số lượng ở dòng " + (dupRow + 1) + " thay vì thêm mới.",
                "Trùng sản phẩm", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal defaultPrice = p.getCurrentPrice();
        rowProductIds.add(p.getProductId());
        tableModel.addRow(new Object[]{p.getName(), 1, defaultPrice, defaultPrice});
        updateTotal();
    }

    private void removeRow() {
        if (importTable.isEditing()) {
            importTable.getCellEditor().stopCellEditing();
        }
        int selectedRow = importTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng chọn 1 dòng trong bảng để xóa.",
                "Chưa chọn dòng", JOptionPane.WARNING_MESSAGE);
            return;
        }
        rowProductIds.remove(selectedRow);
        tableModel.removeRow(selectedRow);
        updateTotal();
    }

    private void recalculateRow(int row) {
        BigDecimal lineTotal = BigDecimal.ZERO;
        try {
            int qty = Integer.parseInt(tableModel.getValueAt(row, 1).toString().trim());
            BigDecimal price = new BigDecimal(tableModel.getValueAt(row, 2).toString().trim());
            if (qty > 0 && price.compareTo(BigDecimal.ZERO) >= 0) {
                lineTotal = price.multiply(BigDecimal.valueOf(qty));
            }
        } catch (Exception ignored) {
            // Neu go sai dinh dang so thi dat tam thanh tien = 0
        }

        // Tam go listener de tranh lap vo han khi set cot thanh tien
        tableModel.removeTableModelListener(tableListener);
        tableModel.setValueAt(lineTotal, row, 3);
        tableModel.addTableModelListener(tableListener);
    }

    private void updateTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                total = total.add(new BigDecimal(tableModel.getValueAt(i, 3).toString().trim()));
            } catch (Exception ignored) {
            }
        }
        lblTotal.setText(String.format("Tổng tiền: %,.0f VNĐ", total));
    }

    private void saveReceipt() {
        if (importTable.isEditing()) {
            importTable.getCellEditor().stopCellEditing();
        }

        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                "Phiếu nhập phải có ít nhất 1 dòng sản phẩm!",
                "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int idx = supplierCombo.getSelectedIndex();
        if (idx < 0 || idx >= suppliers.size()) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng chọn nhà cung cấp!",
                "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Supplier selectedSupplier = suppliers.get(idx);

        int employeeId = Session.currentUser != null ? Session.currentUser.getEmployeeId() : 101;

        ImportReceipt receipt = new ImportReceipt();
        receipt.setEmployeeId(employeeId);
        receipt.setSupplierId(selectedSupplier.getSupplierId());
        receipt.setImportDate(LocalDateTime.now());
        receipt.setDescription(txtNote.getText().trim());

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                Object qtyObj = tableModel.getValueAt(i, 1);
                Object priceObj = tableModel.getValueAt(i, 2);
                if (qtyObj == null || priceObj == null) {
                    throw new NumberFormatException();
                }

                int qty = Integer.parseInt(qtyObj.toString().trim());
                BigDecimal price = new BigDecimal(priceObj.toString().trim());

                if (qty <= 0) {
                    JOptionPane.showMessageDialog(this,
                        "Dòng " + (i + 1) + ": Số lượng phải lớn hơn 0!",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (price.compareTo(BigDecimal.ZERO) < 0) {
                    JOptionPane.showMessageDialog(this,
                        "Dòng " + (i + 1) + ": Giá nhập không được âm!",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int productId = rowProductIds.get(i);
                receipt.getLines().add(new ImportLine(0, productId, price, qty));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Dòng " + (i + 1) + ": Vui lòng nhập số hợp lệ!",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Luu xuong SQL Server (Trigger TRG_INCLUDES_ADD_STOCK tu cong ton kho)
        try {
            int newId = importReceiptDAO.insert(receipt);

            StringBuilder summary = new StringBuilder();
            summary.append("PHIẾU NHẬP KHO #").append(newId).append("\n");
            summary.append("─────────────────────────\n");
            summary.append("NCC: ").append(selectedSupplier.getName()).append("\n");
            summary.append("Ghi chú: ").append(receipt.getDescription()).append("\n");
            summary.append("Số dòng: ").append(receipt.getLines().size()).append("\n");
            summary.append(String.format("Tổng tiền: %,.0f VNĐ\n", receipt.getTotal()));
            summary.append("─────────────────────────\n");
            summary.append("Đã lưu phiếu nhập và cập nhật tồn kho thành công.");

            JOptionPane.showMessageDialog(this, summary.toString(),
                "Lưu phiếu nhập thành công", JOptionPane.INFORMATION_MESSAGE);

            rowProductIds.clear();
            tableModel.setRowCount(0);
            txtNote.setText("");
            updateTotal();
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);
        }
    }
}
