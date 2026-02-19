-- Migration: Add multilingual columns for add-on name and description.
-- Existing columns (name, description) are used as English/default.
-- Run manually if needed: mysql -u root -p restaurant_websites < migration_add_addon_i18n.sql

ALTER TABLE product_addons
  ADD COLUMN name_ar VARCHAR(255) NULL AFTER name,
  ADD COLUMN description_ar TEXT NULL AFTER description;
