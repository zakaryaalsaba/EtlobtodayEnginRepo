-- SQL UPDATE statements to populate total_original_amount, tax, and delivery_fees columns
-- for existing orders in the database

-- Step 1: Update total_original_amount
-- Calculate sum of (original_price * quantity) from products table
-- Falls back to product_price if original_price is NULL
UPDATE orders o
INNER JOIN (
    SELECT 
        oi.order_id,
        COALESCE(SUM(oi.quantity * COALESCE(p.original_price, oi.product_price)), 0) as total_original
    FROM order_items oi
    LEFT JOIN products p ON oi.product_id = p.id
    GROUP BY oi.order_id
) as order_totals ON o.id = order_totals.order_id
SET o.total_original_amount = order_totals.total_original;

-- Step 2: Update tax
-- Calculate tax based on subtotal (sum of product_price * quantity) and tax_rate from restaurant_websites
-- Only applies if tax_enabled = 1
UPDATE orders o
INNER JOIN restaurant_websites rw ON o.website_id = rw.id
INNER JOIN (
    SELECT 
        oi.order_id,
        COALESCE(SUM(oi.quantity * oi.product_price), 0) as subtotal
    FROM order_items oi
    GROUP BY oi.order_id
) as order_subtotals ON o.id = order_subtotals.order_id
SET o.tax = CASE 
    WHEN rw.tax_enabled = 1 AND rw.tax_rate > 0 THEN
        (order_subtotals.subtotal * rw.tax_rate / 100)
    ELSE 0
END;

-- Step 3: Update delivery_fees
-- Get delivery_fee from restaurant_websites for delivery orders
UPDATE orders o
INNER JOIN restaurant_websites rw ON o.website_id = rw.id
SET o.delivery_fees = CASE 
    WHEN o.order_type = 'delivery' THEN COALESCE(rw.delivery_fee, 0)
    ELSE 0
END;

-- Verification query: Check a few orders to verify the updates
SELECT 
    o.id,
    o.order_number,
    o.order_type,
    o.total_amount,
    o.total_original_amount,
    o.tax,
    o.delivery_fees,
    (o.total_original_amount + o.tax + o.delivery_fees) as calculated_total,
    o.total_amount as stored_total
FROM orders o
ORDER BY o.created_at DESC
LIMIT 10;

