# Deploy Frontend to DigitalOcean App Platform

Your frontend is a **Vue.js + Vite** app that builds to static files. Deploy it as a **Static Site** component.

---

## Step 1: Add Frontend Component in DigitalOcean

1. Go to your app: **restaurantwebsitebuilder** → **Components** → **Add Component**
2. Select **Static Site**
3. Configure:

### Source
- **Source**: Your GitHub repo (same as backend)
- **Branch**: `main` (or your default branch)
- **Source Directory**: `frontend` ← **Important!**

### Build
- **Build Command**: `npm install --include=dev && npm run build`  
  (Use `--include=dev` so Vite and other devDependencies are installed; the Node buildpack prunes them before the custom command runs.)
- **Output Directory**: `dist` (Vite outputs to `dist/`)

### Environment Variables (Build-time)
Add these **Build-time Environment Variables**:

| Variable | Value |
|----------|-------|
| `VITE_API_URL` | `https://restaurantwebsitebuilder-q58fe.ondigitalocean.app` |
| `VITE_BASE_PATH` | `/etlobtodayenginrepo-frontend/` |

**Important:**  
- `VITE_API_URL` sets the API base URL.  
- `VITE_BASE_PATH` must match the path where the static site is routed (e.g. `/etlobtodayenginrepo-frontend/`). If the frontend is served at the app root (`/`), omit this or set to `/`.

### HTTP Routes
- **HTTP Routes**: Leave default (serves all routes from `index.html` for Vue Router)

### Name
- **Name**: `frontend` (or `restaurantwebsitebuilder-frontend`)

4. Click **Create Static Site**

---

## Step 2: Verify Deployment

After deployment completes:

1. Go to **Components** → your new frontend component
2. Find the **Live URL** (e.g. `https://frontend-xyz.ondigitalocean.app`)
3. Open it in a browser
4. Check browser console (F12) for any API errors

---

## Step 3: Update Backend CORS (if needed)

If you see CORS errors, ensure your backend allows the frontend domain:

In `backend/server.js`, CORS should allow your frontend URL:

```javascript
app.use(cors({
  origin: [
    'https://frontend-xyz.ondigitalocean.app', // Your frontend URL
    'http://localhost:5173' // For local dev
  ],
  credentials: true
}));
```

Or allow all origins (for testing):
```javascript
app.use(cors()); // Already configured
```

---

## Step 4: Custom Domain (Optional)

If you want a custom domain:

1. In your frontend component → **Settings** → **Domains**
2. Add your domain (e.g. `app.yourdomain.com`)
3. Follow DNS instructions to point your domain to DigitalOcean

---

## Troubleshooting

### Frontend shows "Cannot connect to backend"
- Check `VITE_API_URL` is set correctly in build-time env vars
- Verify backend URL: `https://restaurantwebsitebuilder-q58fe.ondigitalocean.app/health`
- Check browser console (F12) → Network tab for failed API calls

### 404 on page refresh (Vue Router)
- Ensure **HTTP Routes** in Static Site settings serves all routes from `index.html`
- DigitalOcean Static Sites should handle this automatically, but verify in component settings

### Build fails
- Check **Runtime Logs** in the frontend component
- Ensure `package.json` has `"build": "vite build"` script
- Verify `frontend/` directory exists in your repo

---

## Alternative: Deploy as Web Service (if you need server features)

If you need server-side rendering or API routes, deploy as a **Web Service**:

1. Create `frontend/Dockerfile`:
```dockerfile
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

2. Create `frontend/nginx.conf`:
```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

3. Deploy as **Web Service** component (similar to backend)

---

## Summary

**Quick setup:**
- Component type: **Static Site**
- Source directory: `frontend`
- Build command: `npm install && npm run build`
- Output directory: `dist`
- Build env var: `VITE_API_URL=https://restaurantwebsitebuilder-q58fe.ondigitalocean.app`

After deployment, your frontend will be live and connected to your backend! 🚀
