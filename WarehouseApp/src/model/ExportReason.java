package model;

public enum ExportReason {
    SPOILED_OR_BROKEN("SPOIL OR BROKEN", "Hư hỏng / Vỡ"),
    EXPIRED("EXPIRED", "Hết hạn sử dụng"),
    RETURN("RETURN", "Trả hàng");

    private final String dbValue;
    private final String displayName;

    ExportReason(String dbValue, String displayName){
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public String getDbValue(){
        return dbValue;
    }

    public String getDisplayName(){
        return displayName;
    }

    public static ExportReason fromDbValue(String value){
        if (value == null) return null;
        for (ExportReason r : values()) {
            if (r.dbValue.equalsIgnoreCase(value.trim())){
                return r;
            }
        }
        throw new IllegalArgumentException("ExportReason không hợp lệ: " + value);
    }

    @Override 
    public String toString(){
        return displayName;
    }
}
