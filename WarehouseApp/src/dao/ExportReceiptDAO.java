package dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import model.ExportLine;
import model.ExportReason;
import model.ExportReceipt;

public class ExportReceiptDAO extends BaseDAO<ExportReceipt, Integer> {

    private static final String SELECT =
        "SELECT EXPORT_RECEIPT_ID, EMPLOYEE_ID, MANAGER_ID, EXPORT_DATE, REASON, APPROVE_TIME "
        + "FROM WAREHOUSE.EXPORT_RECEIPT ";
    private static final String SELECT_LINES =
        "SELECT PRODUCT_ID, QUANTITY FROM WAREHOUSE.EXPORT_FROM WHERE EXPORT_RECEIPT_ID = ?";
    private static final String INSERT =
        "INSERT INTO WAREHOUSE.EXPORT_RECEIPT (EMPLOYEE_ID, EXPORT_DATE, REASON) VALUES (?, ?, ?)";
    private static final String INSERT_LINE =
        "INSERT INTO WAREHOUSE.EXPORT_FROM (EXPORT_RECEIPT_ID, PRODUCT_ID, QUANTITY) VALUES (?, ?, ?)";
    private static final String INSERT_RETURN =
        "INSERT INTO WAREHOUSE.RETURN_SUPPLIER (EXPORT_RECEIPT_ID, SUPPLIER_ID) VALUES (?, ?)";
    // Chi sua, xoa duoc phieu CHUA duyet 
    private static final String UPDATE =
        "UPDATE WAREHOUSE.EXPORT_RECEIPT SET EXPORT_DATE = ? WHERE EXPORT_RECEIPT_ID = ? AND MANAGER_ID IS NULL";
    private static final String DELETE_LINES =
        "DELETE FROM WAREHOUSE.EXPORT_FROM WHERE EXPORT_RECEIPT_ID = ?";
    private static final String DELETE =
        "DELETE FROM WAREHOUSE.EXPORT_RECEIPT WHERE EXPORT_RECEIPT_ID = ? AND MANAGER_ID IS NULL";
    private static final String APPROVE =
        "{call WAREHOUSE.SP_APPROVE_EXPORT_RECEIPT(?, ?)}";

    @Override
    public List<ExportReceipt> findAll() throws SQLException {
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(SELECT + "ORDER BY EXPORT_RECEIPT_ID DESC");
            ResultSet rs = ps.executeQuery()) {
            return readList(conn, rs);
        }
    }

    // Danh sach phieu cho duyet
    public List<ExportReceipt> findPending() throws SQLException {
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(
                SELECT + "WHERE MANAGER_ID IS NULL ORDER BY EXPORT_DATE, EXPORT_RECEIPT_ID");
            ResultSet rs = ps.executeQuery()) {
            return readList(conn, rs);
        }
    }

    @Override
    public ExportReceipt findById(Integer id) throws SQLException {
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE EXPORT_RECEIPT_ID = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<ExportReceipt> list = readList(conn, rs);
                return list.isEmpty() ? null : list.get(0);
            }
        }
    }

    // Phieu xuat thuong (het han, hu hong):
    @Override
    public Integer insert(ExportReceipt r) throws SQLException {
        return insert(r, null);
    }

    // Phieu tra nha cung cap
    public Integer insert(ExportReceipt r, Integer returnSupplierId) throws SQLException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                int newId;
                try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, r.getEmployeeId());
                    ps.setDate(2, Date.valueOf(r.getExportDate()));
                    ps.setString(3, r.getReason().getDbValue());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        newId = keys.getInt(1);
                    }
                }
                insertLines(conn, newId, r.getLines());
                if (returnSupplierId != null) {
                    try (PreparedStatement ps = conn.prepareStatement(INSERT_RETURN)) {
                        ps.setInt(1, newId);
                        ps.setInt(2, returnSupplierId);
                        ps.executeUpdate();
                    }
                }
                conn.commit();
                r.setExportReceiptId(newId);
                for (ExportLine line : r.getLines()) {
                    line.setExportReceiptId(newId);
                }
                return newId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // Sua ngay xuat va danh sach san pham.
    @Override
    public boolean update(ExportReceipt r) throws SQLException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                int rows;
                try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
                    ps.setDate(1, Date.valueOf(r.getExportDate()));
                    ps.setInt(2, r.getExportReceiptId());
                    rows = ps.executeUpdate();
                }
                if (rows == 0) {            // khong co phieu hoac phieu da duyet
                    conn.rollback();
                    return false;
                }
                try (PreparedStatement ps = conn.prepareStatement(DELETE_LINES)) {
                    ps.setInt(1, r.getExportReceiptId());
                    ps.executeUpdate();
                }
                insertLines(conn, r.getExportReceiptId(), r.getLines());
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // Xoa phieu chua duyet
    @Override
    public boolean delete(Integer id) throws SQLException {
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(DELETE)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // Duyet phieu: SP kiem tra loi
    public void approve(int exportReceiptId, int managerId) throws SQLException {
        try (Connection conn = getConnection();
            CallableStatement cs = conn.prepareCall(APPROVE)) {
            cs.setInt(1, exportReceiptId);
            cs.setInt(2, managerId);
            cs.execute();
        }
    }

    @Override
    protected ExportReceipt map(ResultSet rs) throws SQLException {
        Integer managerId = rs.getObject("MANAGER_ID", Integer.class);   // NULL -> null
        Timestamp approve = rs.getTimestamp("APPROVE_TIME");
        return new ExportReceipt(
            rs.getInt("EXPORT_RECEIPT_ID"),
            rs.getInt("EMPLOYEE_ID"),
            managerId,
            rs.getDate("EXPORT_DATE").toLocalDate(),
            ExportReason.fromDbValue(rs.getString("REASON")),
            approve == null ? null : approve.toLocalDateTime());
    }


    private List<ExportReceipt> readList(Connection conn, ResultSet rs) throws SQLException {
        List<ExportReceipt> list = new ArrayList<>();
        while (rs.next()) {
            list.add(map(rs));
        }
        for (ExportReceipt r : list) {
            r.setLines(loadLines(conn, r.getExportReceiptId()));
        }
        return list;
    }

    private List<ExportLine> loadLines(Connection conn, int receiptId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_LINES)) {
            ps.setInt(1, receiptId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ExportLine> lines = new ArrayList<>();
                while (rs.next()) {
                    lines.add(new ExportLine(receiptId, rs.getInt("PRODUCT_ID"), rs.getInt("QUANTITY")));
                }
                return lines;
            }
        }
    }

    private void insertLines(Connection conn, int receiptId, List<ExportLine> lines) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_LINE)) {
            for (ExportLine line : lines) {
                ps.setInt(1, receiptId);
                ps.setInt(2, line.getProductId());
                ps.setInt(3, line.getQuantity());
                ps.executeUpdate();
            }
        }
    }
}