import express from 'express';
import bcrypt from 'bcryptjs';
import { pool } from '../db/init.js';
import multer from 'multer';
import path from 'path';
import { fileURLToPath } from 'url';
import fs from 'fs';
import { verifySuperAdminToken } from './superAdmin.js';

const router = express.Router();
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Configure multer for profile image uploads
const profileImageStorage = multer.diskStorage({
  destination: (req, file, cb) => {
    const uploadDir = path.join(__dirname, '../uploads/delivery-companies');
    if (!fs.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    cb(null, 'profile-' + uniqueSuffix + path.extname(file.originalname));
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

const uploadProfileImage = multer({
  storage: profileImageStorage,
  limits: { fileSize: 5 * 1024 * 1024 }, // 5MB limit
  fileFilter: imageFilter
});

/**
 * GET /api/delivery-companies
 * Get all delivery companies
 */
router.get('/', verifySuperAdminToken, async (req, res) => {
  try {
    const [companies] = await pool.execute(
      'SELECT * FROM delivery_companies ORDER BY created_at DESC'
    );
    
    // Parse emails JSON and omit password hash
    const companiesWithParsedEmails = companies.map(company => {
      const { admin_password_hash, ...rest } = company;
      if (rest.emails) {
        try {
          rest.emails = typeof rest.emails === 'string' ? JSON.parse(rest.emails) : rest.emails;
        } catch (e) {
          rest.emails = [];
        }
      } else {
        rest.emails = [];
      }
      return rest;
    });
    
    res.json({ companies: companiesWithParsedEmails });
  } catch (error) {
    console.error('Error fetching delivery companies:', error);
    res.status(500).json({ error: 'Failed to fetch delivery companies', message: error.message });
  }
});

/**
 * GET /api/delivery-companies/:id
 * Get a single delivery company by ID
 */
router.get('/:id', verifySuperAdminToken, async (req, res) => {
  try {
    const { id } = req.params;
    const [companies] = await pool.execute(
      'SELECT * FROM delivery_companies WHERE id = ?',
      [id]
    );
    
    if (companies.length === 0) {
      return res.status(404).json({ error: 'Delivery company not found' });
    }
    
    const { admin_password_hash, ...company } = companies[0];
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
    console.error('Error fetching delivery company:', error);
    res.status(500).json({ error: 'Failed to fetch delivery company', message: error.message });
  }
});

/**
 * POST /api/delivery-companies
 * Create a new delivery company
 */
router.post('/', verifySuperAdminToken, uploadProfileImage.single('profile_image'), async (req, res) => {
  try {
    const {
      company_name,
      contact_name,
      phone,
      address,
      emails,
      website,
      status,
      notes,
      admin_username,
      admin_password
    } = req.body;
    
    // Validate required fields
    if (!company_name || !contact_name) {
      return res.status(400).json({ error: 'Company name and contact name are required' });
    }
    
    // Parse emails if it's a string
    let emailsArray = [];
    if (emails) {
      try {
        emailsArray = typeof emails === 'string' ? JSON.parse(emails) : emails;
        if (!Array.isArray(emailsArray)) {
          emailsArray = [emailsArray];
        }
      } catch (e) {
        emailsArray = emails.split(',').map(email => email.trim()).filter(email => email);
      }
    }
    
    // Handle profile image upload
    let profileImageUrl = null;
    let profileImagePath = null;
    if (req.file) {
      profileImagePath = `delivery-companies/${req.file.filename}`;
      profileImageUrl = `/uploads/${profileImagePath}`;
    }

    // Admin credentials: username must be unique; password hashed
    let adminPasswordHash = null;
    const usernameTrimmed = admin_username ? String(admin_username).trim() : null;
    if (usernameTrimmed) {
      const [existing] = await pool.execute(
        'SELECT id FROM delivery_companies WHERE admin_username = ?',
        [usernameTrimmed]
      );
      if (existing.length > 0) {
        return res.status(400).json({ error: 'This admin username is already in use' });
      }
      if (admin_password && admin_password.length >= 6) {
        adminPasswordHash = await bcrypt.hash(admin_password, 10);
      }
    }
    
    // Insert into database
    const [result] = await pool.execute(
      `INSERT INTO delivery_companies 
       (company_name, contact_name, phone, address, emails, website, status, notes, profile_image_url, profile_image_path, admin_username, admin_password_hash)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        company_name,
        contact_name,
        phone || null,
        address || null,
        JSON.stringify(emailsArray),
        website || null,
        status || 'active',
        notes || null,
        profileImageUrl,
        profileImagePath,
        usernameTrimmed,
        adminPasswordHash
      ]
    );
    
    // Fetch the created company
    const [companies] = await pool.execute(
      'SELECT * FROM delivery_companies WHERE id = ?',
      [result.insertId]
    );
    
    const company = companies[0];
    if (company.emails) {
      try {
        company.emails = typeof company.emails === 'string' 
          ? JSON.parse(company.emails) 
          : company.emails;
      } catch (e) {
        company.emails = [];
      }
    } else {
      company.emails = [];
    }
    
    res.status(201).json({ company });
  } catch (error) {
    console.error('Error creating delivery company:', error);
    res.status(500).json({ error: 'Failed to create delivery company', message: error.message });
  }
});

/**
 * PUT /api/delivery-companies/:id
 * Update a delivery company
 */
router.put('/:id', verifySuperAdminToken, uploadProfileImage.single('profile_image'), async (req, res) => {
  try {
    const { id } = req.params;
    const {
      company_name,
      contact_name,
      phone,
      address,
      emails,
      website,
      status,
      notes,
      remove_profile_image,
      admin_username,
      admin_password
    } = req.body;
    
    // Check if company exists
    const [existing] = await pool.execute(
      'SELECT * FROM delivery_companies WHERE id = ?',
      [id]
    );
    
    if (existing.length === 0) {
      return res.status(404).json({ error: 'Delivery company not found' });
    }
    
    const existingCompany = existing[0];
    
    // Parse emails if provided
    let emailsArray = existingCompany.emails ? 
      (typeof existingCompany.emails === 'string' ? JSON.parse(existingCompany.emails) : existingCompany.emails) : 
      [];
    
    if (emails !== undefined) {
      try {
        emailsArray = typeof emails === 'string' ? JSON.parse(emails) : emails;
        if (!Array.isArray(emailsArray)) {
          emailsArray = [emailsArray];
        }
      } catch (e) {
        emailsArray = emails.split(',').map(email => email.trim()).filter(email => email);
      }
    }
    
    // Admin credentials update
    let adminUsername = existingCompany.admin_username;
    let adminPasswordHash = existingCompany.admin_password_hash;
    if (admin_username !== undefined) {
      const usernameTrimmed = admin_username ? String(admin_username).trim() : null;
      if (usernameTrimmed) {
        const [existingUser] = await pool.execute(
          'SELECT id FROM delivery_companies WHERE admin_username = ? AND id != ?',
          [usernameTrimmed, id]
        );
        if (existingUser.length > 0) {
          return res.status(400).json({ error: 'This admin username is already in use' });
        }
        adminUsername = usernameTrimmed;
      } else {
        adminUsername = null;
      }
    }
    const setNewPassword = admin_password !== undefined && admin_password && admin_password.length >= 6;
    if (setNewPassword) {
      adminPasswordHash = await bcrypt.hash(admin_password, 10);
    }

    // Handle profile image
    let profileImageUrl = existingCompany.profile_image_url;
    let profileImagePath = existingCompany.profile_image_path;
    
    if (remove_profile_image === 'true' || remove_profile_image === true) {
      // Delete old image file if exists
      if (existingCompany.profile_image_path) {
        const oldImagePath = path.join(__dirname, '../uploads', existingCompany.profile_image_path);
        if (fs.existsSync(oldImagePath)) {
          fs.unlinkSync(oldImagePath);
        }
      }
      profileImageUrl = null;
      profileImagePath = null;
    } else if (req.file) {
      // Delete old image file if exists
      if (existingCompany.profile_image_path) {
        const oldImagePath = path.join(__dirname, '../uploads', existingCompany.profile_image_path);
        if (fs.existsSync(oldImagePath)) {
          fs.unlinkSync(oldImagePath);
        }
      }
      profileImagePath = `delivery-companies/${req.file.filename}`;
      profileImageUrl = `/uploads/${profileImagePath}`;
    }
    
    // Update database (include admin_username and admin_password_hash only when we have them)
    await pool.execute(
      `UPDATE delivery_companies SET
       company_name = ?,
       contact_name = ?,
       phone = ?,
       address = ?,
       emails = ?,
       website = ?,
       status = ?,
       notes = ?,
       profile_image_url = ?,
       profile_image_path = ?,
       admin_username = ?,
       admin_password_hash = COALESCE(?, admin_password_hash),
       updated_at = CURRENT_TIMESTAMP
       WHERE id = ?`,
      [
        company_name !== undefined ? company_name : existingCompany.company_name,
        contact_name !== undefined ? contact_name : existingCompany.contact_name,
        phone !== undefined ? phone : existingCompany.phone,
        address !== undefined ? address : existingCompany.address,
        JSON.stringify(emailsArray),
        website !== undefined ? website : existingCompany.website,
        status !== undefined ? status : existingCompany.status,
        notes !== undefined ? notes : existingCompany.notes,
        profileImageUrl,
        profileImagePath,
        adminUsername,
        setNewPassword ? adminPasswordHash : null,
        id
      ]
    );
    
    // Fetch updated company
    const [companies] = await pool.execute(
      'SELECT * FROM delivery_companies WHERE id = ?',
      [id]
    );
    
    const company = companies[0];
    if (company.emails) {
      try {
        company.emails = typeof company.emails === 'string' 
          ? JSON.parse(company.emails) 
          : company.emails;
      } catch (e) {
        company.emails = [];
      }
    } else {
      company.emails = [];
    }
    
    res.json({ company });
  } catch (error) {
    console.error('Error updating delivery company:', error);
    res.status(500).json({ error: 'Failed to update delivery company', message: error.message });
  }
});

/**
 * DELETE /api/delivery-companies/:id
 * Delete a delivery company
 */
router.delete('/:id', verifySuperAdminToken, async (req, res) => {
  try {
    const { id } = req.params;
    
    // Check if company exists
    const [existing] = await pool.execute(
      'SELECT * FROM delivery_companies WHERE id = ?',
      [id]
    );
    
    if (existing.length === 0) {
      return res.status(404).json({ error: 'Delivery company not found' });
    }
    
    const company = existing[0];
    
    // Delete profile image file if exists
    if (company.profile_image_path) {
      const imagePath = path.join(__dirname, '../uploads', company.profile_image_path);
      if (fs.existsSync(imagePath)) {
        fs.unlinkSync(imagePath);
      }
    }
    
    // Delete from database
    await pool.execute(
      'DELETE FROM delivery_companies WHERE id = ?',
      [id]
    );
    
    res.json({ message: 'Delivery company deleted successfully' });
  } catch (error) {
    console.error('Error deleting delivery company:', error);
    res.status(500).json({ error: 'Failed to delete delivery company', message: error.message });
  }
});

export default router;
