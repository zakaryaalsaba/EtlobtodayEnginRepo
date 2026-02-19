-- Add 'accepted_by_driver' to orders.status ENUM for driver acceptance flow
-- When driver accepts an order, status changes to 'accepted_by_driver' instead of 'confirmed'
-- Run once. Safe to re-run only if not yet applied (ALTER will fail if value already in ENUM).

ALTER TABLE orders
MODIFY COLUMN status ENUM(
  'pending', 'confirmed', 'preparing', 'ready',
  'accepted_by_driver', 'arrived_at_pickup', 'picked_up', 'completed', 'cancelled'
) DEFAULT 'pending';
