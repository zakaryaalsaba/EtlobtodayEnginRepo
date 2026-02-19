-- Migration: Add website_id to delivery_zones table
-- This allows zones to be associated with specific restaurants/stores
-- One restaurant/store can have many zones
-- Zones are REQUIRED to be associated with a restaurant (NOT NULL)

ALTER TABLE delivery_zones 
ADD COLUMN website_id INT NOT NULL AFTER delivery_company_id,
ADD INDEX idx_website_id (website_id),
ADD FOREIGN KEY (website_id) REFERENCES restaurant_websites(id) ON DELETE CASCADE;

-- Note: If you have existing zones without website_id, you'll need to assign them to a restaurant
-- or delete them before running this migration
