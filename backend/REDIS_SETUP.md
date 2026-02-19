# Redis Setup Guide

## ✅ Status
- ✅ Redis npm package installed (`redis@5.11.0`)
- ✅ Code is ready and configured
- ⏳ Redis server needs to be installed

## Option 1: Install Redis with Homebrew (Recommended)

### Step 1: Fix permissions (if needed)
```bash
sudo chown -R zakaryaalsaba /opt/homebrew/Cellar
```

### Step 2: Install Redis
```bash
brew install redis
```

### Step 3: Start Redis
```bash
brew services start redis
```

### Step 4: Verify it's running
```bash
redis-cli ping
# Should return: PONG
```

## Option 2: Use Docker (If you have Docker)

```bash
docker run -d -p 6379:6379 --name redis redis:alpine
```

To stop: `docker stop redis`
To start: `docker start redis`

## Option 3: Run Redis Manually (Temporary)

If you just want to test, you can run Redis manually:

```bash
redis-server
```

This will run in the foreground. Press Ctrl+C to stop.

## Option 4: Use Without Redis (Fallback Mode)

**Good news:** Your app will work perfectly without Redis! It will automatically fall back to the database.

- All endpoints will work normally
- You'll see warnings in logs: "Cache error, falling back to DB"
- Performance will be slightly slower (but still functional)

## After Redis is Running

1. **Add to `.env` file:**
   ```env
   REDIS_URL=redis://localhost:6379
   REDIS_TTL_RESTAURANTS=3600
   REDIS_TTL_PRODUCTS=1800
   REDIS_TTL_OFFERS=600
   REDIS_TTL_BUSINESS_HOURS=86400
   REDIS_TTL_RESTAURANT_LIST=300
   REDIS_AUTO_CONNECT=true
   ```

2. **Start your server:**
   ```bash
   npm start
   # or
   npm run dev
   ```

3. **Check logs for:**
   ```
   ✅ Redis Client Connected
   ✅ Redis Client Ready
   ```

## Troubleshooting

### Redis not connecting?
- Check if Redis is running: `redis-cli ping`
- Verify port 6379 is not in use: `lsof -i :6379`
- Check `.env` file has `REDIS_URL=redis://localhost:6379`

### App works without Redis?
- That's normal! It falls back to database automatically
- Check logs for "Cache error, falling back to DB" messages
- Install Redis to enable caching and improve performance

### Want to disable Redis temporarily?
- Comment out the Redis connection in `server.js`
- Or set `REDIS_AUTO_CONNECT=false` in `.env`

## Quick Test

Once Redis is running:

```bash
# Test Redis connection
redis-cli ping

# Check if keys are being cached
redis-cli KEYS "*"

# Monitor Redis activity
redis-cli MONITOR
```
