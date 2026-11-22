package view;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import dao.CategoryDAO;
import dao.ProductDAO;
import model.Category;
import model.Electronics;
import model.Foods;
import model.Product;
import model.ProductStatus;
import util.ErrorHandler;
public class ProductPanel extends JPanel {
    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private List<Product> allProducts = new ArrayList<>();
    private List<Product> shown = new ArrayList<>();             // danh sach dang hien tren bang
    private final Map<Integer, String> categoryNames = new HashMap<>();

    private final JTextField searchField = new JTextField(15);
    private final JComboBox<String> typeFilter = new JComboBox<>(new String[] {"Tat ca", "Thuc pham", "Dien tu"});
    private final JCheckBox lowStockOnly = new JCheckBox("Chi hang duoi ton toi thieu");

    private final DefaultTableModel model = new DefaultTableModel(new String[] {
        "Ma", "Barcode", "Ten san pham", "Loai hang", "Nhom", "Gia ban", "Ton kho", "Ton toi thieu",
        "Don vi", "Trang thai", "Chi tiet"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JTextField barcodeField = new JTextField(15);
    private final JTextField nameField = new JTextField(15);
    private final JComboBox<Category> categoryBox = new JComboBox<>();
    private final JTextField priceField = new JTextField(15);
    private final JTextField unitField = new JTextField(15);
    private final JTextField minStockField = new JTextField(15);
    private final JComboBox<ProductStatus> statusBox = new JComboBox<>(ProductStatus.values());
    private final JComboBox<String> typeBox = new JComboBox<>(new String[] {"Thuc pham", "Dien tu"});
    private final JLabel extra1Label = new JLabel();
    private final JTextField extra1Field = new JTextField(15);
    private final JLabel extra2Label = new JLabel();
    private final JTextField extra2Field = new JTextField(15);

    public ProductPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(createFilterBar(), BorderLayout.NORTH);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fillForm();
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(createForm(), BorderLayout.EAST);

        typeBox.addActionListener(e -> updateTypeFields());
        updateTypeFields();
        loadData();
    }

    private JPanel createFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton reload = new JButton("Tai lai");
        searchField.addActionListener(e -> showFiltered());          // bam Enter de loc
        typeFilter.addActionListener(e -> showFiltered());
        lowStockOnly.addActionListener(e -> showFiltered());
        reload.addActionListener(e -> loadData());
        bar.add(new JLabel("Ten / barcode:"));
        bar.add(searchField);
        bar.add(new JLabel("Nhom:"));
        bar.add(typeFilter);
        bar.add(lowStockOnly);
        bar.add(reload);
        return bar;
    }

    private JPanel createForm() {
        JPanel fields = new JPanel(new GridLayout(0, 2, 6, 4));
        addRow(fields, "Barcode *", barcodeField);
        addRow(fields, "Ten san pham *", nameField);
        addRow(fields, "Loai hang", categoryBox);
        addRow(fields, "Gia ban *", priceField);
        addRow(fields, "Don vi tinh *", unitField);
        addRow(fields, "Ton toi thieu *", minStockField);
        addRow(fields, "Trang thai", statusBox);
        addRow(fields, "Nhom", typeBox);
        fields.add(extra1Label);
        fields.add(extra1Field);
        fields.add(extra2Label);
        fields.add(extra2Field);

        JButton add = new JButton("Them");
        JButton update = new JButton("Sua");
        JButton delete = new JButton("Xoa");
        JButton clear = new JButton("Nhap moi");
        add.addActionListener(e -> addProduct());
        update.addActionListener(e -> updateProduct());
        delete.addActionListener(e -> deleteProduct());
        clear.addActionListener(e -> clearForm());

        JPanel buttons = new JPanel(new GridLayout(2, 2, 6, 6));
        buttons.add(add);
        buttons.add(update);
        buttons.add(delete);
        buttons.add(clear);

        JLabel note = new JLabel("<html><i>Ton kho chi thay doi qua phieu nhap, xuat, kiem ke va ban hang.</i></html>");
        JPanel form = new JPanel(new BorderLayout(0, 10));
        form.setBorder(BorderFactory.createTitledBorder("Thong tin san pham"));
        form.add(fields, BorderLayout.NORTH);
        form.add(note, BorderLayout.CENTER);
        form.add(buttons, BorderLayout.SOUTH);
        form.setPreferredSize(new Dimension(380, 0));
        return form;
    }

    private static void addRow(JPanel panel, String label, Component input) {
        panel.add(new JLabel(label));
        panel.add(input);
    }

    // Doi 2 o rieng theo nhom:
    private void updateTypeFields() {
        boolean isFood = typeBox.getSelectedIndex() == 0;
        extra1Label.setText(isFood ? "Cach bao quan *" : "Bao hanh (thang) *");
        extra2Label.setText(isFood ? "Han dung (ngay) *" : "");
        extra2Field.setVisible(isFood);
    }

    private void loadData() {
        try {
            categoryNames.clear();
            categoryBox.removeAllItems();
            for (Category c : categoryDAO.findAll()) {
                categoryNames.put(c.getCategoryId(), c.getName());
                categoryBox.addItem(c);              // JComboBox hien c.toString() = ten loai
            }
            allProducts = productDAO.findAll();
            showFiltered();
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);
        }
    }

    private void showFiltered() {
        String keyword = searchField.getText().trim().toLowerCase();
        String[] types = {null, "FOODS", "ELECTRONICS"};
        String wantedType = types[typeFilter.getSelectedIndex()];

        shown = new ArrayList<>();
        model.setRowCount(0);
        for (Product p : allProducts) {
            boolean matchText = keyword.isEmpty()
                || p.getName().toLowerCase().contains(keyword)
                || p.getBarcode().contains(keyword);
            boolean matchType = wantedType == null || wantedType.equals(p.getProductType());
            boolean matchStock = !lowStockOnly.isSelected() || p.isLowStock();
            if (matchText && matchType && matchStock) {
                shown.add(p);
                model.addRow(new Object[] {
                    p.getProductId(), p.getBarcode(), p.getName(),
                    categoryNames.getOrDefault(p.getCategoryId(), "#" + p.getCategoryId()),
                    p.getProductType(), String.format("%,.0f", p.getCurrentPrice()),
                    p.getCurrentStock(), p.getMinStock(), p.getMeasureUnit(),
                    p.getStatus().getDisplayName(), detail(p)
                });
            }
        }
        clearForm();
    }

    // Thong tin rieng cua tung nhom san pham
    private static String detail(Product p) {
        if (p instanceof Foods f) {
            return "HSD " + f.getDayOfExpire() + " ngay";
        } else if (p instanceof Electronics el) {
            return "BH " + el.getMonthsOfWarranty() + " thang";
        }
        return "";
    }

    private Product selectedProduct() {
        int row = table.getSelectedRow();
        return row >= 0 ? shown.get(row) : null;
    }

    private void fillForm() {
        Product p = selectedProduct();
        if (p == null) {
            return;
        }
        barcodeField.setText(p.getBarcode());
        nameField.setText(p.getName());
        priceField.setText(p.getCurrentPrice().toPlainString());
        unitField.setText(p.getMeasureUnit());
        minStockField.setText(String.valueOf(p.getMinStock()));
        statusBox.setSelectedItem(p.getStatus());
        for (int i = 0; i < categoryBox.getItemCount(); i++) {
            if (categoryBox.getItemAt(i).getCategoryId() == p.getCategoryId()) {
                categoryBox.setSelectedIndex(i);
            }
        }
        if (p instanceof Foods f) {
            typeBox.setSelectedIndex(0);
            extra1Field.setText(f.getPreserveDescription());
            extra2Field.setText(String.valueOf(f.getDayOfExpire()));
        } else if (p instanceof Electronics el) {
            typeBox.setSelectedIndex(1);
            extra1Field.setText(String.valueOf(el.getMonthsOfWarranty()));
            extra2Field.setText("");
        }
    }

    private void clearForm() {
        table.clearSelection();
        for (JTextField f : new JTextField[] {barcodeField, nameField, priceField, unitField,
                minStockField, extra1Field, extra2Field}) {
            f.setText("");
        }
        statusBox.setSelectedItem(ProductStatus.ON_SALE);
    }

    // Doc form thanh Foods hoac Electronics. currentStock: giu ton hien tai (san pham moi = 0).
    private Product readForm(int id, int currentStock) {
        String barcode = barcodeField.getText().trim();
        String name = nameField.getText().trim();
        String unit = unitField.getText().trim();
        String extra1 = extra1Field.getText().trim();
        boolean isFood = typeBox.getSelectedIndex() == 0;
        Category category = (Category) categoryBox.getSelectedItem();
        if (barcode.isEmpty() || name.isEmpty() || unit.isEmpty() || extra1.isEmpty() || category == null) {
            JOptionPane.showMessageDialog(this, "Hay dien du cac o co dau *.",
                "Thieu thong tin", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        try {
            BigDecimal price = new BigDecimal(priceField.getText().trim());
            int minStock = Integer.parseInt(minStockField.getText().trim());
            ProductStatus status = (ProductStatus) statusBox.getSelectedItem();
            if (isFood) {
                int days = Integer.parseInt(extra2Field.getText().trim());
                return new Foods(id, category.getCategoryId(), name, price, status, minStock,
                    barcode, currentStock, unit, extra1, days);
            }
            return new Electronics(id, category.getCategoryId(), name, price, status, minStock,
                barcode, currentStock, unit, Integer.parseInt(extra1));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Gia ban, ton toi thieu, han dung, bao hanh phai la so.",
                "Sai so", JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }

    // Them / Sua / Xoa
    private void addProduct() {
        Product p = readForm(0, 0);                 
        if (p == null) {
            return;
        }
        try {
            int id = productDAO.insert(p);      
            JOptionPane.showMessageDialog(this, "Da them san pham ma " + id);
            loadData();
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);      
        }
    }

    private void updateProduct() {
        Product selected = selectedProduct();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Hay chon san pham can sua.");
            return;
        }
        Product p = readForm(selected.getProductId(), selected.getCurrentStock());
        if (p == null) {
            return;
        }
        try {
            productDAO.update(p);
            JOptionPane.showMessageDialog(this, "Da cap nhat san pham ma " + p.getProductId());
            loadData();
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);
        }
    }

    private void deleteProduct() {
        Product selected = selectedProduct();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Hay chon san pham can xoa.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
            "Xoa san pham \"" + selected.getName() + "\"?\n"
            + "(San pham da co phieu nhap / don hang thi khong xoa duoc, hay chuyen sang Ngung ban)",
            "Xac nhan", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            productDAO.delete(selected.getProductId());
            loadData();
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);       
        }
    }
}
