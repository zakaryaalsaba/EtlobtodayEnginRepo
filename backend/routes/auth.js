import express from 'express';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { pool } from '../db/init.js';

const router = express.Router();

// JWT secret (should be in .env in production)
const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key-change-in-production';
const ACCESS_TOKEN_EXPIRY = process.env.ACCESS_TOKEN_EXPIRY || '1h';   // short-lived access token
const REFRESH_TOKEN_EXPIRY = process.env.REFRESH_TOKEN_EXPIRY || '7d'; // refresh token used to get new access token

/**
 * POST /api/auth/refresh
 * Exchange a valid refresh token for a new access token (ID token).
 * Call this when the access token expires or on app load to restore session.
 */
router.post('/refresh', async (req, res) => {
  try {
    const { refreshToken } = req.body;
    if (!refreshToken) {
      return res.status(400).json({ error: 'refreshToken is required' });
    }

    let decoded;
    try {
      decoded = jwt.verify(refreshToken, JWT_SECRET);
    } catch (err) {
      if (err.name === 'TokenExpiredError') {
        return res.status(401).json({ error: 'Refresh token expired', code: 'REFRESH_EXPIRED' });
      }
      return res.status(401).json({ error: 'Invalid refresh token' });
    }

    if (decoded.type !== 'refresh') {
      return res.status(401).json({ error: 'Invalid refresh token' });
    }

    // Issue new access token (ID token) based on user type
    let tokenPayload;
    if (decoded.adminId != null && decoded.websiteId != null) {
      tokenPayload = { adminId: decoded.adminId, websiteId: decoded.websiteId, email: decoded.email };
    } else if (decoded.customerId != null) {
      tokenPayload = { customerId: decoded.customerId, email: decoded.email, type: 'customer' };
    } else if (decoded.deliveryCompanyId != null) {
      tokenPayload = { deliveryCompanyId: decoded.deliveryCompanyId, type: 'delivery_company' };
    } else {
      return res.status(401).json({ error: 'Invalid refresh token payload' });
    }

    const token = jwt.sign(tokenPayload, JWT_SECRET, { expiresIn: ACCESS_TOKEN_EXPIRY });

    res.json({ token });
  } catch (error) {
    console.error('Refresh token error:', error);
    res.status(500).json({ error: 'Failed to refresh token', message: error.message });
  }
});

/**
 * POST /api/auth/register
 * Register a new customer with email and password
 */
router.post('/register', async (req, res) => {
  try {
    const { name, email, password, phone, address, latitude, longitude } = req.body;

    // Validate required fields
    if (!name || !email || !password) {
      return res.status(400).json({ 
        error: 'Missing required fields: name, email, and password are required' 
      });
    }

    // Validate password strength
    if (password.length < 6) {
      return res.status(400).json({ 
        error: 'Password must be at least 6 characters long' 
      });
    }

    // Check if customer already exists
    const [existing] = await pool.execute(
      'SELECT id FROM customers WHERE email = ?',
      [email]
    );

    if (existing.length > 0) {
      return res.status(409).json({ 
        error: 'Customer with this email already exists' 
      });
    }

    // Hash password
    const passwordHash = await bcrypt.hash(password, 10);

    // Create customer (website_id can be null for app users, they can order from any restaurant)
    // Include latitude and longitude if provided
    const [result] = await pool.execute(
      'INSERT INTO customers (name, email, password_hash, phone, address, latitude, longitude, last_location_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?)',
      [
        name, 
        email, 
        passwordHash, 
        phone || null, 
        address || null,
        latitude || null,
        longitude || null,
        (latitude && longitude) ? new Date() : null
      ]
    );

    const token = jwt.sign(
      { customerId: result.insertId, email, type: 'customer' },
      JWT_SECRET,
      { expiresIn: ACCESS_TOKEN_EXPIRY }
    );
    const refreshToken = jwt.sign(
      { customerId: result.insertId, email, type: 'refresh' },
      JWT_SECRET,
      { expiresIn: REFRESH_TOKEN_EXPIRY }
    );

    const [newCustomer] = await pool.execute(
      'SELECT id, name, email, phone, address, created_at FROM customers WHERE id = ?',
      [result.insertId]
    );

    res.status(201).json({
      customer: newCustomer[0],
      token,
      refreshToken,
      message: 'Registration successful'
    });
  } catch (error) {
    console.error('Error registering customer:', error);
    res.status(500).json({ 
      error: 'Failed to register customer', 
      message: error.message 
    });
  }
});

/**
 * POST /api/auth/login
 * Login customer with email and password
 */
router.post('/login', async (req, res) => {
  try {
    const { email, password } = req.body;

    // Validate required fields
    if (!email || !password) {
      return res.status(400).json({ 
        error: 'Email and password are required' 
      });
    }

    // Find customer by email
    const [customers] = await pool.execute(
      'SELECT id, name, email, password_hash, phone, address FROM customers WHERE email = ?',
      [email]
    );

    if (customers.length === 0) {
      return res.status(401).json({ 
        error: 'Invalid email or password' 
      });
    }

    const customer = customers[0];

    // Check if customer has a password (for backward compatibility with existing customers)
    if (!customer.password_hash) {
      return res.status(401).json({ 
        error: 'Account not set up with password. Please register with a password.' 
      });
    }

    // Verify password
    const isValidPassword = await bcrypt.compare(password, customer.password_hash);

    if (!isValidPassword) {
      return res.status(401).json({ 
        error: 'Invalid email or password' 
      });
    }

    // Access token (ID token) - short-lived; use refresh token to get new one
    const token = jwt.sign(
      { customerId: customer.id, email: customer.email, type: 'customer' },
      JWT_SECRET,
      { expiresIn: ACCESS_TOKEN_EXPIRY }
    );
    const refreshToken = jwt.sign(
      { customerId: customer.id, email: customer.email, type: 'refresh' },
      JWT_SECRET,
      { expiresIn: REFRESH_TOKEN_EXPIRY }
    );

    delete customer.password_hash;

    res.json({ customer, token, refreshToken, message: 'Login successful' });
  } catch (error) {
    console.error('Error logging in customer:', error);
    res.status(500).json({ 
      error: 'Failed to login', 
      message: error.message 
    });
  }
});

/**
 * POST /api/auth/login/phone
 * Login or register customer using phone number (after Firebase phone verification)
 */
router.post('/login/phone', async (req, res) => {
  try {
    const { phone, firebase_token, latitude, longitude, address } = req.body;

    // Basic validation
    if (!phone) {
      return res.status(400).json({
        error: 'Phone number is required',
      });
    }

    // TODO (optional): verify firebase_token with Firebase Admin SDK
    // For now we trust that the mobile app only calls this after successful Firebase verification.
    console.log('[AUTH][PHONE] Login requested', {
      phone,
      hasFirebaseToken: !!firebase_token,
      hasLocation: !!(latitude && longitude),
      hasAddress: !!address,
    });

    // Try to find existing customer by phone (global app customer, website_id can be NULL)
    const [existingCustomers] = await pool.execute(
      'SELECT id, name, email, phone, address FROM customers WHERE phone = ? LIMIT 1',
      [phone]
    );

    let customer;

    if (existingCustomers.length > 0) {
      customer = existingCustomers[0];
      console.log('[AUTH][PHONE] Existing customer found for phone', phone, 'id=', customer.id);
      
      // Update location and address if provided
      if (latitude && longitude) {
        const updateFields = ['latitude = ?', 'longitude = ?', 'last_location_updated = CURRENT_TIMESTAMP'];
        const updateValues = [latitude, longitude];
        
        if (address) {
          updateFields.push('address = ?');
          updateValues.push(address);
        }
        
        updateValues.push(customer.id);
        
        await pool.execute(
          `UPDATE customers SET ${updateFields.join(', ')} WHERE id = ?`,
          updateValues
        );
        console.log('[AUTH][PHONE] Updated location for existing customer', customer.id, 'address:', address || 'none');
      }
    } else {
      // Create a minimal customer record for phone-only login
      const displayName = phone; // You can change this later in profile screen

      const [insertResult] = await pool.execute(
        'INSERT INTO customers (name, email, phone, address, latitude, longitude, last_location_updated) VALUES (?, ?, ?, ?, ?, ?, ?)',
        [
          displayName, 
          null, 
          phone, 
          address || null,
          latitude || null,
          longitude || null,
          (latitude && longitude) ? new Date() : null
        ]
      );

      const newCustomerId = insertResult.insertId;

      const [newCustomerRows] = await pool.execute(
        'SELECT id, name, email, phone, address FROM customers WHERE id = ?',
        [newCustomerId]
      );

      customer = newCustomerRows[0];
      console.log('[AUTH][PHONE] New customer created for phone', phone, 'id=', customer.id);
    }

    // Ensure email is never null in the JSON response (Android model expects a non-null String)
    customer = {
      ...customer,
      email: customer.email || '',
    };

    const token = jwt.sign(
      { customerId: customer.id, email: customer.email || null, type: 'customer' },
      JWT_SECRET,
      { expiresIn: ACCESS_TOKEN_EXPIRY }
    );
    const refreshToken = jwt.sign(
      { customerId: customer.id, email: customer.email || null, type: 'refresh' },
      JWT_SECRET,
      { expiresIn: REFRESH_TOKEN_EXPIRY }
    );

    res.json({ customer, token, refreshToken, message: 'Login successful' });
  } catch (error) {
    console.error('Error logging in customer with phone:', error);
    res.status(500).json({
      error: 'Failed to login with phone',
      message: error.message,
    });
  }
});

/**
 * GET /api/auth/me
 * Get current authenticated customer
 */
router.get('/me', async (req, res) => {
  try {
    // Get token from Authorization header
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({ error: 'No token provided' });
    }

    const token = authHeader.substring(7);

    // Verify token
    let decoded;
    try {
      decoded = jwt.verify(token, JWT_SECRET);
    } catch (err) {
      return res.status(401).json({ error: 'Invalid or expired token' });
    }

    // Get customer from database
    const [customers] = await pool.execute(
      'SELECT id, name, email, phone, address, created_at FROM customers WHERE id = ?',
      [decoded.customerId]
    );

    if (customers.length === 0) {
      return res.status(404).json({ error: 'Customer not found' });
    }

    res.json({ customer: customers[0] });
  } catch (error) {
    console.error('Error fetching customer:', error);
    res.status(500).json({ 
      error: 'Failed to fetch customer', 
      message: error.message 
    });
  }
});

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
      { expiresIn: ACCESS_TOKEN_EXPIRY }
    );
    const refreshToken = jwt.sign(
      { deliveryCompanyId: company.id, type: 'refresh' },
      JWT_SECRET,
      { expiresIn: REFRESH_TOKEN_EXPIRY }
    );

    res.json({
      token,
      refreshToken,
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

