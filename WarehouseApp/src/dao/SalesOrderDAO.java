package dao;

import model.OrderLine;
import model.OrderStatus;
import model.Payment;
import model.SalesOrder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SalesOrderDAO extends BaseDAO<SalesOrder, Integer> {

// Chuyển List<OrderLine> thành chuỗi JSON cho OPENJSON trong SP_CREATE_SALES_ORDER 
    private String toOrderLinesJson(List<OrderLine> lines) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < lines.size(); i++) {
            OrderLine line = lines.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"PRODUCT_ID\":").append(line.getProductId())
            .append(",\"QUANTITY\":").append(line.getQuantity())
            .append(",\"UNIT_PRICE\":").append(line.getUnitPrice().toPlainString())
            .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    public int save(SalesOrder order) throws SQLException {
        String callCreateOrder = "{call SALES.SP_CREATE_SALES_ORDER(?, ?, ?, ?, ?)}";
        String callAddPayment = "{call SALES.SP_ADD_PAYMENT(?, ?, ?)}";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                int newOrderId;
                // Gọi SP_CREATE_SALES_ORDER
                try (CallableStatement csOrder = conn.prepareCall(callCreateOrder)) {
                    if (order.getCustomerId() == null) {
                        csOrder.setNull(1, Types.INTEGER);
                    } else {
                        csOrder.setInt(1, order.getCustomerId());
                    }
                    csOrder.setInt(2, order.getEmployeeId());
                    csOrder.setBigDecimal(3, order.getDiscount());
                    csOrder.setString(4, toOrderLinesJson(order.getOrderLines()));
                    csOrder.registerOutParameter(5, Types.INTEGER); // Tham số OUTPUT @NewOrderID
                    csOrder.execute();

                    newOrderId = csOrder.getInt(5);
                    order.setSalesOrderId(newOrderId);
                }

                // Gọi SP_ADD_PAYMENT cho từng lần thanh toán
                if (order.getPayments() != null && !order.getPayments().isEmpty()) {
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
                conn.rollback();
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
