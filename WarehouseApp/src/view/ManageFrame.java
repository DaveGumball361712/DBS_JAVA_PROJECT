package view;

import model.Manager;
import util.Session;
import javax.swing.*;
import java.awt.*;

public class ManagerFrame extends JFrame {
    public ManagerFrame() {
        setTitle("Hệ Thống Quản Lý Kho - Màn Hình Quản Lý (MANAGER)");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Manager mgr = (Manager) Session.currentUser;
        JLabel lblWelcome = new JLabel(
                "Xin chào Quản lý: " + mgr.getFullName() +
                " | Tuổi: " + mgr.getAge() +
                " | Thưởng: " + mgr.getBonus() + " VNĐ",
                SwingConstants.CENTER
        );
        lblWelcome.setFont(new Font("SansSerif", Font.BOLD, 18));
        add(lblWelcome, BorderLayout.CENTER);
    }
}
