package view;

import util.Session;

import javax.swing.*;
import java.awt.*;

// Frame chinh cua thu kho gom header (ten + dang xuat) va 5 tab nghiep vu
public class WarehouseFrame extends JFrame {

    private final ImportPanel importPanel;
    private final ExportPanel exportPanel;
    private final StockCheckPanel stockCheckPanel;
    private final ImportHistoryPanel importHistoryPanel;
    private final LowStockPanel lowStockPanel;

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

        importPanel = new ImportPanel();
        exportPanel = new ExportPanel();
        stockCheckPanel = new StockCheckPanel();
        importHistoryPanel = new ImportHistoryPanel();
        lowStockPanel = new LowStockPanel();

        tabbedPane.addTab("Phiếu Nhập", importPanel);
        tabbedPane.addTab("Phiếu Xuất", exportPanel);
        tabbedPane.addTab("Kiểm Kê", stockCheckPanel);
        tabbedPane.addTab("Lịch sử nhập", importHistoryPanel);
        tabbedPane.addTab("Sắp hết hàng", lowStockPanel);

        // Tu dong tai lai du lieu moi nhat khi chuyen tab
        tabbedPane.addChangeListener(e -> {
            Component selected = tabbedPane.getSelectedComponent();
            if (selected == importHistoryPanel) {
                importHistoryPanel.reload();
            } else if (selected == lowStockPanel) {
                lowStockPanel.loadLowStock();
            } else if (selected == importPanel) {
                importPanel.refreshSuppliers();
            } else if (selected == exportPanel) {
                exportPanel.refreshSuppliers();
            }
        });

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
            new WarehouseFrame("Thủ kho").setVisible(true);
        });
    }
}
