package model;

import java.math.BigDecimal;

public class ImportLine {
    private int importReceiptId;
    private int productId;
    private BigDecimal importPrice;
    private int quantity;

    public ImportLine() {
    }

    public ImportLine(int importReceiptId, int productId, BigDecimal importPrice, int quantity) {
        this.importReceiptId = importReceiptId;
        this.productId = productId;
        this.importPrice = importPrice;
        this.quantity = quantity;
    }

    public BigDecimal getLineTotal() {
        if (importPrice == null) return BigDecimal.ZERO;
        return importPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public int getImportReceiptId() {
        return importReceiptId;
    }

    public void setImportReceiptId(int importReceiptId) {
        this.importReceiptId = importReceiptId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public BigDecimal getImportPrice() {
        return importPrice;
    }

    public void setImportPrice(BigDecimal importPrice) {
        this.importPrice = importPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}