# Deploying Backend & Database on DigitalOcean

This guide covers publishing the RestaurantEngin **backend (Node.js)** and **MySQL database** on DigitalOcean so all apps (ResturantAndroid, StoreController, DriverAndroid, frontend, menu-extractor) can use the same API.

---

## Architecture options

| Option | Best for | Backend | Database | Uploads |
|--------|----------|---------|----------|---------|
| **A: App Platform + Managed MySQL** | Easiest, auto-deploy from GitHub | App Platform (Node) | Managed MySQL | App Platform Volume or Spaces |
| **B: Droplet (VPS)** | Full control, one server | Node on Droplet | MySQL on same Droplet | Local disk or Spaces |

**Recommended:** Option A for quick production; Option B if you prefer a single VPS.

---

## Option A: App Platform + Managed MySQL

### 1. Create Managed MySQL database

1. Log in to [DigitalOcean](https://cloud.digitalocean.com) → **Databases** → **Create Database**.
2. Choose **MySQL** (e.g. 8.x).
3. Select region (e.g. same as your app).
4. Plan: **Basic** (1 node) is enough to start.
5. Create the cluster. Wait until it’s **Online**.
6. Open the database → **Connection details** (or **Users & Databases**):
   - Create a database name, e.g. `restaurant_websites` (or use default).
   - Note: **host**, **port** (usually 25060), **user**, **password**, **database**.
7. Under **Settings** → **Trusted sources**, add your App Platform’s outbound IPs, or allow **0.0.0.0/0** for simplicity (restrict later with firewall if needed).

### 2. (Optional) Create Managed Redis

- **Databases** → **Create** → **Redis**. Same region as app.
- After creation, copy the **Connection URI** (e.g. `rediss://default:xxx@db-redis-xxx.db.ondigitalocean.com:25061`).
- If you skip Redis, the backend still runs and uses the database instead of cache.

### 3. Deploy backend with App Platform

1. **Apps** → **Create App** → **GitHub**.
2. Connect the repo (e.g. `zakaryaalsaba/EtlobtodayEnginRepo`).
3. Choose **branch** (e.g. `main`).
4. **Source directory:** leave empty or set to `backend` if your repo root is the monorepo.
   - If repo root is the whole project, set **Source Directory** to `backend`.
5. **Build settings:**
   - **Buildpack:** Choose **Dockerfile** if you want to use the repo’s `backend/Dockerfile` (recommended). Set **Dockerfile Path** to `backend/Dockerfile` and **Source Directory** to `backend`.
   - Or use **Node.js** buildpack: **Source Directory** = `backend`, **Build Command** = `npm install`, **Run Command** = `npm start`. **Output Directory** leave empty.
6. **Resources:** Basic (512 MB–1 GB) to start.
7. **Environment variables** (App → Settings → App-Level Environment Variables):

   | Variable | Value | Notes |
   |----------|--------|--------|
   | `NODE_ENV` | `production` | |
   | `PORT` | `8080` | App Platform sets this; 8080 is default |
   | `API_BASE_URL` | `https://your-app-name.ondigitalocean.app` | Your app’s public URL (see after first deploy) |
   | `MYSQL_HOST` | *from Managed DB* | e.g. `db-mysql-xxx.db.ondigitalocean.com` |
   | `MYSQL_PORT` | *from Managed DB* | e.g. `25060` |
   | `MYSQL_DB` | `restaurant_websites` | Database name you created |
   | `MYSQL_USER` | *from Managed DB* | |
   | `MYSQL_PASSWORD` | *from Managed DB* | Use **Encrypt** in DO |
   | `JWT_SECRET` | *long random string* | Generate with `openssl rand -base64 32` |
   | `REDIS_URL` | *Redis connection URI* | Optional; omit if no Redis |
   | `FRONTEND_URL` | `https://your-frontend.vercel.app` | For CORS; adjust to your frontend |
   | `FIREBASE_DATABASE_URL` | *your Firebase Realtime DB URL* | If using Firebase orders |
   | `FIREBASE_WEB_API_KEY` | *from Firebase Console* | If using Firebase |
   | `FIREBASE_SERVICE_ACCOUNT_PATH` | (see below) | Or use JSON in env (not recommended) |

   **Firebase on App Platform:** Upload the service account JSON to a **Volume** mounted at e.g. `/data`, then set `FIREBASE_SERVICE_ACCOUNT_PATH=/data/firebase-service-account.json`. Or add the file in the repo in a secure way (e.g. DO encrypted env for the JSON string if supported).

8. **Persistent storage (uploads):**
   - **Resources** → **Add Volume** → mount path e.g. `/uploads` (or `/app/uploads`).
   - Ensure the backend writes uploads to this path (see “Uploads path” below).

9. Deploy. After first deploy, set `API_BASE_URL` to the real app URL and redeploy if needed.

### 4. Database initialization

- The backend runs `initDatabase()` on startup (see `backend/db/init.js`), which creates the database if missing and runs `schema.sql` and migrations. No manual SQL step is required for a fresh DB.

### 5. Point all apps to the backend

- **Frontend (Vue):** `VITE_API_URL=https://your-app-name.ondigitalocean.app`
- **ResturantAndroid / StoreController / DriverAndroid:** Set `API_BASE_URL` (in `local.properties` or build config) to `https://your-app-name.ondigitalocean.app/api/`
- **Menu extractor:** Same API base URL in its config.

---

## Option B: Single Droplet (VPS)

### 1. Create Droplet

- **Droplets** → **Create** → Ubuntu 22.04, plan Basic (1 GB RAM minimum).
- Add SSH key. Create Droplet.

### 2. Install Node, MySQL, Redis (optional)

SSH into the Droplet, then:

```bash
# Node 20
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs

# MySQL
sudo apt update && sudo apt install -y mysql-server
sudo mysql_secure_installation

# Redis (optional)
sudo apt install -y redis-server
```

### 3. Create MySQL database and user

```bash
sudo mysql -e "
CREATE DATABASE IF NOT EXISTS restaurant_websites CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'restaurant_app'@'localhost' IDENTIFIED BY 'YOUR_STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON restaurant_websites.* TO 'restaurant_app'@'localhost';
FLUSH PRIVILEGES;
"
```

### 4. Deploy backend code

- Clone your repo (e.g. from GitHub) into e.g. `/var/www/restaurant-backend`, or only the `backend` folder.
- In the backend directory:

```bash
cd /var/www/restaurant-backend   # or /var/www/restaurant-backend/backend
npm install --production
```

### 5. Environment file

Create `/var/www/restaurant-backend/.env` (or `backend/.env`):

```env
NODE_ENV=production
PORT=3000
API_BASE_URL=https://your-domain.com
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DB=restaurant_websites
MYSQL_USER=restaurant_app
MYSQL_PASSWORD=YOUR_STRONG_PASSWORD
JWT_SECRET=your-long-random-jwt-secret
REDIS_URL=redis://localhost:6379
FRONTEND_URL=https://your-frontend-url
```

Add Firebase, SMTP, PayTabs, etc. as needed (see `backend/.env.example`).

### 6. Uploads directory

```bash
mkdir -p /var/www/restaurant-backend/uploads
# If using subdirs (menu-extractor, etc.):
mkdir -p /var/www/restaurant-backend/uploads/menu-extractor
mkdir -p /var/www/restaurant-backend/uploads/delivery-zones
mkdir -p /var/www/restaurant-backend/uploads/delivery-companies
```

### 7. Run with PM2 (recommended)

```bash
sudo npm install -g pm2
cd /var/www/restaurant-backend
pm2 start server.js --name restaurant-api
pm2 save && pm2 startup
```

### 8. Nginx as reverse proxy (HTTPS)

- Point a domain to the Droplet’s IP.
- Install Nginx, get a certificate (e.g. Certbot), and proxy to `http://127.0.0.1:3000`. Then use `https://your-domain.com` as `API_BASE_URL` everywhere.

---

## Uploads path (App Platform vs Droplet)

- **Droplet:** Backend uses `path.join(__dirname, 'uploads')`, so uploads go to the app’s `uploads/` directory. Use the same path and ensure the dir exists (step 6 above).
- **App Platform:** Mount a **Volume** (e.g. `/uploads`) and ensure the app writes to that path. If the app currently uses `path.join(__dirname, 'uploads')`, that is relative to the app root; if the volume is mounted at `/uploads`, you can set an env var e.g. `UPLOADS_DIR=/uploads` and use it in the code, or keep writing to `./uploads` and mount the volume at `./uploads` if the platform allows.

---

## Environment variables reference (production)

| Variable | Required | Description |
|----------|----------|-------------|
| `NODE_ENV` | Yes | `production` |
| `PORT` | Yes | Set by App Platform (e.g. 8080); on Droplet use 3000 or match Nginx |
| `API_BASE_URL` | Yes | Public URL of the API (e.g. `https://api.yourdomain.com`) |
| `MYSQL_HOST` | Yes | MySQL host |
| `MYSQL_PORT` | Yes | Usually 3306 (Droplet) or 25060 (Managed) |
| `MYSQL_DB` | Yes | `restaurant_websites` |
| `MYSQL_USER` | Yes | DB user |
| `MYSQL_PASSWORD` | Yes | DB password |
| `JWT_SECRET` | Yes | Long random string for JWT signing |
| `REDIS_URL` | No | Redis URI; app works without Redis (no cache) |
| `FRONTEND_URL` | No | Frontend origin for CORS |
| `FIREBASE_DATABASE_URL` | No | Firebase Realtime DB URL (StoreController orders) |
| `FIREBASE_WEB_API_KEY` | No | Firebase Web API key |
| `FIREBASE_SERVICE_ACCOUNT_PATH` | No | Path to Firebase service account JSON |
| `OPENAI_API_KEY` | No | For menu extractor |
| `PAYTABS_*` / `STRIPE_*` / `SMTP_*` | No | As needed for payments and email |

See `backend/.env.example` for a full template.

---

## After deployment

1. **Health check:** `curl https://your-api-url/health` → `{"status":"ok",...}`.
2. **Database:** Backend creates tables on first start; confirm in MySQL that `restaurant_websites` and other tables exist.
3. **Apps:** Update ResturantAndroid, StoreController, DriverAndroid, frontend, and menu-extractor to use `API_BASE_URL` / `VITE_API_URL` pointing to the new backend URL.
4. **Firebase:** If StoreController uses Firebase Realtime DB, ensure the backend’s Firebase config (database URL, service account) matches the project and has the right permissions.

---

## Quick links

- [DigitalOcean App Platform](https://docs.digitalocean.com/products/app-platform/)
- [DigitalOcean Managed Databases (MySQL)](https://docs.digitalocean.com/products/databases/mysql/)
- [DigitalOcean Managed Redis](https://docs.digitalocean.com/products/databases/redis/)
