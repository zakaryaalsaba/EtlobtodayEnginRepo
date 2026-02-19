-- Migration: Add admin credentials to delivery_companies and link drivers to companies
-- Delivery company admins can log in and manage their zones and drivers
-- Run this after delivery_companies and drivers tables exist.
-- (init.js also adds these columns automatically on startup.)

-- Add admin username and password to delivery_companies (run once)
-- ALTER TABLE delivery_companies ADD COLUMN admin_username VARCHAR(255) NULL UNIQUE COMMENT 'Login username for delivery company admin';
-- ALTER TABLE delivery_companies ADD COLUMN admin_password_hash VARCHAR(255) NULL COMMENT 'Bcrypt hash of admin password';

-- Add delivery_company_id to drivers (run once)
-- ALTER TABLE drivers ADD COLUMN delivery_company_id INT NULL COMMENT 'Company this driver/captain belongs to';
-- ALTER TABLE drivers ADD CONSTRAINT fk_drivers_delivery_company FOREIGN KEY (delivery_company_id) REFERENCES delivery_companies(id) ON DELETE SET NULL;
-- ALTER TABLE drivers ADD INDEX idx_drivers_delivery_company_id (delivery_company_id);
