package model;

import java.math.BigDecimal;

public class Foods extends Product {
    private String preserveDescription;
    private int dayOfExpire;

    public Foods() {
        super();
    }

    public Foods(int productId, int categoryId, String name, BigDecimal currentPrice,
                ProductStatus status, int minStock, String barcode, int currentStock,
                String measureUnit, String preserveDescription, int dayOfExpire) {
        super(productId, categoryId, name, currentPrice, status, minStock, barcode, currentStock, measureUnit);
        this.preserveDescription = preserveDescription;
        this.dayOfExpire = dayOfExpire;
    }

    @Override
    public String getProductType() {
        return "FOODS";
    }

    public String getPreserveDescription() {
        return preserveDescription;
    }

    public void setPreserveDescription(String preserveDescription) {
        this.preserveDescription = preserveDescription;
    }

    public int getDayOfExpire() {
        return dayOfExpire;
    }

    public void setDayOfExpire(int dayOfExpire) {
        this.dayOfExpire = dayOfExpire;
    }
}
