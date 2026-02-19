# PayTabs SDK Integration Code (for sharing / debugging)

**Issue:** After calling `PaymentSdkActivity.startCardPayment()`, the app process ends and restarts (PROCESS ENDED → PROCESS STARTED). PayTabs screen never stays visible; user ends up back at Cart.

**SDK:** `com.paytabs:payment-sdk:6.8.1`

**Credentials:** From `local.properties`: PAYTABS_PROFILE_ID, PAYTABS_SERVER_KEY, PAYTABS_CLIENT_KEY (injected into BuildConfig at build time).

---

## 1. Dependency (app/build.gradle.kts)

```kotlin
implementation("com.paytabs:payment-sdk:6.8.1")
```

---

## 2. Imports (CheckoutActivity.kt)

```kotlin
import com.mnsf.resturantandroid.BuildConfig
import com.payment.paymentsdk.PaymentSdkActivity
import com.payment.paymentsdk.PaymentSdkConfigBuilder
import com.payment.paymentsdk.integrationmodels.PaymentSdkBillingDetails
import com.payment.paymentsdk.integrationmodels.PaymentSdkShippingDetails
import com.payment.paymentsdk.integrationmodels.PaymentSdkLanguageCode
import com.payment.paymentsdk.integrationmodels.PaymentSdkTransactionType
import com.payment.paymentsdk.integrationmodels.PaymentSdkError
import com.payment.paymentsdk.integrationmodels.PaymentSdkTransactionDetails
import com.payment.paymentsdk.sharedclasses.interfaces.CallbackPaymentInterface
```

---

## 3. Activity declaration and pending fields

```kotlin
class CheckoutActivity : AppCompatActivity(), CallbackPaymentInterface {

    // ... other fields ...

    /** Pending order data when PayTabs card payment is in progress (used in onPaymentFinish). */
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
```

---

## 4. Launch PayTabs (full method)

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

    // Amount: 2 decimal places (e.g. 5.999 -> 5.99), min 0.01
    val amountRaw = getOrderTotalAmount()
    var amount = (amountRaw * 100.0).toInt() / 100.0
    if (amount <= 0.0) amount = 0.01
    val currency = currencyCode ?: "JOD"
    val cartId = "order_${restaurantId}_${System.currentTimeMillis()}"
    val cartDesc = getString(R.string.order_summary)

    val billingData = PaymentSdkBillingDetails(
        "N/A",
        "JO",
        customerEmail.ifEmpty { "customer@email.com" },
        customerName,
        customerPhone,
        "N/A",
        customerAddress.ifEmpty { "N/A" },
        "N/A"
    )
    val shippingData = PaymentSdkShippingDetails(
        "N/A",
        "JO",
        customerEmail.ifEmpty { "customer@email.com" },
        customerName,
        customerPhone,
        "N/A",
        customerAddress.ifEmpty { "N/A" },
        "N/A"
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
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            if (isFinishing || isDestroyed) {
                resetPlaceOrderButton()
                return@post
            }
            try {
                PaymentSdkActivity.startCardPayment(this@CheckoutActivity, configData, this@CheckoutActivity)
            } catch (e: Exception) {
                resetPlaceOrderButton()
                Toast.makeText(this@CheckoutActivity, getString(R.string.payment_failed) + ": " + (e.message ?: "Unknown error"), Toast.LENGTH_LONG).show()
            }
        }
    } catch (e: Exception) {
        resetPlaceOrderButton()
        Toast.makeText(this, getString(R.string.payment_failed) + ": " + (e.message ?: "Unknown error"), Toast.LENGTH_LONG).show()
    }
}
```

---

## 5. CallbackPaymentInterface implementation

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
        val customerName = pendingOrderCustomerName ?: "Guest"
        val customerEmail = pendingOrderCustomerEmail
        val customerPhone = pendingOrderCustomerPhone ?: "N/A"
        val customerAddress = pendingOrderCustomerAddress
        val orderType = pendingOrderType ?: "pickup"
        val notes = pendingOrderNotes
        val tip = pendingOrderTip
        val deliveryInstructions = pendingOrderDeliveryInstructions
        val cartItems = pendingOrderCartItems ?: return
        val lat = pendingOrderDeliveryLat
        val lng = pendingOrderDeliveryLng
        // Clear pending
        pendingOrderCustomerName = null
        pendingOrderCustomerEmail = null
        pendingOrderCustomerPhone = null
        pendingOrderCustomerAddress = null
        pendingOrderType = null
        pendingOrderNotes = null
        pendingOrderCartItems = null
        pendingOrderDeliveryLat = null
        pendingOrderDeliveryLng = null
        createOrderWithLocation(
            customerName, customerEmail, customerPhone, customerAddress,
            orderType, "card", notes, lat, lng, cartItems, tip, deliveryInstructions,
            paymentIntentId = transactionRef.ifEmpty { null }
        )
    } else {
        val msg = details.paymentResult?.responseMessage ?: getString(R.string.payment_failed)
        resetPlaceOrderButton()
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
```

---

## 6. When we call launchPayTabsCardPayment

- User selects **Credit/Debit card** (card details card is visible).
- User taps **Place order** → 5-second countdown → then `submitOrderNow()` runs.
- If `paymentMethod == "card"` we set `pendingOrder*` and:
  - **Delivery:** `lifecycleScope.launch { getCurrentLocation() ... runOnUiThread { launchPayTabsCardPayment(lat, lng) } }`
  - **Pickup/Dine-in:** `launchPayTabsCardPayment(null, null)` directly.
- `startCardPayment` is called on the main thread after a 300 ms delay (Handler.postDelayed) so the activity stack can settle. Right after it returns, **PROCESS ENDED** and app restarts (new PID). No FATAL EXCEPTION in our logs; CrashCapture file is empty (suggesting process kill, not uncaught exception).

---

## 7. BuildConfig (from app/build.gradle.kts)

PayTabs keys are read from `local.properties` and written into BuildConfig:

```kotlin
val ptProfile = localPropertiesFile.readLines().firstOrNull { it.startsWith("PAYTABS_PROFILE_ID=") }?.substringAfter("=")?.trim()?.removeSurrounding("\"") ?: ""
val ptServer = ...  // PAYTABS_SERVER_KEY
val ptClient = ...  // PAYTABS_CLIENT_KEY
buildConfigField("String", "PAYTABS_PROFILE_ID", "\"$ptProfile\"")
buildConfigField("String", "PAYTABS_SERVER_KEY", "\"$ptServer\"")
buildConfigField("String", "PAYTABS_CLIENT_KEY", "\"$ptClient\"")
```

---

## 8. AndroidManifest (PayTabs from SDK)

PaymentSdkActivity is declared by the PayTabs AAR (merged into the app manifest). We do not declare it ourselves. It has no `android:process` in the merged manifest (runs in same process).

---

## 9. Possible causes when process dies after startCardPayment

When the app process ends right after `startCardPayment` returns (PROCESS ENDED → PROCESS STARTED, user lands back on Cart), possible causes and things to try:

1. **Full logcat is required**  
   Filtering only `CheckoutPay` hides the real crash/kill. Capture unfiltered logcat around the moment you tap Place Order, then search for:
   - `FATAL EXCEPTION` (stack trace)
   - `AndroidRuntime` (crash)
   - `Process.*killed`
   - `am_proc_died`  
   See `PAYTABS_DEBUG.md` for exact commands.

2. **“Don’t keep activities”**  
   In Developer options, turn **OFF** “Don’t keep activities”. When it’s ON, the system can destroy CheckoutActivity as soon as PayTabs opens, which can contribute to restarts.

3. **Delayed launch**  
   We post `startCardPayment` with a 300 ms delay so the activity stack can settle before the SDK activity is created. If the process still dies, the delay can be increased (e.g. 500 ms) or removed for testing.

4. **Theme / resources**  
   PayTabs uses `@style/PaymentSdkTheme` (parent `Theme.MaterialComponents.Light.NoActionBar`) from the SDK AAR. If your app overrides or strips resources the SDK needs, the payment activity could crash when created. Check merged resources and that you don’t remove PayTabs resources.

5. **ProGuard / R8**  
   In release, ensure PayTabs SDK classes are not stripped or obfuscated. Add keep rules for `com.payment.paymentsdk.**` if you use minify.

6. **Memory / low-end device**  
   The system may kill the process when a heavy SDK activity starts. Try on a device with more free RAM or after closing other apps.

7. **SDK version**  
   We use `com.paytabs:payment-sdk:6.8.1`. Check PayTabs changelog/support for known issues and consider trying a newer patch version if available.

When sharing with ChatGPT or PayTabs support, include: (1) this integration doc, (2) the full unfiltered logcat around PROCESS ENDED, and (3) device/OS and whether “Don’t keep activities” is OFF.
