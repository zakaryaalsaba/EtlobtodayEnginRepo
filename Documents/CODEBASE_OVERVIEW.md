# RestaurantEngin - Codebase Overview

## Project Type
Multi-platform restaurant management system with:
- **Web frontend** (Vue.js)
- **Backend API** (Node.js/Express)
- **Android apps** (Customer, Driver, Order Management)
- **iOS app** (Customer)
- **Order management system**
- **Website builder engine**

## Architecture

```
RestaurantEngin/
├── backend/              # Node.js/Express API server
├── frontend/             # Vue.js web application
├── ResturantAndroid/     # Android customer app (Kotlin)
├── DriverAndroid/        # Android driver app (Kotlin)
├── OrderManageAndroid/   # Android order management app (Kotlin)
├── RestaurantIOS/       # iOS customer app (Swift)
└── Documents/            # Project documentation
```

---

## Backend (`/backend`)

### Tech Stack:
- **Node.js** (ES Modules)
- **Express.js**
- **MySQL2** (Database)
- **Firebase Admin SDK** (Push notifications)
- **JWT authentication**
- **Multer** (File uploads)
- **Stripe** (Payment processing)
- **Nodemailer** (Email notifications)

### Key Routes:
- `/api/websites` - Website builder CRUD operations
- `/api/products` - Product/menu management
- `/api/orders` - Order management (admin & customer)
- `/api/auth` - Authentication endpoints
- `/api/admin` - Restaurant admin operations
- `/api/drivers` - Driver management
- `/api/driverOrders` - Driver order operations
- `/api/customers` - Customer management
- `/api/payments` - Payment processing
- `/api/coupons` - Coupon/discount system
- `/api/super-admin` - Super admin operations
- `/api/restaurant` - Restaurant profile management (includes location)

### Key Features:
- **Domain-based routing middleware** - Multi-tenant support
- **Rate limiting** - API protection
- **SSE (Server-Sent Events)** - Real-time updates for admin dashboard
- **Push notifications** - Firebase Cloud Messaging integration
- **File upload handling** - Logo, menu images, gallery
- **Database auto-initialization** - Schema creation and migrations
- **Location support** - Latitude/longitude for restaurants

### Database Schema:
- `restaurant_websites` - Restaurant configurations (includes `latitude`, `longitude`)
- `products` - Menu items/products
- `orders` - Order records (includes `tax`, `delivery_fees`)
- `order_items` - Order line items
- `customers` - Customer accounts
- `drivers` - Driver accounts (includes location tracking)
- `coupons` - Discount coupons
- `payments` - Payment transactions

---

## Frontend (`/frontend`)

### Tech Stack:
- **Vue.js 3** (Composition API)
- **Vue Router** - Navigation
- **Vue i18n** - Arabic/English localization
- **Tailwind CSS** - Styling
- **Axios** - HTTP client
- **QR Code generation** - Restaurant QR codes

### Key Components:
- `LandingPage.vue` - Customer-facing restaurant website
- `AdminDashboard.vue` - Restaurant admin dashboard
- `WebsiteBuilder.vue` - Super admin website builder
- `Checkout.vue` - Order checkout
- `OrderTracking.vue` - Order status tracking
- `RestaurantLogin.vue` - Restaurant admin login
- `SuperAdminLogin.vue` - Super admin login

---

## Android Apps

### 1. ResturantAndroid (Customer App)
**Tech Stack:** Kotlin, Material Design 3, MVVM Architecture

**Features:**
- Customer authentication (email/password)
- Restaurant browsing and search
- Menu browsing by category
- Shopping cart management
- Order placement and tracking
- Order history
- Firebase integration
- Multi-language support (Arabic/English)

**Key Components:**
- `LoginActivity` / `RegisterActivity`
- `RestaurantListFragment`
- `RestaurantDetailsFragment`
- `CartFragment`
- `CheckoutActivity`
- `OrderHistoryFragment`

**Testing credit card (PayTabs):**
1. **Enable card for the restaurant:** In the web app, open the **Restaurant Dashboard** (restaurant owner) → **Payment Methods** tab → turn on **Credit/Debit Card** and save. The Android app only shows the card option when the restaurant’s `payment_methods` has `creditCard: true`.
2. **Configure PayTabs:** In `ResturantAndroid/local.properties` set:
   - `PAYTABS_PROFILE_ID=...`
   - `PAYTABS_SERVER_KEY=...`
   - `PAYTABS_CLIENT_KEY=...`
   (Get these from the PayTabs dashboard.) Rebuild the app after changing `local.properties`.
3. **Flow:** Add items to cart → Checkout → select **Credit/Debit card** → tap **Place order** → wait for the **5-second countdown** (button shows "Cancel (5)", "Cancel (4)", …; tap again to cancel). When the countdown finishes, the PayTabs card entry screen opens. After a successful payment, the order is created with the transaction reference and the cart is cleared.

**Debugging checkout / PayTabs (logs):** The app logs the checkout and PayTabs flow with tag **`CheckoutPay`**. To capture logs and send them for debugging:
- **Android Studio:** Run the app, reproduce the issue, then in Logcat filter by `CheckoutPay` and copy the output.
- **Terminal:** `adb logcat -s CheckoutPay` (or `adb logcat | grep CheckoutPay`).  
Log sequence: `[0]` countdown finished → `[1]` submitOrderNow → `[4]` paymentMethod and cardDetailsVisible → `[5]` card branch / `[8]` cash path → `[9]` launchPayTabs → `[11]` startCardPayment. If you see `[8]` and `[createOrder]` after selecting card, the app is taking the cash path (visibility or radio state wrong). If you see `[11]` and `[12]` but then **PROCESS ENDED** and the app restarts (back to Cart), the process was killed right after PayTabs started—capture **full logcat** (no filter, or filter by `AndroidRuntime`) around that time to see the crash/exception (e.g. FATAL EXCEPTION). The amount sent to PayTabs is rounded to 2 decimal places to avoid SDK issues.

---

### 2. DriverAndroid (Driver App)
**Tech Stack:** Kotlin, Material Design 3, MVVM Architecture

**Features:**
- Driver authentication
- Available orders list
- Active orders management
- Order acceptance/rejection
- Real-time order updates
- Push notifications
- Location tracking
- Delivery fees display (green card)
- Payment method display (red card)
- Currency formatting from database

**Key Components:**
- `LoginActivity`
- `AvailableOrdersActivity`
- `ActiveOrdersActivity`
- `OrderDetailActivity`
- `ProfileActivity` (with profile image upload)

**Recent Updates:**
- Modern Material Design 3 UI
- Profile management with image upload
- Currency-aware order display
- Delivery fees and payment method highlighting

---

### 3. OrderManageAndroid (Order Management App)
**Tech Stack:** Kotlin, Material Design 3, MVVM Architecture, Material You

**Features:**
- Restaurant admin authentication
- Order list dashboard (pending/active orders)
- Order details with full breakdown
- Order status management (accept/reject/update)
- Statistics dashboard (total orders, revenue, date filtering)
- Restaurant profile management
- **Location management** (latitude/longitude with GPS)
- Publish/unpublish restaurant toggle
- Navigation drawer
- Language switching (Arabic/English)
- Currency formatting from database
- Real-time order updates (WebSocket)
- Push notifications (Firebase)
- Printer integration (Bluetooth ESC/POS)

**Key Activities:**
- `LoginActivity` - Admin authentication
- `DashboardActivity` - Main order list with navigation drawer
- `OrderDetailActivity` - Order details with breakdown (Subtotal, Tax, Delivery Fee, Total)
- `StatisticsActivity` - Orders and revenue statistics with date filtering
- `ProfileActivity` - Restaurant profile management with location

**Key ViewModels:**
- `DashboardViewModel` - Order list management
- `OrderDetailViewModel` - Order details and status updates
- `StatisticsViewModel` - Statistics calculation and filtering
- `ProfileViewModel` - Restaurant profile CRUD
- `LoginViewModel` - Authentication

**Key Utilities:**
- `CurrencyFormatter` - Formats amounts based on `currency_code` and `currency_symbol_position` from database
- `LocaleHelper` - Language switching support
- `SessionManager` - JWT token and session management

**Recent Updates:**
- **Profile Management:**
  - Restaurant profile viewing and editing
  - Address, phone, email management
  - Publish/unpublish toggle
  - **Location support:** Latitude/longitude fields
  - **GPS integration:** "Get Current Location" button with permission handling
  - Auto-prompt for location if missing
  
- **Order Details:**
  - Detailed breakdown: Subtotal, Tax, Delivery Fee (conditional), Total
  - Order status on separate line below order number
  - Rejection confirmation dialog before reason selection
  - Currency-aware formatting
  
- **Statistics Dashboard:**
  - Total orders and revenue
  - Date range filtering
  - Orders by status breakdown
  - Currency-aware revenue display
  
- **Navigation Drawer:**
  - Current Orders (main activity)
  - Statistics
  - Profile
  - Language switching
  - Sign out

**Services:**
- `RealTimeOrderService` - WebSocket connection for live order updates
- `OrderFirebaseMessagingService` - Push notification handling
- `PrinterService` - Bluetooth ESC/POS printing

---

## iOS App (`/RestaurantIOS`)

### Tech Stack:
- **Swift**
- **SwiftUI**
- **Firebase integration**
- **Phone authentication**

### Key Features:
- Customer ordering
- Cart management
- Order tracking
- Multi-language support (Arabic/English)
- Restaurant browsing
- Menu display

### Architecture:
- **MVVM** (Model-View-ViewModel)
- **URLSession** for networking
- **UserDefaults** for session management

---

## Order Status Flow

```
pending → confirmed → preparing → ready → completed
            ↓
         (delivery: driver workflow)
```

**Status Transitions:**
- `pending` - Initial order state
- `confirmed` - Restaurant accepted
- `preparing` - Kitchen preparing
- `ready` - Ready for pickup/delivery
- `completed` - Order fulfilled
- `cancelled` - Order rejected/cancelled

---

## Key Features Across Platform

### Multi-tenant Website Builder
- Create customizable restaurant websites
- Logo upload and branding
- Color customization
- Menu management
- Social links integration

### Order Management
- End-to-end order lifecycle
- Real-time status updates
- Multi-channel notifications
- Order history tracking

### Driver Delivery System
- Driver assignment
- Location tracking
- Order acceptance workflow
- Delivery fee calculation

### Payment Integration
- Stripe payment processing
- Multiple payment methods
- Payment history

### Push Notifications
- Firebase Cloud Messaging
- Order status updates
- Driver notifications
- Admin alerts

### Multi-language Support
- Arabic/English
- RTL support (Android)
- Language switching in-app

### Currency Support
- Database-driven currency codes
- Symbol position (before/after)
- Multi-currency formatting
- Custom symbol mapping (JOD, USD, SAR, etc.)

### Location Services
- Restaurant location storage (latitude/longitude)
- GPS integration (OrderManageAndroid)
- Location permission handling
- Address management

---

## Development Setup

### Backend:
```bash
cd backend
npm install
cp .env.example .env
# Configure MySQL credentials
npm start
```

### Frontend:
```bash
cd frontend
npm install
npm run dev
```

### Android Apps:
```bash
cd [AppName]
./gradlew build
# Open in Android Studio
```

### Database:
- **MySQL 8.0+**
- Auto-initialization on first run
- Schema in `backend/db/schema.sql`
- Migrations in `backend/db/init.js`

---

## API Base URLs

- **Local:** `http://localhost:3000`
- **Android Emulator:** `http://10.0.2.2:3000`
- **Health Check:** `/health`

---

## Documentation

Located in `/Documents`:
- `PROJECT_STRUCTURE.md` - Architecture overview
- `SETUP.md` - Setup instructions
- `ORDER_WORKFLOW.md` - Order lifecycle documentation
- `FIREBASE_SETUP_GUIDE.md` - Firebase configuration
- `ENV_CONFIGURATION.md` - Environment variables
- `CODEBASE_OVERVIEW.md` - This file

---

## Recent Major Updates

### OrderManageAndroid (Latest)
1. **Profile Management System**
   - Restaurant profile viewing/editing
   - Publish/unpublish toggle
   - Location management (GPS integration)
   - Address, phone, email updates

2. **Statistics Dashboard**
   - Separate activity for orders/revenue
   - Date range filtering
   - Status breakdown

3. **Order Details Enhancements**
   - Detailed financial breakdown
   - Improved status display
   - Rejection confirmation flow

4. **Currency Formatting**
   - Database-driven currency display
   - Custom symbol mapping
   - Position-aware formatting

5. **Navigation Drawer**
   - Modern Material Design 3 drawer
   - Centralized navigation
   - Language switching

---

## Technology Highlights

- **Backend:** RESTful API with JWT authentication
- **Real-time:** WebSocket (OrderManageAndroid), SSE (Frontend)
- **Notifications:** Firebase Cloud Messaging
- **Payments:** Stripe integration
- **Database:** MySQL with auto-migrations
- **Mobile:** Native Android (Kotlin) and iOS (Swift)
- **Web:** Vue.js 3 with Composition API
- **UI:** Material Design 3 (Android), SwiftUI (iOS), Tailwind CSS (Web)

---

This is a full-stack restaurant management platform with web, Android, and iOS clients, supporting ordering, delivery, payments, website building, and comprehensive order management.
