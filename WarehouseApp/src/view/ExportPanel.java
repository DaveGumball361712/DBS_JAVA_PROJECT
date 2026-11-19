package view;

import model.ExportLine;
import model.ExportReason;
import model.ExportReceipt;
import model.Product;
import model.ReturnSupplier;
import model.Supplier;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

// Tab lap phieu xuat kho
public class ExportPanel extends JPanel {

    private DefaultTableModel tableModel;
    private JTable exportTable;
    private JComboBox<ExportReason> reasonCombo;
    private List<Supplier> suppliers;
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
        suppliers = MockData.getSuppliers();
        String[] supplierNames = new String[suppliers.size()];
        for (int i = 0; i < suppliers.size(); i++) {
            supplierNames[i] = suppliers.get(i).getName();
        }
        supplierCombo = new JComboBox<>(supplierNames);
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
        List<Product> products = MockData.getProducts();
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

        Product p = MockData.findProductByName(selectedName);
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

        // Phieu xuat moi lap chua co nguoi duyet (managerId = null)
        ExportReceipt receipt = new ExportReceipt();
        receipt.setEmployeeId(101);
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

                Product p = MockData.findProductByName(productName);
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

        ReturnSupplier returnInfo = isReturn
            ? new ReturnSupplier(0, selectedSupplier.getSupplierId())
            : null;

        StringBuilder summary = new StringBuilder();
        summary.append("PHIẾU XUẤT KHO\n");
        summary.append("─────────────────────────\n");
        summary.append("Lý do: ").append(receipt.getReason().getDisplayName())
               .append(" [").append(receipt.getReason().getDbValue()).append("]\n");
        if (returnInfo != null) {
            summary.append("NCC nhận: ").append(selectedSupplier.getName())
                   .append(" (ID: ").append(returnInfo.getSupplierId()).append(")\n");
        }
        summary.append("Số dòng: ").append(receipt.getLines().size()).append("\n");
        summary.append("─────────────────────────\n");
        summary.append("Trạng thái: CHỜ DUYỆT (isApproved = ").append(receipt.isApproved()).append(")\n");
        summary.append("(Quản lý sẽ duyệt trên màn hình Duyệt phiếu)");

        JOptionPane.showMessageDialog(this, summary.toString(),
            "Lưu phiếu xuất thành công", JOptionPane.INFORMATION_MESSAGE);

        tableModel.setRowCount(0);
    }
}
