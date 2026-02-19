import express from 'express';
import { pool } from '../db/init.js';

const router = express.Router();

/**
 * POST /api/coupons/validate
 * Validate a coupon code for a website and order amount
 * Public endpoint (no authentication required)
 */
router.post('/validate', async (req, res) => {
  try {
    const { website_id, code, order_amount } = req.body;

    // Validate required fields
    if (!website_id || !code || order_amount === undefined) {
      return res.status(400).json({ error: 'Missing required fields: website_id, code, and order_amount' });
    }

    const orderAmount = parseFloat(order_amount);
    if (isNaN(orderAmount) || orderAmount < 0) {
      return res.status(400).json({ error: 'Invalid order amount' });
    }

    // Find coupon
    const [coupons] = await pool.execute(
      'SELECT * FROM coupons WHERE website_id = ? AND code = ?',
      [website_id, code.toUpperCase()]
    );

    if (coupons.length === 0) {
      return res.status(404).json({ 
        valid: false,
        error: 'Coupon code not found' 
      });
    }

    const coupon = coupons[0];

    // Check if coupon is active
    if (!coupon.is_active) {
      return res.status(400).json({ 
        valid: false,
        error: 'This coupon is not active' 
      });
    }

    // Check if coupon is within valid date range
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const validFrom = new Date(coupon.valid_from);
    validFrom.setHours(0, 0, 0, 0);
    const validUntil = new Date(coupon.valid_until);
    validUntil.setHours(23, 59, 59, 999);

    if (today < validFrom) {
      return res.status(400).json({ 
        valid: false,
        error: 'This coupon is not yet valid' 
      });
    }

    if (today > validUntil) {
      return res.status(400).json({ 
        valid: false,
        error: 'This coupon has expired' 
      });
    }

    // Check usage limit
    if (coupon.usage_limit !== null && coupon.usage_count >= coupon.usage_limit) {
      return res.status(400).json({ 
        valid: false,
        error: 'This coupon has reached its usage limit' 
      });
    }

    // Check minimum order amount
    const minOrderAmount = parseFloat(coupon.min_order_amount) || 0;
    if (orderAmount < minOrderAmount) {
      return res.status(400).json({ 
        valid: false,
        error: `Minimum order amount of $${minOrderAmount.toFixed(2)} required for this coupon` 
      });
    }

    // Calculate discount
    let discountAmount = 0;
    if (coupon.discount_type === 'percentage') {
      discountAmount = (orderAmount * parseFloat(coupon.discount_value)) / 100;
      
      // Apply max discount if set
      if (coupon.max_discount_amount !== null) {
        const maxDiscount = parseFloat(coupon.max_discount_amount);
        if (discountAmount > maxDiscount) {
          discountAmount = maxDiscount;
        }
      }
    } else {
      // Fixed amount
      discountAmount = parseFloat(coupon.discount_value);
      // Don't allow discount to exceed order amount
      if (discountAmount > orderAmount) {
        discountAmount = orderAmount;
      }
    }

    res.json({
      valid: true,
      coupon: {
        id: coupon.id,
        code: coupon.code,
        description: coupon.description,
        discount_type: coupon.discount_type,
        discount_value: coupon.discount_value,
        discount_amount: parseFloat(discountAmount.toFixed(2)),
        max_discount_amount: coupon.max_discount_amount
      }
    });
  } catch (error) {
    console.error('Error validating coupon:', error);
    res.status(500).json({ 
      valid: false,
      error: 'Failed to validate coupon', 
      message: error.message 
    });
  }
});

export default router;

