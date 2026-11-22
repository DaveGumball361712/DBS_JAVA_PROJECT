package model;

public enum OrderStatus {
    NOT_PAID("NOT PAID", "Chưa thanh toán"),
    PAID("PAID", "Đã thanh toán"),
    CANCELED("CANCELED", "Đã hủy");

    private final String dbValue;
    private final String displayName;

    OrderStatus(String dbValue, String displayName){
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public String getDbValue(){
        return dbValue;
    }

    public String getDisplayName(){
        return displayName;
    }

    public static OrderStatus fromDbValue(String value){
        if (value == null) return null;
        for (OrderStatus o : values()) {
            if (o.dbValue.equalsIgnoreCase(value.trim())){
                return o;
            }
        }
        throw new IllegalArgumentException("OrderStatus không hợp lệ: " + value);
    }

    @Override 
    public String toString(){
        return displayName;
    }
}
