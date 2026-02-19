-- Diagnostic queries for "Total Paid to Restaurants & Drivers" card
-- Run these queries to check what's happening with the calculation

-- Query 1: Check today's orders and their total_amount values
SELECT 
    o.id,
    o.order_number,
    o.status,
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

-- Query 2: Sum of total_amount for today's orders (what the card should show)
SELECT 
    COUNT(*) as orders_count,
    COALESCE(SUM(CAST(total_amount AS DECIMAL(10, 2))), 0) as total_paid_sum,
    COALESCE(SUM(CAST(total_original_amount AS DECIMAL(10, 2))), 0) as total_original_sum,
    COALESCE(SUM(CAST(tax AS DECIMAL(10, 2))), 0) as total_tax_sum,
    COALESCE(SUM(CAST(delivery_fees AS DECIMAL(10, 2))), 0) as total_delivery_fees_sum
FROM orders
WHERE DATE(created_at) = CURDATE()
AND status IN ('completed', 'picked_up', 'ready', 'preparing', 'confirmed');

-- Query 3: Breakdown by status
SELECT 
    status,
    COUNT(*) as count,
    COALESCE(SUM(CAST(total_amount AS DECIMAL(10, 2))), 0) as total_amount_sum
FROM orders
WHERE DATE(created_at) = CURDATE()
GROUP BY status
ORDER BY status;

-- Query 4: Check if there are orders with NULL or 0 total_amount
SELECT 
    COUNT(*) as orders_with_null_or_zero_total,
    COUNT(CASE WHEN total_amount IS NULL OR total_amount = 0 THEN 1 END) as null_or_zero_count
FROM orders
WHERE DATE(created_at) = CURDATE()
AND status IN ('completed', 'picked_up', 'ready', 'preparing', 'confirmed');

-- Query 5: Compare total_amount vs (total_original_amount + tax + delivery_fees)
-- This should help identify if there's a mismatch
SELECT 
    o.id,
    o.order_number,
    o.total_amount,
    o.total_original_amount,
    o.tax,
    o.delivery_fees,
    (COALESCE(o.total_original_amount, 0) + COALESCE(o.tax, 0) + COALESCE(o.delivery_fees, 0)) as calculated_total,
    (o.total_amount - (COALESCE(o.total_original_amount, 0) + COALESCE(o.tax, 0) + COALESCE(o.delivery_fees, 0))) as difference
FROM orders o
WHERE DATE(o.created_at) = CURDATE()
AND o.status IN ('completed', 'picked_up', 'ready', 'preparing', 'confirmed')
HAVING ABS(difference) > 0.01
ORDER BY ABS(difference) DESC;

-- Query 6: The exact query used by the backend (with time range)
SELECT 
    COUNT(*) as orders_count,
    COALESCE(SUM(CAST(total_amount AS DECIMAL(10, 2))), 0) as total_paid
FROM orders
WHERE created_at >= DATE_FORMAT(CURDATE(), '%Y-%m-%d 00:00:00')
AND created_at < DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '%Y-%m-%d 00:00:00')
AND status IN ('completed', 'picked_up', 'ready', 'preparing', 'confirmed');

