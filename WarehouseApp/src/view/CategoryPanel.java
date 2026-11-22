package view;

import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import dao.CategoryDAO;
import dao.ProductDAO;
import model.Category;
import model.Product;
import util.ErrorHandler;

public class CategoryPanel extends JPanel {

    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private List<Product> allProducts = new ArrayList<>();

    private final DefaultTreeModel treeModel = new DefaultTreeModel(new DefaultMutableTreeNode("Dang tai..."));
    private final JTree tree = new JTree(treeModel);

    private final JLabel idLabel = new JLabel("-");
    private final JLabel parentLabel = new JLabel("-");
    private final JTextField nameField = new JTextField(25);
    private final JTextArea descArea = new JTextArea(3, 25);

    private final DefaultTableModel productModel =
        new DefaultTableModel(new String[] {"Ma SP", "Ten san pham", "Nhom", "Gia ban", "Ton kho"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

    public CategoryPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        tree.getSelectionModel().setSelectionMode(
            javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(e -> onSelect());

        JPanel right = new JPanel(new BorderLayout(0, 10));
        right.add(createForm(), BorderLayout.NORTH);
        JTable productTable = new JTable(productModel);
        productTable.setRowHeight(24);
        JScrollPane productScroll = new JScrollPane(productTable);
        productScroll.setBorder(BorderFactory.createTitledBorder("San pham thuoc loai dang chon"));
        right.add(productScroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tree), right);
        split.setDividerLocation(300);
        add(split, BorderLayout.CENTER);

        loadData();
    }

    private JPanel createForm() {
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);

        JPanel grid = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        String[] labels = {"Ma loai:", "Loai cha:", "Ten loai *:", "Mo ta *:"};
        Component[] inputs = {idLabel, parentLabel, nameField, new JScrollPane(descArea)};
        for (int i = 0; i < labels.length; i++) {
            c.gridx = 0;
            c.gridy = i;
            c.weightx = 0;
            grid.add(new JLabel(labels[i]), c);
            c.gridx = 1;
            c.weightx = 1;
            grid.add(inputs[i], c);
        }

        JButton addChild = new JButton("Them loai con");
        JButton update = new JButton("Luu thay doi");
        JButton delete = new JButton("Xoa");
        JButton reload = new JButton("Tai lai");
        addChild.addActionListener(e -> addChild());
        update.addActionListener(e -> updateCategory());
        delete.addActionListener(e -> deleteCategory());
        reload.addActionListener(e -> loadData());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(addChild);
        buttons.add(update);
        buttons.add(delete);
        buttons.add(reload);

        JPanel form = new JPanel(new BorderLayout());
        form.setBorder(BorderFactory.createTitledBorder("Thong tin loai hang"));
        form.add(grid, BorderLayout.CENTER);
        form.add(buttons, BorderLayout.SOUTH);
        return form;
    }


    private void loadData() {
        try {
            DefaultMutableTreeNode root = categoryDAO.findTree();   
            treeModel.setRoot(root);
            for (int i = 0; i < tree.getRowCount(); i++) {         
                tree.expandRow(i);
            }
            allProducts = productDAO.findAll();
            tree.setSelectionRow(0);                                 
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);
        }
    }

    private DefaultMutableTreeNode selectedNode() {
        return (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
    }

    // Tra ve Category dang chon
    private Category selectedCategory() {
        DefaultMutableTreeNode node = selectedNode();
        if (node != null && node.getUserObject() instanceof Category) {
            return (Category) node.getUserObject();
        }
        return null;
    }

    private void onSelect() {
        DefaultMutableTreeNode node = selectedNode();
        if (node == null) {
            return;
        }
        Category cat = selectedCategory();
        if (cat == null) {
            idLabel.setText("-");
            parentLabel.setText("-");
            nameField.setText("");
            descArea.setText("");
        } else {
            idLabel.setText(String.valueOf(cat.getCategoryId()));
            TreeNode parent = node.getParent();
            parentLabel.setText(parent != null ? parent.toString() : "-");
            nameField.setText(cat.getName());
            descArea.setText(cat.getDescription());
        }
        showProducts(node);
    }

    // Hien san pham cua loai dang chon VA tat ca loai con ben duoi no
    private void showProducts(DefaultMutableTreeNode node) {
        Set<Integer> ids = new HashSet<>();
        Enumeration<TreeNode> all = node.breadthFirstEnumeration();
        while (all.hasMoreElements()) {
            Object obj = ((DefaultMutableTreeNode) all.nextElement()).getUserObject();
            if (obj instanceof Category) {
                ids.add(((Category) obj).getCategoryId());
            }
        }
        boolean isRoot = !(node.getUserObject() instanceof Category);

        productModel.setRowCount(0);
        for (Product p : allProducts) {
            if (isRoot || ids.contains(p.getCategoryId())) {
                productModel.addRow(new Object[] {
                    p.getProductId(), p.getName(), p.getProductType(),
                    String.format("%,.0f", p.getCurrentPrice()), p.getCurrentStock()
                });
            }
        }
    }

    // Them / Sua / Xoa

    private void addChild() {
        Category parent = selectedCategory();              // null = them loai goc
        String where = parent == null ? "loai goc" : "loai con cua \"" + parent.getName() + "\"";

        String name = JOptionPane.showInputDialog(this, "Ten " + where + ":");
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        String desc = JOptionPane.showInputDialog(this, "Mo ta:");
        if (desc == null || desc.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Mo ta khong duoc de trong.");
            return;
        }
        try {
            Integer parentId = parent == null ? null : parent.getCategoryId();
            categoryDAO.insert(new Category(0, parentId, name.trim(), desc.trim()));
            loadData();
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);
        }
    }

    private void updateCategory() {
        Category cat = selectedCategory();
        if (cat == null) {
            JOptionPane.showMessageDialog(this, "Hay chon mot loai hang de sua.");
            return;
        }
        String name = nameField.getText().trim();
        String desc = descArea.getText().trim();
        if (name.isEmpty() || desc.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ten va mo ta khong duoc de trong.");
            return;
        }
        try {
            categoryDAO.update(new Category(cat.getCategoryId(), cat.getParentCategoryId(), name, desc));
            loadData();
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);
        }
    }

    private void deleteCategory() {
        Category cat = selectedCategory();
        if (cat == null) {
            JOptionPane.showMessageDialog(this, "Hay chon mot loai hang de xoa.");
            return;
        }
        if (selectedNode().getChildCount() > 0) {
            JOptionPane.showMessageDialog(this, "Loai nay con loai con. Hay xoa loai con truoc.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
            "Xoa loai \"" + cat.getName() + "\"?", "Xac nhan", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            categoryDAO.delete(cat.getCategoryId());
            loadData();
        } catch (SQLException ex) {
            ErrorHandler.showError(this, ex);   
        }
    }
}