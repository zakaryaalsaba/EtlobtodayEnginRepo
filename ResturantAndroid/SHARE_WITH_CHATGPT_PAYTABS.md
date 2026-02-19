# PayTabs Android SDK – process dies right after startCardPayment (share with ChatGPT)

## Problem

We use **PayTabs SDK 6.8.1** in an Android app (Kotlin). When the user selects “Credit/Debit card” and taps “Place order”, we call `PaymentSdkActivity.startCardPayment()`. The method **returns normally**, but immediately after that the **app process ends** (PROCESS ENDED) and a **new process starts** (PROCESS STARTED). The PayTabs payment screen never stays visible; the user ends up back at the Cart. We do **not** see a FATAL EXCEPTION in our filtered logs, and our app’s `UncaughtExceptionHandler` does not log anything (no crash file), so it may be a **process kill** rather than an uncaught Java exception.

## Logs (CheckoutPay tag)

```
[0] countdown onFinish - calling submitOrderNow
[1] submitOrderNow called, thread=main
[2] cartItems.size=1
[3] orderType=delivery
[4] cardCardDetails.visibility=0 (0=VISIBLE), cardDetailsVisible=true -> paymentMethod=card
[4b] rbCash=false rbCard=true rbOnline=false rbMobile=false
[5] ENTERED CARD BRANCH - will launch PayTabs
[5b] PayTabs keys: profileId=true serverKey=true clientKey=true
[6a] orderType=delivery, checking location
[6b] has location permission, launching coroutine for getCurrentLocation
[6c] inside coroutine, calling getCurrentLocation
[7] card branch done, returning (PayTabs should have opened)
[6d] delivery runOnUiThread: isFinishing=false isDestroyed=false
[6e] launching PayTabs (delivery)
[9] launchPayTabsCardPayment called, isFinishing=false isDestroyed=false
[9d] pending data ok, cartItems=1
[10] amount=5.99 (raw=5.999...) currency=JOD cartId=order_12_...
[11] posting PaymentSdkActivity.startCardPayment in 300ms
[11c] calling PaymentSdkActivity.startCardPayment NOW (pid=4344)
[12] startCardPayment returned (pid=4344, PayTabs screen should be visible)
---------------------------- PROCESS ENDED (4344) for package com.mnsf.resturantandroid ----------------------------
---------------------------- PROCESS STARTED (4435) for package com.mnsf.resturantandroid ----------------------------
```

So: `startCardPayment()` is called on the main thread (after a 300 ms delay), it **returns**, we log `[12]`, then the process with that PID ends and a new one starts.

## Environment

- **SDK:** `com.paytabs:payment-sdk:6.8.1`
- **App:** minSdk 26, targetSdk 35, Kotlin, AndroidX
- **Credentials:** PAYTABS_PROFILE_ID, PAYTABS_SERVER_KEY, PAYTABS_CLIENT_KEY from `local.properties` → BuildConfig (all non-empty in logs)
- **Manifest:** `PaymentSdkActivity` comes from the PayTabs AAR (merged); no `android:process` (same process). Theme: `@style/PaymentSdkTheme` (from SDK, parent `Theme.MaterialComponents.Light.NoActionBar`)

We have **not** been able to capture full unfiltered logcat (e.g. `adb logcat > paytabs_full.txt`) at the moment of PROCESS ENDED, so we don’t have a FATAL EXCEPTION stack trace or `am_proc_died` line to share.

## Code we use

### 1. Dependency (app/build.gradle.kts)

```kotlin
implementation("com.paytabs:payment-sdk:6.8.1")
```

We also force `androidx.core:core:1.15.0` (and core-ktx) because PayTabs pulled in 1.17.0 which required compileSdk 36.

### 2. Activity implements CallbackPaymentInterface

```kotlin
class CheckoutActivity : AppCompatActivity(), CallbackPaymentInterface {
    // Pending order data used in onPaymentFinish
    private var pendingOrderCustomerName: String? = null
    private var pendingOrderCustomerEmail: String? = null
    private var pendingOrderCustomerPhone: String? = null
    private var pendingOrderCustomerAddress: String? = null
    private var pendingOrderType: String? = null
    private var pendingOrderNotes: String? = null
    private var pendingOrderDeliveryLat: Double? = null
    private var pendingOrderDeliveryLng: Double? = null
    private var pendingOrderCartItems: List<CartItem>? = null
    private var pendingOrderTip: Double = 0.0
    private var pendingOrderDeliveryInstructions: String? = null
    // ...
}
```

### 3. How we call PayTabs (after 5s countdown, payment method = card)

For **delivery**: we get location in a coroutine, then `runOnUiThread { launchPayTabsCardPayment(lat, lng) }`.  
For **pickup/dine-in**: we call `launchPayTabsCardPayment(null, null)` directly.

### 4. launchPayTabsCardPayment (full method)

```kotlin
private fun launchPayTabsCardPayment(deliveryLat: Double?, deliveryLng: Double?) {
    if (isFinishing || isDestroyed) {
        resetPlaceOrderButton()
        return
    }
    val cartItems = pendingOrderCartItems ?: run {
        resetPlaceOrderButton()
        Toast.makeText(this, getString(R.string.cart_empty), Toast.LENGTH_SHORT).show()
        return
    }
    val customerName = pendingOrderCustomerName ?: "Guest"
    val customerEmail = pendingOrderCustomerEmail ?: ""
    val customerPhone = pendingOrderCustomerPhone ?: "N/A"
    val customerAddress = pendingOrderCustomerAddress ?: ""
    val orderType = pendingOrderType ?: "pickup"
    val notes = pendingOrderNotes
    val tip = pendingOrderTip
    val deliveryInstructions = pendingOrderDeliveryInstructions

    val amountRaw = getOrderTotalAmount()
    var amount = (amountRaw * 100.0).toInt() / 100.0
    if (amount <= 0.0) amount = 0.01
    val currency = currencyCode ?: "JOD"
    val cartId = "order_${restaurantId}_${System.currentTimeMillis()}"
    val cartDesc = getString(R.string.order_summary)

    val billingData = PaymentSdkBillingDetails(
        "N/A", "JO",
        customerEmail.ifEmpty { "customer@email.com" },
        customerName, customerPhone, "N/A",
        customerAddress.ifEmpty { "N/A" }, "N/A"
    )
    val shippingData = PaymentSdkShippingDetails(
        "N/A", "JO",
        customerEmail.ifEmpty { "customer@email.com" },
        customerName, customerPhone, "N/A",
        customerAddress.ifEmpty { "N/A" }, "N/A"
    )

    val configData = PaymentSdkConfigBuilder(
        BuildConfig.PAYTABS_PROFILE_ID,
        BuildConfig.PAYTABS_SERVER_KEY,
        BuildConfig.PAYTABS_CLIENT_KEY,
        amount,  // Double
        currency
    )
        .setCartDescription(cartDesc)
        .setLanguageCode(PaymentSdkLanguageCode.EN)
        .setBillingData(billingData)
        .setMerchantCountryCode("JO")
        .setShippingData(shippingData)
        .setCartId(cartId)
        .setTransactionType(PaymentSdkTransactionType.SALE)
        .showBillingInfo(false)
        .showShippingInfo(false)
        .build()

    try {
        val payTabsDelayMs = 300L
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (isFinishing || isDestroyed) {
                resetPlaceOrderButton()
                return@postDelayed
            }
            try {
                PaymentSdkActivity.startCardPayment(this@CheckoutActivity, configData, this@CheckoutActivity)
                // We log here after return; then PROCESS ENDED happens
            } catch (e: Exception) {
                resetPlaceOrderButton()
                Toast.makeText(this@CheckoutActivity, getString(R.string.payment_failed) + ": " + (e.message ?: "Unknown error"), Toast.LENGTH_LONG).show()
            }
        }, payTabsDelayMs)
    } catch (e: Exception) {
        resetPlaceOrderButton()
        Toast.makeText(this, getString(R.string.payment_failed) + ": " + (e.message ?: "Unknown error"), Toast.LENGTH_LONG).show()
    }
}
```

### 5. Callbacks (CallbackPaymentInterface)

```kotlin
override fun onError(error: PaymentSdkError) {
    Log.e("CheckoutActivity", "PayTabs onError: ${error.msg}")
    resetPlaceOrderButton()
    Toast.makeText(this, getString(R.string.payment_failed) + ": " + error.msg, Toast.LENGTH_LONG).show()
}

override fun onPaymentCancel() {
    Log.d("CheckoutActivity", "PayTabs onPaymentCancel")
    resetPlaceOrderButton()
    Toast.makeText(this, getString(R.string.payment_cancelled), Toast.LENGTH_SHORT).show()
}

override fun onPaymentFinish(details: PaymentSdkTransactionDetails) {
    if (details.isSuccess == true) {
        val transactionRef = details.transactionReference ?: ""
        // ... read pendingOrder* ...
        createOrderWithLocation(..., paymentIntentId = transactionRef.ifEmpty { null })
    } else {
        val msg = details.paymentResult?.responseMessage ?: getString(R.string.payment_failed)
        resetPlaceOrderButton()
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
```

## What we’ve already tried

- Ensuring “Don’t keep activities” is **OFF** in Developer options.
- Posting `startCardPayment` with a **300 ms delay** (Handler.postDelayed) so the activity stack can settle.
- Passing **amount** as `Double` with 2 decimal places (min 0.01).
- Setting a **default UncaughtExceptionHandler** in Application; it never runs (no crash file written).
- Checking that PayTabs activity runs in the **same process** (no `android:process` in merged manifest).

## Official PayTabs demo (for comparison)

Repo: **[paytabs-store-demo-android](https://github.com/paytabscom/paytabs-store-demo-android)**

- **SDK version in demo:** `com.paytabs:payment-sdk:6.2.0-beta01` (we use **6.8.1**).
- **Demo uses a Fragment**, not an Activity: `BagFragment` implements `CallbackPaymentInterface` and calls:
  ```kotlin
  PaymentSdkActivity.startCardPayment(requireActivity(), configData, this)
  ```
  So first argument is the **host Activity** (`requireActivity()`), third is the **Fragment** (`this`). We pass the **Activity** for both.
- **Demo build:** compileSdk 30, targetSdk 30 (we use 35).
- **Demo config** includes we don’t set:
  - `.setTransactionClass(PaymentSdkTransactionClass.ECOM)`
  - `.setScreenTitle("Pay with Card")`
- **Demo** calls `startCardPayment` **directly** on button click (no Handler delay).
- **Demo** reads profile/server/client keys from **SharedPreferences** (settings screen); we use BuildConfig from `local.properties`.

If the demo runs without process kill on similar devices, the difference may be SDK version (6.2 vs 6.8), Activity vs Fragment as callback, or config (TransactionClass / ScreenTitle).

---

## Question for ChatGPT

Why would the **process end** immediately after `PaymentSdkActivity.startCardPayment(activity, configData, activity)` returns, with no FATAL EXCEPTION in our logs and no uncaught exception in our handler? What are the most likely causes (e.g. PayTabs activity theme/resources, native crash, system killing the process, SDK bug), and what concrete steps or code changes do you recommend to fix or work around it? If we cannot capture full logcat, what should we try next (e.g. different launch flags, theme overrides, SDK version, or debugging steps)? We have compared with the [official PayTabs store demo](https://github.com/paytabscom/paytabs-store-demo-android) (see section above)—should we try matching the demo’s SDK version (6.2.0-beta01), adding `setTransactionClass`/`setScreenTitle`, or launching from a Fragment instead of an Activity?
