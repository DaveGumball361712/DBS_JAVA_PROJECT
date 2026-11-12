package model;

public class ExportLine {
    private int exportReceiptId;
    private int productId;
    private int quantity;

    public ExportLine() {
    }

    public ExportLine(int exportReceiptId, int productId, int quantity) {
        this.exportReceiptId = exportReceiptId;
        this.productId = productId;
        this.quantity = quantity;
    }

    public int getExportReceiptId() {
        return exportReceiptId;
    }

    public void setExportReceiptId(int exportReceiptId) {
        this.exportReceiptId = exportReceiptId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}