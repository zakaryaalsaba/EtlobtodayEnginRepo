-- Migration: Create orders_delivery table for restaurant driver requests per zone
-- Run this once: mysql -u root -p your_db < migration_add_orders_delivery.sql

CREATE TABLE IF NOT EXISTS orders_delivery (
  id INT AUTO_INCREMENT PRIMARY KEY,
  website_id INT NOT NULL COMMENT 'Restaurant website ID',
  zone_id INT NOT NULL COMMENT 'Delivery zone ID',
  zone_name VARCHAR(255) NOT NULL COMMENT 'Name of the zone at time of request',
  status ENUM('pending', 'assigned', 'completed', 'cancelled') DEFAULT 'pending',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (website_id) REFERENCES restaurant_websites(id) ON DELETE CASCADE,
  FOREIGN KEY (zone_id) REFERENCES delivery_zones(id) ON DELETE CASCADE,
  INDEX idx_orders_delivery_website (website_id),
  INDEX idx_orders_delivery_zone (zone_id),
  INDEX idx_orders_delivery_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
