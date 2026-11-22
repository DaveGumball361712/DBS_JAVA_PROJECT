package view;

import dao.AccountDAO;
import dao.EmployeeDAO;
import model.Cashier;
import model.Employee;
import model.Manager;
import model.WarehouseStaff;
import util.Session;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class LoginFrame extends JFrame {
    private final JTextField txtUsername = new JTextField(20);
    private final JPasswordField txtPassword = new JPasswordField(20);
    private final JButton btnLogin = new JButton("Đăng nhập");

    private final AccountDAO accountDAO = new AccountDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    public LoginFrame() {
        setTitle("Đăng Nhập Hệ Thống Quản Lý Kho & Bán Hàng");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 240);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Tên đăng nhập:"), gbc);
        gbc.gridx = 1;
        panel.add(txtUsername, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Mật khẩu:"), gbc);
        gbc.gridx = 1;
        panel.add(txtPassword, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(btnLogin, gbc);

        add(panel);

        // Bấm nút Đăng nhập hoặc nhấn phím Enter ở ô mật khẩu
        btnLogin.addActionListener(e -> handleLogin());
        getRootPane().setDefaultButton(btnLogin);
    }

    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            boolean valid = accountDAO.checkLogin(username, password);
            if (!valid) {
                JOptionPane.showMessageDialog(this,
                        "Sai tên đăng nhập hoặc mật khẩu!",
                        "Lỗi đăng nhập", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Employee emp = employeeDAO.findByUsername(username);
            if (emp == null) {
                JOptionPane.showMessageDialog(this,
                        "Tài khoản chưa được liên kết với hồ sơ nhân viên!",
                        "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Lưu phiên đăng nhập
            Session.login(emp);

            // Rẽ nhánh giao diện bằng instanceof theo đúng vai trò R4
            if (emp instanceof Manager) {
                new ManagerFrame().setVisible(true);
            } else if (emp instanceof Cashier) {
                new CashierFrame().setVisible(true);
            } else if (emp instanceof WarehouseStaff) {
                new WarehouseFrame().setVisible(true);
            }

            // Đóng màn hình đăng nhập
            this.dispose();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi kết nối cơ sở dữ liệu: " + ex.getMessage(),
                    "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
        }
    }
}