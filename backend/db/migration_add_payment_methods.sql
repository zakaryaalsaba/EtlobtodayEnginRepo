-- Migration: Add payment_methods table for storing saved credit cards
-- Run this if you have an existing database

CREATE TABLE IF NOT EXISTS payment_methods (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    token VARCHAR(255) NOT NULL COMMENT 'PayTabs tokenization token',
    card_last4 VARCHAR(4) NOT NULL COMMENT 'Last 4 digits of card',
    card_brand VARCHAR(50) NULL COMMENT 'Card brand: Visa, MasterCard, etc.',
    expiry_month INT NULL COMMENT 'Expiry month (1-12)',
    expiry_year INT NULL COMMENT 'Expiry year (4 digits)',
    paytabs_response_json TEXT NULL COMMENT 'Full PayTabs response JSON for reference',
    is_default BOOLEAN DEFAULT FALSE COMMENT 'Default payment method for customer',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    INDEX idx_customer_id (customer_id),
    INDEX idx_token (token),
    INDEX idx_is_default (is_default),
    UNIQUE KEY unique_customer_token (customer_id, token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
