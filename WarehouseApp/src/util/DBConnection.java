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

    private static String url;
    private static String user;
    private static String password;

    // Khối static: chạy đúng 1 lần khi class được dùng lần đầu, để đọc file cấu hình
    static {
        Properties props = new Properties();
        Path config = findConfig();
        try (InputStream in = new FileInputStream(config.toFile())) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Khong doc duoc file " + config, e);
        }
        url = props.getProperty("db.url");
        user = props.getProperty("db.user");
        password = props.getProperty("db.password");
    }

    // Tìm db.properties theo vị trí thư mục bin (WarehouseApp/bin -> WarehouseApp/config),
    // nên chạy từ thư mục nào cũng tìm được
    private static Path findConfig() {
        try {
            Path bin = Path.of(DBConnection.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path[] candidates = {
                bin.getParent().resolve("config/db.properties"),
                Path.of("WarehouseApp/config/db.properties"),
                Path.of("config/db.properties")
            };
            for (Path c : candidates) {
                if (Files.exists(c)) {
                    return c;
                }
            }
        } catch (Exception e) {
            // bỏ qua, báo lỗi bên dưới
        }
        throw new RuntimeException("Khong tim thay WarehouseApp/config/db.properties. Hay git pull de lay file cau hinh");
    }

    // Constructor private: không ai cần tạo đối tượng DBConnection, chỉ gọi hàm static
    private DBConnection() {
    }

    // Mỗi lần gọi trả về một kết nối mới; người dùng tự đóng bằng try-with-resources
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}