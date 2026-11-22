package view;

import util.Session;

import javax.swing.*;
import java.awt.*;

// Frame chinh cua thu kho gom header (ten + dang xuat) va 3 tab: Nhap, Xuat, Kiem ke
public class WarehouseFrame extends JFrame {

    public WarehouseFrame() {
        this(Session.currentUser != null ? Session.currentUser.getFullName() : "Thủ kho");
    }

    public WarehouseFrame(String staffName) {
        setTitle("Quản lý Kho & Bán hàng - Thủ kho");
        setSize(950, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(createHeader(staffName), BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(tabbedPane.getFont().deriveFont(14f));

        tabbedPane.addTab("Phiếu Nhập", new ImportPanel());
        tabbedPane.addTab("Phiếu Xuất", new ExportPanel());
        tabbedPane.addTab("Kiểm Kê", new StockCheckPanel());
        tabbedPane.addTab("Lịch sử nhập", new ImportHistoryPanel());
        tabbedPane.addTab("Sắp hết hàng", new LowStockPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createHeader(String staffName) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel lblHello = new JLabel("Xin chào " + staffName);
        lblHello.setFont(lblHello.getFont().deriveFont(Font.BOLD, 16f));
        header.add(lblHello, BorderLayout.WEST);

        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.addActionListener(e -> {
            Session.logout();
            dispose();
            new LoginFrame().setVisible(true);
        });
        header.add(btnLogout, BorderLayout.EAST);

        return header;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new WarehouseFrame("Thủ kho (test)").setVisible(true);
        });
    }
}
