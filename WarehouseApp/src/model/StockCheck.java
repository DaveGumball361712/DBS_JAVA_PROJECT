package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StockCheck {
    private int stockCheckId;
    private int employeeId;
    private Integer managerId; 
    private LocalDate checkDate;
    private StockCheckStatus status;
    private LocalDateTime approveTime; 
    private List<CheckDetail> details = new ArrayList<>();

    public StockCheck() {
    }

    public StockCheck(int stockCheckId, int employeeId, Integer managerId,
                    LocalDate checkDate, StockCheckStatus status, LocalDateTime approveTime) {
        this.stockCheckId = stockCheckId;
        this.employeeId = employeeId;
        this.managerId = managerId;
        this.checkDate = checkDate;
        this.status = status;
        this.approveTime = approveTime;
    }

    public int getStockCheckId() {
        return stockCheckId;
    }

    public void setStockCheckId(int stockCheckId) {
        this.stockCheckId = stockCheckId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public Integer getManagerId() {
        return managerId;
    }

    public void setManagerId(Integer managerId) {
        this.managerId = managerId;
    }

    public LocalDate getCheckDate() {
        return checkDate;
    }

    public void setCheckDate(LocalDate checkDate) {
        this.checkDate = checkDate;
    }

    public StockCheckStatus getStatus() {
        return status;
    }

    public void setStatus(StockCheckStatus status) {
        this.status = status;
    }

    public LocalDateTime getApproveTime() {
        return approveTime;
    }

    public void setApproveTime(LocalDateTime approveTime) {
        this.approveTime = approveTime;
    }

    public List<CheckDetail> getDetails() {
        return details;
    }

    public void setDetails(List<CheckDetail> details) {
        this.details = details != null ? details : new ArrayList<>();
    }
}