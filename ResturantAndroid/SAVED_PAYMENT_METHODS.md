# Saved Payment Methods Implementation

## Overview
This feature allows customers to save their credit card information after the first PayTabs payment, enabling faster checkout on subsequent orders.

## What's Implemented

### 1. Data Models
- **`SavedPaymentMethod.kt`**: Data class to store tokenized card information
  - `token`: PayTabs token for recurring payments
  - `cardLast4Digits`: Last 4 digits of the card
  - `cardBrand`: Card brand (Visa, Mastercard, etc.)
  - `cardExpiry`: Card expiry date (optional)
  - `savedAt`: Timestamp when saved

### 2. Payment Method Manager
- **`PaymentMethodManager.kt`**: Manages saved payment methods
  - Saves/loads payment methods from SharedPreferences
  - Supports up to 5 saved cards
  - Uses Gson for serialization
  - Methods: `savePaymentMethod()`, `getSavedPaymentMethods()`, `removePaymentMethod()`, `clearAllPaymentMethods()`

### 3. CheckoutActivity Updates
- Added `PaymentMethodManager` instance
- Added `selectedSavedPaymentMethod` to track selected saved card
- Enabled tokenization in PayTabs config (for new cards)
- Extracts token and card info from PayTabs response after successful payment
- Saves payment method automatically after first successful payment
- Uses saved token for subsequent payments (when selected)

## What Still Needs to Be Done

### 1. UI for Saved Cards
You need to add UI elements in `activity_checkout.xml` to display saved cards:

**Option A: Using RecyclerView (Recommended)**
```xml
<!-- Add this inside cardPaymentMethod, before rbCard -->
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/recyclerViewSavedCards"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="16dp"
    android:visibility="gone"
    tools:listitem="@layout/item_saved_card" />
```

**Option B: Using LinearLayout with RadioButtons**
```xml
<LinearLayout
    android:id="@+id/layoutSavedCards"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:visibility="gone">
    
    <!-- Saved cards will be added here programmatically -->
    
    <RadioButton
        android:id="@+id/rbUseNewCard"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Use new card"
        android:checked="true" />
</LinearLayout>
```

### 2. Create Saved Card Item Layout
Create `app/src/main/res/layout/item_saved_card.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<RadioButton xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="16dp"
    android:text="Card ending in XXXX"
    android:drawableEnd="@drawable/ic_delete"
    android:drawablePadding="8dp" />
```

### 3. Update loadSavedPaymentMethods()
Complete the `loadSavedPaymentMethods()` function in `CheckoutActivity.kt` to:
- Display saved cards in the UI
- Handle selection of saved card vs new card
- Show delete option for saved cards
- Update `selectedSavedPaymentMethod` when a card is selected

### 4. PayTabs SDK Token Extraction
The current implementation tries to extract token using reflection. You may need to adjust based on your PayTabs SDK version:

**Check PayTabs SDK version and adjust:**
- SDK 6.6.2+: Uses `setTokenise(true)` and `getToken()`
- Older versions: May use different method names

**To verify token extraction, check logs:**
- Look for `PayTabsResult` logs after payment
- If token is null, check PayTabs SDK documentation for correct property names

### 5. Testing Steps

1. **First Payment (New Card)**:
   - Select "Credit/Debit Card"
   - Complete payment via PayTabs
   - Check logs for "Saved payment method: ****XXXX"
   - Verify card is saved in SharedPreferences

2. **Second Payment (Saved Card)**:
   - Select saved card from UI
   - Payment should use token (no PayTabs screen)
   - Or if PayTabs screen appears, verify token is passed

3. **Delete Saved Card**:
   - Add delete button/icon in saved card UI
   - Call `paymentMethodManager.removePaymentMethod(token)`
   - Refresh UI

## Important Notes

1. **Tokenization**: PayTabs tokenization must be enabled in your PayTabs merchant account
2. **Security**: Tokens are stored locally. Consider encrypting sensitive data
3. **SDK Version**: Token extraction methods may vary by PayTabs SDK version
4. **Backend Integration**: You may want to also save tokens on your backend for cross-device sync

## Code Locations

- Data Model: `app/src/main/java/com/mnsf/resturantandroid/data/model/SavedPaymentMethod.kt`
- Manager: `app/src/main/java/com/mnsf/resturantandroid/util/PaymentMethodManager.kt`
- Integration: `app/src/main/java/com/mnsf/resturantandroid/ui/checkout/CheckoutActivity.kt`
  - Lines: ~54 (PaymentMethodManager initialization)
  - Lines: ~72 (selectedSavedPaymentMethod variable)
  - Lines: ~1001-1025 (PayTabs config with tokenization)
  - Lines: ~1066-1100 (Payment finish handler with token extraction)
  - Lines: ~421-440 (loadSavedPaymentMethods function)

## Next Steps

1. Add UI elements to display saved cards
2. Complete `loadSavedPaymentMethods()` implementation
3. Test token extraction with your PayTabs SDK version
4. Add delete functionality for saved cards
5. Consider adding card expiry validation
