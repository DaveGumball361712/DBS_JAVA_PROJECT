package view;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ApprovalPanel extends JPanel {

    private final DefaultTableModel exportModel =
        createModel("Ma phieu", "Nhan vien kho", "Ngay xuat", "Ly do");
    private final DefaultTableModel checkModel =
        createModel("Ma phieu", "Nhan vien kho", "Ngay kiem", "Trang thai");

    private final JTable exportTable = createTable(exportModel);
    private final JTable checkTable = createTable(checkModel);

    public ApprovalPanel() {
        setLayout(new GridLayout(2, 1, 0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton approveExport = new JButton("Duyet phieu xuat");
        approveExport.addActionListener(e -> approveSelected(exportTable, exportModel, "phieu xuat"));

        JButton approveCheck = new JButton("Duyet phieu kiem ke");
        approveCheck.addActionListener(e -> approveSelected(checkTable, checkModel, "phieu kiem ke"));

        add(createSection("Phieu xuat cho duyet", exportTable, approveExport));
        add(createSection("Phieu kiem ke cho duyet", checkTable, approveCheck));

        loadFakeData();
    }

    private static DefaultTableModel createModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setRowHeight(24);
        return table;
    }

    private JPanel createSection(String title, JTable table, JButton approveButton) {
        JPanel section = new JPanel(new BorderLayout(0, 6));
        section.setBorder(BorderFactory.createTitledBorder(title));
        section.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonBar.add(approveButton);
        section.add(buttonBar, BorderLayout.SOUTH);
        return section;
    }

private void approveSelected(JTable table, DefaultTableModel model, String type) {
    int[] rows = table.getSelectedRows();
    if (rows.length == 0) {
        JOptionPane.showMessageDialog(this, "Hay chon it nhat mot " + type,
            "Chua chon phieu", JOptionPane.WARNING_MESSAGE);
        return;
    }

    StringBuilder ids = new StringBuilder();
    for (int row : rows) {
        if (ids.length() > 0) {
            ids.append(", ");
        }
        ids.append(model.getValueAt(row, 0));
    }

    int answer = JOptionPane.showConfirmDialog(this,
        "Duyet " + rows.length + " " + type + ": " + ids + "?",
        "Xac nhan", JOptionPane.YES_NO_OPTION);

    if (answer == JOptionPane.YES_OPTION) {
        for (int i = rows.length - 1; i >= 0; i--) {
            model.removeRow(rows[i]);
        }
        JOptionPane.showMessageDialog(this, "Da duyet " + type + ": " + ids);
    }
}

    private void loadFakeData() {
        exportModel.addRow(new Object[] {101, "Nguyen Van An", "2026-11-10", "EXPIRED"});
        exportModel.addRow(new Object[] {102, "Tran Thi Binh", "2026-11-11", "SPOILED OR BROKEN"});
        exportModel.addRow(new Object[] {103, "Le Van Cuong", "2026-11-12", "RETURN"});

        checkModel.addRow(new Object[] {201, "Nguyen Van An", "2026-11-09", "WAITING FOR APPROVAL"});
        checkModel.addRow(new Object[] {202, "Pham Thi Dung", "2026-11-12", "WAITING FOR APPROVAL"});
    }
}
