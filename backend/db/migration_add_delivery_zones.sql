-- Migration: Add delivery_zones table
-- This table stores zones that each delivery company serves

CREATE TABLE IF NOT EXISTS delivery_zones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    delivery_company_id INT NOT NULL,
    zone_name_ar VARCHAR(255) NOT NULL COMMENT 'Zone name in Arabic',
    zone_name_en VARCHAR(255) NOT NULL COMMENT 'Zone name in English',
    price DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'Delivery price for this zone',
    status ENUM('active', 'inactive') DEFAULT 'active',
    image_url TEXT,
    image_path VARCHAR(500),
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (delivery_company_id) REFERENCES delivery_companies(id) ON DELETE CASCADE,
    INDEX idx_delivery_company_id (delivery_company_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
