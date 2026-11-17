package dao;

import model.Category;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO extends BaseDAO<Category, Integer> {

    @Override
    protected Category map(ResultSet rs) throws SQLException {
        int id = rs.getInt("CATEGORY_ID");
        int rawParent = rs.getInt("PARENT_CATEGORY_ID");
        Integer parentId = rs.wasNull() ? null : rawParent;
        String name = rs.getString("NAME");
        String desc = rs.getString("DESCRIPTION");
        return new Category(id, parentId, name, desc);
    }

    @Override
    public List<Category> findAll() throws SQLException {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT CATEGORY_ID, PARENT_CATEGORY_ID, NAME, DESCRIPTION FROM CATALOG.CATEGORY";
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    @Override
    public Category findById(Integer id) throws SQLException {
        String sql = "SELECT CATEGORY_ID, PARENT_CATEGORY_ID, NAME, DESCRIPTION FROM CATALOG.CATEGORY WHERE CATEGORY_ID = ?";
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
    public Integer insert(Category obj) throws SQLException {
        String sql = "INSERT INTO CATALOG.CATEGORY (PARENT_CATEGORY_ID, NAME, DESCRIPTION) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (obj.getParentCategoryId() == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, obj.getParentCategoryId());
            }
            ps.setString(2, obj.getName());
            ps.setString(3, obj.getDescription());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int newId = keys.getInt(1);
                    obj.setCategoryId(newId);
                    return newId;
                }
            }
        }
        return null;
    }

    @Override
    public boolean update(Category obj) throws SQLException {
        String sql = "UPDATE CATALOG.CATEGORY SET PARENT_CATEGORY_ID = ?, NAME = ?, DESCRIPTION = ? WHERE CATEGORY_ID = ?";
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            if (obj.getParentCategoryId() == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, obj.getParentCategoryId());
            }
            ps.setString(2, obj.getName());
            ps.setString(3, obj.getDescription());
            ps.setInt(4, obj.getCategoryId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(Integer id) throws SQLException {
        String sql = "DELETE FROM CATALOG.CATEGORY WHERE CATEGORY_ID = ?";
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}