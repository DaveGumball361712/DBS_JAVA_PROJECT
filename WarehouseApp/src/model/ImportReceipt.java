package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ImportReceipt {
    private int importReceiptId;
    private int employeeId;
    private int supplierId;
    private LocalDateTime importDate;
    private String description;
    private List<ImportLine> lines = new ArrayList<>();

    public ImportReceipt() {
    }

    public ImportReceipt(int importReceiptId, int employeeId, int supplierId,
                        LocalDateTime importDate, String description) {
        this.importReceiptId = importReceiptId;
        this.employeeId = employeeId;
        this.supplierId = supplierId;
        this.importDate = importDate;
        this.description = description;
    }

    // Tính tổng tiền phiếu nhập từ danh sách dòng chi tiết
    public BigDecimal getTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (ImportLine line : lines) {
            total = total.add(line.getLineTotal());
        }
        return total;
    }

    public int getImportReceiptId() {
        return importReceiptId;
    }

    public void setImportReceiptId(int importReceiptId) {
        this.importReceiptId = importReceiptId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public int getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(int supplierId) {
        this.supplierId = supplierId;
    }

    public LocalDateTime getImportDate() {
        return importDate;
    }

    public void setImportDate(LocalDateTime importDate) {
        this.importDate = importDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ImportLine> getLines() {
        return lines;
    }

    public void setLines(List<ImportLine> lines) {
        this.lines = lines != null ? lines : new ArrayList<>();
    }
}