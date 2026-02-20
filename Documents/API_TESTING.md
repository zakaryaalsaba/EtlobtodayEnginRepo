# Testing Your Restaurant Backend API

**Base URL:** `https://restaurantwebsitebuilder-q58fe.ondigitalocean.app`

---

## 1. Quick check (browser or curl)

**Health check** (no auth):

- Browser: open  
  **https://restaurantwebsitebuilder-q58fe.ondigitalocean.app/health**
- Or in terminal:
  ```bash
  curl https://restaurantwebsitebuilder-q58fe.ondigitalocean.app/health
  ```
- Expected: `{"status":"ok","timestamp":"..."}`

---

## 2. Public API endpoints (no login)

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/health` | Server health |
| GET | `/api/websites` | List restaurant websites |
| GET | `/api/websites/:id` | Get one website by ID |
| GET | `/api/websites/domain/:domain` | Get website by subdomain/domain |
| GET | `/api/products/website/:websiteId` | Products for a website |
| GET | `/api/settings` | Public settings |

**Examples:**

```bash
# List websites
curl https://restaurantwebsitebuilder-q58fe.ondigitalocean.app/api/websites

# Get website by ID (use a real ID from the list above)
curl https://restaurantwebsitebuilder-q58fe.ondigitalocean.app/api/websites/1

# Products for website 1
curl https://restaurantwebsitebuilder-q58fe.ondigitalocean.app/api/products/website/1

# Public settings
curl https://restaurantwebsitebuilder-q58fe.ondigitalocean.app/api/settings
```

---

## 3. Auth-required endpoints

Most `/api/admin/*`, `/api/restaurant/*`, `/api/orders`, etc. require a token.

**Admin login** (get token):

```bash
curl -X POST https://restaurantwebsitebuilder-q58fe.ondigitalocean.app/api/admin/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"yourpassword"}'
```

Use the `token` from the response in later requests:

```bash
curl https://restaurantwebsitebuilder-q58fe.ondigitalocean.app/api/admin/me \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

**Customer auth:** `POST /api/auth/login` or `POST /api/auth/login/phone`  
**Driver auth:** `POST /api/drivers/login`

---

## 4. Using Postman / Insomnia

1. Create an environment with:
   - `baseUrl` = `https://restaurantwebsitebuilder-q58fe.ondigitalocean.app`
2. For health: `GET {{baseUrl}}/health`
3. For APIs: `GET {{baseUrl}}/api/websites`, etc.
4. For protected routes: add header `Authorization: Bearer <token>` after logging in.

---

## 5. Mobile / Store app

Set the API base URL in your app config to:

`https://restaurantwebsitebuilder-q58fe.ondigitalocean.app`

No path suffix (e.g. the app will call `baseUrl + "/api/websites"`).
