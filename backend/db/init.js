import mysql from 'mysql2/promise';
import dotenv from 'dotenv';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Create MySQL connection pool
export const pool = mysql.createPool({
  host: process.env.MYSQL_HOST || 'localhost',
  port: parseInt(process.env.MYSQL_PORT) || 3306,
  database: process.env.MYSQL_DB || 'restaurant_websites',
  user: process.env.MYSQL_USER || 'root',
  password: process.env.MYSQL_PASSWORD || '',
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0
});

/**
 * Initialize MySQL database schema
 * Creates all necessary tables if they don't exist
 */
export async function initDatabase() {
  try {
    // First, ensure database exists
    const connection = await mysql.createConnection({
      host: process.env.MYSQL_HOST || 'localhost',
      port: parseInt(process.env.MYSQL_PORT) || 3306,
      user: process.env.MYSQL_USER || 'root',
      password: process.env.MYSQL_PASSWORD || '',
    });

    const dbName = process.env.MYSQL_DB || 'restaurant_websites';
    await connection.execute(`CREATE DATABASE IF NOT EXISTS \`${dbName}\``);
    await connection.end();

    // Read and execute schema SQL
    const schemaPath = path.join(__dirname, 'schema.sql');
    const schema = fs.readFileSync(schemaPath, 'utf8');
    
    // Split by semicolons and execute each statement (strip leading comment lines, skip empty)
    const statements = schema.split(';').map(stmt => stmt.trim()).filter(stmt => stmt.length > 0);
    
    for (const statement of statements) {
      const withoutLeadingComments = statement
        .split('\n')
        .filter(line => !line.trim().startsWith('--'))
        .join('\n')
        .trim();
      if (withoutLeadingComments.length > 0) {
        await pool.execute(withoutLeadingComments);
      }
    }
    
    // Run migration to add gallery_images, locations, app_download_url, newsletter_enabled columns
    try {
      // Check if columns exist and add them if they don't
      const [columns] = await pool.execute(`
        SELECT COLUMN_NAME 
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = ? 
        AND TABLE_NAME = 'restaurant_websites'
      `, [process.env.MYSQL_DB || 'restaurant_websites']);
      
      const existingColumns = columns.map(col => col.COLUMN_NAME);
      
      // Add gallery_images if it doesn't exist
      if (!existingColumns.includes('gallery_images')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN gallery_images JSON AFTER social_links
          `);
          console.log('Added gallery_images column');
        } catch (err) {
          console.warn('Error adding gallery_images column:', err.message);
        }
      }
      
      // Add locations if it doesn't exist
      if (!existingColumns.includes('locations')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN locations JSON AFTER gallery_images
          `);
          console.log('Added locations column');
        } catch (err) {
          console.warn('Error adding locations column:', err.message);
        }
      }
      
      // Add app_download_url if it doesn't exist
      if (!existingColumns.includes('app_download_url')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN app_download_url VARCHAR(500) AFTER locations
          `);
          console.log('Added app_download_url column');
        } catch (err) {
          console.warn('Error adding app_download_url column:', err.message);
        }
      }
      
      // Add newsletter_enabled if it doesn't exist
      if (!existingColumns.includes('newsletter_enabled')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN newsletter_enabled BOOLEAN DEFAULT FALSE AFTER app_download_url
          `);
          console.log('Added newsletter_enabled column');
        } catch (err) {
          console.warn('Error adding newsletter_enabled column:', err.message);
        }
      }
      
      // Add menu_image_url and menu_image_path if they don't exist
      if (!existingColumns.includes('menu_image_url')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN menu_image_url TEXT AFTER menu_items
          `);
          console.log('Added menu_image_url column');
        } catch (err) {
          console.warn('Error adding menu_image_url column:', err.message);
        }
      }
      
      if (!existingColumns.includes('menu_image_path')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN menu_image_path VARCHAR(500) AFTER menu_image_url
          `);
          console.log('Added menu_image_path column');
        } catch (err) {
          console.warn('Error adding menu_image_path column:', err.message);
        }
      }
      
      // Add barcode_code if it doesn't exist
      if (!existingColumns.includes('barcode_code')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN barcode_code VARCHAR(50) UNIQUE AFTER is_published
          `);
          console.log('Added barcode_code column');
          
          // Create index for barcode_code
          try {
            await pool.execute(`
              CREATE INDEX idx_barcode_code ON restaurant_websites(barcode_code)
            `);
            console.log('Added barcode_code index');
          } catch (idxErr) {
            console.warn('Index may already exist:', idxErr.message);
          }
        } catch (err) {
          console.warn('Error adding barcode_code column:', err.message);
        }
      }

      // Add subdomain column if it doesn't exist
      if (!existingColumns.includes('subdomain')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN subdomain VARCHAR(100) UNIQUE AFTER barcode_code
          `);
          console.log('Added subdomain column');
          
          try {
            await pool.execute(`
              CREATE INDEX idx_subdomain ON restaurant_websites(subdomain)
            `);
            console.log('Added subdomain index');
          } catch (idxErr) {
            console.warn('Index may already exist:', idxErr.message);
          }
        } catch (err) {
          console.warn('Error adding subdomain column:', err.message);
        }
      }

      // Add custom_domain column if it doesn't exist
      if (!existingColumns.includes('custom_domain')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN custom_domain VARCHAR(255) UNIQUE AFTER subdomain
          `);
          console.log('Added custom_domain column');
          
          try {
            await pool.execute(`
              CREATE INDEX idx_custom_domain ON restaurant_websites(custom_domain)
            `);
            console.log('Added custom_domain index');
          } catch (idxErr) {
            console.warn('Index may already exist:', idxErr.message);
          }
        } catch (err) {
          console.warn('Error adding custom_domain column:', err.message);
        }
      }

      // Add domain_verified column if it doesn't exist
      if (!existingColumns.includes('domain_verified')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN domain_verified BOOLEAN DEFAULT FALSE AFTER custom_domain
          `);
          console.log('Added domain_verified column');
        } catch (err) {
          console.warn('Error adding domain_verified column:', err.message);
        }
      }

      // Add multilingual columns (restaurant_name_ar, description_ar, address_ar)
      if (!existingColumns.includes('restaurant_name_ar')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN restaurant_name_ar VARCHAR(255) NULL AFTER restaurant_name
          `);
          console.log('Added restaurant_name_ar column');
        } catch (err) {
          console.warn('Error adding restaurant_name_ar column:', err.message);
        }
      }
      if (!existingColumns.includes('description_ar')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN description_ar TEXT NULL AFTER description
          `);
          console.log('Added description_ar column');
        } catch (err) {
          console.warn('Error adding description_ar column:', err.message);
        }
      }
      if (!existingColumns.includes('address_ar')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN address_ar TEXT NULL AFTER address
          `);
          console.log('Added address_ar column');
        } catch (err) {
          console.warn('Error adding address_ar column:', err.message);
        }
      }

      // Add notification settings columns if they don't exist
      if (!existingColumns.includes('notifications_enabled')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN notifications_enabled BOOLEAN DEFAULT TRUE AFTER domain_verified
          `);
          console.log('Added notifications_enabled column');
        } catch (err) {
          console.warn('Error adding notifications_enabled column:', err.message);
        }
      }

      if (!existingColumns.includes('notification_email_enabled')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN notification_email_enabled BOOLEAN DEFAULT TRUE AFTER notifications_enabled
          `);
          console.log('Added notification_email_enabled column');
        } catch (err) {
          console.warn('Error adding notification_email_enabled column:', err.message);
        }
      }

      if (!existingColumns.includes('notification_sms_enabled')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN notification_sms_enabled BOOLEAN DEFAULT FALSE AFTER notification_email_enabled
          `);
          console.log('Added notification_sms_enabled column');
        } catch (err) {
          console.warn('Error adding notification_sms_enabled column:', err.message);
        }
      }

      if (!existingColumns.includes('notification_push_enabled')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN notification_push_enabled BOOLEAN DEFAULT FALSE AFTER notification_sms_enabled
          `);
          console.log('Added notification_push_enabled column');
        } catch (err) {
          console.warn('Error adding notification_push_enabled column:', err.message);
        }
      }

      if (!existingColumns.includes('notification_whatsapp_enabled')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN notification_whatsapp_enabled BOOLEAN DEFAULT FALSE AFTER notification_push_enabled
          `);
          console.log('Added notification_whatsapp_enabled column');
        } catch (err) {
          console.warn('Error adding notification_whatsapp_enabled column:', err.message);
        }
      }

      if (!existingColumns.includes('notification_email')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN notification_email VARCHAR(255) AFTER notification_whatsapp_enabled
          `);
          console.log('Added notification_email column');
        } catch (err) {
          console.warn('Error adding notification_email column:', err.message);
        }
      }

      // Add order type settings columns if they don't exist
      if (!existingColumns.includes('order_type_dine_in_enabled')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN order_type_dine_in_enabled BOOLEAN DEFAULT TRUE AFTER notification_email
          `);
          console.log('Added order_type_dine_in_enabled column');
        } catch (err) {
          console.warn('Error adding order_type_dine_in_enabled column:', err.message);
        }
      }

      if (!existingColumns.includes('order_type_pickup_enabled')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN order_type_pickup_enabled BOOLEAN DEFAULT TRUE AFTER order_type_dine_in_enabled
          `);
          console.log('Added order_type_pickup_enabled column');
        } catch (err) {
          console.warn('Error adding order_type_pickup_enabled column:', err.message);
        }
      }

      if (!existingColumns.includes('order_type_delivery_enabled')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN order_type_delivery_enabled BOOLEAN DEFAULT TRUE AFTER order_type_pickup_enabled
          `);
          console.log('Added order_type_delivery_enabled column');
        } catch (err) {
          console.warn('Error adding order_type_delivery_enabled column:', err.message);
        }
      }

      // Add delivery_fee column if it doesn't exist
      if (!existingColumns.includes('delivery_fee')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN delivery_fee DECIMAL(10, 2) DEFAULT 0.00 AFTER order_type_delivery_enabled
          `);
          console.log('Added delivery_fee column');
        } catch (err) {
          console.warn('Error adding delivery_fee column:', err.message);
        }
      }

      // Add delivery_time_min column if it doesn't exist (estimated delivery time range, minutes)
      if (!existingColumns.includes('delivery_time_min')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN delivery_time_min INT NULL AFTER delivery_fee
          `);
          console.log('Added delivery_time_min column');
        } catch (err) {
          console.warn('Error adding delivery_time_min column:', err.message);
        }
      }

      // Add delivery_time_max column if it doesn't exist
      if (!existingColumns.includes('delivery_time_max')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN delivery_time_max INT NULL AFTER delivery_time_min
          `);
          console.log('Added delivery_time_max column');
        } catch (err) {
          console.warn('Error adding delivery_time_max column:', err.message);
        }
      }

      // Add delivery_mode and delivery_company_id for "Delivery Company" option
      if (!existingColumns.includes('delivery_mode')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN delivery_mode ENUM('fixed_fee', 'delivery_company') DEFAULT 'fixed_fee' AFTER delivery_time_max
          `);
          console.log('Added delivery_mode column');
        } catch (err) {
          console.warn('Error adding delivery_mode column:', err.message);
        }
      }
      if (!existingColumns.includes('delivery_company_id')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN delivery_company_id INT NULL AFTER delivery_mode,
            ADD INDEX idx_restaurant_websites_delivery_company_id (delivery_company_id)
          `);
          console.log('Added delivery_company_id column to restaurant_websites');
        } catch (err) {
          console.warn('Error adding delivery_company_id column:', err.message);
        }
      }

      // Add tax_enabled column if it doesn't exist
      if (!existingColumns.includes('tax_enabled')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN tax_enabled BOOLEAN DEFAULT FALSE AFTER delivery_fee
          `);
          console.log('Added tax_enabled column');
        } catch (err) {
          console.warn('Error adding tax_enabled column:', err.message);
        }
      }

      // Add tax_rate column if it doesn't exist
      if (!existingColumns.includes('tax_rate')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN tax_rate DECIMAL(5, 2) DEFAULT 0.00 AFTER tax_enabled
          `);
          console.log('Added tax_rate column');
        } catch (err) {
          console.warn('Error adding tax_rate column:', err.message);
        }
      }

      // Add payment_methods column if it doesn't exist
      if (!existingColumns.includes('payment_methods')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN payment_methods JSON AFTER tax_rate
          `);
          console.log('Added payment_methods column');
        } catch (err) {
          console.warn('Error adding payment_methods column:', err.message);
        }
      }

      // Add default_language column if it doesn't exist
      if (!existingColumns.includes('default_language')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN default_language VARCHAR(10) DEFAULT 'en' AFTER payment_methods
          `);
          console.log('Added default_language column');
        } catch (err) {
          console.warn('Error adding default_language column:', err.message);
        }
      }

      // Add languages_enabled column if it doesn't exist
      if (!existingColumns.includes('languages_enabled')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN languages_enabled JSON AFTER default_language
          `);
          console.log('Added languages_enabled column');
        } catch (err) {
          console.warn('Error adding languages_enabled column:', err.message);
        }
      }

      // Add currency_code column if it doesn't exist
      if (!existingColumns.includes('currency_code')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN currency_code VARCHAR(10) DEFAULT 'USD' AFTER languages_enabled
          `);
          console.log('Added currency_code column');
        } catch (err) {
          console.warn('Error adding currency_code column:', err.message);
        }
      }

      // Add currency_symbol_position column if it doesn't exist
      if (!existingColumns.includes('currency_symbol_position')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN currency_symbol_position ENUM('before', 'after') DEFAULT 'before' AFTER currency_code
          `);
          console.log('Added currency_symbol_position column');
        } catch (err) {
          console.warn('Error adding currency_symbol_position column:', err.message);
        }
      }

      // Add latitude column if it doesn't exist
      if (!existingColumns.includes('latitude')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN latitude DECIMAL(10, 8) NULL AFTER address
          `);
          console.log('Added latitude column to restaurant_websites table');
        } catch (err) {
          console.warn('Error adding latitude column:', err.message);
        }
      }

      // Add longitude column if it doesn't exist
      if (!existingColumns.includes('longitude')) {
        try {
          await pool.execute(`
            ALTER TABLE restaurant_websites 
            ADD COLUMN longitude DECIMAL(11, 8) NULL AFTER latitude
          `);
          console.log('Added longitude column to restaurant_websites table');
        } catch (err) {
          console.warn('Error adding longitude column:', err.message);
        }
      }
    } catch (migrationError) {
      console.warn('Migration warning:', migrationError.message);
    }

    // Add device_token column to admins table if it doesn't exist
    try {
      const [adminColumns] = await pool.execute(`
        SELECT COLUMN_NAME 
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = ? 
        AND TABLE_NAME = 'admins'
      `, [process.env.MYSQL_DB || 'restaurant_websites']);
      
      const existingAdminColumns = adminColumns.map(col => col.COLUMN_NAME);
      
      if (!existingAdminColumns.includes('device_token')) {
        try {
          await pool.execute(`
            ALTER TABLE admins 
            ADD COLUMN device_token VARCHAR(500) NULL AFTER name
          `);
          console.log('Added device_token column to admins table');
        } catch (err) {
          console.warn('Error adding device_token column to admins:', err.message);
        }
      }

      if (!existingAdminColumns.includes('device_type')) {
        try {
          await pool.execute(`
            ALTER TABLE admins 
            ADD COLUMN device_type ENUM('android', 'ios') NULL AFTER device_token
          `);
          console.log('Added device_type column to admins table');
        } catch (err) {
          console.warn('Error adding device_type column to admins:', err.message);
        }
      }
    } catch (migrationError) {
      console.warn('Migration warning for admins table:', migrationError.message);
    }

    // Add multilingual columns to products table (name_ar, description_ar, category_ar)
    try {
      const [productColumns] = await pool.execute(`
        SELECT COLUMN_NAME 
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = ? 
        AND TABLE_NAME = 'products'
      `, [process.env.MYSQL_DB || 'restaurant_websites']);
      const existingProductColumns = productColumns.map(col => col.COLUMN_NAME);

      if (!existingProductColumns.includes('name_ar')) {
        try {
          await pool.execute(`ALTER TABLE products ADD COLUMN name_ar VARCHAR(255) NULL AFTER name`);
          console.log('Added name_ar column to products table');
        } catch (err) {
          console.warn('Error adding name_ar column to products:', err.message);
        }
      }
      if (!existingProductColumns.includes('description_ar')) {
        try {
          await pool.execute(`ALTER TABLE products ADD COLUMN description_ar TEXT NULL AFTER description`);
          console.log('Added description_ar column to products table');
        } catch (err) {
          console.warn('Error adding description_ar column to products:', err.message);
        }
      }
      if (!existingProductColumns.includes('category_ar')) {
        try {
          await pool.execute(`ALTER TABLE products ADD COLUMN category_ar VARCHAR(100) NULL AFTER category`);
          console.log('Added category_ar column to products table');
        } catch (err) {
          console.warn('Error adding category_ar column to products:', err.message);
        }
      }
    } catch (migrationError) {
      console.warn('Migration warning for products table:', migrationError.message);
    }

    // Add multilingual columns to product_addons table (name_ar, description_ar)
    try {
      const [addonColumns] = await pool.execute(`
        SELECT COLUMN_NAME 
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = ? 
        AND TABLE_NAME = 'product_addons'
      `, [process.env.MYSQL_DB || 'restaurant_websites']);
      const existingAddonColumns = addonColumns.map(col => col.COLUMN_NAME);

      if (!existingAddonColumns.includes('name_ar')) {
        try {
          await pool.execute(`ALTER TABLE product_addons ADD COLUMN name_ar VARCHAR(255) NULL AFTER name`);
          console.log('Added name_ar column to product_addons table');
        } catch (err) {
          console.warn('Error adding name_ar column to product_addons:', err.message);
        }
      }
      if (!existingAddonColumns.includes('description_ar')) {
        try {
          await pool.execute(`ALTER TABLE product_addons ADD COLUMN description_ar TEXT NULL AFTER description`);
          console.log('Added description_ar column to product_addons table');
        } catch (err) {
          console.warn('Error adding description_ar column to product_addons:', err.message);
        }
      }
    } catch (migrationError) {
      console.warn('Migration warning for product_addons table:', migrationError.message);
    }

    // Run migration to add order_type column to orders table
    try {
      const [orderColumns] = await pool.execute(`
        SELECT COLUMN_NAME 
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = ? 
        AND TABLE_NAME = 'orders'
      `, [process.env.MYSQL_DB || 'restaurant_websites']);
      
      const existingOrderColumns = orderColumns.map(col => col.COLUMN_NAME);
      
      // Add order_type if it doesn't exist
      if (!existingOrderColumns.includes('order_type')) {
        try {
          await pool.execute(`
            ALTER TABLE orders 
            ADD COLUMN order_type ENUM('dine_in', 'pickup', 'delivery') DEFAULT 'pickup' AFTER customer_address
          `);
          console.log('Added order_type column to orders table');
          
          // Add index for order_type
          await pool.execute(`
            ALTER TABLE orders 
            ADD INDEX idx_order_type (order_type)
          `);
          console.log('Added index for order_type');
        } catch (err) {
          console.warn('Error adding order_type column:', err.message);
        }
      }

      // Add payment_intent_id column if it doesn't exist
      if (!existingOrderColumns.includes('payment_intent_id')) {
        try {
          await pool.execute(`
            ALTER TABLE orders 
            ADD COLUMN payment_intent_id VARCHAR(255) AFTER payment_method
          `);
          console.log('Added payment_intent_id column to orders table');
        } catch (err) {
          console.warn('Error adding payment_intent_id column:', err.message);
        }
      }

      // Add coupon_code column if it doesn't exist
      if (!existingOrderColumns.includes('coupon_code')) {
        try {
          await pool.execute(`
            ALTER TABLE orders 
            ADD COLUMN coupon_code VARCHAR(50) NULL AFTER payment_intent_id
          `);
          console.log('Added coupon_code column to orders table');
        } catch (err) {
          console.warn('Error adding coupon_code column:', err.message);
        }
      }

      // Add delivery location columns to orders table
      if (!existingOrderColumns.includes('delivery_latitude')) {
        try {
          await pool.execute(`
            ALTER TABLE orders 
            ADD COLUMN delivery_latitude DECIMAL(10, 8) NULL AFTER customer_address
          `);
          console.log('Added delivery_latitude column to orders table');
        } catch (err) {
          console.warn('Error adding delivery_latitude column:', err.message);
        }
      }

      if (!existingOrderColumns.includes('delivery_longitude')) {
        try {
          await pool.execute(`
            ALTER TABLE orders 
            ADD COLUMN delivery_longitude DECIMAL(11, 8) NULL AFTER delivery_latitude
          `);
          console.log('Added delivery_longitude column to orders table');
        } catch (err) {
          console.warn('Error adding delivery_longitude column:', err.message);
        }
      }

      // Update status ENUM to include 'picked_up' if it doesn't already exist
      try {
        const [statusInfo] = await pool.execute(`
          SELECT COLUMN_TYPE 
          FROM INFORMATION_SCHEMA.COLUMNS 
          WHERE TABLE_SCHEMA = ? 
          AND TABLE_NAME = 'orders' 
          AND COLUMN_NAME = 'status'
        `, [process.env.MYSQL_DB || 'restaurant_websites']);
        
        if (statusInfo.length > 0) {
          const currentEnum = statusInfo[0].COLUMN_TYPE;
          if (!currentEnum.includes('picked_up')) {
            await pool.execute(`
              ALTER TABLE orders 
              MODIFY COLUMN status ENUM('pending', 'confirmed', 'preparing', 'ready', 'picked_up', 'completed', 'cancelled') DEFAULT 'pending'
            `);
            console.log('Added picked_up status to orders table');
          }
        }
      } catch (err) {
        console.warn('Error updating status ENUM:', err.message);
      }
    } catch (migrationError) {
      console.warn('Migration warning for orders table:', migrationError.message);
    }

    // Create restaurant_applications table if it doesn't exist
    try {
      await pool.execute(`
        CREATE TABLE IF NOT EXISTS restaurant_applications (
          id INT AUTO_INCREMENT PRIMARY KEY,
          restaurant_name VARCHAR(255) NOT NULL,
          owner_name VARCHAR(255) NOT NULL,
          email VARCHAR(255) NOT NULL,
          phone VARCHAR(50) NOT NULL,
          address TEXT NOT NULL,
          cuisine_type VARCHAR(100),
          description TEXT NOT NULL,
          website_url VARCHAR(500),
          status ENUM('pending', 'approved', 'rejected') DEFAULT 'pending',
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          INDEX idx_email (email),
          INDEX idx_status (status),
          INDEX idx_created_at (created_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
      `);
      console.log('Restaurant applications table ready');
    } catch (err) {
      console.warn('Error creating restaurant_applications table:', err.message);
    }

    // Create super_admins table if it doesn't exist
    try {
      await pool.execute(`
        CREATE TABLE IF NOT EXISTS super_admins (
          id INT AUTO_INCREMENT PRIMARY KEY,
          name VARCHAR(255) NOT NULL,
          email VARCHAR(255) UNIQUE NOT NULL,
          password_hash VARCHAR(255) NOT NULL,
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          INDEX idx_email (email)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
      `);
      console.log('Super admins table ready');
      
      // Create default super admin if none exists (password: admin123)
      const [existing] = await pool.execute('SELECT id FROM super_admins LIMIT 1');
      if (existing.length === 0) {
        const bcrypt = (await import('bcryptjs')).default;
        const defaultPassword = await bcrypt.hash('admin123', 10);
        await pool.execute(
          'INSERT INTO super_admins (name, email, password_hash) VALUES (?, ?, ?)',
          ['Super Admin', 'admin@restaurantaai.com', defaultPassword]
        );
        console.log('Default super admin created: admin@restaurantaai.com / admin123');
      }
    } catch (err) {
      console.warn('Error creating super_admins table:', err.message);
    }

    // Create coupons table if it doesn't exist
    try {
      await pool.execute(`
        CREATE TABLE IF NOT EXISTS coupons (
          id INT AUTO_INCREMENT PRIMARY KEY,
          website_id INT NOT NULL,
          code VARCHAR(50) NOT NULL,
          description TEXT,
          discount_type ENUM('percentage', 'fixed') NOT NULL DEFAULT 'percentage',
          discount_value DECIMAL(10, 2) NOT NULL,
          min_order_amount DECIMAL(10, 2) DEFAULT 0.00,
          max_discount_amount DECIMAL(10, 2) NULL,
          valid_from DATE NOT NULL,
          valid_until DATE NOT NULL,
          usage_limit INT NULL,
          usage_count INT DEFAULT 0,
          is_active BOOLEAN DEFAULT TRUE,
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          FOREIGN KEY (website_id) REFERENCES restaurant_websites(id) ON DELETE CASCADE,
          INDEX idx_website_id (website_id),
          INDEX idx_code (code),
          INDEX idx_valid_dates (valid_from, valid_until),
          INDEX idx_is_active (is_active),
          UNIQUE KEY unique_website_code (website_id, code)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
      `);
      console.log('Coupons table ready');
    } catch (err) {
      console.warn('Error creating coupons table:', err.message);
    }

    // Create offers table if it doesn't exist (promotions: free delivery, % off, min order, etc.)
    try {
      await pool.execute(`
        CREATE TABLE IF NOT EXISTS offers (
          id INT AUTO_INCREMENT PRIMARY KEY,
          website_id INT NOT NULL,
          offer_type ENUM('free_delivery', 'free_delivery_over_x_jod', 'percent_off', 'minimum_order_value') NOT NULL,
          title VARCHAR(255) NOT NULL,
          description TEXT,
          value DECIMAL(10, 2) NULL,
          min_order_value DECIMAL(10, 2) NULL,
          is_active BOOLEAN DEFAULT TRUE,
          valid_from DATE NOT NULL,
          valid_until DATE NOT NULL,
          display_order INT DEFAULT 0,
          offer_scope ENUM('all_items', 'selected_items') DEFAULT 'all_items',
          selected_product_ids JSON NULL,
          selected_addon_ids JSON NULL,
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          FOREIGN KEY (website_id) REFERENCES restaurant_websites(id) ON DELETE CASCADE,
          INDEX idx_website_id (website_id),
          INDEX idx_offer_type (offer_type),
          INDEX idx_valid_dates (valid_from, valid_until),
          INDEX idx_is_active (is_active)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
      `);
      console.log('Offers table ready');
    } catch (err) {
      console.warn('Error creating offers table:', err.message);
    }

    // Add offer_scope and selected ids columns to offers if missing (percent_off: selected items)
    try {
      const [cols] = await pool.execute("SHOW COLUMNS FROM offers LIKE 'offer_scope'");
      if (cols.length === 0) {
        await pool.execute(`ALTER TABLE offers
          ADD COLUMN offer_scope ENUM('all_items', 'selected_items') DEFAULT 'all_items' COMMENT 'For percent_off: all or selected items',
          ADD COLUMN selected_product_ids JSON NULL,
          ADD COLUMN selected_addon_ids JSON NULL`);
        console.log('Offers table: offer_scope columns added');
      }
    } catch (err) {
      console.warn('Error altering offers table:', err.message);
    }

    // Create addresses table if it doesn't exist (multiple addresses per customer)
    try {
      await pool.execute(`
        CREATE TABLE IF NOT EXISTS addresses (
          id INT AUTO_INCREMENT PRIMARY KEY,
          customer_id INT NOT NULL,
          area VARCHAR(255) NULL,
          region_id INT NULL COMMENT 'Region ID (FK to regions)',
          region_name VARCHAR(255) NULL COMMENT 'Region name (text value)',
          area_id INT NULL COMMENT 'Area ID (FK to areas)',
          area_name VARCHAR(255) NULL COMMENT 'Area name (text value)',
          zone_id INT NULL COMMENT 'Zone ID (FK to delivery_zones)',
          zone_name VARCHAR(255) NULL COMMENT 'Zone name (text value)',
          zone_price DECIMAL(10, 2) NULL COMMENT 'Zone delivery price',
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
          FOREIGN KEY (region_id) REFERENCES regions(id) ON DELETE SET NULL,
          FOREIGN KEY (area_id) REFERENCES areas(id) ON DELETE SET NULL,
          FOREIGN KEY (zone_id) REFERENCES delivery_zones(id) ON DELETE SET NULL,
          INDEX idx_customer_id (customer_id),
          INDEX idx_is_default (is_default),
          INDEX idx_region_id (region_id),
          INDEX idx_area_id (area_id),
          INDEX idx_zone_id (zone_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
      `);
      console.log('Addresses table ready');
    } catch (err) {
      console.warn('Error creating addresses table:', err.message);
    }

    // Create customer_notifications table if it doesn't exist
    try {
      await pool.execute(`
        CREATE TABLE IF NOT EXISTS customer_notifications (
          id INT AUTO_INCREMENT PRIMARY KEY,
          customer_id INT NULL,
          order_id INT NULL,
          website_id INT NULL,
          title VARCHAR(255) NOT NULL,
          message TEXT NOT NULL,
          type VARCHAR(50) DEFAULT 'order_update',
          status VARCHAR(50) NULL,
          is_read BOOLEAN DEFAULT FALSE,
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
          FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
          FOREIGN KEY (website_id) REFERENCES restaurant_websites(id) ON DELETE CASCADE,
          INDEX idx_customer_id (customer_id),
          INDEX idx_order_id (order_id),
          INDEX idx_website_id (website_id),
          INDEX idx_is_read (is_read),
          INDEX idx_created_at (created_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
      `);
      console.log('Customer notifications table ready');
      
      // Add status column if it doesn't exist (migration)
      try {
        const [columns] = await pool.execute(`
          SELECT COLUMN_NAME 
          FROM INFORMATION_SCHEMA.COLUMNS 
          WHERE TABLE_SCHEMA = ? 
          AND TABLE_NAME = 'customer_notifications'
          AND COLUMN_NAME = 'status'
        `, [process.env.MYSQL_DB || 'restaurant_websites']);
        
        if (columns.length === 0) {
          await pool.execute(`
            ALTER TABLE customer_notifications 
            ADD COLUMN status VARCHAR(50) NULL AFTER type
          `);
          console.log('Added status column to customer_notifications table');
        }
      } catch (err) {
        console.warn('Error adding status column to customer_notifications table:', err.message);
      }
    } catch (err) {
      console.warn('Error creating customer_notifications table:', err.message);
    }

    // Add password_hash column to customers table if it doesn't exist (for app authentication)
    try {
      const [customerColumns] = await pool.execute(`
        SELECT COLUMN_NAME, IS_NULLABLE, COLUMN_DEFAULT
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = ? 
        AND TABLE_NAME = 'customers'
      `, [process.env.MYSQL_DB || 'restaurant_websites']);
      
      const existingCustomerColumns = customerColumns.map(col => col.COLUMN_NAME);
      
      // Make website_id nullable if it's currently NOT NULL (for app users who can order from any restaurant)
      const websiteIdColumn = customerColumns.find(col => col.COLUMN_NAME === 'website_id');
      if (websiteIdColumn && websiteIdColumn.IS_NULLABLE === 'NO') {
        try {
          // First, drop the foreign key constraint if it exists
          const [constraints] = await pool.execute(`
            SELECT CONSTRAINT_NAME 
            FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
            WHERE TABLE_SCHEMA = ? 
            AND TABLE_NAME = 'customers' 
            AND COLUMN_NAME = 'website_id'
            AND REFERENCED_TABLE_NAME IS NOT NULL
          `, [process.env.MYSQL_DB || 'restaurant_websites']);
          
          if (constraints.length > 0) {
            const constraintName = constraints[0].CONSTRAINT_NAME;
            await pool.execute(`ALTER TABLE customers DROP FOREIGN KEY ${constraintName}`);
          }
          
          // Make website_id nullable
          await pool.execute(`
            ALTER TABLE customers 
            MODIFY COLUMN website_id INT NULL
          `);
          
          // Re-add foreign key constraint (allows NULL)
          await pool.execute(`
            ALTER TABLE customers 
            ADD CONSTRAINT customers_website_id_fk 
            FOREIGN KEY (website_id) REFERENCES restaurant_websites(id) ON DELETE CASCADE
          `);
          
          console.log('Made website_id nullable in customers table');
        } catch (err) {
          console.warn('Error making website_id nullable:', err.message);
        }
      }
      
      if (!existingCustomerColumns.includes('password_hash')) {
        await pool.execute(`
          ALTER TABLE customers 
          ADD COLUMN password_hash VARCHAR(255) NULL AFTER address
        `);
        console.log('Added password_hash column to customers table');
      }

      // Add device token and location columns to customers table
      if (!existingCustomerColumns.includes('device_token')) {
        await pool.execute(`
          ALTER TABLE customers 
          ADD COLUMN device_token VARCHAR(500) NULL AFTER password_hash
        `);
        console.log('Added device_token column to customers table');
      }

      if (!existingCustomerColumns.includes('device_type')) {
        await pool.execute(`
          ALTER TABLE customers 
          ADD COLUMN device_type ENUM('android', 'ios') NULL AFTER device_token
        `);
        console.log('Added device_type column to customers table');
      }

      if (!existingCustomerColumns.includes('latitude')) {
        await pool.execute(`
          ALTER TABLE customers 
          ADD COLUMN latitude DECIMAL(10, 8) NULL AFTER device_type
        `);
        console.log('Added latitude column to customers table');
      }

      if (!existingCustomerColumns.includes('longitude')) {
        await pool.execute(`
          ALTER TABLE customers 
          ADD COLUMN longitude DECIMAL(11, 8) NULL AFTER latitude
        `);
        console.log('Added longitude column to customers table');
      }

      if (!existingCustomerColumns.includes('last_location_updated')) {
        await pool.execute(`
          ALTER TABLE customers 
          ADD COLUMN last_location_updated TIMESTAMP NULL AFTER longitude
        `);
        console.log('Added last_location_updated column to customers table');
      }

      if (!existingCustomerColumns.includes('last_token_updated')) {
        await pool.execute(`
          ALTER TABLE customers 
          ADD COLUMN last_token_updated TIMESTAMP NULL AFTER last_location_updated
        `);
        console.log('Added last_token_updated column to customers table');
      }

      // Add profile_picture_url column if it doesn't exist
      if (!existingCustomerColumns.includes('profile_picture_url')) {
        await pool.execute(`
          ALTER TABLE customers 
          ADD COLUMN profile_picture_url TEXT NULL AFTER address
        `);
        console.log('Added profile_picture_url column to customers table');
      }
    } catch (err) {
      console.warn('Error updating customers table:', err.message);
    }

    // Add original_price column to products table if it doesn't exist
    try {
      const [productColumns] = await pool.execute(`
        SELECT COLUMN_NAME 
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = ? 
        AND TABLE_NAME = 'products'
      `, [process.env.MYSQL_DB || 'restaurant_websites']);
      
      const existingProductColumns = productColumns.map(col => col.COLUMN_NAME);
      
      if (!existingProductColumns.includes('original_price')) {
        await pool.execute(`
          ALTER TABLE products 
          ADD COLUMN original_price DECIMAL(10, 2) NULL AFTER price
        `);
        console.log('Added original_price column to products table');
      }

      // Add-on feature: addon_required, addon_required_min (1=at least 1, 2=at least 2, -1=All; NULL when optional)
      if (!existingProductColumns.includes('addon_required')) {
        await pool.execute(`
          ALTER TABLE products 
          ADD COLUMN addon_required BOOLEAN DEFAULT FALSE AFTER is_available
        `);
        console.log('Added addon_required column to products table');
      }
      if (!existingProductColumns.includes('addon_required_min')) {
        await pool.execute(`
          ALTER TABLE products 
          ADD COLUMN addon_required_min INT NULL COMMENT 'When required: 1 2 3 or -1 for All. NULL when optional' AFTER addon_required
        `);
        console.log('Added addon_required_min column to products table');
      }
    } catch (err) {
      console.warn('Error updating products table:', err.message);
    }

    // Create product_addons table if it doesn't exist
    try {
      await pool.execute(`
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
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
      `);
      console.log('Product addons table ready');

      // Add is_required column to product_addons if it doesn't exist (migration for existing tables)
      const [addonColumns] = await pool.execute(`
        SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'product_addons'
      `, [process.env.MYSQL_DB || 'restaurant_websites']);
      const existingAddonColumns = addonColumns.map(c => c.COLUMN_NAME);
      if (!existingAddonColumns.includes('is_required')) {
        await pool.execute(`
          ALTER TABLE product_addons ADD COLUMN is_required BOOLEAN DEFAULT FALSE AFTER price
        `);
        console.log('Added is_required column to product_addons table');
      }
    } catch (err) {
      console.warn('Error creating product_addons table:', err.message);
    }

    // Add total_original_amount, tax, delivery_fees columns to orders table if they don't exist
    try {
      const [orderColumns] = await pool.execute(`
        SELECT COLUMN_NAME 
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = ? 
        AND TABLE_NAME = 'orders'
      `, [process.env.MYSQL_DB || 'restaurant_websites']);
      
      const existingOrderColumns = orderColumns.map(col => col.COLUMN_NAME);
      
      if (!existingOrderColumns.includes('total_original_amount')) {
        await pool.execute(`
          ALTER TABLE orders 
          ADD COLUMN total_original_amount DECIMAL(10, 2) DEFAULT 0.00 AFTER total_amount
        `);
        console.log('Added total_original_amount column to orders table');
      }
      
      if (!existingOrderColumns.includes('tax')) {
        await pool.execute(`
          ALTER TABLE orders 
          ADD COLUMN tax DECIMAL(10, 2) DEFAULT 0.00 AFTER total_original_amount
        `);
        console.log('Added tax column to orders table');
      }
      
      if (!existingOrderColumns.includes('delivery_fees')) {
        await pool.execute(`
          ALTER TABLE orders 
          ADD COLUMN delivery_fees DECIMAL(10, 2) DEFAULT 0.00 AFTER tax
        `);
        console.log('Added delivery_fees column to orders table');
      }

      if (!existingOrderColumns.includes('tip')) {
        await pool.execute(`
          ALTER TABLE orders 
          ADD COLUMN tip DECIMAL(10, 2) DEFAULT 0.00 AFTER notes
        `);
        console.log('Added tip column to orders table');
      }
      if (!existingOrderColumns.includes('delivery_instructions')) {
        await pool.execute(`
          ALTER TABLE orders 
          ADD COLUMN delivery_instructions VARCHAR(500) NULL AFTER tip
        `);
        console.log('Added delivery_instructions column to orders table');
      }
      if (!existingOrderColumns.includes('service_fee')) {
        await pool.execute(`
          ALTER TABLE orders 
          ADD COLUMN service_fee DECIMAL(10, 2) DEFAULT 0.00 AFTER delivery_instructions
        `);
        console.log('Added service_fee column to orders table');
      }
    } catch (err) {
      console.warn('Error updating orders table:', err.message);
    }

    // Create settings table if it doesn't exist (global: service fee for all restaurants; one active row only)
    try {
      await pool.execute(`
        CREATE TABLE IF NOT EXISTS settings (
          id INT AUTO_INCREMENT PRIMARY KEY,
          service_fee DECIMAL(10, 2) DEFAULT 0.00,
          status ENUM('active', 'inactive') NOT NULL DEFAULT 'inactive',
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          INDEX idx_status (status)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
      `);
      console.log('Settings table ready');
    } catch (err) {
      console.warn('Error creating settings table:', err.message);
    }

    // Add status column to settings if it doesn't exist (migration for existing tables)
    try {
      const [settingsCols] = await pool.execute(`
        SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'settings' AND COLUMN_NAME = 'status'
      `);
      if (settingsCols.length === 0) {
        await pool.execute(`
          ALTER TABLE settings ADD COLUMN status ENUM('active', 'inactive') NOT NULL DEFAULT 'inactive' AFTER service_fee,
          ADD INDEX idx_status (status)
        `);
        console.log('Added status column to settings table');
      }
    } catch (err) {
      console.warn('Error adding status to settings:', err.message);
    }

    // Triggers: when a row is set to 'active', all other rows become 'inactive' (one and only one active row)
    try {
      await pool.execute(`DROP TRIGGER IF EXISTS settings_only_one_active_ins`);
      await pool.execute(`
        CREATE TRIGGER settings_only_one_active_ins AFTER INSERT ON settings FOR EACH ROW
        UPDATE settings SET status = 'inactive' WHERE id != NEW.id AND NEW.status = 'active'
      `);
      await pool.execute(`DROP TRIGGER IF EXISTS settings_only_one_active_upd`);
      await pool.execute(`
        CREATE TRIGGER settings_only_one_active_upd AFTER UPDATE ON settings FOR EACH ROW
        UPDATE settings SET status = 'inactive' WHERE id != NEW.id AND NEW.status = 'active'
      `);
      console.log('Settings triggers: only one active row enforced');
    } catch (err) {
      console.warn('Error creating settings triggers:', err.message);
    }

    // Create business_hours table if it doesn't exist
    try {
      await pool.execute(`
        CREATE TABLE IF NOT EXISTS business_hours (
          id INT AUTO_INCREMENT PRIMARY KEY,
          website_id INT NOT NULL,
          day_of_week TINYINT NOT NULL COMMENT '0=Sunday, 1=Monday, ..., 6=Saturday',
          open_time TIME NULL,
          close_time TIME NULL,
          is_closed BOOLEAN NOT NULL DEFAULT TRUE,
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          UNIQUE KEY uk_website_day (website_id, day_of_week),
          FOREIGN KEY (website_id) REFERENCES restaurant_websites(id) ON DELETE CASCADE,
          INDEX idx_website_id (website_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
      `);
      console.log('Business hours table ready');
    } catch (err) {
      console.warn('Error creating business_hours table:', err.message);
    }

    // Create drivers table if it doesn't exist
    try {
      await pool.execute(`
        CREATE TABLE IF NOT EXISTS drivers (
          id INT AUTO_INCREMENT PRIMARY KEY,
          name VARCHAR(255) NOT NULL,
          email VARCHAR(255) UNIQUE NOT NULL,
          password_hash VARCHAR(255) NOT NULL,
          phone VARCHAR(50),
          is_online BOOLEAN DEFAULT FALSE,
          status ENUM('pending', 'approved', 'rejected') DEFAULT 'pending',
          latitude DECIMAL(10, 8),
          longitude DECIMAL(11, 8),
          device_token VARCHAR(500),
          device_type ENUM('android', 'ios'),
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          INDEX idx_email (email),
          INDEX idx_is_online (is_online),
          INDEX idx_status (status),
          INDEX idx_location (latitude, longitude)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
      `);
      console.log('Drivers table ready');
    } catch (err) {
      console.warn('Error creating drivers table:', err.message);
    }

    // Add status column to drivers table if it doesn't exist
    try {
      const [driverColumns] = await pool.execute(`
        SELECT COLUMN_NAME 
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = ? 
        AND TABLE_NAME = 'drivers'
        AND COLUMN_NAME = 'status'
      `, [process.env.MYSQL_DB || 'restaurant_websites']);
      
      if (driverColumns.length === 0) {
        await pool.execute(`
          ALTER TABLE drivers 
          ADD COLUMN status ENUM('pending', 'approved', 'rejected') DEFAULT 'pending' AFTER is_online,
          ADD INDEX idx_status (status)
        `);
        console.log('Added status column to drivers table');
      }
    } catch (err) {
      console.warn('Error adding status column to drivers table:', err.message);
    }

    // Add device_token and device_type columns to drivers table if they don't exist
    try {
      const [driverColumns] = await pool.execute(`
        SELECT COLUMN_NAME 
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = ? 
        AND TABLE_NAME = 'drivers'
        AND COLUMN_NAME IN ('device_token', 'device_type')
      `, [process.env.MYSQL_DB || 'restaurant_websites']);
      
      const existingColumns = driverColumns.map(col => col.COLUMN_NAME);
      
      if (!existingColumns.includes('device_token')) {
        await pool.execute(`
          ALTER TABLE drivers 
          ADD COLUMN device_token VARCHAR(500) NULL AFTER longitude
        `);
        console.log('Added device_token column to drivers table');
      }
      
      if (!existingColumns.includes('device_type')) {
        await pool.execute(`
          ALTER TABLE drivers 
          ADD COLUMN device_type ENUM('android', 'ios') NULL AFTER device_token
        `);
        console.log('Added device_type column to drivers table');
      }
    } catch (err) {
      console.warn('Error adding device token columns to drivers table:', err.message);
    }

    // Add driver_id column to orders table if it doesn't exist
    try {
      const [orderColumns] = await pool.execute(`
        SELECT COLUMN_NAME 
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = ? 
        AND TABLE_NAME = 'orders'
        AND COLUMN_NAME = 'driver_id'
      `, [process.env.MYSQL_DB || 'restaurant_websites']);
      
      if (orderColumns.length === 0) {
        await pool.execute(`
          ALTER TABLE orders 
          ADD COLUMN driver_id INT NULL AFTER customer_id,
          ADD FOREIGN KEY (driver_id) REFERENCES drivers(id) ON DELETE SET NULL,
          ADD INDEX idx_driver_id (driver_id)
        `);
        console.log('Added driver_id column to orders table');
      }
    } catch (err) {
      console.warn('Error adding driver_id column to orders table:', err.message);
    }

    // Add driver-specific order statuses if needed
    // The existing status enum should work, but we may need to add: arrived_at_pickup, picked_up, on_the_way
    // For now, we'll use the existing statuses and map them appropriately
    
    // Create payment_methods table for storing saved credit cards
    try {
      await pool.execute(`
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
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
      `);
      console.log('Created payment_methods table');
    } catch (err) {
      console.warn('Error creating payment_methods table:', err.message);
    }
    
    // Create delivery_companies table if it doesn't exist
    try {
      await pool.execute(`
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
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
      `);
      console.log('Created delivery_companies table');
    } catch (err) {
      console.warn('Error creating delivery_companies table:', err.message);
    }

    // Create cities and areas before delivery_zones (delivery_zones.area_id references areas.id)
    try {
      await pool.execute(`
        CREATE TABLE IF NOT EXISTS cities (
          id INT AUTO_INCREMENT PRIMARY KEY,
          name VARCHAR(255) NOT NULL COMMENT 'City name (e.g. English)',
          name_ar VARCHAR(255) NULL COMMENT 'City name in Arabic',
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          INDEX idx_cities_name (name)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
      `);
      console.log('Created cities table');
    } catch (err) {
      console.warn('Error creating cities table:', err.message);
    }
    try {
      await pool.execute(`
        CREATE TABLE IF NOT EXISTS areas (
          id INT AUTO_INCREMENT PRIMARY KEY,
          region_id INT NOT NULL COMMENT 'Region this area belongs to (FK to regions)',
          name VARCHAR(255) NOT NULL COMMENT 'Area name (e.g. English)',
          name_ar VARCHAR(255) NULL COMMENT 'Area name in Arabic',
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          FOREIGN KEY (region_id) REFERENCES regions(id) ON DELETE CASCADE,
          INDEX idx_areas_region_id (region_id),
          INDEX idx_areas_name (name)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
      `);
      console.log('Created areas table');
    } catch (err) {
      console.warn('Error creating areas table:', err.message);
    }

    // Create regions table (regions belong to a city)
    try {
      await pool.execute(`
        CREATE TABLE IF NOT EXISTS regions (
          id INT AUTO_INCREMENT PRIMARY KEY,
          city_id INT NOT NULL COMMENT 'City this region belongs to (FK to cities)',
          name VARCHAR(255) NOT NULL COMMENT 'Region name (e.g. English)',
          name_ar VARCHAR(255) NULL COMMENT 'Region name in Arabic',
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          FOREIGN KEY (city_id) REFERENCES cities(id) ON DELETE CASCADE,
          INDEX idx_regions_city_id (city_id),
          INDEX idx_regions_name (name)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
      `);
      console.log('Created regions table');
    } catch (err) {
      console.warn('Error creating regions table:', err.message);
    }

    // Create restaurant_branches table (branches belong to a restaurant and are in a region)
    try {
      await pool.execute(`
        CREATE TABLE IF NOT EXISTS restaurant_branches (
          id INT AUTO_INCREMENT PRIMARY KEY,
          website_id INT NOT NULL COMMENT 'Restaurant website this branch belongs to (FK to restaurant_websites)',
          region_id INT NOT NULL COMMENT 'Region where this branch is located (FK to regions)',
          branch_number INT NOT NULL COMMENT 'Branch number/identifier (e.g., 1, 2, 3)',
          name VARCHAR(255) NULL COMMENT 'Optional branch name',
          name_ar VARCHAR(255) NULL COMMENT 'Optional branch name in Arabic',
          address TEXT NULL COMMENT 'Optional branch address',
          phone VARCHAR(50) NULL COMMENT 'Optional branch phone number',
          status ENUM('active', 'inactive') DEFAULT 'active',
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          FOREIGN KEY (website_id) REFERENCES restaurant_websites(id) ON DELETE CASCADE,
          FOREIGN KEY (region_id) REFERENCES regions(id) ON DELETE RESTRICT,
          INDEX idx_restaurant_branches_website_id (website_id),
          INDEX idx_restaurant_branches_region_id (region_id),
          UNIQUE KEY unique_website_branch_number (website_id, branch_number)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
      `);
      console.log('Created restaurant_branches table');
    } catch (err) {
      console.warn('Error creating restaurant_branches table:', err.message);
    }

    // Create delivery_zones_names table (zone names are normalized to avoid duplication)
    try {
      await pool.execute(`
        CREATE TABLE IF NOT EXISTS delivery_zones_names (
          id INT AUTO_INCREMENT PRIMARY KEY,
          name_ar VARCHAR(255) NOT NULL COMMENT 'Zone name in Arabic',
          name_en VARCHAR(255) NOT NULL COMMENT 'Zone name in English',
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          INDEX idx_name_ar (name_ar),
          INDEX idx_name_en (name_en)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
      `);
      console.log('Created delivery_zones_names table');
    } catch (err) {
      console.warn('Error creating delivery_zones_names table:', err.message);
    }

    // Create delivery_zones table if it doesn't exist (zones are per area, not per restaurant)
    try {
      await pool.execute(`
        CREATE TABLE IF NOT EXISTS delivery_zones (
          id INT AUTO_INCREMENT PRIMARY KEY,
          delivery_company_id INT NOT NULL,
          area_id INT NOT NULL COMMENT 'Area this zone serves (FK to areas)',
          zone_name_id INT NOT NULL COMMENT 'Zone name (FK to delivery_zones_names)',
          price DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'Delivery price for this zone',
          status ENUM('active', 'inactive') DEFAULT 'active',
          image_url TEXT,
          image_path VARCHAR(500),
          note TEXT,
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          FOREIGN KEY (delivery_company_id) REFERENCES delivery_companies(id) ON DELETE CASCADE,
          FOREIGN KEY (area_id) REFERENCES areas(id) ON DELETE CASCADE,
          FOREIGN KEY (zone_name_id) REFERENCES delivery_zones_names(id) ON DELETE RESTRICT,
          INDEX idx_delivery_company_id (delivery_company_id),
          INDEX idx_area_id (area_id),
          INDEX idx_zone_name_id (zone_name_id),
          INDEX idx_status (status)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
      `);
      console.log('Created delivery_zones table');
    } catch (err) {
      console.warn('Error creating delivery_zones table:', err.message);
    }

    // Create store_delivery_requests (restaurant requests to work with a delivery company)
    try {
      await pool.execute(`
        CREATE TABLE IF NOT EXISTS store_delivery_requests (
          id INT AUTO_INCREMENT PRIMARY KEY,
          website_id INT NOT NULL,
          delivery_company_id INT NOT NULL,
          status ENUM('pending', 'approved', 'rejected') DEFAULT 'pending',
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          FOREIGN KEY (website_id) REFERENCES restaurant_websites(id) ON DELETE CASCADE,
          FOREIGN KEY (delivery_company_id) REFERENCES delivery_companies(id) ON DELETE CASCADE,
          INDEX idx_store_requests_website (website_id),
          INDEX idx_store_requests_company (delivery_company_id),
          INDEX idx_store_requests_status (status),
          UNIQUE KEY unique_website_company (website_id, delivery_company_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
      `);
      console.log('Created store_delivery_requests table');
    } catch (err) {
      console.warn('Error creating store_delivery_requests table:', err.message);
    }

    // Create orders_delivery (restaurant requests a driver for a zone)
    try {
      await pool.execute(`
        CREATE TABLE IF NOT EXISTS orders_delivery (
          id INT AUTO_INCREMENT PRIMARY KEY,
          website_id INT NOT NULL,
          zone_id INT NOT NULL,
          zone_name VARCHAR(255) NOT NULL,
          status ENUM('pending', 'assigned', 'completed', 'cancelled') DEFAULT 'pending',
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          FOREIGN KEY (website_id) REFERENCES restaurant_websites(id) ON DELETE CASCADE,
          FOREIGN KEY (zone_id) REFERENCES delivery_zones(id) ON DELETE CASCADE,
          INDEX idx_orders_delivery_website (website_id),
          INDEX idx_orders_delivery_zone (zone_id),
          INDEX idx_orders_delivery_status (status)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
      `);
      console.log('Created orders_delivery table');
    } catch (err) {
      console.warn('Error creating orders_delivery table:', err.message);
    }

    // Add area_id to restaurant_websites if not present (for linking stores to areas / delivery zones)
    try {
      const dbName = process.env.MYSQL_DB || 'restaurant_websites';
      const [cols] = await pool.execute(`
        SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'restaurant_websites' AND COLUMN_NAME = 'area_id'
      `, [dbName]);
      if (cols.length === 0) {
        await pool.execute(`
          ALTER TABLE restaurant_websites
          ADD COLUMN area_id INT NULL COMMENT 'Area where this restaurant is located (for delivery zones)' AFTER id,
          ADD INDEX idx_restaurant_websites_area_id (area_id),
          ADD CONSTRAINT fk_restaurant_websites_area FOREIGN KEY (area_id) REFERENCES areas(id) ON DELETE SET NULL
        `);
        console.log('Added area_id to restaurant_websites');
      }
    } catch (err) {
      console.warn('Error adding area_id to restaurant_websites:', err.message);
    }

    // Add FK for restaurant_websites.delivery_company_id (after delivery_companies exists)
    try {
      const dbName = process.env.MYSQL_DB || 'restaurant_websites';
      const [fkExists] = await pool.execute(`
        SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'restaurant_websites'
        AND CONSTRAINT_TYPE = 'FOREIGN KEY' AND CONSTRAINT_NAME = 'fk_restaurant_websites_delivery_company'
      `, [dbName]);
      if (fkExists.length === 0) {
        const [colExists] = await pool.execute(`
          SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
          WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'restaurant_websites' AND COLUMN_NAME = 'delivery_company_id'
        `, [dbName]);
        if (colExists.length > 0) {
          await pool.execute(`
            ALTER TABLE restaurant_websites
            ADD CONSTRAINT fk_restaurant_websites_delivery_company
            FOREIGN KEY (delivery_company_id) REFERENCES delivery_companies(id) ON DELETE SET NULL
          `);
          console.log('Added FK fk_restaurant_websites_delivery_company');
        }
      }
    } catch (err) {
      console.warn('Error adding restaurant_websites delivery_company FK:', err.message);
    }

    // Add admin credentials to delivery_companies if not present
    try {
      const dbName = process.env.MYSQL_DB || 'restaurant_websites';
      const [dcCols] = await pool.execute(`
        SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'delivery_companies'
        AND COLUMN_NAME IN ('admin_username', 'admin_password_hash')
      `, [dbName]);
      const hasAdminUsername = dcCols.some(c => c.COLUMN_NAME === 'admin_username');
      const hasAdminPassword = dcCols.some(c => c.COLUMN_NAME === 'admin_password_hash');
      if (!hasAdminUsername) {
        await pool.execute(`
          ALTER TABLE delivery_companies
          ADD COLUMN admin_username VARCHAR(255) NULL UNIQUE COMMENT 'Login username for delivery company admin'
        `);
        console.log('Added admin_username to delivery_companies');
      }
      if (!hasAdminPassword) {
        await pool.execute(`
          ALTER TABLE delivery_companies
          ADD COLUMN admin_password_hash VARCHAR(255) NULL COMMENT 'Bcrypt hash of admin password'
        `);
        console.log('Added admin_password_hash to delivery_companies');
      }
    } catch (err) {
      console.warn('Error adding delivery company admin columns:', err.message);
    }

    // Add delivery_company_id to drivers if not present
    try {
      const dbName = process.env.MYSQL_DB || 'restaurant_websites';
      const [driverCols] = await pool.execute(`
        SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'drivers'
        AND COLUMN_NAME = 'delivery_company_id'
      `, [dbName]);
      if (driverCols.length === 0) {
        await pool.execute(`
          ALTER TABLE drivers
          ADD COLUMN delivery_company_id INT NULL COMMENT 'Company this driver/captain belongs to',
          ADD INDEX idx_drivers_delivery_company_id (delivery_company_id)
        `);
        await pool.execute(`
          ALTER TABLE drivers
          ADD CONSTRAINT fk_drivers_delivery_company
          FOREIGN KEY (delivery_company_id) REFERENCES delivery_companies(id) ON DELETE SET NULL
        `);
        console.log('Added delivery_company_id to drivers');
      }
    } catch (err) {
      console.warn('Error adding delivery_company_id to drivers:', err.message);
    }
    
    // Add region_id, area_id, zone_id and text columns to addresses if not present
    try {
      const dbName = process.env.MYSQL_DB || 'restaurant_websites';
      const [addressCols] = await pool.execute(`
        SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'addresses'
      `, [dbName]);
      const existingAddressColumns = addressCols.map(col => col.COLUMN_NAME);
      
      // Add region_id if it doesn't exist
      if (!existingAddressColumns.includes('region_id')) {
        await pool.execute(`
          ALTER TABLE addresses
          ADD COLUMN region_id INT NULL COMMENT 'Region ID (FK to regions)' AFTER area,
          ADD INDEX idx_region_id (region_id)
        `);
        console.log('Added region_id to addresses');
      }
      
      // Add region_name if it doesn't exist
      if (!existingAddressColumns.includes('region_name')) {
        await pool.execute(`
          ALTER TABLE addresses
          ADD COLUMN region_name VARCHAR(255) NULL COMMENT 'Region name (text value)' AFTER region_id
        `);
        console.log('Added region_name to addresses');
      }
      
      // Add area_id if it doesn't exist
      if (!existingAddressColumns.includes('area_id')) {
        await pool.execute(`
          ALTER TABLE addresses
          ADD COLUMN area_id INT NULL COMMENT 'Area ID (FK to areas)' AFTER region_name,
          ADD INDEX idx_area_id (area_id)
        `);
        console.log('Added area_id to addresses');
      }
      
      // Add area_name if it doesn't exist
      if (!existingAddressColumns.includes('area_name')) {
        await pool.execute(`
          ALTER TABLE addresses
          ADD COLUMN area_name VARCHAR(255) NULL COMMENT 'Area name (text value)' AFTER area_id
        `);
        console.log('Added area_name to addresses');
      }
      
      // Add zone_id if it doesn't exist
      if (!existingAddressColumns.includes('zone_id')) {
        await pool.execute(`
          ALTER TABLE addresses
          ADD COLUMN zone_id INT NULL COMMENT 'Zone ID (FK to delivery_zones)' AFTER area_name,
          ADD INDEX idx_zone_id (zone_id)
        `);
        console.log('Added zone_id to addresses');
      }
      
      // Add zone_name if it doesn't exist
      if (!existingAddressColumns.includes('zone_name')) {
        await pool.execute(`
          ALTER TABLE addresses
          ADD COLUMN zone_name VARCHAR(255) NULL COMMENT 'Zone name (text value)' AFTER zone_id
        `);
        console.log('Added zone_name to addresses');
      }
      
      // Add zone_price if it doesn't exist
      if (!existingAddressColumns.includes('zone_price')) {
        await pool.execute(`
          ALTER TABLE addresses
          ADD COLUMN zone_price DECIMAL(10, 2) NULL COMMENT 'Zone delivery price' AFTER zone_name
        `);
        console.log('Added zone_price to addresses');
      }
      
      // Add foreign key constraints if they don't exist
      const [constraints] = await pool.execute(`
        SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'addresses'
        AND CONSTRAINT_TYPE = 'FOREIGN KEY'
      `, [dbName]);
      const existingConstraints = constraints.map(c => c.CONSTRAINT_NAME);
      
      if (!existingConstraints.some(c => c.includes('region'))) {
        try {
          await pool.execute(`
            ALTER TABLE addresses
            ADD CONSTRAINT fk_addresses_region
            FOREIGN KEY (region_id) REFERENCES regions(id) ON DELETE SET NULL
          `);
          console.log('Added fk_addresses_region constraint');
        } catch (err) {
          console.warn('Error adding fk_addresses_region constraint:', err.message);
        }
      }
      
      if (!existingConstraints.some(c => c.includes('area') && !c.includes('region'))) {
        try {
          await pool.execute(`
            ALTER TABLE addresses
            ADD CONSTRAINT fk_addresses_area
            FOREIGN KEY (area_id) REFERENCES areas(id) ON DELETE SET NULL
          `);
          console.log('Added fk_addresses_area constraint');
        } catch (err) {
          console.warn('Error adding fk_addresses_area constraint:', err.message);
        }
      }
      
      if (!existingConstraints.some(c => c.includes('zone'))) {
        try {
          await pool.execute(`
            ALTER TABLE addresses
            ADD CONSTRAINT fk_addresses_zone
            FOREIGN KEY (zone_id) REFERENCES delivery_zones(id) ON DELETE SET NULL
          `);
          console.log('Added fk_addresses_zone constraint');
        } catch (err) {
          console.warn('Error adding fk_addresses_zone constraint:', err.message);
        }
      }
    } catch (err) {
      console.warn('Error adding region/area/zone columns to addresses:', err.message);
    }
    
    console.log('MySQL database schema initialized successfully');
    return true;
  } catch (error) {
    console.error('Error initializing MySQL database:', error);
    throw error;
  }
}

/**
 * Test MySQL database connection
 */
export async function testConnection() {
  try {
    const [rows] = await pool.execute('SELECT NOW() as now');
    console.log('MySQL database connected:', rows[0].now);
    return true;
  } catch (error) {
    console.error('MySQL database connection failed:', error);
    return false;
  }
}

