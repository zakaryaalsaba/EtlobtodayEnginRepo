# Environment Configuration Guide

This document describes how to configure environment variables for the Restaurant Engine project to avoid hardcoded localhost/IP addresses.

## Backend Configuration

### Environment Variables

Create a `.env` file in the `backend/` directory (copy from `.env.example`):

```bash
# Server Configuration
PORT=3000
NODE_ENV=development

# API Base URL (used for generating image URLs and API endpoints)
API_BASE_URL=http://localhost:3000

# Database Configuration
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DB=restaurant_websites
MYSQL_USER=root
MYSQL_PASSWORD=

# Frontend URL (for CORS and redirects)
FRONTEND_URL=http://localhost:5173

# Android Emulator URL (for notifications and image URLs sent to Android)
ANDROID_EMULATOR_URL=http://10.0.2.2:3000
```

### Production Configuration

For production, update these values:
- `API_BASE_URL=https://your-domain.com`
- `ANDROID_EMULATOR_URL=https://your-domain.com` (or remove if not needed)
- `FRONTEND_URL=https://your-frontend-domain.com`
- `NODE_ENV=production`

## Frontend Configuration

### Environment Variables

Create a `.env` file in the `frontend/` directory (copy from `.env.example`):

```bash
# API Base URL
VITE_API_URL=http://localhost:3000

# Vite Proxy Target (for development)
VITE_PROXY_TARGET=http://localhost:3000
```

### Production Configuration

For production builds:
- `VITE_API_URL=https://your-api-domain.com`
- `VITE_PROXY_TARGET` is not used in production (only for dev server)

## Android Apps Configuration

### Restaurant Android App

1. Create or update `gradle.properties` in `ResturantAndroid/` directory:

```properties
# API Base URL Configuration
# For Android Emulator: http://10.0.2.2:3000/api/
# For Physical Device: http://YOUR_IP_ADDRESS:3000/api/
# For Production: https://your-domain.com/api/
API_BASE_URL=http://10.0.2.2:3000/api/
```

2. For physical device testing, replace `10.0.2.2` with your computer's IP address on the local network.

3. For production, use your production API URL.

### Driver Android App

1. Create or update `gradle.properties` in `DriverAndroid/` directory:

```properties
# API Base URL Configuration
# For Android Emulator: http://10.0.2.2:3000/api/
# For Physical Device: http://YOUR_IP_ADDRESS:3000/api/
# For Production: https://your-domain.com/api/
API_BASE_URL=http://10.0.2.2:3000/api/
```

## Files Updated

### Backend
- `backend/routes/products.js` - Uses `API_BASE_URL` for image URLs
- `backend/routes/websites.js` - Uses `API_BASE_URL` for logo and gallery image URLs
- `backend/routes/customers.js` - Uses `API_BASE_URL` and `ANDROID_EMULATOR_URL` for profile picture URLs
- `backend/server.js` - Uses environment variables for console output

### Frontend
- `frontend/vite.config.js` - Uses `VITE_PROXY_TARGET` for proxy configuration
- All Vue components already use `VITE_API_URL` via `import.meta.env.VITE_API_URL`

### Android Apps
- `ResturantAndroid/app/build.gradle.kts` - Added `buildConfigField` for `API_BASE_URL`
- `ResturantAndroid/app/src/main/java/.../network/RetrofitClient.kt` - Uses `BuildConfig.API_BASE_URL`
- `ResturantAndroid/app/src/main/java/.../utils/UrlHelper.kt` - New utility for URL conversion
- All image loading code updated to use `UrlHelper.convertUrlForAndroid()`
- `DriverAndroid/app/build.gradle.kts` - Added `buildConfigField` for `API_BASE_URL`
- `DriverAndroid/app/src/main/java/.../network/RetrofitClient.kt` - Uses `BuildConfig.API_BASE_URL`

## Testing

1. **Backend**: Ensure `.env` file exists and restart the server
2. **Frontend**: Ensure `.env` file exists and restart the dev server
3. **Android**: Update `gradle.properties` and rebuild the app

## Notes

- All hardcoded `localhost:3000` and `10.0.2.2:3000` URLs have been replaced with environment variables
- The Android apps use BuildConfig to inject the API URL at build time
- Image URLs are automatically converted for Android using the `UrlHelper` utility
- For production, ensure all environment variables are set correctly before deployment

