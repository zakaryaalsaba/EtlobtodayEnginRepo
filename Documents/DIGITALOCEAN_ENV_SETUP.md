# DigitalOcean App Platform - Environment Variables Setup

## Database Connection (Managed MySQL)

Set these environment variables in your DigitalOcean App Platform:

| Variable | Value |
|----------|-------|
| `MYSQL_HOST` | `restaurantwebsites-do-user-33254093-0.g.db.ondigitalocean.com` |
| `MYSQL_PORT` | `25060` |
| `MYSQL_DB` | `defaultdb` |
| `MYSQL_USER` | `doadmin` |
| `MYSQL_PASSWORD` | `<your-database-password>` (from DO MySQL panel) |
| `MYSQL_SSL_MODE` | `REQUIRED` |

**Note:** The backend automatically detects DigitalOcean Managed MySQL (by checking if host contains `db.ondigitalocean.com`) and enables SSL. Setting `MYSQL_SSL_MODE=REQUIRED` explicitly ensures SSL is used.

---

## Required Environment Variables

### Server Configuration
```
NODE_ENV=production
PORT=8080
API_BASE_URL=https://your-app-name.ondigitalocean.app
```

### Database (Managed MySQL)
```
MYSQL_HOST=restaurantwebsites-do-user-33254093-0.g.db.ondigitalocean.com
MYSQL_PORT=25060
MYSQL_DB=defaultdb
MYSQL_USER=doadmin
MYSQL_PASSWORD=<your-database-password>
MYSQL_SSL_MODE=REQUIRED
```

### JWT Authentication
```
JWT_SECRET=<generate-a-long-random-string>
ACCESS_TOKEN_EXPIRY=1h
REFRESH_TOKEN_EXPIRY=7d
```

**Generate JWT_SECRET:**
```bash
openssl rand -base64 32
```

### Frontend/CORS
```
FRONTEND_URL=https://your-frontend-domain.com
```

---

## Optional Environment Variables

### Redis (if using Managed Redis)
```
REDIS_URL=rediss://default:password@db-redis-xxx.db.ondigitalocean.com:25061
```

### Firebase (for StoreController orders)
```
FIREBASE_DATABASE_URL=https://your-project.firebaseio.com
FIREBASE_WEB_API_KEY=your-web-api-key
FIREBASE_BACKEND_UID=your-backend-uid
FIREBASE_SERVICE_ACCOUNT_PATH=/data/firebase-service-account.json
```

**Note:** For Firebase service account, upload the JSON file to a Volume mounted at `/data` in App Platform.

### Menu Extractor (OpenAI)
```
OPENAI_API_KEY=sk-...
```

### Payments
```
PAYTABS_PROFILE_ID=your-profile-id
PAYTABS_SERVER_KEY=your-server-key
STRIPE_SECRET_KEY=sk_... (optional, only if using Stripe)
```

### Email (SMTP)
```
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_SECURE=false
SMTP_USER=your-email@gmail.com
SMTP_PASSWORD=your-app-password
SMTP_FROM=noreply@yourdomain.com
```

---

## How to Set Environment Variables in DigitalOcean App Platform

1. Go to your App → **Settings** → **App-Level Environment Variables**
2. Click **Edit** or **Add Variable**
3. For each variable:
   - **Key:** Variable name (e.g., `MYSQL_HOST`)
   - **Value:** Variable value
   - **Encrypt:** ✅ Check this for sensitive values (passwords, keys)
4. Click **Save**
5. The app will automatically redeploy with new variables

---

## Database Name Note

Your Managed MySQL database is named `defaultdb`. The backend's `initDatabase()` function will:
- Connect to `defaultdb`
- Create tables inside `defaultdb` (if they don't exist)
- Use `defaultdb` for all queries

If you want to use a different database name (e.g., `restaurant_websites`), you can:
1. Create a new database in DigitalOcean MySQL control panel
2. Update `MYSQL_DB` to the new database name
3. Redeploy

---

## Connection String Format

If you need the full connection string for reference:
```
mysql://doadmin:<your-database-password>@restaurantwebsites-do-user-33254093-0.g.db.ondigitalocean.com:25060/defaultdb?ssl-mode=REQUIRED
```

The backend uses individual environment variables instead of a connection string, which is more secure and flexible.

---

## Troubleshooting

### Connection Refused / SSL Error
- Ensure `MYSQL_SSL_MODE=REQUIRED` is set
- Verify the database host, port, user, and password are correct
- Check that your App Platform app is in the database's **Trusted Sources** (Settings → Trusted Sources in MySQL control panel)

### Database Not Found
- The backend creates tables automatically on first start
- If you see "database not found", ensure `MYSQL_DB=defaultdb` matches the database name in DigitalOcean

### Slow Queries
- Enable Redis caching by setting `REDIS_URL` (optional but recommended)
- Check database indexes in MySQL control panel
