# Redis Cache Integration Examples

This document shows how to integrate Redis caching into your existing routes.

## Quick Start

1. **Install Redis client:**
   ```bash
   npm install redis
   ```

2. **Start Redis server:**
   ```bash
   # macOS
   brew install redis
   brew services start redis

   # Linux
   sudo apt-get install redis-server
   sudo systemctl start redis

   # Docker
   docker run -d -p 6379:6379 redis:alpine
   ```

3. **Add to `.env`:**
   ```env
   REDIS_URL=redis://localhost:6379
   REDIS_TTL_RESTAURANTS=3600
   REDIS_TTL_PRODUCTS=1800
   REDIS_TTL_OFFERS=600
   ```

---

## Example 1: Update `routes/websites.js`

### Before (No Cache):
```javascript
router.get('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const [websites] = await pool.execute(
      'SELECT * FROM restaurant_websites WHERE id = ?',
      [id]
    );
    if (websites.length === 0) {
      return res.status(404).json({ error: 'Website not found' });
    }
    res.json({ website: websites[0] });
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch website', message: error.message });
  }
});
```

### After (With Cache):
```javascript
import CacheService from '../services/cacheService.js';

router.get('/:id', async (req, res) => {
  try {
    const websiteId = parseInt(req.params.id);
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
```

### Update List Endpoint:
```javascript
router.get('/', async (req, res) => {
  try {
    const openNow = req.query.open_now === 'true' || req.query.open_now === true;
    const websites = await CacheService.getAllRestaurants(openNow);
    res.json({ websites });
  } catch (error) {
    console.error('Error fetching websites:', error);
    res.status(500).json({ error: 'Failed to fetch websites', message: error.message });
  }
});
```

### Update Offers Endpoint:
```javascript
router.get('/:id/offers', async (req, res) => {
  try {
    const websiteId = parseInt(req.params.id);
    if (isNaN(websiteId)) {
      return res.status(400).json({ error: 'Invalid website ID' });
    }
    
    const offers = await CacheService.getOffers(websiteId);
    res.json({ offers });
  } catch (error) {
    console.error('Error fetching offers:', error);
    res.status(500).json({ error: 'Failed to fetch offers', message: error.message });
  }
});
```

### Invalidate Cache on Update:
```javascript
router.put('/:id', async (req, res) => {
  try {
    const websiteId = parseInt(req.params.id);
    // ... your update logic ...
    
    // After successful update, invalidate cache
    await CacheService.invalidateRestaurant(websiteId);
    
    res.json({ website: updatedWebsite });
  } catch (error) {
    // ... error handling ...
  }
});
```

---

## Example 2: Update `routes/products.js`

### Before (No Cache):
```javascript
router.get('/website/:websiteId', async (req, res) => {
  try {
    const { websiteId } = req.params;
    const [products] = await pool.execute(
      'SELECT * FROM products WHERE website_id = ? ORDER BY category, name',
      [websiteId]
    );
    res.json({ products });
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch products', message: error.message });
  }
});
```

### After (With Cache):
```javascript
import CacheService from '../services/cacheService.js';

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
```

### Invalidate Cache on Create/Update/Delete:
```javascript
router.post('/', async (req, res) => {
  try {
    const { website_id } = req.body;
    // ... create product logic ...
    
    // Invalidate products cache
    await CacheService.invalidateProducts(website_id);
    
    res.status(201).json({ product: newProduct });
  } catch (error) {
    // ... error handling ...
  }
});

router.put('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    
    // Get website_id before update
    const [oldProduct] = await pool.execute(
      'SELECT website_id FROM products WHERE id = ?',
      [id]
    );
    const websiteId = oldProduct[0]?.website_id;
    
    // ... update logic ...
    
    // Invalidate cache
    if (websiteId) {
      await CacheService.invalidateProducts(websiteId);
    }
    
    res.json({ product: updatedProduct });
  } catch (error) {
    // ... error handling ...
  }
});

router.delete('/:id', async (req, res) => {
  try {
    // Get website_id before delete
    const [product] = await pool.execute(
      'SELECT website_id FROM products WHERE id = ?',
      [id]
    );
    const websiteId = product[0]?.website_id;
    
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

## Example 3: Update `routes/restaurant.js` (Admin Routes)

```javascript
import CacheService from '../services/cacheService.js';

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
```

---

## Example 4: Initialize Redis in `server.js`

```javascript
import { connectRedis } from './services/redis.js';

// ... other imports ...

async function startServer() {
  try {
    // Initialize database
    await initDatabase();
    
    // Initialize Redis connection
    await connectRedis();
    
    // Start Express server
    app.listen(PORT, () => {
      console.log(`Server running on port ${PORT}`);
    });
  } catch (error) {
    console.error('Failed to start server:', error);
    process.exit(1);
  }
}

startServer();
```

---

## Testing Cache

### Check if Redis is working:
```bash
redis-cli
> PING
PONG
> KEYS restaurant:*
> GET restaurant:1
```

### Monitor cache hits:
Add this endpoint to see cache statistics:

```javascript
// routes/admin.js or routes/test.js
router.get('/cache/stats', async (req, res) => {
  try {
    const redisClient = (await import('../services/redis.js')).default;
    const info = await redisClient.info('stats');
    const keys = await redisClient.keys('*');
    
    res.json({
      connected: redisClient.isReady,
      totalKeys: keys.length,
      keys: keys.slice(0, 20), // First 20 keys
      info: info
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});
```

---

## Migration Checklist

- [ ] Install `redis` package
- [ ] Start Redis server
- [ ] Add Redis URL to `.env`
- [ ] Create `services/redis.js`
- [ ] Create `services/cacheService.js`
- [ ] Update `server.js` to connect Redis
- [ ] Update `routes/websites.js` GET endpoints
- [ ] Update `routes/products.js` GET endpoints
- [ ] Add cache invalidation to PUT/POST/DELETE endpoints
- [ ] Test endpoints with Redis running
- [ ] Test endpoints with Redis stopped (fallback should work)
- [ ] Monitor cache hit rates

---

## Troubleshooting

### Redis connection fails:
- Check if Redis is running: `redis-cli ping`
- Verify `REDIS_URL` in `.env`
- Check firewall/network settings
- App will fallback to DB automatically (check logs)

### Cache not updating:
- Ensure you're calling `invalidate*()` methods after writes
- Check TTL values (maybe too long)
- Clear cache manually: `redis-cli FLUSHDB`

### Performance not improving:
- Check cache hit rate (should be >80%)
- Verify Redis is actually being used (check logs)
- Consider increasing TTL for rarely-changing data
