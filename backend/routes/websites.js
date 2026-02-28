import express from 'express';
import { pool } from '../db/init.js';
import multer from 'multer';
import path from 'path';
import { fileURLToPath } from 'url';
import fs from 'fs';
import CacheService from '../services/cacheService.js';
import { saveImage } from '../services/imageStorage.js';

// Helper function to process menu image (handles missing OpenAI gracefully)
async function processMenuImageSafe(imagePath, restaurantName) {
  try {
    const menuProcessor = await import('../services/menuProcessor.js');
    return await menuProcessor.processMenuImage(imagePath, restaurantName);
  } catch (error) {
    console.warn('Menu processing not available:', error.message);
    // Return empty array if processing fails
    return [];
  }
}

const router = express.Router();
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

/**
 * Generate unique barcode code
 */
function generateBarcodeCode() {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  let code = '';
  for (let i = 0; i < 8; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return code;
}

// Memory storage for logo and gallery (processed and stored via imageStorage: local or Spaces)
const logoGalleryMemoryStorage = multer.memoryStorage();

// Configure multer for menu image uploads
const menuImageStorage = multer.diskStorage({
  destination: (req, file, cb) => {
    const uploadDir = path.join(__dirname, '../uploads');
    if (!fs.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    cb(null, 'menu-' + uniqueSuffix + path.extname(file.originalname));
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

const uploadLogo = multer({
  storage: logoGalleryMemoryStorage,
  limits: { fileSize: 5 * 1024 * 1024 }, // 5MB limit
  fileFilter: imageFilter
});

const uploadGallery = multer({
  storage: logoGalleryMemoryStorage,
  limits: { fileSize: 5 * 1024 * 1024 }, // 5MB limit
  fileFilter: imageFilter
});

const uploadMenuImage = multer({
  storage: menuImageStorage,
  limits: { fileSize: 10 * 1024 * 1024 }, // 10MB limit for menu images
  fileFilter: imageFilter
});

/**
 * GET /api/websites
 * Get all restaurant websites.
 * Query: open_now=true — return only restaurants that are operating right now (today's business hours, current time between open_time and close_time).
 * Query: all=true — return all restaurants including unpublished (e.g. for admin dropdowns).
 */
router.get('/', async (req, res) => {
  try {
    const includeUnpublished = req.query.all === 'true' || req.query.all === true;
    if (includeUnpublished) {
      const websites = await CacheService.getAllRestaurantsIncludeUnpublished();
      return res.json({ websites });
    }
    const openNow = req.query.open_now === 'true' || req.query.open_now === true;
    const websites = await CacheService.getAllRestaurants(openNow);
    res.json({ websites });
  } catch (error) {
    console.error('Error fetching websites:', error);
    res.status(500).json({ error: 'Failed to fetch websites', message: error.message });
  }
});

/**
 * GET /api/websites/offers/list
 * Get all active offers from all restaurants (valid today, is_active=1). For app home "Offers" section.
 */
router.get('/offers/list', async (req, res) => {
  try {
    const today = new Date().toISOString().slice(0, 10);
    const [offers] = await pool.execute(
      `SELECT o.id, o.website_id, o.offer_type, o.title, o.description, o.value, o.min_order_value,
              o.valid_from, o.valid_until, o.display_order,
              rw.restaurant_name, rw.logo_url,
              first_product.image_url AS first_product_image_url
       FROM offers o
       INNER JOIN restaurant_websites rw ON rw.id = o.website_id
       LEFT JOIN (
         SELECT p.website_id, p.image_url
         FROM products p
         INNER JOIN (SELECT website_id, MIN(id) AS min_id FROM products GROUP BY website_id) fp ON p.website_id = fp.website_id AND p.id = fp.min_id
         WHERE p.image_url IS NOT NULL AND TRIM(p.image_url) != ''
       ) first_product ON first_product.website_id = o.website_id
       WHERE o.is_active = 1 AND rw.is_published = 1
         AND o.valid_from <= ? AND o.valid_until >= ?
       ORDER BY o.display_order ASC, o.created_at ASC`,
      [today, today]
    );
    res.json({ offers });
  } catch (error) {
    console.error('Error fetching offers list:', error);
    res.status(500).json({ error: 'Failed to fetch offers', message: error.message });
  }
});

/**
 * GET /api/websites/barcode/:code
 * Get restaurant website by barcode code
 */
router.get('/barcode/:code', async (req, res) => {
  try {
    const { code } = req.params;
    const [websites] = await pool.execute(
      'SELECT * FROM restaurant_websites WHERE barcode_code = ?',
      [code.toUpperCase()]
    );
    
    if (websites.length === 0) {
      return res.status(404).json({ error: 'Restaurant not found' });
    }
    
    res.json({ website: websites[0] });
  } catch (error) {
    console.error('Error fetching website by barcode:', error);
    res.status(500).json({ error: 'Failed to fetch website', message: error.message });
  }
});

/**
 * POST /api/websites/:id/generate-barcode
 * Generate a barcode code for an existing website
 */
router.post('/:id/generate-barcode', async (req, res) => {
  try {
    const { id } = req.params;
    
    // Check if website exists
    const [websites] = await pool.execute(
      'SELECT * FROM restaurant_websites WHERE id = ?',
      [id]
    );
    
    if (websites.length === 0) {
      return res.status(404).json({ error: 'Website not found' });
    }
    
    // Check if barcode already exists
    if (websites[0].barcode_code) {
      return res.json({ website: websites[0], message: 'Barcode code already exists' });
    }
    
    // Generate unique barcode code
    let barcodeCode = generateBarcodeCode();
    let attempts = 0;
    while (attempts < 10) {
      const [existing] = await pool.execute(
        'SELECT id FROM restaurant_websites WHERE barcode_code = ?',
        [barcodeCode]
      );
      if (existing.length === 0) break;
      barcodeCode = generateBarcodeCode();
      attempts++;
    }
    
    // Update website with barcode code
    await pool.execute(
      'UPDATE restaurant_websites SET barcode_code = ? WHERE id = ?',
      [barcodeCode, id]
    );
    
    // Get updated website
    const [updatedWebsites] = await pool.execute(
      'SELECT * FROM restaurant_websites WHERE id = ?',
      [id]
    );
    
    // Invalidate cache after barcode update
    await CacheService.invalidateRestaurant(parseInt(id));
    
    res.json({ website: updatedWebsites[0], message: 'Barcode code generated successfully' });
  } catch (error) {
    console.error('Error generating barcode code:', error);
    res.status(500).json({ error: 'Failed to generate barcode code', message: error.message });
  }
});

/**
 * GET /api/websites/domain/:domain
 * Get website by domain or subdomain
 */
router.get('/domain/:domain', async (req, res) => {
  try {
    const { domain } = req.params;
    
    // Try custom domain first
    const [customDomainWebsites] = await pool.execute(
      'SELECT * FROM restaurant_websites WHERE custom_domain = ? AND is_published = 1',
      [domain]
    );
    
    if (customDomainWebsites.length > 0) {
      return res.json({ website: customDomainWebsites[0] });
    }
    
    // Try subdomain
    const [subdomainWebsites] = await pool.execute(
      'SELECT * FROM restaurant_websites WHERE subdomain = ? AND is_published = 1',
      [domain]
    );
    
    if (subdomainWebsites.length > 0) {
      return res.json({ website: subdomainWebsites[0] });
    }
    
    res.status(404).json({ error: 'Website not found' });
  } catch (error) {
    console.error('Error fetching website by domain:', error);
    res.status(500).json({ error: 'Failed to fetch website', message: error.message });
  }
});

/**
 * GET /api/websites/:id/offers
 * Get active offers for a restaurant (public, for customers)
 */
router.get('/:id/offers', async (req, res) => {
  try {
    const websiteId = parseInt(req.params.id);
    if (isNaN(websiteId)) {
      return res.status(400).json({ error: 'Invalid website ID' });
    }
    const offers = await CacheService.getOffers(websiteId);
    console.log(`[offers] GET /api/websites/${websiteId}/offers -> ${offers.length} offer(s)`);
    res.json({ offers });
  } catch (error) {
    console.error('Error fetching offers:', error);
    res.status(500).json({ error: 'Failed to fetch offers', message: error.message });
  }
});

/**
 * GET /api/websites/:id/regions
 * Get all regions for a restaurant's delivery company (public endpoint for customers)
 * Only returns regions if restaurant has an approved delivery company
 */
router.get('/:id/regions', async (req, res) => {
  try {
    const { id } = req.params;
    const websiteId = parseInt(id);
    
    if (isNaN(websiteId)) {
      return res.status(400).json({ error: 'Invalid website ID' });
    }
    
    // Check if restaurant has approved delivery company
    const [website] = await pool.execute(
      'SELECT delivery_company_id FROM restaurant_websites WHERE id = ?',
      [websiteId]
    );
    
    if (website.length === 0) {
      return res.status(404).json({ error: 'Website not found' });
    }
    
    const deliveryCompanyId = website[0]?.delivery_company_id;
    if (!deliveryCompanyId) {
      return res.json({ regions: [] });
    }
    
    // Get all regions from the regions table
    // We show all regions, and areas/zones will be filtered by delivery company
    const [regions] = await pool.execute(`
      SELECT 
        r.id,
        r.city_id,
        r.name,
        r.name_ar,
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
 * GET /api/websites/:id/areas
 * Get areas for a restaurant's delivery company, filtered by region_id (public endpoint for customers)
 */
router.get('/:id/areas', async (req, res) => {
  try {
    const { id } = req.params;
    const { region_id } = req.query;
    const websiteId = parseInt(id);
    
    if (isNaN(websiteId)) {
      return res.status(400).json({ error: 'Invalid website ID' });
    }
    
    // Check if restaurant has approved delivery company
    const [website] = await pool.execute(
      'SELECT delivery_company_id FROM restaurant_websites WHERE id = ?',
      [websiteId]
    );
    
    if (website.length === 0) {
      return res.status(404).json({ error: 'Website not found' });
    }
    
    const deliveryCompanyId = website[0]?.delivery_company_id;
    if (!deliveryCompanyId) {
      return res.json({ areas: [] });
    }
    
    if (!region_id) {
      return res.json({ areas: [] });
    }
    
    // Get all areas in the selected region
    // Areas are filtered by region, and zones will be filtered by delivery company when area is selected
    const [areas] = await pool.execute(`
      SELECT 
        a.id,
        a.region_id,
        a.name,
        a.name_ar,
        r.id as region_id,
        r.name as region_name,
        r.name_ar as region_name_ar,
        c.id as city_id,
        c.name as city_name,
        c.name_ar as city_name_ar
      FROM areas a
      INNER JOIN regions r ON a.region_id = r.id
      INNER JOIN cities c ON r.city_id = c.id
      WHERE a.region_id = ?
      ORDER BY a.name
    `, [parseInt(region_id, 10)]);
    
    res.json({ areas });
  } catch (error) {
    console.error('Error fetching areas:', error);
    res.status(500).json({ error: 'Failed to fetch areas', message: error.message });
  }
});

/**
 * GET /api/websites/:id/zones
 * Get delivery zones for a restaurant's delivery company, filtered by area_id (public endpoint for customers)
 */
router.get('/:id/zones', async (req, res) => {
  try {
    const { id } = req.params;
    const { area_id } = req.query;
    const websiteId = parseInt(id);
    
    if (isNaN(websiteId)) {
      return res.status(400).json({ error: 'Invalid website ID' });
    }
    
    // Check if restaurant has approved delivery company
    const [website] = await pool.execute(
      'SELECT delivery_company_id FROM restaurant_websites WHERE id = ?',
      [websiteId]
    );
    
    if (website.length === 0) {
      return res.status(404).json({ error: 'Website not found' });
    }
    
    const deliveryCompanyId = website[0]?.delivery_company_id;
    if (!deliveryCompanyId) {
      return res.json({ zones: [] });
    }
    
    if (!area_id) {
      return res.json({ zones: [] });
    }
    
    let query = `
      SELECT 
        dz.id,
        dz.delivery_company_id,
        dz.area_id,
        dz.zone_name_id,
        dz.price,
        dz.status,
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
      WHERE dz.delivery_company_id = ? AND dz.status = 'active' AND dz.area_id = ?
      ORDER BY dzn.name_en
    `;
    const params = [deliveryCompanyId, parseInt(area_id, 10)];
    
    const [zones] = await pool.execute(query, params);
    res.json({ zones });
  } catch (error) {
    console.error('Error fetching zones:', error);
    res.status(500).json({ error: 'Failed to fetch zones', message: error.message });
  }
});

/**
 * GET /api/websites/:id/branches
 * Get all active branches for a restaurant (public endpoint for customers)
 */
router.get('/:id/branches', async (req, res) => {
  try {
    const { id } = req.params;
    const websiteId = parseInt(id);
    
    console.log(`[branches] GET /api/websites/${websiteId}/branches`);
    
    if (isNaN(websiteId)) {
      return res.status(400).json({ error: 'Invalid website ID' });
    }
    
    const [branches] = await pool.execute(`
      SELECT 
        b.id,
        b.website_id,
        b.branch_number,
        b.name,
        b.name_ar,
        b.address,
        b.phone,
        b.status,
        NULL as latitude,
        NULL as longitude,
        r.name as region_name,
        r.name_ar as region_name_ar,
        c.name as city_name,
        c.name_ar as city_name_ar
      FROM restaurant_branches b
      INNER JOIN regions r ON b.region_id = r.id
      INNER JOIN cities c ON r.city_id = c.id
      WHERE b.website_id = ? AND b.status = 'active'
      ORDER BY b.branch_number ASC
    `, [websiteId]);
    
    console.log(`[branches] Found ${branches.length} branch(es) for website_id=${websiteId}`);
    res.json({ branches });
  } catch (error) {
    console.error('Error fetching branches:', error);
    res.status(500).json({ error: 'Failed to fetch branches', message: error.message });
  }
});

/**
 * GET /api/websites/:id
 * Get a specific restaurant website
 */
router.get('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    
    // Check if it's a barcode code (8 alphanumeric characters)
    if (/^[A-Z0-9]{8}$/.test(id.toUpperCase())) {
      const [websites] = await pool.execute(
        'SELECT * FROM restaurant_websites WHERE barcode_code = ?',
        [id.toUpperCase()]
      );
      
      if (websites.length > 0) {
        return res.json({ website: websites[0] });
      }
    }
    
    // Otherwise, treat as numeric ID - use cache
    const websiteId = parseInt(id);
    if (isNaN(websiteId)) {
      return res.status(400).json({ error: 'Invalid website ID' });
    }
    
    const website = await CacheService.getRestaurant(websiteId);
    
    if (!website) {
      return res.status(404).json({ error: 'Website not found' });
    }
    
    res.json({ website });
  } catch (error) {
    console.error('Error fetching website:', error);
    res.status(500).json({ error: 'Failed to fetch website', message: error.message });
  }
});

/**
 * POST /api/websites
 * Create a new restaurant website
 */
router.post('/', async (req, res) => {
  try {
    const {
      restaurant_name,
      restaurant_name_ar,
      logo_url,
      description,
      description_ar,
      address,
      address_ar,
      phone,
      email,
      website_url,
      primary_color,
      secondary_color,
      font_family,
      custom_css,
      menu_items,
      social_links,
      gallery_images,
      locations,
      app_download_url,
      newsletter_enabled,
      is_published,
      subdomain,
      custom_domain
    } = req.body;

    if (!restaurant_name) {
      return res.status(400).json({ error: 'Restaurant name is required' });
    }

    // Generate unique barcode code
    let barcodeCode = generateBarcodeCode();
    let attempts = 0;
    while (attempts < 10) {
      const [existing] = await pool.execute(
        'SELECT id FROM restaurant_websites WHERE barcode_code = ?',
        [barcodeCode]
      );
      if (existing.length === 0) break;
      barcodeCode = generateBarcodeCode();
      attempts++;
    }

    // Generate subdomain if not provided
    let finalSubdomain = subdomain;
    if (!finalSubdomain) {
      // Create subdomain from restaurant name (lowercase, alphanumeric, hyphens only)
      finalSubdomain = restaurant_name
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '')
        .substring(0, 50);
      
      // Ensure uniqueness
      let subdomainAttempts = 0;
      let uniqueSubdomain = finalSubdomain;
      while (subdomainAttempts < 10) {
        const [existing] = await pool.execute(
          'SELECT id FROM restaurant_websites WHERE subdomain = ?',
          [uniqueSubdomain]
        );
        if (existing.length === 0) break;
        uniqueSubdomain = `${finalSubdomain}-${subdomainAttempts + 1}`;
        subdomainAttempts++;
      }
      finalSubdomain = uniqueSubdomain;
    } else {
      // Validate subdomain format
      if (!/^[a-z0-9]([a-z0-9-]*[a-z0-9])?$/.test(finalSubdomain.toLowerCase())) {
        return res.status(400).json({ error: 'Invalid subdomain format. Use only lowercase letters, numbers, and hyphens.' });
      }
      
      // Check if subdomain is already taken
      const [existing] = await pool.execute(
        'SELECT id FROM restaurant_websites WHERE subdomain = ?',
        [finalSubdomain.toLowerCase()]
      );
      if (existing.length > 0) {
        return res.status(400).json({ error: 'Subdomain already taken' });
      }
    }

    // Validate custom domain if provided
    if (custom_domain) {
      // Basic domain validation
      const domainRegex = /^([a-z0-9]+(-[a-z0-9]+)*\.)+[a-z]{2,}$/i;
      if (!domainRegex.test(custom_domain)) {
        return res.status(400).json({ error: 'Invalid custom domain format' });
      }
      
      // Check if custom domain is already taken
      const [existing] = await pool.execute(
        'SELECT id FROM restaurant_websites WHERE custom_domain = ?',
        [custom_domain.toLowerCase()]
      );
      if (existing.length > 0) {
        return res.status(400).json({ error: 'Custom domain already in use' });
      }
    }

    const [result] = await pool.execute(
      `INSERT INTO restaurant_websites 
       (restaurant_name, restaurant_name_ar, logo_url, description, description_ar, address, address_ar, phone, email, website_url,
        primary_color, secondary_color, font_family, custom_css, menu_items, social_links, 
        gallery_images, locations, app_download_url, newsletter_enabled, is_published, barcode_code, subdomain, custom_domain)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        restaurant_name,
        restaurant_name_ar || null,
        logo_url || null,
        description || null,
        description_ar || null,
        address || null,
        address_ar || null,
        phone || null,
        email || null,
        website_url || null,
        primary_color || '#4F46E5',
        secondary_color || '#7C3AED',
        font_family || 'Inter, sans-serif',
        custom_css || null,
        menu_items ? JSON.stringify(menu_items) : null,
        social_links ? JSON.stringify(social_links) : null,
        gallery_images ? JSON.stringify(gallery_images) : null,
        locations ? JSON.stringify(locations) : null,
        app_download_url || null,
        newsletter_enabled || false,
        is_published || false,
        barcodeCode,
        finalSubdomain ? finalSubdomain.toLowerCase() : null,
        custom_domain ? custom_domain.toLowerCase() : null
      ]
    );

    const [newWebsite] = await pool.execute(
      'SELECT * FROM restaurant_websites WHERE id = ?',
      [result.insertId]
    );

    // Invalidate restaurant lists cache
    await CacheService.invalidateRestaurantLists();

    res.status(201).json({ website: newWebsite[0] });
  } catch (error) {
    console.error('Error creating website:', error);
    res.status(500).json({ error: 'Failed to create website', message: error.message });
  }
});

/**
 * POST /api/websites/:id/logo
 * Upload logo for a restaurant website
 * Stored under: websites/[website_id]/logo/[filename].webp
 */
router.post('/:id/logo', uploadLogo.single('logo'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'No file uploaded' });
    }

    const { id } = req.params;
    const websiteId = parseInt(id, 10);

    // Check website exists
    const [websites] = await pool.execute('SELECT id, logo_file_path FROM restaurant_websites WHERE id = ?', [id]);
    if (websites.length === 0) {
      return res.status(404).json({ error: 'Website not found' });
    }

    const saved = await saveImage('logo', req.file, { websiteId });

    // Delete old logo file only if it is a local path
    const oldPath = websites[0].logo_file_path;
    if (oldPath && typeof oldPath === 'string' && oldPath.startsWith('/') && fs.existsSync(oldPath)) {
      try {
        fs.unlinkSync(oldPath);
      } catch (err) {
        console.warn('Could not delete old logo file:', err.message);
      }
    }

    await pool.execute(
      'UPDATE restaurant_websites SET logo_url = ?, logo_file_path = ? WHERE id = ?',
      [saved.url, saved.storagePath, id]
    );

    const [updated] = await pool.execute('SELECT * FROM restaurant_websites WHERE id = ?', [id]);
    await CacheService.invalidateRestaurant(websiteId);

    res.json({ website: updated[0] });
  } catch (error) {
    console.error('Error uploading logo:', error);
    res.status(500).json({ error: 'Failed to upload logo', message: error.message });
  }
});

/**
 * PUT /api/websites/:id
 * Update a restaurant website
 */
router.put('/:id', async (req, res) => {
  try {
    console.log('Update website request:', { id: req.params.id, body: Object.keys(req.body) });
    const { id } = req.params;
    const {
      restaurant_name,
      restaurant_name_ar,
      logo_url,
      description,
      description_ar,
      address,
      address_ar,
      phone,
      email,
      website_url,
      primary_color,
      secondary_color,
      font_family,
      custom_css,
      menu_items,
      social_links,
      gallery_images, // Note: gallery_images should be updated via /gallery endpoint, but we allow it here for flexibility
      locations,
      app_download_url,
      newsletter_enabled,
      is_published,
      subdomain,
      custom_domain
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
    if (logo_url !== undefined) {
      updateFields.push('logo_url = ?');
      updateValues.push(logo_url);
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
    if (website_url !== undefined) {
      updateFields.push('website_url = ?');
      updateValues.push(website_url);
    }
    if (primary_color !== undefined) {
      updateFields.push('primary_color = ?');
      updateValues.push(primary_color);
    }
    if (secondary_color !== undefined) {
      updateFields.push('secondary_color = ?');
      updateValues.push(secondary_color);
    }
    if (font_family !== undefined) {
      updateFields.push('font_family = ?');
      updateValues.push(font_family);
    }
    if (custom_css !== undefined) {
      updateFields.push('custom_css = ?');
      updateValues.push(custom_css);
    }
    if (menu_items !== undefined) {
      updateFields.push('menu_items = ?');
      updateValues.push(JSON.stringify(menu_items));
    }
    if (social_links !== undefined) {
      updateFields.push('social_links = ?');
      updateValues.push(JSON.stringify(social_links));
    }
    if (gallery_images !== undefined) {
      updateFields.push('gallery_images = ?');
      updateValues.push(JSON.stringify(gallery_images));
    }
    if (locations !== undefined) {
      updateFields.push('locations = ?');
      updateValues.push(JSON.stringify(locations));
    }
    if (app_download_url !== undefined) {
      updateFields.push('app_download_url = ?');
      updateValues.push(app_download_url);
    }
    if (newsletter_enabled !== undefined) {
      updateFields.push('newsletter_enabled = ?');
      updateValues.push(newsletter_enabled);
    }
    if (is_published !== undefined) {
      updateFields.push('is_published = ?');
      updateValues.push(is_published);
    }

    if (subdomain !== undefined) {
      if (subdomain) {
        // Validate subdomain format
        if (!/^[a-z0-9]([a-z0-9-]*[a-z0-9])?$/.test(subdomain.toLowerCase())) {
          return res.status(400).json({ error: 'Invalid subdomain format. Use only lowercase letters, numbers, and hyphens.' });
        }
        
        // Check if subdomain is already taken by another website
        const [existing] = await pool.execute(
          'SELECT id FROM restaurant_websites WHERE subdomain = ? AND id != ?',
          [subdomain.toLowerCase(), id]
        );
        if (existing.length > 0) {
          return res.status(400).json({ error: 'Subdomain already taken' });
        }
      }
      updateFields.push('subdomain = ?');
      updateValues.push(subdomain ? subdomain.toLowerCase() : null);
    }

    if (custom_domain !== undefined) {
      if (custom_domain) {
        // Basic domain validation
        const domainRegex = /^([a-z0-9]+(-[a-z0-9]+)*\.)+[a-z]{2,}$/i;
        if (!domainRegex.test(custom_domain)) {
          return res.status(400).json({ error: 'Invalid custom domain format' });
        }
        
        // Check if custom domain is already taken by another website
        const [existing] = await pool.execute(
          'SELECT id FROM restaurant_websites WHERE custom_domain = ? AND id != ?',
          [custom_domain.toLowerCase(), id]
        );
        if (existing.length > 0) {
          return res.status(400).json({ error: 'Custom domain already in use' });
        }
      }
      updateFields.push('custom_domain = ?');
      updateValues.push(custom_domain ? custom_domain.toLowerCase() : null);
      // Reset verification when domain changes
      if (custom_domain) {
        updateFields.push('domain_verified = ?');
        updateValues.push(false);
      }
    }

    if (updateFields.length === 0) {
      return res.status(400).json({ error: 'No fields to update' });
    }

    updateValues.push(id);

    try {
      const updateQuery = `UPDATE restaurant_websites SET ${updateFields.join(', ')} WHERE id = ?`;
      console.log('Executing update query:', updateQuery);
      console.log('Update values:', updateValues.map((v, i) => `${updateFields[i]}: ${typeof v === 'string' && v.length > 100 ? v.substring(0, 100) + '...' : v}`));
      
      await pool.execute(updateQuery, updateValues);
      console.log('Update successful');
    } catch (dbError) {
      console.error('Database update error:', dbError);
      console.error('Error code:', dbError.code);
      console.error('Error message:', dbError.message);
      console.error('Error sqlState:', dbError.sqlState);
      throw dbError;
    }

    const [websites] = await pool.execute(
      'SELECT * FROM restaurant_websites WHERE id = ?',
      [id]
    );

    if (websites.length === 0) {
      return res.status(404).json({ error: 'Website not found' });
    }

    // Invalidate cache after update
    await CacheService.invalidateRestaurant(parseInt(id));

    res.json({ website: websites[0] });
  } catch (error) {
    console.error('Error updating website:', error);
    console.error('Error details:', {
      message: error.message,
      code: error.code,
      sqlState: error.sqlState,
      errno: error.errno,
      stack: error.stack
    });
    res.status(500).json({ 
      error: 'Failed to update website', 
      message: error.message,
      details: process.env.NODE_ENV === 'development' ? {
        code: error.code,
        sqlState: error.sqlState
      } : undefined
    });
  }
});

/**
 * POST /api/websites/:id/gallery
 * Upload multiple gallery images for a restaurant website
 * Stored under: websites/[website_id]/gallary/[filename].webp
 */
router.post('/:id/gallery', uploadGallery.array('images', 20), async (req, res) => {
  try {
    if (!req.files || req.files.length === 0) {
      return res.status(400).json({ error: 'No files uploaded' });
    }

    const { id } = req.params;
    const websiteId = parseInt(id, 10);

    const [websites] = await pool.execute(
      'SELECT gallery_images FROM restaurant_websites WHERE id = ?',
      [id]
    );

    if (websites.length === 0) {
      return res.status(404).json({ error: 'Website not found' });
    }

    let existingImages = [];
    try {
      existingImages = websites[0].gallery_images
        ? (typeof websites[0].gallery_images === 'string'
            ? JSON.parse(websites[0].gallery_images)
            : websites[0].gallery_images)
        : [];
    } catch (e) {
      existingImages = [];
    }

    const newImages = [];
    for (const file of req.files) {
      const saved = await saveImage('gallary', file, { websiteId });
      newImages.push({
        url: saved.url,
        file_path: saved.storagePath,
        filename: saved.storagePath.split('/').pop() || `gallery-${Date.now()}.webp`
      });
    }

    const allImages = [...existingImages, ...newImages];

    try {
      await pool.execute(
        'UPDATE restaurant_websites SET gallery_images = ? WHERE id = ?',
        [JSON.stringify(allImages), id]
      );
    } catch (dbError) {
      if (dbError.message && dbError.message.includes("Unknown column 'gallery_images'")) {
        throw new Error('Gallery images column not found in database. Please restart the server to run migrations.');
      }
      throw dbError;
    }

    const [updatedWebsites] = await pool.execute(
      'SELECT * FROM restaurant_websites WHERE id = ?',
      [id]
    );
    await CacheService.invalidateRestaurant(websiteId);

    res.json({ website: updatedWebsites[0] });
  } catch (error) {
    console.error('Error uploading gallery images:', error);
    res.status(500).json({
      error: 'Failed to upload gallery images',
      message: error.message,
      details: process.env.NODE_ENV === 'development' ? { stack: error.stack } : undefined
    });
  }
});

/**
 * DELETE /api/websites/:id/gallery/:imageIndex
 * Delete a gallery image
 */
router.delete('/:id/gallery/:imageIndex', async (req, res) => {
  try {
    const { id, imageIndex } = req.params;
    const index = parseInt(imageIndex);

    const [websites] = await pool.execute(
      'SELECT gallery_images FROM restaurant_websites WHERE id = ?',
      [id]
    );

    if (websites.length === 0) {
      return res.status(404).json({ error: 'Website not found' });
    }

    let galleryImages = [];
    try {
      galleryImages = websites[0].gallery_images 
        ? (typeof websites[0].gallery_images === 'string' 
            ? JSON.parse(websites[0].gallery_images) 
            : websites[0].gallery_images)
        : [];
    } catch (e) {
      galleryImages = [];
    }

    if (index < 0 || index >= galleryImages.length) {
      return res.status(400).json({ error: 'Invalid image index' });
    }

    // Delete file if it is a local path (not Spaces URL)
    const imageToDelete = galleryImages[index];
    if (imageToDelete.file_path && typeof imageToDelete.file_path === 'string' && imageToDelete.file_path.startsWith('/') && fs.existsSync(imageToDelete.file_path)) {
      try {
        fs.unlinkSync(imageToDelete.file_path);
      } catch (err) {
        console.warn('Could not delete image file:', err.message);
      }
    }

    // Remove from array
    galleryImages.splice(index, 1);

    await pool.execute(
      'UPDATE restaurant_websites SET gallery_images = ? WHERE id = ?',
      [JSON.stringify(galleryImages), id]
    );

    // Invalidate cache after gallery image deletion
    await CacheService.invalidateRestaurant(parseInt(id));

    res.json({ message: 'Image deleted successfully' });
  } catch (error) {
    console.error('Error deleting gallery image:', error);
    res.status(500).json({ error: 'Failed to delete image', message: error.message });
  }
});

/**
 * POST /api/websites/:id/menu-image
 * Upload menu image and extract menu items using AI
 */
router.post('/:id/menu-image', uploadMenuImage.single('menu_image'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'No menu image uploaded' });
    }

    const { id } = req.params;
    
    // Check if website exists
    const [websites] = await pool.execute(
      'SELECT * FROM restaurant_websites WHERE id = ?',
      [id]
    );

    if (websites.length === 0) {
      // Clean up uploaded file
      fs.unlinkSync(req.file.path);
      return res.status(404).json({ error: 'Website not found' });
    }

    const website = websites[0];
    const filePath = `/uploads/${req.file.filename}`;
    const apiBaseUrl = process.env.API_BASE_URL || `http://localhost:${process.env.PORT || 3000}`;
    const menuImageUrl = `${apiBaseUrl}${filePath}`;

    // Process menu image with AI (if available)
    let menuItems = [];
    try {
      if (process.env.OPENAI_API_KEY) {
        console.log('Processing menu image with AI...');
        menuItems = await processMenuImageSafe(req.file.path, website.restaurant_name);
        console.log(`Extracted ${menuItems.length} menu items from image`);
      } else {
        console.log('OpenAI not configured. Menu image saved. User can add menu items manually.');
      }
    } catch (aiError) {
      console.error('AI processing error:', aiError);
      console.log('Menu image saved. User can add menu items manually.');
      // Still save the image even if AI processing fails
      // User can manually add menu items later
    }

    // Update website with menu image and extracted items
    await pool.execute(
      'UPDATE restaurant_websites SET menu_image_url = ?, menu_image_path = ?, menu_items = ? WHERE id = ?',
      [
        menuImageUrl,
        req.file.path,
        JSON.stringify(menuItems),
        id
      ]
    );

    const [updatedWebsites] = await pool.execute(
      'SELECT * FROM restaurant_websites WHERE id = ?',
      [id]
    );

    // Invalidate cache after menu image update
    await CacheService.invalidateRestaurant(parseInt(id));

    res.json({ 
      website: updatedWebsites[0],
      menuItems: menuItems,
      itemsExtracted: menuItems.length
    });
  } catch (error) {
    console.error('Error uploading menu image:', error);
    
    // Clean up uploaded file on error
    if (req.file && fs.existsSync(req.file.path)) {
      try {
        fs.unlinkSync(req.file.path);
      } catch (cleanupError) {
        console.error('Failed to cleanup file:', cleanupError);
      }
    }
    
    res.status(500).json({ 
      error: 'Failed to process menu image', 
      message: error.message 
    });
  }
});

/**
 * DELETE /api/websites/:id
 * Delete a restaurant website
 */
router.delete('/:id', async (req, res) => {
  try {
    const { id } = req.params;

    // Get website to delete logo file if exists
    const [websites] = await pool.execute(
      'SELECT logo_file_path FROM restaurant_websites WHERE id = ?',
      [id]
    );

    if (websites.length === 0) {
      return res.status(404).json({ error: 'Website not found' });
    }

    // Delete logo file only if it is a local path (not Spaces)
    const logoPath = websites[0].logo_file_path;
    if (logoPath && typeof logoPath === 'string' && logoPath.startsWith('/') && fs.existsSync(logoPath)) {
      try {
        fs.unlinkSync(logoPath);
      } catch (err) {
        console.warn('Could not delete logo file:', err.message);
      }
    }

    await pool.execute('DELETE FROM restaurant_websites WHERE id = ?', [id]);

    // Invalidate cache after delete
    await CacheService.invalidateRestaurant(parseInt(id));
    await CacheService.invalidateRestaurantLists();

    res.json({ message: 'Website deleted successfully' });
  } catch (error) {
    console.error('Error deleting website:', error);
    res.status(500).json({ error: 'Failed to delete website', message: error.message });
  }
});

export default router;

