# Redis Caching Strategy for Restaurants and Menus

## Overview
Redis caching dramatically improves performance for frequently accessed, read-heavy data like restaurant listings, menus/products, and offers. This guide outlines the best practices for implementing Redis in your restaurant backend.

## Why Redis for Restaurants & Menus?

### Benefits:
1. **Performance**: Reduce MySQL queries by 80-95% for frequently accessed data
2. **Scalability**: Handle high traffic (customer app browsing menus) without DB overload
3. **Cost**: Lower database load = lower hosting costs
4. **User Experience**: Faster page loads = better conversion rates

### What to Cache:
- ✅ **Restaurant websites** (frequently browsed)
- ✅ **Products/menus** (most accessed data)
- ✅ **Offers** (active promotions)
- ✅ **Business hours** (checked on every visit)
- ❌ **Orders** (real-time, write-heavy - use Firebase)
- ❌ **User sessions** (use JWT tokens)

---

## 1. Installation & Setup

### Install Redis Client:
```bash
npm install redis
```

### Create Redis Service (`backend/services/redis.js`):
```javascript
import { createClient } from 'redis';

const redisClient = createClient({
  url: process.env.REDIS_URL || 'redis://localhost:6379',
  socket: {
    reconnectStrategy: (retries) => {
      if (retries > 10) {
        console.error('Redis: Max reconnection attempts reached');
        return new Error('Redis connection failed');
      }
      return Math.min(retries * 100, 3000);
    }
  }
});

redisClient.on('error', (err) => console.error('Redis Client Error', err));
redisClient.on('connect', () => console.log('Redis Client Connected'));

await redisClient.connect();

export default redisClient;
```

### Environment Variables (`.env`):
```env
REDIS_URL=redis://localhost:6379
REDIS_TTL_RESTAURANTS=3600      # 1 hour
REDIS_TTL_PRODUCTS=1800         # 30 minutes
REDIS_TTL_OFFERS=600            # 10 minutes
REDIS_TTL_BUSINESS_HOURS=86400  # 24 hours
```

---

## 2. Cache Key Strategy

### Key Naming Convention:
```
{entity}:{id}:{variant}
```

Examples:
- `restaurant:1` - Full restaurant data
- `restaurant:1:products` - All products for restaurant 1
- `restaurant:1:offers` - Active offers for restaurant 1
- `restaurant:1:business_hours` - Business hours
- `restaurants:list:open_now` - List of open restaurants
- `restaurants:list:all` - All published restaurants

---

## 3. Implementation Patterns

### Pattern 1: Cache-Aside (Lazy Loading)

**Best for**: Products, Restaurants, Offers

```javascript
// backend/services/cacheService.js
import redisClient from './redis.js';

export class CacheService {
  /**
   * Get restaurant with cache
   */
  static async getRestaurant(websiteId) {
    const cacheKey = `restaurant:${websiteId}`;
    
    // Try cache first
    const cached = await redisClient.get(cacheKey);
    if (cached) {
      return JSON.parse(cached);
    }
    
    // Cache miss - fetch from DB
    const [rows] = await pool.execute(
      'SELECT * FROM restaurant_websites WHERE id = ?',
      [websiteId]
    );
    
    if (rows.length === 0) return null;
    
    const restaurant = rows[0];
    
    // Store in cache (TTL: 1 hour)
    await redisClient.setEx(
      cacheKey,
      process.env.REDIS_TTL_RESTAURANTS || 3600,
      JSON.stringify(restaurant)
    );
    
    return restaurant;
  }

  /**
   * Get products for a restaurant with cache
   */
  static async getProducts(websiteId) {
    const cacheKey = `restaurant:${websiteId}:products`;
    
    const cached = await redisClient.get(cacheKey);
    if (cached) {
      return JSON.parse(cached);
    }
    
    const [products] = await pool.execute(
      'SELECT * FROM products WHERE website_id = ? AND is_available = 1 ORDER BY category, name',
      [websiteId]
    );
    
    // Cache for 30 minutes (menus change less frequently)
    await redisClient.setEx(
      cacheKey,
      process.env.REDIS_TTL_PRODUCTS || 1800,
      JSON.stringify(products)
    );
    
    return products;
  }

  /**
   * Get active offers with cache
   */
  static async getOffers(websiteId) {
    const cacheKey = `restaurant:${websiteId}:offers`;
    const today = new Date().toISOString().slice(0, 10);
    
    const cached = await redisClient.get(cacheKey);
    if (cached) {
      return JSON.parse(cached);
    }
    
    const [offers] = await pool.execute(
      `SELECT * FROM offers 
       WHERE website_id = ? AND is_active = 1 
         AND valid_from <= ? AND valid_until >= ?
       ORDER BY display_order ASC`,
      [websiteId, today, today]
    );
    
    // Cache for 10 minutes (offers change more frequently)
    await redisClient.setEx(
      cacheKey,
      process.env.REDIS_TTL_OFFERS || 600,
      JSON.stringify(offers)
    );
    
    return offers;
  }

  /**
   * Invalidate cache when data changes
   */
  static async invalidateRestaurant(websiteId) {
    const keys = [
      `restaurant:${websiteId}`,
      `restaurant:${websiteId}:products`,
      `restaurant:${websiteId}:offers`,
      `restaurant:${websiteId}:business_hours`,
      'restaurants:list:all',
      'restaurants:list:open_now'
    ];
    
    await Promise.all(keys.map(key => redisClient.del(key)));
  }

  /**
   * Invalidate products cache
   */
  static async invalidateProducts(websiteId) {
    await redisClient.del(`restaurant:${websiteId}:products`);
  }
}
```

---

## 4. Updated Route Handlers

### Example: `routes/websites.js`

```javascript
import CacheService from '../services/cacheService.js';

/**
 * GET /api/websites/:id
 * Get restaurant website (with Redis cache)
 */
router.get('/:id', async (req, res) => {
  try {
    const websiteId = parseInt(req.params.id);
    
    // Use cache service
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
 * GET /api/websites/:id/offers
 * Get active offers (with Redis cache)
 */
router.get('/:id/offers', async (req, res) => {
  try {
    const websiteId = parseInt(req.params.id);
    const offers = await CacheService.getOffers(websiteId);
    res.json({ offers });
  } catch (error) {
    console.error('Error fetching offers:', error);
    res.status(500).json({ error: 'Failed to fetch offers', message: error.message });
  }
});
```

### Example: `routes/products.js`

```javascript
import CacheService from '../services/cacheService.js';

/**
 * GET /api/products/website/:websiteId
 * Get all products (with Redis cache)
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
 * POST /api/products
 * Create product (invalidate cache)
 */
router.post('/', async (req, res) => {
  try {
    // ... existing product creation logic ...
    
    // Invalidate cache after creation
    await CacheService.invalidateProducts(website_id);
    
    res.status(201).json({ product: products[0] });
  } catch (error) {
    // ... error handling ...
  }
});

/**
 * PUT /api/products/:id
 * Update product (invalidate cache)
 */
router.put('/:id', async (req, res) => {
  try {
    // ... existing update logic ...
    
    // Get website_id before update
    const [oldProduct] = await pool.execute('SELECT website_id FROM products WHERE id = ?', [id]);
    const websiteId = oldProduct[0]?.website_id;
    
    // ... perform update ...
    
    // Invalidate cache
    if (websiteId) {
      await CacheService.invalidateProducts(websiteId);
    }
    
    res.json({ product: products[0] });
  } catch (error) {
    // ... error handling ...
  }
});

/**
 * DELETE /api/products/:id
 * Delete product (invalidate cache)
 */
router.delete('/:id', async (req, res) => {
  try {
    const [products] = await pool.execute('SELECT website_id FROM products WHERE id = ?', [id]);
    const websiteId = products[0]?.website_id;
    
    // ... delete logic ...
    
    // Invalidate cache
    if (websiteId) {
      await CacheService.invalidateProducts(websiteId);
    }
    
    res.json({ message: 'Product deleted successfully' });
  } catch (error) {
    // ... error handling ...
  }
});
```

---

## 5. Advanced Patterns

### Pattern 2: Write-Through (Immediate Cache Update)

**Best for**: Frequently updated data

```javascript
/**
 * Update restaurant and cache simultaneously
 */
static async updateRestaurant(websiteId, data) {
  // Update DB
  await pool.execute('UPDATE restaurant_websites SET ... WHERE id = ?', [websiteId]);
  
  // Update cache immediately
  const [updated] = await pool.execute('SELECT * FROM restaurant_websites WHERE id = ?', [websiteId]);
  await redisClient.setEx(
    `restaurant:${websiteId}`,
    process.env.REDIS_TTL_RESTAURANTS || 3600,
    JSON.stringify(updated[0])
  );
  
  return updated[0];
}
```

### Pattern 3: Cache Lists (Multiple Restaurants)

**Best for**: Restaurant listings

```javascript
/**
 * Get all restaurants (cached list)
 */
static async getAllRestaurants(openNow = false) {
  const cacheKey = openNow ? 'restaurants:list:open_now' : 'restaurants:list:all';
  
  const cached = await redisClient.get(cacheKey);
  if (cached) {
    return JSON.parse(cached);
  }
  
  let query = 'SELECT * FROM restaurant_websites WHERE is_published = 1';
  if (openNow) {
    query += ` AND id IN (
      SELECT DISTINCT rw.id FROM restaurant_websites rw
      INNER JOIN business_hours bh ON bh.website_id = rw.id
      WHERE bh.day_of_week = (DAYOFWEEK(NOW()) - 1)
        AND bh.is_closed = 0
        AND CURTIME() >= bh.open_time
        AND CURTIME() <= bh.close_time
    )`;
  }
  query += ' ORDER BY created_at DESC';
  
  const [restaurants] = await pool.execute(query);
  
  // Cache for 5 minutes (lists change more frequently)
  await redisClient.setEx(cacheKey, 300, JSON.stringify(restaurants));
  
  return restaurants;
}
```

---

## 6. Cache Invalidation Strategy

### When to Invalidate:

| Action | Invalidate Keys |
|--------|----------------|
| Update restaurant | `restaurant:{id}`, `restaurants:list:*` |
| Create/Update/Delete product | `restaurant:{id}:products` |
| Create/Update/Delete offer | `restaurant:{id}:offers` |
| Update business hours | `restaurant:{id}:business_hours`, `restaurants:list:open_now` |
| Publish/Unpublish restaurant | `restaurant:{id}`, `restaurants:list:*` |

### Implementation:

```javascript
// In routes/websites.js - PUT /api/websites/:id
router.put('/:id', async (req, res) => {
  try {
    // ... update logic ...
    
    // Invalidate all restaurant-related cache
    await CacheService.invalidateRestaurant(websiteId);
    
    res.json({ website: updated[0] });
  } catch (error) {
    // ...
  }
});
```

---

## 7. Best Practices

### ✅ DO:
1. **Set appropriate TTLs**:
   - Restaurants: 1 hour (change infrequently)
   - Products: 30 minutes (moderate changes)
   - Offers: 10 minutes (change more often)
   - Business hours: 24 hours (rarely change)

2. **Cache serialized JSON** (faster than individual fields)

3. **Handle cache misses gracefully** (fallback to DB)

4. **Invalidate on writes** (keep data fresh)

5. **Use Redis pipelining** for bulk operations

6. **Monitor cache hit rate** (aim for >80%)

### ❌ DON'T:
1. **Don't cache user-specific data** (use sessions/JWT)
2. **Don't cache orders** (use Firebase for real-time)
3. **Don't set infinite TTL** (always expire)
4. **Don't cache sensitive data** (passwords, tokens)
5. **Don't ignore cache errors** (fallback to DB)

---

## 8. Performance Monitoring

### Add Cache Metrics:

```javascript
// Track cache hits/misses
let cacheStats = { hits: 0, misses: 0 };

static async getRestaurant(websiteId) {
  const cached = await redisClient.get(`restaurant:${websiteId}`);
  if (cached) {
    cacheStats.hits++;
    return JSON.parse(cached);
  }
  
  cacheStats.misses++;
  // ... fetch from DB ...
}

// Expose stats endpoint
router.get('/cache/stats', (req, res) => {
  const hitRate = cacheStats.hits / (cacheStats.hits + cacheStats.misses) * 100;
  res.json({ ...cacheStats, hitRate: hitRate.toFixed(2) + '%' });
});
```

---

## 9. Migration Strategy

### Phase 1: Read-Through Cache (Non-Breaking)
1. Add Redis service
2. Update GET endpoints to use cache
3. Keep DB as source of truth
4. Monitor performance

### Phase 2: Write-Through (Optimize Writes)
1. Update cache on writes
2. Add invalidation logic
3. Monitor cache consistency

### Phase 3: Advanced Patterns
1. Cache lists (all restaurants)
2. Cache computed data (open_now)
3. Add cache warming on startup

---

## 10. Example: Complete Implementation

### `backend/services/cacheService.js` (Full Implementation):

```javascript
import redisClient from './redis.js';
import { pool } from '../db/init.js';

export class CacheService {
  // Restaurant cache
  static async getRestaurant(websiteId) {
    const key = `restaurant:${websiteId}`;
    try {
      const cached = await redisClient.get(key);
      if (cached) return JSON.parse(cached);
      
      const [rows] = await pool.execute(
        'SELECT * FROM restaurant_websites WHERE id = ?',
        [websiteId]
      );
      
      if (rows.length === 0) return null;
      
      await redisClient.setEx(key, 3600, JSON.stringify(rows[0]));
      return rows[0];
    } catch (error) {
      console.error('Cache error, falling back to DB:', error);
      const [rows] = await pool.execute(
        'SELECT * FROM restaurant_websites WHERE id = ?',
        [websiteId]
      );
      return rows.length > 0 ? rows[0] : null;
    }
  }

  // Products cache
  static async getProducts(websiteId) {
    const key = `restaurant:${websiteId}:products`;
    try {
      const cached = await redisClient.get(key);
      if (cached) return JSON.parse(cached);
      
      const [products] = await pool.execute(
        'SELECT * FROM products WHERE website_id = ? AND is_available = 1 ORDER BY category, name',
        [websiteId]
      );
      
      await redisClient.setEx(key, 1800, JSON.stringify(products));
      return products;
    } catch (error) {
      console.error('Cache error, falling back to DB:', error);
      const [products] = await pool.execute(
        'SELECT * FROM products WHERE website_id = ? AND is_available = 1 ORDER BY category, name',
        [websiteId]
      );
      return products;
    }
  }

  // Offers cache
  static async getOffers(websiteId) {
    const key = `restaurant:${websiteId}:offers`;
    const today = new Date().toISOString().slice(0, 10);
    
    try {
      const cached = await redisClient.get(key);
      if (cached) return JSON.parse(cached);
      
      const [offers] = await pool.execute(
        `SELECT * FROM offers 
         WHERE website_id = ? AND is_active = 1 
           AND valid_from <= ? AND valid_until >= ?
         ORDER BY display_order ASC`,
        [websiteId, today, today]
      );
      
      await redisClient.setEx(key, 600, JSON.stringify(offers));
      return offers;
    } catch (error) {
      console.error('Cache error, falling back to DB:', error);
      const [offers] = await pool.execute(
        `SELECT * FROM offers WHERE website_id = ? AND is_active = 1 
         AND valid_from <= ? AND valid_until >= ?`,
        [websiteId, today, today]
      );
      return offers;
    }
  }

  // Invalidation
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
      console.error('Cache invalidation error:', error);
    }
  }

  static async invalidateProducts(websiteId) {
    try {
      await redisClient.del(`restaurant:${websiteId}:products`);
    } catch (error) {
      console.error('Cache invalidation error:', error);
    }
  }

  static async invalidateOffers(websiteId) {
    try {
      await redisClient.del(`restaurant:${websiteId}:offers`);
    } catch (error) {
      console.error('Cache invalidation error:', error);
    }
  }
}
```

---

## Expected Performance Gains

| Endpoint | Before (MySQL) | After (Redis) | Improvement |
|----------|----------------|---------------|-------------|
| GET /api/websites/:id | 50-100ms | 1-5ms | **95% faster** |
| GET /api/products/website/:id | 80-150ms | 2-8ms | **90% faster** |
| GET /api/websites/:id/offers | 60-120ms | 1-6ms | **95% faster** |
| GET /api/websites (list) | 100-200ms | 5-15ms | **90% faster** |

---

## Next Steps

1. **Install Redis**: `npm install redis`
2. **Create `services/redis.js`** (connection)
3. **Create `services/cacheService.js`** (caching logic)
4. **Update routes** to use cache service
5. **Add invalidation** on write operations
6. **Monitor cache hit rates** and adjust TTLs

---

## Production Considerations

- **Redis Cluster**: For high availability
- **Redis Sentinel**: For automatic failover
- **Memory Limits**: Set `maxmemory` policy (LRU eviction)
- **Persistence**: Use RDB snapshots + AOF for durability
- **Monitoring**: Use Redis Insight or Prometheus
