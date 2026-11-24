package dao;

import model.Electronics;
import model.Foods;
import model.Product;
import model.ProductStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO extends BaseDAO<Product, Integer> {

    private static final String BASE_SELECT =
            "SELECT p.PRODUCT_ID, p.CATEGORY_ID, p.NAME, p.CURRENT_PRICE, p.STATUS, " +
            "       p.MIN_STOCK, p.BARCODE, p.CURRENT_STOCK, p.MEASURE_UNIT, " +
            "       f.PRODUCT_ID AS FOOD_ID, f.PRESERVE_DESCRIPTION, f.DAY_OF_EXPIRE, " +
            "       el.PRODUCT_ID AS ELEC_ID, el.MONTHS_OF_WARRANTY " +
            "FROM CATALOG.PRODUCT p " +
            "LEFT JOIN CATALOG.FOODS f ON p.PRODUCT_ID = f.PRODUCT_ID " +
            "LEFT JOIN CATALOG.ELECTRONICS el ON p.PRODUCT_ID = el.PRODUCT_ID ";

    public Product findByBarcode(String barcode) throws SQLException {
        String sql = BASE_SELECT + "WHERE p.BARCODE = ?";
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, barcode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    @Override
    protected Product map(ResultSet rs) throws SQLException {
        int id = rs.getInt("PRODUCT_ID");
        int categoryId = rs.getInt("CATEGORY_ID");
        String name = rs.getString("NAME");
        var price = rs.getBigDecimal("CURRENT_PRICE");
        ProductStatus status = ProductStatus.fromDbValue(rs.getString("STATUS"));
        int minStock = rs.getInt("MIN_STOCK");
        String barcode = rs.getString("BARCODE");
        int currentStock = rs.getInt("CURRENT_STOCK");
        String unit = rs.getString("MEASURE_UNIT");

        if (rs.getObject("FOOD_ID") != null) {
            return new Foods(id, categoryId, name, price, status, minStock, barcode, currentStock, unit,
                    rs.getString("PRESERVE_DESCRIPTION"), rs.getInt("DAY_OF_EXPIRE"));
        } else if (rs.getObject("ELEC_ID") != null) {
            return new Electronics(id, categoryId, name, price, status, minStock, barcode, currentStock, unit,
                    rs.getInt("MONTHS_OF_WARRANTY"));
        }
        throw new SQLException("Sản phẩm ID=" + id + " chưa thuộc bảng FOODS hoặc ELECTRONICS.");
    }

    @Override
    public List<Product> findAll() throws SQLException {
        List<Product> list = new ArrayList<>();
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(BASE_SELECT);
            ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    @Override
    public Product findById(Integer id) throws SQLException {
        String sql = BASE_SELECT + "WHERE p.PRODUCT_ID = ?";
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    @Override
    public Integer insert(Product obj) throws SQLException {
        String sqlProd = "INSERT INTO CATALOG.PRODUCT (CATEGORY_ID, NAME, CURRENT_PRICE, STATUS, " +
                        "MIN_STOCK, BARCODE, CURRENT_STOCK, MEASURE_UNIT) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sqlProd, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, obj.getCategoryId());
                ps.setString(2, obj.getName());
                ps.setBigDecimal(3, obj.getCurrentPrice());
                ps.setString(4, obj.getStatus().getDbValue());
                ps.setInt(5, obj.getMinStock());
                ps.setString(6, obj.getBarcode());
                ps.setInt(7, obj.getCurrentStock());
                ps.setString(8, obj.getMeasureUnit());
                ps.executeUpdate();

                int newId;
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        newId = keys.getInt(1);
                        obj.setProductId(newId);
                    } else {
                        throw new SQLException("Không lấy được PRODUCT_ID vừa sinh.");
                    }
                }

                if (obj instanceof Foods f) {
                    try (PreparedStatement psSub = conn.prepareStatement(
                            "INSERT INTO CATALOG.FOODS (PRODUCT_ID, PRESERVE_DESCRIPTION, DAY_OF_EXPIRE) VALUES (?, ?, ?)")) {
                        psSub.setInt(1, newId);
                        psSub.setString(2, f.getPreserveDescription());
                        psSub.setInt(3, f.getDayOfExpire());
                        psSub.executeUpdate();
                    }
                } else if (obj instanceof Electronics el) {
                    try (PreparedStatement psSub = conn.prepareStatement(
                            "INSERT INTO CATALOG.ELECTRONICS (PRODUCT_ID, MONTHS_OF_WARRANTY) VALUES (?, ?)")) {
                        psSub.setInt(1, newId);
                        psSub.setInt(2, el.getMonthsOfWarranty());
                        psSub.executeUpdate();
                    }
                }

                conn.commit();
                return newId;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public boolean update(Product obj) throws SQLException {
        String sqlProd = "UPDATE CATALOG.PRODUCT SET CATEGORY_ID = ?, NAME = ?, CURRENT_PRICE = ?, STATUS = ?, " +
                        "MIN_STOCK = ?, BARCODE = ?, CURRENT_STOCK = ?, MEASURE_UNIT = ? WHERE PRODUCT_ID = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Cập nhật bảng cha CATALOG.PRODUCT
                int affected;
                try (PreparedStatement ps = conn.prepareStatement(sqlProd)) {
                    ps.setInt(1, obj.getCategoryId());
                    ps.setString(2, obj.getName());
                    ps.setBigDecimal(3, obj.getCurrentPrice());
                    ps.setString(4, obj.getStatus().getDbValue());
                    ps.setInt(5, obj.getMinStock());
                    ps.setString(6, obj.getBarcode());
                    ps.setInt(7, obj.getCurrentStock());
                    ps.setString(8, obj.getMeasureUnit());
                    ps.setInt(9, obj.getProductId());
                    affected = ps.executeUpdate();
                }

                // Cập nhật bảng con CATALOG.FOODS hoặc CATALOG.ELECTRONICS
                if (obj instanceof Foods f) {
                    try (PreparedStatement psSub = conn.prepareStatement(
                            "UPDATE CATALOG.FOODS SET PRESERVE_DESCRIPTION = ?, DAY_OF_EXPIRE = ? WHERE PRODUCT_ID = ?")) {
                        psSub.setString(1, f.getPreserveDescription());
                        psSub.setInt(2, f.getDayOfExpire());
                        psSub.setInt(3, f.getProductId());
                        psSub.executeUpdate();
                    }
                } else if (obj instanceof Electronics el) {
                    try (PreparedStatement psSub = conn.prepareStatement(
                            "UPDATE CATALOG.ELECTRONICS SET MONTHS_OF_WARRANTY = ? WHERE PRODUCT_ID = ?")) {
                        psSub.setInt(1, el.getMonthsOfWarranty());
                        psSub.setInt(2, el.getProductId());
                        psSub.executeUpdate();
                    }
                }

                conn.commit();
                return affected > 0;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public boolean delete(Integer id) throws SQLException {
        String sql = "DELETE FROM CATALOG.PRODUCT WHERE PRODUCT_ID = ?";
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}