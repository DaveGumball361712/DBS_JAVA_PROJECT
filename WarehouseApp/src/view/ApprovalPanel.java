package view;
import java.awt.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import dao.EmployeeDAO;
import dao.ExportReceiptDAO;
import dao.StockCheckDAO;
import model.Employee;
import model.ExportReceipt;
import model.StockCheck;
import util.ErrorHandler;
import util.Session;

public class ApprovalPanel extends JPanel {

    // Mot hanh dong duyet: nhan ma phieu + ma quan ly, co the nem SQLException
    private interface ApproveAction {
        void approve(int receiptId, int managerId) throws SQLException;
    }

    private final ExportReceiptDAO exportDAO = new ExportReceiptDAO();
    private final StockCheckDAO checkDAO = new StockCheckDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final Map<Integer, String> nameCache = new HashMap<>();

    private final DefaultTableModel exportModel =
        createModel("Ma phieu", "Nhan vien kho", "Ngay xuat", "Ly do", "So mat hang", "Tong so luong");
    private final DefaultTableModel checkModel =
        createModel("Ma phieu", "Nhan vien kho", "Ngay kiem", "So san pham", "So SP lech");

    private final JTable exportTable = createTable(exportModel);
    private final JTable checkTable = createTable(checkModel);

    public ApprovalPanel() {
        setLayout(new GridLayout(2, 1, 0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton approveExport = new JButton("Duyet phieu xuat");
        approveExport.addActionListener(e ->
            approveSelected(exportTable, exportModel, "phieu xuat", exportDAO::approve));

        JButton approveCheck = new JButton("Duyet phieu kiem ke");
        approveCheck.addActionListener(e ->
            approveSelected(checkTable, checkModel, "phieu kiem ke", checkDAO::approve));

        JButton refresh = new JButton("Lam moi");
        refresh.addActionListener(e -> loadData());

        add(createSection("Phieu xuat cho duyet", exportTable, refresh, approveExport));
        add(createSection("Phieu kiem ke cho duyet", checkTable, approveCheck));

        loadData();
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

    private JPanel createSection(String title, JTable table, JButton... buttons) {
        JPanel section = new JPanel(new BorderLayout(0, 6));
        section.setBorder(BorderFactory.createTitledBorder(title));
        section.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        for (JButton b : buttons) {
            buttonBar.add(b);
        }
        section.add(buttonBar, BorderLayout.SOUTH);
        return section;
    }

    // Doc phieu cho duyet tu database va do vao 2 bang
    private void loadData() {
        try {
            exportModel.setRowCount(0);
            for (ExportReceipt r : exportDAO.findPending()) {
                exportModel.addRow(new Object[] {
                    r.getExportReceiptId(),
                    employeeName(r.getEmployeeId()),
                    r.getExportDate(),
                    r.getReason().getDisplayName(),
                    r.getLines().size(),
                    r.getLines().stream().mapToInt(l -> l.getQuantity()).sum()  
                });
            }

            checkModel.setRowCount(0);
            for (StockCheck sc : checkDAO.findPending()) {
                long diffCount = sc.getDetails().stream()
                    .filter(d -> d.getDifference() != 0)
                    .count();
                checkModel.addRow(new Object[] {
                    sc.getStockCheckId(),
                    employeeName(sc.getEmployeeId()),
                    sc.getCheckDate(),
                    sc.getDetails().size(),
                    diffCount
                });
            }
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);
        }
    }

    // Lay ten nhan vien theo ma
    private String employeeName(int employeeId) throws SQLException {
        if (!nameCache.containsKey(employeeId)) {
            Employee e = employeeDAO.findById(employeeId);
            nameCache.put(employeeId, e != null ? e.getFullName() : "NV #" + employeeId);
        }
        return nameCache.get(employeeId);
    }

    private void approveSelected(JTable table, DefaultTableModel model, String type, ApproveAction action) {
        if (Session.currentUser == null) {
            JOptionPane.showMessageDialog(this, "Hay dang nhap bang tai khoan quan ly de duyet phieu.",
                "Chua dang nhap", JOptionPane.WARNING_MESSAGE);
            return;
        }

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
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        int managerId = Session.currentUser.getEmployeeId();
        int success = 0;
        StringBuilder errors = new StringBuilder();
        for (int row : rows) {
            int receiptId = (Integer) model.getValueAt(row, 0);
            try {
                action.approve(receiptId, managerId);
                success++;
            } catch (SQLException ex) {
                errors.append("\n- Phieu ").append(receiptId).append(": ")
                    .append(approveError(ex));
            }
        }

        String message = "Da duyet " + success + "/" + rows.length + " " + type + ".";
        if (errors.length() > 0) {
            JOptionPane.showMessageDialog(this, message + "\nKhong duyet duoc:" + errors,
                "Ket qua duyet", JOptionPane.WARNING_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, message);
        }
        loadData();
    }
    // Doi loi vuot ton kho sang cau de hieu; cac loi khac dung ErrorHandler 
    private static String approveError(SQLException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        if (msg.contains("CK_PRODUCT_CURRENTSTOCK")) {
            return "Khong du hang trong kho de xuat (ton kho khong duoc am).";
        }
        return ErrorHandler.translateSqlException(ex);
    }
}