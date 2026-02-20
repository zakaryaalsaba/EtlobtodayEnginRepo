# DigitalOcean Deployment Troubleshooting

## Common Error: "Non-Zero Exit Code"

If your container exits with a non-zero exit code, check the logs in DigitalOcean App Platform → **Runtime Logs**.

---

## Issue 1: Database Connection Failed

### Symptoms
- Logs show: `❌ MySQL database connection failed`
- Error code: `ECONNREFUSED`, `ETIMEDOUT`, or SSL-related errors

### Solutions

#### A. Check Environment Variables
Ensure these are set correctly in App Platform → **Settings** → **App-Level Environment Variables**:

```
MYSQL_HOST=restaurantwebsites-do-user-33254093-0.g.db.ondigitalocean.com
MYSQL_PORT=25060
MYSQL_DB=defaultdb
MYSQL_USER=doadmin
MYSQL_PASSWORD=<your-database-password>
MYSQL_SSL_MODE=REQUIRED
```

**Important:** 
- In Runtime Logs you should see a line like: `Connecting to MySQL at <host>:<port>`. If it says `localhost:3306`, your app-level env vars are not set or not applied.
- `MYSQL_DB` must be `defaultdb` (not `restaurant_websites`)
- `MYSQL_PASSWORD` should be **encrypted** (check the "Encrypt" box)
- `MYSQL_SSL_MODE=REQUIRED` is mandatory for DigitalOcean Managed MySQL

#### B. Check Database Trusted Sources
1. Go to DigitalOcean → **Databases** → Your MySQL cluster
2. Click **Settings** → **Trusted Sources**
3. Add your app’s allowed IPs. If your app has **public static ingress IPs** (e.g. `162.159.140.98`, `172.66.0.96`), add these as trusted sources so the app can reach MySQL.
4. Alternatively, use “Allow inbound from same region” if available, or temporarily allow `0.0.0.0/0` for testing (restrict later).

#### C. Verify Database Credentials
- Double-check username, password, host, and port from the MySQL control panel
- Test connection string format: `mysql://doadmin:<your-password>@restaurantwebsites-do-user-33254093-0.g.db.ondigitalocean.com:25060/defaultdb?ssl-mode=REQUIRED`

---

## Issue 2: Schema File Not Found

### Symptoms
- Logs show: `Schema file not found: /app/db/schema.sql`
- Error: `ENOENT`

### Solution
- Ensure `backend/db/schema.sql` exists in your GitHub repo
- Check that **Source Directory** in App Platform is set to `backend` (not root)

---

## Issue 3: Redis Connection Failed (Non-Critical)

### Symptoms
- Logs show: `⚠️ Redis connection failed (continuing without cache)`

### Solution
- **This is OK** - Redis is optional. The app will work without it (uses DB directly).
- If you want Redis, create a Managed Redis cluster and set `REDIS_URL` env var.
- If you don't want Redis, you can ignore this warning.

---

## Issue 4: Port Binding Error

### Symptoms
- Logs show: `EADDRINUSE` or port binding errors
- Error: `Port 3000 is already in use`

### Solution
- DigitalOcean App Platform sets `PORT` automatically (usually `8080`)
- The app listens on `process.env.PORT` (default 8080 in production). Do not hardcode port 3000.
- If you see **Readiness probe failed: dial tcp ... 3000**, set your component’s **HTTP Port** to `8080` in App Platform (Settings → the component → HTTP Port / Health check port).

---

## Issue 5: Redis ECONNREFUSED localhost:6379

### Symptoms
- Logs show: `Redis Client Error: ECONNREFUSED ::1:6379` or `127.0.0.1:6379`

### Solution
- **This is expected** if you have not set `REDIS_URL`. The app does not connect to Redis when `REDIS_URL` is unset, so you will not see a connection attempt in the latest code.
- Redis is optional. If you do not use Managed Redis, ignore any Redis messages. If you do, set `REDIS_URL` to your Managed Redis URL.

---

## Issue 6: Missing Environment Variables

### Symptoms
- Logs show: `❌ Missing` for required env vars
- Database connection fails with authentication errors

### Solution
Check the **Runtime Logs** for the "Environment check" section. It will show which variables are missing:

```
Environment check:
  MYSQL_HOST: ✅ Set / ❌ Missing
  MYSQL_USER: ✅ Set / ❌ Missing
  MYSQL_PASSWORD: ✅ Set / ❌ Missing
  ...
```

Add any missing variables in App Platform → **Settings** → **App-Level Environment Variables**.

---

## Issue 7: Database Name Mismatch

### Symptoms
- Connection succeeds but tables aren't created
- Logs show: `Error initializing MySQL database`

### Solution
- Your DigitalOcean database is named `defaultdb`
- Set `MYSQL_DB=defaultdb` (not `restaurant_websites`)
- The backend will create tables inside `defaultdb` automatically

---

## Debugging Steps

1. **Check Runtime Logs**
   - App Platform → Your App → **Runtime Logs**
   - Look for error messages with `❌` or `Error:`

2. **Check Build Logs**
   - App Platform → Your App → **Build Logs**
   - Ensure Docker build succeeded

3. **Verify Environment Variables**
   - App Platform → **Settings** → **App-Level Environment Variables**
   - Ensure all required vars are set and **encrypted** (for passwords/keys)

4. **Test Database Connection Manually**
   - Use a MySQL client (e.g., MySQL Workbench, DBeaver) to connect:
     - Host: `restaurantwebsites-do-user-33254093-0.g.db.ondigitalocean.com`
     - Port: `25060`
     - User: `doadmin`
     - Password: (use your database password from DO panel)
     - SSL: Required
   - If this fails, the issue is with database access, not your app

5. **Check Health Endpoint**
   - Once deployed, test: `https://your-app.ondigitalocean.app/health`
   - Should return: `{"status":"ok","timestamp":"..."}`

---

## Quick Fix Checklist

- [ ] All environment variables set (see DIGITALOCEAN_ENV_SETUP.md)
- [ ] `MYSQL_DB=defaultdb` (not `restaurant_websites`)
- [ ] `MYSQL_SSL_MODE=REQUIRED` is set
- [ ] Database Trusted Sources includes App Platform IPs
- [ ] `JWT_SECRET` is set (generate with `openssl rand -base64 32`)
- [ ] `API_BASE_URL` matches your app's public URL
- [ ] Source Directory is set to `backend` in App Platform
- [ ] Dockerfile path is `backend/Dockerfile` (if using Dockerfile build)

---

## Still Not Working?

Share the **Runtime Logs** output (especially lines with `❌` or `Error:`) and I can help diagnose the specific issue.
