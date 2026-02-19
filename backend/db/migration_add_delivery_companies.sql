-- Migration: Add delivery_companies table
-- This table stores information about delivery companies that the platform works with

CREATE TABLE IF NOT EXISTS delivery_companies (
    id INT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    contact_name VARCHAR(255) NOT NULL COMMENT 'Full name of the contact person',
    phone VARCHAR(50),
    address TEXT,
    emails JSON COMMENT 'Array of email addresses',
    website VARCHAR(500),
    status ENUM('active', 'inactive', 'suspended') DEFAULT 'active',
    notes TEXT,
    profile_image_url TEXT,
    profile_image_path VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_company_name (company_name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
