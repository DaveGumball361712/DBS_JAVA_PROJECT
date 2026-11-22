package view;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import dao.EmployeeDAO;
import model.Cashier;
import model.Employee;
import model.Manager;
import model.WarehouseStaff;
import util.ErrorHandler;

public class EmployeePanel extends JPanel {
    private static final String[] ROLES = {"Quan ly", "Thu ngan", "Nhan vien kho"};

    private final EmployeeDAO dao = new EmployeeDAO();
    private List<Employee> allEmployees = new ArrayList<>();
    private List<Employee> shown = new ArrayList<>();        // danh sach dang hien tren bang
    private List<Manager> managers = new ArrayList<>();      // cho o "Quan ly phu trach"

    private final JComboBox<String> filterBox =
        new JComboBox<>(new String[] {"Tat ca", "Quan ly", "Thu ngan", "Nhan vien kho"});

    private final DefaultTableModel model = new DefaultTableModel(
        new String[] {"Ma", "Username", "Ho ten", "Vai tro", "Tuoi", "Ngay vao lam", "So dien thoai", "Chi tiet"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    private final JTextField usernameField = new JTextField(15);
    private final JPasswordField passwordField = new JPasswordField(15);
    private final JTextField lastNameField = new JTextField(15);
    private final JTextField firstNameField = new JTextField(15);
    private final JTextField dobField = new JTextField(15);
    private final JTextField startField = new JTextField(15);
    private final JTextField addressField = new JTextField(15);
    private final JTextField phoneField = new JTextField(15);
    private final JComboBox<String> roleBox = new JComboBox<>(ROLES);
    private final JLabel extraLabel = new JLabel();
    private final JTextField extraField = new JTextField(15);
    private final JComboBox<String> managerBox = new JComboBox<>();

    public EmployeePanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton reload = new JButton("Tai lai");
        filterBox.addActionListener(e -> showFiltered());
        reload.addActionListener(e -> loadData());
        top.add(new JLabel("Vai tro:"));
        top.add(filterBox);
        top.add(reload);
        add(top, BorderLayout.NORTH);

        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fillForm();
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(createForm(), BorderLayout.EAST);

        roleBox.addActionListener(e -> updateRoleFields());
        updateRoleFields();
        loadData();
    }

    // ---------- Giao dien ----------

    private JPanel createForm() {
        JPanel fields = new JPanel(new GridLayout(0, 2, 6, 4));
        addRow(fields, "Username *", usernameField);
        addRow(fields, "Mat khau (khi them) *", passwordField);
        addRow(fields, "Ho *", lastNameField);
        addRow(fields, "Ten *", firstNameField);
        addRow(fields, "Ngay sinh * (yyyy-MM-dd)", dobField);
        addRow(fields, "Ngay vao lam *", startField);
        addRow(fields, "Dia chi *", addressField);
        addRow(fields, "So dien thoai", phoneField);
        addRow(fields, "Vai tro", roleBox);
        fields.add(extraLabel);
        fields.add(extraField);
        addRow(fields, "Quan ly phu trach", managerBox);

        JButton add = new JButton("Them");
        JButton update = new JButton("Sua");
        JButton delete = new JButton("Xoa");
        JButton clear = new JButton("Nhap moi");
        add.addActionListener(e -> addEmployee());
        update.addActionListener(e -> updateEmployee());
        delete.addActionListener(e -> deleteEmployee());
        clear.addActionListener(e -> clearForm());

        JPanel buttons = new JPanel(new GridLayout(2, 2, 6, 6));
        buttons.add(add);
        buttons.add(update);
        buttons.add(delete);
        buttons.add(clear);

        JPanel form = new JPanel(new BorderLayout(0, 10));
        form.setBorder(BorderFactory.createTitledBorder("Thong tin nhan vien"));
        form.add(fields, BorderLayout.NORTH);
        form.add(buttons, BorderLayout.SOUTH);
        form.setPreferredSize(new Dimension(400, 0));
        return form;
    }

    private static void addRow(JPanel panel, String label, Component input) {
        panel.add(new JLabel(label));
        panel.add(input);
    }

    // Doi nhan va o nhap rieng theo vai tro dang chon
    private void updateRoleFields() {
        int role = roleBox.getSelectedIndex();
        if (role == 0) {
            extraLabel.setText("Thuong (VND) *");
        } else if (role == 1) {
            extraLabel.setText("So quay *");
        } else {
            extraLabel.setText("Khu vuc kho *");
        }
        managerBox.setEnabled(role != 0);     // quan ly khong co nguoi phu trach
    }

    // ---------- Du lieu ----------

    private void loadData() {
        try {
            allEmployees = dao.findAll();
            managers = new ArrayList<>();
            managerBox.removeAllItems();
            for (Employee e : allEmployees) {
                if (e instanceof Manager m) {
                    managers.add(m);
                    managerBox.addItem(m.getEmployeeId() + " - " + m.getFullName());
                }
            }
            showFiltered();
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);
        }
    }

    private void showFiltered() {
        String[] codes = {null, "MANAGER", "CASHIER", "WAREHOUSE_STAFF"};
        String wanted = codes[filterBox.getSelectedIndex()];

        shown = new ArrayList<>();
        model.setRowCount(0);
        for (Employee e : allEmployees) {
            if (wanted == null || wanted.equals(e.getRole())) {
                shown.add(e);
                model.addRow(new Object[] {
                    e.getEmployeeId(), e.getUsername(), e.getFullName(), e.getRole(), e.getAge(),
                    e.getStartDate(), String.join(", ", e.getPhoneNumbers()), roleDetail(e)
                });
            }
        }
        clearForm();
    }

    // Thong tin rieng cua tung loai nhan vien
    private static String roleDetail(Employee e) {
        if (e instanceof Manager m) {
            return "Thuong: " + String.format("%,.0f", m.getBonus());
        } else if (e instanceof Cashier c) {
            return "Quay " + c.getAssignedCounter() + " | QL #" + c.getManagerId();
        } else if (e instanceof WarehouseStaff w) {
            return w.getAssignedZone() + " | QL #" + w.getManagerId();
        }
        return "";
    }

    private Employee selectedEmployee() {
        int row = table.getSelectedRow();
        return row >= 0 ? shown.get(row) : null;
    }

    private void fillForm() {
        Employee e = selectedEmployee();
        if (e == null) {
            return;
        }
        usernameField.setText(e.getUsername());
        passwordField.setText("");
        lastNameField.setText(e.getLastName());
        firstNameField.setText(e.getFirstName());
        dobField.setText(e.getDateOfBirth().toString());
        startField.setText(e.getStartDate().toString());
        addressField.setText(e.getAddress());
        phoneField.setText(String.join(", ", e.getPhoneNumbers()));

        int managerId = 0;
        if (e instanceof Manager m) {
            roleBox.setSelectedIndex(0);
            extraField.setText(m.getBonus().toPlainString());
        } else if (e instanceof Cashier c) {
            roleBox.setSelectedIndex(1);
            extraField.setText(String.valueOf(c.getAssignedCounter()));
            managerId = c.getManagerId();
        } else if (e instanceof WarehouseStaff w) {
            roleBox.setSelectedIndex(2);
            extraField.setText(w.getAssignedZone());
            managerId = w.getManagerId();
        }
        for (int i = 0; i < managers.size(); i++) {
            if (managers.get(i).getEmployeeId() == managerId) {
                managerBox.setSelectedIndex(i);
            }
        }
    }

    private void clearForm() {
        table.clearSelection();
        for (JTextField f : new JTextField[] {usernameField, lastNameField, firstNameField,
                dobField, startField, addressField, phoneField, extraField}) {
            f.setText("");
        }
        passwordField.setText("");
        startField.setText(LocalDate.now().toString());
    }

    // Doc form thanh dung loai doi tuong (Manager / Cashier / WarehouseStaff).
    // Tra ve null neu du lieu khong hop le.
    private Employee readForm(int id) {
        String username = usernameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String address = addressField.getText().trim();
        String extra = extraField.getText().trim();
        if (username.isEmpty() || lastName.isEmpty() || firstName.isEmpty() || address.isEmpty() || extra.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Hay dien du cac o co dau *.",
                "Thieu thong tin", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        LocalDate dob;
        LocalDate start;
        try {
            dob = LocalDate.parse(dobField.getText().trim());
            start = LocalDate.parse(startField.getText().trim());
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Ngay phai co dang yyyy-MM-dd, vi du 2000-05-20.",
                "Sai ngay", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        int role = roleBox.getSelectedIndex();
        if (role != 0 && managers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chua co quan ly nao de phu trach.");
            return null;
        }
        int managerId = role == 0 ? 0 : managers.get(managerBox.getSelectedIndex()).getEmployeeId();

        Employee e;
        try {
            if (role == 0) {
                e = new Manager(id, username, firstName, lastName, start, dob, address, new BigDecimal(extra));
            } else if (role == 1) {
                e = new Cashier(id, username, firstName, lastName, start, dob, address,
                    managerId, Integer.parseInt(extra));
            } else {
                e = new WarehouseStaff(id, username, firstName, lastName, start, dob, address,
                    managerId, extra);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, extraLabel.getText() + " phai la so.",
                "Sai so", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        for (String phone : phoneField.getText().split(",")) {
            if (!phone.trim().isEmpty()) {
                e.addPhoneNumber(phone.trim());
            }
        }
        return e;
    }

    // ---------- Them / Sua / Xoa ----------

    private void addEmployee() {
        String password = new String(passwordField.getPassword());
        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nhan vien moi phai co mat khau.");
            return;
        }
        Employee e = readForm(0);
        if (e == null) {
            return;
        }
        try {
            int id = dao.insert(e, password);     // tao ACCOUNT + EMPLOYEE + bang vai tro + SDT
            JOptionPane.showMessageDialog(this, "Da them nhan vien ma " + id);
            loadData();
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);
        }
    }

    // Sua thong tin chung (ho ten, ngay, dia chi). Vai tro va username khong doi.
    private void updateEmployee() {
        Employee selected = selectedEmployee();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Hay chon nhan vien can sua.");
            return;
        }
        Employee e = readForm(selected.getEmployeeId());
        if (e == null) {
            return;
        }
        try {
            dao.update(e);
            JOptionPane.showMessageDialog(this, "Da cap nhat nhan vien ma " + e.getEmployeeId());
            loadData();
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);
        }
    }

    private void deleteEmployee() {
        Employee selected = selectedEmployee();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Hay chon nhan vien can xoa.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
            "Xoa nhan vien \"" + selected.getFullName() + "\"?", "Xac nhan", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            dao.delete(selected.getEmployeeId());
            loadData();
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);   // ma 547: nhan vien da lap phieu / don hang
        }
    }
}
