import express from 'express';
import { pool } from '../db/init.js';
import multer from 'multer';
import path from 'path';
import { fileURLToPath } from 'url';
import fs from 'fs';
import CacheService from '../services/cacheService.js';
import { saveImage } from '../services/imageStorage.js';

const router = express.Router();
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Configure multer for product image uploads (in-memory; actual storage is handled by imageStorage)
const imageFilter = (req, file, cb) => {
  if (file.mimetype.startsWith('image/')) {
    cb(null, true);
  } else {
    cb(new Error('Only image files are allowed'), false);
  }
};

const uploadProductImage = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 5 * 1024 * 1024 }, // 5MB limit
  fileFilter: imageFilter
});

/**
 * GET /api/products/website/:websiteId
 * Get all products for a website
 */
router.get('/website/:websiteId', async (req, res) => {
  try {
    const { websiteId } = req.params;
    const products = await CacheService.getProducts(parseInt(websiteId));
    res.json({ products });
  } catch (error) {
    console.error('Error fetching products:', error);
    res.status(500).json({ error: 'Failed to fetch products', message: error.message });
  }
});

/**
 * GET /api/products/:id
 * Get a single product
 */
router.get('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const [products] = await pool.execute(
      'SELECT * FROM products WHERE id = ?',
      [id]
    );
    if (products.length === 0) {
      return res.status(404).json({ error: 'Product not found' });
    }
    res.json({ product: products[0] });
  } catch (error) {
    console.error('Error fetching product:', error);
    res.status(500).json({ error: 'Failed to fetch product', message: error.message });
  }
});

/**
 * GET /api/products/:id/addons
 * Get addons for a product (public endpoint)
 */
router.get('/:id/addons', async (req, res) => {
  try {
    const { id } = req.params;
    const [addons] = await pool.execute(
      'SELECT * FROM product_addons WHERE product_id = ? ORDER BY display_order, id',
      [id]
    );
    const [product] = await pool.execute(
      'SELECT addon_required, addon_required_min FROM products WHERE id = ?',
      [id]
    );
    res.json({
      addons: addons || [],
      addon_required: product[0]?.addon_required ?? false,
      addon_required_min: product[0]?.addon_required_min ?? null
    });
  } catch (error) {
    console.error('Error fetching product addons:', error);
    res.status(500).json({ error: 'Failed to fetch addons', message: error.message });
  }
});

/**
 * POST /api/products
 * Create a new product
 */
router.post('/', async (req, res) => {
  try {
    console.log('Product creation request received:', req.body);
    
    const {
      website_id,
      name,
      name_ar,
      description,
      description_ar,
      price,
      original_price,
      category,
      category_ar,
      is_available
    } = req.body;

    // Validate required fields
    if (!website_id) {
      console.error('Missing website_id');
      return res.status(400).json({ error: 'website_id is required' });
    }
    
    if (!name || !name.trim()) {
      console.error('Missing or empty name');
      return res.status(400).json({ error: 'name is required' });
    }
    
    if (!price || isNaN(parseFloat(price)) || parseFloat(price) <= 0) {
      console.error('Invalid price:', price);
      return res.status(400).json({ error: 'Valid price is required' });
    }

    const priceValue = parseFloat(price);
    
    // Parse original_price if provided
    let originalPriceValue = null;
    if (original_price !== undefined && original_price !== null && original_price !== '') {
      const parsedOriginalPrice = parseFloat(original_price);
      if (!isNaN(parsedOriginalPrice) && parsedOriginalPrice >= 0) {
        originalPriceValue = parsedOriginalPrice;
      }
    }
    
    console.log('Inserting product with values:', {
      website_id,
      name: name.trim(),
      description: description?.trim() || null,
      price: priceValue,
      original_price: originalPriceValue,
      category: category?.trim() || null,
      is_available: is_available !== undefined ? is_available : true
    });

    const [result] = await pool.execute(
      `INSERT INTO products (website_id, name, name_ar, description, description_ar, price, original_price, category, category_ar, is_available)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        website_id,
        name.trim(),
        name_ar?.trim() || null,
        description?.trim() || null,
        description_ar?.trim() || null,
        priceValue,
        originalPriceValue,
        category?.trim() || null,
        category_ar?.trim() || null,
        is_available !== undefined ? is_available : true
      ]
    );
    
    console.log('Product inserted with ID:', result.insertId);

    const [products] = await pool.execute(
      'SELECT * FROM products WHERE id = ?',
      [result.insertId]
    );

    // Invalidate products cache after creation
    await CacheService.invalidateProducts(website_id);

    console.log('Product created successfully:', products[0]);
    res.status(201).json({ product: products[0] });
  } catch (error) {
    console.error('Error creating product:', error);
    console.error('Error stack:', error.stack);
    console.error('Request body:', req.body);
    console.error('SQL Error code:', error.code);
    console.error('SQL Error sqlMessage:', error.sqlMessage);
    
    // Check if the error is due to missing column
    if (error.code === 'ER_BAD_FIELD_ERROR' && error.sqlMessage && error.sqlMessage.includes('original_price')) {
      return res.status(500).json({ 
        error: 'Database column missing', 
        message: 'The original_price column does not exist. Please restart the backend server to run the migration.',
        sqlError: error.sqlMessage
      });
    }
    
    res.status(500).json({ 
      error: 'Failed to create product', 
      message: error.message,
      sqlError: error.sqlMessage,
      sqlCode: error.code
    });
  }
});

/**
 * POST /api/products/:id/image
 * Upload product image
 */
router.post('/:id/image', uploadProductImage.single('image'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'No image uploaded' });
    }

    const { id } = req.params;
    
    // Check if product exists
    const [products] = await pool.execute(
      'SELECT * FROM products WHERE id = ?',
      [id]
    );
    
    if (products.length === 0) {
      return res.status(404).json({ error: 'Product not found' });
    }

    // Process and store image (local or Spaces)
    const saved = await saveImage('products', req.file);
    const imageUrl = saved.url;
    const storagePath = saved.storagePath;

    // Delete old local image if exists and looks like a filesystem path
    if (products[0].image_path && products[0].image_path.startsWith('/') && fs.existsSync(products[0].image_path)) {
      try {
        fs.unlinkSync(products[0].image_path);
      } catch (err) {
        console.warn('Could not delete old image:', err.message);
      }
    }
    
    await pool.execute(
      'UPDATE products SET image_url = ?, image_path = ? WHERE id = ?',
      [imageUrl, storagePath, id]
    );

    const [updatedProducts] = await pool.execute(
      'SELECT * FROM products WHERE id = ?',
      [id]
    );

    // Invalidate products cache after image upload
    if (updatedProducts.length > 0 && updatedProducts[0].website_id) {
      await CacheService.invalidateProducts(updatedProducts[0].website_id);
    }

    res.json({ product: updatedProducts[0] });
  } catch (error) {
    console.error('Error uploading product image:', error);
    if (req.file && fs.existsSync(req.file.path)) {
      fs.unlinkSync(req.file.path);
    }
    res.status(500).json({ error: 'Failed to upload product image', message: error.message });
  }
});

/**
 * PUT /api/products/:id
 * Update a product
 */
router.put('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const {
      name,
      name_ar,
      description,
      description_ar,
      price,
      original_price,
      category,
      category_ar,
      is_available
    } = req.body;

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
    if (original_price !== undefined) {
      // Allow null to clear the original_price
      if (original_price === null || original_price === '') {
        updateFields.push('original_price = NULL');
      } else {
        const parsedOriginalPrice = parseFloat(original_price);
        if (!isNaN(parsedOriginalPrice) && parsedOriginalPrice >= 0) {
          updateFields.push('original_price = ?');
          updateValues.push(parsedOriginalPrice);
        } else {
          // Invalid value, set to NULL
          updateFields.push('original_price = NULL');
        }
      }
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

    if (updateFields.length === 0) {
      return res.status(400).json({ error: 'No fields to update' });
    }

    updateValues.push(id);

    await pool.execute(
      `UPDATE products SET ${updateFields.join(', ')} WHERE id = ?`,
      updateValues
    );

    // Get website_id before update for cache invalidation
    const [oldProduct] = await pool.execute(
      'SELECT website_id FROM products WHERE id = ?',
      [id]
    );
    const websiteId = oldProduct[0]?.website_id;

    const [products] = await pool.execute(
      'SELECT * FROM products WHERE id = ?',
      [id]
    );

    if (products.length === 0) {
      return res.status(404).json({ error: 'Product not found' });
    }

    // Invalidate products cache after update
    if (websiteId) {
      await CacheService.invalidateProducts(websiteId);
    }

    res.json({ product: products[0] });
  } catch (error) {
    console.error('Error updating product:', error);
    console.error('Error stack:', error.stack);
    console.error('Request body:', req.body);
    console.error('Product ID:', req.params.id);
    console.error('SQL Error code:', error.code);
    console.error('SQL Error sqlMessage:', error.sqlMessage);
    
    // Check if the error is due to missing column
    if (error.code === 'ER_BAD_FIELD_ERROR' && error.sqlMessage && error.sqlMessage.includes('original_price')) {
      return res.status(500).json({ 
        error: 'Database column missing', 
        message: 'The original_price column does not exist. Please restart the backend server to run the migration.',
        sqlError: error.sqlMessage
      });
    }
    
    res.status(500).json({ 
      error: 'Failed to update product', 
      message: error.message,
      sqlError: error.sqlMessage,
      sqlCode: error.code
    });
  }
});

/**
 * DELETE /api/products/:id
 * Delete a product
 */
router.delete('/:id', async (req, res) => {
  try {
    const { id } = req.params;

    // Get product to delete image
    const [products] = await pool.execute(
      'SELECT * FROM products WHERE id = ?',
      [id]
    );

    if (products.length === 0) {
      return res.status(404).json({ error: 'Product not found' });
    }

    // Delete image file if exists
    if (products[0].image_path && fs.existsSync(products[0].image_path)) {
      try {
        fs.unlinkSync(products[0].image_path);
      } catch (err) {
        console.warn('Could not delete product image:', err.message);
      }
    }

    const websiteId = products[0].website_id;

    await pool.execute('DELETE FROM products WHERE id = ?', [id]);

    // Invalidate products cache after deletion
    if (websiteId) {
      await CacheService.invalidateProducts(websiteId);
    }

    res.json({ message: 'Product deleted successfully' });
  } catch (error) {
    console.error('Error deleting product:', error);
    res.status(500).json({ error: 'Failed to delete product', message: error.message });
  }
});

export default router;

