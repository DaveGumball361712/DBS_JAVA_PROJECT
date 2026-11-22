package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Payment {
    private int salesOrderId;
    private int paymentNumber;
    private PaymentMethod method;
    private BigDecimal amount;
    private LocalDateTime paidTime;

    public Payment() {
    }

    public Payment(int salesOrderId, int paymentNumber, PaymentMethod method,
                BigDecimal amount, LocalDateTime paidTime) {
        this.salesOrderId = salesOrderId;
        this.paymentNumber = paymentNumber;
        this.method = method;
        this.amount = amount;
        this.paidTime = paidTime;
    }

    public int getSalesOrderId() {
        return salesOrderId;
    }

    public void setSalesOrderId(int salesOrderId) {
        this.salesOrderId = salesOrderId;
    }

    public int getPaymentNumber() {
        return paymentNumber;
    }

    public void setPaymentNumber(int paymentNumber) {
        this.paymentNumber = paymentNumber;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getPaidTime() {
        return paidTime;
    }

    public void setPaidTime(LocalDateTime paidTime) {
        this.paidTime = paidTime;
    }
}
