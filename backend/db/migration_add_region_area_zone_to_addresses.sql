-- Migration: Add region_id, area_id, and zone_id columns to addresses table
-- Run once: mysql -u root -p restaurant_websites < migration_add_region_area_zone_to_addresses.sql

USE restaurant_websites;

-- Add region_id column
ALTER TABLE addresses
ADD COLUMN region_id INT NULL COMMENT 'Region ID (FK to regions)' AFTER area,
ADD INDEX idx_region_id (region_id);

-- Add region_name column
ALTER TABLE addresses
ADD COLUMN region_name VARCHAR(255) NULL COMMENT 'Region name (text value)' AFTER region_id;

-- Add area_id column
ALTER TABLE addresses
ADD COLUMN area_id INT NULL COMMENT 'Area ID (FK to areas)' AFTER region_name,
ADD INDEX idx_area_id (area_id);

-- Add area_name column
ALTER TABLE addresses
ADD COLUMN area_name VARCHAR(255) NULL COMMENT 'Area name (text value)' AFTER area_id;

-- Add zone_id column
ALTER TABLE addresses
ADD COLUMN zone_id INT NULL COMMENT 'Zone ID (FK to delivery_zones)' AFTER area_name,
ADD INDEX idx_zone_id (zone_id);

-- Add zone_name column
ALTER TABLE addresses
ADD COLUMN zone_name VARCHAR(255) NULL COMMENT 'Zone name (text value)' AFTER zone_id;

-- Add zone_price column
ALTER TABLE addresses
ADD COLUMN zone_price DECIMAL(10, 2) NULL COMMENT 'Zone delivery price' AFTER zone_name;

-- Add foreign key constraints
ALTER TABLE addresses
ADD CONSTRAINT fk_addresses_region
FOREIGN KEY (region_id) REFERENCES regions(id) ON DELETE SET NULL;

ALTER TABLE addresses
ADD CONSTRAINT fk_addresses_area
FOREIGN KEY (area_id) REFERENCES areas(id) ON DELETE SET NULL;

ALTER TABLE addresses
ADD CONSTRAINT fk_addresses_zone
FOREIGN KEY (zone_id) REFERENCES delivery_zones(id) ON DELETE SET NULL;
