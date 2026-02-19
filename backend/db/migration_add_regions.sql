-- Migration: Add regions table
-- Regions belong to a city (one city has many regions)
-- Run once: mysql -u root -p restaurant_websites < migration_add_regions.sql
use restaurant_websites;
CREATE TABLE IF NOT EXISTS regions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    city_id INT NOT NULL COMMENT 'City this region belongs to (FK to cities)',
    name VARCHAR(255) NOT NULL COMMENT 'Region name (e.g. English)',
    name_ar VARCHAR(255) NULL COMMENT 'Region name in Arabic',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (city_id) REFERENCES cities(id) ON DELETE CASCADE,
    INDEX idx_regions_city_id (city_id),
    INDEX idx_regions_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
