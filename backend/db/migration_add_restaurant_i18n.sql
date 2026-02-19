-- Migration: Add multilingual columns for restaurant name, description, and address.
-- Existing columns (restaurant_name, description, address) are used as English/default.
-- New columns store Arabic (and can be extended for more languages later).
-- Run manually if needed: mysql -u root -p restaurant_websites < migration_add_restaurant_i18n.sql

ALTER TABLE restaurant_websites
  ADD COLUMN restaurant_name_ar VARCHAR(255) NULL AFTER restaurant_name,
  ADD COLUMN description_ar TEXT NULL AFTER description,
  ADD COLUMN address_ar TEXT NULL AFTER address;
