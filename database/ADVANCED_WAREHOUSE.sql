USE WAREHOUSEMANAGEMENT;
GO

--VIEWS

-- 1. Tong tien tung phieu nhap (R12)
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

-- 2. Chenh lech kiem ke: thuc te - he thong (R16)
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

-- 3. San pham dang ban co ton kho duoi muc toi thieu
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

-- 4. Bao cao nhap hang theo tung nha cung cap
CREATE OR ALTER VIEW WAREHOUSE.V_IMPORT_BY_SUPPLIER
AS
SELECT 
    s.SUPPLIER_ID,
    s.NAME                                              AS SUPPLIER_NAME,
    s.TAX_CODE,
    s.EMAIL,
    COUNT(DISTINCT ir.IMPORT_RECEIPT_ID)                AS RECEIPT_COUNT,
    ISNULL(SUM(inc.QUANTITY), 0)                        AS TOTAL_QUANTITY,
    ISNULL(SUM(inc.QUANTITY * inc.IMPORT_PRICE), 0)     AS TOTAL_IMPORT_AMOUNT,
    MAX(ir.IMPORT_DATE)                                 AS LAST_IMPORT_DATE
FROM WAREHOUSE.SUPPLIER s
    LEFT JOIN WAREHOUSE.IMPORT_RECEIPT ir 
        ON s.SUPPLIER_ID = ir.SUPPLIER_ID
    LEFT JOIN WAREHOUSE.INCLUDES inc 
        ON ir.IMPORT_RECEIPT_ID = inc.IMPORT_RECEIPT_ID
GROUP BY 
    s.SUPPLIER_ID,
    s.NAME,
    s.TAX_CODE,
    s.EMAIL;
GO


-- TRIGGERS
-- 1. Cong ton kho khi them dong phieu nhap (ho tro insert nhieu dong)
CREATE OR ALTER TRIGGER WAREHOUSE.TRG_INCLUDES_ADD_STOCK
ON WAREHOUSE.INCLUDES
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

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

-- 2. Tru ton kho khi quan ly duyet phieu xuat (MANAGER_ID tu NULL sang NOT NULL)
CREATE OR ALTER TRIGGER WAREHOUSE.TRG_EXPORT_APPROVE_SUB_STOCK
ON WAREHOUSE.EXPORT_RECEIPT
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

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

-- 3. Chan gan NCC nhan hang neu ly do xuat khong phai RETURN (R15)
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


--STORED PROCEDURES

-- 1. Duyet phieu xuat (cap nhat MANAGER_ID de trigger tu tru kho)
CREATE OR ALTER PROCEDURE WAREHOUSE.SP_APPROVE_EXPORT_RECEIPT
    @ExportReceiptId INT,
    @ManagerId       INT
AS
BEGIN
    SET NOCOUNT ON;

    IF NOT EXISTS (
        SELECT 1 FROM WAREHOUSE.EXPORT_RECEIPT
        WHERE EXPORT_RECEIPT_ID = @ExportReceiptId
    )
    BEGIN
        ;THROW 50010, N'Phiếu xuất không tồn tại.', 1;
        RETURN;
    END

    IF EXISTS (
        SELECT 1 FROM WAREHOUSE.EXPORT_RECEIPT
        WHERE EXPORT_RECEIPT_ID = @ExportReceiptId
          AND MANAGER_ID IS NOT NULL
    )
    BEGIN
        ;THROW 50011, N'Phiếu xuất đã được duyệt trước đó.', 1;
        RETURN;
    END

    IF NOT EXISTS (
        SELECT 1 FROM HR.MANAGER
        WHERE EMPLOYEE_ID = @ManagerId
    )
    BEGIN
        ;THROW 50012, N'Mã quản lý không hợp lệ.', 1;
        RETURN;
    END

    UPDATE WAREHOUSE.EXPORT_RECEIPT
    SET MANAGER_ID   = @ManagerId,
        APPROVE_TIME = GETDATE()
    WHERE EXPORT_RECEIPT_ID = @ExportReceiptId;
END;
GO

-- 2. Duyet phieu kiem ke (Trigger TRG_STOCK_CHECK_APPROVE tu cap nhat ton kho theo so thuc te)
CREATE OR ALTER PROCEDURE WAREHOUSE.SP_APPROVE_STOCK_CHECK
    @StockCheckId INT,
    @ManagerId    INT
AS
BEGIN
    SET NOCOUNT ON;

    IF NOT EXISTS (
        SELECT 1 FROM WAREHOUSE.STOCK_CHECK
        WHERE STOCK_CHECK_ID = @StockCheckId
    )
    BEGIN
        ;THROW 50020, N'Phiếu kiểm kê không tồn tại.', 1;
        RETURN;
    END

    IF NOT EXISTS (
        SELECT 1 FROM WAREHOUSE.STOCK_CHECK
        WHERE STOCK_CHECK_ID = @StockCheckId
          AND STATUS = N'WAITING FOR APPROVAL'
    )
    BEGIN
        ;THROW 50021, N'Phiếu kiểm kê đã được duyệt trước đó hoặc không ở trạng thái chờ.', 1;
        RETURN;
    END

    IF NOT EXISTS (
        SELECT 1 FROM HR.MANAGER
        WHERE EMPLOYEE_ID = @ManagerId
    )
    BEGIN
        ;THROW 50022, N'Mã quản lý không hợp lệ.', 1;
        RETURN;
    END

    UPDATE WAREHOUSE.STOCK_CHECK
    SET STATUS       = N'APPROVED',
        MANAGER_ID   = @ManagerId,
        APPROVE_TIME = GETDATE()
    WHERE STOCK_CHECK_ID = @StockCheckId;
END;
GO
