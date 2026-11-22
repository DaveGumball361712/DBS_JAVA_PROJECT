package model;

public class ReturnSupplier {
    private int exportReceiptId;
    private int supplierId;

    public ReturnSupplier() {
    }

    public ReturnSupplier(int exportReceiptId, int supplierId) {
        this.exportReceiptId = exportReceiptId;
        this.supplierId = supplierId;
    }

    public int getExportReceiptId() {
        return exportReceiptId;
    }

    public void setExportReceiptId(int exportReceiptId) {
        this.exportReceiptId = exportReceiptId;
    }

    public int getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(int supplierId) {
        this.supplierId = supplierId;
    }
}