package dao;

import model.Account;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AccountDAO extends BaseDAO<Account, String> {

    public boolean checkLogin(String username, String password) throws SQLException {
    String sql = "{call HR.SP_LOGIN(?, ?)}";
    try (Connection conn = getConnection();
        java.sql.CallableStatement cs = conn.prepareCall(sql)) {
        cs.setString(1, username);
        cs.setString(2, password);
        try (ResultSet rs = cs.executeQuery()) {
            return rs.next();
            }
        }
    }

    @Override
    protected Account map(ResultSet rs) throws SQLException {
        return new Account(rs.getString("USERNAME"), rs.getString("PASSWORD"));
    }

    @Override
    public List<Account> findAll() throws SQLException {
        List<Account> list = new ArrayList<>();
        String sql = "SELECT USERNAME, PASSWORD FROM HR.ACCOUNT";
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
    public Account findById(String username) throws SQLException {
        String sql = "SELECT USERNAME, PASSWORD FROM HR.ACCOUNT WHERE USERNAME = ?";
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    @Override
    public String insert(Account obj) throws SQLException {
        String sql = "INSERT INTO HR.ACCOUNT (USERNAME, PASSWORD) VALUES (?, ?)";
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, obj.getUsername());
            ps.setString(2, obj.getPassword());
            ps.executeUpdate();
            return obj.getUsername();
        }
    }

    @Override
    public boolean update(Account obj) throws SQLException {
        String sql = "UPDATE HR.ACCOUNT SET PASSWORD = ? WHERE USERNAME = ?";
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, obj.getPassword());
            ps.setString(2, obj.getUsername());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(String username) throws SQLException {
        String sql = "DELETE FROM HR.ACCOUNT WHERE USERNAME = ?";
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            return ps.executeUpdate() > 0;
        }
    }
}