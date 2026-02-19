import express from 'express';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { pool } from '../db/init.js';

const router = express.Router();
const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key-change-in-production';

/**
 * Middleware to verify delivery company admin token
 */
export async function verifyDeliveryCompanyToken(req, res, next) {
  try {
    const token = req.headers.authorization?.split(' ')[1];
    if (!token) {
      return res.status(401).json({ error: 'No token provided' });
    }
    const decoded = jwt.verify(token, JWT_SECRET);
    if (decoded.type !== 'delivery_company' || !decoded.deliveryCompanyId) {
      return res.status(401).json({ error: 'Invalid token' });
    }
    const [companies] = await pool.execute(
      'SELECT id, company_name, contact_name, phone, address, emails, website, status, profile_image_url FROM delivery_companies WHERE id = ? AND status = ?',
      [decoded.deliveryCompanyId, 'active']
    );
    if (companies.length === 0) {
      return res.status(401).json({ error: 'Company not found or inactive' });
    }
    req.deliveryCompany = companies[0];
    req.deliveryCompanyId = decoded.deliveryCompanyId;
    next();
  } catch (error) {
    if (error.name === 'TokenExpiredError') {
      return res.status(401).json({ error: 'Token expired', expired: true });
    }
    console.error('Delivery company token verification error:', error.message);
    res.status(401).json({ error: 'Invalid token' });
  }
}

/**
 * POST /api/auth/delivery-company/login
 * Login for delivery company admin (username + password)
 */
router.post('/delivery-company/login', async (req, res) => {
  try {
    const { username, password } = req.body;
    if (!username || !password) {
      return res.status(400).json({ error: 'Username and password are required' });
    }

    const [companies] = await pool.execute(
      'SELECT id, company_name, contact_name, admin_username, admin_password_hash, status FROM delivery_companies WHERE admin_username = ?',
      [username.trim()]
    );

    if (companies.length === 0) {
      return res.status(401).json({ error: 'Invalid username or password' });
    }

    const company = companies[0];
    if (company.status !== 'active') {
      return res.status(403).json({ error: 'Company account is not active' });
    }

    if (!company.admin_password_hash) {
      return res.status(401).json({ error: 'Account not set up. Contact platform administrator.' });
    }

    const valid = await bcrypt.compare(password, company.admin_password_hash);
    if (!valid) {
      return res.status(401).json({ error: 'Invalid username or password' });
    }

    const token = jwt.sign(
      { deliveryCompanyId: company.id, type: 'delivery_company' },
      JWT_SECRET,
      { expiresIn: '30d' }
    );

    res.json({
      token,
      company: {
        id: company.id,
        company_name: company.company_name,
        contact_name: company.contact_name,
        status: company.status,
      },
      message: 'Login successful',
    });
  } catch (error) {
    console.error('Delivery company login error:', error);
    res.status(500).json({ error: 'Login failed', message: error.message });
  }
});

export default router;
