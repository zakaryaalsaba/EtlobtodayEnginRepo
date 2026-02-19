# Company Delivery Approval Process

## Overview
The Company Delivery feature allows restaurants to partner with delivery companies for zone-based delivery services. This document explains how restaurants get approved to use a delivery company's services.

## Current Implementation

### Database Structure
- **`delivery_companies`** table: Stores delivery company information
- **`restaurant_websites`** table: Contains `delivery_company_id` field (NULL if not approved)
- **`store_delivery_requests`** table: Tracks restaurant requests to partner with delivery companies
  - Status: `pending`, `approved`, `rejected`

### Approval Flow

#### Step 1: Restaurant Requests Partnership
1. Restaurant admin goes to **Order Type Settings > Delivery Company** tab
2. Selects a delivery company from the dropdown
3. Clicks "Request Partnership" or similar action
4. System creates a record in `store_delivery_requests` table with status `pending`

#### Step 2: Delivery Company Reviews Request
1. Delivery company admin logs into their dashboard
2. Views pending requests in the "Store Requests" section
3. Reviews restaurant details (name, location, area, etc.)
4. Can approve or reject the request

#### Step 3: Approval Process
**When Approved:**
- Delivery company admin clicks "Approve" on the request
- System updates `store_delivery_requests.status` to `approved`
- System sets `restaurant_websites.delivery_company_id` to the approved company's ID
- Restaurant can now use the delivery company's zones

**When Rejected:**
- Delivery company admin clicks "Reject" on the request
- System updates `store_delivery_requests.status` to `rejected`
- Restaurant's `delivery_company_id` remains NULL
- Restaurant can submit a new request later

### Current API Endpoints

#### Restaurant Side (Admin Dashboard)
- `GET /api/restaurant/delivery-company-request` - Get current request status
- `POST /api/restaurant/delivery-company-request` - Submit new request
- `DELETE /api/restaurant/delivery-company-request/:id` - Cancel request

#### Delivery Company Side (Admin Dashboard)
- `GET /api/delivery-company/store-requests` - List all store requests
- `PATCH /api/delivery-company/store-requests/:id` - Approve/reject request

## What Needs to Be Done for Approval

### For Restaurant Admin:
1. **Navigate to Order Type Settings**
   - Go to Restaurant Dashboard
   - Click on "Order Type Settings" tab
   - Navigate to "Delivery Company" section

2. **Select Delivery Company**
   - View list of available delivery companies
   - Select the company you want to partner with
   - Click "Request Partnership" or "Send Request"

3. **Wait for Approval**
   - Request status will show as "Pending"
   - You'll be notified when the delivery company reviews your request
   - Once approved, you can start using their zones

### For Delivery Company Admin:
1. **Review Store Requests**
   - Log into Delivery Company Dashboard
   - Navigate to "Store Requests" section
   - View pending requests with restaurant details

2. **Evaluate Request**
   - Check restaurant location and area coverage
   - Verify restaurant is in a zone you serve
   - Review restaurant profile and details

3. **Approve or Reject**
   - Click "Approve" to accept the partnership
   - Click "Reject" to decline (restaurant can request again later)
   - Optionally add notes/comments about the decision

### System Behavior After Approval

Once a restaurant is approved:
- `restaurant_websites.delivery_company_id` is set
- Restaurant can access delivery company's zones
- Customers see Region/Area/Zone dropdowns when ordering
- Delivery fees are calculated from zone prices
- Orders can be assigned to delivery company drivers

## Future Enhancements

### Multiple Delivery Companies
Currently, a restaurant can only partner with one delivery company at a time. Future enhancements could include:
- Multiple delivery company partnerships per restaurant
- Restaurant chooses which company to use per order
- Dynamic pricing comparison between companies

### Additional Approval Features
- Approval workflow with multiple approval levels
- Contract/agreement management
- Commission/fee structure configuration
- Service level agreements (SLAs)
- Performance metrics and reporting

## Technical Notes

### Zone Price Usage
- When a customer selects a zone during address entry, the `zone_price` is saved in the `addresses` table
- In checkout, the delivery fee is calculated from `addresses.zone_price` if available
- Falls back to `restaurant_websites.delivery_fee` if no zone_price is found
- This ensures accurate pricing based on the selected delivery zone

### Data Flow
1. Customer selects Region → Area → Zone in NewAddressActivity
2. Zone price is saved with the address (`addresses.zone_price`)
3. CheckoutActivity loads the customer's default address
4. Delivery fee is calculated from `zone_price` if restaurant uses delivery company
5. Order is placed with the correct delivery fee
