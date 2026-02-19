import express from 'express';
import { pool } from '../db/init.js';
import { verifyAdminToken } from './admin.js';
import { saveOrderDeliveryToFirebase } from '../services/firebaseOrderSync.js';
import CacheService from '../services/cacheService.js';
import multer from 'multer';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const router = express.Router();

// Multer for addon image uploads
const addonImageStorage = multer.diskStorage({
  destination: (req, file, cb) => {
    const uploadDir = path.join(__dirname, '../uploads');
    if (!fs.existsSync(uploadDir)) fs.mkdirSync(uploadDir, { recursive: true });
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    cb(null, 'addon-' + Date.now() + '-' + Math.round(Math.random() * 1E9) + path.extname(file.originalname));
  }
});
const uploadAddonImage = multer({
  storage: addonImageStorage,
  limits: { fileSize: 5 * 1024 * 1024 },
  fileFilter: (req, file, cb) => (file.mimetype.startsWith('image/') ? cb(null, true) : cb(new Error('Only image files allowed'), false))
});

/**
 * GET /api/restaurant/website
 * Get restaurant's own website (restaurant-scoped)
 */
router.get('/website', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    
    const [websites] = await pool.execute(
      'SELECT * FROM restaurant_websites WHERE id = ?',
      [websiteId]
    );
    
    if (websites.length === 0) {
      return res.status(404).json({ error: 'Website not found' });
    }
    
    res.json({ website: websites[0] });
  } catch (error) {
    console.error('Error fetching restaurant website:', error);
    res.status(500).json({ error: 'Failed to fetch website', message: error.message });
  }
});

/**
 * GET /api/restaurant/delivery-companies
 * List delivery companies for dropdown (id, company_name only)
 */
router.get('/delivery-companies', verifyAdminToken, async (req, res) => {
  try {
    const [rows] = await pool.execute(
      'SELECT id, company_name FROM delivery_companies WHERE status = ? ORDER BY company_name',
      ['active']
    );
    res.json({ companies: rows });
  } catch (error) {
    console.error('Error listing delivery companies:', error);
    res.status(500).json({ error: 'Failed to list delivery companies', message: error.message });
  }
});

/**
 * POST /api/restaurant/delivery-company-request
 * Send a request to work with a delivery company
 */
router.post('/delivery-company-request', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const { delivery_company_id } = req.body;
    if (!delivery_company_id) {
      return res.status(400).json({ error: 'delivery_company_id is required' });
    }
    const companyId = parseInt(delivery_company_id, 10);
    const [company] = await pool.execute('SELECT id FROM delivery_companies WHERE id = ?', [companyId]);
    if (company.length === 0) {
      return res.status(404).json({ error: 'Delivery company not found' });
    }
    const [existing] = await pool.execute(
      'SELECT id, status FROM store_delivery_requests WHERE website_id = ? AND delivery_company_id = ?',
      [websiteId, companyId]
    );
    if (existing.length > 0) {
      if (existing[0].status === 'pending') {
        return res.status(400).json({ error: 'Request already pending' });
      }
      if (existing[0].status === 'approved') {
        return res.status(400).json({ error: 'Already approved for this company' });
      }
      await pool.execute(
        'UPDATE store_delivery_requests SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
        ['pending', existing[0].id]
      );
      const [updated] = await pool.execute(
        `SELECT r.id, r.website_id, r.delivery_company_id, r.status, r.created_at, c.company_name
         FROM store_delivery_requests r
         JOIN delivery_companies c ON c.id = r.delivery_company_id
         WHERE r.id = ?`,
        [existing[0].id]
      );
      return res.json({ request: updated[0] });
    }
    const [result] = await pool.execute(
      'INSERT INTO store_delivery_requests (website_id, delivery_company_id, status) VALUES (?, ?, ?)',
      [websiteId, companyId, 'pending']
    );
    const [created] = await pool.execute(
      `SELECT r.id, r.website_id, r.delivery_company_id, r.status, r.created_at, c.company_name
       FROM store_delivery_requests r
       JOIN delivery_companies c ON c.id = r.delivery_company_id
       WHERE r.id = ?`,
      [result.insertId]
    );
    res.status(201).json({ request: created[0] });
  } catch (error) {
    console.error('Error creating delivery company request:', error);
    res.status(500).json({ error: 'Failed to create request', message: error.message });
  }
});

/**
 * GET /api/restaurant/delivery-company-request
 * Get current request(s) and approved delivery company for this restaurant
 */
router.get('/delivery-company-request', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const [requests] = await pool.execute(
      `SELECT r.id, r.delivery_company_id, r.status, r.created_at, r.updated_at, c.company_name
       FROM store_delivery_requests r
       JOIN delivery_companies c ON c.id = r.delivery_company_id
       WHERE r.website_id = ?
       ORDER BY r.updated_at DESC`,
      [websiteId]
    );
    const [website] = await pool.execute(
      'SELECT delivery_company_id, delivery_mode FROM restaurant_websites WHERE id = ?',
      [websiteId]
    );
    const approvedCompanyId = website[0]?.delivery_company_id || null;
    const approvedRequest = requests.find(r => r.status === 'approved');
    const pendingRequest = requests.find(r => r.status === 'pending');
    let approvedDeliveryCompany = null;
    if (approvedCompanyId) {
      const reqWithName = requests.find(r => r.delivery_company_id === approvedCompanyId);
      approvedDeliveryCompany = {
        id: approvedCompanyId,
        company_name: reqWithName?.company_name || 'Delivery Company',
        status: 'approved'
      };
      if (!reqWithName) {
        const [co] = await pool.execute('SELECT company_name FROM delivery_companies WHERE id = ?', [approvedCompanyId]);
        if (co.length) approvedDeliveryCompany.company_name = co[0].company_name;
      }
    }
    res.json({
      requests,
      approvedDeliveryCompany,
      pendingRequest: pendingRequest || null
    });
  } catch (error) {
    console.error('Error fetching delivery company request:', error);
    res.status(500).json({ error: 'Failed to fetch request', message: error.message });
  }
});

/**
 * DELETE /api/restaurant/delivery-company-request/:id
 * Delete/cancel a delivery company request
 */
router.delete('/delivery-company-request/:id', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const requestId = parseInt(req.params.id, 10);
    
    if (isNaN(requestId)) {
      return res.status(400).json({ error: 'Invalid request ID' });
    }
    
    // Verify the request belongs to this restaurant
    const [requests] = await pool.execute(
      'SELECT id, status FROM store_delivery_requests WHERE id = ? AND website_id = ?',
      [requestId, websiteId]
    );
    
    if (requests.length === 0) {
      return res.status(404).json({ error: 'Request not found' });
    }
    
    // Delete the request
    await pool.execute(
      'DELETE FROM store_delivery_requests WHERE id = ? AND website_id = ?',
      [requestId, websiteId]
    );
    
    // If this was an approved request, also clear delivery_company_id from website
    if (requests[0].status === 'approved') {
      await pool.execute(
        'UPDATE restaurant_websites SET delivery_company_id = NULL WHERE id = ?',
        [websiteId]
      );
    }
    
    res.json({ message: 'Request deleted successfully' });
  } catch (error) {
    console.error('Error deleting delivery company request:', error);
    res.status(500).json({ error: 'Failed to delete request', message: error.message });
  }
});

/**
 * GET /api/restaurant/zones
 * Get delivery zones for the logged-in restaurant's delivery company
 */
router.get('/zones', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    
    // Get the restaurant's delivery company and restaurant names
    const [website] = await pool.execute(
      'SELECT delivery_company_id, restaurant_name, restaurant_name_ar FROM restaurant_websites WHERE id = ?',
      [websiteId]
    );
    
    if (website.length === 0) {
      return res.status(404).json({ error: 'Website not found' });
    }
    
    const deliveryCompanyId = website[0]?.delivery_company_id;
    
    if (!deliveryCompanyId) {
      return res.json({
        deliveryCompany: null,
        restaurant: null,
        zones: []
      });
    }
    
    const restaurant = {
      restaurant_name: website[0].restaurant_name || null,
      restaurant_name_ar: website[0].restaurant_name_ar || null
    };
    
    // Get delivery company name
    const [company] = await pool.execute(
      'SELECT id, company_name FROM delivery_companies WHERE id = ?',
      [deliveryCompanyId]
    );
    
    const deliveryCompany = company.length > 0 ? {
      id: company[0].id,
      company_name: company[0].company_name
    } : null;
    
    // Get zones for this restaurant's area and delivery company
    let zones = [];
    const [siteRow] = await pool.execute('SELECT area_id FROM restaurant_websites WHERE id = ?', [websiteId]);
    const areaId = siteRow[0]?.area_id;
    if (areaId != null) {
      const [zonesRows] = await pool.execute(
        'SELECT * FROM delivery_zones WHERE delivery_company_id = ? AND area_id = ? ORDER BY created_at DESC',
        [deliveryCompanyId, areaId]
      );
      zones = zonesRows;
    }
    
    res.json({
      deliveryCompany,
      restaurant,
      zones
    });
  } catch (error) {
    console.error('Error fetching restaurant zones:', error);
    res.status(500).json({ error: 'Failed to fetch zones', message: error.message });
  }
});

/**
 * POST /api/restaurant/request-driver
 * Insert a driver request record in orders_delivery (website_id, zone_id, zone_name)
 */
router.post('/request-driver', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const { zone_id: zoneId } = req.body;

    if (!zoneId) {
      return res.status(400).json({ error: 'zone_id is required' });
    }

    const zoneIdInt = parseInt(zoneId, 10);
    if (isNaN(zoneIdInt)) {
      return res.status(400).json({ error: 'Invalid zone_id' });
    }

    // Ensure zone exists, belongs to this website's delivery company, and is in the restaurant's area
    const [website] = await pool.execute(
      'SELECT delivery_company_id, area_id FROM restaurant_websites WHERE id = ?',
      [websiteId]
    );
    if (website.length === 0) {
      return res.status(404).json({ error: 'Website not found' });
    }
    const deliveryCompanyId = website[0].delivery_company_id;
    const restaurantAreaId = website[0].area_id;
    if (!deliveryCompanyId) {
      return res.status(400).json({ error: 'No delivery company assigned to this restaurant' });
    }
    if (restaurantAreaId == null) {
      return res.status(400).json({ error: 'Restaurant has no area set. Set area_id on this restaurant to request drivers.' });
    }

    const [zones] = await pool.execute(`
      SELECT 
        dz.id,
        COALESCE(dzn.name_en, dzn.name_ar) AS zone_name
      FROM delivery_zones dz
      INNER JOIN delivery_zones_names dzn ON dz.zone_name_id = dzn.id
      WHERE dz.id = ? AND dz.delivery_company_id = ? AND dz.area_id = ?
    `, [zoneIdInt, deliveryCompanyId, restaurantAreaId]);
    if (zones.length === 0) {
      return res.status(404).json({ error: 'Zone not found or not in this restaurant\'s area' });
    }

    const zoneName = zones[0].zone_name || `Zone ${zoneIdInt}`;

    await pool.execute(
      'INSERT INTO orders_delivery (website_id, zone_id, zone_name) VALUES (?, ?, ?)',
      [websiteId, zoneIdInt, zoneName]
    );

    const [rows] = await pool.execute('SELECT * FROM orders_delivery WHERE id = LAST_INSERT_ID()');
    const record = rows[0];

    // Save to Firebase Realtime Database (non-blocking; same pattern as order placement)
    (async () => {
      try {
        await saveOrderDeliveryToFirebase(record);
      } catch (firebaseError) {
        console.error('[request-driver] Firebase sync failed (record still saved in DB):', firebaseError);
      }
    })();

    res.status(201).json({ success: true, record });
  } catch (error) {
    if (error.code === 'ER_NO_SUCH_TABLE' && error.message && error.message.includes('orders_delivery')) {
      return res.status(503).json({
        error: 'orders_delivery table not found. Please run the migration: backend/db/migration_add_orders_delivery.sql'
      });
    }
    console.error('Error creating request-driver record:', error);
    res.status(500).json({ error: 'Failed to request driver', message: error.message });
  }
});

/**
 * PUT /api/restaurant/website
 * Update restaurant's own website (restaurant-scoped)
 */
router.put('/website', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const {
      restaurant_name,
      restaurant_name_ar,
      description,
      description_ar,
      address,
      address_ar,
      phone,
      email,
      primary_color,
      secondary_color,
      is_published,
      app_download_url,
      locations,
      newsletter_enabled,
      subdomain,
      custom_domain,
      order_types,
      delivery_fee,
      delivery_time_min,
      delivery_time_max,
      delivery_mode,
      delivery_company_id,
      tax_enabled,
      tax_rate,
      payment_methods,
      default_language,
      languages_enabled,
      currency_code,
      currency_symbol_position,
      latitude,
      longitude
    } = req.body;

    const updateFields = [];
    const updateValues = [];

    if (restaurant_name !== undefined) {
      updateFields.push('restaurant_name = ?');
      updateValues.push(restaurant_name);
    }
    if (restaurant_name_ar !== undefined) {
      updateFields.push('restaurant_name_ar = ?');
      updateValues.push(restaurant_name_ar || null);
    }
    if (description !== undefined) {
      updateFields.push('description = ?');
      updateValues.push(description);
    }
    if (description_ar !== undefined) {
      updateFields.push('description_ar = ?');
      updateValues.push(description_ar || null);
    }
    if (address !== undefined) {
      updateFields.push('address = ?');
      updateValues.push(address);
    }
    if (address_ar !== undefined) {
      updateFields.push('address_ar = ?');
      updateValues.push(address_ar || null);
    }
    if (phone !== undefined) {
      updateFields.push('phone = ?');
      updateValues.push(phone);
    }
    if (email !== undefined) {
      updateFields.push('email = ?');
      updateValues.push(email);
    }
    if (primary_color !== undefined) {
      updateFields.push('primary_color = ?');
      updateValues.push(primary_color);
    }
    if (secondary_color !== undefined) {
      updateFields.push('secondary_color = ?');
      updateValues.push(secondary_color);
    }
    if (is_published !== undefined) {
      updateFields.push('is_published = ?');
      updateValues.push(is_published);
    }
    if (app_download_url !== undefined) {
      updateFields.push('app_download_url = ?');
      updateValues.push(app_download_url || null);
    }
    if (locations !== undefined) {
      updateFields.push('locations = ?');
      updateValues.push(locations ? JSON.stringify(locations) : null);
    }
    if (newsletter_enabled !== undefined) {
      updateFields.push('newsletter_enabled = ?');
      updateValues.push(newsletter_enabled);
    }
    if (subdomain !== undefined) {
      updateFields.push('subdomain = ?');
      updateValues.push(subdomain || null);
    }
    if (custom_domain !== undefined) {
      updateFields.push('custom_domain = ?');
      updateValues.push(custom_domain || null);
    }
    
    // Handle order_types - can be sent as JSON string or object
    if (order_types !== undefined) {
      let orderTypesData;
      if (typeof order_types === 'string') {
        try {
          orderTypesData = JSON.parse(order_types);
        } catch (e) {
          orderTypesData = { dineInEnabled: true, pickupEnabled: true, deliveryEnabled: true };
        }
      } else {
        orderTypesData = order_types;
      }
      
      if (orderTypesData.dineInEnabled !== undefined) {
        updateFields.push('order_type_dine_in_enabled = ?');
        updateValues.push(orderTypesData.dineInEnabled);
      }
      if (orderTypesData.pickupEnabled !== undefined) {
        updateFields.push('order_type_pickup_enabled = ?');
        updateValues.push(orderTypesData.pickupEnabled);
      }
      if (orderTypesData.deliveryEnabled !== undefined) {
        updateFields.push('order_type_delivery_enabled = ?');
        updateValues.push(orderTypesData.deliveryEnabled);
      }
    }
    
    // Handle delivery_fee
    if (delivery_fee !== undefined) {
      updateFields.push('delivery_fee = ?');
      updateValues.push(parseFloat(delivery_fee) || 0);
    }

    // Handle delivery time range (minutes)
    if (delivery_time_min !== undefined) {
      updateFields.push('delivery_time_min = ?');
      updateValues.push(delivery_time_min !== null && delivery_time_min !== '' ? parseInt(delivery_time_min, 10) : null);
    }
    if (delivery_time_max !== undefined) {
      updateFields.push('delivery_time_max = ?');
      updateValues.push(delivery_time_max !== null && delivery_time_max !== '' ? parseInt(delivery_time_max, 10) : null);
    }
    if (delivery_mode !== undefined && ['fixed_fee', 'delivery_company'].includes(delivery_mode)) {
      updateFields.push('delivery_mode = ?');
      updateValues.push(delivery_mode);
    }
    if (delivery_company_id !== undefined) {
      updateFields.push('delivery_company_id = ?');
      updateValues.push(delivery_company_id === null || delivery_company_id === '' ? null : parseInt(delivery_company_id, 10));
    }

    // Handle tax settings
    if (tax_enabled !== undefined) {
      updateFields.push('tax_enabled = ?');
      updateValues.push(tax_enabled);
    }
    if (tax_rate !== undefined) {
      updateFields.push('tax_rate = ?');
      updateValues.push(parseFloat(tax_rate) || 0);
    }
    
    // Handle payment methods
    if (payment_methods !== undefined) {
      updateFields.push('payment_methods = ?');
      updateValues.push(typeof payment_methods === 'string' ? payment_methods : JSON.stringify(payment_methods));
    }

    // Handle language settings
    if (default_language !== undefined) {
      updateFields.push('default_language = ?');
      updateValues.push(default_language);
    }
    if (languages_enabled !== undefined) {
      updateFields.push('languages_enabled = ?');
      updateValues.push(typeof languages_enabled === 'string' ? languages_enabled : JSON.stringify(languages_enabled));
    }

    // Handle currency settings
    if (currency_code !== undefined) {
      updateFields.push('currency_code = ?');
      updateValues.push(currency_code);
    }
    if (currency_symbol_position !== undefined) {
      updateFields.push('currency_symbol_position = ?');
      updateValues.push(currency_symbol_position);
    }

    // Handle latitude and longitude
    if (latitude !== undefined) {
      updateFields.push('latitude = ?');
      updateValues.push(latitude !== null && latitude !== '' ? parseFloat(latitude) : null);
    }
    if (longitude !== undefined) {
      updateFields.push('longitude = ?');
      updateValues.push(longitude !== null && longitude !== '' ? parseFloat(longitude) : null);
    }

    if (updateFields.length === 0) {
      return res.status(400).json({ error: 'No fields to update' });
    }

    updateValues.push(websiteId);

    await pool.execute(
      `UPDATE restaurant_websites SET ${updateFields.join(', ')}, updated_at = CURRENT_TIMESTAMP WHERE id = ?`,
      updateValues
    );

    const [websites] = await pool.execute(
      'SELECT * FROM restaurant_websites WHERE id = ?',
      [websiteId]
    );

    res.json({ website: websites[0] });
  } catch (error) {
    console.error('Error updating restaurant website:', error);
    res.status(500).json({ error: 'Failed to update website', message: error.message });
  }
});

/**
 * GET /api/restaurant/products
 * Get restaurant's own products (restaurant-scoped)
 */
router.get('/products', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const products = await CacheService.getProducts(websiteId);
    res.json({ products });
  } catch (error) {
    console.error('Error fetching restaurant products:', error);
    res.status(500).json({ error: 'Failed to fetch products', message: error.message });
  }
});

/**
 * GET /api/restaurant/products-with-addons
 * Get restaurant's products with addons nested (for offer percent_off selected items)
 */
router.get('/products-with-addons', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const [products] = await pool.execute(
      'SELECT * FROM products WHERE website_id = ? ORDER BY category, name',
      [websiteId]
    );
    const [addonsRows] = await pool.execute(
      'SELECT pa.* FROM product_addons pa INNER JOIN products p ON p.id = pa.product_id WHERE p.website_id = ? ORDER BY pa.product_id, pa.display_order, pa.id',
      [websiteId]
    );
    const addonsByProduct = {};
    for (const a of addonsRows) {
      const pid = a.product_id;
      if (!addonsByProduct[pid]) addonsByProduct[pid] = [];
      addonsByProduct[pid].push(a);
    }
    const productsWithAddons = products.map(p => ({
      ...p,
      addons: addonsByProduct[p.id] || []
    }));
    res.json({ products: productsWithAddons });
  } catch (error) {
    console.error('Error fetching products with addons:', error);
    res.status(500).json({ error: 'Failed to fetch products', message: error.message });
  }
});

/**
 * POST /api/restaurant/products
 * Create product for restaurant's own website (restaurant-scoped)
 */
router.post('/products', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const { name, name_ar, description, description_ar, price, category, category_ar, is_available } = req.body;

    if (!name || !price) {
      return res.status(400).json({ error: 'Name and price are required' });
    }

    const [result] = await pool.execute(
      'INSERT INTO products (website_id, name, name_ar, description, description_ar, price, category, category_ar, is_available) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)',
      [websiteId, name, name_ar || null, description || null, description_ar || null, price, category || null, category_ar || null, is_available !== false]
    );

    const [products] = await pool.execute(
      'SELECT * FROM products WHERE id = ?',
      [result.insertId]
    );

    res.status(201).json({ product: products[0] });
  } catch (error) {
    console.error('Error creating product:', error);
    res.status(500).json({ error: 'Failed to create product', message: error.message });
  }
});

/**
 * PUT /api/restaurant/products/:id
 * Update product for restaurant's own website (restaurant-scoped)
 */
router.put('/products/:id', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const { id } = req.params;
    const { name, name_ar, description, description_ar, price, category, category_ar, is_available, addon_required, addon_required_min } = req.body;

    // Verify product belongs to restaurant's website
    const [products] = await pool.execute(
      'SELECT * FROM products WHERE id = ? AND website_id = ?',
      [id, websiteId]
    );

    if (products.length === 0) {
      return res.status(404).json({ error: 'Product not found or access denied' });
    }

    const updateFields = [];
    const updateValues = [];

    if (name !== undefined) {
      updateFields.push('name = ?');
      updateValues.push(name);
    }
    if (name_ar !== undefined) {
      updateFields.push('name_ar = ?');
      updateValues.push(name_ar || null);
    }
    if (description !== undefined) {
      updateFields.push('description = ?');
      updateValues.push(description);
    }
    if (description_ar !== undefined) {
      updateFields.push('description_ar = ?');
      updateValues.push(description_ar || null);
    }
    if (price !== undefined) {
      updateFields.push('price = ?');
      updateValues.push(price);
    }
    if (category !== undefined) {
      updateFields.push('category = ?');
      updateValues.push(category);
    }
    if (category_ar !== undefined) {
      updateFields.push('category_ar = ?');
      updateValues.push(category_ar || null);
    }
    if (is_available !== undefined) {
      updateFields.push('is_available = ?');
      updateValues.push(is_available);
    }
    if (addon_required !== undefined) {
      updateFields.push('addon_required = ?');
      updateValues.push(!!addon_required);
    }
    if (addon_required_min !== undefined) {
      const v = addon_required_min === '' || addon_required_min === null ? null : parseInt(addon_required_min, 10);
      updateFields.push('addon_required_min = ?');
      updateValues.push((v !== null && !isNaN(v)) ? v : null);
    }

    if (updateFields.length === 0) {
      return res.status(400).json({ error: 'No fields to update' });
    }

    updateValues.push(id, websiteId);

    await pool.execute(
      `UPDATE products SET ${updateFields.join(', ')}, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND website_id = ?`,
      updateValues
    );

    const [updatedProducts] = await pool.execute(
      'SELECT * FROM products WHERE id = ?',
      [id]
    );

    res.json({ product: updatedProducts[0] });
  } catch (error) {
    console.error('Error updating product:', error);
    res.status(500).json({ error: 'Failed to update product', message: error.message });
  }
});

/**
 * DELETE /api/restaurant/products/:id
 * Delete product for restaurant's own website (restaurant-scoped)
 */
router.delete('/products/:id', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const { id } = req.params;

    // Verify product belongs to restaurant's website
    const [products] = await pool.execute(
      'SELECT * FROM products WHERE id = ? AND website_id = ?',
      [id, websiteId]
    );

    if (products.length === 0) {
      return res.status(404).json({ error: 'Product not found or access denied' });
    }

    await pool.execute('DELETE FROM products WHERE id = ? AND website_id = ?', [id, websiteId]);

    res.json({ message: 'Product deleted successfully' });
  } catch (error) {
    console.error('Error deleting product:', error);
    res.status(500).json({ error: 'Failed to delete product', message: error.message });
  }
});

// ----- Product Add-ons -----
async function ensureProductOwnership(websiteId, productId) {
  const [rows] = await pool.execute(
    'SELECT id FROM products WHERE id = ? AND website_id = ?',
    [productId, websiteId]
  );
  return rows.length > 0;
}

/**
 * GET /api/restaurant/products/:id/addons
 */
router.get('/products/:id/addons', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const productId = req.params.id;
    if (!(await ensureProductOwnership(websiteId, productId))) {
      return res.status(404).json({ error: 'Product not found or access denied' });
    }
    const [addons] = await pool.execute(
      'SELECT * FROM product_addons WHERE product_id = ? ORDER BY display_order, id',
      [productId]
    );
    const [product] = await pool.execute(
      'SELECT addon_required, addon_required_min FROM products WHERE id = ?',
      [productId]
    );
    res.json({
      addons,
      addon_required: product[0]?.addon_required ?? false,
      addon_required_min: product[0]?.addon_required_min ?? null
    });
  } catch (error) {
    console.error('Error fetching addons:', error);
    res.status(500).json({ error: 'Failed to fetch addons', message: error.message });
  }
});

/**
 * POST /api/restaurant/products/:id/addons
 */
router.post('/products/:id/addons', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const productId = req.params.id;
    if (!(await ensureProductOwnership(websiteId, productId))) {
      return res.status(404).json({ error: 'Product not found or access denied' });
    }
    const { name, name_ar, description, description_ar, price, is_required } = req.body;
    if (!name || !name.trim()) {
      return res.status(400).json({ error: 'Add-on name is required' });
    }
    const priceVal = parseFloat(price);
    if (isNaN(priceVal) || priceVal < 0) {
      return res.status(400).json({ error: 'Valid price is required' });
    }
    const required = !!is_required;
    const [result] = await pool.execute(
      'INSERT INTO product_addons (product_id, name, name_ar, description, description_ar, price, is_required) VALUES (?, ?, ?, ?, ?, ?, ?)',
      [productId, name.trim(), name_ar?.trim() || null, description?.trim() || null, description_ar?.trim() || null, priceVal, required]
    );
    const [addons] = await pool.execute('SELECT * FROM product_addons WHERE id = ?', [result.insertId]);
    res.status(201).json({ addon: addons[0] });
  } catch (error) {
    console.error('Error creating addon:', error);
    res.status(500).json({ error: 'Failed to create addon', message: error.message });
  }
});

/**
 * PUT /api/restaurant/products/:id/addons/:addonId
 */
router.put('/products/:id/addons/:addonId', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const { id: productId, addonId } = req.params;
    if (!(await ensureProductOwnership(websiteId, productId))) {
      return res.status(404).json({ error: 'Product not found or access denied' });
    }
    const { name, name_ar, description, description_ar, price, is_required } = req.body;
    const updateFields = [];
    const updateValues = [];
    if (name !== undefined) {
      updateFields.push('name = ?');
      updateValues.push(name.trim());
    }
    if (name_ar !== undefined) {
      updateFields.push('name_ar = ?');
      updateValues.push(name_ar?.trim() || null);
    }
    if (description !== undefined) {
      updateFields.push('description = ?');
      updateValues.push(description?.trim() || null);
    }
    if (description_ar !== undefined) {
      updateFields.push('description_ar = ?');
      updateValues.push(description_ar?.trim() || null);
    }
    if (price !== undefined) {
      const priceVal = parseFloat(price);
      if (!isNaN(priceVal) && priceVal >= 0) {
        updateFields.push('price = ?');
        updateValues.push(priceVal);
      }
    }
    if (is_required !== undefined) {
      updateFields.push('is_required = ?');
      updateValues.push(!!is_required);
    }
    if (updateFields.length === 0) {
      return res.status(400).json({ error: 'No fields to update' });
    }
    updateValues.push(addonId, productId);
    await pool.execute(
      `UPDATE product_addons SET ${updateFields.join(', ')}, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND product_id = ?`,
      updateValues
    );
    const [addons] = await pool.execute('SELECT * FROM product_addons WHERE id = ? AND product_id = ?', [addonId, productId]);
    if (addons.length === 0) {
      return res.status(404).json({ error: 'Add-on not found' });
    }
    res.json({ addon: addons[0] });
  } catch (error) {
    console.error('Error updating addon:', error);
    res.status(500).json({ error: 'Failed to update addon', message: error.message });
  }
});

/**
 * DELETE /api/restaurant/products/:id/addons/:addonId
 */
router.delete('/products/:id/addons/:addonId', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const { id: productId, addonId } = req.params;
    if (!(await ensureProductOwnership(websiteId, productId))) {
      return res.status(404).json({ error: 'Product not found or access denied' });
    }
    const [addon] = await pool.execute('SELECT image_path FROM product_addons WHERE id = ? AND product_id = ?', [addonId, productId]);
    if (addon.length > 0 && addon[0].image_path && fs.existsSync(addon[0].image_path)) {
      try { fs.unlinkSync(addon[0].image_path); } catch (e) { /* ignore */ }
    }
    await pool.execute('DELETE FROM product_addons WHERE id = ? AND product_id = ?', [addonId, productId]);
    res.json({ message: 'Add-on deleted successfully' });
  } catch (error) {
    console.error('Error deleting addon:', error);
    res.status(500).json({ error: 'Failed to delete addon', message: error.message });
  }
});

/**
 * POST /api/restaurant/products/:id/addons/:addonId/image
 */
router.post('/products/:id/addons/:addonId/image', verifyAdminToken, uploadAddonImage.single('image'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'No image uploaded' });
    }
    const websiteId = req.websiteId;
    const { id: productId, addonId } = req.params;
    if (!(await ensureProductOwnership(websiteId, productId))) {
      fs.unlinkSync(req.file.path);
      return res.status(404).json({ error: 'Product not found or access denied' });
    }
    const [addons] = await pool.execute('SELECT * FROM product_addons WHERE id = ? AND product_id = ?', [addonId, productId]);
    if (addons.length === 0) {
      fs.unlinkSync(req.file.path);
      return res.status(404).json({ error: 'Add-on not found' });
    }
    if (addons[0].image_path && fs.existsSync(addons[0].image_path)) {
      try { fs.unlinkSync(addons[0].image_path); } catch (e) { /* ignore */ }
    }
    const apiBaseUrl = process.env.API_BASE_URL || `http://localhost:${process.env.PORT || 3000}`;
    const imageUrl = `${apiBaseUrl}/uploads/${req.file.filename}`;
    await pool.execute(
      'UPDATE product_addons SET image_url = ?, image_path = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND product_id = ?',
      [imageUrl, req.file.path, addonId, productId]
    );
    const [updated] = await pool.execute('SELECT * FROM product_addons WHERE id = ?', [addonId]);
    res.json({ addon: updated[0] });
  } catch (error) {
    if (req.file && fs.existsSync(req.file.path)) fs.unlinkSync(req.file.path);
    console.error('Error uploading addon image:', error);
    res.status(500).json({ error: 'Failed to upload addon image', message: error.message });
  }
});

/**
 * PUT /api/restaurant/products/:id/addon-settings
 * Update only addon_required and addon_required_min for a product
 */
router.put('/products/:id/addon-settings', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const { id } = req.params;
    const { addon_required, addon_required_min } = req.body;
    if (!(await ensureProductOwnership(websiteId, id))) {
      return res.status(404).json({ error: 'Product not found or access denied' });
    }
    const minVal = addon_required_min === '' || addon_required_min === null || addon_required_min === undefined
      ? null
      : parseInt(addon_required_min, 10);
    const minFinal = (minVal !== null && !isNaN(minVal)) ? minVal : null;
    await pool.execute(
      'UPDATE products SET addon_required = ?, addon_required_min = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND website_id = ?',
      [!!addon_required, minFinal, id, websiteId]
    );
    const [products] = await pool.execute('SELECT * FROM products WHERE id = ?', [id]);
    res.json({ product: products[0] });
  } catch (error) {
    console.error('Error updating addon settings:', error);
    res.status(500).json({ error: 'Failed to update addon settings', message: error.message });
  }
});

/**
 * GET /api/restaurant/notifications/settings
 * Get notification settings for restaurant
 */
router.get('/notifications/settings', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    
    const [websites] = await pool.execute(
      `SELECT 
        notifications_enabled,
        notification_email_enabled,
        notification_sms_enabled,
        notification_push_enabled,
        notification_whatsapp_enabled,
        notification_email,
        email as restaurant_email
      FROM restaurant_websites 
      WHERE id = ?`,
      [websiteId]
    );
    
    if (websites.length === 0) {
      return res.status(404).json({ error: 'Website not found' });
    }
    
    const website = websites[0];
    res.json({
      notificationsEnabled: website.notifications_enabled || false,
      emailEnabled: website.notification_email_enabled || false,
      smsEnabled: website.notification_sms_enabled || false,
      pushEnabled: website.notification_push_enabled || false,
      whatsappEnabled: website.notification_whatsapp_enabled || false,
      notificationEmail: website.notification_email || website.restaurant_email || null,
    });
  } catch (error) {
    console.error('Error fetching notification settings:', error);
    res.status(500).json({ error: 'Failed to fetch notification settings', message: error.message });
  }
});

/**
 * PUT /api/restaurant/notifications/settings
 * Update notification settings for restaurant
 */
router.put('/notifications/settings', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const {
      notificationsEnabled,
      emailEnabled,
      smsEnabled,
      pushEnabled,
      whatsappEnabled,
      notificationEmail
    } = req.body;

    const updateFields = [];
    const updateValues = [];

    if (notificationsEnabled !== undefined) {
      updateFields.push('notifications_enabled = ?');
      updateValues.push(notificationsEnabled);
    }
    if (emailEnabled !== undefined) {
      updateFields.push('notification_email_enabled = ?');
      updateValues.push(emailEnabled);
    }
    if (smsEnabled !== undefined) {
      updateFields.push('notification_sms_enabled = ?');
      updateValues.push(smsEnabled);
    }
    if (pushEnabled !== undefined) {
      updateFields.push('notification_push_enabled = ?');
      updateValues.push(pushEnabled);
    }
    if (whatsappEnabled !== undefined) {
      updateFields.push('notification_whatsapp_enabled = ?');
      updateValues.push(whatsappEnabled);
    }
    if (notificationEmail !== undefined) {
      updateFields.push('notification_email = ?');
      updateValues.push(notificationEmail || null);
    }

    if (updateFields.length === 0) {
      return res.status(400).json({ error: 'No fields to update' });
    }

    updateValues.push(websiteId);

    await pool.execute(
      `UPDATE restaurant_websites SET ${updateFields.join(', ')}, updated_at = CURRENT_TIMESTAMP WHERE id = ?`,
      updateValues
    );

    // Return updated settings
    const [websites] = await pool.execute(
      `SELECT 
        notifications_enabled,
        notification_email_enabled,
        notification_sms_enabled,
        notification_push_enabled,
        notification_whatsapp_enabled,
        notification_email,
        email as restaurant_email
      FROM restaurant_websites 
      WHERE id = ?`,
      [websiteId]
    );

    const website = websites[0];
    res.json({
      notificationsEnabled: website.notifications_enabled || false,
      emailEnabled: website.notification_email_enabled || false,
      smsEnabled: website.notification_sms_enabled || false,
      pushEnabled: website.notification_push_enabled || false,
      whatsappEnabled: website.notification_whatsapp_enabled || false,
      notificationEmail: website.notification_email || website.restaurant_email || null,
    });
  } catch (error) {
    console.error('Error updating notification settings:', error);
    res.status(500).json({ error: 'Failed to update notification settings', message: error.message });
  }
});

/**
 * GET /api/restaurant/coupons
 * Get all coupons for the restaurant
 */
router.get('/coupons', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    
    const [coupons] = await pool.execute(
      'SELECT * FROM coupons WHERE website_id = ? ORDER BY created_at DESC',
      [websiteId]
    );
    
    res.json({ coupons });
  } catch (error) {
    console.error('Error fetching coupons:', error);
    res.status(500).json({ error: 'Failed to fetch coupons', message: error.message });
  }
});

/**
 * POST /api/restaurant/coupons
 * Create a new coupon
 */
router.post('/coupons', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const {
      code,
      description,
      discount_type,
      discount_value,
      min_order_amount,
      max_discount_amount,
      valid_from,
      valid_until,
      usage_limit,
      is_active
    } = req.body;

    // Validate required fields
    if (!code || !discount_type || !discount_value || !valid_from || !valid_until) {
      return res.status(400).json({ error: 'Missing required fields' });
    }

    // Validate dates
    const fromDate = new Date(valid_from);
    const untilDate = new Date(valid_until);
    if (untilDate < fromDate) {
      return res.status(400).json({ error: 'Valid until date must be after valid from date' });
    }

    // Check if code already exists for this website
    const [existing] = await pool.execute(
      'SELECT id FROM coupons WHERE website_id = ? AND code = ?',
      [websiteId, code.toUpperCase()]
    );

    if (existing.length > 0) {
      return res.status(400).json({ error: 'Coupon code already exists' });
    }

    // Insert coupon
    const [result] = await pool.execute(
      `INSERT INTO coupons (
        website_id, code, description, discount_type, discount_value,
        min_order_amount, max_discount_amount, valid_from, valid_until,
        usage_limit, is_active
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        websiteId,
        code.toUpperCase(),
        description || null,
        discount_type,
        parseFloat(discount_value),
        parseFloat(min_order_amount) || 0,
        max_discount_amount ? parseFloat(max_discount_amount) : null,
        valid_from,
        valid_until,
        usage_limit ? parseInt(usage_limit) : null,
        is_active !== undefined ? is_active : true
      ]
    );

    const [coupons] = await pool.execute(
      'SELECT * FROM coupons WHERE id = ?',
      [result.insertId]
    );

    res.status(201).json({ coupon: coupons[0] });
  } catch (error) {
    console.error('Error creating coupon:', error);
    res.status(500).json({ error: 'Failed to create coupon', message: error.message });
  }
});

/**
 * PUT /api/restaurant/coupons/:id
 * Update a coupon
 */
router.put('/coupons/:id', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const couponId = parseInt(req.params.id);
    const {
      code,
      description,
      discount_type,
      discount_value,
      min_order_amount,
      max_discount_amount,
      valid_from,
      valid_until,
      usage_limit,
      is_active
    } = req.body;

    // Verify coupon belongs to this website
    const [existing] = await pool.execute(
      'SELECT id FROM coupons WHERE id = ? AND website_id = ?',
      [couponId, websiteId]
    );

    if (existing.length === 0) {
      return res.status(404).json({ error: 'Coupon not found' });
    }

    // If code is being changed, check if new code already exists
    if (code) {
      const [codeCheck] = await pool.execute(
        'SELECT id FROM coupons WHERE website_id = ? AND code = ? AND id != ?',
        [websiteId, code.toUpperCase(), couponId]
      );

      if (codeCheck.length > 0) {
        return res.status(400).json({ error: 'Coupon code already exists' });
      }
    }

    // Build update query dynamically
    const updateFields = [];
    const updateValues = [];

    if (code !== undefined) {
      updateFields.push('code = ?');
      updateValues.push(code.toUpperCase());
    }
    if (description !== undefined) {
      updateFields.push('description = ?');
      updateValues.push(description);
    }
    if (discount_type !== undefined) {
      updateFields.push('discount_type = ?');
      updateValues.push(discount_type);
    }
    if (discount_value !== undefined) {
      updateFields.push('discount_value = ?');
      updateValues.push(parseFloat(discount_value));
    }
    if (min_order_amount !== undefined) {
      updateFields.push('min_order_amount = ?');
      updateValues.push(parseFloat(min_order_amount));
    }
    if (max_discount_amount !== undefined) {
      updateFields.push('max_discount_amount = ?');
      updateValues.push(max_discount_amount ? parseFloat(max_discount_amount) : null);
    }
    if (valid_from !== undefined) {
      updateFields.push('valid_from = ?');
      updateValues.push(valid_from);
    }
    if (valid_until !== undefined) {
      updateFields.push('valid_until = ?');
      updateValues.push(valid_until);
    }
    if (usage_limit !== undefined) {
      updateFields.push('usage_limit = ?');
      updateValues.push(usage_limit ? parseInt(usage_limit) : null);
    }
    if (is_active !== undefined) {
      updateFields.push('is_active = ?');
      updateValues.push(is_active);
    }

    if (updateFields.length === 0) {
      return res.status(400).json({ error: 'No fields to update' });
    }

    // Validate dates if both are being updated
    if (valid_from && valid_until) {
      const fromDate = new Date(valid_from);
      const untilDate = new Date(valid_until);
      if (untilDate < fromDate) {
        return res.status(400).json({ error: 'Valid until date must be after valid from date' });
      }
    }

    updateValues.push(couponId, websiteId);

    await pool.execute(
      `UPDATE coupons SET ${updateFields.join(', ')}, updated_at = CURRENT_TIMESTAMP 
       WHERE id = ? AND website_id = ?`,
      updateValues
    );

    const [coupons] = await pool.execute(
      'SELECT * FROM coupons WHERE id = ?',
      [couponId]
    );

    res.json({ coupon: coupons[0] });
  } catch (error) {
    console.error('Error updating coupon:', error);
    res.status(500).json({ error: 'Failed to update coupon', message: error.message });
  }
});

/**
 * DELETE /api/restaurant/coupons/:id
 * Delete a coupon
 */
router.delete('/coupons/:id', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const couponId = parseInt(req.params.id);

    // Verify coupon belongs to this website
    const [existing] = await pool.execute(
      'SELECT id FROM coupons WHERE id = ? AND website_id = ?',
      [couponId, websiteId]
    );

    if (existing.length === 0) {
      return res.status(404).json({ error: 'Coupon not found' });
    }

    await pool.execute(
      'DELETE FROM coupons WHERE id = ? AND website_id = ?',
      [couponId, websiteId]
    );

    res.json({ message: 'Coupon deleted successfully' });
  } catch (error) {
    console.error('Error deleting coupon:', error);
    res.status(500).json({ error: 'Failed to delete coupon', message: error.message });
  }
});

/**
 * GET /api/restaurant/offers
 * Get all offers for the restaurant
 */
router.get('/offers', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const [offers] = await pool.execute(
      'SELECT * FROM offers WHERE website_id = ? ORDER BY display_order ASC, created_at DESC',
      [websiteId]
    );
    res.json({ offers });
  } catch (error) {
    console.error('Error fetching offers:', error);
    res.status(500).json({ error: 'Failed to fetch offers', message: error.message });
  }
});

/**
 * POST /api/restaurant/offers
 * Create a new offer
 */
router.post('/offers', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const {
      offer_type,
      title,
      description,
      value,
      min_order_value,
      is_active,
      valid_from,
      valid_until,
      display_order,
      offer_scope,
      selected_product_ids,
      selected_addon_ids
    } = req.body;

    if (!offer_type || !title || !valid_from || !valid_until) {
      return res.status(400).json({ error: 'Missing required fields: offer_type, title, valid_from, valid_until' });
    }

    const validTypes = ['free_delivery', 'free_delivery_over_x_jod', 'percent_off', 'minimum_order_value'];
    if (!validTypes.includes(offer_type)) {
      return res.status(400).json({ error: 'Invalid offer_type' });
    }

    const scope = offer_scope === 'selected_items' ? 'selected_items' : 'all_items';
    const productIdsJson = Array.isArray(selected_product_ids) ? JSON.stringify(selected_product_ids) : (selected_product_ids || null);
    const addonIdsJson = Array.isArray(selected_addon_ids) ? JSON.stringify(selected_addon_ids) : (selected_addon_ids || null);

    const fromDate = new Date(valid_from);
    const untilDate = new Date(valid_until);
    if (untilDate < fromDate) {
      return res.status(400).json({ error: 'Valid until date must be after valid from date' });
    }

    const [result] = await pool.execute(
      `INSERT INTO offers (
        website_id, offer_type, title, description, value, min_order_value,
        is_active, valid_from, valid_until, display_order,
        offer_scope, selected_product_ids, selected_addon_ids
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        websiteId,
        offer_type,
        title,
        description || null,
        value != null ? parseFloat(value) : null,
        min_order_value != null ? parseFloat(min_order_value) : null,
        is_active !== undefined ? is_active : true,
        valid_from,
        valid_until,
        display_order != null ? parseInt(display_order) : 0,
        scope,
        productIdsJson,
        addonIdsJson
      ]
    );

    const [rows] = await pool.execute('SELECT * FROM offers WHERE id = ?', [result.insertId]);
    res.status(201).json({ offer: rows[0] });
  } catch (error) {
    console.error('Error creating offer:', error);
    res.status(500).json({ error: 'Failed to create offer', message: error.message });
  }
});

/**
 * PUT /api/restaurant/offers/:id
 * Update an offer
 */
router.put('/offers/:id', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const offerId = parseInt(req.params.id);
    const {
      offer_type,
      title,
      description,
      value,
      min_order_value,
      is_active,
      valid_from,
      valid_until,
      display_order,
      offer_scope,
      selected_product_ids,
      selected_addon_ids
    } = req.body;

    const [existing] = await pool.execute(
      'SELECT id FROM offers WHERE id = ? AND website_id = ?',
      [offerId, websiteId]
    );
    if (existing.length === 0) {
      return res.status(404).json({ error: 'Offer not found' });
    }

    if (valid_from && valid_until) {
      const fromDate = new Date(valid_from);
      const untilDate = new Date(valid_until);
      if (untilDate < fromDate) {
        return res.status(400).json({ error: 'Valid until date must be after valid from date' });
      }
    }

    const updates = [];
    const values = [];
    if (offer_type !== undefined) { updates.push('offer_type = ?'); values.push(offer_type); }
    if (title !== undefined) { updates.push('title = ?'); values.push(title); }
    if (description !== undefined) { updates.push('description = ?'); values.push(description); }
    if (value !== undefined) { updates.push('value = ?'); values.push(value != null ? parseFloat(value) : null); }
    if (min_order_value !== undefined) { updates.push('min_order_value = ?'); values.push(min_order_value != null ? parseFloat(min_order_value) : null); }
    if (is_active !== undefined) { updates.push('is_active = ?'); values.push(is_active); }
    if (valid_from !== undefined) { updates.push('valid_from = ?'); values.push(valid_from); }
    if (valid_until !== undefined) { updates.push('valid_until = ?'); values.push(valid_until); }
    if (display_order !== undefined) { updates.push('display_order = ?'); values.push(parseInt(display_order)); }
    if (offer_scope !== undefined) { updates.push('offer_scope = ?'); values.push(offer_scope === 'selected_items' ? 'selected_items' : 'all_items'); }
    if (selected_product_ids !== undefined) { updates.push('selected_product_ids = ?'); values.push(Array.isArray(selected_product_ids) ? JSON.stringify(selected_product_ids) : selected_product_ids); }
    if (selected_addon_ids !== undefined) { updates.push('selected_addon_ids = ?'); values.push(Array.isArray(selected_addon_ids) ? JSON.stringify(selected_addon_ids) : selected_addon_ids); }

    if (updates.length === 0) {
      return res.status(400).json({ error: 'No fields to update' });
    }
    values.push(offerId, websiteId);
    await pool.execute(
      `UPDATE offers SET ${updates.join(', ')}, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND website_id = ?`,
      values
    );
    const [rows] = await pool.execute('SELECT * FROM offers WHERE id = ?', [offerId]);
    res.json({ offer: rows[0] });
  } catch (error) {
    console.error('Error updating offer:', error);
    res.status(500).json({ error: 'Failed to update offer', message: error.message });
  }
});

/**
 * DELETE /api/restaurant/offers/:id
 * Delete an offer
 */
router.delete('/offers/:id', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const offerId = parseInt(req.params.id);
    const [existing] = await pool.execute(
      'SELECT id FROM offers WHERE id = ? AND website_id = ?',
      [offerId, websiteId]
    );
    if (existing.length === 0) {
      return res.status(404).json({ error: 'Offer not found' });
    }
    await pool.execute('DELETE FROM offers WHERE id = ? AND website_id = ?', [offerId, websiteId]);
    res.json({ message: 'Offer deleted successfully' });
  } catch (error) {
    console.error('Error deleting offer:', error);
    res.status(500).json({ error: 'Failed to delete offer', message: error.message });
  }
});

/**
 * GET /api/restaurant/business-hours
 * Get business hours for all 7 days (0=Sunday .. 6=Saturday)
 */
router.get('/business-hours', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const [rows] = await pool.execute(
      'SELECT id, website_id, day_of_week, open_time, close_time, is_closed FROM business_hours WHERE website_id = ? ORDER BY day_of_week',
      [websiteId]
    );
    // Ensure we have 7 rows (one per day); if missing days, return defaults
    const dayMap = {};
    rows.forEach(r => { dayMap[r.day_of_week] = r; });
    const hours = [];
    for (let d = 0; d <= 6; d++) {
      if (dayMap[d]) {
        hours.push({
          id: dayMap[d].id,
          day_of_week: dayMap[d].day_of_week,
          open_time: dayMap[d].open_time ? String(dayMap[d].open_time).slice(0, 5) : null,
          close_time: dayMap[d].close_time ? String(dayMap[d].close_time).slice(0, 5) : null,
          is_closed: Boolean(dayMap[d].is_closed)
        });
      } else {
        hours.push({
          id: null,
          day_of_week: d,
          open_time: null,
          close_time: null,
          is_closed: true
        });
      }
    }
    res.json({ business_hours: hours });
  } catch (error) {
    console.error('Error fetching business hours:', error);
    res.status(500).json({ error: 'Failed to fetch business hours', message: error.message });
  }
});

/**
 * PUT /api/restaurant/business-hours
 * Create/update business hours for all 7 days. Body: { hours: [ { day_of_week, open_time, close_time, is_closed }, ... ] }
 */
router.put('/business-hours', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const { hours } = req.body;
    if (!Array.isArray(hours) || hours.length !== 7) {
      return res.status(400).json({ error: 'hours must be an array of 7 items (day_of_week 0-6)' });
    }
    for (let d = 0; d <= 6; d++) {
      const item = hours.find(h => Number(h.day_of_week) === d);
      if (!item) continue;
      const day_of_week = d;
      const is_closed = Boolean(item.is_closed);
      const open_time = is_closed || item.open_time == null || item.open_time === '' ? null : item.open_time;
      const close_time = is_closed || item.close_time == null || item.close_time === '' ? null : item.close_time;
      const [existing] = await pool.execute(
        'SELECT id FROM business_hours WHERE website_id = ? AND day_of_week = ?',
        [websiteId, day_of_week]
      );
      if (existing.length > 0) {
        await pool.execute(
          'UPDATE business_hours SET open_time = ?, close_time = ?, is_closed = ?, updated_at = CURRENT_TIMESTAMP WHERE website_id = ? AND day_of_week = ?',
          [open_time, close_time, is_closed, websiteId, day_of_week]
        );
      } else {
        await pool.execute(
          'INSERT INTO business_hours (website_id, day_of_week, open_time, close_time, is_closed) VALUES (?, ?, ?, ?, ?)',
          [websiteId, day_of_week, open_time, close_time, is_closed]
        );
      }
    }
    const [updated] = await pool.execute(
      'SELECT id, website_id, day_of_week, open_time, close_time, is_closed FROM business_hours WHERE website_id = ? ORDER BY day_of_week',
      [websiteId]
    );
    const business_hours = updated.map(r => ({
      id: r.id,
      day_of_week: r.day_of_week,
      open_time: r.open_time ? String(r.open_time).slice(0, 5) : null,
      close_time: r.close_time ? String(r.close_time).slice(0, 5) : null,
      is_closed: Boolean(r.is_closed)
    }));
    res.json({ business_hours });
  } catch (error) {
    console.error('Error updating business hours:', error);
    res.status(500).json({ error: 'Failed to update business hours', message: error.message });
  }
});

/**
 * GET /api/restaurant/regions
 * Get all regions (for branch selection)
 */
router.get('/regions', verifyAdminToken, async (req, res) => {
  try {
    const [regions] = await pool.execute(`
      SELECT 
        r.id,
        r.name,
        r.name_ar,
        r.city_id,
        c.name as city_name,
        c.name_ar as city_name_ar
      FROM regions r
      INNER JOIN cities c ON r.city_id = c.id
      ORDER BY c.name, r.name
    `);
    res.json({ regions });
  } catch (error) {
    console.error('Error fetching regions:', error);
    res.status(500).json({ error: 'Failed to fetch regions', message: error.message });
  }
});

/**
 * GET /api/restaurant/branches
 * Get all branches for the logged-in restaurant
 */
router.get('/branches', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const [branches] = await pool.execute(`
      SELECT 
        b.*,
        r.name as region_name,
        r.name_ar as region_name_ar,
        c.name as city_name,
        c.name_ar as city_name_ar
      FROM restaurant_branches b
      INNER JOIN regions r ON b.region_id = r.id
      INNER JOIN cities c ON r.city_id = c.id
      WHERE b.website_id = ?
      ORDER BY b.branch_number ASC
    `, [websiteId]);
    res.json({ branches });
  } catch (error) {
    console.error('Error fetching branches:', error);
    res.status(500).json({ error: 'Failed to fetch branches', message: error.message });
  }
});

/**
 * POST /api/restaurant/branches
 * Create a new branch for the logged-in restaurant
 */
router.post('/branches', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const { region_id, branch_number, name, name_ar, address, phone, status } = req.body;

    if (!region_id || !branch_number) {
      return res.status(400).json({ error: 'region_id and branch_number are required' });
    }

    // Verify region exists
    const [regions] = await pool.execute('SELECT id FROM regions WHERE id = ?', [region_id]);
    if (regions.length === 0) {
      return res.status(404).json({ error: 'Region not found' });
    }

    // Check if branch number already exists for this website
    const [existing] = await pool.execute(
      'SELECT id FROM restaurant_branches WHERE website_id = ? AND branch_number = ?',
      [websiteId, branch_number]
    );
    if (existing.length > 0) {
      return res.status(400).json({ error: 'Branch number already exists for this restaurant' });
    }

    const [result] = await pool.execute(
      `INSERT INTO restaurant_branches 
       (website_id, region_id, branch_number, name, name_ar, address, phone, status)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        websiteId,
        region_id,
        branch_number,
        name || null,
        name_ar || null,
        address || null,
        phone || null,
        status || 'active'
      ]
    );

    // Fetch the created branch with region info
    const [branches] = await pool.execute(`
      SELECT 
        b.*,
        r.name as region_name,
        r.name_ar as region_name_ar,
        c.name as city_name,
        c.name_ar as city_name_ar
      FROM restaurant_branches b
      INNER JOIN regions r ON b.region_id = r.id
      INNER JOIN cities c ON r.city_id = c.id
      WHERE b.id = ?
    `, [result.insertId]);

    res.status(201).json({ branch: branches[0] });
  } catch (error) {
    console.error('Error creating branch:', error);
    if (error.code === 'ER_DUP_ENTRY') {
      return res.status(400).json({ error: 'Branch number already exists for this restaurant' });
    }
    res.status(500).json({ error: 'Failed to create branch', message: error.message });
  }
});

/**
 * PUT /api/restaurant/branches/:id
 * Update a branch (must belong to the logged-in restaurant)
 */
router.put('/branches/:id', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const { id } = req.params;
    const { region_id, branch_number, name, name_ar, address, phone, status } = req.body;

    // Verify branch belongs to this restaurant
    const [existing] = await pool.execute(
      'SELECT * FROM restaurant_branches WHERE id = ? AND website_id = ?',
      [id, websiteId]
    );
    if (existing.length === 0) {
      return res.status(404).json({ error: 'Branch not found' });
    }

    // If branch_number is being changed, check for duplicates
    if (branch_number !== undefined && branch_number !== existing[0].branch_number) {
      const [duplicate] = await pool.execute(
        'SELECT id FROM restaurant_branches WHERE website_id = ? AND branch_number = ? AND id != ?',
        [websiteId, branch_number, id]
      );
      if (duplicate.length > 0) {
        return res.status(400).json({ error: 'Branch number already exists for this restaurant' });
      }
    }

    // If region_id is being changed, verify it exists
    if (region_id !== undefined) {
      const [regions] = await pool.execute('SELECT id FROM regions WHERE id = ?', [region_id]);
      if (regions.length === 0) {
        return res.status(404).json({ error: 'Region not found' });
      }
    }

    await pool.execute(
      `UPDATE restaurant_branches SET
       region_id = ?,
       branch_number = ?,
       name = ?,
       name_ar = ?,
       address = ?,
       phone = ?,
       status = ?,
       updated_at = CURRENT_TIMESTAMP
       WHERE id = ? AND website_id = ?`,
      [
        region_id !== undefined ? region_id : existing[0].region_id,
        branch_number !== undefined ? branch_number : existing[0].branch_number,
        name !== undefined ? name : existing[0].name,
        name_ar !== undefined ? name_ar : existing[0].name_ar,
        address !== undefined ? address : existing[0].address,
        phone !== undefined ? phone : existing[0].phone,
        status !== undefined ? status : existing[0].status,
        id,
        websiteId
      ]
    );

    // Fetch updated branch with region info
    const [branches] = await pool.execute(`
      SELECT 
        b.*,
        r.name as region_name,
        r.name_ar as region_name_ar,
        c.name as city_name,
        c.name_ar as city_name_ar
      FROM restaurant_branches b
      INNER JOIN regions r ON b.region_id = r.id
      INNER JOIN cities c ON r.city_id = c.id
      WHERE b.id = ?
    `, [id]);

    res.json({ branch: branches[0] });
  } catch (error) {
    console.error('Error updating branch:', error);
    if (error.code === 'ER_DUP_ENTRY') {
      return res.status(400).json({ error: 'Branch number already exists for this restaurant' });
    }
    res.status(500).json({ error: 'Failed to update branch', message: error.message });
  }
});

/**
 * DELETE /api/restaurant/branches/:id
 * Delete a branch (must belong to the logged-in restaurant)
 */
router.delete('/branches/:id', verifyAdminToken, async (req, res) => {
  try {
    const websiteId = req.websiteId;
    const { id } = req.params;

    const [existing] = await pool.execute(
      'SELECT id FROM restaurant_branches WHERE id = ? AND website_id = ?',
      [id, websiteId]
    );
    if (existing.length === 0) {
      return res.status(404).json({ error: 'Branch not found' });
    }

    await pool.execute('DELETE FROM restaurant_branches WHERE id = ? AND website_id = ?', [id, websiteId]);
    res.json({ message: 'Branch deleted successfully' });
  } catch (error) {
    console.error('Error deleting branch:', error);
    res.status(500).json({ error: 'Failed to delete branch', message: error.message });
  }
});

export default router;

