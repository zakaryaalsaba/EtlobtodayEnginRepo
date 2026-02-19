-- Query to check orders status for today
-- This shows what the backend query returns

-- Query 1: All today's orders with their actual status from database
SELECT 
    o.id,
    o.order_number,
    o.status as actual_status,
    o.order_type,
    o.total_amount,
    o.total_original_amount,
    o.tax,
    o.delivery_fees,
    o.created_at,
    DATE(o.created_at) as order_date
FROM orders o
WHERE DATE(o.created_at) = CURDATE()
ORDER BY o.created_at DESC;

-- Query 2: Count orders by status for today
SELECT 
    status,
    COUNT(*) as count,
    COALESCE(SUM(CAST(total_amount AS DECIMAL(10, 2))), 0) as total_amount_sum
FROM orders
WHERE DATE(created_at) = CURDATE()
GROUP BY status
ORDER BY status;

-- Query 3: The exact query the backend uses (with time range, no status filter)
SELECT 
    o.id,
    o.order_number,
    o.status,
    o.order_type,
    o.total_amount,
    o.created_at
FROM orders o
WHERE o.created_at >= DATE_FORMAT(CURDATE(), '%Y-%m-%d 00:00:00')
AND o.created_at < DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '%Y-%m-%d 00:00:00')
ORDER BY o.created_at DESC;

-- Query 4: Check if there are pending orders today
SELECT 
    COUNT(*) as pending_orders_count
FROM orders
WHERE DATE(created_at) = CURDATE()
AND status = 'pending';

