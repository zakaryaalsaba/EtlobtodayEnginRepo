# Order Workflow Documentation

## Complete Order Lifecycle

### 1. Order Placement (Customer)
**Status:** `pending`

**Location:** `backend/routes/orders.js` - `POST /api/orders`

**Process:**
1. Customer adds items to cart and proceeds to checkout
2. Customer provides:
   - Name, phone, email (optional)
   - Order type: `dine_in`, `pickup`, or `delivery`
   - Delivery address (if delivery)
   - Payment method
   - Coupon code (optional)
3. System calculates:
   - Subtotal (sum of all items)
   - Discount (if coupon applied)
   - Tax (if enabled for restaurant)
   - Delivery fee (if delivery order type)
   - Total amount
4. Order is created with:
   - Unique order number (e.g., `ORD-MK3ZCT1O-IRH0`)
   - Status: `pending`
   - Payment status: `pending` or `paid` (if online payment)
5. Order items are saved to `order_items` table
6. If coupon used, coupon usage count is incremented
7. Notifications sent:
   - SSE broadcast to restaurant admin dashboard (real-time)
   - Email notification to restaurant owner (if enabled)
   - Customer notification created (if customer_id exists)

---

### 2. Order Confirmation (Restaurant Admin)
**Status:** `confirmed`

**Location:** `backend/routes/orders.js` - `PUT /api/orders/:id/status`

**Process:**
1. Restaurant admin views new order in dashboard
2. Admin confirms the order
3. Status changes from `pending` → `confirmed`
4. Customer receives notification (if registered)
5. For delivery orders: Order becomes available for drivers to accept

**Valid Status Transitions:**
- `pending` → `confirmed`
- `pending` → `cancelled`

---

### 3. Order Preparation (Restaurant Admin)
**Status:** `preparing`

**Location:** `backend/routes/orders.js` - `PUT /api/orders/:id/status`

**Process:**
1. Restaurant starts preparing the order
2. Admin updates status: `confirmed` → `preparing`
3. Customer receives notification
4. For delivery orders: Driver can still accept at this stage

**Valid Status Transitions:**
- `confirmed` → `preparing`
- `confirmed` → `cancelled`

---

### 4. Order Ready (Restaurant Admin)
**Status:** `ready`

**Location:** `backend/routes/orders.js` - `PUT /api/orders/:id/status`

**Process:**
1. Order is ready for pickup/delivery
2. Admin updates status: `preparing` → `ready`
3. Customer receives notification
4. For pickup orders: Customer can now pick up
5. For delivery orders: Driver should pick up from restaurant

**Valid Status Transitions:**
- `preparing` → `ready`
- `preparing` → `cancelled`

---

### 5. Order Completion

#### For Pickup Orders:
**Status:** `completed`

**Location:** `backend/routes/orders.js` - `PUT /api/orders/:id/status`

**Process:**
1. Customer arrives and picks up order
2. Payment collected (if not paid online)
3. Admin marks order as `ready` → `completed`
4. Order is moved to archive

#### For Dine-in Orders:
**Status:** `completed`

**Process:**
1. Customer receives order at table
2. Payment collected
3. Admin marks order as `completed`
4. Order is moved to archive

#### For Delivery Orders (Driver Workflow):
**Status:** `completed` (mapped from driver status `delivered`)

**Location:** `backend/routes/driverOrders.js` - `PUT /api/orders/:orderId/status`

**Driver Status Flow:**
1. **Driver Accepts Order**
   - Order status: `pending`/`confirmed` → `confirmed`
   - Driver assigned to order (`driver_id` set)
   - Location: `POST /api/orders/:orderId/accept`

2. **Driver Arrives at Pickup**
   - Driver status: `arrived_at_pickup`
   - Order status: Remains `confirmed` (or `preparing` if restaurant still preparing)
   - Location: `PUT /api/orders/:orderId/status` with `status: "arrived_at_pickup"`

3. **Driver Picks Up Order**
   - Driver status: `picked_up`
   - Order status: Maps to `preparing` (or `ready` if already ready)
   - Location: `PUT /api/orders/:orderId/status` with `status: "picked_up"`

4. **Driver On The Way**
   - Driver status: `on_the_way`
   - Order status: Maps to `ready`
   - Location: `PUT /api/orders/:orderId/status` with `status: "on_the_way"`

5. **Driver Delivers Order**
   - Driver status: `delivered`
   - Order status: Maps to `completed`
   - Location: `PUT /api/orders/:orderId/status` with `status: "delivered"`
   - Order is moved to archive

---

## Order Status Reference

### Main Order Statuses (in database)
These are stored in the `orders` table:

| Status | Description | Who Sets It | When |
|--------|-------------|-------------|------|
| `pending` | Order just placed, awaiting confirmation | System (auto) | Order creation |
| `confirmed` | Restaurant confirmed the order | Restaurant Admin | After reviewing order |
| `preparing` | Restaurant is preparing the order | Restaurant Admin | When kitchen starts |
| `ready` | Order is ready for pickup/delivery | Restaurant Admin | When order is complete |
| `completed` | Order is finished (picked up or delivered) | Restaurant Admin / Driver | Final status |
| `cancelled` | Order was cancelled | Restaurant Admin | If order cannot be fulfilled |

### Driver-Specific Statuses (mapped to order statuses)
These are used by drivers but mapped to main statuses:

| Driver Status | Maps To Order Status | Description |
|---------------|---------------------|-------------|
| `arrived_at_pickup` | `confirmed` | Driver arrived at restaurant |
| `picked_up` | `preparing` | Driver picked up order from restaurant |
| `on_the_way` | `ready` | Driver is delivering to customer |
| `delivered` | `completed` | Driver delivered to customer |

**Note:** Driver statuses are temporary and map to the main order statuses. The database only stores the main statuses.

---

## Differences: Completed vs Picked-up vs Delivered

### `completed` (Final Status)
- **What it means:** The order is fully finished, regardless of order type
- **Used for:** All order types (dine-in, pickup, delivery)
- **Set by:** Restaurant admin (for pickup/dine-in) or Driver (for delivery)
- **When:** After customer receives the order
- **Database:** Stored in `orders.status` column

### `picked_up` (Driver Status Only)
- **What it means:** Driver has collected the order from the restaurant
- **Used for:** Delivery orders only
- **Set by:** Driver via driver app
- **When:** Driver physically picks up the order from restaurant
- **Database:** Maps to `preparing` or `ready` in `orders.status`
- **Note:** This is NOT a final status - order continues to delivery

### `delivered` (Driver Status Only)
- **What it means:** Driver has delivered the order to the customer
- **Used for:** Delivery orders only
- **Set by:** Driver via driver app
- **When:** Driver completes delivery to customer location
- **Database:** Maps to `completed` in `orders.status`
- **Note:** This is the final status for delivery orders

---

## Status Transition Diagram

```
Order Placement
     ↓
[pending]
     ↓
Restaurant Confirms
     ↓
[confirmed]
     ↓
Restaurant Prepares
     ↓
[preparing]
     ↓
Order Ready
     ↓
[ready]
     ↓
┌─────────────────┬──────────────────┐
│                 │                  │
Pickup/Dine-in    Delivery (Driver)   │
│                 │                  │
│                 Driver Accepts     │
│                 │                  │
│                 [confirmed]        │
│                 │                  │
│                 Driver Arrives     │
│                 [arrived_at_pickup]│
│                 │                  │
│                 Driver Picks Up    │
│                 [picked_up]       │
│                 │                  │
│                 Driver On Way     │
│                 [on_the_way]      │
│                 │                  │
│                 Driver Delivers   │
│                 [delivered]       │
│                 │                  │
└─────────────────┴──────────────────┘
     ↓
[completed]
     ↓
Order Archived
```

---

## Key Files

### Order Management
- `backend/routes/orders.js` - Main order CRUD operations
- `backend/routes/driverOrders.js` - Driver-specific order operations
- `backend/db/schema.sql` - Database schema with status enum

### Frontend
- `frontend/src/components/AdminDashboard.vue` - Restaurant admin order management
- `frontend/src/components/WebsiteBuilder.vue` - Super admin order view
- `frontend/src/components/OrderTracking.vue` - Customer order tracking

### Android Apps
- `ResturantAndroid/.../OrderConfirmationActivity.kt` - Order confirmation screen
- `ResturantAndroid/.../OrderHistoryActivity.kt` - Order history
- `DriverAndroid/...` - Driver order management

---

## Important Notes

1. **No Separate "Picked-up" Status in Database:**
   - The database only stores: `pending`, `confirmed`, `preparing`, `ready`, `completed`, `cancelled`
   - Driver statuses (`picked_up`, `delivered`) are mapped to these main statuses

2. **"Completed" is Universal:**
   - `completed` is the final status for ALL order types
   - For delivery, it's set when driver marks as `delivered`
   - For pickup/dine-in, it's set by restaurant admin

3. **Driver Status Mapping:**
   - Driver statuses are for UX in the driver app
   - They map to order statuses for consistency
   - The mapping is in `driverOrders.js` lines 378-388

4. **Real-time Updates:**
   - SSE (Server-Sent Events) broadcasts status changes to admin dashboard
   - Customer notifications are created for each status change
   - Push notifications sent to customer app (if registered)

5. **Order Archiving:**
   - Completed orders are moved to archive in admin dashboard
   - Only today's active orders shown by default
   - Archive view shows all completed orders

