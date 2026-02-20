# DigitalOcean App Platform - Quick Environment Variables Setup

## ⚠️ IMPORTANT: Set these in App Platform, NOT in code

Go to: **DigitalOcean → Your App → Your Backend Component → Settings → App-Level Environment Variables**

---

## Required Environment Variables

Copy and paste these **exact** values:

| Variable Name | Value | Encrypt? |
|--------------|-------|----------|
| `MYSQL_HOST` | `restaurantwebsites-do-user-33254093-0.g.db.ondigitalocean.com` | ❌ No |
| `MYSQL_PORT` | `25060` | ❌ No |
| `MYSQL_DB` | `defaultdb` | ❌ No |
| `MYSQL_USER` | `doadmin` | ❌ No |
| `MYSQL_PASSWORD` | `<your-database-password>` | ✅ **YES** |
| `MYSQL_SSL_MODE` | `REQUIRED` | ❌ No |
| `NODE_ENV` | `production` | ❌ No |
| `PORT` | `8080` | ❌ No |

---

## Additional Required Variables

You also need these (generate or set appropriate values):

| Variable Name | Value | How to Get |
|--------------|-------|------------|
| `JWT_SECRET` | (generate random string) | Run: `openssl rand -base64 32` |
| `API_BASE_URL` | `https://your-backend-app.ondigitalocean.app` | Your backend app's public URL from App Platform |

---

## Optional (but recommended)

| Variable Name | Value | Notes |
|--------------|-------|-------|
| `FRONTEND_URL` | `https://your-frontend-domain.com` | For CORS |
| `REDIS_URL` | (if using Managed Redis) | Only if you set up Redis |

---

## Steps to Add Variables

1. Go to **DigitalOcean App Platform** → Your App
2. Click on your **backend component** (the one that runs Node.js)
3. Go to **Settings** tab
4. Scroll to **App-Level Environment Variables**
5. Click **Edit** or **Add Variable**
6. For each variable:
   - **Key:** Variable name (e.g., `MYSQL_HOST`)
   - **Value:** The value from the table above
   - **Encrypt:** ✅ Check ONLY for `MYSQL_PASSWORD` and `JWT_SECRET`
7. Click **Save**
8. The app will **automatically redeploy** with new variables

---

## Verify Variables Are Set

After redeploy, check **Runtime Logs**. You should see:

✅ **Good:**
```
Connecting to MySQL at restaurantwebsites-do-user-33254093-0.g.db.ondigitalocean.com:25060 (database: defaultdb)
✅ MySQL database connected
✅ Server running on port 8080
```

❌ **Bad (if you see this, variables aren't set):**
```
Connecting to MySQL at localhost:3306 (database: restaurant_websites)
❌ Missing required environment variables: MYSQL_HOST, MYSQL_USER, MYSQL_PASSWORD
```

---

## Security Note

⚠️ Never commit real passwords. Get `MYSQL_PASSWORD` from DigitalOcean → Databases → your cluster → Connection details. After changing it, consider:
1. Rotating the database password in DigitalOcean MySQL panel
2. Updating `MYSQL_PASSWORD` in App Platform with the new password
3. Deleting this file or moving it to a secure location

---

## Troubleshooting

- **Still seeing `localhost:3306`?** → Variables aren't reaching the container. Check:
  - Did you set them on the **correct component** (backend, not frontend)?
  - Did you click **Save** after adding variables?
  - Did the app redeploy after saving?

- **Connection refused?** → Check **Database Trusted Sources**:
  - DigitalOcean → Databases → Your MySQL cluster → Settings → Trusted Sources
  - Add your App Platform app's outbound IPs, or temporarily allow `0.0.0.0/0` for testing
