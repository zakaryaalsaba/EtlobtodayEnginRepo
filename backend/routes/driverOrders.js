import express from 'express';
import jwt from 'jsonwebtoken';
import { pool } from '../db/init.js';
import { tryAcceptOrderInFirebase } from '../services/firebaseOrderSync.js';

const router = express.Router();
const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key-change-in-production';

/**
 * Middleware to authenticate driver
 */
function authenticateDriver(req, res, next) {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({ error: 'No token provided' });
    }

    const token = authHeader.substring(7);
    const decoded = jwt.verify(token, JWT_SECRET);

    if (decoded.role !== 'driver') {
      return res.status(403).json({ error: 'Invalid token role' });
    }

    req.driverId = decoded.driverId;
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
 * GET /api/orders/delivery/available
 * Get available delivery orders (not yet assigned to a driver)
 */
router.get('/delivery/available', authenticateDriver, async (req, res) => {
  try {
    const query = `
      SELECT o.*, 
       rw.restaurant_name,
       rw.phone as restaurant_phone,
       rw.logo_url,
       rw.address as restaurant_address,
       rw.latitude as restaurant_latitude,
       rw.longitude as restaurant_longitude,
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
       WHERE o.order_type = 'delivery'
       AND o.status IN ('confirmed', 'preparing', 'ready')
       AND (o.driver_id IS NULL OR o.driver_id = 0)
       -- Note: 'accepted_by_driver' orders are already assigned, so they won't appear here
       ORDER BY o.created_at ASC
    `;

    const [orders] = await pool.execute(query);

    // Process items and add restaurant info
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
        
        // Add restaurant object for easier access
        order.restaurant = {
          name: order.restaurant_name || null,
          phone: order.restaurant_phone || null,
          logo_url: order.logo_url || null,
          address: order.restaurant_address || null,
          latitude: order.restaurant_latitude || null,
          longitude: order.restaurant_longitude || null
        };
        
        // Add currency info to order (defaults if not present)
        order.currency_code = order.currency_code || 'USD';
        order.currency_symbol_position = order.currency_symbol_position || 'before';
        
        // Remove individual restaurant fields to avoid duplication
        delete order.restaurant_name;
        delete order.restaurant_phone;
        delete order.restaurant_address;
        delete order.restaurant_latitude;
        delete order.restaurant_longitude;
      } catch (e) {
        console.warn('Error processing order items:', e);
        order.items = [];
        order.restaurant = {
          name: null,
          phone: null,
          logo_url: null,
          address: null,
          latitude: null,
          longitude: null
        };
      }
    });

    res.json(orders);
  } catch (error) {
    console.error('Error fetching available orders:', error);
    res.status(500).json({ error: 'Failed to fetch available orders', message: error.message });
  }
});

/**
 * GET /api/orders/delivery/assigned
 * Get orders assigned to the current driver
 */
router.get('/delivery/assigned', authenticateDriver, async (req, res) => {
  try {
    const driverId = req.driverId;

    const query = `
      SELECT o.*, 
       rw.restaurant_name,
       rw.phone as restaurant_phone,
       rw.logo_url,
       rw.address as restaurant_address,
       rw.latitude as restaurant_latitude,
       rw.longitude as restaurant_longitude,
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
       WHERE o.order_type = 'delivery'
       AND o.driver_id = ?
       AND o.status NOT IN ('completed', 'cancelled')
       ORDER BY o.created_at DESC
    `;

    const [orders] = await pool.execute(query, [driverId]);

    // Process items and add restaurant info
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
        
        // Add restaurant object for easier access
        order.restaurant = {
          name: order.restaurant_name || null,
          phone: order.restaurant_phone || null,
          logo_url: order.logo_url || null,
          address: order.restaurant_address || null,
          latitude: order.restaurant_latitude || null,
          longitude: order.restaurant_longitude || null
        };
        
        // Add currency info to order (defaults if not present)
        order.currency_code = order.currency_code || 'USD';
        order.currency_symbol_position = order.currency_symbol_position || 'before';
        
        // Remove individual restaurant fields to avoid duplication
        delete order.restaurant_name;
        delete order.restaurant_phone;
        delete order.restaurant_address;
        delete order.restaurant_latitude;
        delete order.restaurant_longitude;
      } catch (e) {
        console.warn('Error processing order items:', e);
        order.items = [];
        order.restaurant = {
          name: null,
          phone: null,
          logo_url: null,
          address: null,
          latitude: null,
          longitude: null
        };
      }
    });

    res.json(orders);
  } catch (error) {
    console.error('Error fetching assigned orders:', error);
    res.status(500).json({ error: 'Failed to fetch assigned orders', message: error.message });
  }
});

/**
 * GET /api/orders/delivery/history
 * Get completed orders for the current driver
 */
router.get('/delivery/history', authenticateDriver, async (req, res) => {
  try {
    const driverId = req.driverId;

    const query = `
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
       WHERE o.order_type = 'delivery'
       AND o.driver_id = ?
       AND o.status IN ('completed', 'cancelled')
       ORDER BY o.created_at DESC
       LIMIT 50
    `;

    const [orders] = await pool.execute(query, [driverId]);

    // Process items (same as above)
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
        
        // Add currency info to order (defaults if not present)
        order.currency_code = order.currency_code || 'USD';
        order.currency_symbol_position = order.currency_symbol_position || 'before';
      } catch (e) {
        console.warn('Error processing order items:', e);
        order.items = [];
        order.currency_code = order.currency_code || 'USD';
        order.currency_symbol_position = order.currency_symbol_position || 'before';
      }
    });

    res.json(orders);
  } catch (error) {
    console.error('Error fetching order history:', error);
    res.status(500).json({ error: 'Failed to fetch order history', message: error.message });
  }
});

/**
 * POST /api/orders/:orderId/accept
 * Accept an order (assign it to the driver)
 */
router.post('/:orderId/accept', authenticateDriver, async (req, res) => {
  try {
    const driverId = req.driverId;
    const { orderId } = req.params;

    // Check if order exists and is available (need website_id, order_number for Firebase)
    const [orders] = await pool.execute(
      'SELECT id, driver_id, status, order_type, website_id, order_number FROM orders WHERE id = ?',
      [orderId]
    );

    if (orders.length === 0) {
      return res.status(404).json({ error: 'Order not found' });
    }

    const order = orders[0];

    if (order.order_type !== 'delivery') {
      return res.status(400).json({ error: 'Only delivery orders can be accepted' });
    }

    if (order.driver_id && order.driver_id !== driverId) {
      return res.status(409).json({ error: 'Order already assigned to another driver' });
    }

    // Claim in Firebase: only one driver can set request_status to Accepted (transaction)
    const { accepted } = await tryAcceptOrderInFirebase(order.website_id, order.order_number);
    if (!accepted) {
      return res.status(409).json({ error: 'Order was already accepted by another driver' });
    }

    // Assign order to driver in MySQL
    // Set status to 'accepted_by_driver' when driver accepts (if order was 'confirmed' or 'pending')
    await pool.execute(
      'UPDATE orders SET driver_id = ?, status = CASE WHEN status IN ("pending", "confirmed") THEN "accepted_by_driver" ELSE status END, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      [driverId, orderId]
    );

    // Get updated order with items and restaurant info
    const [updatedOrders] = await pool.execute(
      `SELECT o.*, 
       rw.restaurant_name,
       rw.phone as restaurant_phone,
       rw.logo_url,
       rw.address as restaurant_address,
       rw.latitude as restaurant_latitude,
       rw.longitude as restaurant_longitude,
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
       WHERE o.id = ?`,
      [orderId]
    );

    const updatedOrder = updatedOrders[0];
    
    // Process items and add restaurant info
    try {
      if (updatedOrder.items !== null && updatedOrder.items !== undefined) {
        if (Array.isArray(updatedOrder.items)) {
          // Already an array
        } else if (typeof updatedOrder.items === 'string') {
          const parsed = JSON.parse(updatedOrder.items);
          updatedOrder.items = Array.isArray(parsed) ? parsed : (parsed ? [parsed] : []);
        } else if (Buffer.isBuffer(updatedOrder.items)) {
          const parsed = JSON.parse(updatedOrder.items.toString());
          updatedOrder.items = Array.isArray(parsed) ? parsed : (parsed ? [parsed] : []);
        } else if (typeof updatedOrder.items === 'object') {
          updatedOrder.items = [updatedOrder.items];
        } else {
          updatedOrder.items = [];
        }
      } else {
        updatedOrder.items = [];
      }
      
      // Add restaurant object for easier access
      updatedOrder.restaurant = {
        name: updatedOrder.restaurant_name || null,
        phone: updatedOrder.restaurant_phone || null,
        logo_url: updatedOrder.logo_url || null,
        address: updatedOrder.restaurant_address || null,
        latitude: updatedOrder.restaurant_latitude || null,
        longitude: updatedOrder.restaurant_longitude || null
      };
      
      // Remove individual restaurant fields to avoid duplication
      delete updatedOrder.restaurant_name;
      delete updatedOrder.restaurant_phone;
      delete updatedOrder.restaurant_address;
      delete updatedOrder.restaurant_latitude;
      delete updatedOrder.restaurant_longitude;
    } catch (e) {
      updatedOrder.items = [];
      updatedOrder.restaurant = {
        name: null,
        phone: null,
        logo_url: null,
        address: null,
        latitude: null,
        longitude: null
      };
    }

    res.json(updatedOrder);
  } catch (error) {
    console.error('Error accepting order:', error);
    res.status(500).json({ error: 'Failed to accept order', message: error.message });
  }
});

/**
 * POST /api/orders/:orderId/reject
 * Reject an order (driver declines)
 */
router.post('/:orderId/reject', authenticateDriver, async (req, res) => {
  try {
    const { orderId } = req.params;

    // Just return success - order remains available for other drivers
    res.json({ success: true, message: 'Order rejected' });
  } catch (error) {
    console.error('Error rejecting order:', error);
    res.status(500).json({ error: 'Failed to reject order', message: error.message });
  }
});

/**
 * PUT /api/orders/:orderId/status
 * Update order status (for driver workflow)
 */
router.put('/:orderId/status', authenticateDriver, async (req, res) => {
  try {
    const driverId = req.driverId;
    const { orderId } = req.params;
    const { status } = req.body;

    // Valid driver statuses
    const validStatuses = ['arrived_at_pickup', 'picked_up', 'delivered'];
    if (!status || !validStatuses.includes(status)) {
      return res.status(400).json({ error: 'Invalid status. Valid statuses: ' + validStatuses.join(', ') });
    }

    // Verify order is assigned to this driver
    const [orders] = await pool.execute(
      'SELECT id, driver_id, status FROM orders WHERE id = ?',
      [orderId]
    );

    if (orders.length === 0) {
      return res.status(404).json({ error: 'Order not found' });
    }

    const order = orders[0];
    if (order.driver_id !== driverId) {
      return res.status(403).json({ error: 'Order not assigned to you' });
    }

    // Map driver statuses to order statuses (MySQL: use arrived_at_pickup for flow)
    let orderStatus = status;
    if (status === 'arrived_at_pickup') {
      orderStatus = 'arrived_at_pickup'; // Driver arrived at store
    } else if (status === 'picked_up') {
      orderStatus = 'picked_up'; // Driver picked up order, on the way
    } else if (status === 'delivered') {
      orderStatus = 'completed';
    }

    console.log(`[DRIVER STATUS UPDATE] Order ${orderId}: Updating status from "${order.status}" to "${orderStatus}"`);

    // Update order status
    try {
      const [updateResult] = await pool.execute(
        'UPDATE orders SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
        [orderStatus, orderId]
      );
      
      console.log(`[DRIVER STATUS UPDATE] Update result:`, updateResult);
      console.log(`[DRIVER STATUS UPDATE] Rows affected: ${updateResult.affectedRows}`);
      
      if (updateResult.affectedRows === 0) {
        console.error(`[DRIVER STATUS UPDATE] No rows were updated! Order ${orderId} may not exist or status update failed.`);
        return res.status(404).json({ error: 'Order not found or update failed' });
      }
    } catch (dbError) {
      console.error(`[DRIVER STATUS UPDATE] Database error updating order ${orderId}:`, dbError);
      // Check if it's an ENUM value error
      if (dbError.code === 'ER_TRUNCATED_WRONG_VALUE_FOR_FIELD' || dbError.message?.includes('ENUM')) {
        console.error(`[DRIVER STATUS UPDATE] ENUM value error! Status "${orderStatus}" is not valid. Did you run the migration?`);
        return res.status(400).json({ 
          error: 'Invalid status value', 
          message: `Status "${orderStatus}" is not a valid ENUM value. Please run migration: migration_add_arrived_at_pickup_status.sql`,
          details: dbError.message 
        });
      }
      throw dbError; // Re-throw to be caught by outer try-catch
    }

    // When status becomes completed (e.g. driver marked delivered), remove from Firebase only (MySQL already updated)
    if (orderStatus === 'completed' || orderStatus === 'cancelled') {
      try {
        const { removeOrderFromFirebase } = await import('../services/firebaseOrderSync.js');
        await removeOrderFromFirebase(order.website_id, order.order_number);
        console.log(`[DRIVER STATUS UPDATE] Order ${order.order_number} removed from Firebase`);
      } catch (firebaseErr) {
        console.error('[DRIVER STATUS UPDATE] Error removing order from Firebase:', firebaseErr);
      }
    }

    // Small delay to ensure database commit completes
    await new Promise(resolve => setTimeout(resolve, 100));

    // Get updated order with items and restaurant info
    const [updatedOrders] = await pool.execute(
      `SELECT o.*, 
       rw.restaurant_name,
       rw.phone as restaurant_phone,
       rw.logo_url,
       rw.address as restaurant_address,
       rw.latitude as restaurant_latitude,
       rw.longitude as restaurant_longitude,
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
       WHERE o.id = ?`,
      [orderId]
    );

    const updatedOrder = updatedOrders[0];
    console.log(`[DRIVER STATUS UPDATE] Fetched updated order ${orderId}: status = "${updatedOrder.status}"`);
    
    // Process items and add restaurant info
    try {
      if (updatedOrder.items !== null && updatedOrder.items !== undefined) {
        if (Array.isArray(updatedOrder.items)) {
          // Already an array
        } else if (typeof updatedOrder.items === 'string') {
          const parsed = JSON.parse(updatedOrder.items);
          updatedOrder.items = Array.isArray(parsed) ? parsed : (parsed ? [parsed] : []);
        } else if (Buffer.isBuffer(updatedOrder.items)) {
          const parsed = JSON.parse(updatedOrder.items.toString());
          updatedOrder.items = Array.isArray(parsed) ? parsed : (parsed ? [parsed] : []);
        } else if (typeof updatedOrder.items === 'object') {
          updatedOrder.items = [updatedOrder.items];
        } else {
          updatedOrder.items = [];
        }
      } else {
        updatedOrder.items = [];
      }
      
      // Add restaurant object for easier access
      updatedOrder.restaurant = {
        name: updatedOrder.restaurant_name || null,
        phone: updatedOrder.restaurant_phone || null,
        logo_url: updatedOrder.logo_url || null,
        address: updatedOrder.restaurant_address || null,
        latitude: updatedOrder.restaurant_latitude || null,
        longitude: updatedOrder.restaurant_longitude || null
      };
      
      // Remove individual restaurant fields to avoid duplication
      delete updatedOrder.restaurant_name;
      delete updatedOrder.restaurant_phone;
      delete updatedOrder.restaurant_address;
      delete updatedOrder.restaurant_latitude;
      delete updatedOrder.restaurant_longitude;
    } catch (e) {
      updatedOrder.items = [];
      updatedOrder.restaurant = {
        name: null,
        phone: null,
        logo_url: null,
        address: null,
        latitude: null,
        longitude: null
      };
    }

    console.log(`[DRIVER STATUS UPDATE] Returning order ${orderId} with status: "${updatedOrder.status}"`);
    res.json(updatedOrder);
  } catch (error) {
    console.error('Error updating order status:', error);
    res.status(500).json({ error: 'Failed to update order status', message: error.message });
  }
});

export default router;

