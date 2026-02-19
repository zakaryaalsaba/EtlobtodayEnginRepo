# PayTabs Web API Server Key Setup

## Problem
When processing saved card payments server-to-server, you're getting this error:
```
Request must be type application/octet-stream
```

This happens because you're using a **Mobile SDK server key** for server-to-server API calls. PayTabs requires different keys for different purposes:
- **Mobile SDK keys**: For Android/iOS SDK (client-side)
- **Web API keys**: For server-to-server API calls (backend)

## Solution: Get Web API Server Key

### Step 1: Log in to PayTabs Dashboard
1. Go to: https://dashboard.paytabs.com/
2. Log in with your merchant account

### Step 2: Navigate to API Keys
1. Click on **"Developers"** in the main menu
2. Select **"API Keys"** or **"Key Management"**
3. Look for **"Web API"** section (not Mobile SDK)

### Step 3: Get Web API Server Key
1. In the **Web API** section, find your **Server Key**
2. It should look like: `SKJ9DJTWB2-JM26GTZHB6-2BMG9GZZKN` (but different)
3. **Important**: This is different from your Mobile SDK server key
4. Copy the entire server key

### Step 4: Add to Backend .env File
1. Open your backend `.env` file:
   ```bash
   cd /Users/zakaryaalsaba/Desktop/RestaurantEngin/backend
   nano .env
   ```
   (or use any text editor)

2. Add this line (replace with your actual Web API server key):
   ```bash
   PAYTABS_SERVER_KEY=your_web_api_server_key_here
   ```

3. Save the file

### Step 5: Restart Backend
```bash
npm start
```

You should see:
```
✅ Using PayTabs Web API server key (length): 32 from environment variable
```

## Verification

After setting up, test a saved card payment. The backend logs should show:
- ✅ Using PayTabs Web API server key from environment variable
- No more "application/octet-stream" errors
- Successful payment processing

## Troubleshooting

### Still getting "application/octet-stream" error?
- Make sure you're using the **Web API** server key, not the Mobile SDK key
- Verify the key is correctly set in `.env` file (no extra spaces, quotes, etc.)
- Restart the backend server after changing `.env`

### Can't find Web API keys in dashboard?
- Contact PayTabs support to enable Web API keys for your account
- Some accounts may need to request Web API access separately

### Key format
- Web API server keys are typically 32 characters
- Format: `SKJ9DJTWB2-JM26GTZHB6-2BMG9GZZKN` (with hyphens)
- Mobile SDK keys have the same format but are different keys

## Security Note
- Never commit `.env` file to git (it's already in `.gitignore`)
- Keep your Web API server key secure
- Use different keys for development and production
