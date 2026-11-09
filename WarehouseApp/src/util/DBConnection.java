package util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {

    // Tìm file cấu hình ở cả hai vị trí, vì VS Code có thể chạy từ thư mục repo hoặc từ WarehouseApp
    private static final String[] CONFIG_PATHS = {
        "WarehouseApp/config/db.properties",
        "config/db.properties"
    };

    private static String url;
    private static String user;
    private static String password;

    // Khối static: chạy đúng 1 lần khi class được dùng lần đầu, để đọc file cấu hình
    static {
        Properties props = new Properties();
        boolean loaded = false;
        for (String p : CONFIG_PATHS) {
            if (Files.exists(Path.of(p))) {
                try (InputStream in = new FileInputStream(p)) {
                    props.load(in);
                    loaded = true;
                    break;
                } catch (IOException e) {
                    throw new RuntimeException("Khong doc duoc file " + p, e);
                }
            }
        }
        if (!loaded) {
            throw new RuntimeException("Khong tim thay config/db.properties. Hay copy tu db.properties.example");
        }
        url = props.getProperty("db.url");
        user = props.getProperty("db.user");
        password = props.getProperty("db.password");
    }

    // Constructor private: không ai cần tạo đối tượng DBConnection, chỉ gọi hàm static
    private DBConnection() {
    }

    // Mỗi lần gọi trả về một kết nối mới; người dùng tự đóng bằng try-with-resources
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}