public class App {
    public static void main(String[] args) {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            System.out.println("Da tim thay JDBC driver!");
        } catch (ClassNotFoundException e) {
            System.out.println("Chua co JDBC driver trong lib!");
        }
    }
}
