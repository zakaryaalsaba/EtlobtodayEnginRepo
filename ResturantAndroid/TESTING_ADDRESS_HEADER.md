# Testing: Address type in header ("Deliver to Apartment/House/Office")

The header shows a **short label** so the arrow stays visible: **address_type** (Apartment, House, Office) when you have a saved address, or "Current location" when you used current location only.

## How to see **address_type** in the header

### Option A: Create a new address (recommended)

1. **Log in** to the app (addresses are stored per customer).
2. Open **Home** (restaurants list). The header shows "Deliver to …" or "Deliver to Current location" if you used location before.
3. Tap the **header row** (or the arrow) to open the address bottom sheet.
4. Tap **"Deliver to a different location"**.
5. On the map, tap **"Enter complete address"**.
6. On the **New address** screen:
   - Choose a type: **Apartment**, **House**, or **Office** (chips at top).
   - Fill street (required) and any other fields.
   - Tap **"Save address"**.
7. You are taken to Checkout (or back to Home if you opened from there). The header should now show **"Deliver to Apartment"**, **"Deliver to House"**, or **"Deliver to Office"** depending on the chip you selected.

### Option B: Use an existing saved address

1. **Log in** and ensure the backend has at least one address for your customer with `address_type` set (apartment/house/office).
2. Open **Home** or **Restaurant details**.
3. Tap the **header** to open the address sheet.
4. Under **Saved addresses**, tap one of the listed addresses.
5. The header should update to **"Deliver to Apartment"**, **"Deliver to House"**, or **"Deliver to Office"** (from that address’s `address_type`).

### Option C: "Current location" (no address_type)

1. Tap the header → **"Deliver to current location"** (with location permission granted).
2. The header should show **"Deliver to Current location"** (short label, arrow visible).

## If address_type still does not show

- **Backend**: Ensure the backend is running and the `addresses` table exists with an `address_type` column (values: `apartment`, `house`, `office`). GET `/api/customers/:customerId/addresses` should return `address_type` for each address.
- **Logged out**: Without an account, only "Current location" or the placeholder ("…") can appear; there are no saved addresses with a type.
- **Old session**: If the app had saved a long address string as the label before, you may need to select a saved address again (Option B) or add a new address (Option A) so the header gets the short `address_type` label.
- **Clear app data**: As a last resort, clear app data and log in again, then add a new address (Option A).
