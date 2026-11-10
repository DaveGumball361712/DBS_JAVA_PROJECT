package model;

import java.math.BigDecimal;

public class Electronics extends Product {
    private int monthsOfWarranty;

    public Electronics() {
        super();
    }

    public Electronics(int productId, int categoryId, String name, BigDecimal currentPrice,
                    ProductStatus status, int minStock, String barcode, int currentStock,
                    String measureUnit, int monthsOfWarranty) {
        super(productId, categoryId, name, currentPrice, status, minStock, barcode, currentStock, measureUnit);
        this.monthsOfWarranty = monthsOfWarranty;
    }

    @Override
    public String getProductType() {
        return "ELECTRONICS";
    }

    public int getMonthsOfWarranty() {
        return monthsOfWarranty;
    }

    public void setMonthsOfWarranty(int monthsOfWarranty) {
        this.monthsOfWarranty = monthsOfWarranty;
    }
}