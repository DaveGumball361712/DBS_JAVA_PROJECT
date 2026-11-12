package model;

public class CheckDetail {
    private int stockCheckId;
    private int productId;
    private int systemQuantity;
    private int actualQuantity;

    public CheckDetail() {
    }

    public CheckDetail(int stockCheckId, int productId, int systemQuantity, int actualQuantity) {
        this.stockCheckId = stockCheckId;
        this.productId = productId;
        this.systemQuantity = systemQuantity;
        this.actualQuantity = actualQuantity;
    }

    public int getDifference() {
        return this.actualQuantity - this.systemQuantity;
    }

    public int getStockCheckId() {
        return stockCheckId;
    }

    public void setStockCheckId(int stockCheckId) {
        this.stockCheckId = stockCheckId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getSystemQuantity() {
        return systemQuantity;
    }

    public void setSystemQuantity(int systemQuantity) {
        this.systemQuantity = systemQuantity;
    }

    public int getActualQuantity() {
        return actualQuantity;
    }

    public void setActualQuantity(int actualQuantity) {
        this.actualQuantity = actualQuantity;
    }
}
