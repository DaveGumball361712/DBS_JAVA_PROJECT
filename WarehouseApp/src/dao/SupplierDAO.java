package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import model.Supplier;

public class SupplierDAO extends BaseDAO<Supplier, Integer> {

    private static final String SELECT =
        "SELECT SUPPLIER_ID, NAME, EMAIL, ADDRESS, TAX_CODE FROM WAREHOUSE.SUPPLIER ";
    private static final String SELECT_PHONES =
        "SELECT PHONE_NUMBER FROM WAREHOUSE.SUPPLIER_PHONE_NUMBER WHERE SUPPLIER_ID = ?";
    private static final String INSERT =
        "INSERT INTO WAREHOUSE.SUPPLIER (NAME, EMAIL, ADDRESS, TAX_CODE) VALUES (?, ?, ?, ?)";
    private static final String UPDATE =
        "UPDATE WAREHOUSE.SUPPLIER SET NAME = ?, EMAIL = ?, ADDRESS = ?, TAX_CODE = ? WHERE SUPPLIER_ID = ?";
    private static final String DELETE =
        "DELETE FROM WAREHOUSE.SUPPLIER WHERE SUPPLIER_ID = ?";
    private static final String INSERT_PHONE =
        "INSERT INTO WAREHOUSE.SUPPLIER_PHONE_NUMBER (SUPPLIER_ID, PHONE_NUMBER) VALUES (?, ?)";
    private static final String DELETE_PHONES =
        "DELETE FROM WAREHOUSE.SUPPLIER_PHONE_NUMBER WHERE SUPPLIER_ID = ?";

    @Override
    public List<Supplier> findAll() throws SQLException {
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(SELECT + "ORDER BY SUPPLIER_ID");
            ResultSet rs = ps.executeQuery()) {
            return readList(conn, rs);
        }
    }

    @Override
    public Supplier findById(Integer id) throws SQLException {
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE SUPPLIER_ID = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<Supplier> list = readList(conn, rs);
                return list.isEmpty() ? null : list.get(0);
            }
        }
    }

    // Tim theo ten hoac ma so thue
    public List<Supplier> search(String keyword) throws SQLException {
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(
                SELECT + "WHERE NAME LIKE ? OR TAX_CODE LIKE ? ORDER BY SUPPLIER_ID")) {
            String pattern = "%" + keyword.trim() + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                return readList(conn, rs);
            }
        }
    }

    @Override
    public Integer insert(Supplier s) throws SQLException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                int newId;
                try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                    fillMain(ps, s);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        newId = keys.getInt(1);
                    }
                }
                insertPhones(conn, newId, s.getPhoneNumbers());
                conn.commit();
                s.setSupplierId(newId);
                return newId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    @Override
    public boolean update(Supplier s) throws SQLException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                int rows;
                try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
                    fillMain(ps, s);
                    ps.setInt(5, s.getSupplierId());
                    rows = ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(DELETE_PHONES)) {
                    ps.setInt(1, s.getSupplierId());
                    ps.executeUpdate();
                }
                insertPhones(conn, s.getSupplierId(), s.getPhoneNumbers());
                conn.commit();
                return rows > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // Nha cung cap da co phieu nhap thi SQL Server bao loi khoa ngoai 
    @Override
    public boolean delete(Integer id) throws SQLException {
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(DELETE)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    protected Supplier map(ResultSet rs) throws SQLException {
        return new Supplier(
            rs.getInt("SUPPLIER_ID"),
            rs.getString("NAME"),
            rs.getString("EMAIL"),
            rs.getString("ADDRESS"),
            rs.getString("TAX_CODE"));
    }


    private List<Supplier> readList(Connection conn, ResultSet rs) throws SQLException {
        List<Supplier> list = new ArrayList<>();
        while (rs.next()) {
            list.add(map(rs));
        }
        for (Supplier s : list) {
            s.setPhoneNumbers(loadPhones(conn, s.getSupplierId()));
        }
        return list;
    }

    private void fillMain(PreparedStatement ps, Supplier s) throws SQLException {
        ps.setString(1, s.getName());
        ps.setString(2, s.getEmail());
        ps.setString(3, s.getAddress());
        ps.setString(4, s.getTaxCode());
    }

    private List<String> loadPhones(Connection conn, int supplierId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_PHONES)) {
            ps.setInt(1, supplierId);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> phones = new ArrayList<>();
                while (rs.next()) {
                    phones.add(rs.getString("PHONE_NUMBER"));
                }
                return phones;
            }
        }
    }

    private void insertPhones(Connection conn, int supplierId, List<String> phones) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_PHONE)) {
            for (String phone : phones) {
                ps.setInt(1, supplierId);
                ps.setString(2, phone);
                ps.executeUpdate();
            }
        }
    }
}