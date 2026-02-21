# Stores Web – eCommerce Frontend

Vue 3 eCommerce frontend for the RestaurantEngin backend. Browse stores, products, cart, checkout (guest or registered), and order tracking.

## Stack

- **Vue 3** + **Vite**
- **Vue Router** – Stores, Store detail, Product detail, Cart, Checkout, Orders, Order tracking, Login, Register
- **Pinia** – Auth and Cart state
- **Vue I18n** – English & Arabic with RTL support
- **Tailwind CSS** – Styling and responsive layout

## Setup

```bash
cd StoresWeb
npm install
```

## Environment

Create `.env` (or copy from `.env.example`):

```env
VITE_API_BASE_URL=http://localhost:3000
```

For local dev with Vite proxy, you can leave it empty so requests go to same origin and Vite proxies `/api` to the backend.

## Run

```bash
# Development (proxy to backend at localhost:3000)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

## Deploy (DigitalOcean)

See **[Documents/STORESWEB_DEPLOYMENT.md](../Documents/STORESWEB_DEPLOYMENT.md)** for Static Site setup, build command, env vars (`VITE_API_BASE_URL`, `VITE_BASE_PATH`), and routing.

## Features

- **Stores** – List all stores from API
- **Store detail** – Products per store
- **Product detail** – Add to cart with quantity
- **Cart** – Update quantity, remove items
- **Checkout** – Guest or registered; place order
- **Order tracking** – View order by number and status
- **My Orders** – For logged-in users
- **Auth** – Login / Register; token persisted
- **i18n** – EN / AR with RTL layout and language switcher

## Backend

Uses the existing backend at `../backend`:

- `GET /api/websites` – stores
- `GET /api/websites/:id` – store detail
- `GET /api/products/website/:websiteId` – products
- `GET /api/products/:id` – product detail
- `POST /api/orders` – create order
- `GET /api/orders/:orderNumber` – order by number
- `GET /api/customers/:customerId/orders` – customer orders
- `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`

Ensure the backend is running (e.g. `npm run dev` in `backend`) and CORS allows the frontend origin.
