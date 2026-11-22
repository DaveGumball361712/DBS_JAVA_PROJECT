package dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import util.DBConnection;
public class ReportDAO {
    public List<Object[]> revenueByDay(LocalDate from, LocalDate to) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
            CallableStatement cs = conn.prepareCall("{call SALES.SP_REVENUE_REPORT(?, ?)}")) {
            cs.setDate(1, Date.valueOf(from));
            cs.setDate(2, Date.valueOf(to));
            try (ResultSet rs = cs.executeQuery()) {
                List<Object[]> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new Object[] {
                        rs.getDate("REPORT_DATE").toLocalDate(),
                        rs.getInt("ORDER_COUNT"),
                        rs.getBigDecimal("REVENUE")
                    });
                }
                return rows;
            }
        }
    }

    // View cua Dat: doanh thu theo thu ngan
    public List<Object[]> revenueByCashier(LocalDate from, LocalDate to) throws SQLException {
        return callRange("{call SALES.SP_REVENUE_BY_CASHIER(?, ?)}", from, to);
    }
 
    public List<Object[]> topProducts(LocalDate from, LocalDate to) throws SQLException {
        return callRange("{call SALES.SP_TOP_PRODUCTS(?, ?, 20)}", from, to);
    }
 
    private List<Object[]> callRange(String call, LocalDate from, LocalDate to) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
            CallableStatement cs = conn.prepareCall(call)) {
            cs.setDate(1, Date.valueOf(from));
            cs.setDate(2, Date.valueOf(to));
            try (ResultSet rs = cs.executeQuery()) {
                int n = rs.getMetaData().getColumnCount();
                List<Object[]> rows = new ArrayList<>();
                while (rs.next()) {
                    Object[] row = new Object[n];
                    for (int i = 0; i < n; i++) {
                        row[i] = rs.getObject(i + 1);
                    }
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    public List<Object[]> lowStock() throws SQLException {
        return query("SELECT PRODUCT_ID, PRODUCT_NAME, CATEGORY_NAME, CURRENT_STOCK, MIN_STOCK, SHORTAGE_QUANTITY "
                + "FROM CATALOG.V_LOW_STOCK ORDER BY SHORTAGE_QUANTITY DESC");
    }

    public List<Object[]> importBySupplier() throws SQLException {
        return query("SELECT SUPPLIER_NAME, RECEIPT_COUNT, TOTAL_QUANTITY, TOTAL_IMPORT_AMOUNT "
                + "FROM WAREHOUSE.V_IMPORT_BY_SUPPLIER WHERE RECEIPT_COUNT > 0 "
                + "ORDER BY TOTAL_IMPORT_AMOUNT DESC");
    }

    private List<Object[]> query(String sql) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
            int columnCount = rs.getMetaData().getColumnCount();
            List<Object[]> rows = new ArrayList<>();
            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    row[i] = rs.getObject(i + 1);
                }
                rows.add(row);
            }
            return rows;
        }
    }
}