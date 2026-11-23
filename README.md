# Quản lý kho & Bán hàng

Đồ án môn **Cơ sở dữ liệu** và **Lập trình hướng đối tượng** (PTIT). Ứng dụng Java Swing kết nối SQL Server, quản lý kho hàng (nhập, xuất, kiểm kê, duyệt phiếu) và bán hàng tại quầy cho một cửa hàng bán lẻ.

## Thành viên

| Thành viên | Phần phụ trách |
|---|---|
| Lưu Thanh Phúc | DAO kho (nhà cung cấp, phiếu nhập, xuất, kiểm kê), ReportDAO, màn hình Quản lý (6 tab), tích hợp và quản lý repo |
| Đức | Model, BaseDAO, DAO nhân sự / sản phẩm / bán hàng, màn hình Đăng nhập, Session, ErrorHandler, dữ liệu mẫu |
| Đạt | SQL bán hàng (view, trigger, procedure, báo cáo), màn hình Thu ngân |
| Bảo | SQL kho (view, trigger, procedure duyệt phiếu), màn hình Kho |

## Yêu cầu

- **JDK 17** trở lên (code dùng pattern matching `instanceof` của Java 16+)
- **SQL Server** 2019 / 2022 (bản Express hoặc Developer đều được) và **SSMS**
- **VS Code** với *Extension Pack for Java*
- Driver JDBC có sẵn trong `WarehouseApp/lib/mssql-jdbc-13.6.0.jre11.jar`

## Cài đặt database

Mở SSMS, đăng nhập bằng **SQL Server Authentication**, rồi chạy lần lượt các file trong thư mục `database/` (mỗi file bấm F5):

| # | File | Nội dung |
|---|---|---|
| 1 | `SQLWarehouseManagement.sql` | Tạo database `WAREHOUSEMANAGEMENT`, 4 schema (HR, CATALOG, WAREHOUSE, SALES) và 24 bảng |
| 2 | `insertdata_warehousemanagement.sql` | Dữ liệu mẫu: 150 nhân viên, sản phẩm, nhà cung cấp, phiếu, đơn hàng |
| 3 | `ADVANCED_WAREHOUSE.sql` | View, trigger, procedure phần kho |
| 4 | `ADVANCED_SALES.sql` | View, trigger, procedure phần bán hàng và báo cáo |
| 5 | `TEST_WAREHOUSE.sql`, `TEST_SALES.sql` | (Không bắt buộc) Kiểm tra trigger và procedure; các phép thử tự rollback |

Muốn dựng lại từ đầu thì tắt ứng dụng Java rồi xóa database cũ trước:

```sql
USE master;
ALTER DATABASE WAREHOUSEMANAGEMENT SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
DROP DATABASE WAREHOUSEMANAGEMENT;
```

## Cấu hình kết nối

File `WarehouseApp/config/db.properties`:

```properties
db.url=jdbc:sqlserver://localhost:1433;databaseName=WAREHOUSEMANAGEMENT;encrypt=true;trustServerCertificate=true
db.user=sa
db.password=<mật khẩu tài khoản sa trên máy bạn>
```

Nếu máy bạn dùng mật khẩu khác bản chung của nhóm, sửa file này rồi chạy lệnh sau để Git không commit thay đổi đó:

```
git update-index --skip-worktree WarehouseApp/config/db.properties
```

## Chạy ứng dụng

1. Mở **thư mục gốc của repo** trong VS Code (không mở riêng thư mục `WarehouseApp`).
2. Chọn cấu hình **Run App** ở tab Run and Debug, hoặc mở `WarehouseApp/src/App.java` rồi bấm **Run** trên dòng `main`.
3. Màn hình đăng nhập hiện ra; đăng nhập theo vai trò để vào đúng màn hình.

### Tài khoản mẫu

| Vai trò | Username | Mật khẩu | Màn hình |
|---|---|---|---|
| Quản lý | `mgr001` | `Pass@001#2026` | Quản lý |
| Thu ngân | `csh051` | `Pass@051#2026` | Bán hàng |
| Nhân viên kho | `whs101` | `Pass@101#2026` | Kho |

Mọi tài khoản mẫu theo quy luật: mật khẩu là `Pass@` + 3 số cuối của username + `#2026` (quản lý `mgr001`–`mgr050`, thu ngân `csh051`–`csh100`, nhân viên kho `whs101`–`whs150`).

## Chức năng theo vai trò

**Quản lý**
- Nhân viên: lọc theo vai trò, thêm / sửa / xóa quản lý, thu ngân, nhân viên kho
- Sản phẩm: tìm kiếm, lọc thực phẩm / điện tử, hàng sắp hết; thêm / sửa / xóa
- Loại hàng: cây danh mục cha-con (JTree), sản phẩm theo loại
- Nhà cung cấp: tìm theo tên / mã số thuế, nhiều số điện thoại
- Duyệt phiếu: duyệt nhiều phiếu xuất và phiếu kiểm kê cùng lúc
- Báo cáo: doanh thu theo ngày, theo thu ngân, sản phẩm bán chạy, hàng sắp hết, nhập hàng theo nhà cung cấp

**Nhân viên kho**
- Lập phiếu nhập (tồn kho tự cộng), phiếu xuất chờ duyệt (hết hạn, hư hỏng, trả nhà cung cấp), phiếu kiểm kê
- Xem lịch sử nhập và danh sách hàng sắp hết

**Thu ngân**
- Quét mã vạch, kiểm tra tồn kho, giảm giá, tìm khách theo số điện thoại
- Thanh toán một hoặc nhiều lần (tiền mặt, chuyển khoản, thẻ)

## Cấu trúc thư mục

```
database/           Script SQL: tạo bảng, dữ liệu mẫu, view/trigger/procedure, test
docs/               Đề bài, ERD / EERD (draw.io)
WarehouseApp/
  config/           db.properties
  lib/              Driver JDBC SQL Server
  src/
    App.java        Điểm khởi động (mở màn hình đăng nhập)
    model/          Lớp dữ liệu: Employee (Manager, Cashier, WarehouseStaff), Product (Foods, Electronics), phiếu, đơn hàng...
    dao/            Truy cập database: BaseDAO<T, K> và các DAO kế thừa
    view/           Giao diện Swing: LoginFrame, ManagerFrame, WarehouseFrame, CashierFrame và các panel
    util/           DBConnection, Session, ErrorHandler
```

## Lỗi thường gặp

| Lỗi | Cách xử lý |
|---|---|
| `Khong doc duoc file .../db.properties` | Mở đúng thư mục gốc của repo trong VS Code; chạy bằng cấu hình **Run App** |
| `No suitable driver` | Kiểm tra `.vscode/settings.json` có `"java.project.referencedLibraries": ["WarehouseApp/lib/**/*.jar"]` |
| `Connection refused` / `connect timed out` | Bật TCP/IP cổng 1433 trong SQL Server Configuration Manager rồi khởi động lại dịch vụ SQL Server |
| `Login failed for user 'sa'` | Bật chế độ đăng nhập *SQL Server and Windows Authentication* và đặt mật khẩu cho `sa` |
| `Invalid object name` / `Could not find stored procedure` | Chưa chạy đủ các file SQL ở mục Cài đặt database |
