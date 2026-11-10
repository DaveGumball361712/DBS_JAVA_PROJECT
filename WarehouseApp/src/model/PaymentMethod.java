package model;

public enum PaymentMethod {
    IN_CASH("IN CASH", "Tiền mặt"),
    ONLINE_BANKING("ONELINE BANKING", "Chuyển khoản"),
    BY_CARD("BY CARD", "Thẻ ngân hàng");

    private final String dbValue;
    private final String displayName;

    PaymentMethod(String dbValue, String displayName){
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public String getDbValue(){
        return dbValue;
    }

    public String getDisplayName(){
        return displayName;
    }

    public static PaymentMethod fromDbValue(String value){
        if (value == null) return null;
        for (PaymentMethod p : values()) {
            if (p.dbValue.equalsIgnoreCase(value.trim())){
                return p;
            }
        }
        throw new IllegalArgumentException("PaymentMethod không hợp lệ: " + value);
    }

    @Override 
    public String toString(){
        return displayName;
    }
}
