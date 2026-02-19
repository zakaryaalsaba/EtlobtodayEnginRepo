import express from 'express';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import multer from 'multer';
import path from 'path';
import { fileURLToPath } from 'url';
import fs from 'fs';
import { pool } from '../db/init.js';

const router = express.Router();
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key-change-in-production';

// Configure multer for driver profile image uploads
const driverImageStorage = multer.diskStorage({
  destination: (req, file, cb) => {
    const uploadDir = path.join(__dirname, '../uploads');
    if (!fs.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    cb(null, 'driver-' + uniqueSuffix + path.extname(file.originalname));
  }
});

const imageFilter = (req, file, cb) => {
  const allowedTypes = /jpeg|jpg|png|gif|webp/;
  const extname = allowedTypes.test(path.extname(file.originalname).toLowerCase());
  const mimetype = file.mimetype && allowedTypes.test(file.mimetype);
  
  if (mimetype && extname) {
    return cb(null, true);
  } else {
    cb(new Error('Only image files are allowed!'));
  }
};

const uploadDriverImage = multer({
  storage: driverImageStorage,
  limits: { fileSize: 5 * 1024 * 1024 }, // 5MB limit
  fileFilter: imageFilter
});

/**
 * Helper function to ensure image_url column exists in drivers table
 */
async function ensureImageUrlColumn() {
  try {
    const dbName = process.env.MYSQL_DB || 'restaurant_websites';
    const [columns] = await pool.execute(`
      SELECT COLUMN_NAME 
      FROM INFORMATION_SCHEMA.COLUMNS 
      WHERE TABLE_SCHEMA = ? 
      AND TABLE_NAME = 'drivers'
      AND COLUMN_NAME = 'image_url'
    `, [dbName]);

    if (columns.length === 0) {
      console.log('Adding image_url column to drivers table...');
      await pool.execute(`
        ALTER TABLE drivers 
        ADD COLUMN image_url VARCHAR(500) NULL
      `);
      console.log('Successfully added image_url column to drivers table');
      
      // Verify it was added
      const [verifyColumns] = await pool.execute(`
        SELECT COLUMN_NAME 
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = ? 
        AND TABLE_NAME = 'drivers'
        AND COLUMN_NAME = 'image_url'
      `, [dbName]);
      
      if (verifyColumns.length === 0) {
        throw new Error('Failed to verify image_url column was added');
      }
      
      return true;
    }
    return true;
  } catch (error) {
    console.error('Error ensuring image_url column:', error);
    // Don't throw - let the calling function handle gracefully
    return false;
  }
}

/**
 * Helper function to get driver with image_url (handles missing column)
 */
async function getDriverWithImage(driverId) {
  try {
    // First ensure column exists
    const columnExists = await ensureImageUrlColumn();
    
    // Try with image_url first
    let query = 'SELECT id, name, email, phone, is_online, status';
    if (columnExists) {
      query += ', image_url';
    }
    query += ' FROM drivers WHERE id = ?';
    
    const [drivers] = await pool.execute(query, [driverId]);

    if (drivers.length === 0) {
      return null;
    }

    const driver = drivers[0];
    const baseUrl = process.env.API_BASE_URL || `http://localhost:${process.env.PORT || 3000}`;
    const imageUrl = driver.image_url 
      ? (driver.image_url.startsWith('http') ? driver.image_url : `${baseUrl}/uploads/${driver.image_url}`)
      : null;

    return {
      id: driver.id,
      name: driver.name,
      email: driver.email,
      phone: driver.phone,
      isOnline: driver.is_online === 1 || driver.is_online === true,
      status: driver.status,
      image_url: imageUrl
    };
  } catch (error) {
    // If column doesn't exist, try without it
    if (error.message.includes('Unknown column') || error.message.includes('image_url')) {
      console.warn('image_url column not available, fetching driver without it');
      const [drivers] = await pool.execute(
        'SELECT id, name, email, phone, is_online, status FROM drivers WHERE id = ?',
        [driverId]
      );
      if (drivers.length === 0) {
        return null;
      }
      const driver = drivers[0];
      return {
        id: driver.id,
        name: driver.name,
        email: driver.email,
        phone: driver.phone,
        isOnline: driver.is_online === 1 || driver.is_online === true,
        status: driver.status,
        image_url: null
      };
    }
    throw error;
  }
}

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
 * POST /api/drivers/register
 * Register a new driver
 */
router.post('/register', async (req, res) => {
  try {
    const { name, email, password, phone } = req.body;

    if (!name || !email || !password) {
      return res.status(400).json({ error: 'Name, email, and password are required' });
    }

    if (password.length < 6) {
      return res.status(400).json({ error: 'Password must be at least 6 characters long' });
    }

    // Check if driver already exists
    const [existing] = await pool.execute(
      'SELECT id FROM drivers WHERE email = ?',
      [email]
    );

    if (existing.length > 0) {
      return res.status(409).json({ error: 'Driver with this email already exists' });
    }

    // Hash password
    const passwordHash = await bcrypt.hash(password, 10);

    // Create driver with pending status (requires admin approval)
    const [result] = await pool.execute(
      'INSERT INTO drivers (name, email, password_hash, phone, status) VALUES (?, ?, ?, ?, ?)',
      [name, email, passwordHash, phone || null, 'pending']
    );

    const [drivers] = await pool.execute(
      'SELECT id, name, email, phone, is_online, status FROM drivers WHERE id = ?',
      [result.insertId]
    );

    const driver = drivers[0];

    // Don't generate token for pending drivers - they need approval first
    res.status(201).json({
      driver: {
        id: driver.id,
        name: driver.name,
        email: driver.email,
        phone: driver.phone,
        isOnline: driver.is_online === 1 || driver.is_online === true,
        status: driver.status
      },
      message: 'Registration successful. Your account is pending approval from the administrator.'
    });
  } catch (error) {
    console.error('Error registering driver:', error);
    res.status(500).json({ error: 'Failed to register driver', message: error.message });
  }
});

/**
 * POST /api/drivers/login
 * Driver login
 */
router.post('/login', async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password are required' });
    }

    // Find driver by email
    const [drivers] = await pool.execute(
      'SELECT id, name, email, phone, password_hash, is_online, status FROM drivers WHERE email = ?',
      [email]
    );

    if (drivers.length === 0) {
      return res.status(401).json({ error: 'Invalid email or password' });
    }

    const driver = drivers[0];

    // Verify password
    const isValidPassword = await bcrypt.compare(password, driver.password_hash);
    if (!isValidPassword) {
      return res.status(401).json({ error: 'Invalid email or password' });
    }

    // Check if driver is approved
    if (driver.status !== 'approved') {
      if (driver.status === 'pending') {
        return res.status(403).json({ error: 'Your account is pending approval. Please wait for administrator approval.' });
      } else if (driver.status === 'rejected') {
        return res.status(403).json({ error: 'Your account has been rejected. Please contact support.' });
      }
    }

    // Generate JWT token
    const token = jwt.sign(
      { driverId: driver.id, email: driver.email, role: 'driver' },
      JWT_SECRET,
      { expiresIn: '30d' }
    );

    res.json({
      driver: {
        id: driver.id,
        name: driver.name,
        email: driver.email,
        phone: driver.phone,
        isOnline: driver.is_online === 1 || driver.is_online === true,
        status: driver.status
      },
      token
    });
  } catch (error) {
    console.error('Error logging in driver:', error);
    res.status(500).json({ error: 'Failed to login', message: error.message });
  }
});

/**
 * GET /api/drivers/me
 * Get current driver profile
 */
router.get('/me', authenticateDriver, async (req, res) => {
  try {
    const driverId = req.driverId;
    const driver = await getDriverWithImage(driverId);

    if (!driver) {
      return res.status(404).json({ error: 'Driver not found' });
    }

    res.json(driver);
  } catch (error) {
    console.error('Error fetching driver profile:', error);
    res.status(500).json({ error: 'Failed to fetch driver profile', message: error.message });
  }
});

/**
 * PUT /api/drivers/status
 * Update driver online/offline status
 */
router.put('/status', authenticateDriver, async (req, res) => {
  try {
    const driverId = req.driverId;
    const { isOnline } = req.body;

    if (typeof isOnline !== 'boolean') {
      return res.status(400).json({ error: 'isOnline must be a boolean' });
    }

    await pool.execute(
      'UPDATE drivers SET is_online = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      [isOnline, driverId]
    );

    const [drivers] = await pool.execute(
      'SELECT id, name, email, phone, is_online FROM drivers WHERE id = ?',
      [driverId]
    );

    const driver = drivers[0];
    res.json({
      id: driver.id,
      name: driver.name,
      email: driver.email,
      phone: driver.phone,
      isOnline: driver.is_online === 1 || driver.is_online === true
    });
  } catch (error) {
    console.error('Error updating driver status:', error);
    res.status(500).json({ error: 'Failed to update status', message: error.message });
  }
});

/**
 * PUT /api/drivers/location
 * Update driver location
 */
router.put('/location', authenticateDriver, async (req, res) => {
  try {
    const driverId = req.driverId;
    const { latitude, longitude } = req.body;

    if (latitude === undefined || longitude === undefined) {
      return res.status(400).json({ error: 'Latitude and longitude are required' });
    }

    await pool.execute(
      'UPDATE drivers SET latitude = ?, longitude = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      [latitude, longitude, driverId]
    );

    res.json({ success: true, message: 'Location updated' });
  } catch (error) {
    console.error('Error updating driver location:', error);
    res.status(500).json({ error: 'Failed to update location', message: error.message });
  }
});

/**
 * PUT /api/drivers/device-token
 * Update driver device token for push notifications
 */
router.put('/device-token', authenticateDriver, async (req, res) => {
  try {
    const driverId = req.driverId;
    const { device_token, device_type } = req.body;

    if (!device_token) {
      return res.status(400).json({ error: 'Device token is required' });
    }

    const validDeviceTypes = ['android', 'ios'];
    const finalDeviceType = device_type && validDeviceTypes.includes(device_type.toLowerCase()) 
      ? device_type.toLowerCase() 
      : 'android';

    // Check if device_token column exists
    const [columns] = await pool.execute(`
      SELECT COLUMN_NAME 
      FROM INFORMATION_SCHEMA.COLUMNS 
      WHERE TABLE_SCHEMA = ? 
      AND TABLE_NAME = 'drivers'
      AND COLUMN_NAME IN ('device_token', 'device_type')
    `, [process.env.MYSQL_DB || 'restaurant_websites']);
    
    const existingColumns = columns.map(col => col.COLUMN_NAME);
    const hasDeviceToken = existingColumns.includes('device_token');
    const hasDeviceType = existingColumns.includes('device_type');

    console.log(`Driver ${driverId} updating device token. Columns exist: device_token=${hasDeviceToken}, device_type=${hasDeviceType}`);

    if (hasDeviceToken && hasDeviceType) {
      await pool.execute(
        'UPDATE drivers SET device_token = ?, device_type = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
        [device_token, finalDeviceType, driverId]
      );
      console.log(`Device token updated for driver ${driverId}`);
    } else if (hasDeviceToken) {
      await pool.execute(
        'UPDATE drivers SET device_token = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
        [device_token, driverId]
      );
      console.log(`Device token updated for driver ${driverId} (device_type column missing)`);
    } else {
      // Columns don't exist - try to add them
      console.warn('Device token columns not found in drivers table. Attempting to add them...');
      try {
        if (!hasDeviceToken) {
          await pool.execute(`
            ALTER TABLE drivers 
            ADD COLUMN device_token VARCHAR(500) NULL
          `);
          console.log('Added device_token column to drivers table');
        }
        if (!hasDeviceType) {
          await pool.execute(`
            ALTER TABLE drivers 
            ADD COLUMN device_type ENUM('android', 'ios') NULL
          `);
          console.log('Added device_type column to drivers table');
        }
        // Retry the update
        await pool.execute(
          'UPDATE drivers SET device_token = ?, device_type = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
          [device_token, finalDeviceType, driverId]
        );
        console.log(`Device token updated for driver ${driverId} after adding columns`);
      } catch (alterError) {
        console.error('Error adding device token columns:', alterError);
        return res.status(500).json({ 
          error: 'Device token feature not available', 
          message: alterError.message 
        });
      }
    }

    res.json({ success: true, message: 'Device token updated' });
  } catch (error) {
    console.error('Error updating device token:', error);
    res.status(500).json({ error: 'Failed to update device token', message: error.message });
  }
});

/**
 * PUT /api/drivers/profile
 * Update driver profile (name, phone)
 */
router.put('/profile', authenticateDriver, async (req, res) => {
  try {
    const driverId = req.driverId;
    const { name, phone } = req.body;

    // Ensure image_url column exists before proceeding
    await ensureImageUrlColumn();

    // Build update query dynamically based on provided fields
    const updateFields = [];
    const updateValues = [];

    if (name !== undefined) {
      if (!name || name.trim().length === 0) {
        return res.status(400).json({ error: 'Name cannot be empty' });
      }
      updateFields.push('name = ?');
      updateValues.push(name.trim());
    }

    if (phone !== undefined) {
      updateFields.push('phone = ?');
      updateValues.push(phone && phone.trim().length > 0 ? phone.trim() : null);
    }

    if (updateFields.length === 0) {
      return res.status(400).json({ error: 'No fields to update' });
    }

    // Add updated_at
    updateFields.push('updated_at = CURRENT_TIMESTAMP');
    updateValues.push(driverId);

    await pool.execute(
      `UPDATE drivers SET ${updateFields.join(', ')} WHERE id = ?`,
      updateValues
    );

    // Fetch updated driver using helper function
    const driver = await getDriverWithImage(driverId);
    
    if (!driver) {
      return res.status(404).json({ error: 'Driver not found' });
    }

    res.json(driver);
  } catch (error) {
    console.error('Error updating driver profile:', error);
    res.status(500).json({ error: 'Failed to update profile', message: error.message });
  }
});

/**
 * POST /api/drivers/profile/image
 * Upload driver profile image
 */
router.post('/profile/image', authenticateDriver, uploadDriverImage.single('image'), async (req, res) => {
  try {
    const driverId = req.driverId;

    if (!req.file) {
      return res.status(400).json({ error: 'No image file provided' });
    }

    const imageFilename = req.file.filename;

    // Ensure image_url column exists
    await ensureImageUrlColumn();

    // Delete old image if exists
    try {
      const [drivers] = await pool.execute(
        'SELECT image_url FROM drivers WHERE id = ?',
        [driverId]
      );

      if (drivers.length > 0 && drivers[0].image_url) {
        const oldImagePath = path.join(__dirname, '../uploads', drivers[0].image_url);
        if (fs.existsSync(oldImagePath)) {
          try {
            fs.unlinkSync(oldImagePath);
          } catch (unlinkError) {
            console.warn('Failed to delete old image:', unlinkError);
          }
        }
      }
    } catch (selectError) {
      // Column might not exist yet, that's okay
      console.warn('Could not check for old image:', selectError.message);
    }

    // Update driver with new image
    await pool.execute(
      'UPDATE drivers SET image_url = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      [imageFilename, driverId]
    );

    // Fetch updated driver using helper function
    const driver = await getDriverWithImage(driverId);
    
    if (!driver) {
      return res.status(404).json({ error: 'Driver not found' });
    }

    // Override image_url with the new uploaded image URL
    const baseUrl = process.env.API_BASE_URL || `http://localhost:${process.env.PORT || 3000}`;
    driver.image_url = `${baseUrl}/uploads/${imageFilename}`;

    res.json(driver);
  } catch (error) {
    console.error('Error uploading driver image:', error);
    res.status(500).json({ error: 'Failed to upload image', message: error.message });
  }
});

export default router;

