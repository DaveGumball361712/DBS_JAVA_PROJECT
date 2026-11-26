package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.OrderLine;
import model.OrderStatus;
import model.Payment;
import model.SalesOrder;

public class SalesOrderDAO extends BaseDAO<SalesOrder, Integer> {

    private String toOrderLinesJson(List<OrderLine> lines) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < lines.size(); i++) {
            OrderLine line = lines.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"productId\":").append(line.getProductId())
            .append(",\"quantity\":").append(line.getQuantity())
            .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    public int save(SalesOrder order) throws SQLException {
        String callCreateOrder = "{call SALES.SP_CREATE_SALES_ORDER(?, ?, ?, ?)}";
        String callAddPayment = "{call SALES.SP_ADD_PAYMENT(?, ?, ?)}";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                int newOrderId;
                try (CallableStatement csOrder = conn.prepareCall(callCreateOrder)) {
                    csOrder.setInt(1, order.getEmployeeId());
                    csOrder.setString(2, toOrderLinesJson(order.getOrderLines()));
                    if (order.getCustomerId() == null) {
                        csOrder.setNull(3, Types.INTEGER);
                    } else {
                        csOrder.setInt(3, order.getCustomerId());
                    }
                    csOrder.setBigDecimal(4, order.getDiscount());

                    try (ResultSet rs = csOrder.executeQuery()) {
                        rs.next();
                        newOrderId = rs.getInt("SALES_ORDER_ID");
                    }
                    order.setSalesOrderId(newOrderId);
                }

                if (order.getPayments() != null) {
                    try (CallableStatement csPay = conn.prepareCall(callAddPayment)) {
                        for (Payment p : order.getPayments()) {
                            csPay.setInt(1, newOrderId);
                            csPay.setString(2, p.getMethod().getDbValue());
                            csPay.setBigDecimal(3, p.getAmount());
                            csPay.execute();
                        }
                    }
                }

                conn.commit();
                return newOrderId;
            } catch (SQLException ex) {
                try { conn.rollback(); } catch (SQLException ignored) {}
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    protected SalesOrder map(ResultSet rs) throws SQLException {
        int id = rs.getInt("SALES_ORDER_ID");
        int rawCust = rs.getInt("CUSTOMER_ID");
        Integer custId = rs.wasNull() ? null : rawCust;
        int empId = rs.getInt("EMPLOYEE_ID");
        var orderDate = rs.getTimestamp("ORDER_DATE").toLocalDateTime();
        OrderStatus status = OrderStatus.fromDbValue(rs.getString("STATUS"));
        var discount = rs.getBigDecimal("DISCOUNT");
        return new SalesOrder(id, custId, empId, orderDate, status, discount);
    }

    @Override
    public List<SalesOrder> findAll() throws SQLException {
        List<SalesOrder> list = new ArrayList<>();
        String sql = "SELECT SALES_ORDER_ID, CUSTOMER_ID, EMPLOYEE_ID, ORDER_DATE, STATUS, DISCOUNT FROM SALES.SALES_ORDER";
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
    public SalesOrder findById(Integer id) throws SQLException {
        String sql = "SELECT SALES_ORDER_ID, CUSTOMER_ID, EMPLOYEE_ID, ORDER_DATE, STATUS, DISCOUNT " +
                    "FROM SALES.SALES_ORDER WHERE SALES_ORDER_ID = ?";
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
    public Integer insert(SalesOrder obj) throws SQLException {
        return save(obj);
    }

    @Override
    public boolean update(SalesOrder obj) throws SQLException {
        String sql = "UPDATE SALES.SALES_ORDER SET STATUS = ?, DISCOUNT = ? WHERE SALES_ORDER_ID = ?";
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, obj.getStatus().getDbValue());
            ps.setBigDecimal(2, obj.getDiscount());
            ps.setInt(3, obj.getSalesOrderId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(Integer id) throws SQLException {
        String sql = "DELETE FROM SALES.SALES_ORDER WHERE SALES_ORDER_ID = ?";
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}
