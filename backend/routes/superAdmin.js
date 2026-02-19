import express from 'express';
import { pool } from '../db/init.js';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';

const router = express.Router();

/**
 * Verify super admin token middleware
 */
export const verifySuperAdminToken = (req, res, next) => {
  try {
    const token = req.headers.authorization?.split(' ')[1];
    
    if (!token) {
      return res.status(401).json({ error: 'No token provided' });
    }

    const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key-change-in-production';
    const decoded = jwt.verify(token, JWT_SECRET);
    
    // Check if token has superAdmin flag
    if (!decoded.superAdmin) {
      return res.status(403).json({ error: 'Access denied. Super admin privileges required.' });
    }

    req.superAdmin = decoded;
    next();
  } catch (error) {
    console.error('Token verification error:', error);
    res.status(401).json({ error: 'Invalid token' });
  }
};

/**
 * POST /api/super-admin/login
 * Super admin login
 */
router.post('/login', async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password are required' });
    }

    // Find super admin by email
    const [superAdmins] = await pool.execute(
      'SELECT * FROM super_admins WHERE email = ?',
      [email]
    );

    if (superAdmins.length === 0) {
      return res.status(401).json({ error: 'Invalid email or password' });
    }

    const superAdmin = superAdmins[0];

    // Verify password
    const isValidPassword = await bcrypt.compare(password, superAdmin.password_hash);

    if (!isValidPassword) {
      return res.status(401).json({ error: 'Invalid email or password' });
    }

    // Generate JWT token
    const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key-change-in-production';
    const token = jwt.sign(
      { 
        superAdminId: superAdmin.id, 
        email: superAdmin.email,
        superAdmin: true 
      },
      JWT_SECRET,
      { expiresIn: '7d' }
    );

    res.json({
      token,
      superAdmin: {
        id: superAdmin.id,
        email: superAdmin.email,
        name: superAdmin.name
      }
    });
  } catch (error) {
    console.error('Error during super admin login:', error);
    res.status(500).json({ error: 'Failed to login', message: error.message });
  }
});

/**
 * GET /api/super-admin/me
 * Get current super admin info
 */
router.get('/me', verifySuperAdminToken, async (req, res) => {
  try {
    const [superAdmins] = await pool.execute(
      'SELECT id, email, name, created_at FROM super_admins WHERE id = ?',
      [req.superAdmin.superAdminId]
    );

    if (superAdmins.length === 0) {
      return res.status(404).json({ error: 'Super admin not found' });
    }

    res.json({ superAdmin: superAdmins[0] });
  } catch (error) {
    console.error('Error fetching super admin:', error);
    res.status(500).json({ error: 'Failed to fetch super admin', message: error.message });
  }
});

export default router;

