import express from 'express';
import { pool } from '../db/init.js';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';

const router = express.Router();

/**
 * POST /api/register
 * Register a new restaurant and admin account in one step
 */
router.post('/', async (req, res) => {
  const connection = await pool.getConnection();
  try {
    await connection.beginTransaction();

    const {
      restaurant_name,
      owner_name,
      email,
      password,
      phone,
      address,
      cuisine_type,
      description
    } = req.body;

    // Validate required fields
    if (!restaurant_name || !owner_name || !email || !password || !phone) {
      await connection.rollback();
      return res.status(400).json({ 
        error: 'Missing required fields: restaurant_name, owner_name, email, password, and phone are required' 
      });
    }

    // Validate password length
    if (password.length < 6) {
      await connection.rollback();
      return res.status(400).json({ 
        error: 'Password must be at least 6 characters long' 
      });
    }

    // Check if email already exists (in admins table)
    const [existingAdmin] = await connection.execute(
      'SELECT id FROM admins WHERE email = ?',
      [email]
    );

    if (existingAdmin.length > 0) {
      await connection.rollback();
      return res.status(400).json({ 
        error: 'An account with this email already exists. Please log in instead.' 
      });
    }

    // Generate unique subdomain from restaurant name
    let subdomain = restaurant_name.toLowerCase()
      .replace(/[^a-z0-9-]/g, '')
      .replace(/--+/g, '-')
      .replace(/^-|-$/g, '');
    
    if (!subdomain) {
      subdomain = 'restaurant-' + Date.now().toString(36);
    }

    // Check if subdomain is already taken
    let attempts = 0;
    while (attempts < 10) {
      const [existingSubdomain] = await connection.execute(
        'SELECT id FROM restaurant_websites WHERE subdomain = ?',
        [subdomain]
      );
      if (existingSubdomain.length === 0) break;
      subdomain = subdomain + '-' + Date.now().toString(36).slice(-4);
      attempts++;
    }

    // Generate unique barcode code
    const generateBarcodeCode = () => {
      const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
      let result = '';
      for (let i = 0; i < 8; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length));
      }
      return result;
    };

    let barcodeCode = generateBarcodeCode();
    attempts = 0;
    while (attempts < 10) {
      const [existing] = await connection.execute(
        'SELECT id FROM restaurant_websites WHERE barcode_code = ?',
        [barcodeCode]
      );
      if (existing.length === 0) break;
      barcodeCode = generateBarcodeCode();
      attempts++;
    }

    // Create restaurant website
    const [websiteResult] = await connection.execute(
      `INSERT INTO restaurant_websites 
       (restaurant_name, phone, email, address, description, subdomain, barcode_code, is_published)
       VALUES (?, ?, ?, ?, ?, ?, ?, FALSE)`,
      [
        restaurant_name,
        phone,
        email,
        address || null,
        description || null,
        subdomain,
        barcodeCode
      ]
    );

    const websiteId = websiteResult.insertId;

    // Hash password
    const passwordHash = await bcrypt.hash(password, 10);

    // Create admin account
    await connection.execute(
      'INSERT INTO admins (website_id, name, email, password_hash) VALUES (?, ?, ?, ?)',
      [websiteId, owner_name, email, passwordHash]
    );

    await connection.commit();
    connection.release();

    // Get admin info
    const [admins] = await pool.execute(
      'SELECT a.*, rw.restaurant_name FROM admins a JOIN restaurant_websites rw ON a.website_id = rw.id WHERE a.website_id = ?',
      [websiteId]
    );

    const admin = admins[0];

    const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key-change-in-production';
    const ACCESS_TOKEN_EXPIRY = process.env.ACCESS_TOKEN_EXPIRY || '1h';
    const REFRESH_TOKEN_EXPIRY = process.env.REFRESH_TOKEN_EXPIRY || '7d';

    const token = jwt.sign(
      { adminId: admin.id, websiteId: websiteId, email: email },
      JWT_SECRET,
      { expiresIn: ACCESS_TOKEN_EXPIRY }
    );
    const refreshToken = jwt.sign(
      { adminId: admin.id, websiteId: websiteId, email: email, type: 'refresh' },
      JWT_SECRET,
      { expiresIn: REFRESH_TOKEN_EXPIRY }
    );

    res.status(201).json({
      message: 'Restaurant registered successfully',
      token,
      refreshToken,
      admin: {
        id: admin.id,
        website_id: admin.website_id,
        email: admin.email,
        name: admin.name,
        restaurant_name: admin.restaurant_name
      },
      website: {
        id: websiteId,
        restaurant_name,
        subdomain,
        barcode_code: barcodeCode
      }
    });
  } catch (error) {
    await connection.rollback();
    connection.release();
    console.error('Error registering restaurant:', error);
    res.status(500).json({ 
      error: 'Failed to register restaurant', 
      message: error.message 
    });
  }
});

export default router;

