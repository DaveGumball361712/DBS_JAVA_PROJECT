package view;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;
import util.Session;

public class ManagerFrame extends JFrame {

    private final JTabbedPane tabs=new JTabbedPane();
    // Dung khi dang nhap: lay ten nguoi dung tu Session
    public ManagerFrame() {
        this(Session.currentUser != null ? Session.currentUser.getFullName() : "Quan ly");
    }
    public ManagerFrame(String managerName){
        setTitle("Quan ly kho & Ban hang - Quan ly");
        setSize(1000,650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(createHeader(managerName), BorderLayout.NORTH);
        
        tabs.addTab("Nhan vien", createPlaceholder("Quan ly nhan vien"));
        tabs.addTab("San pham", createPlaceholder("Quan ly san pham"));
        tabs.addTab("Loai hang", createPlaceholder("Cay loai hang (JTree)"));
        tabs.addTab("Nha cung cap", new SupplierPanel());
        tabs.addTab("Duyet phieu", new ApprovalPanel());    
        tabs.addTab("Bao cao", new ReportPanel());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel createHeader(String managerName){
        JPanel header=new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(8,12,8,12));

        JLabel hello = new JLabel("Xin chào " + managerName);
        hello.setFont(hello.getFont().deriveFont(Font.BOLD, 16f));
        header.add(hello,BorderLayout.WEST);

        JButton logout = new JButton("Đăng xuất");
        logout.addActionListener(e -> {
            Session.logout();
            dispose();
            new LoginFrame().setVisible(true);
        });
        header.add(logout, BorderLayout.EAST);
        return header;
    }

    private JPanel createPlaceholder(String text){
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(text,SwingConstants.CENTER), BorderLayout.CENTER);
        return panel;
    }

    public void showTab(int index) {
        tabs.setSelectedIndex(index);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new ManagerFrame("Phuc (test)").setVisible(true));
    }
}
