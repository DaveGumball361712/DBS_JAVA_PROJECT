package view;

import model.CheckDetail;
import model.Product;
import model.StockCheck;
import model.StockCheckStatus;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

// Tab lap phieu kiem ke
public class StockCheckPanel extends JPanel {

    private DefaultTableModel tableModel;
    private TableModelListener tableListener;
    private JTable checkTable;
    private JLabel lblStatus;
    private JButton btnCreate;
    private JButton btnSave;
    private boolean isEditing = false;

    public StockCheckPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(createTopPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

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

    private JScrollPane createTablePanel() {
        String[] columns = {"Mã SP", "Tên sản phẩm", "Đơn vị", "Số hệ thống", "Số thực tế", "Chênh lệch"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                // Chi cho nhap cot so thuc te khi dang tao phieu
                return isEditing && col == 4;
            }
        };

        checkTable = new JTable(tableModel);
        checkTable.setRowHeight(28);
        checkTable.getTableHeader().setReorderingAllowed(false);

        checkTable.getColumnModel().getColumn(5).setCellRenderer(new DifferenceRenderer());

        tableListener = e -> {
            if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 4) {
                recalculateDifference(e.getFirstRow());
            }
        };
        tableModel.addTableModelListener(tableListener);

        checkTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        checkTable.getColumnModel().getColumn(1).setPreferredWidth(280);
        checkTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        checkTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        checkTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        checkTable.getColumnModel().getColumn(5).setPreferredWidth(80);

        return new JScrollPane(checkTable);
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));

        btnSave = new JButton("Lưu phiếu kiểm kê");
        btnSave.setEnabled(false);
        btnSave.addActionListener(e -> saveStockCheck());
        panel.add(btnSave);

        return panel;
    }

    private void createStockCheck() {
        if (isEditing) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn đang có phiếu kiểm kê chưa lưu.\nTạo phiếu mới sẽ mất dữ liệu cũ. Tiếp tục?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        // Load toan bo san pham, mac dinh so thuc te = so he thong
        tableModel.setRowCount(0);
        for (Product p : MockData.getProducts()) {
            int systemQty = p.getCurrentStock();
            tableModel.addRow(new Object[]{
                p.getProductId(),
                p.getName(),
                p.getMeasureUnit(),
                systemQty,
                systemQty,
                0
            });
        }

        isEditing = true;
        btnSave.setEnabled(true);
        lblStatus.setText(StockCheckStatus.WAITING_FOR_APPROVAL.getDbValue());
        lblStatus.setForeground(new Color(200, 150, 0));
    }

    private void recalculateDifference(int row) {
        try {
            int systemQty = Integer.parseInt(tableModel.getValueAt(row, 3).toString().trim());
            int actualQty = Integer.parseInt(tableModel.getValueAt(row, 4).toString().trim());

            if (actualQty < 0) {
                JOptionPane.showMessageDialog(this,
                    "Số thực tế không được âm!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                tableModel.setValueAt(systemQty, row, 4);
                return;
            }

            int diff = actualQty - systemQty;
            tableModel.removeTableModelListener(tableListener);
            tableModel.setValueAt(diff, row, 5);
            tableModel.addTableModelListener(tableListener);
            checkTable.repaint();
        } catch (NumberFormatException ignored) {
        }
    }

    private void saveStockCheck() {
        StockCheck stockCheck = new StockCheck();
        stockCheck.setEmployeeId(101);
        stockCheck.setManagerId(null);
        stockCheck.setCheckDate(LocalDate.now());
        stockCheck.setStatus(StockCheckStatus.WAITING_FOR_APPROVAL);
        stockCheck.setApproveTime(null);

        int diffCount = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                int productId = Integer.parseInt(tableModel.getValueAt(i, 0).toString());
                int systemQty = Integer.parseInt(tableModel.getValueAt(i, 3).toString());
                int actualQty = Integer.parseInt(tableModel.getValueAt(i, 4).toString().trim());

                if (actualQty < 0) {
                    JOptionPane.showMessageDialog(this,
                        "Dòng " + (i + 1) + ": Số thực tế không được âm!",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                CheckDetail detail = new CheckDetail(0, productId, systemQty, actualQty);
                if (detail.getDifference() != 0) diffCount++;
                stockCheck.getDetails().add(detail);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                    "Dòng " + (i + 1) + ": Vui lòng nhập số hợp lệ cho Số thực tế!",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        StringBuilder summary = new StringBuilder();
        summary.append("PHIẾU KIỂM KÊ\n");
        summary.append("─────────────────────────\n");
        summary.append("Tổng sản phẩm kiểm: ").append(stockCheck.getDetails().size()).append("\n");
        summary.append("Số SP có chênh lệch: ").append(diffCount).append("\n");
        summary.append("Số SP khớp kho: ").append(stockCheck.getDetails().size() - diffCount).append("\n");
        summary.append("─────────────────────────\n");
        summary.append("Trạng thái: ").append(stockCheck.getStatus().getDisplayName())
               .append(" [").append(stockCheck.getStatus().getDbValue()).append("]\n");
        summary.append("(Quản lý sẽ duyệt trên màn hình Duyệt phiếu)");

        JOptionPane.showMessageDialog(this, summary.toString(),
            "Lưu phiếu kiểm kê thành công", JOptionPane.INFORMATION_MESSAGE);

        isEditing = false;
        btnSave.setEnabled(false);
        lblStatus.setText("ĐÃ LƯU - " + stockCheck.getStatus().getDisplayName().toUpperCase());
        lblStatus.setForeground(new Color(0, 130, 0));
    }

    // To mau cot chenh lech: thieu to do, thua to xanh
    private static class DifferenceRenderer extends DefaultTableCellRenderer {
        private static final Color COLOR_SHORTAGE = new Color(255, 200, 200);
        private static final Color COLOR_SURPLUS  = new Color(200, 255, 200);
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
