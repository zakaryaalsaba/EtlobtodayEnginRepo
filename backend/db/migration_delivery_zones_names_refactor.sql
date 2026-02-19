-- Migration: Refactor delivery_zones to use delivery_zones_names table
-- Move zone_name_ar and zone_name_en to a separate table for normalization
-- Run once: mysql -u root -p restaurant_websites < migration_delivery_zones_names_refactor.sql
use restaurant_websites;
-- Step 1: Create the new delivery_zones_names table
CREATE TABLE IF NOT EXISTS delivery_zones_names (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name_ar VARCHAR(255) NOT NULL COMMENT 'Zone name in Arabic',
    name_en VARCHAR(255) NOT NULL COMMENT 'Zone name in English',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name_ar (name_ar),
    INDEX idx_name_en (name_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 2: Migrate existing zone names to the new table
-- Insert unique combinations of zone_name_ar and zone_name_en
INSERT INTO delivery_zones_names (name_ar, name_en, created_at, updated_at)
SELECT DISTINCT 
    zone_name_ar,
    zone_name_en,
    MIN(created_at) as created_at,
    MAX(updated_at) as updated_at
FROM delivery_zones
WHERE zone_name_ar IS NOT NULL AND zone_name_en IS NOT NULL
GROUP BY zone_name_ar, zone_name_en;

-- Step 3: Add zone_name_id column to delivery_zones (nullable first, then we'll populate it)
ALTER TABLE delivery_zones
ADD COLUMN zone_name_id INT NULL AFTER area_id,
ADD INDEX idx_zone_name_id (zone_name_id);

-- Step 4: Update delivery_zones to reference the new delivery_zones_names table
-- Using WHERE clause with key column (id) to satisfy MySQL safe update mode
UPDATE delivery_zones dz
INNER JOIN delivery_zones_names dzn ON dz.zone_name_ar = dzn.name_ar AND dz.zone_name_en = dzn.name_en
SET dz.zone_name_id = dzn.id
WHERE dz.zone_name_id IS NULL AND dz.id > 0;

-- Step 5: Add foreign key constraint
ALTER TABLE delivery_zones
ADD CONSTRAINT fk_delivery_zones_zone_name
FOREIGN KEY (zone_name_id) REFERENCES delivery_zones_names(id) ON DELETE RESTRICT;

-- Step 6: Make zone_name_id NOT NULL (after all records are updated)
ALTER TABLE delivery_zones
MODIFY COLUMN zone_name_id INT NOT NULL;

-- Step 7: Drop the old zone_name_ar and zone_name_en columns
ALTER TABLE delivery_zones
DROP COLUMN zone_name_ar,
DROP COLUMN zone_name_en;
