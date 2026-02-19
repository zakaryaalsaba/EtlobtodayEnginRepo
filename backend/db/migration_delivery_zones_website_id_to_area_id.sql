-- Migration: Replace delivery_zones.website_id with delivery_zones.area_id
-- Also add restaurant_websites.area_id so stores can be linked to an area (for "zones for this store").
-- Run once: mysql -u root -p restaurant_websites < migration_delivery_zones_website_id_to_area_id.sql
-- If you have existing zones, backfill area_id or set it NULL and update later.
use restaurant_websites;
-- 1. Add area_id to delivery_zones (nullable for existing rows)
ALTER TABLE delivery_zones
  ADD COLUMN area_id INT NULL AFTER delivery_company_id,
  ADD INDEX idx_area_id (area_id),
  ADD CONSTRAINT fk_delivery_zones_area FOREIGN KEY (area_id) REFERENCES areas(id) ON DELETE CASCADE;

-- 2. Drop foreign key on website_id (MySQL auto-name is often delivery_zones_ibfk_2; if error, run:
--    SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='delivery_zones' AND CONSTRAINT_TYPE='FOREIGN KEY';
--    then DROP FOREIGN KEY <name>;)
ALTER TABLE delivery_zones DROP FOREIGN KEY delivery_zones_ibfk_2;

-- 3. Drop website_id column (index idx_website_id is dropped with the column)
ALTER TABLE delivery_zones DROP COLUMN website_id;

-- 4. Add area_id to restaurant_websites so "zones for this store" = zones where area_id = store.area_id
--    If this fails with "Duplicate column name 'area_id'", the column already exists (e.g. from init.js); skip step 4.
ALTER TABLE restaurant_websites
  ADD COLUMN area_id INT NULL COMMENT 'Area where this restaurant is located (for delivery zones)' AFTER id,
  ADD INDEX idx_restaurant_websites_area_id (area_id),
  ADD CONSTRAINT fk_restaurant_websites_area FOREIGN KEY (area_id) REFERENCES areas(id) ON DELETE SET NULL;
