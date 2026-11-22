package view;

import model.Electronics;
import model.Foods;
import model.Product;
import model.ProductStatus;
import model.Supplier;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

// Du lieu gia dung tam truoc khi noi DAO
public class MockData {

    private static final List<Supplier> SUPPLIERS = Arrays.asList(
        new Supplier(1, "Công ty TNHH Acecook Việt Nam", "contact@acecook.vn", "KCN Tân Bình, TP.HCM", "0300808687"),
        new Supplier(2, "Công ty CP Masan Consumer", "support@masan.vn", "Quận 1, TP.HCM", "0302017440"),
        new Supplier(3, "Tổng công ty Cổ phần Bia Sài Gòn", "info@sabeco.com.vn", "Quận 5, TP.HCM", "0300583659"),
        new Supplier(4, "Công ty TNHH Nestlé Việt Nam", "cs@nestle.vn", "Biên Hòa, Đồng Nai", "3600235305"),
        new Supplier(5, "Công ty CP Sữa Vinamilk", "vinamilk@vinamilk.com.vn", "Quận 7, TP.HCM", "0300588569")
    );

    private static final List<Product> PRODUCTS = Arrays.asList(
        new Foods(1, 11, "Thùng mì Hảo Hảo tôm chua cay 30 gói", new BigDecimal("33500"),
                ProductStatus.ON_SALE, 20, "893456700001", 53, "Thùng", "Nơi khô ráo", 180),
        new Foods(2, 11, "Thùng mì Omachi xốt bò hầm 30 gói", new BigDecimal("42000"),
                ProductStatus.ON_SALE, 20, "893456700002", 56, "Thùng", "Nơi khô ráo", 180),
        new Foods(3, 11, "Mì ly Modern lẩu thái tôm 65g", new BigDecimal("50500"),
                ProductStatus.ON_SALE, 20, "893456700003", 59, "Lốc", "Nơi khô ráo", 180),
        new Foods(4, 12, "Phở bò Đệ Nhất gói 65g", new BigDecimal("59000"),
                ProductStatus.ON_SALE, 20, "893456700004", 62, "Lốc", "Nơi khô ráo", 180),
        new Foods(5, 12, "Bún tươi sấy khô Safoco 400g", new BigDecimal("67500"),
                ProductStatus.ON_SALE, 20, "893456700005", 65, "Gói", "Nơi khô ráo", 365),
        new Foods(6, 13, "Gạo thơm ST25 Ông Cua túi 5kg", new BigDecimal("84500"),
                ProductStatus.ON_SALE, 20, "893456700007", 71, "Túi", "Nơi thoáng mát", 180),
        new Foods(7, 14, "Nước mắm Nam Ngư Phú Quốc 750ml", new BigDecimal("110000"),
                ProductStatus.ON_SALE, 20, "893456700010", 80, "Chai", "Tránh ánh nắng", 365),
        new Foods(8, 20, "Sữa tươi Vinamilk 100% lốc 4 hộp", new BigDecimal("32000"),
                ProductStatus.ON_SALE, 25, "893456700028", 134, "Lốc", "Bảo quản lạnh", 90),
        new Electronics(9, 31, "Nồi cơm điện Sharp 1.8 lít KS-18TJV", new BigDecimal("315000"),
                ProductStatus.ON_SALE, 10, "893890100051", 27, "Cái", 12),
        new Electronics(10, 32, "Ấm đun siêu tốc Sunhouse 1.8 lít", new BigDecimal("510000"),
                ProductStatus.ON_SALE, 10, "893890100054", 33, "Cái", 12)
    );

    public static List<Supplier> getSuppliers() {
        return SUPPLIERS;
    }

    public static List<Product> getProducts() {
        return PRODUCTS;
    }

    public static Product findProductByName(String name) {
        for (Product p : PRODUCTS) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }
}
