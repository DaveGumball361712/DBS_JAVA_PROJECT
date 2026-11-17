package view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.HashMap;

public class CashierFrame extends JFrame {

    JTextField txtBarcode;
    JTextField txtDiscount;
    JLabel lblTotal;
    JTable table;
    DefaultTableModel model;

    HashMap<String, String> names = new HashMap<>();
    HashMap<String, Double> prices = new HashMap<>();
    DecimalFormat money = new DecimalFormat("#,###");

    public CashierFrame() {
        setTitle("Bán hàng");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // dữ liệu giả
        names.put("893456700001", "Thùng mì Hảo Hảo tôm chua cay 30 gói");
        prices.put("893456700001", 33500.0);
        names.put("893456700002", "Thùng mì Omachi xốt bò hầm 30 gói");
        prices.put("893456700002", 42000.0);
        names.put("893456700003", "Mì ly Modern lẩu thái tôm 65g");
        prices.put("893456700003", 50500.0);
        names.put("893456700004", "Phở bò Đệ Nhất gói 65g");
        prices.put("893456700004", 59000.0);

        // phía trên
        JPanel top = new JPanel();
        txtBarcode = new JTextField(20);
        JButton btnAdd = new JButton("Thêm");
        top.add(new JLabel("Mã vạch:"));
        top.add(txtBarcode);
        top.add(btnAdd);
        add(top, BorderLayout.NORTH);

        // bảng giỏ hàng
        String[] columns = {"Mã vạch", "Tên sản phẩm", "Đơn giá", "SL", "Thành tiền"};
        model = new DefaultTableModel(columns, 0);
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // phía dưới
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
        btnRemove.addActionListener(e -> removeRow());
        txtDiscount.addActionListener(e -> updateTotal());
        btnPay.addActionListener(e -> pay());
    }

    void addToCart() {
        String code = txtBarcode.getText().trim();

        if (!names.containsKey(code)) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy mã vạch: " + code);
            return;
        }

        String name = names.get(code);
        double price = prices.get(code);

        for (int i = 0; i < model.getRowCount(); i++) {
            if (model.getValueAt(i, 0).equals(code)) {
                int qty = (int) model.getValueAt(i, 3) + 1;
                model.setValueAt(qty, i, 3);
                model.setValueAt(price * qty, i, 4);
                txtBarcode.setText("");
                updateTotal();
                return;
            }
        }

        model.addRow(new Object[]{code, name, price, 1, price});
        txtBarcode.setText("");
        updateTotal();
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

    void updateTotal() {
        double sum = 0;
        for (int i = 0; i < model.getRowCount(); i++) {
            sum = sum + (double) model.getValueAt(i, 4);
        }

        double discount = 0;
        try {
            discount = Double.parseDouble(txtDiscount.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Giảm giá phải là số");
        }

        double total = sum - discount;
        if (total < 0) {
            total = 0;
        }
        lblTotal.setText("Tổng tiền: " + money.format(total));
    }

    void pay() {
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Giỏ hàng đang trống");
            return;
        }
        JOptionPane.showMessageDialog(this, "Cần thanh toán: " + lblTotal.getText());
    }

    public static void main(String[] args) {
        new CashierFrame().setVisible(true);
    }
}