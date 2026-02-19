import express from 'express';
import { sendPushNotification } from '../services/pushNotificationService.js';
import { pool } from '../db/init.js';

const router = express.Router();

/**
 * POST /api/test-notification
 * Send a test push notification to a customer
 * Body: { customerId: number, title?: string, body?: string }
 */
router.post('/', async (req, res) => {
  try {
    const { customerId, title, body } = req.body;

    if (!customerId) {
      return res.status(400).json({ error: 'customerId is required' });
    }

    // Get customer's device token
    const [customers] = await pool.execute(
      'SELECT device_token, name FROM customers WHERE id = ?',
      [customerId]
    );

    if (customers.length === 0) {
      return res.status(404).json({ error: 'Customer not found' });
    }

    const customer = customers[0];

    if (!customer.device_token) {
      return res.status(400).json({ 
        error: 'Customer does not have a device token. Make sure the app is running and the user is logged in.' 
      });
    }

    const notificationTitle = title || 'Test Notification';
    const notificationBody = body || `Hello ${customer.name || 'User'}, this is a test notification!`;

    const success = await sendPushNotification(
      customer.device_token,
      notificationTitle,
      notificationBody,
      {
        type: 'test',
        timestamp: new Date().toISOString()
      }
    );

    if (success) {
      res.json({ 
        success: true, 
        message: 'Test notification sent successfully',
        customerName: customer.name,
        deviceToken: customer.device_token.substring(0, 20) + '...' // Show first 20 chars only
      });
    } else {
      res.status(500).json({ 
        error: 'Failed to send notification. Check Firebase configuration.' 
      });
    }
  } catch (error) {
    console.error('Error sending test notification:', error);
    res.status(500).json({ 
      error: 'Failed to send test notification', 
      message: error.message 
    });
  }
});

/**
 * GET /api/test-notification/customers
 * Get list of customers with device tokens (for testing)
 */
router.get('/customers', async (req, res) => {
  try {
    const [customers] = await pool.execute(
      `SELECT 
        id, 
        name, 
        email, 
        phone,
        device_token IS NOT NULL as has_token,
        SUBSTRING(device_token, 1, 20) as token_preview,
        last_token_updated
      FROM customers 
      WHERE device_token IS NOT NULL
      ORDER BY last_token_updated DESC
      LIMIT 50`
    );

    res.json({ customers });
  } catch (error) {
    console.error('Error fetching customers:', error);
    res.status(500).json({ 
      error: 'Failed to fetch customers', 
      message: error.message 
    });
  }
});

export default router;

