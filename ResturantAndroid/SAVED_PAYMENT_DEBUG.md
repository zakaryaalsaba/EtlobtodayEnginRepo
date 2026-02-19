# Saved Payment Methods - Debugging Guide

## Important: No Database Changes Made

**I did NOT make any database schema changes.** This feature uses **local storage only** (Android SharedPreferences). Cards are saved locally on the device, not in your backend database.

## Changes Made

### 1. New Files Created
- `app/src/main/java/com/mnsf/resturantandroid/data/model/SavedPaymentMethod.kt` - Data model
- `app/src/main/java/com/mnsf/resturantandroid/util/PaymentMethodManager.kt` - Manager class

### 2. CheckoutActivity Updates
- Added `PaymentMethodManager` initialization (line ~111)
- Added `selectedSavedPaymentMethod` variable (line ~72)
- Updated PayTabs config to enable tokenization (lines ~1058-1080)
- Updated `onPaymentFinish` to extract and save token (lines ~1144-1200)
- Added `loadSavedPaymentMethods()` function (lines ~421-440)

## Why Cards Might Not Be Saving

### Issue 1: Tokenization Not Enabled
**Check Logcat for:**
```
✅ Tokenization enabled via setTokenise(true)
```
**OR**
```
❌ Could not enable tokenization - no method found
```

**Solution:** PayTabs SDK version might not support tokenization, or method name is different.

### Issue 2: Token Not in Response
**Check Logcat for:**
```
✅ SUCCESS: Saved payment method: ****1234
```
**OR**
```
❌ FAILED: Token or cardLast4 not available
```

**Solution:** Tokenization might not be enabled in your PayTabs merchant account.

### Issue 3: PaymentMethodManager Not Initialized
**Check Logcat for:**
```
PaymentMethodManager: Saved payment method: 1234, total methods: 1
```

## Debugging Steps

### Step 1: Check Tokenization Enablement
After clicking "Make Payment", check Logcat filter `CheckoutPay`:
- Look for "Tokenization enabled" message
- If you see "Could not enable tokenization", the SDK method name might be wrong

### Step 2: Check Token Extraction
After successful payment, check Logcat filter `PayTabsResult`:
- Look for "Attempting to extract payment method details..."
- Check "Available methods" and "Available fields" logs
- Look for "✅ SUCCESS" or "❌ FAILED" messages

### Step 3: Verify Card Was Saved
After payment, check Logcat filter `PaymentMethodManager`:
- Look for "=== DEBUG: Saved Payment Methods ==="
- Should show saved cards with last 4 digits

### Step 4: Manual Test
Add this code temporarily in `onCreate()` to test PaymentMethodManager:
```kotlin
// Test PaymentMethodManager
val testMethod = SavedPaymentMethod(
    token = "test_token_123",
    cardLast4Digits = "1234",
    cardBrand = "Visa"
)
paymentMethodManager.savePaymentMethod(testMethod)
paymentMethodManager.debugPrintSavedMethods()
```

## Common Issues & Solutions

### Issue: "Token or cardLast4 not available"
**Cause:** PayTabs is not returning token in response
**Solutions:**
1. Enable tokenization in PayTabs merchant dashboard
2. Check PayTabs SDK version - tokenization might require specific version
3. Verify PayTabs account has tokenization feature enabled

### Issue: "Could not enable tokenization"
**Cause:** SDK method name doesn't match
**Solutions:**
1. Check PayTabs SDK documentation for correct method name
2. Try different method names: `setTokenise`, `setTokenize`, `setToken`
3. Check SDK version compatibility

### Issue: Cards saved but not showing in UI
**Cause:** UI not implemented yet
**Solution:** Complete `loadSavedPaymentMethods()` function to display cards

## Testing Checklist

- [ ] Payment completes successfully
- [ ] Logcat shows "Tokenization enabled" message
- [ ] Logcat shows "Available methods" with token-related methods
- [ ] Logcat shows "✅ SUCCESS: Saved payment method"
- [ ] Logcat shows "=== DEBUG: Saved Payment Methods ===" with saved cards
- [ ] PaymentMethodManager.debugPrintSavedMethods() shows saved cards

## Next Steps

1. **Run a test payment** and check Logcat for all the debug messages
2. **Share the Logcat output** so we can see:
   - Is tokenization being enabled?
   - What methods/fields are available in PaymentSdkTransactionDetails?
   - Is token being extracted?
   - Is PaymentMethodManager saving the card?

3. **Check PayTabs Merchant Account:**
   - Login to PayTabs dashboard
   - Check if tokenization feature is enabled
   - Verify SDK version supports tokenization

4. **If tokenization works but cards aren't showing:**
   - Complete the UI implementation (see SAVED_PAYMENT_METHODS.md)
   - Call `loadSavedPaymentMethods()` and display cards

## Files Modified

- `CheckoutActivity.kt` - Main integration
- `PaymentMethodManager.kt` - Storage manager
- `SavedPaymentMethod.kt` - Data model

## Files NOT Modified (No Database Changes)

- No backend database schema changes
- No API endpoints added
- No database migrations

All data is stored locally using Android SharedPreferences.
