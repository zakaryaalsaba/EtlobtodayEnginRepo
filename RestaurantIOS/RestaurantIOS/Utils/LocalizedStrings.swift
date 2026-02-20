//
//  LocalizedStrings.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import Foundation

struct LocalizedStrings {
    private static let languageManager = LanguageManager.shared
    
    // MARK: - Common
    static var close: String {
        languageManager.currentLanguage == .arabic ? "إغلاق" : "Close"
    }
    
    static var cancel: String {
        languageManager.currentLanguage == .arabic ? "إلغاء" : "Cancel"
    }
    
    static var save: String {
        languageManager.currentLanguage == .arabic ? "حفظ" : "Save"
    }
    
    static var loading: String {
        languageManager.currentLanguage == .arabic ? "جاري التحميل..." : "Loading..."
    }
    
    // MARK: - Auth
    static var login: String {
        languageManager.currentLanguage == .arabic ? "تسجيل الدخول" : "Login"
    }
    
    static var register: String {
        languageManager.currentLanguage == .arabic ? "التسجيل" : "Register"
    }
    
    static var logout: String {
        languageManager.currentLanguage == .arabic ? "تسجيل الخروج" : "Logout"
    }
    
    static var email: String {
        languageManager.currentLanguage == .arabic ? "البريد الإلكتروني" : "Email"
    }
    
    static var password: String {
        languageManager.currentLanguage == .arabic ? "كلمة المرور" : "Password"
    }
    
    static var name: String {
        languageManager.currentLanguage == .arabic ? "الاسم" : "Name"
    }
    
    static var phone: String {
        languageManager.currentLanguage == .arabic ? "الهاتف" : "Phone"
    }
    
    static var phoneNumberHint: String {
        languageManager.currentLanguage == .arabic ? "7XXXXXXXX" : "7XXXXXXXX"
    }
    
    static var address: String {
        languageManager.currentLanguage == .arabic ? "العنوان" : "Address"
    }
    
    // MARK: - Phone Authentication
    static var signInWithPhone: String {
        languageManager.currentLanguage == .arabic ? "تسجيل الدخول بالهاتف" : "Sign in with Phone"
    }
    
    static var or: String {
        languageManager.currentLanguage == .arabic ? "أو" : "OR"
    }
    
    static var enterPhoneNumber: String {
        languageManager.currentLanguage == .arabic ? "أدخل رقم هاتفك" : "Enter your phone number"
    }
    
    static var sendVerificationCode: String {
        languageManager.currentLanguage == .arabic ? "إرسال رمز التحقق" : "Send Verification Code"
    }
    
    static var verificationCodeSent: String {
        languageManager.currentLanguage == .arabic ? "تم إرسال رمز التحقق" : "Verification code sent"
    }
    
    static var enterVerificationCode: String {
        languageManager.currentLanguage == .arabic ? "أدخل رمز التحقق" : "Enter verification code"
    }
    
    static var verificationCodeHint: String {
        languageManager.currentLanguage == .arabic ? "123456" : "123456"
    }
    
    static var verifyCode: String {
        languageManager.currentLanguage == .arabic ? "التحقق من الرمز" : "Verify Code"
    }
    
    static var resendCode: String {
        languageManager.currentLanguage == .arabic ? "إعادة إرسال الرمز" : "Resend Code"
    }
    
    static var invalidPhoneNumber: String {
        languageManager.currentLanguage == .arabic ? "رقم هاتف غير صالح. يرجى إدخال رقم هاتف صالح مع رمز الدولة." : "Invalid phone number. Please enter a valid phone number with country code."
    }
    
    static var phoneVerificationFailed: String {
        languageManager.currentLanguage == .arabic ? "فشل التحقق من الهاتف. يرجى المحاولة مرة أخرى." : "Phone verification failed. Please try again."
    }
    
    static var codeVerificationFailed: String {
        languageManager.currentLanguage == .arabic ? "فشل التحقق من الرمز. يرجى التحقق من الرمز والمحاولة مرة أخرى." : "Code verification failed. Please check the code and try again."
    }
    
    static var phoneAuthSuccessful: String {
        languageManager.currentLanguage == .arabic ? "تم التحقق من الهاتف بنجاح!" : "Phone authentication successful!"
    }
    
    static var verifying: String {
        languageManager.currentLanguage == .arabic ? "جارٍ التحقق…" : "Verifying…"
    }
    
    static var sendingCode: String {
        languageManager.currentLanguage == .arabic ? "جارٍ إرسال الرمز…" : "Sending code…"
    }
    
    static var phoneAuthNotEnabled: String {
        languageManager.currentLanguage == .arabic ? "لم يتم تفعيل التحقق بالهاتف. يرجى الاتصال بالدعم أو استخدام تسجيل الدخول بالبريد الإلكتروني/كلمة المرور." : "Phone authentication is not enabled. Please contact support or use email/password login."
    }
    
    // MARK: - Profile
    static var profile: String {
        languageManager.currentLanguage == .arabic ? "الملف الشخصي" : "Profile"
    }
    
    static var language: String {
        languageManager.currentLanguage == .arabic ? "اللغة" : "Language"
    }
    
    static var pleaseLoginToViewProfile: String {
        languageManager.currentLanguage == .arabic ? "يرجى تسجيل الدخول لعرض ملفك الشخصي" : "Please login to view your profile"
    }
    
    static var user: String {
        languageManager.currentLanguage == .arabic ? "مستخدم" : "User"
    }
    
    // MARK: - Restaurants
    static var restaurants: String {
        languageManager.currentLanguage == .arabic ? "المطاعم" : "Restaurants"
    }
    
    static var restaurant: String {
        languageManager.currentLanguage == .arabic ? "مطعم" : "Restaurant"
    }
    
    static var loadingRestaurants: String {
        languageManager.currentLanguage == .arabic ? "جاري تحميل المطاعم..." : "Loading restaurants..."
    }
    
    static var noRestaurantsFound: String {
        languageManager.currentLanguage == .arabic ? "لم يتم العثور على مطاعم" : "No restaurants found"
    }
    
    static var searchRestaurants: String {
        languageManager.currentLanguage == .arabic ? "البحث عن المطاعم" : "Search restaurants"
    }
    
    // MARK: - Cart
    static var cart: String {
        languageManager.currentLanguage == .arabic ? "السلة" : "Cart"
    }
    
    static var viewCart: String {
        languageManager.currentLanguage == .arabic ? "عرض السلة" : "View Cart"
    }
    
    static var yourCartIsEmpty: String {
        languageManager.currentLanguage == .arabic ? "سلة التسوق فارغة" : "Your cart is empty"
    }
    
    static var addItemsFromMenu: String {
        languageManager.currentLanguage == .arabic ? "أضف عناصر من القائمة للبدء" : "Add items from the menu to get started"
    }
    
    static var total: String {
        languageManager.currentLanguage == .arabic ? "المجموع" : "Total"
    }
    
    static var items: String {
        languageManager.currentLanguage == .arabic ? "عناصر" : "items"
    }
    
    static var proceedToCheckout: String {
        languageManager.currentLanguage == .arabic ? "المتابعة إلى الدفع" : "Proceed to Checkout"
    }
    
    static var addedToCart: String {
        languageManager.currentLanguage == .arabic ? "تمت الإضافة إلى السلة" : "Added to cart"
    }
    
    static var paymentSummary: String {
        languageManager.currentLanguage == .arabic ? "ملخص الدفع" : "Payment summary"
    }
    
    static var salesTax: String {
        languageManager.currentLanguage == .arabic ? "ضريبة المبيعات" : "Sales tax"
    }
    
    static var totalAmount: String {
        languageManager.currentLanguage == .arabic ? "المبلغ الإجمالي" : "Total amount"
    }
    
    static var placeOrderEarnStamp: String {
        languageManager.currentLanguage == .arabic ? "قدّم طلبك واحصل على ختم" : "Place your order and earn a stamp"
    }
    
    static var cartEmptyTitle: String {
        languageManager.currentLanguage == .arabic ? "سلة التسوق فارغة" : "Your cart is empty"
    }
    
    static var cartEmptyMessage: String {
        languageManager.currentLanguage == .arabic ? "ابدأ بإضافة أشهى الأصناف من القائمة" : "Start adding delicious items from the menu"
    }
    
    static var addItems: String {
        languageManager.currentLanguage == .arabic ? "إضافة عناصر" : "Add items"
    }
    
    static var edit: String {
        languageManager.currentLanguage == .arabic ? "تعديل" : "Edit"
    }
    
    static var subtotal: String {
        languageManager.currentLanguage == .arabic ? "المجموع الفرعي" : "Subtotal"
    }
    
    // MARK: - Checkout
    static var checkout: String {
        languageManager.currentLanguage == .arabic ? "الدفع" : "Checkout"
    }
    
    static var customerInformation: String {
        languageManager.currentLanguage == .arabic ? "معلومات العميل" : "Customer Information"
    }
    
    static var customerName: String {
        languageManager.currentLanguage == .arabic ? "اسم العميل" : "Customer Name"
    }
    
    static var customerEmail: String {
        languageManager.currentLanguage == .arabic ? "بريد العميل الإلكتروني" : "Customer Email"
    }
    
    static var customerPhone: String {
        languageManager.currentLanguage == .arabic ? "هاتف العميل" : "Customer Phone"
    }
    
    static var deliveryAddress: String {
        languageManager.currentLanguage == .arabic ? "عنوان التسليم" : "Delivery Address"
    }
    
    static var orderType: String {
        languageManager.currentLanguage == .arabic ? "نوع الطلب" : "Order Type"
    }
    
    static var dineIn: String {
        languageManager.currentLanguage == .arabic ? "تناول الطعام في المطعم" : "Dine-in"
    }
    
    static var pickup: String {
        languageManager.currentLanguage == .arabic ? "الاستلام" : "Pick up"
    }
    
    static var delivery: String {
        languageManager.currentLanguage == .arabic ? "التوصيل" : "Delivery"
    }
    
    static var paymentMethod: String {
        languageManager.currentLanguage == .arabic ? "طريقة الدفع" : "Payment Method"
    }
    
    static var cash: String {
        languageManager.currentLanguage == .arabic ? "نقدي" : "Cash"
    }
    
    static var creditCard: String {
        languageManager.currentLanguage == .arabic ? "بطاقة ائتمانية" : "Credit Card"
    }
    
    static var notes: String {
        languageManager.currentLanguage == .arabic ? "ملاحظات" : "Notes"
    }
    
    static var optional: String {
        languageManager.currentLanguage == .arabic ? "(اختياري)" : "(Optional)"
    }
    
    static var placeOrder: String {
        languageManager.currentLanguage == .arabic ? "تقديم الطلب" : "Place Order"
    }
    
    static var sayThanksWithTip: String {
        languageManager.currentLanguage == .arabic ? "اشكر سائقك بإكرامية" : "Say thanks with a tip"
    }
    
    static var yourRiderKeepsTips: String {
        languageManager.currentLanguage == .arabic ? "الإكرامية تذهب بالكامل للسائق" : "Your rider keeps 100% of tips"
    }
    
    static var tipCustom: String {
        languageManager.currentLanguage == .arabic ? "مبلغ آخر" : "Custom"
    }
    
    static var tipCustomHint: String {
        languageManager.currentLanguage == .arabic ? "المبلغ" : "Amount"
    }
    
    static var apply: String {
        languageManager.currentLanguage == .arabic ? "تطبيق" : "Apply"
    }
    
    static var readyForPickupApprox: String {
        languageManager.currentLanguage == .arabic ? "جاهز للاستلام خلال حوالى 10 دقائق" : "Ready for pick up in approx. 10 min"
    }
    
    static func arrivingInApproxMins(_ min: Int, _ max: Int) -> String {
        languageManager.currentLanguage == .arabic ? "الوصول خلال حوالى \(min)–\(max) دقيقة" : "Arriving in approx. \(min) – \(max) mins"
    }
    
    static var deliveryInstructions: String {
        languageManager.currentLanguage == .arabic ? "تعليمات التوصيل" : "Delivery instructions"
    }
    
    static var callOnArrival: String {
        languageManager.currentLanguage == .arabic ? "اتصل عند الوصول" : "Call on arrival"
    }
    
    static var dontRingBell: String {
        languageManager.currentLanguage == .arabic ? "لا ترن الجرس" : "Don't ring bell"
    }
    
    static var leaveAtReception: String {
        languageManager.currentLanguage == .arabic ? "اترك عند الاستقبال" : "Leave at reception"
    }
    
    static var ringDoorbell: String {
        languageManager.currentLanguage == .arabic ? "رن الجرس" : "Ring doorbell"
    }
    
    static var useMyInstructionsForAddress: String {
        languageManager.currentLanguage == .arabic ? "استخدم تعليماتي لهذا العنوان" : "Use my instructions for this address"
    }
    
    static var payWith: String {
        languageManager.currentLanguage == .arabic ? "الدفع بـ" : "Pay with"
    }
    
    static var protectedByPci: String {
        languageManager.currentLanguage == .arabic ? "محمي بمعيار أمان بيانات PCI" : "Protected by PCI Data Security Standard"
    }
    
    static var riderTip: String {
        languageManager.currentLanguage == .arabic ? "إكرامية السائق" : "Rider tip"
    }
    
    static var tipGoesToDriver: String {
        languageManager.currentLanguage == .arabic ? "الإكرامية تذهب بالكامل للسائق" : "This tip goes 100% to the driver"
    }
    
    static var pickupDiscount: String {
        languageManager.currentLanguage == .arabic ? "خصم الاستلام" : "Pickup discount"
    }
    
    static var serviceFee: String {
        languageManager.currentLanguage == .arabic ? "رسوم الخدمة" : "Service fee"
    }
    
    static var serviceFeeInfo: String {
        languageManager.currentLanguage == .arabic ? "رسوم خدمة إضافية قد تطبق حسب المطعم." : "An additional service fee may apply as set by the restaurant."
    }
    
    static var change: String {
        languageManager.currentLanguage == .arabic ? "تغيير" : "Change"
    }
    
    static var mobileNumber: String {
        languageManager.currentLanguage == .arabic ? "رقم الجوال" : "Mobile number"
    }
    
    static var notesOptional: String {
        languageManager.currentLanguage == .arabic ? "ملاحظات (اختياري)" : "Notes (optional)"
    }
    
    static var welcomeGift: String {
        languageManager.currentLanguage == .arabic ? "هدية ترحيب" : "Welcome gift"
    }
    
    // MARK: - Orders
    static var orders: String {
        languageManager.currentLanguage == .arabic ? "الطلبات" : "Orders"
    }
    
    static var orderHistory: String {
        languageManager.currentLanguage == .arabic ? "سجل الطلبات" : "Order History"
    }
    
    static var orderNumber: String {
        languageManager.currentLanguage == .arabic ? "رقم الطلب" : "Order Number"
    }
    
    static var orderConfirmed: String {
        languageManager.currentLanguage == .arabic ? "تم تأكيد الطلب!" : "Order Confirmed!"
    }
    
    static var orderPlacedSuccessfully: String {
        languageManager.currentLanguage == .arabic ? "تم تقديم طلبك بنجاح" : "Your order has been placed successfully"
    }
    
    static var status: String {
        languageManager.currentLanguage == .arabic ? "الحالة" : "Status"
    }
    
    static var pending: String {
        languageManager.currentLanguage == .arabic ? "قيد الانتظار" : "Pending"
    }
    
    static var active: String {
        languageManager.currentLanguage == .arabic ? "نشط" : "Active"
    }
    
    static var archive: String {
        languageManager.currentLanguage == .arabic ? "أرشيف" : "Archive"
    }
    
    static var noOrdersFound: String {
        languageManager.currentLanguage == .arabic ? "لم يتم العثور على طلبات" : "No orders found"
    }
    
    static var viewAllOrders: String {
        languageManager.currentLanguage == .arabic ? "عرض جميع الطلبات" : "View all Orders"
    }
    
    static var trackOrder: String {
        languageManager.currentLanguage == .arabic ? "تتبع الطلب" : "Track Order"
    }
    
    static var reorder: String {
        languageManager.currentLanguage == .arabic ? "إعادة الطلب" : "Reorder"
    }
    
    // MARK: - Additional
    static var retry: String {
        languageManager.currentLanguage == .arabic ? "إعادة المحاولة" : "Retry"
    }
    
    static var namePlaceholder: String {
        languageManager.currentLanguage == .arabic ? "الاسم" : "Name"
    }
    
    static var emailPlaceholder: String {
        languageManager.currentLanguage == .arabic ? "البريد الإلكتروني (اختياري)" : "Email (Optional)"
    }
    
    static var phonePlaceholder: String {
        languageManager.currentLanguage == .arabic ? "الهاتف" : "Phone"
    }
    
    static var deliveryAddressPlaceholder: String {
        languageManager.currentLanguage == .arabic ? "عنوان التسليم" : "Delivery Address"
    }
    
    static var tax: String {
        languageManager.currentLanguage == .arabic ? "الضريبة" : "Tax"
    }
    
    static var deliveryFee: String {
        languageManager.currentLanguage == .arabic ? "رسوم التوصيل" : "Delivery Fee"
    }
    
    static var backToHome: String {
        languageManager.currentLanguage == .arabic ? "العودة إلى الصفحة الرئيسية" : "Back to Home"
    }
    
    static var viewOrders: String {
        languageManager.currentLanguage == .arabic ? "عرض الطلبات" : "View Orders"
    }
    
    // MARK: - Home
    static var home: String {
        languageManager.currentLanguage == .arabic ? "الرئيسية" : "Home"
    }
    
    static var freeDelivery: String {
        languageManager.currentLanguage == .arabic ? "توصيل مجاني" : "Free Delivery"
    }
    
    static var offers: String {
        languageManager.currentLanguage == .arabic ? "العروض" : "Offers"
    }
    
    static var popularRestaurants: String {
        languageManager.currentLanguage == .arabic ? "المطاعم الشائعة" : "Popular Restaurants"
    }
    
    static var allRestaurants: String {
        languageManager.currentLanguage == .arabic ? "جميع المطاعم" : "All Restaurants"
    }
    
    static var viewAll: String {
        languageManager.currentLanguage == .arabic ? "عرض الكل" : "View all"
    }
    
    // MARK: - Restaurant Details / Menu
    static var trending: String {
        languageManager.currentLanguage == .arabic ? "الرائج" : "Trending"
    }
    
    static var noMenuItemsAvailable: String {
        languageManager.currentLanguage == .arabic ? "لا توجد عناصر قائمة متاحة" : "No menu items available"
    }
    
    static var checkBackLaterForMenu: String {
        languageManager.currentLanguage == .arabic ? "تحقق لاحقاً من عناصر القائمة" : "Check back later for menu items"
    }
    
    static var deliveryTimePlaceholder: String {
        languageManager.currentLanguage == .arabic ? "— دقيقة" : "— mins"
    }
    
    static var clearCart: String {
        languageManager.currentLanguage == .arabic ? "تفريغ السلة" : "Clear cart"
    }
    
    static var cartContainsDifferentRestaurant: String {
        languageManager.currentLanguage == .arabic ? "السلة تحتوي على عناصر من مطعم آخر. هل تريد تفريغ السلة وإضافة هذا العنصر؟" : "Your cart has items from another restaurant. Clear cart and add this item?"
    }
    
    static var clearCartAndAdd: String {
        languageManager.currentLanguage == .arabic ? "تفريغ السلة والإضافة" : "Clear cart and add"
    }
    
    static var addItem: String {
        languageManager.currentLanguage == .arabic ? "إضافة عنصر" : "Add item"
    }
    
    static var noDescriptionAvailable: String {
        languageManager.currentLanguage == .arabic ? "لا يوجد وصف" : "No description available"
    }
    
    static var shareRestaurant: String {
        languageManager.currentLanguage == .arabic ? "مشاركة المطعم" : "Share restaurant"
    }
    
    static var checkOutThisRestaurant: String {
        languageManager.currentLanguage == .arabic ? "اطلع على هذا المطعم" : "Check out this restaurant"
    }
    
    static var addedToFavorites: String {
        languageManager.currentLanguage == .arabic ? "تمت الإضافة إلى المفضلة" : "Added to favorites"
    }
    
    static var removedFromFavorites: String {
        languageManager.currentLanguage == .arabic ? "تمت الإزالة من المفضلة" : "Removed from favorites"
    }
    
    static var cartClearedAndItemAdded: String {
        languageManager.currentLanguage == .arabic ? "تم تفريغ السلة وإضافة العنصر" : "Cart cleared and item added"
    }
    
    static var requiredAddons: String {
        languageManager.currentLanguage == .arabic ? "إلزامي" : "Required"
    }
    
    static var optionalAddons: String {
        languageManager.currentLanguage == .arabic ? "اختياري" : "Optional"
    }
    
    static func chooseN(_ n: Int) -> String {
        languageManager.currentLanguage == .arabic ? "اختر \(n)" : "Choose \(n)"
    }
    
    static var pleaseSelectRequiredAddons: String {
        languageManager.currentLanguage == .arabic ? "يرجى اختيار الإضافات الإلزامية" : "Please select required add-ons"
    }
    
    static var chooseFrom: String {
        languageManager.currentLanguage == .arabic ? "اختر من:" : "Choose from:"
    }
    
    static var requiredChip: String {
        languageManager.currentLanguage == .arabic ? "مطلوب" : "Required"
    }
    
    static var orderedTogether: String {
        languageManager.currentLanguage == .arabic ? "تُطلب معًا" : "Ordered Together"
    }
    
    static var orderedTogetherSubtitle: String {
        languageManager.currentLanguage == .arabic ? "عادةً ما يطلب المستخدمون هذه المنتجات أيضًا" : "Users usually order these products as well"
    }
    
    static var selectRequiredToAddItem: String {
        languageManager.currentLanguage == .arabic ? "اختر المطلوب لإضافة العنصر" : "Select required to add item"
    }
    
    static var quantity: String {
        languageManager.currentLanguage == .arabic ? "الكمية" : "Quantity"
    }
    
    static var unavailable: String {
        languageManager.currentLanguage == .arabic ? "غير متوفر" : "Unavailable"
    }
}

