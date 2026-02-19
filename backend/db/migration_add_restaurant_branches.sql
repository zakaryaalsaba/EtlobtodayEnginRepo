-- Migration: Add restaurant_branches table
-- Branches belong to a restaurant website and are associated with a region
-- Run once: mysql -u root -p restaurant_websites < migration_add_restaurant_branches.sql

CREATE TABLE IF NOT EXISTS restaurant_branches (
    id INT AUTO_INCREMENT PRIMARY KEY,
    website_id INT NOT NULL COMMENT 'Restaurant website this branch belongs to (FK to restaurant_websites)',
    region_id INT NOT NULL COMMENT 'Region where this branch is located (FK to regions)',
    branch_number INT NOT NULL COMMENT 'Branch number/identifier (e.g., 1, 2, 3)',
    name VARCHAR(255) NULL COMMENT 'Optional branch name',
    name_ar VARCHAR(255) NULL COMMENT 'Optional branch name in Arabic',
    address TEXT NULL COMMENT 'Optional branch address',
    phone VARCHAR(50) NULL COMMENT 'Optional branch phone number',
    status ENUM('active', 'inactive') DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (website_id) REFERENCES restaurant_websites(id) ON DELETE CASCADE,
    FOREIGN KEY (region_id) REFERENCES regions(id) ON DELETE RESTRICT,
    INDEX idx_restaurant_branches_website_id (website_id),
    INDEX idx_restaurant_branches_region_id (region_id),
    UNIQUE KEY unique_website_branch_number (website_id, branch_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
