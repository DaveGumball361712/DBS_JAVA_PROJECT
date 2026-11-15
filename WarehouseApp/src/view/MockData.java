package view;

/**
 * Dữ liệu giả (Mock Data) dùng chung cho 3 panel trong WarehouseFrame.
 * Lấy mẫu từ file insertdata_warehousemanagement.sql để khi chuyển sang
 * model thật (của Đức) và DAO thật (của Phúc) thì dễ ánh xạ.
 *
 * Giai đoạn 2: Thay toàn bộ class này bằng các lời gọi DAO thật.
 */
public class MockData {

    // ===== NHÀ CUNG CẤP (WAREHOUSE.SUPPLIER) =====
    public static final String[] SUPPLIER_NAMES = {
        "Công ty TNHH Acecook Việt Nam",
        "Công ty CP Masan Consumer",
        "Tổng công ty Cổ phần Bia Sài Gòn",
        "Công ty TNHH Nestlé Việt Nam",
        "Công ty CP Sữa Vinamilk"
    };

    // ===== SẢN PHẨM (CATALOG.PRODUCT) =====
    // Mỗi sản phẩm gồm: ID, Tên, Giá bán hiện tại, Tồn kho, Đơn vị
    public static final int[]    PRODUCT_IDS     = { 1,  2,  3,  4,  5,  6,  7,  8,  9, 10};
    public static final String[] PRODUCT_NAMES   = {
        "Thùng mì Hảo Hảo tôm chua cay 30 gói",
        "Thùng mì Omachi xốt bò hầm 30 gói",
        "Mì ly Modern lẩu thái tôm 65g",
        "Phở bò Đệ Nhất gói 65g",
        "Bún tươi sấy khô Safoco 400g",
        "Miến phú hương sườn heo 55g",
        "Gạo thơm ST25 Ông Cua túi 5kg",
        "Gạo Jasmine thượng hạng túi 5kg",
        "Yến mạch cán mỏng Quaker 600g",
        "Nước mắm Nam Ngư Phú Quốc 750ml"
    };
    public static final double[] PRODUCT_PRICES  = {
        33500, 42000, 50500, 59000, 67500, 76000, 84500, 93000, 101500, 110000
    };
    public static final int[]    PRODUCT_STOCKS  = {
        53, 56, 59, 62, 65, 68, 71, 74, 77, 80
    };
    public static final String[] PRODUCT_UNITS   = {
        "Thùng", "Gói", "Chai", "Lốc", "Hộp", "Túi", "Thùng", "Gói", "Chai", "Lốc"
    };

    /**
     * Tìm index của sản phẩm theo tên hiển thị.
     * Trả về -1 nếu không tìm thấy.
     */
    public static int findProductIndex(String productName) {
        for (int i = 0; i < PRODUCT_NAMES.length; i++) {
            if (PRODUCT_NAMES[i].equals(productName)) return i;
        }
        return -1;
    }
}
