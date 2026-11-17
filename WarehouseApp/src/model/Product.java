package model;

import java.math.BigDecimal;

public abstract class Product {
    protected int productId;
    protected int categoryId;
    protected String name;
    protected BigDecimal currentPrice;
    protected ProductStatus status;
    protected int minStock;
    protected String barcode;
    protected int currentStock;
    protected String measureUnit;

    public Product() {
    }

    public Product(int productId, int categoryId, String name, BigDecimal currentPrice,
                ProductStatus status, int minStock, String barcode, int currentStock, String measureUnit) {
        this.productId = productId;
        this.categoryId = categoryId;
        this.name = name;
        this.currentPrice = currentPrice;
        this.status = status;
        this.minStock = minStock;
        this.barcode = barcode;
        this.currentStock = currentStock;
        this.measureUnit = measureUnit;
    }

    public boolean isLowStock() {
        return this.currentStock <= this.minStock;
    }

    public abstract String getProductType();

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public int getMinStock() {
        return minStock;
    }

    public void setMinStock(int minStock) {
        this.minStock = minStock;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(int currentStock) {
        this.currentStock = currentStock;
    }

    public String getMeasureUnit() {
        return measureUnit;
    }

    public void setMeasureUnit(String measureUnit) {
        this.measureUnit = measureUnit;
    }
}