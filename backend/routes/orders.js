import express from 'express';
import { pool } from '../db/init.js';
import { broadcastOrderUpdate, broadcastOrderStatusToCustomer } from '../services/sseManager.js';
import { sendOrderNotification } from '../services/notificationService.js';
import { saveOrderToFirebase } from '../services/firebaseOrderSync.js';
import jwt from 'jsonwebtoken';

const router = express.Router();
const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key-change-in-production';

/**
 * Middleware to authenticate super admin
 */
function authenticateSuperAdmin(req, res, next) {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({ error: 'No token provided' });
    }

    const token = authHeader.substring(7);
    const decoded = jwt.verify(token, JWT_SECRET);

    // Check for super admin flag (from super admin login) OR role (for compatibility)
    if (!decoded.superAdmin && decoded.role !== 'super_admin') {
      return res.status(403).json({ error: 'Access denied. Super admin only.' });
    }

    req.superAdminId = decoded.superAdminId || decoded.id;
    next();
  } catch (error) {
    if (error.name === 'JsonWebTokenError') {
      return res.status(401).json({ error: 'Invalid token' });
    }
    if (error.name === 'TokenExpiredError') {
      return res.status(401).json({ error: 'Token expired' });
    }
    return res.status(500).json({ error: 'Authentication error', message: error.message });
  }
}

/**
 * Generate unique order number
 */
function generateOrderNumber() {
  const timestamp = Date.now().toString(36).toUpperCase();
  const random = Math.random().toString(36).substring(2, 6).toUpperCase();
  return `ORD-${timestamp}-${random}`;
}

/**
 * POST /api/orders
 * Create a new order
 */
router.post('/', async (req, res) => {
  const connection = await pool.getConnection();
  try {
    await connection.beginTransaction();

    const {
      website_id,
      customer_id,
      customer_name,
      customer_email,
      customer_phone,
      customer_address,
      order_type,
      payment_method,
      payment_intent_id,
      coupon_code,
      delivery_latitude,
      delivery_longitude,
      items,
      notes,
      tip,
      delivery_instructions,
      total_amount: client_total_amount,
      restaurant: restaurantFromRequest
    } = req.body;

    // Validate required fields
    if (!website_id || !customer_name || !customer_phone || !items || items.length === 0) {
      return res.status(400).json({ error: 'Missing required fields' });
    }

    // Calculate total
    let subtotalAmount = 0;
    let totalOriginalAmount = 0; // Sum of (original_price * quantity)
    const orderItems = [];

    for (const item of items) {
      const [products] = await connection.execute(
        'SELECT * FROM products WHERE id = ? AND website_id = ?',
        [item.product_id, website_id]
      );

      if (products.length === 0) {
        throw new Error(`Product ${item.product_id} not found`);
      }

      const product = products[0];
      if (!product.is_available) {
        throw new Error(`Product ${product.name} is not available`);
      }

      const quantity = item.quantity || 1;
      const subtotal = parseFloat(product.price) * quantity;
      subtotalAmount += subtotal;

      // Calculate original amount (use original_price if available, otherwise use price)
      const originalPrice = parseFloat(product.original_price) || parseFloat(product.price);
      const originalSubtotal = originalPrice * quantity;
      totalOriginalAmount += originalSubtotal;

      orderItems.push({
        product_id: product.id,
        product_name: product.name,
        product_price: product.price,
        quantity,
        subtotal
      });
    }

    // Get restaurant settings for delivery fee and tax
    const [websites] = await connection.execute(
      'SELECT delivery_fee, tax_enabled, tax_rate FROM restaurant_websites WHERE id = ?',
      [website_id]
    );
    
    let discountAmount = 0;
    let appliedCoupon = null;
    let totalAmount = subtotalAmount;
    
    // Validate and apply coupon if provided (before tax and delivery)
    if (coupon_code) {
      const [coupons] = await connection.execute(
        'SELECT * FROM coupons WHERE website_id = ? AND code = ?',
        [website_id, coupon_code.toUpperCase()]
      );
      
      if (coupons.length > 0) {
        const coupon = coupons[0];
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const validFrom = new Date(coupon.valid_from);
        validFrom.setHours(0, 0, 0, 0);
        const validUntil = new Date(coupon.valid_until);
        validUntil.setHours(23, 59, 59, 999);
        
        // Validate coupon
        if (coupon.is_active && 
            today >= validFrom && 
            today <= validUntil &&
            (coupon.usage_limit === null || coupon.usage_count < coupon.usage_limit) &&
            subtotalAmount >= (parseFloat(coupon.min_order_amount) || 0)) {
          
          // Calculate discount on subtotal
          if (coupon.discount_type === 'percentage') {
            discountAmount = (subtotalAmount * parseFloat(coupon.discount_value)) / 100;
            if (coupon.max_discount_amount !== null) {
              const maxDiscount = parseFloat(coupon.max_discount_amount);
              if (discountAmount > maxDiscount) {
                discountAmount = maxDiscount;
              }
            }
          } else {
            discountAmount = parseFloat(coupon.discount_value);
            if (discountAmount > subtotalAmount) {
              discountAmount = subtotalAmount;
            }
          }
          
          appliedCoupon = coupon;
          totalAmount = subtotalAmount - discountAmount;
          console.log(`Applied coupon ${coupon_code}: -$${discountAmount.toFixed(2)}`);
        }
      }
    }
    
    // Initialize tax and delivery fees
    let taxAmount = 0;
    let deliveryFee = 0;

    if (websites.length > 0) {
      const restaurantSettings = websites[0];
      
      // Add tax if enabled (tax is calculated on subtotal before discount)
      if (restaurantSettings.tax_enabled === 1 || restaurantSettings.tax_enabled === true) {
        const taxRate = parseFloat(restaurantSettings.tax_rate) || 0;
        if (taxRate > 0) {
          taxAmount = (subtotalAmount * taxRate) / 100;
          totalAmount += taxAmount;
          console.log(`Added tax (${taxRate}%): $${taxAmount.toFixed(2)} to order total`);
        }
      }
      
      // Add delivery fee if order type is delivery
      if (order_type === 'delivery') {
        deliveryFee = parseFloat(restaurantSettings.delivery_fee) || 0;
        if (deliveryFee > 0) {
          totalAmount += deliveryFee;
          console.log(`Added delivery fee: $${deliveryFee} to order total`);
        }
      }
    }

    // Get service fee from settings table (only the row with status = 'active'; one active row enforced by trigger)
    let serviceFee = 0;
    const [settingsRows] = await connection.execute(
      "SELECT service_fee FROM settings WHERE status = 'active' LIMIT 1"
    );
    if (settingsRows.length > 0 && settingsRows[0].service_fee != null) {
      serviceFee = parseFloat(settingsRows[0].service_fee) || 0;
      if (serviceFee > 0) {
        totalAmount += serviceFee;
        console.log(`Added service fee: $${serviceFee} to order total`);
      }
    }

    // Use client-provided total when present (matches checkout display including offers/discounts)
    if (client_total_amount != null && typeof client_total_amount === 'number' && client_total_amount >= 0) {
      totalAmount = Number(client_total_amount);
      console.log(`Using client total_amount: $${totalAmount.toFixed(2)}`);
    }

    // Generate order number
    let orderNumber = generateOrderNumber();
    let attempts = 0;
    while (attempts < 10) {
      const [existing] = await connection.execute(
        'SELECT id FROM orders WHERE order_number = ?',
        [orderNumber]
      );
      if (existing.length === 0) break;
      orderNumber = generateOrderNumber();
      attempts++;
    }

    // Determine payment status based on payment intent
    const paymentStatus = payment_intent_id ? 'paid' : 'pending';
    const finalPaymentMethod = payment_method || 'pickup'; // Default to 'pickup' for cash on pickup

    // Check if payment_intent_id and coupon_code columns exist
    const [columns] = await connection.execute(`
      SELECT COLUMN_NAME 
      FROM INFORMATION_SCHEMA.COLUMNS 
      WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME IN ('payment_intent_id', 'coupon_code')
    `);
    
    const existingColumns = columns.map(col => col.COLUMN_NAME);
    const hasPaymentIntentId = existingColumns.includes('payment_intent_id');
    const hasCouponCode = existingColumns.includes('coupon_code');

    // Check if delivery location columns exist
    const [deliveryColumns] = await connection.execute(`
      SELECT COLUMN_NAME 
      FROM INFORMATION_SCHEMA.COLUMNS 
      WHERE TABLE_SCHEMA = ? 
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME IN ('delivery_latitude', 'delivery_longitude')
    `, [process.env.MYSQL_DB || 'restaurant_websites']);
    
    const existingDeliveryColumns = deliveryColumns.map(col => col.COLUMN_NAME);
    const hasDeliveryLatitude = existingDeliveryColumns.includes('delivery_latitude');
    const hasDeliveryLongitude = existingDeliveryColumns.includes('delivery_longitude');

    // Check if new order amount columns exist
    const [orderAmountColumns] = await connection.execute(`
      SELECT COLUMN_NAME 
      FROM INFORMATION_SCHEMA.COLUMNS 
      WHERE TABLE_SCHEMA = ? 
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME IN ('total_original_amount', 'tax', 'delivery_fees')
    `, [process.env.MYSQL_DB || 'restaurant_websites']);
    
    const existingOrderAmountColumns = orderAmountColumns.map(col => col.COLUMN_NAME);
    const hasTotalOriginalAmount = existingOrderAmountColumns.includes('total_original_amount');
    const hasTax = existingOrderAmountColumns.includes('tax');
    const hasDeliveryFees = existingOrderAmountColumns.includes('delivery_fees');

    // Check if tip, delivery_instructions, service_fee columns exist
    const [extraOrderColumns] = await connection.execute(`
      SELECT COLUMN_NAME 
      FROM INFORMATION_SCHEMA.COLUMNS 
      WHERE TABLE_SCHEMA = ? 
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME IN ('tip', 'delivery_instructions', 'service_fee')
    `, [process.env.MYSQL_DB || 'restaurant_websites']);
    const existingExtraColumns = extraOrderColumns.map(col => col.COLUMN_NAME);
    const hasTip = existingExtraColumns.includes('tip');
    const hasDeliveryInstructions = existingExtraColumns.includes('delivery_instructions');
    const hasServiceFee = existingExtraColumns.includes('service_fee');

    // Create order
    let insertQuery, insertValues;
    const baseFields = [
      'website_id', 'customer_id', 'order_number', 'customer_name', 'customer_email',
      'customer_phone', 'customer_address', 'order_type', 'total_amount', 'payment_method'
    ];
    const baseValues = [
      website_id,
      customer_id || null,
      orderNumber,
      customer_name,
      customer_email || null,
      customer_phone,
      customer_address || null,
      order_type || 'pickup',
      totalAmount,
      finalPaymentMethod
    ];

    // Add delivery location if delivery order type and columns exist
    if (order_type === 'delivery' && hasDeliveryLatitude && hasDeliveryLongitude) {
      if (delivery_latitude !== undefined && delivery_longitude !== undefined) {
        baseFields.push('delivery_latitude', 'delivery_longitude');
        baseValues.push(delivery_latitude, delivery_longitude);
      }
    }

    if (hasPaymentIntentId) {
      baseFields.push('payment_intent_id');
      baseValues.push(payment_intent_id || null);
    }

    if (hasCouponCode && appliedCoupon) {
      baseFields.push('coupon_code');
      baseValues.push(coupon_code.toUpperCase());
    }

    // Add new order amount columns if they exist
    if (hasTotalOriginalAmount) {
      baseFields.push('total_original_amount');
      baseValues.push(totalOriginalAmount);
    }

    if (hasTax) {
      baseFields.push('tax');
      baseValues.push(taxAmount);
    }

    if (hasDeliveryFees) {
      baseFields.push('delivery_fees');
      baseValues.push(deliveryFee);
    }

    if (hasTip) {
      baseFields.push('tip');
      baseValues.push(typeof tip === 'number' ? tip : (parseFloat(tip) || 0));
    }
    if (hasDeliveryInstructions) {
      baseFields.push('delivery_instructions');
      baseValues.push(delivery_instructions && String(delivery_instructions).trim() ? String(delivery_instructions).trim() : null);
    }
    if (hasServiceFee) {
      baseFields.push('service_fee');
      baseValues.push(serviceFee);
    }

    baseFields.push('payment_status', 'notes');
    baseValues.push(paymentStatus, notes || null);

    insertQuery = `INSERT INTO orders (${baseFields.join(', ')}) VALUES (${baseFields.map(() => '?').join(', ')})`;
    insertValues = baseValues;

    const [orderResult] = await connection.execute(insertQuery, insertValues);

    const orderId = orderResult.insertId;

    // Create order items
    for (const item of orderItems) {
      await connection.execute(
        `INSERT INTO order_items (
          order_id, product_id, product_name, product_price, quantity, subtotal
        ) VALUES (?, ?, ?, ?, ?, ?)`,
        [
          orderId,
          item.product_id,
          item.product_name,
          item.product_price,
          item.quantity,
          item.subtotal
        ]
      );
    }

    // Increment coupon usage count if coupon was applied
    if (appliedCoupon) {
      await connection.execute(
        'UPDATE coupons SET usage_count = usage_count + 1 WHERE id = ?',
        [appliedCoupon.id]
      );
      console.log(`Incremented usage count for coupon ${coupon_code}`);
    }

    await connection.commit();

    // Get complete order
    const [orders] = await connection.execute(
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
       FROM orders o WHERE o.id = ?`,
      [orderId]
    );

    const order = orders[0];
    // Handle items - MySQL JSON_ARRAYAGG might return different types
    try {
      if (order.items !== null && order.items !== undefined) {
        // Check if it's already an array first (most common case with mysql2)
        if (Array.isArray(order.items)) {
          // Already an array, use as is
          // No action needed
        } else if (typeof order.items === 'string') {
          // If it's a string, try to parse it
          try {
            const parsed = JSON.parse(order.items);
            order.items = Array.isArray(parsed) ? parsed : (parsed ? [parsed] : []);
          } catch (parseError) {
            console.warn('Failed to parse order items JSON string:', parseError.message);
            console.warn('Raw value:', order.items);
            order.items = [];
          }
        } else if (Buffer.isBuffer(order.items)) {
          // If it's a Buffer, convert to string first
          try {
            const parsed = JSON.parse(order.items.toString());
            order.items = Array.isArray(parsed) ? parsed : (parsed ? [parsed] : []);
          } catch (e) {
            order.items = [];
          }
        } else if (typeof order.items === 'object') {
          // If it's an object (but not array), wrap it in an array
          // This handles cases where MySQL returns a single object
          order.items = [order.items];
        } else {
          // Unknown type, set to empty array
          order.items = [];
        }
      } else {
        order.items = [];
      }
    } catch (handleError) {
      console.error('Error handling order items:', handleError);
      console.error('Items value:', order.items, 'Type:', typeof order.items);
      order.items = [];
    }

    // Broadcast new order via SSE to connected admin clients
    try {
      broadcastOrderUpdate(website_id, order);
    } catch (sseError) {
      console.error('Error broadcasting order update via SSE:', sseError);
      // Don't fail the order creation if SSE fails
    }

    // Do NOT notify drivers when order is first placed (pending).
    // Drivers are notified only when the restaurant CONFIRMS the order (pending → confirmed)
    // via PUT /api/admin/orders/:id/status. That way the captain hears the sound when the
    // order is confirmed and ready for a driver to accept.

    // Send notification to restaurant owner (non-blocking)
    console.log(`[RESTAURANT NOTIFICATION] Order placed: sending notification to restaurant for order ${order.order_number} (website_id: ${website_id})`);
    (async () => {
      try {
        await sendOrderNotification(website_id, order);
      } catch (notificationError) {
        console.error('[RESTAURANT NOTIFICATION] Error sending order notification:', notificationError);
        // Don't fail the order creation if notification fails
      }
    })();

    // Create notification for customer if order has a customer_id
    try {
      if (order.customer_id) {
        const statusMessages = {
          'pending': 'Your order has been received and is being processed',
          'confirmed': 'Your order has been confirmed',
          'preparing': 'Your order is being prepared',
          'ready': 'Your order is ready for pickup',
          'picked_up': 'Your order has been picked up and is on the way to you!',
          'completed': 'Your order has been completed',
          'cancelled': 'Your order has been cancelled'
        };
        
        const title = `Order ${order.order_number} - ${order.status.charAt(0).toUpperCase() + order.status.slice(1)}`;
        const message = statusMessages[order.status] || `Your order status: ${order.status}`;
        
        await pool.execute(
          `INSERT INTO customer_notifications (customer_id, order_id, website_id, title, message, type, status)
           VALUES (?, ?, ?, ?, ?, 'order_update', ?)`,
          [order.customer_id, orderId, order.website_id || null, title, message, order.status || 'pending']
        );
        console.log(`Created customer notification for order ${order.order_number}`);
      }
    } catch (notificationError) {
      console.error('Error creating customer notification:', notificationError);
      // Don't fail the order creation if notification fails
    }

    // Note: Order is NOT saved to Firebase here. It will be saved when restaurant confirms the order
    // (status changes from 'pending' to 'confirmed'). This ensures drivers only see confirmed orders.

    res.status(201).json({ order });
  } catch (error) {
    await connection.rollback();
    console.error('Error creating order:', error);
    res.status(500).json({ error: 'Failed to create order', message: error.message });
  } finally {
    connection.release();
  }
});

/**
 * GET /api/orders/website/:websiteId
 * Get all orders for a website (for restaurant)
 */
router.get('/website/:websiteId', async (req, res) => {
  try {
    const { websiteId } = req.params;
    const { status } = req.query;

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

    orders.forEach(order => {
      try {
        if (order.items !== null && order.items !== undefined) {
          if (Array.isArray(order.items)) {
            // Already an array, use as is
            // No action needed
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
 * GET /api/orders (for super admin)
 * Get all orders across all restaurants
 * Must be before /:orderNumber route
 * Query params: dateFrom, dateTo, orderType, status
 */
router.get('/', authenticateSuperAdmin, async (req, res) => {
  try {
    const { dateFrom, dateTo, orderType, status } = req.query;
    
    let query = `
      SELECT o.*,
       d.id as driver_id,
       d.name as driver_name,
       d.phone as driver_phone,
       d.email as driver_email,
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
      LEFT JOIN drivers d ON o.driver_id = d.id
      WHERE 1=1
    `;
    const params = [];
    
    // Filter by date range
    if (dateFrom) {
      query += ' AND o.created_at >= ?';
      params.push(dateFrom);
    }
    if (dateTo) {
      query += ' AND o.created_at < ?';
      params.push(dateTo);
    }
    
    // Filter by order type
    if (orderType) {
      query += ' AND o.order_type = ?';
      params.push(orderType);
    }
    
    // Filter by status
    if (status) {
      query += ' AND o.status = ?';
      params.push(status);
    }
    
    query += ' ORDER BY o.created_at DESC';
    
    const [orders] = await pool.execute(query, params);
    
    // Parse JSON items for each order
    const ordersWithItems = orders.map(order => {
      try {
        if (order.items !== null && order.items !== undefined) {
          if (Array.isArray(order.items)) {
            // Already an array, use as is
          } else if (typeof order.items === 'string') {
            order.items = JSON.parse(order.items);
          } else if (Buffer.isBuffer(order.items)) {
            order.items = JSON.parse(order.items.toString());
          } else if (typeof order.items === 'object') {
            order.items = [order.items];
          } else {
            order.items = [];
          }
        } else {
          order.items = [];
        }
      } catch (e) {
        console.warn('Error processing order items:', e);
        order.items = [];
      }
      return order;
    });
    
    res.json(ordersWithItems);
  } catch (error) {
    console.error('Error fetching all orders:', error);
    res.status(500).json({ error: 'Failed to fetch orders', message: error.message });
  }
});

/**
 * GET /api/orders/:orderNumber
 * Get order by order number (for customer tracking)
 */
router.get('/:orderNumber', async (req, res) => {
  try {
    const { orderNumber } = req.params;

    const [orders] = await pool.execute(
      `SELECT o.*, 
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
       WHERE o.order_number = ?`,
      [orderNumber]
    );

    if (orders.length === 0) {
      return res.status(404).json({ error: 'Order not found' });
    }

    const order = orders[0];
    if (order.items) {
      // MySQL JSON_ARRAYAGG might return string or object depending on version
      if (typeof order.items === 'string') {
        try {
          order.items = JSON.parse(order.items);
        } catch (e) {
          order.items = [];
        }
      } else if (Array.isArray(order.items)) {
        // Already an array, no need to parse
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

    res.json({ order });
  } catch (error) {
    console.error('Error fetching order:', error);
    res.status(500).json({ error: 'Failed to fetch order', message: error.message });
  }
});

/**
 * PUT /api/orders/:id/status
 * Update order status
 */
router.put('/:id/status', async (req, res) => {
  console.log(`[STATUS UPDATE ROUTE] Called for order ID: ${req.params.id}`);
  console.log(`[STATUS UPDATE ROUTE] Request body:`, req.body);
  console.log(`[STATUS UPDATE ROUTE] New status: ${req.body.status}`);
  
  try {
    const { id } = req.params;
    const { status } = req.body;

    console.log(`[STATUS UPDATE ROUTE] Processing: orderId=${id}, newStatus=${status}`);

    // Valid statuses include both standard order statuses and driver-specific statuses
    const validStatuses = [
      'pending', 'confirmed', 'preparing', 'ready', 'accepted_by_driver', 'arrived_at_pickup', 
      'picked_up', 'completed', 'cancelled', 'delivered'
    ];
    if (!status || !validStatuses.includes(status)) {
      console.log(`[STATUS UPDATE ROUTE] Invalid status: ${status}`);
      return res.status(400).json({ error: 'Invalid status' });
    }

    // Get current order status before updating (to check if it changed)
    const [orderCheck] = await pool.execute(
      'SELECT customer_id, website_id, order_number, status, order_type, driver_id FROM orders WHERE id = ?',
      [id]
    );

    if (orderCheck.length === 0) {
      return res.status(404).json({ error: 'Order not found' });
    }

    const currentOrder = orderCheck[0];
    const oldStatus = currentOrder.status;
    
    // Map driver-specific statuses to order statuses
    let orderStatus = status;
    if (status === 'arrived_at_pickup') {
      // Driver arrived at pickup location - use arrived_at_pickup status
      orderStatus = 'arrived_at_pickup';
    } else if (status === 'picked_up') {
      // Picked up - set to 'picked_up' status (order is picked up and driver is on the way)
      orderStatus = 'picked_up';
    } else if (status === 'delivered') {
      orderStatus = 'completed';
    }
    
    const statusChanged = oldStatus !== orderStatus;

    console.log(`[STATUS UPDATE ROUTE] Current order: order_type=${currentOrder.order_type}, oldStatus=${oldStatus}, newStatus=${orderStatus}, statusChanged=${statusChanged}`);

    // Update order status
    await pool.execute(
      'UPDATE orders SET status = ? WHERE id = ?',
      [orderStatus, id]
    );
    
    console.log(`[STATUS UPDATE ROUTE] Order status updated from ${oldStatus} to ${orderStatus}`);

    // When status becomes completed or cancelled, remove from Firebase only (MySQL already updated)
    if (statusChanged && (orderStatus === 'completed' || orderStatus === 'cancelled')) {
      (async () => {
        try {
          const { removeOrderFromFirebase } = await import('../services/firebaseOrderSync.js');
          await removeOrderFromFirebase(currentOrder.website_id, currentOrder.order_number);
        } catch (e) {
          console.error('[STATUS UPDATE ROUTE] Error removing order from Firebase:', e);
        }
      })();
    }

    // Get updated order for broadcasting
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
       FROM orders o WHERE o.id = ?`,
      [id]
    );

    const updatedOrder = updatedOrders[0];
    if (!updatedOrder) {
      return res.status(404).json({ error: 'Order not found' });
    }
    
    if (updatedOrder.items) {
      // Handle items parsing
      if (typeof updatedOrder.items === 'string') {
        try {
          updatedOrder.items = JSON.parse(updatedOrder.items);
        } catch (e) {
          updatedOrder.items = [];
        }
      } else if (Array.isArray(updatedOrder.items)) {
        // Already an array
      } else {
        updatedOrder.items = [];
      }
    } else {
      updatedOrder.items = [];
    }

    // Broadcast order status update to customer (if they're tracking)
    if (statusChanged && updatedOrder) {
      try {
        broadcastOrderStatusToCustomer(id, updatedOrder);
      } catch (sseError) {
        console.error('Error broadcasting order status to customer:', sseError);
      }
    }

    // Send push notifications to online drivers if this is a delivery order that just became "pending" or "confirmed"
    // and doesn't have a driver assigned yet
    console.log(`[STATUS UPDATE] Order ${id}: statusChanged=${statusChanged}, oldStatus=${oldStatus}, newStatus=${status}, order_type=${updatedOrder.order_type}, driver_id=${updatedOrder.driver_id || 'null'}`);
    
    console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] Checking conditions for order ${updatedOrder.order_number}:`);
    console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] - statusChanged: ${statusChanged}`);
    console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] - status: ${status}`);
    console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] - order_type: ${updatedOrder.order_type}`);
    console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] - driver_id: ${updatedOrder.driver_id || 'null'}`);
    console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] - Conditions check: statusChanged=${statusChanged}, status=${(status === 'pending' || status === 'confirmed')}, delivery=${updatedOrder.order_type === 'delivery'}, no_driver=${!updatedOrder.driver_id}`);
    
    if (statusChanged && (status === 'pending' || status === 'confirmed') && updatedOrder.order_type === 'delivery' && !updatedOrder.driver_id) {
      console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] ✅ Conditions met! Starting driver notification process for order ${updatedOrder.order_number}`);
      
      // Send driver notifications in a non-blocking way
      (async () => {
        try {
          console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] Step 1: Importing pushNotificationService...`);
          const { sendPushNotificationToMultiple } = await import('../services/pushNotificationService.js');
          console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] Step 1: ✅ Successfully imported pushNotificationService`);
          
          console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] Step 2: Querying database for online drivers...`);
          // Get all online drivers with device tokens
          const [drivers] = await pool.execute(
            'SELECT id, name, phone, device_token FROM drivers WHERE is_online = 1 AND device_token IS NOT NULL AND device_token != ""'
          );
          console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] Step 2: ✅ Found ${drivers.length} online driver(s) in database`);
          
          // Log all online drivers with their tokens
          console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] Online drivers details:`);
          drivers.forEach((driver, index) => {
            console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] Driver ${index + 1}:`);
            console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE]   - ID: ${driver.id}`);
            console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE]   - Name: ${driver.name || 'N/A'}`);
            console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE]   - Phone: ${driver.phone || 'N/A'}`);
            console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE]   - Device Token: ${driver.device_token}`);
            console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE]   - Token Length: ${driver.device_token?.length || 0}`);
          });
          
          if (drivers.length > 0) {
            console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] Step 3: Processing device tokens...`);
            const deviceTokens = drivers
              .map(d => d.device_token)
              .filter(token => token); // Remove null/empty tokens
            
            console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] Step 3: ✅ Filtered to ${deviceTokens.length} valid device token(s)`);
            console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] Full device tokens list:`);
            deviceTokens.forEach((token, index) => {
              console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE]   Token ${index + 1}: ${token}`);
            });
            
            if (deviceTokens.length > 0) {
              const title = 'New Delivery Order Available';
              const deliveryAddress = updatedOrder.customer_address || 
                                     (updatedOrder.delivery_latitude && updatedOrder.delivery_longitude 
                                       ? `Location: ${updatedOrder.delivery_latitude}, ${updatedOrder.delivery_longitude}`
                                       : 'Delivery order');
              const body = `Order #${updatedOrder.order_number} - $${parseFloat(updatedOrder.total_amount).toFixed(2)} - ${deliveryAddress}`;
              
              const data = {
                type: 'new_delivery_order',
                order_id: String(updatedOrder.id),
                order_number: updatedOrder.order_number,
                website_id: String(updatedOrder.website_id),
                total_amount: String(updatedOrder.total_amount)
              };
              
              console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] Step 4: Preparing notification payload:`);
              console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] - Title: ${title}`);
              console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] - Body: ${body}`);
              console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] - Data:`, JSON.stringify(data));
              
              console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] Step 5: Calling sendPushNotificationToMultiple with ${deviceTokens.length} token(s)...`);
              const result = await sendPushNotificationToMultiple(deviceTokens, title, body, data);
              
              console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] Step 5: ✅ Notification call completed`);
              console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] Result:`, JSON.stringify(result));
              console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] ✅ Push notifications sent to ${result.success} driver(s) for delivery order ${updatedOrder.order_number} (status updated to ${status}), failures: ${result.failure}`);
              
              // Clean up invalid tokens if any
              if (result.invalidTokens.length > 0) {
                console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] Step 6: Cleaning up ${result.invalidTokens.length} invalid device tokens`);
                for (const invalidToken of result.invalidTokens) {
                  await pool.execute(
                    'UPDATE drivers SET device_token = NULL WHERE device_token = ?',
                    [invalidToken]
                  );
                }
                console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] Step 6: ✅ Cleanup completed`);
              } else {
                console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] Step 6: No invalid tokens to clean up`);
              }
            } else {
              console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] ❌ No valid device tokens found for ${drivers.length} driver(s)`);
            }
          } else {
            console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] ❌ No online drivers found with device tokens`);
          }
        } catch (driverNotificationError) {
          console.error('[DRIVER NOTIFICATION] [STATUS UPDATE] ❌ ERROR sending driver notifications:');
          console.error('[DRIVER NOTIFICATION] [STATUS UPDATE] Error message:', driverNotificationError.message);
          console.error('[DRIVER NOTIFICATION] [STATUS UPDATE] Error stack:', driverNotificationError.stack);
          console.error('[DRIVER NOTIFICATION] [STATUS UPDATE] Full error:', driverNotificationError);
        }
      })();
    } else {
      console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] ❌ Conditions NOT met - skipping driver notification`);
      console.log(`[DRIVER NOTIFICATION] [STATUS UPDATE] Reason: ${!statusChanged ? 'Status did not change' : (status !== 'pending' && status !== 'confirmed') ? 'Status not pending/confirmed' : updatedOrder.order_type !== 'delivery' ? 'Not a delivery order' : updatedOrder.driver_id ? 'Driver already assigned' : 'Unknown'}`);
    }

    // Create notification for customer if order has a customer_id and status changed
    if (statusChanged && currentOrder.customer_id) {
      try {
        const statusMessages = {
          'pending': 'Your order has been received and is being processed',
          'confirmed': 'Your order has been confirmed',
          'preparing': 'Your order is being prepared',
          'ready': 'Your order is ready for pickup',
          'picked_up': 'Your order has been picked up and is on the way to you!',
          'completed': 'Your order has been completed',
          'cancelled': 'Your order has been cancelled'
        };
        
        const title = `Order ${currentOrder.order_number} - ${orderStatus.charAt(0).toUpperCase() + orderStatus.slice(1)}`;
        const message = statusMessages[orderStatus] || `Your order status has been updated to ${orderStatus}`;
        
        await pool.execute(
          `INSERT INTO customer_notifications (customer_id, order_id, website_id, title, message, type, status)
           VALUES (?, ?, ?, ?, ?, 'order_update', ?)`,
          [currentOrder.customer_id, id, currentOrder.website_id || null, title, message, orderStatus]
        );
        console.log(`Created customer notification for order ${currentOrder.order_number} - status changed from ${oldStatus} to ${orderStatus}`);
      } catch (notificationError) {
        console.error('Error creating customer notification:', notificationError);
        // Don't fail the status update if notification creation fails
      }
    }

    // Use the already fetched updatedOrder
    if (!updatedOrder) {
      return res.status(404).json({ error: 'Order not found' });
    }

    const order = updatedOrder;
    // Items are already parsed above

    res.json({ order });
  } catch (error) {
    console.error('Error updating order status:', error);
    res.status(500).json({ error: 'Failed to update order status', message: error.message });
  }
});

/**
 * PUT /api/orders/:id/payment
 * Update payment status (when customer pays on pickup)
 */
router.put('/:id/payment', async (req, res) => {
  try {
    const { id } = req.params;
    const { payment_status } = req.body;

    if (!payment_status || !['pending', 'paid'].includes(payment_status)) {
      return res.status(400).json({ error: 'Invalid payment status' });
    }

    await pool.execute(
      'UPDATE orders SET payment_status = ? WHERE id = ?',
      [payment_status, id]
    );

    const [orders] = await pool.execute(
      'SELECT * FROM orders WHERE id = ?',
      [id]
    );

    if (orders.length === 0) {
      return res.status(404).json({ error: 'Order not found' });
    }

    res.json({ order: orders[0] });
  } catch (error) {
    console.error('Error updating payment status:', error);
    res.status(500).json({ error: 'Failed to update payment status', message: error.message });
  }
});


/**
 * GET /api/orders (for super admin)
 * Get all orders across all restaurants
 */
router.get('/', authenticateSuperAdmin, async (req, res) => {
  try {
    const query = `
      SELECT o.*, 
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
      ORDER BY o.created_at DESC
    `;
    
    const [orders] = await pool.execute(query);
    
    // Parse JSON items for each order
    const ordersWithItems = orders.map(order => {
      try {
        if (order.items !== null && order.items !== undefined) {
          if (Array.isArray(order.items)) {
            // Already an array, use as is
          } else if (typeof order.items === 'string') {
            order.items = JSON.parse(order.items);
          } else if (Buffer.isBuffer(order.items)) {
            order.items = JSON.parse(order.items.toString());
          } else if (typeof order.items === 'object') {
            order.items = [order.items];
          } else {
            order.items = [];
          }
        } else {
          order.items = [];
        }
      } catch (e) {
        console.warn('Error processing order items:', e);
        order.items = [];
      }
      return order;
    });
    
    res.json(ordersWithItems);
  } catch (error) {
    console.error('Error fetching all orders:', error);
    res.status(500).json({ error: 'Failed to fetch orders', message: error.message });
  }
});

/**
 * POST /api/orders/process-payment-with-token
 * Process payment with saved PayTabs token (server-side, no UI)
 * Server key is read from environment variable PAYTABS_SERVER_KEY
 */
router.post('/process-payment-with-token', async (req, res) => {
  try {
    const {
      profile_id,
      server_key,
      serverKey: serverKeyCamelCase, // Also check camelCase in case of serialization issues
      token,
      amount,
      currency,
      cart_id,
      cart_description,
      customer_details
    } = req.body;

    // Log full request body for debugging
    console.log('process-payment-with-token request received - Full req.body:', JSON.stringify(req.body, null, 2));
    console.log('process-payment-with-token request received - Summary:', {
      profile_id: profile_id ? `present: ${profile_id}` : 'missing',
      server_key: server_key ? `present (length: ${server_key.length}, prefix: ${server_key.substring(0, 8)})` : 'missing',
      token: token ? `present (length: ${token.length})` : 'missing',
      amount,
      currency,
      cart_id,
      'req.body keys': Object.keys(req.body)
    });

    // Validate required fields
    if (!profile_id || !token || !amount || !currency || !cart_id) {
      return res.status(400).json({ error: 'Missing required fields: profile_id, token, amount, currency, cart_id are required' });
    }

    // IMPORTANT: For server-to-server PayTabs API calls, we MUST use the Web API server key,
    // NOT the Mobile SDK server key. Mobile SDK keys cause "application/octet-stream" errors.
    // The Web API server key MUST be stored in PAYTABS_SERVER_KEY environment variable.
    // We do NOT accept server_key from request body because Android sends Mobile SDK key.
    const serverKey = process.env.PAYTABS_SERVER_KEY;
    
    if (!serverKey) {
      console.error('❌ PAYTABS_SERVER_KEY not configured in environment variables');
      console.error('Environment variable PAYTABS_SERVER_KEY:', process.env.PAYTABS_SERVER_KEY ? `set (length: ${process.env.PAYTABS_SERVER_KEY.length})` : 'not set');
      if (server_key || serverKeyCamelCase) {
        console.warn('⚠️ Request contains server_key, but it is ignored (likely Mobile SDK key, not Web API key)');
      }
      console.error('Request body keys:', Object.keys(req.body));
      return res.status(500).json({ 
        success: false,
        error: 'PayTabs Web API server key not configured',
        message: 'Please set PAYTABS_SERVER_KEY in your backend .env file. You need the Web API server key (not Mobile SDK key) for server-to-server calls. Get it from PayTabs Dashboard > Developers > API Keys > Key Management > Web API Server Key.'
      });
    }
    
    console.log('✅ Using PayTabs Web API server key (length):', serverKey.length, 'from environment variable');

    // Call PayTabs transaction API with saved token
    // PayTabs API requires the server key in Authorization header
    // Format: Authorization: {server_key} (no Bearer prefix)
    // IMPORTANT: For token-based payments, use tran_class: 'recurring' instead of 'ecom'
    // This is required for token payments and may avoid PCI DSS requirements
    const paytabsRequestPayload = {
      profile_id: parseInt(profile_id),
      tran_type: 'sale',
      tran_class: 'recurring', // Use 'recurring' for token-based payments (not 'ecom')
      cart_id: cart_id,
      cart_description: cart_description || 'Order Payment',
      cart_currency: currency,
      cart_amount: amount,
      payment_token: token, // Use saved token
      customer_details: customer_details || {}
    };
    
    console.log('📤 PayTabs API request payload:', JSON.stringify(paytabsRequestPayload, null, 2));
    console.log('📤 PayTabs API Authorization header:', serverKey.substring(0, 8) + '...');
    
    const paytabsResponse = await fetch('https://secure-jordan.paytabs.com/payment/request', {
      method: 'POST',
      headers: {
        'Authorization': serverKey,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(paytabsRequestPayload)
    });

    const paytabsResult = await paytabsResponse.json();

    if (!paytabsResponse.ok) {
      console.error('PayTabs API error:', paytabsResult);
      
      // Check for specific errors that might need different handling
      const errorCode = paytabsResult.code;
      const errorMessage = paytabsResult.message || paytabsResult.error || 'Unknown error';
      
      // If recurring is not enabled, provide helpful error message
      if (errorCode === 1 && errorMessage.includes('PCI DSS')) {
        console.warn('⚠️ PCI DSS error - recurring transactions may need to be enabled on PayTabs profile');
        console.warn('⚠️ Contact PayTabs support to enable recurring transaction mode');
      }
      
      return res.status(paytabsResponse.status).json({
        success: false,
        error: 'PayTabs payment failed',
        message: errorMessage,
        paytabs_response: paytabsResult,
        hint: errorCode === 1 && errorMessage.includes('PCI DSS') 
          ? 'Recurring transactions may need to be enabled on your PayTabs profile. Contact PayTabs support.' 
          : undefined
      });
    }

    // Check if payment was successful
    const isSuccess = paytabsResult.payment_result?.response_status === 'A' ||
                     paytabsResult.response_status === 'A' ||
                     (paytabsResult.payment_result?.responseCode && paytabsResult.payment_result.responseCode.startsWith('G')) ||
                     (paytabsResult.payment_result?.responseMessage && (
                       paytabsResult.payment_result.responseMessage.toLowerCase().includes('authorised') ||
                       paytabsResult.payment_result.responseMessage.toLowerCase().includes('authorized')
                     ));

    if (isSuccess) {
      res.json({
        success: true,
        transaction_reference: paytabsResult.tran_ref || paytabsResult.transaction_reference,
        payment_result: paytabsResult.payment_result,
        payment_info: paytabsResult.payment_info,
        paytabs_response: paytabsResult
      });
    } else {
      res.status(400).json({
        success: false,
        error: 'Payment not authorized',
        message: paytabsResult.payment_result?.responseMessage || paytabsResult.message || 'Payment failed',
        paytabs_response: paytabsResult
      });
    }
  } catch (error) {
    console.error('Error processing payment with token:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to process payment',
      message: error.message
    });
  }
});

export default router;

