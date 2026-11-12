package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ExportReceipt {
    private int exportReceiptId;
    private int employeeId;
    private Integer managerId; 
    private LocalDate exportDate;
    private ExportReason reason;
    private LocalDateTime approveTime; 
    private List<ExportLine> lines = new ArrayList<>();

    public ExportReceipt() {
    }

    public ExportReceipt(int exportReceiptId, int employeeId, Integer managerId,
                        LocalDate exportDate, ExportReason reason, LocalDateTime approveTime) {
        this.exportReceiptId = exportReceiptId;
        this.employeeId = employeeId;
        this.managerId = managerId;
        this.exportDate = exportDate;
        this.reason = reason;
        this.approveTime = approveTime;
    }

    // Kiểm tra phiếu xuất đã được quản lý duyệt chưa
    public boolean isApproved() {
        return this.managerId != null && this.approveTime != null;
    }

    public int getExportReceiptId() {
        return exportReceiptId;
    }

    public void setExportReceiptId(int exportReceiptId) {
        this.exportReceiptId = exportReceiptId;
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

    public LocalDate getExportDate() {
        return exportDate;
    }

    public void setExportDate(LocalDate exportDate) {
        this.exportDate = exportDate;
    }

    public ExportReason getReason() {
        return reason;
    }

    public void setReason(ExportReason reason) {
        this.reason = reason;
    }

    public LocalDateTime getApproveTime() {
        return approveTime;
    }

    public void setApproveTime(LocalDateTime approveTime) {
        this.approveTime = approveTime;
    }

    public List<ExportLine> getLines() {
        return lines;
    }

    public void setLines(List<ExportLine> lines) {
        this.lines = lines != null ? lines : new ArrayList<>();
    }
}