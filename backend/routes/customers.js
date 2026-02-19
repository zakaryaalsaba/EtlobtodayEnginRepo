import express from 'express';
import { pool } from '../db/init.js';
import { addCustomerConnection, removeCustomerConnection } from '../services/sseManager.js';
import jwt from 'jsonwebtoken';
import multer from 'multer';
import path from 'path';
import { fileURLToPath } from 'url';
import fs from 'fs';

const router = express.Router();
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Configure multer for profile picture uploads
const profilePictureStorage = multer.diskStorage({
  destination: (req, file, cb) => {
    const uploadDir = path.join(__dirname, '../uploads');
    if (!fs.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    cb(null, 'profile-' + uniqueSuffix + path.extname(file.originalname));
  }
});

const imageFilter = (req, file, cb) => {
  const allowedTypes = /jpeg|jpg|png|gif|webp/;
  const extname = allowedTypes.test(path.extname(file.originalname).toLowerCase());
  const mimetype = file.mimetype && allowedTypes.test(file.mimetype);
  
  console.log('File upload check:', {
    originalname: file.originalname,
    mimetype: file.mimetype,
    extname: extname,
    mimetypeValid: mimetype
  });
  
  if (mimetype && extname) {
    return cb(null, true);
  } else {
    console.error('File rejected:', {
      originalname: file.originalname,
      mimetype: file.mimetype,
      extname: extname,
      mimetypeValid: mimetype
    });
    cb(new Error('Only image files are allowed!'));
  }
};

const uploadProfilePicture = multer({
  storage: profilePictureStorage,
  limits: { fileSize: 5 * 1024 * 1024 }, // 5MB limit
  fileFilter: imageFilter
});

/**
 * POST /api/customers
 * Register a new customer or get existing customer
 */
router.post('/', async (req, res) => {
  try {
    const { website_id, name, email, phone, address } = req.body;

    // Validate required fields
    if (!website_id || !name || !phone) {
      return res.status(400).json({ error: 'Missing required fields: website_id, name, and phone are required' });
    }

    // Check if customer already exists (by email or phone)
    let customer = null;
    
    if (email) {
      const [existingByEmail] = await pool.execute(
        'SELECT * FROM customers WHERE website_id = ? AND email = ?',
        [website_id, email]
      );
      if (existingByEmail.length > 0) {
        customer = existingByEmail[0];
      }
    }

    // If not found by email, check by phone
    if (!customer) {
      const [existingByPhone] = await pool.execute(
        'SELECT * FROM customers WHERE website_id = ? AND phone = ?',
        [website_id, phone]
      );
      if (existingByPhone.length > 0) {
        customer = existingByPhone[0];
        
        // Update email if provided and not already set
        if (email && !customer.email) {
          await pool.execute(
            'UPDATE customers SET email = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
            [email, customer.id]
          );
          customer.email = email;
        }
      }
    }

    // If customer exists, update their info if needed
    if (customer) {
      // Update address if provided and different
      if (address && customer.address !== address) {
        await pool.execute(
          'UPDATE customers SET address = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
          [address, customer.id]
        );
        customer.address = address;
      }
      
      // Update name if different
      if (customer.name !== name) {
        await pool.execute(
          'UPDATE customers SET name = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
          [name, customer.id]
        );
        customer.name = name;
      }

      return res.json({ customer, isNew: false });
    }

    // Create new customer
    const [result] = await pool.execute(
      'INSERT INTO customers (website_id, name, email, phone, address) VALUES (?, ?, ?, ?, ?)',
      [website_id, name, email || null, phone, address || null]
    );

    const [newCustomer] = await pool.execute(
      'SELECT * FROM customers WHERE id = ?',
      [result.insertId]
    );

    res.status(201).json({ customer: newCustomer[0], isNew: true });
  } catch (error) {
    console.error('Error creating/updating customer:', error);
    res.status(500).json({ error: 'Failed to register customer', message: error.message });
  }
});

/**
 * POST /api/customers/:customerId/link-order
 * Link an existing order to a customer
 */
router.post('/:customerId/link-order', async (req, res) => {
  try {
    const { customerId } = req.params;
    const { order_id } = req.body;

    if (!order_id) {
      return res.status(400).json({ error: 'order_id is required' });
    }

    // Verify customer exists
    const [customers] = await pool.execute('SELECT * FROM customers WHERE id = ?', [customerId]);
    if (customers.length === 0) {
      return res.status(404).json({ error: 'Customer not found' });
    }

    // Update order with customer_id
    await pool.execute(
      'UPDATE orders SET customer_id = ? WHERE id = ?',
      [customerId, order_id]
    );

    res.json({ success: true, message: 'Order linked to customer successfully' });
  } catch (error) {
    console.error('Error linking order to customer:', error);
    res.status(500).json({ error: 'Failed to link order to customer', message: error.message });
  }
});

/**
 * GET /api/customers/:customerId/orders
 * Get all orders for a customer
 */
router.get('/:customerId/orders', async (req, res) => {
  try {
    const { customerId } = req.params;

    const [orders] = await pool.execute(
      `SELECT o.*, 
       (SELECT JSON_ARRAYAGG(
         JSON_OBJECT(
           'id', oi.id,
           'product_id', oi.product_id,
           'product_name', oi.product_name,
           'product_price', oi.product_price,
           'quantity', oi.quantity,
           'subtotal', oi.subtotal
         )
       ) FROM order_items oi WHERE oi.order_id = o.id) as items
       FROM orders o 
       WHERE o.customer_id = ?
       ORDER BY o.created_at DESC`,
      [customerId]
    );

    // Process items for each order
    orders.forEach(order => {
      try {
        if (order.items !== null && order.items !== undefined) {
          if (Array.isArray(order.items)) {
            // Already an array
          } else if (typeof order.items === 'string') {
            try {
              const parsed = JSON.parse(order.items);
              order.items = Array.isArray(parsed) ? parsed : (parsed ? [parsed] : []);
            } catch (e) {
              order.items = [];
            }
          } else if (Buffer.isBuffer(order.items)) {
            try {
              const parsed = JSON.parse(order.items.toString());
              order.items = Array.isArray(parsed) ? parsed : (parsed ? [parsed] : []);
            } catch (e) {
              order.items = [];
            }
          } else if (typeof order.items === 'object') {
            order.items = [order.items];
          } else {
            order.items = [];
          }
        } else {
          order.items = [];
        }
      } catch (e) {
        order.items = [];
      }
    });

    res.json({ orders });
  } catch (error) {
    console.error('Error fetching customer orders:', error);
    res.status(500).json({ error: 'Failed to fetch customer orders', message: error.message });
  }
});

/**
 * GET /api/customers/:customerId/notifications
 * Get all notifications for a customer
 */
router.get('/:customerId/notifications', async (req, res) => {
  try {
    const { customerId } = req.params;

    const [notifications] = await pool.execute(
      `SELECT 
        cn.*,
        rw.restaurant_name,
        o.order_number
      FROM customer_notifications cn
      LEFT JOIN restaurant_websites rw ON cn.website_id = rw.id
      LEFT JOIN orders o ON cn.order_id = o.id
      WHERE cn.customer_id = ?
      ORDER BY cn.created_at DESC
      LIMIT 100`,
      [customerId]
    );

    res.json({ notifications });
  } catch (error) {
    console.error('Error fetching customer notifications:', error);
    res.status(500).json({ error: 'Failed to fetch customer notifications', message: error.message });
  }
});

/**
 * PUT /api/customers/:customerId/notifications/:notificationId/read
 * Mark a notification as read
 */
router.put('/:customerId/notifications/:notificationId/read', async (req, res) => {
  try {
    const { customerId, notificationId } = req.params;

    // Verify notification belongs to customer
    const [notifications] = await pool.execute(
      'SELECT id FROM customer_notifications WHERE id = ? AND customer_id = ?',
      [notificationId, customerId]
    );

    if (notifications.length === 0) {
      return res.status(404).json({ error: 'Notification not found' });
    }

    await pool.execute(
      'UPDATE customer_notifications SET is_read = TRUE WHERE id = ?',
      [notificationId]
    );

    res.json({ success: true, message: 'Notification marked as read' });
  } catch (error) {
    console.error('Error marking notification as read:', error);
    res.status(500).json({ error: 'Failed to mark notification as read', message: error.message });
  }
});

/**
 * PUT /api/customers/:customerId/notifications/read-all
 * Mark all notifications as read for a customer
 */
router.put('/:customerId/notifications/read-all', async (req, res) => {
  try {
    const { customerId } = req.params;

    await pool.execute(
      'UPDATE customer_notifications SET is_read = TRUE WHERE customer_id = ? AND is_read = FALSE',
      [customerId]
    );

    res.json({ success: true, message: 'All notifications marked as read' });
  } catch (error) {
    console.error('Error marking all notifications as read:', error);
    res.status(500).json({ error: 'Failed to mark all notifications as read', message: error.message });
  }
});

/**
 * GET /api/customers/:customerId/orders/:orderId/stream
 * Server-Sent Events endpoint for real-time order tracking
 * Supports token via query parameter for EventSource compatibility
 */
router.get('/:customerId/orders/:orderId/stream', async (req, res, next) => {
  const { customerId, orderId } = req.params;
  const token = req.query.token || req.headers.authorization?.split(' ')[1];
  
  if (!token) {
    return res.status(401).json({ error: 'No token provided' });
  }

  try {
    const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key-change-in-production';
    const decoded = jwt.verify(token, JWT_SECRET);
    
    // Verify customer matches token
    if (decoded.customerId && parseInt(decoded.customerId) !== parseInt(customerId)) {
      return res.status(403).json({ error: 'Unauthorized' });
    }

    // Verify customer exists
    const [customers] = await pool.execute(
      'SELECT id FROM customers WHERE id = ?',
      [customerId]
    );

    if (customers.length === 0) {
      return res.status(404).json({ error: 'Customer not found' });
    }

    // Verify order belongs to customer
    const [orders] = await pool.execute(
      'SELECT id, customer_id FROM orders WHERE id = ? AND customer_id = ?',
      [orderId, customerId]
    );

    if (orders.length === 0) {
      return res.status(404).json({ error: 'Order not found or does not belong to customer' });
    }

    req.customerId = parseInt(customerId);
    req.orderId = parseInt(orderId);
    next();
  } catch (error) {
    console.error('Token verification error:', error);
    return       res.status(401).json({ error: 'Invalid token' });
  }
}, (req, res) => {
  const orderId = req.orderId;

  // Set headers for SSE
  res.setHeader('Content-Type', 'text/event-stream');
  res.setHeader('Cache-Control', 'no-cache');
  res.setHeader('Connection', 'keep-alive');
  res.setHeader('X-Accel-Buffering', 'no'); // Disable nginx buffering

  // Add CORS headers if needed
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Headers', 'Cache-Control');

  // Add this connection to the SSE manager
  addCustomerConnection(orderId, res);

  // Send a ping every 30 seconds to keep connection alive
  const pingInterval = setInterval(() => {
    try {
      res.write(`: ping\n\n`);
    } catch (error) {
      clearInterval(pingInterval);
      removeCustomerConnection(orderId, res);
    }
  }, 30000);

  // Clean up on client disconnect
  req.on('close', () => {
    clearInterval(pingInterval);
    removeCustomerConnection(orderId, res);
  });
});

/**
 * PUT /api/customers/:customerId/device-token
 * Update device token for push notifications
 */
router.put('/:customerId/device-token', async (req, res) => {
  try {
    const { customerId } = req.params;
    const { device_token, device_type } = req.body;

    if (!device_token) {
      return res.status(400).json({ error: 'device_token is required' });
    }

    if (device_type && !['android', 'ios'].includes(device_type)) {
      return res.status(400).json({ error: 'device_type must be "android" or "ios"' });
    }

    // Verify customer exists
    const [customers] = await pool.execute(
      'SELECT id FROM customers WHERE id = ?',
      [customerId]
    );

    if (customers.length === 0) {
      return res.status(404).json({ error: 'Customer not found' });
    }

    // Update device token
    await pool.execute(
      `UPDATE customers 
       SET device_token = ?, 
           device_type = ?,
           last_token_updated = CURRENT_TIMESTAMP 
       WHERE id = ?`,
      [device_token, device_type || 'android', customerId]
    );

    res.json({ 
      success: true, 
      message: 'Device token updated successfully' 
    });
  } catch (error) {
    console.error('Error updating device token:', error);
    res.status(500).json({ 
      error: 'Failed to update device token', 
      message: error.message 
    });
  }
});

/**
 * PUT /api/customers/:customerId/location
 * Update customer location
 */
router.put('/:customerId/location', async (req, res) => {
  try {
    const { customerId } = req.params;
    const { latitude, longitude, address } = req.body;

    if (latitude === undefined || longitude === undefined) {
      return res.status(400).json({ error: 'latitude and longitude are required' });
    }

    // Validate coordinates
    if (latitude < -90 || latitude > 90) {
      return res.status(400).json({ error: 'Invalid latitude. Must be between -90 and 90' });
    }

    if (longitude < -180 || longitude > 180) {
      return res.status(400).json({ error: 'Invalid longitude. Must be between -180 and 180' });
    }

    // Verify customer exists
    const [customers] = await pool.execute(
      'SELECT id FROM customers WHERE id = ?',
      [customerId]
    );

    if (customers.length === 0) {
      return res.status(404).json({ error: 'Customer not found' });
    }

    // Update location
    const updateFields = ['latitude = ?', 'longitude = ?', 'last_location_updated = CURRENT_TIMESTAMP'];
    const updateValues = [latitude, longitude];

    if (address !== undefined) {
      updateFields.push('address = ?');
      updateValues.push(address);
    }

    updateValues.push(customerId);

    await pool.execute(
      `UPDATE customers 
       SET ${updateFields.join(', ')} 
       WHERE id = ?`,
      updateValues
    );

    res.json({ 
      success: true, 
      message: 'Location updated successfully',
      location: { latitude, longitude, address: address || null }
    });
  } catch (error) {
    console.error('Error updating location:', error);
    res.status(500).json({ 
      error: 'Failed to update location', 
      message: error.message 
    });
  }
});

/**
 * GET /api/customers/:customerId/location
 * Get customer location (for drivers)
 */
router.get('/:customerId/location', async (req, res) => {
  try {
    const { customerId } = req.params;

    const [customers] = await pool.execute(
      'SELECT latitude, longitude, address, last_location_updated FROM customers WHERE id = ?',
      [customerId]
    );

    if (customers.length === 0) {
      return res.status(404).json({ error: 'Customer not found' });
    }

    const customer = customers[0];

    if (!customer.latitude || !customer.longitude) {
      return res.status(404).json({ error: 'Customer location not set' });
    }

    res.json({
      latitude: parseFloat(customer.latitude),
      longitude: parseFloat(customer.longitude),
      address: customer.address,
      last_location_updated: customer.last_location_updated
    });
  } catch (error) {
    console.error('Error fetching customer location:', error);
    res.status(500).json({ 
      error: 'Failed to fetch location', 
      message: error.message 
    });
  }
});

/**
 * GET /api/customers/:customerId
 * Get customer profile information
 */
router.get('/:customerId', async (req, res) => {
  try {
    const { customerId } = req.params;
    
    // Optional: Verify token if provided
    let authenticatedCustomerId = null;
    const authHeader = req.headers.authorization;
    if (authHeader && authHeader.startsWith('Bearer ')) {
      try {
        const token = authHeader.substring(7);
        const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key';
        const decoded = jwt.verify(token, JWT_SECRET);
        authenticatedCustomerId = decoded.customerId || decoded.id;
      } catch (err) {
        // Token invalid, but continue without authentication
        console.warn('Invalid token for customer profile request:', err.message);
      }
    }

    const [customers] = await pool.execute(
      'SELECT id, name, email, phone, address, profile_picture_url, created_at, updated_at FROM customers WHERE id = ?',
      [customerId]
    );

    if (customers.length === 0) {
      console.log(`Customer not found: ${customerId}`);
      return res.status(404).json({ error: 'Customer not found' });
    }

    // If authenticated, verify the customer ID matches
    if (authenticatedCustomerId && parseInt(customerId) !== authenticatedCustomerId) {
      return res.status(403).json({ error: 'Access denied' });
    }

    res.json({ customer: customers[0] });
  } catch (error) {
    console.error('Error fetching customer profile:', error);
    res.status(500).json({ 
      error: 'Failed to fetch customer profile', 
      message: error.message 
    });
  }
});

/**
 * PUT /api/customers/:customerId
 * Update customer profile information
 */
router.put('/:customerId', async (req, res) => {
  try {
    const { customerId } = req.params;
    const { name, email, phone, address, profile_picture_url } = req.body;

    // Verify customer exists
    const [customers] = await pool.execute(
      'SELECT id FROM customers WHERE id = ?',
      [customerId]
    );

    if (customers.length === 0) {
      return res.status(404).json({ error: 'Customer not found' });
    }

    // Build update query dynamically based on provided fields
    const updateFields = [];
    const updateValues = [];

    if (name !== undefined) {
      updateFields.push('name = ?');
      updateValues.push(name);
    }

    if (email !== undefined) {
      updateFields.push('email = ?');
      updateValues.push(email);
    }

    if (phone !== undefined) {
      updateFields.push('phone = ?');
      updateValues.push(phone);
    }

    if (address !== undefined) {
      updateFields.push('address = ?');
      updateValues.push(address);
    }

    if (profile_picture_url !== undefined) {
      updateFields.push('profile_picture_url = ?');
      updateValues.push(profile_picture_url);
    }

    if (updateFields.length === 0) {
      return res.status(400).json({ error: 'No fields to update' });
    }

    // Add updated_at
    updateFields.push('updated_at = CURRENT_TIMESTAMP');
    updateValues.push(customerId);

    await pool.execute(
      `UPDATE customers SET ${updateFields.join(', ')} WHERE id = ?`,
      updateValues
    );

    // Fetch updated customer
    const [updatedCustomers] = await pool.execute(
      'SELECT id, name, email, phone, address, profile_picture_url, created_at, updated_at FROM customers WHERE id = ?',
      [customerId]
    );

    res.json({ 
      success: true,
      customer: updatedCustomers[0],
      message: 'Customer profile updated successfully' 
    });
  } catch (error) {
    console.error('Error updating customer profile:', error);
    res.status(500).json({ 
      error: 'Failed to update customer profile', 
      message: error.message 
    });
  }
});

/**
 * POST /api/customers/:customerId/profile-picture
 * Upload customer profile picture
 */
router.post('/:customerId/profile-picture', uploadProfilePicture.single('profile_picture'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'No image uploaded' });
    }

    const { customerId } = req.params;
    
    // Verify customer exists
    const [customers] = await pool.execute(
      'SELECT id, profile_picture_url FROM customers WHERE id = ?',
      [customerId]
    );

    if (customers.length === 0) {
      // Clean up uploaded file
      if (fs.existsSync(req.file.path)) {
        fs.unlinkSync(req.file.path);
      }
      return res.status(404).json({ error: 'Customer not found' });
    }

    // Delete old profile picture if exists
    const oldProfilePicture = customers[0].profile_picture_url;
    if (oldProfilePicture) {
      try {
        // Extract filename from URL
        const oldFilename = oldProfilePicture.split('/').pop();
        const oldFilePath = path.join(__dirname, '../uploads', oldFilename);
        if (fs.existsSync(oldFilePath)) {
          fs.unlinkSync(oldFilePath);
        }
      } catch (err) {
        console.warn('Error deleting old profile picture:', err.message);
      }
    }

    // Get base URL from environment or construct from request
    // For Android emulator, use ANDROID_EMULATOR_URL, for device use actual IP
    // For web, use the request origin or API_BASE_URL
    let baseUrl = process.env.API_BASE_URL;
    if (!baseUrl) {
      // Check if request is from Android (common pattern)
      const userAgent = req.get('user-agent') || '';
      const isAndroid = userAgent.toLowerCase().includes('android') || userAgent.toLowerCase().includes('okhttp');
      
      // Try to get from request origin (for web clients)
      const origin = req.get('origin') || req.get('referer');
      if (origin && !isAndroid) {
        try {
          const url = new URL(origin);
          baseUrl = `${url.protocol}//${url.host}`;
        } catch (e) {
          // Fallback to API_BASE_URL or default
          baseUrl = process.env.API_BASE_URL || `http://localhost:${process.env.PORT || 3000}`;
        }
      } else if (isAndroid) {
        // For Android, use the emulator address from environment
        baseUrl = process.env.ANDROID_EMULATOR_URL || `http://10.0.2.2:${process.env.PORT || 3000}`;
      } else {
        // Default fallback
        baseUrl = process.env.API_BASE_URL || `http://localhost:${process.env.PORT || 3000}`;
      }
    }
    
    const filePath = `/uploads/${req.file.filename}`;
    const profilePictureUrl = `${baseUrl}${filePath}`;
    
    console.log('Profile picture URL:', profilePictureUrl, 'for customer:', customerId, 'user-agent:', req.get('user-agent'));

    // Update customer with new profile picture URL
    await pool.execute(
      'UPDATE customers SET profile_picture_url = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      [profilePictureUrl, customerId]
    );

    // Get updated customer
    const [updatedCustomers] = await pool.execute(
      'SELECT id, name, email, phone, address, profile_picture_url, created_at, updated_at FROM customers WHERE id = ?',
      [customerId]
    );

    res.json({ 
      success: true,
      customer: updatedCustomers[0],
      message: 'Profile picture uploaded successfully' 
    });
  } catch (error) {
    console.error('Error uploading profile picture:', error);
    
    // Clean up uploaded file on error
    if (req.file && fs.existsSync(req.file.path)) {
      try {
        fs.unlinkSync(req.file.path);
      } catch (cleanupError) {
        console.error('Failed to cleanup file:', cleanupError);
      }
    }
    
    res.status(500).json({ 
      error: 'Failed to upload profile picture', 
      message: error.message 
    });
  }
});

/**
 * POST /api/customers/:customerId/payment-methods
 * Save a payment method (credit card) for a customer
 */
router.post('/:customerId/payment-methods', async (req, res) => {
  try {
    const { customerId } = req.params;
    const { token, card_last4, card_brand, expiry_month, expiry_year, paytabs_response_json, is_default } = req.body;

    // Validate required fields
    if (!token || !card_last4) {
      return res.status(400).json({ error: 'Token and card_last4 are required' });
    }

    // Verify customer exists
    const [customers] = await pool.execute(
      'SELECT id FROM customers WHERE id = ?',
      [customerId]
    );

    if (customers.length === 0) {
      return res.status(404).json({ error: 'Customer not found' });
    }

    // If this is set as default, unset other defaults for this customer
    if (is_default) {
      await pool.execute(
        'UPDATE payment_methods SET is_default = FALSE WHERE customer_id = ?',
        [customerId]
      );
    }

    // Check if payment method with same token already exists
    const [existing] = await pool.execute(
      'SELECT id FROM payment_methods WHERE customer_id = ? AND token = ?',
      [customerId, token]
    );

    let paymentMethod;
    if (existing.length > 0) {
      // Update existing payment method
      await pool.execute(
        `UPDATE payment_methods 
         SET card_last4 = ?, card_brand = ?, expiry_month = ?, expiry_year = ?, 
             paytabs_response_json = ?, is_default = ?, updated_at = CURRENT_TIMESTAMP
         WHERE id = ?`,
        [card_last4, card_brand || null, expiry_month || null, expiry_year || null, 
         paytabs_response_json || null, is_default || false, existing[0].id]
      );

      const [updated] = await pool.execute(
        'SELECT * FROM payment_methods WHERE id = ?',
        [existing[0].id]
      );
      paymentMethod = updated[0];
    } else {
      // Insert new payment method
      const [result] = await pool.execute(
        `INSERT INTO payment_methods 
         (customer_id, token, card_last4, card_brand, expiry_month, expiry_year, paytabs_response_json, is_default)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
        [customerId, token, card_last4, card_brand || null, expiry_month || null, 
         expiry_year || null, paytabs_response_json || null, is_default || false]
      );

      const [newPaymentMethod] = await pool.execute(
        'SELECT * FROM payment_methods WHERE id = ?',
        [result.insertId]
      );
      paymentMethod = newPaymentMethod[0];
    }

    res.status(201).json({ payment_method: paymentMethod });
  } catch (error) {
    console.error('Error saving payment method:', error);
    res.status(500).json({ error: 'Failed to save payment method', message: error.message });
  }
});

/**
 * GET /api/customers/:customerId/payment-methods
 * Get all saved payment methods for a customer
 */
router.get('/:customerId/payment-methods', async (req, res) => {
  try {
    const { customerId } = req.params;

    // Verify customer exists
    const [customers] = await pool.execute(
      'SELECT id FROM customers WHERE id = ?',
      [customerId]
    );

    if (customers.length === 0) {
      return res.status(404).json({ error: 'Customer not found' });
    }

    // Get all payment methods for this customer, ordered by is_default DESC, then created_at DESC
    const [paymentMethods] = await pool.execute(
      `SELECT id, token, card_last4, card_brand, expiry_month, expiry_year, is_default, created_at, updated_at
       FROM payment_methods 
       WHERE customer_id = ?
       ORDER BY is_default DESC, created_at DESC`,
      [customerId]
    );

    res.json({ payment_methods: paymentMethods });
  } catch (error) {
    console.error('Error fetching payment methods:', error);
    res.status(500).json({ error: 'Failed to fetch payment methods', message: error.message });
  }
});

/**
 * DELETE /api/customers/:customerId/payment-methods/:paymentMethodId
 * Delete a saved payment method
 */
router.delete('/:customerId/payment-methods/:paymentMethodId', async (req, res) => {
  try {
    const { customerId, paymentMethodId } = req.params;

    // Verify payment method belongs to customer
    const [paymentMethods] = await pool.execute(
      'SELECT id FROM payment_methods WHERE id = ? AND customer_id = ?',
      [paymentMethodId, customerId]
    );

    if (paymentMethods.length === 0) {
      return res.status(404).json({ error: 'Payment method not found' });
    }

    await pool.execute(
      'DELETE FROM payment_methods WHERE id = ?',
      [paymentMethodId]
    );

    res.json({ success: true, message: 'Payment method deleted successfully' });
  } catch (error) {
    console.error('Error deleting payment method:', error);
    res.status(500).json({ error: 'Failed to delete payment method', message: error.message });
  }
});

export default router;

