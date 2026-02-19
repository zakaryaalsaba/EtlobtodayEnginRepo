-- Product Add-ons feature
-- Each product can have add-on items (e.g. extra cheese, sides).
-- At product level: addon_required (boolean) and addon_required_min (1 = at least 1, 2 = at least 2, -1 = All).
-- When Optional: no minimum; when Required: admin sets "at least N" or "All".

-- Add columns to products table for add-on rules
ALTER TABLE products
  ADD COLUMN addon_required BOOLEAN DEFAULT FALSE AFTER is_available,
  ADD COLUMN addon_required_min INT NULL COMMENT 'When required: 1 2 3 or -1 for All. NULL when optional' AFTER addon_required;

-- Product add-ons table: each row is one add-on item (name, description, picture, price, optional/required)
CREATE TABLE IF NOT EXISTS product_addons (
  id INT AUTO_INCREMENT PRIMARY KEY,
  product_id INT NOT NULL,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  image_url TEXT,
  image_path VARCHAR(500),
  price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
  is_required BOOLEAN DEFAULT FALSE,
  display_order INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
