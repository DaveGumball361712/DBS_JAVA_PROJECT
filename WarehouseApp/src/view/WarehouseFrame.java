package view;

import javax.swing.*;
import java.awt.*;

/**
 * Cửa sổ chính của Thủ kho (Warehouse Staff).
 * Chứa JTabbedPane với 3 tab nghiệp vụ:
 *   1. Phiếu Nhập kho (ImportPanel)
 *   2. Phiếu Xuất kho (ExportPanel)
 *   3. Kiểm Kê (StockCheckPanel)
 *
 * Giai đoạn 2: Đức sẽ viết LoginFrame → xác định vai trò → mở frame này.
 */
public class WarehouseFrame extends JFrame {

    public WarehouseFrame() {
        setTitle("Quản lý Kho - Warehouse Management");
        setSize(950, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Tạo JTabbedPane chứa 3 tab
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(tabbedPane.getFont().deriveFont(14f));

        tabbedPane.addTab("📥 Phiếu Nhập", new ImportPanel());
        tabbedPane.addTab("📤 Phiếu Xuất", new ExportPanel());
        tabbedPane.addTab("📋 Kiểm Kê", new StockCheckPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        // Chạy UI trên Event Dispatch Thread (quy tắc bắt buộc của Swing)
        SwingUtilities.invokeLater(() -> {
            new WarehouseFrame().setVisible(true);
        });
    }
}
