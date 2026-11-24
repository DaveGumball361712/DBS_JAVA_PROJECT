package view;

import dao.CustomerDAO;
import dao.ProductDAO;
import dao.SalesOrderDAO;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.Customer;
import model.OrderLine;
import model.Payment;
import model.PaymentMethod;
import model.Product;
import model.ProductStatus;
import model.SalesOrder;
import util.ErrorHandler;
import util.Session;

public class CashierFrame extends JFrame {

    JTextField txtBarcode;
    JTextField txtDiscount;
    JTextField txtPhone;
    JLabel lblCustomer;
    JLabel lblTotal;
    JTable table;
    DefaultTableModel model;

    ProductDAO productDAO = new ProductDAO();
    CustomerDAO customerDAO = new CustomerDAO();
    SalesOrderDAO orderDAO = new SalesOrderDAO();
    DecimalFormat money = new DecimalFormat("#,###");

    Customer customer = null;

    public CashierFrame() {
        setTitle("Bán hàng");
        setSize(950, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // header: tên thu ngân + đăng xuất
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JLabel lblHello = new JLabel("Xin chào " + Session.currentUser.getFullName());
        lblHello.setFont(lblHello.getFont().deriveFont(Font.BOLD, 16f));
        JButton btnLogout = new JButton("Đăng xuất");
        header.add(lblHello, BorderLayout.WEST);
        header.add(btnLogout, BorderLayout.EAST);

        // dòng nhập mã vạch
        JPanel scan = new JPanel();
        txtBarcode = new JTextField(20);
        JButton btnAdd = new JButton("Thêm");
        scan.add(new JLabel("Mã vạch:"));
        scan.add(txtBarcode);
        scan.add(btnAdd);

        // dòng khách hàng
        JPanel cus = new JPanel();
        txtPhone = new JTextField(12);
        JButton btnFind = new JButton("Tìm khách");
        lblCustomer = new JLabel("Khách vãng lai");
        cus.add(new JLabel("SĐT khách:"));
        cus.add(txtPhone);
        cus.add(btnFind);
        cus.add(lblCustomer);

        JPanel top = new JPanel(new GridLayout(3, 1));
        top.add(header);
        top.add(scan);
        top.add(cus);
        add(top, BorderLayout.NORTH);

        String[] columns = {"Mã SP", "Mã vạch", "Tên sản phẩm", "Đơn giá", "SL", "Thành tiền"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        JButton btnRemove = new JButton("Xóa dòng");
        txtDiscount = new JTextField("0", 10);
        lblTotal = new JLabel("Tổng tiền: 0");
        lblTotal.setFont(new Font("Arial", Font.BOLD, 18));
        JButton btnPay = new JButton("Thanh toán");
        bottom.add(btnRemove);
        bottom.add(new JLabel("Giảm giá:"));
        bottom.add(txtDiscount);
        bottom.add(lblTotal);
        bottom.add(btnPay);
        add(bottom, BorderLayout.SOUTH);

        btnAdd.addActionListener(e -> addToCart());
        txtBarcode.addActionListener(e -> addToCart());
        btnFind.addActionListener(e -> findCustomer());
        txtPhone.addActionListener(e -> findCustomer());
        btnRemove.addActionListener(e -> removeRow());
        txtDiscount.addActionListener(e -> updateTotal());
        btnPay.addActionListener(e -> pay());
        btnLogout.addActionListener(e -> {
            Session.logout();
            dispose();
            new LoginFrame().setVisible(true);
        });
    }

    void addToCart() {
        String code = txtBarcode.getText().trim();
        if (code.isEmpty()) {
            return;
        }

        Product p;
        try {
            p = productDAO.findByBarcode(code);
        } catch (Exception ex) {
            ErrorHandler.showError(this, ex);
            return;
        }

        if (p == null) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy mã vạch: " + code);
            return;
        }
        if (p.getStatus() != ProductStatus.ON_SALE) {
            JOptionPane.showMessageDialog(this, "Sản phẩm này đã ngưng bán");
            return;
        }

        for (int i = 0; i < model.getRowCount(); i++) {
            if ((int) model.getValueAt(i, 0) == p.getProductId()) {
                int qty = (int) model.getValueAt(i, 4) + 1;
                if (qty > p.getCurrentStock()) {
                    JOptionPane.showMessageDialog(this, "Chỉ còn " + p.getCurrentStock() + " sản phẩm trong kho");
                    return;
                }
                model.setValueAt(qty, i, 4);
                model.setValueAt(p.getCurrentPrice().multiply(BigDecimal.valueOf(qty)), i, 5);
                txtBarcode.setText("");
                updateTotal();
                return;
            }
        }

        if (p.getCurrentStock() < 1) {
            JOptionPane.showMessageDialog(this, "Sản phẩm đã hết hàng");
            return;
        }

        model.addRow(new Object[]{p.getProductId(), p.getBarcode(), p.getName(),
                p.getCurrentPrice(), 1, p.getCurrentPrice()});
        txtBarcode.setText("");
        updateTotal();
    }

    void findCustomer() {
        String phone = txtPhone.getText().trim();
        if (phone.isEmpty()) {
            customer = null;
            lblCustomer.setText("Khách vãng lai");
            return;
        }

        customer = null;
        lblCustomer.setText("Khách vãng lai");

        if (!phone.matches("[0-9]{1,12}")) {
            JOptionPane.showMessageDialog(this, "Số điện thoại chỉ gồm chữ số, tối đa 12 số");
            return;
        }

        try {
            customer = customerDAO.findByPhone(phone);
            if (customer == null) {
                int choice = JOptionPane.showConfirmDialog(this,
                        "Chưa có khách với SĐT này. Đăng ký mới?", "Khách hàng", JOptionPane.YES_NO_OPTION);
                if (choice != JOptionPane.YES_OPTION) {
                    lblCustomer.setText("Khách vãng lai");
                    return;
                }
                String name = JOptionPane.showInputDialog(this, "Tên khách hàng:");
                if (name == null || name.trim().isEmpty()) {
                    return;
                }
                String email = JOptionPane.showInputDialog(this, "Email khách hàng (ví dụ ten@gmail.com):");
                if (email == null) {
                    return;
                }
                email = email.trim();
                if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
                    JOptionPane.showMessageDialog(this, "Email không đúng định dạng, ví dụ ten@gmail.com");
                    return;
                }
                Customer c = new Customer(0, name.trim(), email, 0, phone, LocalDate.now());
                customerDAO.insert(c);
                customer = c;
            }
            lblCustomer.setText("Khách: " + customer.getName());
        } catch (Exception ex) {
            customer = null;
            lblCustomer.setText("Khách vãng lai");
            ErrorHandler.showError(this, ex);
        }
    }

    void removeRow() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một dòng để xóa");
            return;
        }
        model.removeRow(row);
        updateTotal();
    }

    BigDecimal getSubtotal() {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < model.getRowCount(); i++) {
            sum = sum.add((BigDecimal) model.getValueAt(i, 5));
        }
        return sum;
    }

    BigDecimal getDiscount() {
        try {
            BigDecimal d = new BigDecimal(txtDiscount.getText().trim());
            if (d.signum() < 0) {
                return null;
            }
            return d;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    void updateTotal() {
        BigDecimal discount = getDiscount();
        if (discount == null) {
            JOptionPane.showMessageDialog(this, "Giảm giá phải là số không âm");
            return;
        }
        BigDecimal total = getSubtotal().subtract(discount);
        if (total.signum() < 0) {
            total = BigDecimal.ZERO;
        }
        lblTotal.setText("Tổng tiền: " + money.format(total));
    }

    void pay() {
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Giỏ hàng đang trống");
            return;
        }

        BigDecimal discount = getDiscount();
        if (discount == null) {
            JOptionPane.showMessageDialog(this, "Giảm giá phải là số không âm");
            return;
        }
        BigDecimal total = getSubtotal().subtract(discount);
        if (total.signum() <= 0) {
            JOptionPane.showMessageDialog(this, "Giảm giá không được lớn hơn tiền hàng");
            return;
        }

        // nhập các lần thanh toán cho đến khi đủ tiền
        List<Payment> payments = new ArrayList<>();
        BigDecimal remaining = total;
        while (remaining.signum() > 0) {
            PaymentMethod method = (PaymentMethod) JOptionPane.showInputDialog(this,
                    "Còn thiếu: " + money.format(remaining) + "\nChọn hình thức thanh toán:",
                    "Thanh toán", JOptionPane.QUESTION_MESSAGE, null,
                    PaymentMethod.values(), PaymentMethod.IN_CASH);
            if (method == null) {
                return;
            }

            String text = JOptionPane.showInputDialog(this,
                    "Số tiền trả bằng " + method.getDisplayName() + ":", remaining.toPlainString());
            if (text == null) {
                return;
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(text.trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Số tiền phải là số");
                continue;
            }
            if (amount.signum() <= 0 || amount.compareTo(remaining) > 0) {
                JOptionPane.showMessageDialog(this, "Số tiền phải lớn hơn 0 và không vượt quá số còn thiếu");
                continue;
            }

            Payment pm = new Payment();
            pm.setMethod(method);
            pm.setAmount(amount);
            payments.add(pm);
            remaining = remaining.subtract(amount);
        }

        SalesOrder order = new SalesOrder();
        order.setEmployeeId(Session.currentUser.getEmployeeId());
        order.setCustomerId(customer == null ? null : customer.getCustomerId());
        order.setDiscount(discount);
        for (int i = 0; i < model.getRowCount(); i++) {
            OrderLine line = new OrderLine();
            line.setProductId((int) model.getValueAt(i, 0));
            line.setUnitPrice((BigDecimal) model.getValueAt(i, 3));
            line.setQuantity((int) model.getValueAt(i, 4));
            order.getOrderLines().add(line);
        }
        order.setPayments(payments);

        try {
            int id = orderDAO.save(order);
            JOptionPane.showMessageDialog(this, "Đã lưu đơn hàng số " + id + "\nTổng tiền: " + money.format(total));
            model.setRowCount(0);
            txtDiscount.setText("0");
            txtPhone.setText("");
            customer = null;
            lblCustomer.setText("Khách vãng lai");
            updateTotal();
        } catch (Exception ex) {
            ErrorHandler.showError(this, ex);
        }
    }
}