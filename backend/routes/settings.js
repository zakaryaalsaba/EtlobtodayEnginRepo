import express from 'express';
import { pool } from '../db/init.js';

const router = express.Router();

/**
 * GET /api/settings
 * Returns the active settings row (service_fee). Used by app to show service fee in checkout.
 */
router.get('/', async (req, res) => {
  try {
    const [rows] = await pool.execute(
      "SELECT id, service_fee, status FROM settings WHERE status = 'active' LIMIT 1"
    );
    if (rows.length === 0) {
      return res.json({ service_fee: 0 });
    }
    const row = rows[0];
    res.json({
      id: row.id,
      service_fee: parseFloat(row.service_fee) || 0,
      status: row.status
    });
  } catch (error) {
    console.error('Error fetching settings:', error);
    res.status(500).json({ error: 'Failed to fetch settings', message: error.message });
  }
});

export default router;
