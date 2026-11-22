package view;

import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import dao.SupplierDAO;
import model.Supplier;
import util.ErrorHandler;

public class SupplierPanel extends JPanel {

    private final SupplierDAO dao = new SupplierDAO();
    private List<Supplier> suppliers = new ArrayList<>();   // danh sach dang hien tren bang

    private final DefaultTableModel model =
        new DefaultTableModel(new String[] {"Ma", "Ten", "Email", "Dia chi", "Ma so thue", "So dien thoai"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    private final JTable table = new JTable(model);

    private final JTextField searchField = new JTextField(20);
    private final JTextField nameField = new JTextField(20);
    private final JTextField emailField = new JTextField(20);
    private final JTextField addressField = new JTextField(20);
    private final JTextField taxField = new JTextField(20);
    private final JTextField phoneField = new JTextField(20);

    public SupplierPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(createSearchBar(), BorderLayout.NORTH);

        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fillForm();
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);

        add(createForm(), BorderLayout.EAST);

        loadData(dao::findAll);
    }


    private JPanel createSearchBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton search = new JButton("Tim");
        JButton all = new JButton("Tat ca");
        search.addActionListener(e -> loadData(() -> dao.search(searchField.getText())));
        searchField.addActionListener(e -> loadData(() -> dao.search(searchField.getText())));  // bam Enter
        all.addActionListener(e -> {
            searchField.setText("");
            loadData(dao::findAll);
        });
        bar.add(new JLabel("Ten / ma so thue:"));
        bar.add(searchField);
        bar.add(search);
        bar.add(all);
        return bar;
    }

    private JPanel createForm() {
        JPanel fields = new JPanel(new GridLayout(0, 1, 0, 4));
        fields.add(new JLabel("Ten nha cung cap *"));
        fields.add(nameField);
        fields.add(new JLabel("Email"));
        fields.add(emailField);
        fields.add(new JLabel("Dia chi"));
        fields.add(addressField);
        fields.add(new JLabel("Ma so thue *"));
        fields.add(taxField);
        fields.add(new JLabel("So dien thoai (cach nhau dau phay)"));
        fields.add(phoneField);

        JButton add = new JButton("Them");
        JButton update = new JButton("Sua");
        JButton delete = new JButton("Xoa");
        JButton clear = new JButton("Nhap moi");
        add.addActionListener(e -> addSupplier());
        update.addActionListener(e -> updateSupplier());
        delete.addActionListener(e -> deleteSupplier());
        clear.addActionListener(e -> clearForm());

        JPanel buttons = new JPanel(new GridLayout(2, 2, 6, 6));
        buttons.add(add);
        buttons.add(update);
        buttons.add(delete);
        buttons.add(clear);

        JPanel form = new JPanel(new BorderLayout(0, 10));
        form.setBorder(BorderFactory.createTitledBorder("Thong tin nha cung cap"));
        form.add(fields, BorderLayout.NORTH);
        form.add(buttons, BorderLayout.SOUTH);
        form.setPreferredSize(new Dimension(280, 0));
        return form;
    }


    // Cach lay danh sach (tat ca hoac tim kiem), co the nem SQLException
    private interface Loader {
        List<Supplier> load() throws SQLException;
    }

    private void loadData(Loader loader) {
        try {
            suppliers = loader.load();
            model.setRowCount(0);
            for (Supplier s : suppliers) {
                model.addRow(new Object[] {
                    s.getSupplierId(), s.getName(), s.getEmail(), s.getAddress(),
                    s.getTaxCode(), String.join(", ", s.getPhoneNumbers())
                });
            }
            clearForm();
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);
        }
    }

    private Supplier selectedSupplier() {
        int row = table.getSelectedRow();
        return row >= 0 ? suppliers.get(row) : null;
    }

    private void fillForm() {
        Supplier s = selectedSupplier();
        if (s == null) {
            return;
        }
        nameField.setText(s.getName());
        emailField.setText(s.getEmail());
        addressField.setText(s.getAddress());
        taxField.setText(s.getTaxCode());
        phoneField.setText(String.join(", ", s.getPhoneNumbers()));
    }

    private void clearForm() {
        table.clearSelection();
        nameField.setText("");
        emailField.setText("");
        addressField.setText("");
        taxField.setText("");
        phoneField.setText("");
    }

    // Doc form thanh doi tuong Supplier
    private Supplier readForm(int id) {
        String name = nameField.getText().trim();
        String tax = taxField.getText().trim();
        if (name.isEmpty() || tax.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ten va ma so thue khong duoc de trong.",
                "Thieu thong tin", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        Supplier s = new Supplier(id, name, emptyToNull(emailField.getText()),
            emptyToNull(addressField.getText()), tax);
        for (String phone : phoneField.getText().split(",")) {
            if (!phone.trim().isEmpty()) {
                s.getPhoneNumbers().add(phone.trim());
            }
        }
        return s;
    }

    private static String emptyToNull(String text) {
        return text.trim().isEmpty() ? null : text.trim();
    }

    // Them / Sua / Xoa

    private void addSupplier() {
        Supplier s = readForm(0);
        if (s == null) {
            return;
        }
        try {
            int id = dao.insert(s);
            JOptionPane.showMessageDialog(this, "Da them nha cung cap ma " + id);
            loadData(dao::findAll);
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);
        }
    }

    private void updateSupplier() {
        Supplier selected = selectedSupplier();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Hay chon nha cung cap can sua.");
            return;
        }
        Supplier s = readForm(selected.getSupplierId());
        if (s == null) {
            return;
        }
        try {
            dao.update(s);
            JOptionPane.showMessageDialog(this, "Da cap nhat nha cung cap ma " + s.getSupplierId());
            loadData(dao::findAll);
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);
        }
    }

    private void deleteSupplier() {
        Supplier selected = selectedSupplier();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Hay chon nha cung cap can xoa.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
            "Xoa nha cung cap \"" + selected.getName() + "\"?", "Xac nhan", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            dao.delete(selected.getSupplierId());
            loadData(dao::findAll);
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);   
        }
    }
}