package dao;

import model.Customer;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO extends BaseDAO<Customer, Integer> {

    @Override
    protected Customer map(ResultSet rs) throws SQLException {
        return new Customer(
                rs.getInt("CUSTOMER_ID"),
                rs.getString("NAME"),
                rs.getString("EMAIL"),
                rs.getInt("POINTS"),
                rs.getString("PHONE_NUMBER"),
                rs.getDate("REGISTER_DATE").toLocalDate()
        );
    }

    public Customer findByPhone(String phoneNumber) throws SQLException {
        String sql = "SELECT CUSTOMER_ID, NAME, EMAIL, POINTS, PHONE_NUMBER, REGISTER_DATE " +
                    "FROM SALES.CUSTOMER WHERE PHONE_NUMBER = ?";
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phoneNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<Customer> findAll() throws SQLException {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT CUSTOMER_ID, NAME, EMAIL, POINTS, PHONE_NUMBER, REGISTER_DATE FROM SALES.CUSTOMER";
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
    public Customer findById(Integer id) throws SQLException {
        String sql = "SELECT CUSTOMER_ID, NAME, EMAIL, POINTS, PHONE_NUMBER, REGISTER_DATE " +
                    "FROM SALES.CUSTOMER WHERE CUSTOMER_ID = ?";
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
    public Integer insert(Customer obj) throws SQLException {
        String sql = "INSERT INTO SALES.CUSTOMER (NAME, EMAIL, POINTS, PHONE_NUMBER, REGISTER_DATE) " +
                    "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, obj.getName());
            ps.setString(2, obj.getEmail());
            ps.setInt(3, obj.getPoints());
            ps.setString(4, obj.getPhoneNumber());
            ps.setDate(5, Date.valueOf(obj.getRegisterDate()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int newId = keys.getInt(1);
                    obj.setCustomerId(newId);
                    return newId;
                }
            }
        }
        return null;
    }

    @Override
    public boolean update(Customer obj) throws SQLException {
        String sql = "UPDATE SALES.CUSTOMER SET NAME = ?, EMAIL = ?, POINTS = ?, " +
                    "PHONE_NUMBER = ?, REGISTER_DATE = ? WHERE CUSTOMER_ID = ?";
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, obj.getName());
            ps.setString(2, obj.getEmail());
            ps.setInt(3, obj.getPoints());
            ps.setString(4, obj.getPhoneNumber());
            ps.setDate(5, Date.valueOf(obj.getRegisterDate()));
            ps.setInt(6, obj.getCustomerId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(Integer id) throws SQLException {
        String sql = "DELETE FROM SALES.CUSTOMER WHERE CUSTOMER_ID = ?";
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}
