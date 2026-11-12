package model;

import java.math.BigDecimal;

public class ProductSupply {
    private int productId;
    private int supplierId;
    private BigDecimal quotedPrice;

    public ProductSupply() {
    }

    public ProductSupply(int productId, int supplierId, BigDecimal quotedPrice) {
        this.productId = productId;
        this.supplierId = supplierId;
        this.quotedPrice = quotedPrice;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(int supplierId) {
        this.supplierId = supplierId;
    }

    public BigDecimal getQuotedPrice() {
        return quotedPrice;
    }

    public void setQuotedPrice(BigDecimal quotedPrice) {
        this.quotedPrice = quotedPrice;
    }
}