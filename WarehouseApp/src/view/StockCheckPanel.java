package view;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.TableModelEvent;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Màn hình Lập Phiếu Kiểm Kê (cho Thủ kho).
 * Tương ứng bảng: WAREHOUSE.STOCK_CHECK + WAREHOUSE.CHECK_DETAIL
 *
 * Chức năng:
 * - Tạo phiếu kiểm kê: load toàn bộ sản phẩm vào bảng
 * - Thủ kho nhập Số thực tế cho từng sản phẩm
 * - Tự động tính Chênh lệch = Số thực tế - Số hệ thống
 * - Tô màu dòng có chênh lệch != 0 (đỏ nếu thiếu, xanh nếu thừa)
 * - Lưu phiếu ở trạng thái "WAITING FOR APPROVAL"
 */
public class StockCheckPanel extends JPanel {

    private DefaultTableModel tableModel;
    private JTable checkTable;
    private JLabel lblStatus;
    private JButton btnCreate;
    private JButton btnSave;
    private boolean isEditing = false; // Đang có phiếu kiểm kê chưa lưu

    public StockCheckPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(createTopPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    // ========== PANEL TRÊN: Nút tạo phiếu + Trạng thái ==========
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        btnCreate = new JButton("Tạo phiếu kiểm kê");
        btnCreate.addActionListener(e -> createStockCheck());
        panel.add(btnCreate);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        panel.add(new JLabel("  |  Ngày: " + LocalDate.now().format(fmt)));

        panel.add(new JLabel("  |  Trạng thái: "));
        lblStatus = new JLabel("CHƯA TẠO");
        lblStatus.setFont(lblStatus.getFont().deriveFont(Font.BOLD));
        lblStatus.setForeground(Color.GRAY);
        panel.add(lblStatus);

        return panel;
    }

    // ========== PANEL GIỮA: Bảng kiểm kê ==========
    private JScrollPane createTablePanel() {
        String[] columns = {"Mã SP", "Tên sản phẩm", "Đơn vị", "Số hệ thống", "Số thực tế", "Chênh lệch"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                // Chỉ cho sửa cột Số thực tế (4) khi đang kiểm kê
                return isEditing && col == 4;
            }
        };

        checkTable = new JTable(tableModel);
        checkTable.setRowHeight(28);
        checkTable.getTableHeader().setReorderingAllowed(false);

        // Tô màu cột Chênh lệch
        checkTable.getColumnModel().getColumn(5).setCellRenderer(new DifferenceRenderer());

        // Lắng nghe khi sửa cột Số thực tế → tính lại Chênh lệch
        tableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 4) {
                recalculateDifference(e.getFirstRow());
            }
        });

        // Điều chỉnh độ rộng cột
        checkTable.getColumnModel().getColumn(0).setPreferredWidth(50);   // Mã SP
        checkTable.getColumnModel().getColumn(1).setPreferredWidth(280);  // Tên SP
        checkTable.getColumnModel().getColumn(2).setPreferredWidth(60);   // Đơn vị
        checkTable.getColumnModel().getColumn(3).setPreferredWidth(80);   // Số HT
        checkTable.getColumnModel().getColumn(4).setPreferredWidth(80);   // Số TT
        checkTable.getColumnModel().getColumn(5).setPreferredWidth(80);   // Chênh lệch

        return new JScrollPane(checkTable);
    }

    // ========== PANEL DƯỚI: Nút lưu ==========
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));

        btnSave = new JButton("Lưu phiếu kiểm kê");
        btnSave.setEnabled(false);
        btnSave.addActionListener(e -> saveStockCheck());
        panel.add(btnSave);

        return panel;
    }

    // ==================== XỬ LÝ SỰ KIỆN ====================

    /** Tạo phiếu kiểm kê: load toàn bộ sản phẩm vào bảng */
    private void createStockCheck() {
        if (isEditing) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn đang có phiếu kiểm kê chưa lưu.\nTạo phiếu mới sẽ mất dữ liệu cũ. Tiếp tục?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        // Xóa bảng cũ và nạp tất cả sản phẩm
        tableModel.setRowCount(0);
        for (int i = 0; i < MockData.PRODUCT_IDS.length; i++) {
            int systemQty = MockData.PRODUCT_STOCKS[i]; // Số hệ thống = tồn kho hiện tại
            tableModel.addRow(new Object[]{
                MockData.PRODUCT_IDS[i],        // Mã SP
                MockData.PRODUCT_NAMES[i],       // Tên SP
                MockData.PRODUCT_UNITS[i],       // Đơn vị
                systemQty,                       // Số hệ thống (chỉ đọc)
                systemQty,                       // Số thực tế (mặc định = Số HT, thủ kho sửa lại)
                0                                // Chênh lệch (ban đầu = 0)
            });
        }

        isEditing = true;
        btnSave.setEnabled(true);
        lblStatus.setText("WAITING FOR APPROVAL");
        lblStatus.setForeground(new Color(200, 150, 0)); // Màu vàng cam
    }

    /** Tính lại Chênh lệch khi thủ kho sửa Số thực tế */
    private void recalculateDifference(int row) {
        try {
            int systemQty = Integer.parseInt(tableModel.getValueAt(row, 3).toString());
            int actualQty = Integer.parseInt(tableModel.getValueAt(row, 4).toString());

            if (actualQty < 0) {
                JOptionPane.showMessageDialog(this,
                    "Số thực tế không được âm!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                // Đặt lại về giá trị cũ
                tableModel.setValueAt(systemQty, row, 4);
                return;
            }

            int diff = actualQty - systemQty;
            // Tạm bỏ listener để tránh loop vô hạn khi set cột Chênh lệch
            tableModel.removeTableModelListener(tableModel.getTableModelListeners()[0]);
            tableModel.setValueAt(diff, row, 5);
            tableModel.addTableModelListener(e -> {
                if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 4) {
                    recalculateDifference(e.getFirstRow());
                }
            });
            checkTable.repaint(); // Vẽ lại để cập nhật màu
        } catch (NumberFormatException ex) {
            // Người dùng đang gõ dở
        }
    }

    /** Validate và lưu phiếu kiểm kê */
    private void saveStockCheck() {
        // Kiểm tra tất cả ô Số thực tế phải là số >= 0
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                int actualQty = Integer.parseInt(tableModel.getValueAt(i, 4).toString());
                if (actualQty < 0) {
                    JOptionPane.showMessageDialog(this,
                        "Dòng " + (i + 1) + ": Số thực tế không được âm!",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                    "Dòng " + (i + 1) + ": Vui lòng nhập số hợp lệ cho Số thực tế!",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Đếm số dòng có chênh lệch
        int diffCount = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            int diff = Integer.parseInt(tableModel.getValueAt(i, 5).toString());
            if (diff != 0) diffCount++;
        }

        StringBuilder summary = new StringBuilder();
        summary.append("PHIẾU KIỂM KÊ\n");
        summary.append("─────────────────────────\n");
        summary.append("Tổng sản phẩm kiểm: ").append(tableModel.getRowCount()).append("\n");
        summary.append("Số SP có chênh lệch: ").append(diffCount).append("\n");
        summary.append("Số SP khớp kho: ").append(tableModel.getRowCount() - diffCount).append("\n");
        summary.append("─────────────────────────\n");
        summary.append("Trạng thái: CHỜ DUYỆT\n");
        summary.append("(Quản lý sẽ duyệt trên màn hình Duyệt phiếu)");

        JOptionPane.showMessageDialog(this, summary.toString(),
            "Lưu phiếu kiểm kê thành công", JOptionPane.INFORMATION_MESSAGE);

        // Khóa bảng lại, không cho sửa nữa
        isEditing = false;
        btnSave.setEnabled(false);
        lblStatus.setText("ĐÃ LƯU - CHỜ DUYỆT");
        lblStatus.setForeground(new Color(0, 130, 0)); // Xanh lá
    }

    // ==================== RENDERER TÔ MÀU ====================

    /**
     * Tô màu ô Chênh lệch:
     * - Đỏ nhạt nếu < 0 (thiếu hàng)
     * - Xanh nhạt nếu > 0 (thừa hàng)
     * - Trắng nếu = 0 (khớp kho)
     */
    private static class DifferenceRenderer extends DefaultTableCellRenderer {
        private static final Color COLOR_SHORTAGE = new Color(255, 200, 200); // Đỏ nhạt
        private static final Color COLOR_SURPLUS  = new Color(200, 255, 200); // Xanh nhạt
        private static final Color COLOR_MATCH    = Color.WHITE;

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

            if (!isSelected && value != null) {
                try {
                    int diff = Integer.parseInt(value.toString());
                    if (diff < 0) {
                        c.setBackground(COLOR_SHORTAGE);
                        c.setForeground(Color.RED);
                    } else if (diff > 0) {
                        c.setBackground(COLOR_SURPLUS);
                        c.setForeground(new Color(0, 100, 0));
                    } else {
                        c.setBackground(COLOR_MATCH);
                        c.setForeground(Color.BLACK);
                    }
                } catch (NumberFormatException e) {
                    c.setBackground(COLOR_MATCH);
                    c.setForeground(Color.BLACK);
                }
            }
            setHorizontalAlignment(CENTER);
            return c;
        }
    }
}
