import express from 'express';
import multer from 'multer';
import path from 'path';
import { fileURLToPath } from 'url';
import fs from 'fs';
import { pool } from '../db/init.js';
import { processMenuImage } from '../services/menuProcessor.js';
import CacheService from '../services/cacheService.js';

const router = express.Router();
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

/**
 * GET /api/menu-extractor/test
 * Test endpoint to verify route is working
 */
router.get('/test', (req, res) => {
  res.json({ 
    success: true, 
    message: 'Menu extractor API is working',
    timestamp: new Date().toISOString()
  });
});

// Configure multer for menu image uploads
const menuImageStorage = multer.diskStorage({
  destination: (req, file, cb) => {
    const uploadDir = path.join(__dirname, '../uploads/menu-extractor');
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
  const allowedTypes = /jpeg|jpg|png|gif|webp/;
  const extname = allowedTypes.test(path.extname(file.originalname).toLowerCase());
  const mimetype = allowedTypes.test(file.mimetype);
  
  if (mimetype && extname) {
    return cb(null, true);
  } else {
    cb(new Error('Only image files are allowed!'));
  }
};

const upload = multer({
  storage: menuImageStorage,
  limits: { fileSize: 10 * 1024 * 1024 }, // 10MB limit
  fileFilter: imageFilter
});

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

/**
 * POST /api/menu-extractor/process
 * Process menu images and extract products
 */
router.post('/process', upload.array('images', 10), async (req, res) => {
  try {
    if (!req.files || req.files.length === 0) {
      return res.status(400).json({ error: 'No images uploaded' });
    }

    const allProducts = [];
    let restaurantName = null;
    let restaurantNameAr = null;

    // Process each image
    for (const file of req.files) {
      try {
        console.log(`Processing image: ${file.filename}`);
        // Use OpenAI to extract menu items
        const extractionResult = await processMenuImage(file.path, 'Restaurant');

        // Handle new format (object with restaurant info) or old format (array)
        let menuItems = [];
        if (extractionResult && typeof extractionResult === 'object') {
          if (extractionResult.products && Array.isArray(extractionResult.products)) {
            // New format
            menuItems = extractionResult.products;
            if (!restaurantName && extractionResult.restaurant_name) {
              restaurantName = extractionResult.restaurant_name;
            }
            if (!restaurantNameAr && extractionResult.restaurant_name_ar) {
              restaurantNameAr = extractionResult.restaurant_name_ar;
            }
          } else if (Array.isArray(extractionResult)) {
            // Old format (array)
            menuItems = extractionResult;
          }
        } else if (Array.isArray(extractionResult)) {
          menuItems = extractionResult;
        }

        if (menuItems && menuItems.length > 0) {
          console.log(`Extracted ${menuItems.length} items from ${file.filename}`);

          // Process and normalize menu items
          const processedItems = menuItems.map(item => {
            // Extract numeric price
            let price = 0;
            if (item.price) {
              const priceStr = item.price.toString().trim();
              // Remove currency symbols and extract number
              const priceMatch = priceStr.match(/[\d.]+/);
              if (priceMatch) {
                price = parseFloat(priceMatch[0]);
              }
            }

            return {
              name: item.name || '',
              name_ar: item.name_ar || item.name || '',
              description: item.description || '',
              description_ar: item.description_ar || item.description || '',
              price: price || 0,
              category: item.category || 'General',
              category_ar: item.category_ar || item.category || 'عام',
              is_available: true
            };
          }).filter(item => item.name && item.name.trim() !== ''); // Filter out empty names

          allProducts.push(...processedItems);
        } else {
          console.log(`No items extracted from ${file.filename}`);
        }
      } catch (error) {
        console.error(`Error processing image ${file.filename}:`, error);
        console.error('Error stack:', error.stack);
        // Continue with other images
      }
    }

    // Group products by category
    const productsByCategory = {};
    allProducts.forEach(product => {
      const category = product.category || 'General';
      if (!productsByCategory[category]) {
        productsByCategory[category] = [];
      }
      productsByCategory[category].push(product);
    });

    // Flatten and deduplicate products (by name)
    const uniqueProducts = [];
    const seenNames = new Set();
    
    Object.values(productsByCategory).flat().forEach(product => {
      const key = product.name.toLowerCase().trim();
      if (!seenNames.has(key) && product.name && product.price > 0) {
        seenNames.add(key);
        uniqueProducts.push(product);
      }
    });

    // Try to extract restaurant name from first image filename or use default
    if (!restaurantName && req.files[0]) {
      const filename = req.files[0].originalname;
      restaurantName = filename.replace(/\.[^/.]+$/, '').replace(/[-_]/g, ' ');
    }

    // Try to extract restaurant name from first image filename if still not found
    if (!restaurantName && req.files[0]) {
      const filename = req.files[0].originalname;
      restaurantName = filename.replace(/\.[^/.]+$/, '').replace(/[-_]/g, ' ');
    }

    console.log(`Processing complete: ${uniqueProducts.length} unique products extracted from ${req.files.length} images`);
    console.log(`Restaurant name: ${restaurantName || 'Not found'}`);
    console.log(`Restaurant name (AR): ${restaurantNameAr || 'Not found'}`);

    res.json({
      success: true,
      products: uniqueProducts,
      restaurant_name: restaurantName || null,
      restaurant_name_ar: restaurantNameAr || null,
      total_images: req.files.length,
      total_products: uniqueProducts.length,
      extraction_status: uniqueProducts.length > 0 ? 'success' : 'no_products_found'
    });

  } catch (error) {
    console.error('Error processing menu images:', error);
    res.status(500).json({ 
      error: 'Failed to process menu images', 
      message: error.message 
    });
  }
});

/**
 * POST /api/menu-extractor/create
 * Create restaurant and insert all products
 */
router.post('/create', async (req, res) => {
  const connection = await pool.getConnection();
  try {
    await connection.beginTransaction();

    const { restaurant, products } = req.body;

    if (!restaurant || !restaurant.restaurant_name || !restaurant.phone) {
      await connection.rollback();
      return res.status(400).json({ 
        error: 'Restaurant name and phone are required' 
      });
    }

    if (!products || products.length === 0) {
      await connection.rollback();
      return res.status(400).json({ 
        error: 'At least one product is required' 
      });
    }

    // Generate unique barcode code
    let barcodeCode = generateBarcodeCode();
    let attempts = 0;
    while (attempts < 10) {
      const [existing] = await connection.execute(
        'SELECT id FROM restaurant_websites WHERE barcode_code = ?',
        [barcodeCode]
      );
      if (existing.length === 0) break;
      barcodeCode = generateBarcodeCode();
      attempts++;
    }

    // Generate subdomain from restaurant name
    let subdomain = restaurant.restaurant_name
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
      .substring(0, 50);
    
    if (!subdomain) {
      subdomain = 'restaurant-' + Date.now().toString(36);
    }

    // Ensure uniqueness
    let subdomainAttempts = 0;
    let uniqueSubdomain = subdomain;
    while (subdomainAttempts < 10) {
      const [existing] = await connection.execute(
        'SELECT id FROM restaurant_websites WHERE subdomain = ?',
        [uniqueSubdomain]
      );
      if (existing.length === 0) break;
      uniqueSubdomain = `${subdomain}-${subdomainAttempts + 1}`;
      subdomainAttempts++;
    }

    // Create restaurant website
    const [websiteResult] = await connection.execute(
      `INSERT INTO restaurant_websites 
       (restaurant_name, restaurant_name_ar, phone, email, address, address_ar, 
        description, description_ar, subdomain, barcode_code, is_published)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE)`,
      [
        restaurant.restaurant_name,
        restaurant.restaurant_name_ar || null,
        restaurant.phone,
        restaurant.email || null,
        restaurant.address || null,
        restaurant.address_ar || null,
        restaurant.description || null,
        restaurant.description_ar || null,
        uniqueSubdomain,
        barcodeCode
      ]
    );

    const websiteId = websiteResult.insertId;

    // Insert products
    const productPromises = products.map(product => {
      return connection.execute(
        `INSERT INTO products 
         (website_id, name, name_ar, description, description_ar, price, category, category_ar, is_available)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        [
          websiteId,
          product.name,
          product.name_ar || product.name,
          product.description || null,
          product.description_ar || product.description || null,
          parseFloat(product.price) || 0,
          product.category || 'General',
          product.category_ar || product.category || 'عام',
          product.is_available !== undefined ? product.is_available : true
        ]
      );
    });

    await Promise.all(productPromises);

    await connection.commit();
    connection.release();

    // Invalidate cache
    await CacheService.invalidateRestaurantLists();

    res.json({
      success: true,
      website_id: websiteId,
      subdomain: uniqueSubdomain,
      barcode_code: barcodeCode,
      products_created: products.length
    });

  } catch (error) {
    await connection.rollback();
    connection.release();
    console.error('Error creating restaurant:', error);
    res.status(500).json({ 
      error: 'Failed to create restaurant', 
      message: error.message 
    });
  }
});

/**
 * POST /api/menu-extractor/add-products
 * Add products to an existing restaurant (by website_id)
 */
router.post('/add-products', async (req, res) => {
  try {
    const { website_id, products } = req.body;

    if (!website_id) {
      return res.status(400).json({ error: 'website_id is required' });
    }

    if (!products || !Array.isArray(products) || products.length === 0) {
      return res.status(400).json({ error: 'At least one product is required' });
    }

    // Verify restaurant exists
    const [websites] = await pool.execute(
      'SELECT id, restaurant_name FROM restaurant_websites WHERE id = ?',
      [website_id]
    );

    if (websites.length === 0) {
      return res.status(404).json({ error: 'Restaurant not found' });
    }

    const restaurantName = websites[0].restaurant_name;

    // Insert products
    for (const product of products) {
      await pool.execute(
        `INSERT INTO products 
         (website_id, name, name_ar, description, description_ar, price, category, category_ar, is_available)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        [
          website_id,
          product.name || '',
          product.name_ar || product.name || '',
          product.description || null,
          product.description_ar || product.description || null,
          parseFloat(product.price) || 0,
          product.category || 'General',
          product.category_ar || product.category || 'عام',
          product.is_available !== undefined ? product.is_available : true
        ]
      );
    }

    // Invalidate products cache for this restaurant
    await CacheService.invalidateProducts(parseInt(website_id));

    res.json({
      success: true,
      website_id: parseInt(website_id),
      restaurant_name: restaurantName,
      products_added: products.length
    });
  } catch (error) {
    console.error('Error adding products:', error);
    res.status(500).json({
      error: 'Failed to add products',
      message: error.message
    });
  }
});

export default router;
