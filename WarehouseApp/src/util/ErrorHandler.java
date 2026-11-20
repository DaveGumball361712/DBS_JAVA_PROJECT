package util;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class ErrorHandler {

    public static void showError(Component parent, Exception ex) {
        String message;
        if (ex instanceof SQLException sqlEx) {
            message = translateSqlException(sqlEx);
        } else {
            message = ex.getMessage() != null ? ex.getMessage() : "Đã xảy ra lỗi không xác định.";
        }
        JOptionPane.showMessageDialog(parent, message, "Thông báo lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public static String translateSqlException(SQLException ex) {
        int code = ex.getErrorCode();
        String rawMsg = ex.getMessage() != null ? ex.getMessage() : "";

        // Mã 50000+: Lỗi nghiệp vụ từ Trigger / Stored Procedure 
        if (code >= 50000) {
            return "Lỗi nghiệp vụ: " + rawMsg;
        }
        // Mã 2627 hoặc 2601: Vi phạm ràng buộc UNIQUE hoặc PRIMARY KEY
        if (code == 2627 || code == 2601) {
            if (rawMsg.contains("UQ_PRODUCT_BARCODE")) {
                return "Mã vạch (Barcode) sản phẩm này đã tồn tại!";
            }
            if (rawMsg.contains("UQ_CUSTOMER_PHONENUMBER")) {
                return "Số điện thoại khách hàng này đã được đăng ký!";
            }
            if (rawMsg.contains("UQ_EMPLOYEE_USERNAME") || rawMsg.contains("PK_ACCOUNT")) {
                return "Tên đăng nhập (Username) này đã tồn tại!";
            }
            if (rawMsg.contains("UQ_SUPPLIER_TAXCODE")) {
                return "Mã số thuế nhà cung cấp này đã tồn tại!";
            }
            return "Dữ liệu bị trùng lặp (vi phạm ràng buộc duy nhất): " + rawMsg;
        }
        // Mã 547: Vi phạm khóa ngoại (FOREIGN KEY) hoặc ràng buộc kiểm tra (CHECK)
        if (code == 547) {
            if (rawMsg.contains("DELETE")) {
                return "Không thể xóa bản ghi này vì đang có dữ liệu phát sinh liên quan!";
            }
            return "Dữ liệu nhập vào không hợp lệ với ràng buộc hệ thống: " + rawMsg;
        }
        return "Lỗi cơ sở dữ liệu (Mã " + code + "): " + rawMsg;
    }
}
