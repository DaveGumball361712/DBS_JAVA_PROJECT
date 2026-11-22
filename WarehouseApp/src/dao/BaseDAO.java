package dao;

import util.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public abstract class BaseDAO<T, K> {

    protected Connection getConnection() throws SQLException {
        return DBConnection.getConnection();
    }

    // Lấy toàn bộ danh sách
    public abstract List<T> findAll() throws SQLException;

    // Tìm theo khóa chính
    public abstract T findById(K id) throws SQLException;

    // Thêm mới và trả về khóa chính (ID) vừa sinh ra
    public abstract K insert(T obj) throws SQLException;

    // Cập nhật bản ghi
    public abstract boolean update(T obj) throws SQLException;

    // Xóa theo khóa chính
    public abstract boolean delete(K id) throws SQLException;

    // Ánh xạ 1 dòng từ ResultSet sang đối tượng Model T
    protected abstract T map(ResultSet rs) throws SQLException;
}
