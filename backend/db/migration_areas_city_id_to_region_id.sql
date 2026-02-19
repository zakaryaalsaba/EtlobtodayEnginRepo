-- Migration: Replace areas.city_id with areas.region_id
-- Run once: mysql -u root -p restaurant_websites < migration_areas_city_id_to_region_id.sql

-- Step 1: Add region_id column to areas (nullable first)
ALTER TABLE areas
ADD COLUMN region_id INT NULL AFTER city_id,
ADD INDEX idx_region_id (region_id);

-- Step 2: Update all existing areas to have region_id = 1
UPDATE areas
SET region_id = 1
WHERE region_id IS NULL AND id > 0;

-- Step 3: Add foreign key constraint on region_id
ALTER TABLE areas
ADD CONSTRAINT fk_areas_region
FOREIGN KEY (region_id) REFERENCES regions(id) ON DELETE CASCADE;

-- Step 4: Make region_id NOT NULL (after all records are updated)
ALTER TABLE areas
MODIFY COLUMN region_id INT NOT NULL;

-- Step 5: Drop foreign key constraint on city_id
-- Find the constraint name dynamically and drop it
SET @constraint_name = (
  SELECT CONSTRAINT_NAME 
  FROM information_schema.TABLE_CONSTRAINTS 
  WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'areas' 
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    AND CONSTRAINT_NAME LIKE '%city%'
  LIMIT 1
);
SET @drop_constraint = CONCAT('ALTER TABLE areas DROP FOREIGN KEY ', @constraint_name);
PREPARE stmt FROM @drop_constraint;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 6: Drop city_id column (index idx_areas_city_id is dropped with the column)
ALTER TABLE areas
DROP COLUMN city_id;
