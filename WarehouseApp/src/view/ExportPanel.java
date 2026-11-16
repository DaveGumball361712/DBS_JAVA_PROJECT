package view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Màn hình Lập Phiếu Xuất Kho (cho Thủ kho).
 * Tương ứng bảng: WAREHOUSE.EXPORT_RECEIPT + WAREHOUSE.EXPORT_FROM
 *                  + WAREHOUSE.RETURN_SUPPLIER (nếu lý do RETURN)
 *
 * Chức năng:
 * - Chọn lý do xuất: Huỷ hỏng / Hết hạn / Trả NCC
 * - Nếu lý do = Trả NCC → bắt buộc chọn nhà cung cấp nhận hàng (R15)
 * - Thêm/Xóa dòng sản phẩm xuất kho
 * - Kiểm tra: Số lượng xuất phải > 0 và <= Tồn kho hiện tại
 * - Lưu phiếu ở trạng thái "Chờ duyệt" (MANAGER_ID = NULL)
 */
public class ExportPanel extends JPanel {

    // Giá trị lý do khớp với CHECK constraint trong DB
    private static final String[] REASON_DISPLAY = {
        "Huỷ hỏng",
        "Hết hạn",
        "Trả NCC"
    };

    private DefaultTableModel tableModel;
    private JTable exportTable;
    private JComboBox<String> reasonCombo;
    private JComboBox<String> supplierCombo;

    public ExportPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(createInfoPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    // ========== PANEL TRÊN: Lý do xuất + NCC nhận ==========
    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        panel.add(new JLabel("Ngày lập: " + LocalDate.now().format(fmt) + "  |"));

        panel.add(new JLabel("Lý do xuất:"));
        reasonCombo = new JComboBox<>(REASON_DISPLAY);
        panel.add(reasonCombo);

        panel.add(new JLabel("  NCC nhận:"));
        supplierCombo = new JComboBox<>(MockData.SUPPLIER_NAMES);
        supplierCombo.setEnabled(false); // Mặc định tắt, chỉ bật khi chọn "Trả NCC"

        // Sự kiện đổi lý do xuất (R15)
        reasonCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                boolean isReturn = reasonCombo.getSelectedIndex() == 2;
                supplierCombo.setEnabled(isReturn);
            }
        });

        panel.add(supplierCombo);

        return panel;
    }

    // ========== PANEL GIỮA: Bảng chi tiết hàng xuất ==========
    private JScrollPane createTablePanel() {
        String[] columns = {"Sản phẩm", "Tồn hiện tại", "Số lượng xuất"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                // Chỉ cho sửa cột Số lượng xuất (2)
                // Cột Sản phẩm (0) và Tồn hiện tại (1) chỉ đọc
                return col == 2;
            }
        };

        exportTable = new JTable(tableModel);
        exportTable.setRowHeight(28);
        exportTable.getTableHeader().setReorderingAllowed(false);

        return new JScrollPane(exportTable);
    }

    // ========== PANEL DƯỚI: Nút thao tác ==========
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

    // ==================== XỬ LÝ SỰ KIỆN ====================

    /** Thêm 1 dòng: chọn sản phẩm, tự động điền tồn kho */
    private void addRow() {
        String selected = (String) JOptionPane.showInputDialog(
            this,
            "Chọn sản phẩm cần xuất:",
            "Thêm dòng xuất kho",
            JOptionPane.PLAIN_MESSAGE,
            null,
            MockData.PRODUCT_NAMES,
            MockData.PRODUCT_NAMES[0]
        );

        if (selected == null) return;

        // Kiểm tra trùng
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (selected.equals(tableModel.getValueAt(i, 0))) {
                JOptionPane.showMessageDialog(this,
                    "Sản phẩm \"" + selected + "\" đã có trong phiếu!",
                    "Trùng sản phẩm", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        int idx = MockData.findProductIndex(selected);
        int currentStock = MockData.PRODUCT_STOCKS[idx];
        // Sản phẩm | Tồn hiện tại | Số lượng xuất (mặc định = 1)
        tableModel.addRow(new Object[]{selected, currentStock, 1});
    }

    /** Xóa dòng đang chọn */
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

    /** Validate và lưu phiếu xuất */
    private void saveReceipt() {
        // 1. Kiểm tra ít nhất 1 dòng
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                "Phiếu xuất phải có ít nhất 1 dòng sản phẩm!",
                "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. Nếu lý do = Trả NCC (R15), phải chọn nhà cung cấp
        boolean isReturn = reasonCombo.getSelectedIndex() == 2;
        if (isReturn && !supplierCombo.isEnabled()) {
            JOptionPane.showMessageDialog(this,
                "Lý do Trả NCC phải chọn nhà cung cấp nhận hàng!",
                "Lỗi R15", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 3. Kiểm tra từng dòng: 0 < Số lượng xuất <= Tồn hiện tại
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                int stock = Integer.parseInt(tableModel.getValueAt(i, 1).toString());
                int qty = Integer.parseInt(tableModel.getValueAt(i, 2).toString());
                String productName = tableModel.getValueAt(i, 0).toString();

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
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                    "Dòng " + (i + 1) + ": Vui lòng nhập số hợp lệ!",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // 4. Lưu thành công
        StringBuilder summary = new StringBuilder();
        summary.append("PHIẾU XUẤT KHO\n");
        summary.append("─────────────────────────\n");
        summary.append("Lý do: ").append(reasonCombo.getSelectedItem()).append("\n");
        if (isReturn) {
            summary.append("NCC nhận: ").append(supplierCombo.getSelectedItem()).append("\n");
        }
        summary.append("Số dòng: ").append(tableModel.getRowCount()).append("\n");
        summary.append("─────────────────────────\n");
        summary.append("Trạng thái: CHỜ DUYỆT\n");
        summary.append("(Quản lý sẽ duyệt trên màn hình Duyệt phiếu)");

        JOptionPane.showMessageDialog(this, summary.toString(),
            "Lưu phiếu xuất thành công", JOptionPane.INFORMATION_MESSAGE);

        // Xóa trắng form
        tableModel.setRowCount(0);
    }
}
