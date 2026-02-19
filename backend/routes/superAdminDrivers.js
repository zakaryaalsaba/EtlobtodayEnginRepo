import express from 'express';
import { pool } from '../db/init.js';
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
 * GET /api/super-admin/drivers
 * Get all drivers with their status
 */
router.get('/drivers', authenticateSuperAdmin, async (req, res) => {
  try {
    const { status } = req.query;
    
    let query = 'SELECT id, name, email, phone, is_online, status, delivery_company_id, created_at, updated_at FROM drivers';
    const params = [];
    
    if (status) {
      query += ' WHERE status = ?';
      params.push(status);
    }
    
    query += ' ORDER BY created_at DESC';
    
    const [drivers] = await pool.execute(query, params);
    
    res.json(drivers);
  } catch (error) {
    console.error('Error fetching drivers:', error);
    res.status(500).json({ error: 'Failed to fetch drivers', message: error.message });
  }
});

/**
 * PUT /api/super-admin/drivers/:id/approve
 * Approve a driver
 */
router.put('/drivers/:id/approve', authenticateSuperAdmin, async (req, res) => {
  try {
    const { id } = req.params;
    const { delivery_company_id } = req.body || {};
    
    // Check if driver exists
    const [drivers] = await pool.execute(
      'SELECT id, status FROM drivers WHERE id = ?',
      [id]
    );
    
    if (drivers.length === 0) {
      return res.status(404).json({ error: 'Driver not found' });
    }
    
    // Update driver status to approved and optionally set delivery_company_id
    const companyId = delivery_company_id ? parseInt(delivery_company_id, 10) : null;
    if (companyId) {
      const [companies] = await pool.execute('SELECT id FROM delivery_companies WHERE id = ?', [companyId]);
      if (companies.length === 0) {
        return res.status(400).json({ error: 'Invalid delivery company' });
      }
    }
    await pool.execute(
      'UPDATE drivers SET status = ?, delivery_company_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      ['approved', companyId || null, id]
    );
    
    // Get updated driver
    const [updatedDrivers] = await pool.execute(
      'SELECT id, name, email, phone, is_online, status, delivery_company_id, created_at, updated_at FROM drivers WHERE id = ?',
      [id]
    );
    
    res.json({
      success: true,
      driver: updatedDrivers[0],
      message: 'Driver approved successfully'
    });
  } catch (error) {
    console.error('Error approving driver:', error);
    res.status(500).json({ error: 'Failed to approve driver', message: error.message });
  }
});

/**
 * PUT /api/super-admin/drivers/:id/reject
 * Reject a driver
 */
router.put('/drivers/:id/reject', authenticateSuperAdmin, async (req, res) => {
  try {
    const { id } = req.params;
    
    // Check if driver exists
    const [drivers] = await pool.execute(
      'SELECT id, status FROM drivers WHERE id = ?',
      [id]
    );
    
    if (drivers.length === 0) {
      return res.status(404).json({ error: 'Driver not found' });
    }
    
    // Update driver status to rejected
    await pool.execute(
      'UPDATE drivers SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      ['rejected', id]
    );
    
    // Get updated driver
    const [updatedDrivers] = await pool.execute(
      'SELECT id, name, email, phone, is_online, status, created_at, updated_at FROM drivers WHERE id = ?',
      [id]
    );
    
    res.json({
      success: true,
      driver: updatedDrivers[0],
      message: 'Driver rejected successfully'
    });
  } catch (error) {
    console.error('Error rejecting driver:', error);
    res.status(500).json({ error: 'Failed to reject driver', message: error.message });
  }
});

/**
 * DELETE /api/super-admin/drivers/:id
 * Delete a driver
 */
router.delete('/drivers/:id', authenticateSuperAdmin, async (req, res) => {
  try {
    const { id } = req.params;
    
    // Check if driver exists
    const [drivers] = await pool.execute(
      'SELECT id FROM drivers WHERE id = ?',
      [id]
    );
    
    if (drivers.length === 0) {
      return res.status(404).json({ error: 'Driver not found' });
    }
    
    // Delete driver
    await pool.execute('DELETE FROM drivers WHERE id = ?', [id]);
    
    res.json({
      success: true,
      message: 'Driver deleted successfully'
    });
  } catch (error) {
    console.error('Error deleting driver:', error);
    res.status(500).json({ error: 'Failed to delete driver', message: error.message });
  }
});

export default router;

