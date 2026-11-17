package view;

import model.WarehouseStaff;
import util.Session;
import javax.swing.*;
import java.awt.*;

public class WarehouseFrame extends JFrame {
    public WarehouseFrame() {
        setTitle("Hệ Thống Kho - Màn Hình Thủ Kho (WAREHOUSE STAFF)");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        WarehouseStaff whs = (WarehouseStaff) Session.currentUser;
        JLabel lblInfo = new JLabel(
                "Vai trò: THỦ KHO (WAREHOUSE STAFF) - " + whs.getFullName() +
                " | Khu vực: " + whs.getAssignedZone(),
                SwingConstants.CENTER
        );
        lblInfo.setFont(new Font("SansSerif", Font.BOLD, 18));
        add(lblInfo, BorderLayout.CENTER);
    }
}