import express from 'express';
import { pool } from '../db/init.js';

const router = express.Router();

/**
 * POST /api/applications
 * Submit a restaurant application
 */
router.post('/', async (req, res) => {
  try {
    const {
      restaurant_name,
      owner_name,
      email,
      phone,
      address,
      cuisine_type,
      description,
      website_url
    } = req.body;

    // Validate required fields
    if (!restaurant_name || !owner_name || !email || !phone || !address || !cuisine_type || !description) {
      return res.status(400).json({ 
        error: 'Missing required fields: restaurant_name, owner_name, email, phone, address, cuisine_type, and description are required' 
      });
    }

    // Check if application already exists for this email or restaurant name
    const [existing] = await pool.execute(
      'SELECT id FROM restaurant_applications WHERE email = ? OR restaurant_name = ?',
      [email, restaurant_name]
    );

    if (existing.length > 0) {
      return res.status(400).json({ 
        error: 'An application with this email or restaurant name already exists. We\'ll review it and get back to you soon!' 
      });
    }

    // Insert application
    const [result] = await pool.execute(
      `INSERT INTO restaurant_applications 
       (restaurant_name, owner_name, email, phone, address, cuisine_type, description, website_url, status)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'pending')`,
      [restaurant_name, owner_name, email, phone, address, cuisine_type, description, website_url || null]
    );

    res.status(201).json({ 
      message: 'Application submitted successfully',
      application_id: result.insertId 
    });
  } catch (error) {
    console.error('Error submitting application:', error);
    res.status(500).json({ 
      error: 'Failed to submit application', 
      message: error.message 
    });
  }
});

/**
 * GET /api/applications
 * Get all applications (admin only - can add auth later)
 */
router.get('/', async (req, res) => {
  try {
    const [applications] = await pool.execute(
      'SELECT * FROM restaurant_applications ORDER BY created_at DESC'
    );

    res.json({ applications });
  } catch (error) {
    console.error('Error fetching applications:', error);
    res.status(500).json({ 
      error: 'Failed to fetch applications', 
      message: error.message 
    });
  }
});

/**
 * PUT /api/applications/:id/status
 * Update application status (admin only - can add auth later)
 */
router.put('/:id/status', async (req, res) => {
  try {
    const { id } = req.params;
    const { status } = req.body;

    if (!status || !['pending', 'approved', 'rejected'].includes(status)) {
      return res.status(400).json({ error: 'Invalid status. Must be pending, approved, or rejected' });
    }

    await pool.execute(
      'UPDATE restaurant_applications SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      [status, id]
    );

    res.json({ message: 'Application status updated successfully' });
  } catch (error) {
    console.error('Error updating application status:', error);
    res.status(500).json({ 
      error: 'Failed to update application status', 
      message: error.message 
    });
  }
});

export default router;

