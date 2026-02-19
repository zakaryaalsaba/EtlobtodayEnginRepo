-- Migration: Add multilingual columns for product name, description, and category.
-- Existing columns (name, description, category) are used as English/default.
-- Run manually if needed: mysql -u root -p restaurant_websites < migration_add_product_i18n.sql

ALTER TABLE products
  ADD COLUMN name_ar VARCHAR(255) NULL AFTER name,
  ADD COLUMN description_ar TEXT NULL AFTER description,
  ADD COLUMN category_ar VARCHAR(100) NULL AFTER category;
