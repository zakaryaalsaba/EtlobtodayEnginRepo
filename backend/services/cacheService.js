import redisClient from './redis.js';
import { pool } from '../db/init.js';

/**
 * Cache Service for Restaurants and Menus
 * Implements cache-aside pattern with automatic fallback to database
 */
export class CacheService {
  // Cache TTLs (in seconds)
  static TTL = {
    RESTAURANT: parseInt(process.env.REDIS_TTL_RESTAURANTS) || 3600,      // 1 hour
    PRODUCTS: parseInt(process.env.REDIS_TTL_PRODUCTS) || 1800,           // 30 minutes
    OFFERS: parseInt(process.env.REDIS_TTL_OFFERS) || 600,                 // 10 minutes
    BUSINESS_HOURS: parseInt(process.env.REDIS_TTL_BUSINESS_HOURS) || 86400, // 24 hours
    RESTAURANT_LIST: parseInt(process.env.REDIS_TTL_RESTAURANT_LIST) || 300  // 5 minutes
  };

  /**
   * Get restaurant by ID (with cache)
   * @param {number} websiteId - Restaurant website ID
   * @returns {Promise<Object|null>} Restaurant object or null
   */
  static async getRestaurant(websiteId) {
    const key = `restaurant:${websiteId}`;
    
    try {
      // Try cache first
      const cached = await redisClient.get(key);
      if (cached) {
        return JSON.parse(cached);
      }
      
      // Cache miss - fetch from DB
      const [rows] = await pool.execute(
        'SELECT * FROM restaurant_websites WHERE id = ?',
        [websiteId]
      );
      
      if (rows.length === 0) {
        return null;
      }
      
      const restaurant = rows[0];
      
      // Store in cache
      await redisClient.setEx(key, this.TTL.RESTAURANT, JSON.stringify(restaurant));
      
      return restaurant;
    } catch (error) {
      // Redis error - fallback to DB
      console.error('Cache error (getRestaurant), falling back to DB:', error.message);
      try {
        const [rows] = await pool.execute(
          'SELECT * FROM restaurant_websites WHERE id = ?',
          [websiteId]
        );
        return rows.length > 0 ? rows[0] : null;
      } catch (dbError) {
        console.error('DB fallback error:', dbError);
        throw dbError;
      }
    }
  }

  /**
   * Get all products for a restaurant (with cache)
   * @param {number} websiteId - Restaurant website ID
   * @returns {Promise<Array>} Array of products
   */
  static async getProducts(websiteId) {
    const key = `restaurant:${websiteId}:products`;
    
    try {
      const cached = await redisClient.get(key);
      if (cached) {
        return JSON.parse(cached);
      }
      
      const [products] = await pool.execute(
        'SELECT * FROM products WHERE website_id = ? AND is_available = 1 ORDER BY category, name',
        [websiteId]
      );
      
      await redisClient.setEx(key, this.TTL.PRODUCTS, JSON.stringify(products));
      
      return products;
    } catch (error) {
      console.error('Cache error (getProducts), falling back to DB:', error.message);
      try {
        const [products] = await pool.execute(
          'SELECT * FROM products WHERE website_id = ? AND is_available = 1 ORDER BY category, name',
          [websiteId]
        );
        return products;
      } catch (dbError) {
        console.error('DB fallback error:', dbError);
        throw dbError;
      }
    }
  }

  /**
   * Get active offers for a restaurant (with cache)
   * @param {number} websiteId - Restaurant website ID
   * @returns {Promise<Array>} Array of active offers
   */
  static async getOffers(websiteId) {
    const key = `restaurant:${websiteId}:offers`;
    const today = new Date().toISOString().slice(0, 10);
    
    try {
      const cached = await redisClient.get(key);
      if (cached) {
        return JSON.parse(cached);
      }
      
      const [offers] = await pool.execute(
        `SELECT id, website_id, offer_type, title, description, value, min_order_value,
                valid_from, valid_until, display_order,
                offer_scope, selected_product_ids, selected_addon_ids
         FROM offers
         WHERE website_id = ? AND is_active = 1
           AND valid_from <= ? AND valid_until >= ?
         ORDER BY display_order ASC, created_at ASC`,
        [websiteId, today, today]
      );
      
      await redisClient.setEx(key, this.TTL.OFFERS, JSON.stringify(offers));
      
      return offers;
    } catch (error) {
      console.error('Cache error (getOffers), falling back to DB:', error.message);
      try {
        const [offers] = await pool.execute(
          `SELECT id, website_id, offer_type, title, description, value, min_order_value,
                  valid_from, valid_until, display_order,
                  offer_scope, selected_product_ids, selected_addon_ids
           FROM offers
           WHERE website_id = ? AND is_active = 1
             AND valid_from <= ? AND valid_until >= ?
           ORDER BY display_order ASC, created_at ASC`,
          [websiteId, today, today]
        );
        return offers;
      } catch (dbError) {
        console.error('DB fallback error:', dbError);
        throw dbError;
      }
    }
  }

  /**
   * Get all restaurants (with cache)
   * @param {boolean} openNow - Filter to only open restaurants
   * @returns {Promise<Array>} Array of restaurants
   */
  static async getAllRestaurants(openNow = false) {
    const key = openNow ? 'restaurants:list:open_now' : 'restaurants:list:all';
    
    try {
      const cached = await redisClient.get(key);
      if (cached) {
        return JSON.parse(cached);
      }
      
      let query = 'SELECT * FROM restaurant_websites WHERE is_published = 1';
      let params = [];
      
      if (openNow) {
        query = `
          SELECT rw.* FROM restaurant_websites rw
          INNER JOIN business_hours bh ON bh.website_id = rw.id
          WHERE rw.is_published = 1
            AND bh.day_of_week = (DAYOFWEEK(NOW()) - 1)
            AND bh.is_closed = 0
            AND bh.open_time IS NOT NULL
            AND bh.close_time IS NOT NULL
            AND CURTIME() >= bh.open_time
            AND CURTIME() <= bh.close_time
          ORDER BY rw.created_at DESC
        `;
      } else {
        query += ' ORDER BY created_at DESC';
      }
      
      const [restaurants] = await pool.execute(query, params);
      
      await redisClient.setEx(key, this.TTL.RESTAURANT_LIST, JSON.stringify(restaurants));
      
      return restaurants;
    } catch (error) {
      console.error('Cache error (getAllRestaurants), falling back to DB:', error.message);
      try {
        let query = 'SELECT * FROM restaurant_websites WHERE is_published = 1';
        if (openNow) {
          query = `
            SELECT rw.* FROM restaurant_websites rw
            INNER JOIN business_hours bh ON bh.website_id = rw.id
            WHERE rw.is_published = 1
              AND bh.day_of_week = (DAYOFWEEK(NOW()) - 1)
              AND bh.is_closed = 0
              AND CURTIME() >= bh.open_time
              AND CURTIME() <= bh.close_time
            ORDER BY rw.created_at DESC
          `;
        } else {
          query += ' ORDER BY created_at DESC';
        }
        const [restaurants] = await pool.execute(query);
        return restaurants;
      } catch (dbError) {
        console.error('DB fallback error:', dbError);
        throw dbError;
      }
    }
  }

  /**
   * Get all restaurant websites including unpublished (e.g. for admin dropdowns).
   * @returns {Promise<Array>} Array of all restaurants
   */
  static async getAllRestaurantsIncludeUnpublished() {
    const key = 'restaurants:list:all_including_unpublished';
    try {
      const cached = await redisClient.get(key);
      if (cached) {
        return JSON.parse(cached);
      }
      const [restaurants] = await pool.execute(
        'SELECT * FROM restaurant_websites ORDER BY created_at DESC'
      );
      await redisClient.setEx(key, this.TTL.RESTAURANT_LIST, JSON.stringify(restaurants));
      return restaurants;
    } catch (error) {
      console.error('Cache error (getAllRestaurantsIncludeUnpublished), falling back to DB:', error.message);
      const [restaurants] = await pool.execute(
        'SELECT * FROM restaurant_websites ORDER BY created_at DESC'
      );
      return restaurants;
    }
  }

  /**
   * Get business hours for a restaurant (with cache)
   * @param {number} websiteId - Restaurant website ID
   * @returns {Promise<Array>} Array of business hours
   */
  static async getBusinessHours(websiteId) {
    const key = `restaurant:${websiteId}:business_hours`;
    
    try {
      const cached = await redisClient.get(key);
      if (cached) {
        return JSON.parse(cached);
      }
      
      const [hours] = await pool.execute(
        'SELECT * FROM business_hours WHERE website_id = ? ORDER BY day_of_week',
        [websiteId]
      );
      
      await redisClient.setEx(key, this.TTL.BUSINESS_HOURS, JSON.stringify(hours));
      
      return hours;
    } catch (error) {
      console.error('Cache error (getBusinessHours), falling back to DB:', error.message);
      try {
        const [hours] = await pool.execute(
          'SELECT * FROM business_hours WHERE website_id = ? ORDER BY day_of_week',
          [websiteId]
        );
        return hours;
      } catch (dbError) {
        console.error('DB fallback error:', dbError);
        throw dbError;
      }
    }
  }

  /**
   * Invalidate all cache for a restaurant
   * Call this when restaurant data is updated
   * @param {number} websiteId - Restaurant website ID
   */
  static async invalidateRestaurant(websiteId) {
    const keys = [
      `restaurant:${websiteId}`,
      `restaurant:${websiteId}:products`,
      `restaurant:${websiteId}:offers`,
      `restaurant:${websiteId}:business_hours`
    ];
    
    try {
      await Promise.all(keys.map(key => redisClient.del(key)));
      // Also invalidate list caches
      await redisClient.del('restaurants:list:all');
      await redisClient.del('restaurants:list:open_now');
    } catch (error) {
      console.error('Cache invalidation error:', error.message);
      // Don't throw - invalidation failure shouldn't break the app
    }
  }

  /**
   * Invalidate products cache for a restaurant
   * Call this when products are created/updated/deleted
   * @param {number} websiteId - Restaurant website ID
   */
  static async invalidateProducts(websiteId) {
    try {
      await redisClient.del(`restaurant:${websiteId}:products`);
    } catch (error) {
      console.error('Cache invalidation error:', error.message);
    }
  }

  /**
   * Invalidate offers cache for a restaurant
   * Call this when offers are created/updated/deleted
   * @param {number} websiteId - Restaurant website ID
   */
  static async invalidateOffers(websiteId) {
    try {
      await redisClient.del(`restaurant:${websiteId}:offers`);
    } catch (error) {
      console.error('Cache invalidation error:', error.message);
    }
  }

  /**
   * Invalidate business hours cache for a restaurant
   * @param {number} websiteId - Restaurant website ID
   */
  static async invalidateBusinessHours(websiteId) {
    try {
      await redisClient.del(`restaurant:${websiteId}:business_hours`);
      await redisClient.del('restaurants:list:open_now'); // Also invalidate open_now list
    } catch (error) {
      console.error('Cache invalidation error:', error.message);
    }
  }

  /**
   * Invalidate all restaurant list caches
   * Call this when a restaurant is published/unpublished
   */
  static async invalidateRestaurantLists() {
    try {
      await redisClient.del('restaurants:list:all');
      await redisClient.del('restaurants:list:open_now');
    } catch (error) {
      console.error('Cache invalidation error:', error.message);
    }
  }

  /**
   * Clear all cache (use with caution - mainly for testing)
   */
  static async clearAllCache() {
    try {
      await redisClient.flushDb();
      console.log('All cache cleared');
    } catch (error) {
      console.error('Cache clear error:', error.message);
    }
  }
}

export default CacheService;
