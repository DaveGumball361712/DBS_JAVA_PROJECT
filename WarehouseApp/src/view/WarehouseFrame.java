package view;

import javax.swing.*;
import java.awt.*;

// Frame chinh cua thu kho gom 3 tab: Nhap, Xuat, Kiem ke
public class WarehouseFrame extends JFrame {

    public WarehouseFrame() {
        setTitle("Quản lý Kho - Warehouse Management");
        setSize(950, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(tabbedPane.getFont().deriveFont(14f));

        tabbedPane.addTab("📥 Phiếu Nhập", new ImportPanel());
        tabbedPane.addTab("📤 Phiếu Xuất", new ExportPanel());
        tabbedPane.addTab("📋 Kiểm Kê", new StockCheckPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new WarehouseFrame().setVisible(true);
        });
    }
}
