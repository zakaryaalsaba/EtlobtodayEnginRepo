import express from 'express';
import { pool } from '../db/init.js';

const router = express.Router({ mergeParams: true }); // mergeParams so :customerId is available

/**
 * GET /api/customers/:customerId/addresses
 * Get all addresses for a customer
 */
router.get('/', async (req, res) => {
  try {
    const customerId = req.params.customerId;

    const [customers] = await pool.execute(
      'SELECT id FROM customers WHERE id = ?',
      [customerId]
    );
    if (customers.length === 0) {
      return res.status(404).json({ error: 'Customer not found' });
    }

    const [addresses] = await pool.execute(
      `SELECT id, customer_id, area, region_id, region_name, area_id, area_name, zone_id, zone_name, zone_price, latitude, longitude, address_type, building_name,
              apartment_number, floor, street, phone_country_code, phone_number,
              additional_directions, address_label, is_default, created_at, updated_at
       FROM addresses
       WHERE customer_id = ?
       ORDER BY is_default DESC, created_at DESC`,
      [customerId]
    );

    res.json({ addresses });
  } catch (error) {
    console.error('Error fetching addresses:', error);
    res.status(500).json({
      error: 'Failed to fetch addresses',
      message: error.message
    });
  }
});

/**
 * GET /api/customers/:customerId/addresses/:addressId
 * Get a single address by id (must belong to customer)
 */
router.get('/:addressId', async (req, res) => {
  try {
    const { customerId, addressId } = req.params;

    const [rows] = await pool.execute(
      `SELECT id, customer_id, area, region_id, region_name, area_id, area_name, zone_id, zone_name, zone_price, latitude, longitude, address_type, building_name,
              apartment_number, floor, street, phone_country_code, phone_number,
              additional_directions, address_label, is_default, created_at, updated_at
       FROM addresses
       WHERE id = ? AND customer_id = ?`,
      [addressId, customerId]
    );

    if (rows.length === 0) {
      return res.status(404).json({ error: 'Address not found' });
    }

    res.json({ address: rows[0] });
  } catch (error) {
    console.error('Error fetching address:', error);
    res.status(500).json({
      error: 'Failed to fetch address',
      message: error.message
    });
  }
});

/**
 * POST /api/customers/:customerId/addresses
 * Create a new address for the customer
 * Body: area, latitude, longitude, address_type, building_name, apartment_number,
 *       floor, street, phone_country_code, phone_number, additional_directions, address_label, is_default
 */
router.post('/', async (req, res) => {
  try {
    const customerId = req.params.customerId;
    const {
      area,
      region_id,
      region_name,
      area_id,
      area_name,
      zone_id,
      zone_name,
      zone_price,
      latitude,
      longitude,
      address_type,
      building_name,
      apartment_number,
      floor,
      street,
      phone_country_code,
      phone_number,
      additional_directions,
      address_label,
      is_default
    } = req.body;

    const [customers] = await pool.execute(
      'SELECT id FROM customers WHERE id = ?',
      [customerId]
    );
    if (customers.length === 0) {
      return res.status(404).json({ error: 'Customer not found' });
    }

    const allowedTypes = ['apartment', 'house', 'office'];
    const type = address_type && allowedTypes.includes(address_type) ? address_type : 'apartment';

    if (is_default === true) {
      await pool.execute(
        'UPDATE addresses SET is_default = FALSE WHERE customer_id = ?',
        [customerId]
      );
    }

    const [result] = await pool.execute(
      `INSERT INTO addresses (
        customer_id, area, region_id, region_name, area_id, area_name, zone_id, zone_name, zone_price, latitude, longitude, address_type,
        building_name, apartment_number, floor, street,
        phone_country_code, phone_number, additional_directions, address_label, is_default
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        customerId,
        area || null,
        region_id || null,
        region_name || null,
        area_id || null,
        area_name || null,
        zone_id || null,
        zone_name || null,
        zone_price ?? null,
        latitude ?? null,
        longitude ?? null,
        type,
        building_name || null,
        apartment_number || null,
        floor || null,
        street || null,
        phone_country_code || null,
        phone_number || null,
        additional_directions || null,
        address_label || null,
        is_default === true ? 1 : 0
      ]
    );

    const [newAddress] = await pool.execute(
      'SELECT * FROM addresses WHERE id = ?',
      [result.insertId]
    );

    res.status(201).json({ address: newAddress[0] });
  } catch (error) {
    console.error('Error creating address:', error);
    res.status(500).json({
      error: 'Failed to create address',
      message: error.message
    });
  }
});

/**
 * PUT /api/customers/:customerId/addresses/:addressId
 * Update an address (must belong to customer)
 */
router.put('/:addressId', async (req, res) => {
  try {
    const { customerId, addressId } = req.params;
    const {
      area,
      region_id,
      region_name,
      area_id,
      area_name,
      zone_id,
      zone_name,
      zone_price,
      latitude,
      longitude,
      address_type,
      building_name,
      apartment_number,
      floor,
      street,
      phone_country_code,
      phone_number,
      additional_directions,
      address_label,
      is_default
    } = req.body;

    const [existing] = await pool.execute(
      'SELECT id FROM addresses WHERE id = ? AND customer_id = ?',
      [addressId, customerId]
    );
    if (existing.length === 0) {
      return res.status(404).json({ error: 'Address not found' });
    }

    if (is_default === true) {
      await pool.execute(
        'UPDATE addresses SET is_default = FALSE WHERE customer_id = ? AND id != ?',
        [customerId, addressId]
      );
    }

    const allowedTypes = ['apartment', 'house', 'office'];
    const updates = [];
    const values = [];

    if (area !== undefined) { updates.push('area = ?'); values.push(area); }
    if (region_id !== undefined) { updates.push('region_id = ?'); values.push(region_id); }
    if (region_name !== undefined) { updates.push('region_name = ?'); values.push(region_name); }
    if (area_id !== undefined) { updates.push('area_id = ?'); values.push(area_id); }
    if (area_name !== undefined) { updates.push('area_name = ?'); values.push(area_name); }
    if (zone_id !== undefined) { updates.push('zone_id = ?'); values.push(zone_id); }
    if (zone_name !== undefined) { updates.push('zone_name = ?'); values.push(zone_name); }
    if (zone_price !== undefined) { updates.push('zone_price = ?'); values.push(zone_price); }
    if (latitude !== undefined) { updates.push('latitude = ?'); values.push(latitude); }
    if (longitude !== undefined) { updates.push('longitude = ?'); values.push(longitude); }
    if (address_type !== undefined && allowedTypes.includes(address_type)) {
      updates.push('address_type = ?');
      values.push(address_type);
    }
    if (building_name !== undefined) { updates.push('building_name = ?'); values.push(building_name); }
    if (apartment_number !== undefined) { updates.push('apartment_number = ?'); values.push(apartment_number); }
    if (floor !== undefined) { updates.push('floor = ?'); values.push(floor); }
    if (street !== undefined) { updates.push('street = ?'); values.push(street); }
    if (phone_country_code !== undefined) { updates.push('phone_country_code = ?'); values.push(phone_country_code); }
    if (phone_number !== undefined) { updates.push('phone_number = ?'); values.push(phone_number); }
    if (additional_directions !== undefined) { updates.push('additional_directions = ?'); values.push(additional_directions); }
    if (address_label !== undefined) { updates.push('address_label = ?'); values.push(address_label); }
    if (is_default !== undefined) { updates.push('is_default = ?'); values.push(is_default ? 1 : 0); }

    if (updates.length === 0) {
      const [addr] = await pool.execute('SELECT * FROM addresses WHERE id = ? AND customer_id = ?', [addressId, customerId]);
      return res.json({ address: addr[0] });
    }

    updates.push('updated_at = CURRENT_TIMESTAMP');
    values.push(addressId);

    await pool.execute(
      `UPDATE addresses SET ${updates.join(', ')} WHERE id = ? AND customer_id = ?`,
      [...values, addressId, customerId]
    );

    const [updated] = await pool.execute(
      'SELECT * FROM addresses WHERE id = ? AND customer_id = ?',
      [addressId, customerId]
    );

    res.json({ address: updated[0] });
  } catch (error) {
    console.error('Error updating address:', error);
    res.status(500).json({
      error: 'Failed to update address',
      message: error.message
    });
  }
});

/**
 * DELETE /api/customers/:customerId/addresses/:addressId
 * Delete an address (must belong to customer)
 */
router.delete('/:addressId', async (req, res) => {
  try {
    const { customerId, addressId } = req.params;

    const [result] = await pool.execute(
      'DELETE FROM addresses WHERE id = ? AND customer_id = ?',
      [addressId, customerId]
    );

    if (result.affectedRows === 0) {
      return res.status(404).json({ error: 'Address not found' });
    }

    res.json({ success: true, message: 'Address deleted successfully' });
  } catch (error) {
    console.error('Error deleting address:', error);
    res.status(500).json({
      error: 'Failed to delete address',
      message: error.message
    });
  }
});

export default router;
