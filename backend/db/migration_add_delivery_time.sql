-- Add delivery time range (minutes) to restaurant_websites.
-- Run manually once if needed: mysql -u root -p restaurant_websites < migration_add_delivery_time.sql
-- Or just restart the backend; init.js will add these columns on startup.

ALTER TABLE restaurant_websites 
  ADD COLUMN delivery_time_min INT NULL AFTER delivery_fee;

ALTER TABLE restaurant_websites 
  ADD COLUMN delivery_time_max INT NULL AFTER delivery_time_min;
