package model;

public enum ProductStatus {
    ON_SALE("ON SALE", "Đang bán"),
    OFF_SALE("OFF SALE", "Ngưng bán");

    private final String dbValue;
    private final String displayName;

    ProductStatus(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public String getDbValue(){
        return dbValue;
    }

    public String getDisplayName(){
        return displayName;
    }

    public static ProductStatus fromDbValue(String value){
        if (value == null) return null;
        for (ProductStatus s : values()) {
            if (s.dbValue.equalsIgnoreCase(value.trim())){
                return s;
            }
        }
        throw new IllegalArgumentException("ProductStatus Không hợp lệ: " + value);
    }

    @Override 
    public String toString(){
        return displayName;
    }
}
