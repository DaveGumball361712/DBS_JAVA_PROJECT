-- =====================================================================
-- File:    ADVANCED_WAREHOUSE.sql
-- Mô tả:   3 View + 3 Trigger + 2 Stored Procedure cho phân hệ Kho
-- =====================================================================

USE WAREHOUSEMANAGEMENT;
GO

-- =============================================================
-- PHẦN 1: VIEWS (3 View)
-- =============================================================

-- -------------------------------------------------------------
-- VIEW 1: V_IMPORT_RECEIPT_TOTAL
-- Mục đích: Tổng tiền từng phiếu nhập = SUM(Số lượng × Giá nhập)
--           kèm thông tin nhà cung cấp và nhân viên lập phiếu.
-- Giải quyết: Thuộc tính dẫn xuất "Tổng tiền phiếu nhập" (R12)
--             không lưu cứng trong bảng, tính ra mỗi lần truy vấn.
-- Cách dùng: SELECT * FROM WAREHOUSE.V_IMPORT_RECEIPT_TOTAL
-- -------------------------------------------------------------
CREATE OR ALTER VIEW WAREHOUSE.V_IMPORT_RECEIPT_TOTAL
AS
SELECT 
    ir.IMPORT_RECEIPT_ID,
    ir.IMPORT_DATE,
    ir.EMPLOYEE_ID,
    e.FIRST_NAME + N' ' + e.LAST_NAME  AS EMPLOYEE_NAME,
    ir.SUPPLIER_ID,
    s.NAME                              AS SUPPLIER_NAME,
    ir.DESCRIPTION,
    ISNULL(SUM(inc.QUANTITY * inc.IMPORT_PRICE), 0) AS TOTAL_AMOUNT
FROM WAREHOUSE.IMPORT_RECEIPT ir
    JOIN HR.EMPLOYEE e
        ON ir.EMPLOYEE_ID = e.EMPLOYEE_ID
    JOIN WAREHOUSE.SUPPLIER s 
        ON ir.SUPPLIER_ID = s.SUPPLIER_ID
    LEFT JOIN WAREHOUSE.INCLUDES inc 
        ON ir.IMPORT_RECEIPT_ID = inc.IMPORT_RECEIPT_ID
GROUP BY 
    ir.IMPORT_RECEIPT_ID,
    ir.IMPORT_DATE,
    ir.EMPLOYEE_ID,
    e.FIRST_NAME,
    e.LAST_NAME,
    ir.SUPPLIER_ID,
    s.NAME,
    ir.DESCRIPTION;
GO

PRINT N'✓ Tạo VIEW WAREHOUSE.V_IMPORT_RECEIPT_TOTAL thành công';
GO


-- -------------------------------------------------------------
-- VIEW 2: V_CHECK_DIFFERENCE
-- Mục đích: Chi tiết từng dòng kiểm kê, kèm tên sản phẩm
--           và cột chênh lệch = Số thực tế − Số hệ thống.
-- Giải quyết: Thuộc tính dẫn xuất "Chênh lệch kiểm kê" (R16)
--    Chênh lệch < 0: thiếu hàng (mất mát/hao hụt)
--    Chênh lệch > 0: thừa hàng
--    Chênh lệch = 0: khớp kho
-- Cách dùng: SELECT * FROM WAREHOUSE.V_CHECK_DIFFERENCE
--            WHERE STOCK_CHECK_ID = 1
-- -------------------------------------------------------------
CREATE OR ALTER VIEW WAREHOUSE.V_CHECK_DIFFERENCE
AS
SELECT 
    cd.STOCK_CHECK_ID,
    sc.CHECK_DATE,
    sc.STATUS                                       AS CHECK_STATUS,
    cd.PRODUCT_ID,
    p.BARCODE,
    p.NAME                                          AS PRODUCT_NAME,
    p.MEASURE_UNIT,
    cd.SYSTEM_QUANTITY,
    cd.ACTUAL_QUANTITY,
    (cd.ACTUAL_QUANTITY - cd.SYSTEM_QUANTITY)        AS DIFFERENCE_QUANTITY
FROM WAREHOUSE.CHECK_DETAIL cd
    JOIN WAREHOUSE.STOCK_CHECK sc
        ON cd.STOCK_CHECK_ID = sc.STOCK_CHECK_ID
    JOIN CATALOG.PRODUCT p 
        ON cd.PRODUCT_ID = p.PRODUCT_ID;
GO

PRINT N'✓ Tạo VIEW WAREHOUSE.V_CHECK_DIFFERENCE thành công';
GO


-- -------------------------------------------------------------
-- VIEW 3: V_LOW_STOCK
-- Mục đích: Cảnh báo sản phẩm đang bán (ON SALE) mà
--           tồn kho hiện tại < mức tồn tối thiểu.
--           Thủ kho nhìn view này để biết cần nhập thêm hàng.
-- Cách dùng: SELECT * FROM CATALOG.V_LOW_STOCK
--            ORDER BY SHORTAGE_QUANTITY DESC
-- -------------------------------------------------------------
CREATE OR ALTER VIEW CATALOG.V_LOW_STOCK
AS
SELECT 
    p.PRODUCT_ID,
    p.BARCODE,
    p.NAME                                  AS PRODUCT_NAME,
    p.CATEGORY_ID,
    c.NAME                                  AS CATEGORY_NAME,
    p.MEASURE_UNIT,
    p.CURRENT_PRICE,
    p.CURRENT_STOCK,
    p.MIN_STOCK,
    (p.MIN_STOCK - p.CURRENT_STOCK)         AS SHORTAGE_QUANTITY
FROM CATALOG.PRODUCT p
    JOIN CATALOG.CATEGORY c 
        ON p.CATEGORY_ID = c.CATEGORY_ID
WHERE p.STATUS = N'ON SALE' 
  AND p.CURRENT_STOCK < p.MIN_STOCK;
GO

PRINT N'✓ Tạo VIEW CATALOG.V_LOW_STOCK thành công';
GO


-- =============================================================
-- PHẦN 2: TRIGGERS (3 Trigger)
-- =============================================================
-- LƯU Ý QUAN TRỌNG:
--   - Tất cả trigger đều xử lý MULTI-ROW (nhiều dòng INSERT/UPDATE
--     cùng lúc) bằng cách JOIN với bảng inserted/deleted rồi
--     GROUP BY, KHÔNG dùng biến đơn lẻ kiểu:
--       DECLARE @id INT = (SELECT ... FROM inserted)  ← SAI!
--   - Bảng 'inserted': chứa dòng MỚI (sau INSERT hoặc sau UPDATE).
--   - Bảng 'deleted':  chứa dòng CŨ (trước UPDATE hoặc bị DELETE).
-- =============================================================

-- -------------------------------------------------------------
-- TRIGGER 1: TRG_INCLUDES_ADD_STOCK
-- Bảng gắn: WAREHOUSE.INCLUDES
-- Sự kiện:  AFTER INSERT
-- Mục đích: Khi thêm dòng hàng vào phiếu nhập, tự động CỘNG
--           số lượng nhập vào tồn kho (CURRENT_STOCK) của sản phẩm.
-- Ví dụ:   INSERT 100 thùng mì Hảo Hảo vào phiếu nhập
--           → CURRENT_STOCK của mì Hảo Hảo tự động +100
-- -------------------------------------------------------------
CREATE OR ALTER TRIGGER WAREHOUSE.TRG_INCLUDES_ADD_STOCK
ON WAREHOUSE.INCLUDES
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    -- Gom nhóm số lượng nhập theo từng sản phẩm rồi cộng vào tồn kho
    UPDATE p
    SET p.CURRENT_STOCK = p.CURRENT_STOCK + i.TOTAL_IMPORTED
    FROM CATALOG.PRODUCT p
    JOIN (
        SELECT PRODUCT_ID, SUM(QUANTITY) AS TOTAL_IMPORTED
        FROM inserted
        GROUP BY PRODUCT_ID
    ) i ON p.PRODUCT_ID = i.PRODUCT_ID;
END;
GO

PRINT N'✓ Tạo TRIGGER WAREHOUSE.TRG_INCLUDES_ADD_STOCK thành công';
GO


-- -------------------------------------------------------------
-- TRIGGER 2: TRG_EXPORT_APPROVE_SUB_STOCK
-- Bảng gắn: WAREHOUSE.EXPORT_RECEIPT
-- Sự kiện:  AFTER UPDATE
-- Mục đích: Khi Quản lý duyệt phiếu xuất (MANAGER_ID đổi từ NULL
--           sang NOT NULL), tự động TRỪ tồn kho theo số lượng
--           trong bảng chi tiết EXPORT_FROM.
-- Nghiệp vụ R14:
--   - Thủ kho lập phiếu xuất → MANAGER_ID = NULL → Chưa trừ kho.
--   - Quản lý bấm Duyệt → MANAGER_ID có giá trị → Trừ kho ngay.
-- Bảo vệ:  Nếu trừ xuống dưới 0, ràng buộc CHECK
--           CK_PRODUCT_CURRENTSTOCK (CURRENT_STOCK >= 0) trong bảng
--           PRODUCT sẽ tự động báo lỗi và ROLLBACK toàn bộ.
-- -------------------------------------------------------------
CREATE OR ALTER TRIGGER WAREHOUSE.TRG_EXPORT_APPROVE_SUB_STOCK
ON WAREHOUSE.EXPORT_RECEIPT
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    -- Chỉ xử lý những phiếu vừa chuyển từ chưa duyệt sang đã duyệt
    -- (MANAGER_ID cũ là NULL, MANAGER_ID mới là NOT NULL)
    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN deleted d ON i.EXPORT_RECEIPT_ID = d.EXPORT_RECEIPT_ID
        WHERE d.MANAGER_ID IS NULL 
          AND i.MANAGER_ID IS NOT NULL
    )
    BEGIN
        UPDATE p
        SET p.CURRENT_STOCK = p.CURRENT_STOCK - exp_sum.TOTAL_EXPORTED
        FROM CATALOG.PRODUCT p
        JOIN (
            SELECT ef.PRODUCT_ID, SUM(ef.QUANTITY) AS TOTAL_EXPORTED
            FROM WAREHOUSE.EXPORT_FROM ef
            JOIN inserted i 
                ON ef.EXPORT_RECEIPT_ID = i.EXPORT_RECEIPT_ID
            JOIN deleted d 
                ON i.EXPORT_RECEIPT_ID = d.EXPORT_RECEIPT_ID
            WHERE d.MANAGER_ID IS NULL 
              AND i.MANAGER_ID IS NOT NULL
            GROUP BY ef.PRODUCT_ID
        ) exp_sum ON p.PRODUCT_ID = exp_sum.PRODUCT_ID;
    END
END;
GO

PRINT N'✓ Tạo TRIGGER WAREHOUSE.TRG_EXPORT_APPROVE_SUB_STOCK thành công';
GO


-- -------------------------------------------------------------
-- TRIGGER 3: TRG_RETURN_SUPPLIER_REASON
-- Bảng gắn: WAREHOUSE.RETURN_SUPPLIER
-- Sự kiện:  AFTER INSERT, UPDATE
-- Mục đích: Chặn vi phạm nghiệp vụ R15:
--           Chỉ phiếu xuất có lý do = N'RETURN' mới được phép
--           gắn nhà cung cấp nhận hàng trong bảng RETURN_SUPPLIER.
--           Nếu lý do là SPOILED OR BROKEN hoặc EXPIRED thì
--           THROW lỗi và ROLLBACK.
-- -------------------------------------------------------------
CREATE OR ALTER TRIGGER WAREHOUSE.TRG_RETURN_SUPPLIER_REASON
ON WAREHOUSE.RETURN_SUPPLIER
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN WAREHOUSE.EXPORT_RECEIPT er 
            ON i.EXPORT_RECEIPT_ID = er.EXPORT_RECEIPT_ID
        WHERE er.REASON <> N'RETURN'
    )
    BEGIN
        ROLLBACK TRANSACTION;
        ;THROW 50001, N'Lỗi R15: Chỉ phiếu xuất có lý do RETURN (Trả nhà cung cấp) mới được phép gắn nhà cung cấp nhận hàng trong bảng RETURN_SUPPLIER.', 1;
    END
END;
GO

PRINT N'✓ Tạo TRIGGER WAREHOUSE.TRG_RETURN_SUPPLIER_REASON thành công';
GO


-- =============================================================
-- PHẦN 3: STORED PROCEDURES (Giai đoạn 2 - 16-17/11)
-- =============================================================

-- -------------------------------------------------------------
-- PROCEDURE 1: SP_APPROVE_EXPORT_RECEIPT
-- Mục đích: Quản lý duyệt 1 phiếu xuất kho.
-- Tham số:
--   @ExportReceiptId INT : Mã phiếu xuất cần duyệt
--   @ManagerId       INT : Mã nhân viên quản lý thực hiện duyệt
-- Nghiệp vụ:
--   1. Kiểm tra phiếu tồn tại và đang ở trạng thái chưa duyệt.
--   2. Kiểm tra Manager hợp lệ (có trong bảng HR.MANAGER).
--   3. Cập nhật MANAGER_ID và APPROVE_TIME → Trigger
--      TRG_EXPORT_APPROVE_SUB_STOCK tự động trừ tồn kho.
--      (Nếu xuất vượt tồn, CHECK constraint sẽ tự rollback)
-- -------------------------------------------------------------
CREATE OR ALTER PROCEDURE WAREHOUSE.SP_APPROVE_EXPORT_RECEIPT
    @ExportReceiptId INT,
    @ManagerId       INT
AS
BEGIN
    SET NOCOUNT ON;

    -- 1. Kiểm tra phiếu xuất có tồn tại không
    IF NOT EXISTS (
        SELECT 1 FROM WAREHOUSE.EXPORT_RECEIPT
        WHERE EXPORT_RECEIPT_ID = @ExportReceiptId
    )
    BEGIN
        ;THROW 50010, N'Phiếu xuất không tồn tại.', 1;
        RETURN;
    END

    -- 2. Kiểm tra phiếu đã duyệt chưa
    IF EXISTS (
        SELECT 1 FROM WAREHOUSE.EXPORT_RECEIPT
        WHERE EXPORT_RECEIPT_ID = @ExportReceiptId
          AND MANAGER_ID IS NOT NULL
    )
    BEGIN
        ;THROW 50011, N'Phiếu xuất đã được duyệt trước đó.', 1;
        RETURN;
    END

    -- 3. Kiểm tra Manager hợp lệ
    IF NOT EXISTS (
        SELECT 1 FROM HR.MANAGER
        WHERE EMPLOYEE_ID = @ManagerId
    )
    BEGIN
        ;THROW 50012, N'Mã quản lý không hợp lệ.', 1;
        RETURN;
    END

    -- 4. Duyệt phiếu (Trigger TRG_EXPORT_APPROVE_SUB_STOCK sẽ tự trừ kho)
    UPDATE WAREHOUSE.EXPORT_RECEIPT
    SET MANAGER_ID   = @ManagerId,
        APPROVE_TIME = GETDATE()
    WHERE EXPORT_RECEIPT_ID = @ExportReceiptId;

    PRINT N'✓ Duyệt phiếu xuất #' + CAST(@ExportReceiptId AS NVARCHAR) + N' thành công.';
END;
GO

PRINT N'✓ Tạo PROCEDURE WAREHOUSE.SP_APPROVE_EXPORT_RECEIPT thành công';
GO


-- -------------------------------------------------------------
-- PROCEDURE 2: SP_APPROVE_STOCK_CHECK
-- Mục đích: Quản lý duyệt 1 phiếu kiểm kê.
-- Tham số:
--   @StockCheckId INT : Mã phiếu kiểm kê cần duyệt
--   @ManagerId    INT : Mã nhân viên quản lý thực hiện duyệt
-- Nghiệp vụ:
--   1. Kiểm tra phiếu tồn tại và đang ở trạng thái WAITING FOR APPROVAL.
--   2. Kiểm tra Manager hợp lệ.
--   3. Đổi STATUS = 'APPROVED', ghi MANAGER_ID và APPROVE_TIME.
--   4. Cập nhật CURRENT_STOCK = ACTUAL_QUANTITY cho các sản phẩm
--      trong phiếu kiểm kê (trong cùng 1 Transaction).
-- -------------------------------------------------------------
CREATE OR ALTER PROCEDURE WAREHOUSE.SP_APPROVE_STOCK_CHECK
    @StockCheckId INT,
    @ManagerId    INT
AS
BEGIN
    SET NOCOUNT ON;

    -- 1. Kiểm tra phiếu kiểm kê có tồn tại không
    IF NOT EXISTS (
        SELECT 1 FROM WAREHOUSE.STOCK_CHECK
        WHERE STOCK_CHECK_ID = @StockCheckId
    )
    BEGIN
        ;THROW 50020, N'Phiếu kiểm kê không tồn tại.', 1;
        RETURN;
    END

    -- 2. Kiểm tra phiếu đang ở trạng thái chờ duyệt
    IF NOT EXISTS (
        SELECT 1 FROM WAREHOUSE.STOCK_CHECK
        WHERE STOCK_CHECK_ID = @StockCheckId
          AND STATUS = N'WAITING FOR APPROVAL'
    )
    BEGIN
        ;THROW 50021, N'Phiếu kiểm kê đã được duyệt trước đó hoặc không ở trạng thái chờ.', 1;
        RETURN;
    END

    -- 3. Kiểm tra Manager hợp lệ
    IF NOT EXISTS (
        SELECT 1 FROM HR.MANAGER
        WHERE EMPLOYEE_ID = @ManagerId
    )
    BEGIN
        ;THROW 50022, N'Mã quản lý không hợp lệ.', 1;
        RETURN;
    END

    BEGIN TRANSACTION;
    BEGIN TRY
        -- Bước 1: Cập nhật trạng thái phiếu kiểm kê sang APPROVED
        UPDATE WAREHOUSE.STOCK_CHECK
        SET STATUS       = N'APPROVED',
            MANAGER_ID   = @ManagerId,
            APPROVE_TIME = GETDATE()
        WHERE STOCK_CHECK_ID = @StockCheckId;

        -- Bước 2: Đồng bộ tồn kho: CURRENT_STOCK = ACTUAL_QUANTITY
        UPDATE p
        SET p.CURRENT_STOCK = cd.ACTUAL_QUANTITY
        FROM CATALOG.PRODUCT p
        JOIN WAREHOUSE.CHECK_DETAIL cd
            ON p.PRODUCT_ID = cd.PRODUCT_ID
        WHERE cd.STOCK_CHECK_ID = @StockCheckId;

        COMMIT TRANSACTION;
        PRINT N'✓ Duyệt phiếu kiểm kê #' + CAST(@StockCheckId AS NVARCHAR) + N' thành công.';
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        ;THROW;
    END CATCH;
END;
GO

PRINT N'✓ Tạo PROCEDURE WAREHOUSE.SP_APPROVE_STOCK_CHECK thành công';
GO



