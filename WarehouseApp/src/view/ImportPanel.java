package view;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Màn hình Lập Phiếu Nhập Kho (cho Thủ kho).
 * Tương ứng bảng: WAREHOUSE.IMPORT_RECEIPT + WAREHOUSE.INCLUDES
 *
 * Chức năng:
 * - Chọn nhà cung cấp, tự động ghi ngày lập, nhập ghi chú
 * - Thêm/Xóa dòng sản phẩm nhập kho
 * - Tự động tính Thành tiền = Số lượng × Giá nhập
 * - Tự động cập nhật Tổng tiền phiếu
 * - Validate trước khi lưu: ít nhất 1 dòng, không trùng SP, SL > 0
 */
public class ImportPanel extends JPanel {

    private DefaultTableModel  tableModel;
    private JTable importTable;
    private JLabel lblTotal;
    private JComboBox<String> supplierCombo;
    private JTextField txtNote;

    public ImportPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(createInfoPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    // ========== PANEL TRÊN: Thông tin chung phiếu nhập ==========
    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        panel.add(new JLabel("Nhà cung cấp:"));
        supplierCombo = new JComboBox<>(MockData.SUPPLIER_NAMES);
        panel.add(supplierCombo);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        panel.add(new JLabel("  |  Ngày lập: " + LocalDateTime.now().format(fmt)));

        panel.add(new JLabel("  |  Ghi chú:"));
        txtNote = new JTextField(15);
        panel.add(txtNote);

        return panel;
    }

    // ========== PANEL GIỮA: Bảng chi tiết hàng nhập ==========
    private JScrollPane createTablePanel() {
        String[] columns = {"Sản phẩm", "Số lượng", "Giá nhập", "Thành tiền"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                // Chỉ cho sửa cột Số lượng (1) và Giá nhập (2)
                // Cột Sản phẩm (0) và Thành tiền (3) chỉ đọc
                return col == 1 || col == 2;
            }
        };

        importTable = new JTable(tableModel);
        importTable.setRowHeight(28);
        importTable.getTableHeader().setReorderingAllowed(false);

        // Lắng nghe khi người dùng sửa ô trong bảng → tính lại Thành tiền
        tableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int col = e.getColumn();
                // Chỉ tính lại khi sửa cột Số lượng (1) hoặc Giá nhập (2)
                if (col == 1 || col == 2) {
                    recalculateRow(row);
                    updateTotal();
                }
            }
        });

        return new JScrollPane(importTable);
    }

    // ========== PANEL DƯỚI: Nút thao tác và Tổng tiền ==========
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

    // ==================== XỬ LÝ SỰ KIỆN ====================

    /** Thêm 1 dòng hàng mới: hiện hộp thoại chọn sản phẩm */
    private void addRow() {
        String selected = (String) JOptionPane.showInputDialog(
            this,
            "Chọn sản phẩm cần nhập:",
            "Thêm dòng nhập kho",
            JOptionPane.PLAIN_MESSAGE,
            null,
            MockData.PRODUCT_NAMES,
            MockData.PRODUCT_NAMES[0]
        );

        if (selected == null) return; // Người dùng bấm Cancel

        // Kiểm tra trùng sản phẩm trong phiếu
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (selected.equals(tableModel.getValueAt(i, 0))) {
                JOptionPane.showMessageDialog(this,
                    "Sản phẩm \"" + selected + "\" đã có trong phiếu!\n"
                    + "Hãy sửa số lượng ở dòng " + (i + 1) + " thay vì thêm mới.",
                    "Trùng sản phẩm", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        // Thêm dòng mới: Sản phẩm, Số lượng = 1, Giá nhập = giá bán mặc định, Thành tiền
        int idx = MockData.findProductIndex(selected);
        double defaultPrice = MockData.PRODUCT_PRICES[idx];
        tableModel.addRow(new Object[]{selected, 1, defaultPrice, defaultPrice});
        updateTotal();
    }

    /** Xóa dòng đang chọn */
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

    /** Tính lại Thành tiền của 1 dòng */
    private void recalculateRow(int row) {
        try {
            int qty = Integer.parseInt(tableModel.getValueAt(row, 1).toString());
            double price = Double.parseDouble(tableModel.getValueAt(row, 2).toString());
            // Tạm tắt listener để tránh gọi đệ quy khi set cột Thành tiền
            tableModel.removeTableModelListener(tableModel.getTableModelListeners()[0]);
            tableModel.setValueAt(qty * price, row, 3);
            tableModel.addTableModelListener(e -> {
                if (e.getType() == TableModelEvent.UPDATE && (e.getColumn() == 1 || e.getColumn() == 2)) {
                    recalculateRow(e.getFirstRow());
                    updateTotal();
                }
            });
        } catch (NumberFormatException ignored) {
            // Người dùng đang gõ dở, chưa phải số hợp lệ → bỏ qua
        }
    }

    /** Cập nhật nhãn Tổng tiền phiếu */
    private void updateTotal() {
        double total = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                total += Double.parseDouble(tableModel.getValueAt(i, 3).toString());
            } catch (Exception ignored) {}
        }
        lblTotal.setText(String.format("Tổng tiền: %,.0f VNĐ", total));
    }

    /** Validate và lưu phiếu nhập */
    private void saveReceipt() {
        // 1. Kiểm tra ít nhất 1 dòng
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                "Phiếu nhập phải có ít nhất 1 dòng sản phẩm!",
                "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. Kiểm tra từng dòng: Số lượng > 0, Giá nhập >= 0
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                int qty = Integer.parseInt(tableModel.getValueAt(i, 1).toString());
                double price = Double.parseDouble(tableModel.getValueAt(i, 2).toString());
                if (qty <= 0) {
                    JOptionPane.showMessageDialog(this,
                        "Dòng " + (i + 1) + ": Số lượng phải lớn hơn 0!",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (price < 0) {
                    JOptionPane.showMessageDialog(this,
                        "Dòng " + (i + 1) + ": Giá nhập không được âm!",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                    "Dòng " + (i + 1) + ": Vui lòng nhập số hợp lệ!",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // 3. Lưu thành công → hiển thị tóm tắt
        StringBuilder summary = new StringBuilder();
        summary.append("PHIẾU NHẬP KHO\n");
        summary.append("─────────────────────────\n");
        summary.append("NCC: ").append(supplierCombo.getSelectedItem()).append("\n");
        summary.append("Ghi chú: ").append(txtNote.getText()).append("\n");
        summary.append("Số dòng: ").append(tableModel.getRowCount()).append("\n");
        summary.append(lblTotal.getText()).append("\n");
        summary.append("─────────────────────────\n");
        summary.append("Trạng thái: ĐÃ LƯU THÀNH CÔNG");

        JOptionPane.showMessageDialog(this, summary.toString(),
            "Lưu phiếu nhập thành công", JOptionPane.INFORMATION_MESSAGE);

        // Xóa trắng form để lập phiếu mới
        tableModel.setRowCount(0);
        txtNote.setText("");
        updateTotal();
    }
}
