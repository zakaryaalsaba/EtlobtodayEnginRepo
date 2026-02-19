import express from 'express';
import { pool } from '../db/init.js';
import multer from 'multer';
import path from 'path';
import { fileURLToPath } from 'url';
import fs from 'fs';
import { verifySuperAdminToken } from './superAdmin.js';

const router = express.Router();
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Configure multer for zone image uploads
const zoneImageStorage = multer.diskStorage({
  destination: (req, file, cb) => {
    const uploadDir = path.join(__dirname, '../uploads/delivery-zones');
    if (!fs.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    cb(null, 'zone-' + uniqueSuffix + path.extname(file.originalname));
  }
});

const imageFilter = (req, file, cb) => {
  const allowedTypes = /jpeg|jpg|png|gif|webp|svg/;
  const extname = allowedTypes.test(path.extname(file.originalname).toLowerCase());
  const mimetype = allowedTypes.test(file.mimetype);
  
  if (mimetype && extname) {
    return cb(null, true);
  } else {
    cb(new Error('Only image files are allowed!'));
  }
};

const uploadZoneImage = multer({
  storage: zoneImageStorage,
  limits: { fileSize: 5 * 1024 * 1024 }, // 5MB limit
  fileFilter: imageFilter
});

/**
 * GET /api/delivery-companies/:companyId/zones
 * Get all zones for a specific delivery company
 */
router.get('/:companyId/zones', verifySuperAdminToken, async (req, res) => {
  try {
    const { companyId } = req.params;
    const [zones] = await pool.execute(`
      SELECT 
        dz.*,
        dzn.name_ar as zone_name_ar,
        dzn.name_en as zone_name_en
      FROM delivery_zones dz
      INNER JOIN delivery_zones_names dzn ON dz.zone_name_id = dzn.id
      WHERE dz.delivery_company_id = ?
      ORDER BY dz.created_at DESC
    `, [companyId]);
    
    res.json({ zones });
  } catch (error) {
    console.error('Error fetching delivery zones:', error);
    res.status(500).json({ error: 'Failed to fetch delivery zones', message: error.message });
  }
});

/**
 * GET /api/delivery-zones/:id
 * Get a single zone by ID
 */
router.get('/:id', verifySuperAdminToken, async (req, res) => {
  try {
    const { id } = req.params;
    const [zones] = await pool.execute(`
      SELECT 
        dz.*,
        dzn.name_ar as zone_name_ar,
        dzn.name_en as zone_name_en
      FROM delivery_zones dz
      INNER JOIN delivery_zones_names dzn ON dz.zone_name_id = dzn.id
      WHERE dz.id = ?
    `, [id]);
    
    if (zones.length === 0) {
      return res.status(404).json({ error: 'Delivery zone not found' });
    }
    
    res.json({ zone: zones[0] });
  } catch (error) {
    console.error('Error fetching delivery zone:', error);
    res.status(500).json({ error: 'Failed to fetch delivery zone', message: error.message });
  }
});

/**
 * POST /api/delivery-companies/:companyId/zones
 * Create a new zone for a delivery company
 */
router.post('/:companyId/zones', verifySuperAdminToken, uploadZoneImage.single('image'), async (req, res) => {
  try {
    const { companyId } = req.params;
    const {
      zone_name_ar,
      zone_name_en,
      price,
      status,
      note
    } = req.body;
    
    // Validate required fields
    if (!zone_name_ar || !zone_name_en) {
      return res.status(400).json({ error: 'Zone name in both Arabic and English are required' });
    }
    
    // Check if company exists
    const [companies] = await pool.execute(
      'SELECT id FROM delivery_companies WHERE id = ?',
      [companyId]
    );
    
    if (companies.length === 0) {
      return res.status(404).json({ error: 'Delivery company not found' });
    }
    
    // Find or create zone name in delivery_zones_names
    let [zoneNames] = await pool.execute(
      'SELECT id FROM delivery_zones_names WHERE name_ar = ? AND name_en = ?',
      [zone_name_ar, zone_name_en]
    );
    
    let zoneNameId;
    if (zoneNames.length > 0) {
      zoneNameId = zoneNames[0].id;
    } else {
      // Create new zone name
      const [nameResult] = await pool.execute(
        'INSERT INTO delivery_zones_names (name_ar, name_en) VALUES (?, ?)',
        [zone_name_ar, zone_name_en]
      );
      zoneNameId = nameResult.insertId;
    }
    
    // Handle image upload
    let imageUrl = null;
    let imagePath = null;
    if (req.file) {
      imagePath = `delivery-zones/${req.file.filename}`;
      imageUrl = `/uploads/${imagePath}`;
    }
    
    // Insert into database
    const [result] = await pool.execute(
      `INSERT INTO delivery_zones 
       (delivery_company_id, zone_name_id, price, status, image_url, image_path, note)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
      [
        companyId,
        zoneNameId,
        price ? parseFloat(price) : 0.00,
        status || 'active',
        imageUrl,
        imagePath,
        note || null
      ]
    );
    
    // Fetch the created zone with joined zone name
    const [zones] = await pool.execute(`
      SELECT 
        dz.*,
        dzn.name_ar as zone_name_ar,
        dzn.name_en as zone_name_en
      FROM delivery_zones dz
      INNER JOIN delivery_zones_names dzn ON dz.zone_name_id = dzn.id
      WHERE dz.id = ?
    `, [result.insertId]);
    
    res.status(201).json({ zone: zones[0] });
  } catch (error) {
    console.error('Error creating delivery zone:', error);
    res.status(500).json({ error: 'Failed to create delivery zone', message: error.message });
  }
});

/**
 * PUT /api/delivery-zones/:id
 * Update a delivery zone
 */
router.put('/:id', verifySuperAdminToken, uploadZoneImage.single('image'), async (req, res) => {
  try {
    const { id } = req.params;
    const {
      zone_name_ar,
      zone_name_en,
      price,
      status,
      note,
      remove_image
    } = req.body;
    
    // Check if zone exists
    const [existing] = await pool.execute(
      'SELECT * FROM delivery_zones WHERE id = ?',
      [id]
    );
    
    if (existing.length === 0) {
      return res.status(404).json({ error: 'Delivery zone not found' });
    }
    
    const existingZone = existing[0];
    
    // Handle image
    let imageUrl = existingZone.image_url;
    let imagePath = existingZone.image_path;
    
    if (remove_image === 'true' || remove_image === true) {
      // Delete old image file if exists
      if (existingZone.image_path) {
        const oldImagePath = path.join(__dirname, '../uploads', existingZone.image_path);
        if (fs.existsSync(oldImagePath)) {
          fs.unlinkSync(oldImagePath);
        }
      }
      imageUrl = null;
      imagePath = null;
    } else if (req.file) {
      // Delete old image file if exists
      if (existingZone.image_path) {
        const oldImagePath = path.join(__dirname, '../uploads', existingZone.image_path);
        if (fs.existsSync(oldImagePath)) {
          fs.unlinkSync(oldImagePath);
        }
      }
      imagePath = `delivery-zones/${req.file.filename}`;
      imageUrl = `/uploads/${imagePath}`;
    }
    
    // Handle zone name update: find or create zone name in delivery_zones_names
    let zoneNameId = existingZone.zone_name_id;
    if (zone_name_ar && zone_name_en) {
      let [zoneNames] = await pool.execute(
        'SELECT id FROM delivery_zones_names WHERE name_ar = ? AND name_en = ?',
        [zone_name_ar, zone_name_en]
      );
      
      if (zoneNames.length > 0) {
        zoneNameId = zoneNames[0].id;
      } else {
        // Create new zone name
        const [nameResult] = await pool.execute(
          'INSERT INTO delivery_zones_names (name_ar, name_en) VALUES (?, ?)',
          [zone_name_ar, zone_name_en]
        );
        zoneNameId = nameResult.insertId;
      }
    }
    
    // Update database
    await pool.execute(
      `UPDATE delivery_zones SET
       zone_name_id = ?,
       price = ?,
       status = ?,
       image_url = ?,
       image_path = ?,
       note = ?,
       updated_at = CURRENT_TIMESTAMP
       WHERE id = ?`,
      [
        zoneNameId,
        price !== undefined ? parseFloat(price) : existingZone.price,
        status !== undefined ? status : existingZone.status,
        imageUrl,
        imagePath,
        note !== undefined ? note : existingZone.note,
        id
      ]
    );
    
    // Fetch updated zone with joined zone name
    const [zones] = await pool.execute(`
      SELECT 
        dz.*,
        dzn.name_ar as zone_name_ar,
        dzn.name_en as zone_name_en
      FROM delivery_zones dz
      INNER JOIN delivery_zones_names dzn ON dz.zone_name_id = dzn.id
      WHERE dz.id = ?
    `, [id]);
    
    res.json({ zone: zones[0] });
  } catch (error) {
    console.error('Error updating delivery zone:', error);
    res.status(500).json({ error: 'Failed to update delivery zone', message: error.message });
  }
});

/**
 * DELETE /api/delivery-zones/:id
 * Delete a delivery zone
 */
router.delete('/:id', verifySuperAdminToken, async (req, res) => {
  try {
    const { id } = req.params;
    
    // Check if zone exists
    const [existing] = await pool.execute(
      'SELECT * FROM delivery_zones WHERE id = ?',
      [id]
    );
    
    if (existing.length === 0) {
      return res.status(404).json({ error: 'Delivery zone not found' });
    }
    
    const zone = existing[0];
    
    // Delete image file if exists
    if (zone.image_path) {
      const imagePath = path.join(__dirname, '../uploads', zone.image_path);
      if (fs.existsSync(imagePath)) {
        fs.unlinkSync(imagePath);
      }
    }
    
    // Delete from database
    await pool.execute(
      'DELETE FROM delivery_zones WHERE id = ?',
      [id]
    );
    
    res.json({ message: 'Delivery zone deleted successfully' });
  } catch (error) {
    console.error('Error deleting delivery zone:', error);
    res.status(500).json({ error: 'Failed to delete delivery zone', message: error.message });
  }
});

export default router;
