import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import util.DBConnection;

public class App {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM sys.tables")) {

            rs.next();
            System.out.println("Ket noi thanh cong! So bang: " + rs.getInt(1));

        } catch (Exception e) {
            System.out.println("Ket noi that bai: " + e.getMessage());
        }
    }
}