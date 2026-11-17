package model;

public enum StockCheckStatus {
    WAITING_FOR_APPROVAL("WAITING FOR APPROVAL", "Chờ duyệt"),
    APPROVED("APPROVED", "Đã duyệt");

    private final String dbValue;
    private final String displayName;

    StockCheckStatus(String dbValue, String displayName){
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public String getDbValue(){
        return dbValue;
    }

    public String getDisplayName(){
        return displayName;
    }

    public static StockCheckStatus fromDbValue(String value){
        if (value == null) return null;
        for (StockCheckStatus s : values()) {
            if (s.dbValue.equalsIgnoreCase(value.trim())){
                return s;
            }
        }
        throw new IllegalArgumentException("StockCheckStatus không hợp lệ: " + value);
    }

    @Override 
    public String toString(){
        return displayName;
    }
}
