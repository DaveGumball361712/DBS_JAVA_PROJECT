package view;

import model.ImportLine;
import model.ImportReceipt;
import model.Product;
import model.Supplier;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// Tab lap phieu nhap kho
public class ImportPanel extends JPanel {

    private DefaultTableModel tableModel;
    private TableModelListener tableListener;
    private JTable importTable;
    private JLabel lblTotal;
    private List<Supplier> suppliers;
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
        suppliers = MockData.getSuppliers();
        String[] supplierNames = new String[suppliers.size()];
        for (int i = 0; i < suppliers.size(); i++) {
            supplierNames[i] = suppliers.get(i).getName();
        }
        supplierCombo = new JComboBox<>(supplierNames);
        panel.add(supplierCombo);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        panel.add(new JLabel("  |  Ngày lập: " + LocalDateTime.now().format(fmt)));

        panel.add(new JLabel("  |  Ghi chú:"));
        txtNote = new JTextField(15);
        panel.add(txtNote);

        return panel;
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
        List<Product> products = MockData.getProducts();
        String[] productNames = new String[products.size()];
        for (int i = 0; i < products.size(); i++) {
            productNames[i] = products.get(i).getName();
        }

        String selectedName = (String) JOptionPane.showInputDialog(
            this,
            "Chọn sản phẩm cần nhập:",
            "Thêm dòng nhập kho",
            JOptionPane.PLAIN_MESSAGE,
            null,
            productNames,
            productNames[0]
        );

        if (selectedName == null) return;

        // Khong cho them trung san pham trong cung 1 phieu
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (selectedName.equals(tableModel.getValueAt(i, 0))) {
                JOptionPane.showMessageDialog(this,
                    "Sản phẩm \"" + selectedName + "\" đã có trong phiếu!\n"
                    + "Hãy sửa số lượng ở dòng " + (i + 1) + " thay vì thêm mới.",
                    "Trùng sản phẩm", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        Product p = MockData.findProductByName(selectedName);
        if (p == null) return;

        BigDecimal defaultPrice = p.getCurrentPrice();
        tableModel.addRow(new Object[]{p.getName(), 1, defaultPrice, defaultPrice});
        updateTotal();
    }

    private void removeRow() {
        int selectedRow = importTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng chọn 1 dòng trong bảng để xóa.",
                "Chưa chọn dòng", JOptionPane.WARNING_MESSAGE);
            return;
        }
        tableModel.removeRow(selectedRow);
        updateTotal();
    }

    private void recalculateRow(int row) {
        try {
            int qty = Integer.parseInt(tableModel.getValueAt(row, 1).toString().trim());
            BigDecimal price = new BigDecimal(tableModel.getValueAt(row, 2).toString().trim());
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(qty));

            // Tam go listener de tranh lap vo han khi set cot thanh tien
            tableModel.removeTableModelListener(tableListener);
            tableModel.setValueAt(lineTotal, row, 3);
            tableModel.addTableModelListener(tableListener);
        } catch (Exception ignored) {
        }
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

        // Tao model ImportReceipt de chuan bi truyen vao DAO
        ImportReceipt receipt = new ImportReceipt();
        receipt.setEmployeeId(101); // tam dung id co dinh, sau lay tu Session
        receipt.setSupplierId(selectedSupplier.getSupplierId());
        receipt.setImportDate(LocalDateTime.now());
        receipt.setDescription(txtNote.getText().trim());

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                String productName = tableModel.getValueAt(i, 0).toString();
                int qty = Integer.parseInt(tableModel.getValueAt(i, 1).toString().trim());
                BigDecimal price = new BigDecimal(tableModel.getValueAt(i, 2).toString().trim());

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

                Product p = MockData.findProductByName(productName);
                if (p != null) {
                    receipt.getLines().add(new ImportLine(0, p.getProductId(), price, qty));
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Dòng " + (i + 1) + ": Vui lòng nhập số hợp lệ!",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        StringBuilder summary = new StringBuilder();
        summary.append("PHIẾU NHẬP KHO\n");
        summary.append("─────────────────────────\n");
        summary.append("NCC: ").append(selectedSupplier.getName()).append(" (ID: ").append(receipt.getSupplierId()).append(")\n");
        summary.append("Ghi chú: ").append(receipt.getDescription()).append("\n");
        summary.append("Số dòng: ").append(receipt.getLines().size()).append("\n");
        summary.append(String.format("Tổng tiền (model): %,.0f VNĐ\n", receipt.getTotal()));
        summary.append("─────────────────────────\n");
        summary.append("Trạng thái: ĐÃ ĐÓNG GÓI MODEL THÀNH CÔNG");

        JOptionPane.showMessageDialog(this, summary.toString(),
            "Lưu phiếu nhập thành công", JOptionPane.INFORMATION_MESSAGE);

        tableModel.setRowCount(0);
        txtNote.setText("");
        updateTotal();
    }
}
