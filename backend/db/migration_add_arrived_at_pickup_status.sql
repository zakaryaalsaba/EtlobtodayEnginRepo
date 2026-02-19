-- Add 'arrived_at_pickup' to orders.status ENUM for driver flow: arrive at store -> mark arrived -> mark picked up -> deliver
-- Run once. Safe to re-run only if not yet applied (ALTER will fail if value already in ENUM).

ALTER TABLE orders
MODIFY COLUMN status ENUM(
  'pending', 'confirmed', 'preparing', 'ready',
  'arrived_at_pickup', 'picked_up', 'completed', 'cancelled'
) DEFAULT 'pending';
