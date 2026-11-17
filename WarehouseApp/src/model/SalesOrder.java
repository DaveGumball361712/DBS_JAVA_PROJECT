package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SalesOrder {
    private int salesOrderId;
    private Integer customerId; // Cho phép NULL nếu là khách vãng lai
    private int employeeId;
    private LocalDateTime orderDate;
    private OrderStatus status;
    private BigDecimal discount = BigDecimal.ZERO;
    private List<OrderLine> orderLines = new ArrayList<>();
    private List<Payment> payments = new ArrayList<>();

    public SalesOrder() {
    }

    public SalesOrder(int salesOrderId, Integer customerId, int employeeId,
                      LocalDateTime orderDate, OrderStatus status, BigDecimal discount) {
        this.salesOrderId = salesOrderId;
        this.customerId = customerId;
        this.employeeId = employeeId;
        this.orderDate = orderDate;
        this.status = status;
        this.discount = discount != null ? discount : BigDecimal.ZERO;
    }

    // Tổng tiền đơn hàng sau khi trừ giảm giá (không nhỏ hơn 0)
    public BigDecimal getTotal() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderLine line : orderLines) {
            subtotal = subtotal.add(line.getLineTotal());
        }
        BigDecimal disc = discount != null ? discount : BigDecimal.ZERO;
        BigDecimal result = subtotal.subtract(disc);
        return result.compareTo(BigDecimal.ZERO) > 0 ? result : BigDecimal.ZERO;
    }

    // Tổng số tiền khách đã thanh toán qua các lần trả
    public BigDecimal getPaid() {
        BigDecimal paid = BigDecimal.ZERO;
        for (Payment p : payments) {
            if (p.getAmount() != null) {
                paid = paid.add(p.getAmount());
            }
        }
        return paid;
    }

    // Số tiền còn nợ/chưa thanh toán
    public BigDecimal getRemaining() {
        BigDecimal rem = getTotal().subtract(getPaid());
        return rem.compareTo(BigDecimal.ZERO) > 0 ? rem : BigDecimal.ZERO;
    }

    public int getSalesOrderId() {
        return salesOrderId;
    }

    public void setSalesOrderId(int salesOrderId) {
        this.salesOrderId = salesOrderId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public List<OrderLine> getOrderLines() {
        return orderLines;
    }

    public void setOrderLines(List<OrderLine> orderLines) {
        this.orderLines = orderLines != null ? orderLines : new ArrayList<>();
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments != null ? payments : new ArrayList<>();
    }
}