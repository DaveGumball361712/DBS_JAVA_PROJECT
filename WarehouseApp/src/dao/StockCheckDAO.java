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

import model.CheckDetail;
import model.StockCheck;
import model.StockCheckStatus;

public class StockCheckDAO extends BaseDAO<StockCheck, Integer> {

    private static final String SELECT =
        "SELECT STOCK_CHECK_ID, EMPLOYEE_ID, MANAGER_ID, CHECK_DATE, STATUS, APPROVE_TIME "
        + "FROM WAREHOUSE.STOCK_CHECK ";
    private static final String SELECT_DETAILS =
        "SELECT PRODUCT_ID, SYSTEM_QUANTITY, ACTUAL_QUANTITY FROM WAREHOUSE.CHECK_DETAIL WHERE STOCK_CHECK_ID = ?";
    private static final String INSERT =
        "INSERT INTO WAREHOUSE.STOCK_CHECK (EMPLOYEE_ID, CHECK_DATE, STATUS) VALUES (?, ?, ?)";
    private static final String INSERT_DETAIL =
        "INSERT INTO WAREHOUSE.CHECK_DETAIL (STOCK_CHECK_ID, PRODUCT_ID, SYSTEM_QUANTITY, ACTUAL_QUANTITY) "
        + "VALUES (?, ?, ?, ?)";
    // Chi sua / xoa duoc phieu CHUA duyet 
    private static final String UPDATE =
        "UPDATE WAREHOUSE.STOCK_CHECK SET CHECK_DATE = ? WHERE STOCK_CHECK_ID = ? AND MANAGER_ID IS NULL";
    private static final String DELETE_DETAILS =
        "DELETE FROM WAREHOUSE.CHECK_DETAIL WHERE STOCK_CHECK_ID = ?";
    private static final String DELETE =
        "DELETE FROM WAREHOUSE.STOCK_CHECK WHERE STOCK_CHECK_ID = ? AND MANAGER_ID IS NULL";
    private static final String APPROVE =
        "{call WAREHOUSE.SP_APPROVE_STOCK_CHECK(?, ?)}";

    @Override
    public List<StockCheck> findAll() throws SQLException {
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(SELECT + "ORDER BY STOCK_CHECK_ID DESC");
            ResultSet rs = ps.executeQuery()) {
            return readList(conn, rs);
        }
    }

    // Danh sach phieu cho duyet 
    public List<StockCheck> findPending() throws SQLException {
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(
                SELECT + "WHERE MANAGER_ID IS NULL ORDER BY CHECK_DATE, STOCK_CHECK_ID");
            ResultSet rs = ps.executeQuery()) {
            return readList(conn, rs);
        }
    }

    @Override
    public StockCheck findById(Integer id) throws SQLException {
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE STOCK_CHECK_ID = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<StockCheck> list = readList(conn, rs);
                return list.isEmpty() ? null : list.get(0);
            }
        }
    }

    // Phieu moi lap luon o trang thai cho duyet
    @Override
    public Integer insert(StockCheck sc) throws SQLException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                int newId;
                try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, sc.getEmployeeId());
                    ps.setDate(2, Date.valueOf(sc.getCheckDate()));
                    ps.setString(3, StockCheckStatus.WAITING_FOR_APPROVAL.getDbValue());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        newId = keys.getInt(1);
                    }
                }
                insertDetails(conn, newId, sc.getDetails());
                conn.commit();
                sc.setStockCheckId(newId);
                sc.setStatus(StockCheckStatus.WAITING_FOR_APPROVAL);
                for (CheckDetail d : sc.getDetails()) {
                    d.setStockCheckId(newId);
                }
                return newId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // Sua ngay kiem va danh sach chi tiet cua phieu chua duyet
    @Override
    public boolean update(StockCheck sc) throws SQLException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                int rows;
                try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
                    ps.setDate(1, Date.valueOf(sc.getCheckDate()));
                    ps.setInt(2, sc.getStockCheckId());
                    rows = ps.executeUpdate();
                }
                if (rows == 0) {            // khong co phieu hoac phieu da duyet
                    conn.rollback();
                    return false;
                }
                try (PreparedStatement ps = conn.prepareStatement(DELETE_DETAILS)) {
                    ps.setInt(1, sc.getStockCheckId());
                    ps.executeUpdate();
                }
                insertDetails(conn, sc.getStockCheckId(), sc.getDetails());
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

    // Duyet phieu: SP bao loi 50020, 50021, 50022; ton kho duoc cap nhat theo so thuc te
    public void approve(int stockCheckId, int managerId) throws SQLException {
        try (Connection conn = getConnection();
            CallableStatement cs = conn.prepareCall(APPROVE)) {
            cs.setInt(1, stockCheckId);
            cs.setInt(2, managerId);
            cs.execute();
        }
    }

    @Override
    protected StockCheck map(ResultSet rs) throws SQLException {
        Integer managerId = rs.getObject("MANAGER_ID", Integer.class);   // NULL -> null
        Timestamp approve = rs.getTimestamp("APPROVE_TIME");
        return new StockCheck(
            rs.getInt("STOCK_CHECK_ID"),
            rs.getInt("EMPLOYEE_ID"),
            managerId,
            rs.getDate("CHECK_DATE").toLocalDate(),
            StockCheckStatus.fromDbValue(rs.getString("STATUS")),
            approve == null ? null : approve.toLocalDateTime());
    }

    private List<StockCheck> readList(Connection conn, ResultSet rs) throws SQLException {
        List<StockCheck> list = new ArrayList<>();
        while (rs.next()) {
            list.add(map(rs));
        }
        for (StockCheck sc : list) {
            sc.setDetails(loadDetails(conn, sc.getStockCheckId()));
        }
        return list;
    }

    private List<CheckDetail> loadDetails(Connection conn, int stockCheckId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_DETAILS)) {
            ps.setInt(1, stockCheckId);
            try (ResultSet rs = ps.executeQuery()) {
                List<CheckDetail> details = new ArrayList<>();
                while (rs.next()) {
                    details.add(new CheckDetail(stockCheckId,
                        rs.getInt("PRODUCT_ID"),
                        rs.getInt("SYSTEM_QUANTITY"),
                        rs.getInt("ACTUAL_QUANTITY")));
                }
                return details;
            }
        }
    }

    private void insertDetails(Connection conn, int stockCheckId, List<CheckDetail> details) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_DETAIL)) {
            for (CheckDetail d : details) {
                ps.setInt(1, stockCheckId);
                ps.setInt(2, d.getProductId());
                ps.setInt(3, d.getSystemQuantity());
                ps.setInt(4, d.getActualQuantity());
                ps.executeUpdate();
            }
        }
    }
}