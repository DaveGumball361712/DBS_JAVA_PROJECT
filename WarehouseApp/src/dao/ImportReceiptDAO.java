package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import model.ImportLine;
import model.ImportReceipt;

public class ImportReceiptDAO extends BaseDAO<ImportReceipt, Integer> {

    private static final String SELECT =
        "SELECT IMPORT_RECEIPT_ID, EMPLOYEE_ID, SUPPLIER_ID, IMPORT_DATE, DESCRIPTION "
        + "FROM WAREHOUSE.IMPORT_RECEIPT ";
    private static final String SELECT_LINES =
        "SELECT PRODUCT_ID, IMPORT_PRICE, QUANTITY FROM WAREHOUSE.INCLUDES WHERE IMPORT_RECEIPT_ID = ?";
    private static final String INSERT =
        "INSERT INTO WAREHOUSE.IMPORT_RECEIPT (EMPLOYEE_ID, SUPPLIER_ID, IMPORT_DATE, DESCRIPTION) "
        + "VALUES (?, ?, ?, ?)";
    // Moi dong INCLUDES them vao se kich hoat trigger
    private static final String INSERT_LINE =
        "INSERT INTO WAREHOUSE.INCLUDES (IMPORT_RECEIPT_ID, PRODUCT_ID, IMPORT_PRICE, QUANTITY) "
        + "VALUES (?, ?, ?, ?)";
    private static final String UPDATE =
        "UPDATE WAREHOUSE.IMPORT_RECEIPT SET DESCRIPTION = ? WHERE IMPORT_RECEIPT_ID = ?";

    @Override
    public List<ImportReceipt> findAll() throws SQLException {
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(SELECT + "ORDER BY IMPORT_RECEIPT_ID DESC");
            ResultSet rs = ps.executeQuery()) {
            return readList(conn, rs);
        }
    }

    // Cac phieu nhap cua mot nha cung cap
    public List<ImportReceipt> findBySupplier(int supplierId) throws SQLException {
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(
                SELECT + "WHERE SUPPLIER_ID = ? ORDER BY IMPORT_DATE DESC")) {
            ps.setInt(1, supplierId);
            try (ResultSet rs = ps.executeQuery()) {
                return readList(conn, rs);
            }
        }
    }

    @Override
    public ImportReceipt findById(Integer id) throws SQLException {
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE IMPORT_RECEIPT_ID = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<ImportReceipt> list = readList(conn, rs);
                return list.isEmpty() ? null : list.get(0);
            }
        }
    }

    // Lap phieu nhap
    @Override
    public Integer insert(ImportReceipt r) throws SQLException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                int newId;
                try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, r.getEmployeeId());
                    ps.setInt(2, r.getSupplierId());
                    ps.setTimestamp(3, Timestamp.valueOf(r.getImportDate()));
                    ps.setString(4, r.getDescription());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        newId = keys.getInt(1);
                    }
                }
                insertLines(conn, newId, r.getLines());
                conn.commit();
                r.setImportReceiptId(newId);
                for (ImportLine line : r.getLines()) {
                    line.setImportReceiptId(newId);
                }
                return newId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // Chi sua ghi chu.
    @Override
    public boolean update(ImportReceipt r) throws SQLException {
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setString(1, r.getDescription());
            ps.setInt(2, r.getImportReceiptId());
            return ps.executeUpdate() > 0;
        }
    }

    // Khong cho xoa
    @Override
    public boolean delete(Integer id) throws SQLException {
        throw new UnsupportedOperationException(
            "Khong the xoa phieu nhap: hang da duoc cong vao ton kho.");
    }

    @Override
    protected ImportReceipt map(ResultSet rs) throws SQLException {
        return new ImportReceipt(
            rs.getInt("IMPORT_RECEIPT_ID"),
            rs.getInt("EMPLOYEE_ID"),
            rs.getInt("SUPPLIER_ID"),
            rs.getTimestamp("IMPORT_DATE").toLocalDateTime(),
            rs.getString("DESCRIPTION"));
    }


    private List<ImportReceipt> readList(Connection conn, ResultSet rs) throws SQLException {
        List<ImportReceipt> list = new ArrayList<>();
        while (rs.next()) {
            list.add(map(rs));
        }
        for (ImportReceipt r : list) {
            r.setLines(loadLines(conn, r.getImportReceiptId()));
        }
        return list;
    }

    private List<ImportLine> loadLines(Connection conn, int receiptId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_LINES)) {
            ps.setInt(1, receiptId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ImportLine> lines = new ArrayList<>();
                while (rs.next()) {
                    lines.add(new ImportLine(receiptId,
                        rs.getInt("PRODUCT_ID"),
                        rs.getBigDecimal("IMPORT_PRICE"),
                        rs.getInt("QUANTITY")));
                }
                return lines;
            }
        }
    }

    private void insertLines(Connection conn, int receiptId, List<ImportLine> lines) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_LINE)) {
            for (ImportLine line : lines) {
                ps.setInt(1, receiptId);
                ps.setInt(2, line.getProductId());
                ps.setBigDecimal(3, line.getImportPrice());
                ps.setInt(4, line.getQuantity());
                ps.executeUpdate();
            }
        }
    }
}