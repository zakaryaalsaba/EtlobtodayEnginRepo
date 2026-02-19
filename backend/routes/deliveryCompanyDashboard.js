import express from 'express';
import bcrypt from 'bcryptjs';
import { pool } from '../db/init.js';
import multer from 'multer';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';
import { verifyDeliveryCompanyToken } from './deliveryCompanyAuth.js';

const router = express.Router();
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const zoneImageStorage = multer.diskStorage({
  destination: (req, file, cb) => {
    const uploadDir = path.join(__dirname, '../uploads/delivery-zones');
    if (!fs.existsSync(uploadDir)) fs.mkdirSync(uploadDir, { recursive: true });
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    cb(null, 'zone-' + Date.now() + '-' + Math.round(Math.random() * 1E9) + path.extname(file.originalname));
  },
});
const imageFilter = (req, file, cb) => {
  const allowed = /jpeg|jpg|png|gif|webp|svg/;
  if (allowed.test(path.extname(file.originalname).toLowerCase()) && allowed.test(file.mimetype)) {
    return cb(null, true);
  }
  cb(new Error('Only image files are allowed'));
};
const uploadZoneImage = multer({
  storage: zoneImageStorage,
  limits: { fileSize: 5 * 1024 * 1024 },
  fileFilter: imageFilter,
});

// All routes require delivery company auth
router.use(verifyDeliveryCompanyToken);

/**
 * GET /api/delivery-company/me
 * Current company profile (no password)
 */
router.get('/me', async (req, res) => {
  try {
    const company = req.deliveryCompany;
    if (company.emails) {
      try {
        company.emails = typeof company.emails === 'string' ? JSON.parse(company.emails) : company.emails;
      } catch (e) {
        company.emails = [];
      }
    } else {
      company.emails = [];
    }
    res.json({ company });
  } catch (error) {
    console.error('Error fetching delivery company profile:', error);
    res.status(500).json({ error: 'Failed to fetch profile', message: error.message });
  }
});

/**
 * GET /api/delivery-company/areas
 * List all areas (for zone creation dropdown)
 */
router.get('/areas', async (req, res) => {
  try {
    const [areas] = await pool.execute(`
      SELECT 
        a.id, 
        a.name, 
        a.name_ar, 
        a.region_id,
        r.name as region_name, 
        r.name_ar as region_name_ar,
        c.id as city_id,
        c.name as city_name, 
        c.name_ar as city_name_ar
      FROM areas a
      INNER JOIN regions r ON a.region_id = r.id
      INNER JOIN cities c ON r.city_id = c.id
      ORDER BY c.name, r.name, a.name
    `);
    res.json({ areas });
  } catch (error) {
    console.error('Error fetching areas:', error);
    res.status(500).json({ error: 'Failed to fetch areas', message: error.message });
  }
});

/**
 * GET /api/delivery-company/zones
 * List zones for this company (optionally filtered by area_id)
 */
router.get('/zones', async (req, res) => {
  try {
    const { area_id, grouped } = req.query;
    
    // If grouped=true, return zones grouped by area
    if (grouped === 'true') {
      const [zones] = await pool.execute(`
        SELECT 
          dz.id,
          dz.delivery_company_id,
          dz.area_id,
          dz.zone_name_id,
          dz.price,
          dz.status,
          dz.image_url,
          dz.image_path,
          dz.note,
          dz.created_at,
          dz.updated_at,
          dzn.name_ar as zone_name_ar,
          dzn.name_en as zone_name_en,
          a.id as area_id,
          a.name as area_name,
          a.name_ar as area_name_ar,
          r.id as region_id,
          r.name as region_name,
          r.name_ar as region_name_ar,
          c.id as city_id,
          c.name as city_name,
          c.name_ar as city_name_ar
        FROM delivery_zones dz
        INNER JOIN delivery_zones_names dzn ON dz.zone_name_id = dzn.id
        INNER JOIN areas a ON dz.area_id = a.id
        INNER JOIN regions r ON a.region_id = r.id
        INNER JOIN cities c ON r.city_id = c.id
        WHERE dz.delivery_company_id = ?
        ORDER BY c.name, r.name, a.name, dz.created_at DESC
      `, [req.deliveryCompanyId]);
      
      // Group zones by area
      const groupedZones = {};
      zones.forEach(zone => {
        const areaKey = zone.area_id;
        if (!groupedZones[areaKey]) {
          groupedZones[areaKey] = {
            area_id: zone.area_id,
            area_name: zone.area_name,
            area_name_ar: zone.area_name_ar,
            region_id: zone.region_id,
            region_name: zone.region_name,
            region_name_ar: zone.region_name_ar,
            city_id: zone.city_id,
            city_name: zone.city_name,
            city_name_ar: zone.city_name_ar,
            zones: []
          };
        }
        groupedZones[areaKey].zones.push({
          id: zone.id,
          zone_name_id: zone.zone_name_id,
          zone_name_ar: zone.zone_name_ar,
          zone_name_en: zone.zone_name_en,
          price: zone.price,
          status: zone.status,
          image_url: zone.image_url,
          note: zone.note,
          created_at: zone.created_at,
          updated_at: zone.updated_at
        });
      });
      
      return res.json({ groupedZones: Object.values(groupedZones) });
    }
    
    // Regular query (not grouped) - JOIN with delivery_zones_names
    let query = `
      SELECT 
        dz.*,
        dzn.name_ar as zone_name_ar,
        dzn.name_en as zone_name_en
      FROM delivery_zones dz
      INNER JOIN delivery_zones_names dzn ON dz.zone_name_id = dzn.id
      WHERE dz.delivery_company_id = ?
    `;
    const params = [req.deliveryCompanyId];
    
    if (area_id) {
      query += ' AND dz.area_id = ?';
      params.push(parseInt(area_id, 10));
    }
    
    query += ' ORDER BY dz.created_at DESC';
    
    const [zones] = await pool.execute(query, params);
    res.json({ zones });
  } catch (error) {
    console.error('Error fetching zones:', error);
    res.status(500).json({ error: 'Failed to fetch zones', message: error.message });
  }
});

/**
 * POST /api/delivery-company/zones
 * Create a zone for this company (zones are per area)
 */
router.post('/zones', uploadZoneImage.single('image'), async (req, res) => {
  try {
    const { zone_name_ar, zone_name_en, price, status, note, area_id } = req.body;
    if (!zone_name_ar || !zone_name_en) {
      return res.status(400).json({ error: 'Zone name in both Arabic and English are required' });
    }
    
    if (!area_id) {
      return res.status(400).json({ error: 'area_id is required. Zones must be associated with an area.' });
    }
    
    const areaIdInt = parseInt(area_id, 10);
    if (isNaN(areaIdInt)) {
      return res.status(400).json({ error: 'Invalid area_id' });
    }
    
    // Verify the area exists
    const [areas] = await pool.execute('SELECT id FROM areas WHERE id = ?', [areaIdInt]);
    if (areas.length === 0) {
      return res.status(404).json({ error: 'Area not found' });
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
    
    let imageUrl = null;
    let imagePath = null;
    if (req.file) {
      imagePath = `delivery-zones/${req.file.filename}`;
      imageUrl = `/uploads/${imagePath}`;
    }
    
    const [result] = await pool.execute(
      `INSERT INTO delivery_zones (delivery_company_id, area_id, zone_name_id, price, status, image_url, image_path, note)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        req.deliveryCompanyId,
        areaIdInt,
        zoneNameId,
        price ? parseFloat(price) : 0,
        status || 'active',
        imageUrl,
        imagePath,
        note || null,
      ]
    );
    
    // Return zone with joined zone name
    const [rows] = await pool.execute(`
      SELECT 
        dz.*,
        dzn.name_ar as zone_name_ar,
        dzn.name_en as zone_name_en
      FROM delivery_zones dz
      INNER JOIN delivery_zones_names dzn ON dz.zone_name_id = dzn.id
      WHERE dz.id = ?
    `, [result.insertId]);
    res.status(201).json({ zone: rows[0] });
  } catch (error) {
    console.error('Error creating zone:', error);
    res.status(500).json({ error: 'Failed to create zone', message: error.message });
  }
});

/**
 * PUT /api/delivery-company/zones/:id
 * Update a zone (must belong to this company)
 */
router.put('/zones/:id', uploadZoneImage.single('image'), async (req, res) => {
  try {
    const { id } = req.params;
    const { zone_name_ar, zone_name_en, price, status, note, remove_image } = req.body;

    const [existing] = await pool.execute(
      'SELECT * FROM delivery_zones WHERE id = ? AND delivery_company_id = ?',
      [id, req.deliveryCompanyId]
    );
    if (existing.length === 0) {
      return res.status(404).json({ error: 'Zone not found' });
    }

    const existingZone = existing[0];
    let imageUrl = existingZone.image_url;
    let imagePath = existingZone.image_path;

    if (remove_image === 'true' || remove_image === true) {
      if (existingZone.image_path) {
        const oldPath = path.join(__dirname, '../uploads', existingZone.image_path);
        if (fs.existsSync(oldPath)) fs.unlinkSync(oldPath);
      }
      imageUrl = null;
      imagePath = null;
    } else if (req.file) {
      if (existingZone.image_path) {
        const oldPath = path.join(__dirname, '../uploads', existingZone.image_path);
        if (fs.existsSync(oldPath)) fs.unlinkSync(oldPath);
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

    await pool.execute(
      `UPDATE delivery_zones SET zone_name_id = ?, price = ?, status = ?, image_url = ?, image_path = ?, note = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND delivery_company_id = ?`,
      [
        zoneNameId,
        price !== undefined ? parseFloat(price) : existingZone.price,
        status !== undefined ? status : existingZone.status,
        imageUrl,
        imagePath,
        note !== undefined ? note : existingZone.note,
        id,
        req.deliveryCompanyId,
      ]
    );
    
    // Return zone with joined zone name
    const [rows] = await pool.execute(`
      SELECT 
        dz.*,
        dzn.name_ar as zone_name_ar,
        dzn.name_en as zone_name_en
      FROM delivery_zones dz
      INNER JOIN delivery_zones_names dzn ON dz.zone_name_id = dzn.id
      WHERE dz.id = ?
    `, [id]);
    res.json({ zone: rows[0] });
  } catch (error) {
    console.error('Error updating zone:', error);
    res.status(500).json({ error: 'Failed to update zone', message: error.message });
  }
});

/**
 * DELETE /api/delivery-company/zones/:id
 * Delete a zone (must belong to this company)
 */
router.delete('/zones/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const [existing] = await pool.execute(
      'SELECT * FROM delivery_zones WHERE id = ? AND delivery_company_id = ?',
      [id, req.deliveryCompanyId]
    );
    if (existing.length === 0) {
      return res.status(404).json({ error: 'Zone not found' });
    }
    const zone = existing[0];
    if (zone.image_path) {
      const imagePath = path.join(__dirname, '../uploads', zone.image_path);
      if (fs.existsSync(imagePath)) fs.unlinkSync(imagePath);
    }
    await pool.execute('DELETE FROM delivery_zones WHERE id = ? AND delivery_company_id = ?', [id, req.deliveryCompanyId]);
    res.json({ message: 'Zone deleted successfully' });
  } catch (error) {
    console.error('Error deleting zone:', error);
    res.status(500).json({ error: 'Failed to delete zone', message: error.message });
  }
});

/**
 * GET /api/delivery-company/orders
 * Orders delivered by this company's drivers only
 */
router.get('/orders', async (req, res) => {
  try {
    const { dateFrom, dateTo, orderType, status } = req.query;
    let query = `
      SELECT o.*,
       d.id as driver_id,
       d.name as driver_name,
       d.phone as driver_phone,
       d.email as driver_email,
       rw.restaurant_name,
       (SELECT JSON_ARRAYAGG(
         JSON_OBJECT(
           'id', oi.id,
           'product_id', oi.product_id,
           'product_name', oi.product_name,
           'product_price', oi.product_price,
           'quantity', oi.quantity,
           'subtotal', oi.subtotal
         )
       ) FROM order_items oi WHERE oi.order_id = o.id) as items
      FROM orders o
      INNER JOIN drivers d ON o.driver_id = d.id AND d.delivery_company_id = ?
      LEFT JOIN restaurant_websites rw ON o.website_id = rw.id
      WHERE 1=1
    `;
    const params = [req.deliveryCompanyId];
    if (dateFrom) {
      query += ' AND o.created_at >= ?';
      params.push(dateFrom);
    }
    if (dateTo) {
      query += ' AND o.created_at < ?';
      params.push(dateTo);
    }
    if (orderType) {
      query += ' AND o.order_type = ?';
      params.push(orderType);
    }
    if (status) {
      query += ' AND o.status = ?';
      params.push(status);
    }
    query += ' ORDER BY o.created_at DESC';
    const [orders] = await pool.execute(query, params);
    const ordersWithItems = orders.map(order => {
      try {
        if (order.items != null) {
          if (typeof order.items === 'string') order.items = JSON.parse(order.items);
          else if (Buffer.isBuffer(order.items)) order.items = JSON.parse(order.items.toString());
          else if (!Array.isArray(order.items)) order.items = order.items ? [order.items] : [];
        } else {
          order.items = [];
        }
      } catch (e) {
        order.items = [];
      }
      return order;
    });
    res.json({ orders: ordersWithItems });
  } catch (error) {
    console.error('Error fetching delivery company orders:', error);
    res.status(500).json({ error: 'Failed to fetch orders', message: error.message });
  }
});

/**
 * GET /api/delivery-company/stats
 * Statistics for this company's drivers only
 */
router.get('/stats', async (req, res) => {
  try {
    const [rows] = await pool.execute(
      `SELECT
        COUNT(*) AS total,
        SUM(CASE WHEN is_online = 1 THEN 1 ELSE 0 END) AS online,
        SUM(CASE WHEN created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) THEN 1 ELSE 0 END) AS new_last_7_days,
        SUM(CASE WHEN status = 'pending' THEN 1 ELSE 0 END) AS pending,
        SUM(CASE WHEN status = 'approved' THEN 1 ELSE 0 END) AS approved,
        SUM(CASE WHEN status = 'rejected' THEN 1 ELSE 0 END) AS rejected
       FROM drivers WHERE delivery_company_id = ?`,
      [req.deliveryCompanyId]
    );
    const s = rows[0];
    res.json({
      totalDrivers: Number(s?.total || 0),
      onlineDrivers: Number(s?.online || 0),
      newDriversLast7Days: Number(s?.new_last_7_days || 0),
      pending: Number(s?.pending || 0),
      approved: Number(s?.approved || 0),
      rejected: Number(s?.rejected || 0),
    });
  } catch (error) {
    console.error('Error fetching delivery company stats:', error);
    res.status(500).json({ error: 'Failed to fetch stats', message: error.message });
  }
});

/**
 * GET /api/delivery-company/drivers
 * List drivers/captains belonging to this company
 */
router.get('/drivers', async (req, res) => {
  try {
    const [drivers] = await pool.execute(
      `SELECT id, name, email, phone, is_online, status, latitude, longitude, created_at, updated_at
       FROM drivers WHERE delivery_company_id = ? ORDER BY name`,
      [req.deliveryCompanyId]
    );
    res.json({ drivers });
  } catch (error) {
    console.error('Error fetching drivers:', error);
    res.status(500).json({ error: 'Failed to fetch drivers', message: error.message });
  }
});

/**
 * POST /api/delivery-company/drivers
 * Create a driver/captain for this company (approved by default so they can log in)
 */
router.post('/drivers', async (req, res) => {
  try {
    const { name, email, password, phone } = req.body;
    if (!name || !email || !password) {
      return res.status(400).json({ error: 'Name, email, and password are required' });
    }
    if (password.length < 6) {
      return res.status(400).json({ error: 'Password must be at least 6 characters long' });
    }
    const [existing] = await pool.execute('SELECT id FROM drivers WHERE email = ?', [email]);
    if (existing.length > 0) {
      return res.status(409).json({ error: 'A driver with this email already exists' });
    }
    const driverStatus = ['pending', 'approved', 'rejected'].includes(req.body.status) ? req.body.status : 'approved';
    const passwordHash = await bcrypt.hash(password, 10);
    const [result] = await pool.execute(
      `INSERT INTO drivers (name, email, password_hash, phone, status, delivery_company_id)
       VALUES (?, ?, ?, ?, ?, ?)`,
      [name.trim(), email.trim(), passwordHash, phone || null, driverStatus, req.deliveryCompanyId]
    );
    const [drivers] = await pool.execute(
      'SELECT id, name, email, phone, is_online, status, created_at, updated_at FROM drivers WHERE id = ?',
      [result.insertId]
    );
    res.status(201).json({ driver: drivers[0] });
  } catch (error) {
    console.error('Error creating driver:', error);
    res.status(500).json({ error: 'Failed to create driver', message: error.message });
  }
});

/**
 * PUT /api/delivery-company/drivers/:id
 * Update a driver (must belong to this company)
 */
router.put('/drivers/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { name, email, phone, password, status } = req.body;
    const [existing] = await pool.execute(
      'SELECT id, name, email, phone FROM drivers WHERE id = ? AND delivery_company_id = ?',
      [id, req.deliveryCompanyId]
    );
    if (existing.length === 0) {
      return res.status(404).json({ error: 'Driver not found' });
    }
    const current = existing[0];
    if (email !== undefined && email !== current.email) {
      const [dup] = await pool.execute('SELECT id FROM drivers WHERE email = ? AND id != ?', [email.trim(), id]);
      if (dup.length > 0) {
        return res.status(409).json({ error: 'Another driver already uses this email' });
      }
    }
    const updates = [];
    const values = [];
    if (name !== undefined) { updates.push('name = ?'); values.push(name.trim()); }
    if (email !== undefined) { updates.push('email = ?'); values.push(email.trim()); }
    if (phone !== undefined) { updates.push('phone = ?'); values.push(phone || null); }
    if (password !== undefined && password && password.length >= 6) {
      updates.push('password_hash = ?');
      values.push(await bcrypt.hash(password, 10));
    }
    const allowedStatuses = ['pending', 'approved', 'rejected'];
    if (status !== undefined && allowedStatuses.includes(status)) {
      updates.push('status = ?');
      values.push(status);
    }
    if (updates.length === 0) {
      const [drivers] = await pool.execute(
        'SELECT id, name, email, phone, is_online, status, created_at, updated_at FROM drivers WHERE id = ?',
        [id]
      );
      return res.json({ driver: drivers[0] });
    }
    await pool.execute(
      `UPDATE drivers SET ${updates.join(', ')}, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND delivery_company_id = ?`,
      [...values, id, req.deliveryCompanyId]
    );
    const [drivers] = await pool.execute(
      'SELECT id, name, email, phone, is_online, status, created_at, updated_at FROM drivers WHERE id = ?',
      [id]
    );
    res.json({ driver: drivers[0] });
  } catch (error) {
    console.error('Error updating driver:', error);
    res.status(500).json({ error: 'Failed to update driver', message: error.message });
  }
});

/**
 * DELETE /api/delivery-company/drivers/:id
 * Delete a driver (must belong to this company)
 */
router.delete('/drivers/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const [existing] = await pool.execute(
      'SELECT id FROM drivers WHERE id = ? AND delivery_company_id = ?',
      [id, req.deliveryCompanyId]
    );
    if (existing.length === 0) {
      return res.status(404).json({ error: 'Driver not found' });
    }
    await pool.execute('DELETE FROM drivers WHERE id = ? AND delivery_company_id = ?', [id, req.deliveryCompanyId]);
    res.json({ message: 'Driver deleted successfully' });
  } catch (error) {
    console.error('Error deleting driver:', error);
    res.status(500).json({ error: 'Failed to delete driver', message: error.message });
  }
});

/**
 * GET /api/delivery-company/store-requests
 * List store (restaurant) requests for this delivery company
 */
router.get('/store-requests', async (req, res) => {
  try {
    const [rows] = await pool.execute(
      `SELECT r.id, r.website_id, r.status, r.created_at, r.updated_at,
              w.restaurant_name, w.subdomain
       FROM store_delivery_requests r
       JOIN restaurant_websites w ON w.id = r.website_id
       WHERE r.delivery_company_id = ?
       ORDER BY r.created_at DESC`,
      [req.deliveryCompanyId]
    );
    res.json({ requests: rows });
  } catch (error) {
    console.error('Error fetching store requests:', error);
    res.status(500).json({ error: 'Failed to fetch store requests', message: error.message });
  }
});

/**
 * GET /api/delivery-company/stores/:websiteId/zones
 * Get zones for a specific store/restaurant (zones in the store's area)
 */
router.get('/stores/:websiteId/zones', async (req, res) => {
  try {
    const { websiteId } = req.params;
    const websiteIdInt = parseInt(websiteId, 10);
    
    // Verify the store is approved and get its area_id
    const [stores] = await pool.execute(
      `SELECT r.id, r.area_id FROM restaurant_websites r
       JOIN store_delivery_requests sdr ON sdr.website_id = r.id
       WHERE r.id = ? AND sdr.delivery_company_id = ? AND sdr.status = 'approved'`,
      [websiteIdInt, req.deliveryCompanyId]
    );
    
    if (stores.length === 0) {
      return res.status(404).json({ error: 'Store not found or not approved for this delivery company' });
    }
    
    const areaId = stores[0].area_id;
    if (areaId == null) {
      return res.json({ zones: [], message: 'Store has no area set. Set restaurant_websites.area_id to see zones.' });
    }
    
    const [zones] = await pool.execute(`
      SELECT 
        dz.*,
        dzn.name_ar as zone_name_ar,
        dzn.name_en as zone_name_en
      FROM delivery_zones dz
      INNER JOIN delivery_zones_names dzn ON dz.zone_name_id = dzn.id
      WHERE dz.delivery_company_id = ? AND dz.area_id = ?
      ORDER BY dz.created_at DESC
    `, [req.deliveryCompanyId, areaId]);
    res.json({ zones });
  } catch (error) {
    console.error('Error fetching store zones:', error);
    res.status(500).json({ error: 'Failed to fetch store zones', message: error.message });
  }
});

/**
 * PATCH /api/delivery-company/store-requests/:id
 * Approve or reject a store request
 */
router.patch('/store-requests/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { status } = req.body;
    if (!status || !['approved', 'rejected'].includes(status)) {
      return res.status(400).json({ error: 'status must be approved or rejected' });
    }
    const [existing] = await pool.execute(
      'SELECT id, website_id, delivery_company_id FROM store_delivery_requests WHERE id = ? AND delivery_company_id = ?',
      [id, req.deliveryCompanyId]
    );
    if (existing.length === 0) {
      return res.status(404).json({ error: 'Request not found' });
    }
    const websiteId = existing[0].website_id;
    await pool.execute(
      'UPDATE store_delivery_requests SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      [status, id]
    );
    if (status === 'approved') {
      await pool.execute(
        'UPDATE restaurant_websites SET delivery_mode = ?, delivery_company_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
        ['delivery_company', req.deliveryCompanyId, websiteId]
      );
    }
    const [updated] = await pool.execute(
      `SELECT r.id, r.website_id, r.status, r.created_at, r.updated_at,
              w.restaurant_name, w.subdomain
       FROM store_delivery_requests r
       JOIN restaurant_websites w ON w.id = r.website_id
       WHERE r.id = ?`,
      [id]
    );
    res.json({ request: updated[0] });
  } catch (error) {
    console.error('Error responding to store request:', error);
    res.status(500).json({ error: 'Failed to respond to request', message: error.message });
  }
});

export default router;
