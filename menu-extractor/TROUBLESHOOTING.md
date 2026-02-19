# Troubleshooting Guide

## Error: "Unexpected token '<', "<!DOCTYPE "... is not valid JSON"

This error means the API is returning an HTML page instead of JSON. Here's how to fix it:

### Step 1: Restart Backend Server

The backend needs to be restarted to load the new `/api/menu-extractor` route:

```bash
cd /Users/zakaryaalsaba/Desktop/RestaurantEngin/backend
# Stop the current server (Ctrl+C)
npm start
# or
npm run dev
```

### Step 2: Verify API Endpoint

Test if the endpoint is accessible:

```bash
curl http://localhost:3000/api/menu-extractor/test
```

Should return:
```json
{"success":true,"message":"Menu extractor API is working","timestamp":"..."}
```

### Step 3: Check API URL Configuration

Make sure your Vue app is pointing to the correct backend URL:

1. Check if `.env` file exists in `menu-extractor/` folder
2. If not, create it:
   ```env
   VITE_API_BASE_URL=http://localhost:3000/api
   ```
3. Restart the Vue dev server after creating/updating `.env`

### Step 4: Check Browser Console

Open browser DevTools (F12) and check:
- **Console tab**: Look for detailed error messages
- **Network tab**: Check the failed request:
  - Status code (should be 200, not 404 or 500)
  - Response preview (should be JSON, not HTML)

### Step 5: Check Backend Logs

Look at your backend terminal for any errors when the request is made.

### Common Issues:

1. **Backend not running**: Start it with `npm start` in the backend folder
2. **Wrong port**: Make sure backend is on port 3000 (or update `.env`)
3. **CORS issues**: Backend should have CORS enabled (already configured)
4. **Route not loaded**: Restart backend after adding new routes

### Quick Test:

Open in browser: `http://localhost:3000/api/menu-extractor/test`

If you see JSON response, the route is working. If you see HTML or 404, the route isn't loaded.
