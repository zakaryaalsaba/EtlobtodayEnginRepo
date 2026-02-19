import express from 'express';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { pool } from '../db/init.js';
import { addConnection } from '../services/sseManager.js';

const router = express.Router();

// JWT secret (in production, use environment variable)
const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key-change-in-production';

/**
 * Middleware to verify admin token
 * Can be used by both admin and restaurant dashboards
 */
export const verifyAdminToken = async (req, res, next) => {
  try {
    const token = req.headers.authorization?.split(' ')[1]; // Bearer <token>
    
    if (!token) {
      return res.status(401).json({ error: 'No token provided' });
    }

    const decoded = jwt.verify(token, JWT_SECRET);
    
    // Verify admin still exists and is valid
    const [admins] = await pool.execute(
      'SELECT * FROM admins WHERE id = ? AND website_id = ?',
      [decoded.adminId, decoded.websiteId]
    );

    if (admins.length === 0) {
      return res.status(401).json({ error: 'Invalid token' });
    }

    req.admin = admins[0];
    req.websiteId = decoded.websiteId;
    next();
  } catch (error) {
    // Handle expired tokens gracefully (don't log as error, just debug)
    if (error.name === 'TokenExpiredError') {
      // Token expired - this is expected behavior, just return 401
      return res.status(401).json({ error: 'Token expired', expired: true });
    }
    // For other errors, log them
    console.error('Token verification error:', error.message || error);
    res.status(401).json({ error: 'Invalid token' });
  }
};

/**
 * POST /api/admin/register
 * Register a new admin for a restaurant
 */
router.post('/register', async (req, res) => {
  try {
    const { website_id, email, password, name } = req.body;

    if (!website_id || !email || !password || !name) {
      return res.status(400).json({ error: 'Missing required fields' });
    }

    // Check if website exists
    const [websites] = await pool.execute(
      'SELECT id FROM restaurant_websites WHERE id = ?',
      [website_id]
    );

    if (websites.length === 0) {
      return res.status(404).json({ error: 'Website not found' });
    }

    // Check if admin already exists for this website
    const [existingAdmins] = await pool.execute(
      'SELECT id FROM admins WHERE website_id = ? OR email = ?',
      [website_id, email]
    );

    if (existingAdmins.length > 0) {
      return res.status(400).json({ error: 'Admin already exists for this website or email already in use' });
    }

    // Hash password
    const passwordHash = await bcrypt.hash(password, 10);

    // Create admin
    await pool.execute(
      'INSERT INTO admins (website_id, email, password_hash, name) VALUES (?, ?, ?, ?)',
      [website_id, email, passwordHash, name]
    );

    res.status(201).json({ message: 'Admin created successfully' });
  } catch (error) {
    console.error('Error creating admin:', error);
    res.status(500).json({ error: 'Failed to create admin', message: error.message });
  }
});

/**
 * POST /api/admin/login
 * Admin login
 */
router.post('/login', async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password are required' });
    }

    // Find admin
    const [admins] = await pool.execute(
      'SELECT a.*, rw.restaurant_name FROM admins a JOIN restaurant_websites rw ON a.website_id = rw.id WHERE a.email = ?',
      [email]
    );

    if (admins.length === 0) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const admin = admins[0];

    // Verify password
    const isValidPassword = await bcrypt.compare(password, admin.password_hash);
    if (!isValidPassword) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    // Access token (ID token) - short-lived; client uses refresh token to get new one
    const token = jwt.sign(
      { adminId: admin.id, websiteId: admin.website_id, email: admin.email },
      JWT_SECRET,
      { expiresIn: process.env.ACCESS_TOKEN_EXPIRY || '1h' }
    );
    const refreshToken = jwt.sign(
      { adminId: admin.id, websiteId: admin.website_id, email: admin.email, type: 'refresh' },
      JWT_SECRET,
      { expiresIn: process.env.REFRESH_TOKEN_EXPIRY || '7d' }
    );

    res.json({
      token,
      refreshToken,
      admin: {
        id: admin.id,
        website_id: admin.website_id,
        email: admin.email,
        name: admin.name,
        restaurant_name: admin.restaurant_name
      }
    });
  } catch (error) {
    console.error('Error during login:', error);
    res.status(500).json({ error: 'Failed to login', message: error.message });
  }
});

/**
 * GET /api/admin/me
 * Get current admin info
 */
router.get('/me', verifyAdminToken, async (req, res) => {
  try {
    const [admins] = await pool.execute(
      'SELECT a.*, rw.restaurant_name FROM admins a JOIN restaurant_websites rw ON a.website_id = rw.id WHERE a.id = ?',
      [req.admin.id]
    );

    if (admins.length === 0) {
      return res.status(404).json({ error: 'Admin not found' });
    }

    const admin = admins[0];
    res.json({
      admin: {
        id: admin.id,
        website_id: admin.website_id,
        email: admin.email,
        name: admin.name,
        restaurant_name: admin.restaurant_name
      }
    });
  } catch (error) {
    console.error('Error fetching admin:', error);
    res.status(500).json({ error: 'Failed to fetch admin', message: error.message });
  }
});

/**
 * GET /api/admin/website/:websiteId
 * Get admin info by website ID (for website builder, no auth required)
 */
router.get('/website/:websiteId', async (req, res) => {
  try {
    const { websiteId } = req.params;

    const [admins] = await pool.execute(
      'SELECT a.id, a.website_id, a.email, a.name, rw.restaurant_name FROM admins a JOIN restaurant_websites rw ON a.website_id = rw.id WHERE a.website_id = ?',
      [websiteId]
    );

    if (admins.length === 0) {
      return res.status(404).json({ error: 'Admin not found for this website' });
    }

    const admin = admins[0];
    res.json({
      admin: {
        id: admin.id,
        website_id: admin.website_id,
        email: admin.email,
        name: admin.name,
        restaurant_name: admin.restaurant_name
      }
    });
  } catch (error) {
    console.error('Error fetching admin by website:', error);
    res.status(500).json({ error: 'Failed to fetch admin', message: error.message });
  }
});

/**
 * PUT /api/admin/device-token
 * Update admin device token for push notifications
 */
router.put('/device-token', verifyAdminToken, async (req, res) => {
  try {
    const adminId = req.admin.id;
    const { device_token, device_type } = req.body;

    if (!device_token) {
      return res.status(400).json({ error: 'Device token is required' });
    }

    const validDeviceTypes = ['android', 'ios'];
    const finalDeviceType = device_type && validDeviceTypes.includes(device_type.toLowerCase()) 
      ? device_type.toLowerCase() 
      : 'android';

    // Check if device_token column exists
    const [columns] = await pool.execute(`
      SELECT COLUMN_NAME 
      FROM INFORMATION_SCHEMA.COLUMNS 
      WHERE TABLE_SCHEMA = ? 
      AND TABLE_NAME = 'admins'
      AND COLUMN_NAME IN ('device_token', 'device_type')
    `, [process.env.MYSQL_DB || 'restaurant_websites']);
    
    const existingColumns = columns.map(col => col.COLUMN_NAME);
    const hasDeviceToken = existingColumns.includes('device_token');
    const hasDeviceType = existingColumns.includes('device_type');

    const websiteId = req.websiteId;
    if (hasDeviceToken && hasDeviceType) {
      await pool.execute(
        'UPDATE admins SET device_token = ?, device_type = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
        [device_token, finalDeviceType, adminId]
      );
      console.log(`[RESTAURANT NOTIFICATION] Device token registered: admin_id=${adminId}, website_id=${websiteId}, token_length=${device_token.length}`);
    } else if (hasDeviceToken) {
      await pool.execute(
        'UPDATE admins SET device_token = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
        [device_token, adminId]
      );
      console.log(`[RESTAURANT NOTIFICATION] Device token registered: admin_id=${adminId}, website_id=${websiteId}, token_length=${device_token.length} (device_type column missing)`);
    } else {
      return res.status(500).json({ 
        error: 'Device token feature not available', 
        message: 'Database migration required' 
      });
    }

    res.json({ success: true, message: 'Device token updated' });
  } catch (error) {
    console.error('Error updating device token:', error);
    res.status(500).json({ error: 'Failed to update device token', message: error.message });
  }
});

/**
 * GET /api/admin/orders
 * Get all orders for the admin's restaurant
 */
router.get('/orders', verifyAdminToken, async (req, res) => {
  try {
    const { status } = req.query;
    const websiteId = req.websiteId;

    let query = `
      SELECT o.*, 
       rw.currency_code,
       rw.currency_symbol_position,
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
       LEFT JOIN restaurant_websites rw ON o.website_id = rw.id
       WHERE o.website_id = ?
    `;
    const params = [websiteId];

    if (status) {
      query += ' AND o.status = ?';
      params.push(status);
    }

    query += ' ORDER BY o.created_at DESC';

    const [orders] = await pool.execute(query, params);

    // Process items for each order
    orders.forEach(order => {
      try {
        if (order.items !== null && order.items !== undefined) {
          if (Array.isArray(order.items)) {
            // Already an array, use as is
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
        
        // Add currency info to order (defaults only if not present)
        if (!order.currency_code) {
            order.currency_code = 'USD';
        }
        if (!order.currency_symbol_position) {
            order.currency_symbol_position = 'before';
        }
      } catch (e) {
        console.warn('Error processing order items:', e);
        order.items = [];
        // Add currency info to order (defaults only if not present)
        if (!order.currency_code) {
            order.currency_code = 'USD';
        }
        if (!order.currency_symbol_position) {
            order.currency_symbol_position = 'before';
        }
      }
    });

    res.json({ orders });
  } catch (error) {
    console.error('Error fetching orders:', error);
    res.status(500).json({ error: 'Failed to fetch orders', message: error.message });
  }
});

/**
 * PUT /api/admin/orders/:id/status
 * Update order status
 */
router.put('/orders/:id/status', verifyAdminToken, async (req, res) => {
  console.log(`[ADMIN STATUS UPDATE] Called for order ID: ${req.params.id}`);
  console.log(`[ADMIN STATUS UPDATE] Request body:`, req.body);
  console.log(`[ADMIN STATUS UPDATE] New status: ${req.body.status}`);
  
  try {
    const { id } = req.params;
    const { status } = req.body;
    const websiteId = req.websiteId;

    console.log(`[ADMIN STATUS UPDATE] Processing: orderId=${id}, newStatus=${status}, websiteId=${websiteId}`);

    const validStatuses = ['pending', 'confirmed', 'preparing', 'ready', 'accepted_by_driver', 'arrived_at_pickup', 'picked_up', 'completed', 'cancelled'];
    if (!status || !validStatuses.includes(status)) {
      console.log(`[ADMIN STATUS UPDATE] Invalid status: ${status}`);
      return res.status(400).json({ error: 'Invalid status' });
    }

    // Get current order status before updating (to check if it changed)
    const [orderCheck] = await pool.execute(
      'SELECT id, status, order_type, driver_id FROM orders WHERE id = ? AND website_id = ?',
      [id, websiteId]
    );

    if (orderCheck.length === 0) {
      console.log(`[ADMIN STATUS UPDATE] Order not found: id=${id}, websiteId=${websiteId}`);
      return res.status(404).json({ error: 'Order not found' });
    }

    const currentOrder = orderCheck[0];
    const oldStatus = currentOrder.status;
    const statusChanged = oldStatus !== status;

    console.log(`[ADMIN STATUS UPDATE] Current order: order_type=${currentOrder.order_type}, oldStatus=${oldStatus}, statusChanged=${statusChanged}, driver_id=${currentOrder.driver_id || 'null'}`);

    // Update status
    await pool.execute(
      'UPDATE orders SET status = ? WHERE id = ? AND website_id = ?',
      [status, id, websiteId]
    );
    
    console.log(`[ADMIN STATUS UPDATE] Order status updated from ${oldStatus} to ${status}`);

    // Get updated order
    const [updatedOrders] = await pool.execute(
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
       FROM orders o WHERE o.id = ? AND o.website_id = ?`,
      [id, websiteId]
    );

    if (updatedOrders.length === 0) {
      return res.status(404).json({ error: 'Order not found' });
    }

    const order = updatedOrders[0];
    if (order.items) {
      if (typeof order.items === 'string') {
        try {
          order.items = JSON.parse(order.items);
        } catch (e) {
          order.items = [];
        }
      } else if (Array.isArray(order.items)) {
        // Already an array
      } else if (Buffer.isBuffer(order.items)) {
        try {
          order.items = JSON.parse(order.items.toString());
        } catch (e) {
          order.items = [];
        }
      } else {
        order.items = [];
      }
    } else {
      order.items = [];
    }

    // If status changed from 'pending' to 'confirmed', save order to Firebase (drivers can now see it)
    if (statusChanged && oldStatus === 'pending' && status === 'confirmed') {
      console.log(`[ADMIN STATUS UPDATE] Order confirmed by restaurant: saving ${order.order_number} to Firebase`);
      (async () => {
        try {
          const { saveOrderToFirebase } = await import('../services/firebaseOrderSync.js');
          
          // Get restaurant info for Firebase
          const [sites] = await pool.execute(
            'SELECT restaurant_name, phone, address, latitude, longitude FROM restaurant_websites WHERE id = ?',
            [websiteId]
          );
          
          const orderForFirebase = { ...order };
          if (sites && sites[0]) {
            orderForFirebase.restaurant = {
              name: sites[0].restaurant_name || null,
              phone: sites[0].phone || null,
              address: sites[0].address || null,
              latitude: sites[0].latitude != null ? Number(sites[0].latitude) : null,
              longitude: sites[0].longitude != null ? Number(sites[0].longitude) : null
            };
          }
          
          await saveOrderToFirebase(orderForFirebase);
          console.log(`[ADMIN STATUS UPDATE] ✅ Order ${order.order_number} saved to Firebase`);
        } catch (firebaseError) {
          console.error('[ADMIN STATUS UPDATE] ❌ Error saving order to Firebase:', firebaseError);
          // Don't fail the status update if Firebase sync fails
        }
      })();
    }

    // If status changed to 'completed' or 'cancelled', remove order from Firebase only (MySQL already updated above)
    if (statusChanged && (status === 'completed' || status === 'cancelled')) {
      console.log(`[ADMIN STATUS UPDATE] Order ${order.order_number} is ${status}: removing from Firebase only`);
      (async () => {
        try {
          const { removeOrderFromFirebase } = await import('../services/firebaseOrderSync.js');
          await removeOrderFromFirebase(order.website_id, order.order_number);
          console.log(`[ADMIN STATUS UPDATE] ✅ Order ${order.order_number} removed from Firebase`);
        } catch (firebaseError) {
          console.error('[ADMIN STATUS UPDATE] ❌ Error removing order from Firebase:', firebaseError);
          // Don't fail the status update if Firebase remove fails
        }
      })();
    }

    // Send push notifications to drivers based on order status
    console.log(`[ADMIN STATUS UPDATE] Checking driver notification: statusChanged=${statusChanged}, status=${status}, order_type=${order.order_type}, driver_id=${order.driver_id || 'null'}`);
    
    if (statusChanged && order.order_type === 'delivery') {
      try {
        const { sendPushNotificationToMultiple, sendPushNotification } = await import('../services/pushNotificationService.js');
        
        // Case 1: Order is "pending" or "confirmed" and no driver assigned - notify all online drivers
        if ((status === 'pending' || status === 'confirmed') && !order.driver_id) {
          console.log(`[ADMIN STATUS UPDATE] Sending notification to all online drivers for order ${order.order_number} with status ${status}`);
          
          // Get all online drivers with device tokens
          const [drivers] = await pool.execute(
            'SELECT device_token FROM drivers WHERE is_online = 1 AND device_token IS NOT NULL AND device_token != ""'
          );
          
          console.log(`[ADMIN STATUS UPDATE] Found ${drivers.length} online driver(s) with device tokens`);
          
          if (drivers.length > 0) {
            const deviceTokens = drivers
              .map(d => d.device_token)
              .filter(token => token); // Remove null/empty tokens
            
            console.log(`[ADMIN STATUS UPDATE] Sending notifications to ${deviceTokens.length} driver(s)`);
            
            if (deviceTokens.length > 0) {
              const title = 'New Delivery Order Available';
              const deliveryAddress = order.customer_address || 
                                     (order.delivery_latitude && order.delivery_longitude 
                                       ? `Location: ${order.delivery_latitude}, ${order.delivery_longitude}`
                                       : 'Delivery order');
              const body = `Order #${order.order_number} - $${parseFloat(order.total_amount).toFixed(2)} - ${deliveryAddress}`;
              
              const data = {
                type: 'new_delivery_order',
                order_id: String(order.id),
                order_number: order.order_number,
                website_id: String(order.website_id),
                total_amount: String(order.total_amount)
              };
              
              const result = await sendPushNotificationToMultiple(deviceTokens, title, body, data);
              
              console.log(`[ADMIN STATUS UPDATE] Push notifications sent to ${result.success} driver(s) for delivery order ${order.order_number} (status updated to ${status})`);
              
              // Clean up invalid tokens if any
              if (result.invalidTokens.length > 0) {
                console.log(`[ADMIN STATUS UPDATE] Cleaning up ${result.invalidTokens.length} invalid device tokens`);
                for (const invalidToken of result.invalidTokens) {
                  await pool.execute(
                    'UPDATE drivers SET device_token = NULL WHERE device_token = ?',
                    [invalidToken]
                  );
                }
              }
            }
          } else {
            console.log(`[ADMIN STATUS UPDATE] No online drivers found with device tokens`);
          }
        }
        // Case 2: Order is "ready" and has a driver assigned - notify the assigned driver to pick up
        else if (status === 'ready' && order.driver_id) {
          console.log(`[ADMIN STATUS UPDATE] Sending pickup notification to assigned driver ${order.driver_id} for order ${order.order_number}`);
          
          // Get the assigned driver's device token
          const [drivers] = await pool.execute(
            'SELECT device_token FROM drivers WHERE id = ? AND device_token IS NOT NULL AND device_token != ""',
            [order.driver_id]
          );
          
          if (drivers.length > 0 && drivers[0].device_token) {
            const deviceToken = drivers[0].device_token;
            const title = 'Order Ready for Pickup';
            const body = `Order #${order.order_number} is ready! Please pick it up from the restaurant.`;
            
            const data = {
              type: 'order_ready',
              order_id: String(order.id),
              order_number: order.order_number,
              website_id: String(order.website_id),
              total_amount: String(order.total_amount)
            };
            
            const success = await sendPushNotification(deviceToken, title, body, data);
            
            if (success) {
              console.log(`[ADMIN STATUS UPDATE] Pickup notification sent to driver ${order.driver_id} for order ${order.order_number}`);
            } else {
              console.log(`[ADMIN STATUS UPDATE] Failed to send pickup notification to driver ${order.driver_id}`);
            }
          } else {
            console.log(`[ADMIN STATUS UPDATE] Driver ${order.driver_id} has no device token`);
          }
        } else {
          console.log(`[ADMIN STATUS UPDATE] Driver notification skipped: statusChanged=${statusChanged}, status=${status}, order_type=${order.order_type}, driver_id=${order.driver_id || 'null'}`);
        }
      } catch (driverNotificationError) {
        console.error('[ADMIN STATUS UPDATE] Error sending driver notifications on status update:', driverNotificationError);
        // Don't fail the status update if driver notification fails
      }
    } else {
      console.log(`[ADMIN STATUS UPDATE] Driver notification skipped: statusChanged=${statusChanged}, status=${status}, order_type=${order.order_type}, driver_id=${order.driver_id || 'null'}`);
    }

    res.json({ order });
  } catch (error) {
    console.error('Error updating order status:', error);
    res.status(500).json({ error: 'Failed to update order status', message: error.message });
  }
});

/**
 * GET /api/admin/orders/stream
 * Server-Sent Events endpoint for real-time order updates
 * Supports token via query parameter for EventSource compatibility
 */
router.get('/orders/stream', async (req, res, next) => {
  // EventSource doesn't support custom headers, so we accept token as query parameter
  const token = req.query.token || req.headers.authorization?.split(' ')[1];
  
  if (!token) {
    return res.status(401).json({ error: 'No token provided' });
  }

  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    
    // Verify admin still exists and is valid
    const [admins] = await pool.execute(
      'SELECT * FROM admins WHERE id = ? AND website_id = ?',
      [decoded.adminId, decoded.websiteId]
    );

    if (admins.length === 0) {
      return res.status(401).json({ error: 'Invalid token' });
    }

    req.admin = admins[0];
    req.websiteId = decoded.websiteId;
    next();
  } catch (error) {
    // Handle expired tokens gracefully (don't log as error, just debug)
    if (error.name === 'TokenExpiredError') {
      // Token expired - this is expected behavior, just return 401
      return res.status(401).json({ error: 'Token expired', expired: true });
    }
    // For other errors, log them
    console.error('Token verification error:', error.message || error);
    return res.status(401).json({ error: 'Invalid token' });
  }
}, (req, res) => {
  const websiteId = req.websiteId;

  // Set headers for SSE
  res.setHeader('Content-Type', 'text/event-stream');
  res.setHeader('Cache-Control', 'no-cache');
  res.setHeader('Connection', 'keep-alive');
  res.setHeader('X-Accel-Buffering', 'no'); // Disable nginx buffering

  // Add CORS headers if needed
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Headers', 'Cache-Control');

  // Add this connection to the SSE manager
  addConnection(websiteId, res);

  // Send a ping every 30 seconds to keep connection alive
  const pingInterval = setInterval(() => {
    try {
      res.write(`: ping\n\n`);
    } catch (error) {
      clearInterval(pingInterval);
      // Connection is closed, will be removed by on('close') handler
    }
  }, 30000);

  // Clean up on client disconnect
  req.on('close', () => {
    clearInterval(pingInterval);
    res.end();
  });
});

/**
 * GET /api/admin/statistics/today
 * Get today's statistics for all restaurants (orders count, revenue, delivery fees)
 */
router.get('/statistics/today', async (req, res) => {
  try {
    // Use the specified query that groups by website_id, status, and order_type
    // Filtering by status = 'completed' using HAVING clause
    const [statsResult] = await pool.execute(
      `SELECT 
        COUNT(1) as number_of_orders,
        o.status,
        o.order_type,
        o.website_id,
        SUM(CAST(o.total_amount AS DECIMAL(10, 2))) as total_amount_sum,
        SUM(CAST(o.total_original_amount AS DECIMAL(10, 2))) as total_original_amount_sum,
        SUM(CAST(o.tax AS DECIMAL(10, 2))) as tax_sum,
        SUM(CAST(o.delivery_fees AS DECIMAL(10, 2))) as delivery_fees_sum
       FROM orders o
       WHERE DATE(o.created_at) = CURDATE()
       GROUP BY website_id, o.status, o.order_type
       HAVING o.status = 'completed'`
    );

    // Aggregate the results
    let ordersCount = 0;
    let totalAmount = 0;
    let totalOriginalAmount = 0;
    let tax = 0;
    let deliveryFees = 0;

    statsResult.forEach(row => {
      ordersCount += parseInt(row.number_of_orders) || 0;
      totalAmount += parseFloat(row.total_amount_sum) || 0;
      totalOriginalAmount += parseFloat(row.total_original_amount_sum) || 0;
      tax += parseFloat(row.tax_sum) || 0;
      deliveryFees += parseFloat(row.delivery_fees_sum) || 0;
    });

    // Calculate metrics based on the specified formulas
    // Total Paid (Gross/Total Money) = sum(total_amount)
    const totalPaidToRestaurantsAndDrivers = totalAmount;

    // Total Owed to Restaurants = sum(total_original_amount) + sum(tax)
    const totalOwedToRestaurants = totalOriginalAmount + tax;

    // Today's Revenue = sum(total_amount) - (sum(total_original_amount) + sum(tax) + sum(delivery_fees))
    const revenue = totalAmount - (totalOriginalAmount + tax + deliveryFees);

    res.json({
      ordersCount,
      revenue: revenue.toFixed(2),
      deliveryFees: deliveryFees.toFixed(2),
      totalOwedToRestaurants: totalOwedToRestaurants.toFixed(2),
      totalPaidToRestaurantsAndDrivers: totalPaidToRestaurantsAndDrivers.toFixed(2)
    });
  } catch (error) {
    console.error('Error fetching today\'s statistics:', error);
    res.status(500).json({ error: 'Failed to fetch statistics', message: error.message });
  }
});

/**
 * GET /api/admin/statistics/today/restaurants-owed
 * Get total owed per restaurant (buying price + tax) for completed orders today
 */
router.get('/statistics/today/restaurants-owed', async (req, res) => {
  try {
    const [restaurantsOwedResult] = await pool.execute(
      `SELECT 
        o.website_id,
        rw.restaurant_name,
        COUNT(o.id) as orders_count,
        SUM(CAST(o.total_original_amount AS DECIMAL(10, 2))) as total_buying_price,
        SUM(CAST(o.tax AS DECIMAL(10, 2))) as total_tax,
        SUM(CAST(o.total_original_amount AS DECIMAL(10, 2)) + CAST(o.tax AS DECIMAL(10, 2))) as total_owed
       FROM orders o
       INNER JOIN restaurant_websites rw ON o.website_id = rw.id
       WHERE DATE(o.created_at) = CURDATE()
       AND o.status = 'completed'
       GROUP BY o.website_id, rw.restaurant_name
       ORDER BY total_owed DESC`
    );

    res.json({ restaurants: restaurantsOwedResult });
  } catch (error) {
    console.error('Error fetching restaurants owed:', error);
    res.status(500).json({ error: 'Failed to fetch restaurants owed', message: error.message });
  }
});

export default router;

