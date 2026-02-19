-- Migration: Add addresses table (multiple addresses per customer)
-- Run this if you have an existing database and addresses table was not in schema.sql

CREATE TABLE IF NOT EXISTS addresses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    area VARCHAR(255) NULL,
    latitude DECIMAL(10, 8) NULL,
    longitude DECIMAL(11, 8) NULL,
    address_type ENUM('apartment', 'house', 'office') DEFAULT 'apartment',
    building_name VARCHAR(255) NULL,
    apartment_number VARCHAR(50) NULL,
    floor VARCHAR(50) NULL,
    street VARCHAR(500) NULL,
    phone_country_code VARCHAR(20) NULL,
    phone_number VARCHAR(50) NULL,
    additional_directions TEXT NULL,
    address_label VARCHAR(255) NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    INDEX idx_customer_id (customer_id),
    INDEX idx_is_default (is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
