USE WAREHOUSEMANAGEMENT;
GO

--- VIEW ---
-- AGE
CREATE OR ALTER VIEW HR.V_EMPLOYEE_AGE AS
SELECT EMPLOYEE_ID, LAST_NAME, FIRST_NAME , DATE_OF_BIRTH,
LAST_NAME + N' ' + FIRST_NAME AS FULL_NAME, 
DATEDIFF(YEAR, DATE_OF_BIRTH, GETDATE()) 
- CASE WHEN DATEADD(YEAR, DATEDIFF(YEAR, DATE_OF_BIRTH, GETDATE()), DATE_OF_BIRTH) > GETDATE() THEN 1 ELSE 0 END AS AGE
FROM HR.EMPLOYEE;
GO

-- SALES
CREATE OR ALTER VIEW SALES.V_SALES_ORDER_TOTAL AS 
SELECT o.SALES_ORDER_ID, o.CUSTOMER_ID, o.EMPLOYEE_ID, o.ORDER_DATE, o.STATUS, o.DISCOUNT,
    ISNULL(l.SUBTOTAL, 0) AS SUBTOTAL,
    ISNULL(p.PAID_AMOUNT, 0) AS PAID_AMOUNT,
    ISNULL(l.SUBTOTAL, 0) - o.DISCOUNT AS TOTAL_AMOUNT,
    (ISNULL(l.SUBTOTAL, 0) - o.DISCOUNT) - ISNULL(p.PAID_AMOUNT, 0) AS REMAINING
FROM SALES.SALES_ORDER AS o
LEFT JOIN (
    SELECT SALES_ORDER_ID,
        SUM(UNIT_PRICE * QUANTITY) AS SUBTOTAL
    FROM SALES.ORDER_LINE
    GROUP BY SALES_ORDER_ID
) AS l ON l.SALES_ORDER_ID = o.SALES_ORDER_ID
LEFT JOIN (
    SELECT SALES_ORDER_ID,
        SUM(AMOUNT) AS PAID_AMOUNT
    FROM SALES.PAYMENT
    GROUP BY SALES_ORDER_ID
) AS p ON p.SALES_ORDER_ID = o.SALES_ORDER_ID;
GO

-- Trigger 1
CREATE OR ALTER TRIGGER SALES.TRG_ORDER_LINE_SUB_STOCK
ON SALES.ORDER_LINE
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

-- Tổng số lượng bán của từng sản phẩm trong lần thêm này
    DECLARE @sold TABLE (PRODUCT_ID INT, SOLD_QTY INT);
    INSERT INTO @sold (PRODUCT_ID, SOLD_QTY)
    SELECT PRODUCT_ID, SUM(QUANTITY)
    FROM inserted
    GROUP BY PRODUCT_ID;

-- Kiểm tra: có sản phẩm nào tồn kho ít hơn số bán không?
    IF EXISTS (
        SELECT 1
        FROM CATALOG.PRODUCT AS p
        JOIN @sold AS s ON s.PRODUCT_ID = p.PRODUCT_ID
        WHERE p.CURRENT_STOCK < s.SOLD_QTY
    )
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50001, N'Không đủ hàng trong kho', 1;
    END

-- Trừ kho
    UPDATE p
    SET p.CURRENT_STOCK = p.CURRENT_STOCK - s.SOLD_QTY
    FROM CATALOG.PRODUCT AS p
    JOIN @sold AS s ON s.PRODUCT_ID = p.PRODUCT_ID;
END;
GO

-- Trigger 2
CREATE OR ALTER TRIGGER WAREHOUSE.TRG_STOCK_CHECK_APPROVE
ON WAREHOUSE.STOCK_CHECK
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE p
    SET p.CURRENT_STOCK = d.ACTUAL_QUANTITY
    FROM CATALOG.PRODUCT AS p
    JOIN WAREHOUSE.CHECK_DETAIL AS d ON d.PRODUCT_ID = p.PRODUCT_ID
    JOIN inserted AS i ON i.STOCK_CHECK_ID = d.STOCK_CHECK_ID
    JOIN deleted  AS o ON o.STOCK_CHECK_ID = i.STOCK_CHECK_ID
    WHERE o.STATUS = N'WAITING FOR APPROVAL'     
      AND i.STATUS = N'APPROVED';
END;
GO

-- ===== PROCEDURES =====

-- Đăng nhập: đúng trả 1 dòng, sai trả 0 dòng
CREATE OR ALTER PROCEDURE HR.SP_LOGIN
    @username VARCHAR(50),
    @password VARCHAR(255)
AS
BEGIN
    SET NOCOUNT ON;

    SELECT e.EMPLOYEE_ID,
           e.LAST_NAME + N' ' + e.FIRST_NAME AS FULL_NAME,
           CASE WHEN m.EMPLOYEE_ID IS NOT NULL THEN N'MANAGER'
                WHEN c.EMPLOYEE_ID IS NOT NULL THEN N'CASHIER'
                WHEN w.EMPLOYEE_ID IS NOT NULL THEN N'WAREHOUSE_STAFF'
                ELSE N'UNKNOWN' END AS USER_ROLE
    FROM HR.ACCOUNT AS a
    JOIN HR.EMPLOYEE AS e ON e.USERNAME = a.USERNAME
    LEFT JOIN HR.MANAGER AS m ON m.EMPLOYEE_ID = e.EMPLOYEE_ID
    LEFT JOIN HR.CASHIER AS c ON c.EMPLOYEE_ID = e.EMPLOYEE_ID
    LEFT JOIN HR.WAREHOUSE_STAFF AS w ON w.EMPLOYEE_ID = e.EMPLOYEE_ID
    WHERE a.USERNAME = @username
      AND a.PASSWORD = @password COLLATE Latin1_General_CS_AS;
END;
GO

-- Tạo đơn hàng từ chuỗi JSON: [{"productId":1,"quantity":2}, ...]
CREATE OR ALTER PROCEDURE SALES.SP_CREATE_SALES_ORDER
    @employee_id INT,
    @items NVARCHAR(MAX),
    @customer_id INT = NULL,
    @discount DECIMAL(15,2) = 0
AS
BEGIN
    SET NOCOUNT ON;

    IF ISJSON(@items) = 0
        THROW 50002, N'Danh sách hàng không hợp lệ', 1;

    DECLARE @lines TABLE (PRODUCT_ID INT, QUANTITY INT, UNIT_PRICE DECIMAL(15,2));

    INSERT INTO @lines (PRODUCT_ID, QUANTITY, UNIT_PRICE)
    SELECT j.productId, SUM(j.quantity), p.CURRENT_PRICE
    FROM OPENJSON(@items) WITH (productId INT '$.productId', quantity INT '$.quantity') AS j
    JOIN CATALOG.PRODUCT AS p ON p.PRODUCT_ID = j.productId AND p.STATUS = N'ON SALE'
    GROUP BY j.productId, p.CURRENT_PRICE;

    DECLARE @expected INT = (SELECT COUNT(DISTINCT productId)
                             FROM OPENJSON(@items) WITH (productId INT '$.productId'));

    IF @expected = 0 OR @expected <> (SELECT COUNT(*) FROM @lines)
        THROW 50002, N'Có sản phẩm không tồn tại hoặc ngừng bán', 1;

    IF @discount > (SELECT SUM(UNIT_PRICE * QUANTITY) FROM @lines)
        THROW 50003, N'Giảm giá lớn hơn tiền hàng', 1;

    BEGIN TRY
        BEGIN TRAN;

        INSERT INTO SALES.SALES_ORDER (CUSTOMER_ID, EMPLOYEE_ID, ORDER_DATE, STATUS, DISCOUNT)
        VALUES (@customer_id, @employee_id, GETDATE(), N'NOT PAID', @discount);

        DECLARE @order_id INT = SCOPE_IDENTITY();

        INSERT INTO SALES.ORDER_LINE (PRODUCT_ID, SALES_ORDER_ID, UNIT_PRICE, QUANTITY)
        SELECT PRODUCT_ID, @order_id, UNIT_PRICE, QUANTITY FROM @lines;

        COMMIT;
        SELECT @order_id AS SALES_ORDER_ID;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK;
        THROW;
    END CATCH
END;
GO

-- Thêm một lần thanh toán cho đơn; đủ tiền thì đổi trạng thái PAID
CREATE OR ALTER PROCEDURE SALES.SP_ADD_PAYMENT
    @sales_order_id INT,
    @method NVARCHAR(50),
    @amount DECIMAL(15,2)
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @status NVARCHAR(20), @remaining DECIMAL(15,2);

    SELECT @status = STATUS, @remaining = REMAINING
    FROM SALES.V_SALES_ORDER_TOTAL
    WHERE SALES_ORDER_ID = @sales_order_id;

    IF @status IS NULL OR @status <> N'NOT PAID'
        THROW 50004, N'Đơn hàng không tồn tại hoặc không ở trạng thái chưa thanh toán', 1;

    IF @amount <= 0 OR @amount > @remaining
        THROW 50005, N'Số tiền thanh toán không hợp lệ', 1;

    BEGIN TRY
        BEGIN TRAN;

        DECLARE @next INT = (SELECT ISNULL(MAX(PAYMENT_NUMBER), 0) + 1
                             FROM SALES.PAYMENT
                             WHERE SALES_ORDER_ID = @sales_order_id);

        INSERT INTO SALES.PAYMENT (SALES_ORDER_ID, PAYMENT_NUMBER, METHOD, AMOUNT, PAID_TIME)
        VALUES (@sales_order_id, @next, @method, @amount, GETDATE());

        IF @amount = @remaining
            UPDATE SALES.SALES_ORDER SET STATUS = N'PAID'
            WHERE SALES_ORDER_ID = @sales_order_id;

        COMMIT;

        SELECT SALES_ORDER_ID, STATUS, TOTAL_AMOUNT, PAID_AMOUNT, REMAINING
        FROM SALES.V_SALES_ORDER_TOTAL
        WHERE SALES_ORDER_ID = @sales_order_id;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK;
        THROW;
    END CATCH
END;
GO

-- Báo cáo doanh thu theo ngày trong khoảng [from, to]
CREATE OR ALTER PROCEDURE SALES.SP_REVENUE_REPORT
    @from_date DATE,
    @to_date DATE
AS
BEGIN
    SET NOCOUNT ON;

    IF @from_date > @to_date
        THROW 50006, N'Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc', 1;

    SELECT CAST(ORDER_DATE AS DATE) AS REPORT_DATE,
           COUNT(*) AS ORDER_COUNT,
           SUM(TOTAL_AMOUNT) AS REVENUE
    FROM SALES.V_SALES_ORDER_TOTAL
    WHERE STATUS = N'PAID'
      AND ORDER_DATE >= @from_date
      AND ORDER_DATE < DATEADD(DAY, 1, @to_date)
    GROUP BY CAST(ORDER_DATE AS DATE)
    ORDER BY REPORT_DATE;
END;
GO

-- ===== VIEWS BÁO CÁO =====

-- Doanh thu theo thu ngân (chỉ đơn đã thanh toán)
CREATE OR ALTER VIEW SALES.V_REVENUE_BY_CASHIER AS
SELECT e.EMPLOYEE_ID,
       e.LAST_NAME + N' ' + e.FIRST_NAME AS FULL_NAME,
       COUNT(*) AS ORDER_COUNT,
       SUM(t.TOTAL_AMOUNT) AS REVENUE
FROM SALES.V_SALES_ORDER_TOTAL AS t
JOIN HR.EMPLOYEE AS e ON e.EMPLOYEE_ID = t.EMPLOYEE_ID
WHERE t.STATUS = N'PAID'
GROUP BY e.EMPLOYEE_ID, e.LAST_NAME, e.FIRST_NAME;
GO

-- Sản phẩm bán chạy, có xếp hạng theo số lượng bán
CREATE OR ALTER VIEW SALES.V_TOP_PRODUCTS AS
SELECT p.PRODUCT_ID,
       p.NAME,
       SUM(l.QUANTITY) AS TOTAL_QTY,
       SUM(l.UNIT_PRICE * l.QUANTITY) AS TOTAL_REVENUE,
       RANK() OVER (ORDER BY SUM(l.QUANTITY) DESC) AS RANK_NO
FROM SALES.ORDER_LINE AS l
JOIN SALES.SALES_ORDER AS o ON o.SALES_ORDER_ID = l.SALES_ORDER_ID
JOIN CATALOG.PRODUCT AS p ON p.PRODUCT_ID = l.PRODUCT_ID
WHERE o.STATUS = N'PAID'
GROUP BY p.PRODUCT_ID, p.NAME;
GO

-- Doanh thu theo thu ngân trong khoảng ngày [from, to]
CREATE OR ALTER PROCEDURE SALES.SP_REVENUE_BY_CASHIER
    @from_date DATE,
    @to_date DATE
AS
BEGIN
    SET NOCOUNT ON;

    IF @from_date > @to_date
        THROW 50006, N'Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc', 1;

    SELECT e.LAST_NAME + N' ' + e.FIRST_NAME AS FULL_NAME,
           COUNT(*) AS ORDER_COUNT,
           SUM(t.TOTAL_AMOUNT) AS REVENUE
    FROM SALES.V_SALES_ORDER_TOTAL AS t
    JOIN HR.EMPLOYEE AS e ON e.EMPLOYEE_ID = t.EMPLOYEE_ID
    WHERE t.STATUS = N'PAID'
      AND t.ORDER_DATE >= @from_date
      AND t.ORDER_DATE < DATEADD(DAY, 1, @to_date)
    GROUP BY e.EMPLOYEE_ID, e.LAST_NAME, e.FIRST_NAME
    ORDER BY REVENUE DESC;
END;
GO

-- Top sản phẩm bán chạy trong khoảng ngày, có xếp hạng
CREATE OR ALTER PROCEDURE SALES.SP_TOP_PRODUCTS
    @from_date DATE,
    @to_date DATE,
    @top_n INT = 20
AS
BEGIN
    SET NOCOUNT ON;

    IF @from_date > @to_date
        THROW 50006, N'Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc', 1;

    SELECT TOP (@top_n) *
    FROM (
        SELECT p.PRODUCT_ID,
               p.NAME,
               SUM(l.QUANTITY) AS TOTAL_QTY,
               SUM(l.UNIT_PRICE * l.QUANTITY) AS TOTAL_REVENUE,
               RANK() OVER (ORDER BY SUM(l.QUANTITY) DESC) AS RANK_NO
        FROM SALES.ORDER_LINE AS l
        JOIN SALES.SALES_ORDER AS o ON o.SALES_ORDER_ID = l.SALES_ORDER_ID
        JOIN CATALOG.PRODUCT AS p ON p.PRODUCT_ID = l.PRODUCT_ID
        WHERE o.STATUS = N'PAID'
          AND o.ORDER_DATE >= @from_date
          AND o.ORDER_DATE < DATEADD(DAY, 1, @to_date)
        GROUP BY p.PRODUCT_ID, p.NAME
    ) AS x
    ORDER BY RANK_NO;
END;
GO