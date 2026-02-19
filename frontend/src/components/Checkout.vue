<template>
  <div class="min-h-screen bg-gray-50 py-12 px-4 sm:px-6 lg:px-8" :dir="$i18n.locale === 'ar' ? 'rtl' : 'ltr'">
    <div class="max-w-4xl mx-auto">
      <!-- Header -->
      <div class="mb-8">
        <div class="flex items-center justify-between mb-4">
          <button
            @click="$router.back()"
            class="flex items-center gap-2 text-gray-600 hover:text-gray-800"
            :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''"
          >
            <span v-if="$i18n.locale === 'ar'">→</span>
            <span v-else>←</span>
            {{ $t('checkout.backToMenu') }}
          </button>
          <LanguageSwitcher v-if="false" />
        </div>
        <h1 class="text-4xl font-bold" :style="{ color: website?.primary_color || '#4F46E5' }">
          {{ $t('checkout.title') }}
        </h1>
        <p class="text-gray-600 mt-2">{{ $t('checkout.subtitle') }}</p>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <!-- Order Summary -->
        <div class="lg:col-span-1">
          <div class="bg-white rounded-lg shadow-lg p-6 sticky top-4">
            <h2 class="text-xl font-bold mb-4">{{ $t('checkout.orderSummary') }}</h2>
            
            <div class="space-y-4 mb-6">
              <div
                v-for="(item, index) in cart"
                :key="index"
                class="flex items-center gap-3 pb-4 border-b border-gray-200"
              >
                <div v-if="item.image_url" class="w-16 h-16 flex-shrink-0">
                  <img :src="item.image_url" :alt="item.name" class="w-full h-full object-cover rounded" />
                </div>
                <div class="flex-1 min-w-0">
                  <h4 class="font-semibold text-gray-800 truncate">{{ item.name }}</h4>
                  <p class="text-sm text-gray-500">{{ $t('checkout.quantity') }}: {{ item.quantity }}</p>
                  <p class="text-sm font-medium text-gray-700">
                    {{ formatCurrency(parseFloat(item.price) * item.quantity) }}
                  </p>
                </div>
              </div>
            </div>

            <!-- Coupon Code Section -->
            <div class="mb-4 pb-4 border-b border-gray-200">
              <label class="block text-sm font-medium text-gray-700 mb-2">
                {{ $t('checkout.couponCode') }}
              </label>
              <div class="flex gap-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <input
                  v-model="couponCode"
                  type="text"
                  :placeholder="$t('checkout.couponCodePlaceholder')"
                  class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 uppercase"
                  @keyup.enter="applyCoupon"
                />
                <button
                  type="button"
                  @click="applyCoupon"
                  :disabled="validatingCoupon || !couponCode"
                  class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {{ $t('checkout.applyCoupon') }}
                </button>
              </div>
              <div v-if="couponError" class="mt-2 text-sm text-red-600">{{ couponError }}</div>
              <div v-if="appliedCoupon" class="mt-2 flex items-center justify-between p-2 bg-green-50 border border-green-200 rounded">
                <div class="flex items-center gap-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <span class="text-sm font-semibold text-green-800">{{ appliedCoupon.code }}</span>
                  <span class="text-sm text-green-600">- {{ formatCurrency(appliedCoupon.discount_amount) }}</span>
                </div>
                <button
                  type="button"
                  @click="removeCoupon"
                  class="text-sm text-red-600 hover:text-red-800"
                >
                  {{ $t('checkout.remove') }}
                </button>
              </div>
            </div>

            <div class="space-y-2 pt-4 border-t border-gray-200">
              <div class="flex justify-between text-gray-600">
                <span>{{ $t('checkout.subtotal') }}</span>
                <span>{{ formatCurrency(cartTotal) }}</span>
              </div>
              <div v-if="appliedCoupon && discountAmount > 0" class="flex justify-between text-green-600">
                <span>{{ $t('checkout.discount') }}</span>
                <span>-{{ formatCurrency(discountAmount) }}</span>
              </div>
              <div v-if="website?.tax_enabled && taxAmount > 0" class="flex justify-between text-gray-600">
                <span>{{ $t('checkout.tax') }}</span>
                <span>{{ formatCurrency(taxAmount) }}</span>
              </div>
              <div v-if="checkoutForm.order_type === 'delivery' && deliveryFee > 0" class="flex justify-between text-gray-600">
                <span>{{ $t('checkout.deliveryFee') }}</span>
                <span>{{ formatCurrency(deliveryFee) }}</span>
              </div>
              <div class="flex justify-between text-lg font-bold pt-2 border-t border-gray-200" :style="{ color: website?.primary_color || '#4F46E5' }">
                <span>{{ $t('checkout.total') }}</span>
                <span>{{ formatCurrency(finalTotal) }}</span>
              </div>
            </div>

            <div class="mt-6 p-4 bg-blue-50 rounded-lg">
              <p class="text-sm text-blue-800">
                💳 {{ paymentNote }}
              </p>
            </div>
          </div>
        </div>

        <!-- Checkout Form -->
        <div class="lg:col-span-2">
          <div class="bg-white rounded-lg shadow-lg p-6">
            <form @submit.prevent="placeOrder" class="space-y-6">
              <div>
                <h2 class="text-2xl font-bold mb-4">{{ $t('checkout.customerInformation') }}</h2>
                
                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label class="block text-sm font-medium text-gray-700 mb-2">
                      {{ $t('checkout.fullName') }} <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="checkoutForm.customer_name"
                      type="text"
                      required
                      class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      :placeholder="$t('checkout.fullNamePlaceholder')"
                    />
                  </div>

                  <div>
                    <label class="block text-sm font-medium text-gray-700 mb-2">
                      {{ $t('checkout.phoneNumber') }} <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="checkoutForm.customer_phone"
                      type="tel"
                      required
                      class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      :placeholder="$t('checkout.phoneNumberPlaceholder')"
                    />
                  </div>
                </div>

                <div class="mt-4">
                  <label class="block text-sm font-medium text-gray-700 mb-2">
                    {{ $t('checkout.emailAddress') }}
                  </label>
                  <input
                    v-model="checkoutForm.customer_email"
                    type="email"
                    class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                    :placeholder="$t('checkout.emailAddressPlaceholder')"
                  />
                  <p class="text-xs text-gray-500 mt-1">{{ $t('checkout.emailHint') }}</p>
                </div>
              </div>

              <div>
                <h2 class="text-2xl font-bold mb-4">{{ $t('checkout.orderType') }}</h2>
                <div class="space-y-3">
                  <label v-if="isDineInEnabled" class="flex items-center cursor-pointer p-4 border-2 rounded-lg transition-colors" :class="checkoutForm.order_type === 'dine_in' ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200 hover:border-gray-300'" :style="checkoutForm.order_type === 'dine_in' ? { borderColor: website?.primary_color || '#4F46E5', backgroundColor: (website?.primary_color || '#4F46E5') + '10' } : {}">
                    <input
                      v-model="checkoutForm.order_type"
                      type="radio"
                      value="dine_in"
                      class="w-5 h-5 text-indigo-600 focus:ring-indigo-500"
                      :style="{ accentColor: website?.primary_color || '#4F46E5' }"
                    />
                    <span :class="['ml-3 text-gray-700 font-medium', $i18n.locale === 'ar' ? 'mr-3 ml-0' : '']">{{ $t('checkout.dineIn') }}</span>
                  </label>
                  <label v-if="isPickupEnabled" class="flex items-center cursor-pointer p-4 border-2 rounded-lg transition-colors" :class="checkoutForm.order_type === 'pickup' ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200 hover:border-gray-300'" :style="checkoutForm.order_type === 'pickup' ? { borderColor: website?.primary_color || '#4F46E5', backgroundColor: (website?.primary_color || '#4F46E5') + '10' } : {}">
                    <input
                      v-model="checkoutForm.order_type"
                      type="radio"
                      value="pickup"
                      class="w-5 h-5 text-indigo-600 focus:ring-indigo-500"
                      :style="{ accentColor: website?.primary_color || '#4F46E5' }"
                    />
                    <span :class="['ml-3 text-gray-700 font-medium', $i18n.locale === 'ar' ? 'mr-3 ml-0' : '']">{{ $t('checkout.pickup') }}</span>
                  </label>
                  <label v-if="isDeliveryEnabled" class="flex items-center cursor-pointer p-4 border-2 rounded-lg transition-colors" :class="checkoutForm.order_type === 'delivery' ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200 hover:border-gray-300'" :style="checkoutForm.order_type === 'delivery' ? { borderColor: website?.primary_color || '#4F46E5', backgroundColor: (website?.primary_color || '#4F46E5') + '10' } : {}">
                    <input
                      v-model="checkoutForm.order_type"
                      type="radio"
                      value="delivery"
                      class="w-5 h-5 text-indigo-600 focus:ring-indigo-500"
                      :style="{ accentColor: website?.primary_color || '#4F46E5' }"
                    />
                    <span :class="['ml-3 text-gray-700 font-medium', $i18n.locale === 'ar' ? 'mr-3 ml-0' : '']">{{ $t('checkout.delivery') }}</span>
                  </label>
                </div>
              </div>

              <div v-if="checkoutForm.order_type === 'delivery'">
                <h2 class="text-2xl font-bold mb-4">{{ $t('checkout.deliveryAddress') }}</h2>
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">
                    {{ $t('checkout.deliveryAddress') }} <span class="text-red-500">*</span>
                  </label>
                  <textarea
                    v-model="checkoutForm.customer_address"
                    rows="3"
                    required
                    class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                    :placeholder="$t('checkout.deliveryAddressPlaceholder')"
                  ></textarea>
                </div>
              </div>

              <div>
                <h2 class="text-2xl font-bold mb-4">{{ $t('checkout.paymentMethod') }}</h2>
                <div class="space-y-3">
                  <label v-if="paymentMethods.cashOnPickup && (checkoutForm.order_type === 'pickup' || checkoutForm.order_type === 'dine_in')" class="flex items-center cursor-pointer p-4 border-2 rounded-lg transition-colors" :class="checkoutForm.payment_method === 'cash' ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200 hover:border-gray-300'" :style="checkoutForm.payment_method === 'cash' ? { borderColor: website?.primary_color || '#4F46E5', backgroundColor: (website?.primary_color || '#4F46E5') + '10' } : {}">
                    <input
                      v-model="checkoutForm.payment_method"
                      type="radio"
                      value="cash"
                      class="w-5 h-5 text-indigo-600 focus:ring-indigo-500"
                      :style="{ accentColor: website?.primary_color || '#4F46E5' }"
                    />
                    <span :class="['ml-3 text-gray-700 font-medium', $i18n.locale === 'ar' ? 'mr-3 ml-0' : '']">{{ $t('checkout.paymentCash') }}</span>
                  </label>
                  <label v-if="paymentMethods.cashOnDelivery && checkoutForm.order_type === 'delivery'" class="flex items-center cursor-pointer p-4 border-2 rounded-lg transition-colors" :class="checkoutForm.payment_method === 'cash' ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200 hover:border-gray-300'" :style="checkoutForm.payment_method === 'cash' ? { borderColor: website?.primary_color || '#4F46E5', backgroundColor: (website?.primary_color || '#4F46E5') + '10' } : {}">
                    <input
                      v-model="checkoutForm.payment_method"
                      type="radio"
                      value="cash"
                      class="w-5 h-5 text-indigo-600 focus:ring-indigo-500"
                      :style="{ accentColor: website?.primary_color || '#4F46E5' }"
                    />
                    <span :class="['ml-3 text-gray-700 font-medium', $i18n.locale === 'ar' ? 'mr-3 ml-0' : '']">{{ $t('checkout.paymentCash') }}</span>
                  </label>
                  <label v-if="paymentMethods.creditCard" class="flex items-center cursor-pointer p-4 border-2 rounded-lg transition-colors" :class="checkoutForm.payment_method === 'card' ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200 hover:border-gray-300'" :style="checkoutForm.payment_method === 'card' ? { borderColor: website?.primary_color || '#4F46E5', backgroundColor: (website?.primary_color || '#4F46E5') + '10' } : {}">
                    <input
                      v-model="checkoutForm.payment_method"
                      type="radio"
                      value="card"
                      class="w-5 h-5 text-indigo-600 focus:ring-indigo-500"
                      :style="{ accentColor: website?.primary_color || '#4F46E5' }"
                    />
                    <span :class="['ml-3 text-gray-700 font-medium', $i18n.locale === 'ar' ? 'mr-3 ml-0' : '']">{{ $t('checkout.paymentCard') }}</span>
                  </label>
                  <label v-if="paymentMethods.onlinePayment" class="flex items-center cursor-pointer p-4 border-2 rounded-lg transition-colors" :class="checkoutForm.payment_method === 'online' ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200 hover:border-gray-300'" :style="checkoutForm.payment_method === 'online' ? { borderColor: website?.primary_color || '#4F46E5', backgroundColor: (website?.primary_color || '#4F46E5') + '10' } : {}">
                    <input
                      v-model="checkoutForm.payment_method"
                      type="radio"
                      value="online"
                      class="w-5 h-5 text-indigo-600 focus:ring-indigo-500"
                      :style="{ accentColor: website?.primary_color || '#4F46E5' }"
                    />
                    <span :class="['ml-3 text-gray-700 font-medium', $i18n.locale === 'ar' ? 'mr-3 ml-0' : '']">{{ $t('checkout.paymentOnline') }}</span>
                  </label>
                  <label v-if="paymentMethods.mobilePayment" class="flex items-center cursor-pointer p-4 border-2 rounded-lg transition-colors" :class="checkoutForm.payment_method === 'mobile' ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200 hover:border-gray-300'" :style="checkoutForm.payment_method === 'mobile' ? { borderColor: website?.primary_color || '#4F46E5', backgroundColor: (website?.primary_color || '#4F46E5') + '10' } : {}">
                    <input
                      v-model="checkoutForm.payment_method"
                      type="radio"
                      value="mobile"
                      class="w-5 h-5 text-indigo-600 focus:ring-indigo-500"
                      :style="{ accentColor: website?.primary_color || '#4F46E5' }"
                    />
                    <span :class="['ml-3 text-gray-700 font-medium', $i18n.locale === 'ar' ? 'mr-3 ml-0' : '']">{{ $t('checkout.paymentMobile') }}</span>
                  </label>
                  <label v-if="paymentMethods.cliQServices?.enabled" class="flex items-center cursor-pointer p-4 border-2 rounded-lg transition-colors" :class="checkoutForm.payment_method === 'cliq' ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200 hover:border-gray-300'" :style="checkoutForm.payment_method === 'cliq' ? { borderColor: website?.primary_color || '#4F46E5', backgroundColor: (website?.primary_color || '#4F46E5') + '10' } : {}">
                    <input
                      v-model="checkoutForm.payment_method"
                      type="radio"
                      value="cliq"
                      class="w-5 h-5 text-indigo-600 focus:ring-indigo-500"
                      :style="{ accentColor: website?.primary_color || '#4F46E5' }"
                    />
                    <div :class="['ml-3', $i18n.locale === 'ar' ? 'mr-3 ml-0' : '']">
                      <span class="text-gray-700 font-medium">{{ $t('checkout.paymentCliQ') }}</span>
                      <p v-if="paymentMethods.cliQServices?.name || paymentMethods.cliQServices?.phone" class="text-xs text-gray-500 mt-1">
                        {{ paymentMethods.cliQServices?.name || '' }} {{ paymentMethods.cliQServices?.phone ? `(${paymentMethods.cliQServices.phone})` : '' }}
                      </p>
                    </div>
                  </label>
                </div>
              </div>

              <!-- Stripe Payment Element (shown when card or online payment is selected) -->
              <div v-if="checkoutForm.payment_method === 'card' || checkoutForm.payment_method === 'online'" id="stripe-payment-element" class="mt-4">
                <div v-if="!stripeReady && !stripeError" class="p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
                  <p class="text-sm text-yellow-800">{{ $t('checkout.loadingPayment') }}</p>
                </div>
                <div v-if="stripeError" class="p-4 bg-red-50 border border-red-200 rounded-lg">
                  <p class="text-sm text-red-600">{{ stripeError }}</p>
                </div>
                <div id="card-element" v-show="stripeReady" class="p-4 border border-gray-300 rounded-lg bg-white">
                  <!-- Stripe Elements will mount here -->
                </div>
              </div>

              <div>
                <h2 class="text-2xl font-bold mb-4">{{ $t('checkout.specialInstructions') }}</h2>
                <textarea
                  v-model="checkoutForm.notes"
                  rows="4"
                  class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  :placeholder="$t('checkout.specialInstructionsPlaceholder')"
                ></textarea>
              </div>

              <div class="flex gap-4 pt-4">
                <button
                  type="button"
                  @click="$router.back()"
                  class="flex-1 px-6 py-3 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition-colors font-semibold"
                >
                  {{ $t('checkout.backToCart') }}
                </button>
                <button
                  type="submit"
                  :disabled="placingOrder"
                  :style="{ backgroundColor: website?.primary_color || '#4F46E5' }"
                  class="flex-1 px-6 py-3 text-white rounded-lg font-semibold hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed transition-opacity"
                >
                  {{ placingOrder ? $t('checkout.placingOrder') : $t('checkout.placeOrder') }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import LanguageSwitcher from './LanguageSwitcher.vue';
import { getWebsite, createOrder, createPaymentIntent, validateCoupon } from '../services/api.js';

const { locale, t } = useI18n();

const route = useRoute();
const router = useRouter();

const website = ref(null);
const cart = ref([]);
const placingOrder = ref(false);
const checkoutForm = ref({
  customer_name: '',
  customer_email: '',
  customer_phone: '',
  customer_address: '',
  notes: '',
  order_type: 'pickup', // Default to pickup
  payment_method: 'cash' // Default to cash
});

// Coupon state
const couponCode = ref('');
const appliedCoupon = ref(null);
const validatingCoupon = ref(false);
const couponError = ref('');
const discountAmount = ref(0);

// Stripe integration
const stripeReady = ref(false);
const stripe = ref(null);
const elements = ref(null);
const cardElement = ref(null);
const paymentIntentClientSecret = ref(null);
const stripeError = ref('');

const cartTotal = computed(() => {
  return cart.value.reduce((total, item) => {
    return total + (parseFloat(item.price) * item.quantity);
  }, 0);
});

// Get delivery fee from website settings
const deliveryFee = computed(() => {
  if (!website.value || checkoutForm.value.order_type !== 'delivery') {
    return 0;
  }
  const fee = website.value.delivery_fee;
  if (fee === undefined || fee === null || fee === '') {
    return 0;
  }
  // Handle both string and number types, ensure it's a valid number
  const parsedFee = typeof fee === 'string' ? parseFloat(fee) : Number(fee);
  return isNaN(parsedFee) || parsedFee < 0 ? 0 : parsedFee;
});

// Calculate tax amount
const taxAmount = computed(() => {
  if (!website.value || !website.value.tax_enabled) {
    return 0;
  }
  const taxRate = parseFloat(website.value.tax_rate) || 0;
  return (cartTotal.value * taxRate) / 100;
});

// Calculate final total including tax, delivery fee, and discount
const finalTotal = computed(() => {
  const subtotal = cartTotal.value - discountAmount.value;
  return subtotal + taxAmount.value + deliveryFee.value;
});

// Currency symbol mapping - language-aware
const getCurrencySymbol = (currencyCode) => {
  const currentLang = locale.value || 'en';
  
  if (currencyCode === 'JOD') {
    // JOD: Show "JD" in English, "د.ا" in Arabic
    return currentLang === 'ar' ? 'د.ا' : 'JD';
  }
  
  // Default currency symbols (same for all languages)
  const symbols = {
    USD: '$',
    JOD: currentLang === 'ar' ? 'د.ا' : 'JD'
  };
  
  return symbols[currencyCode] || '$';
};

// Format currency based on restaurant settings
const formatCurrency = (amount) => {
  if (!website.value) {
    return `$${parseFloat(amount).toFixed(2)}`;
  }

  const currencyCode = website.value.currency_code || 'USD';
  const symbolPosition = website.value.currency_symbol_position || 'before';
  const symbol = getCurrencySymbol(currencyCode);
  const formattedAmount = parseFloat(amount).toFixed(2);

  if (symbolPosition === 'before') {
    return `${symbol}${formattedAmount}`;
  } else {
    return `${formattedAmount} ${symbol}`;
  }
};

// Apply coupon
const applyCoupon = async () => {
  if (!couponCode.value || !website.value) {
    return;
  }

  validatingCoupon.value = true;
  couponError.value = '';

  try {
    const result = await validateCoupon(
      parseInt(route.params.id),
      couponCode.value.toUpperCase(),
      cartTotal.value
    );

    if (result.valid && result.coupon) {
      appliedCoupon.value = result.coupon;
      discountAmount.value = result.coupon.discount_amount;
      couponError.value = '';
    } else {
      couponError.value = result.error || t('checkout.invalidCoupon');
      appliedCoupon.value = null;
      discountAmount.value = 0;
    }
  } catch (error) {
    couponError.value = error.message || t('checkout.couponValidationFailed');
    appliedCoupon.value = null;
    discountAmount.value = 0;
  } finally {
    validatingCoupon.value = false;
  }
};

// Remove coupon
const removeCoupon = () => {
  appliedCoupon.value = null;
  discountAmount.value = 0;
  couponCode.value = '';
  couponError.value = '';
};

// Check which order types are enabled from database
const isDineInEnabled = computed(() => {
  if (!website.value) return true; // Default to enabled if website not loaded yet
  return website.value.order_type_dine_in_enabled === 1 || website.value.order_type_dine_in_enabled === true;
});

const isPickupEnabled = computed(() => {
  if (!website.value) return true; // Default to enabled if website not loaded yet
  return website.value.order_type_pickup_enabled === 1 || website.value.order_type_pickup_enabled === true;
});

const isDeliveryEnabled = computed(() => {
  if (!website.value) return true; // Default to enabled if website not loaded yet
  return website.value.order_type_delivery_enabled === 1 || website.value.order_type_delivery_enabled === true;
});

// Parse payment methods from website settings
const paymentMethods = computed(() => {
  if (!website.value || !website.value.payment_methods) {
    // Default payment methods if not set
    return {
      cashOnPickup: true,
      cashOnDelivery: true,
      creditCard: false,
      onlinePayment: false,
      mobilePayment: false,
      cliQServices: {
        enabled: false,
        phone: '',
        name: ''
      }
    };
  }
  
  try {
    const parsed = typeof website.value.payment_methods === 'string' 
      ? JSON.parse(website.value.payment_methods) 
      : website.value.payment_methods;
    return {
      cashOnPickup: parsed.cashOnPickup !== false, // Default to true
      cashOnDelivery: parsed.cashOnDelivery !== false, // Default to true
      creditCard: parsed.creditCard === true,
      onlinePayment: parsed.onlinePayment === true,
      mobilePayment: parsed.mobilePayment === true,
      cliQServices: parsed.cliQServices || {
        enabled: false,
        phone: '',
        name: ''
      }
    };
  } catch (e) {
    console.warn('Failed to parse payment methods:', e);
    return {
      cashOnPickup: true,
      cashOnDelivery: true,
      creditCard: false,
      onlinePayment: false,
      mobilePayment: false,
      cliQServices: {
        enabled: false,
        phone: '',
        name: ''
      }
    };
  }
});

// Get available payment methods based on order type
const availablePaymentMethods = computed(() => {
  const methods = [];
  const orderType = checkoutForm.value.order_type || 'pickup';
  
  if (orderType === 'pickup' || orderType === 'dine_in') {
    if (paymentMethods.value.cashOnPickup) methods.push('cash');
    if (paymentMethods.value.creditCard) methods.push('card');
    if (paymentMethods.value.onlinePayment) methods.push('online');
    if (paymentMethods.value.mobilePayment) methods.push('mobile');
    if (paymentMethods.value.cliQServices?.enabled) methods.push('cliq');
  } else if (orderType === 'delivery') {
    if (paymentMethods.value.cashOnDelivery) methods.push('cash');
    if (paymentMethods.value.creditCard) methods.push('card');
    if (paymentMethods.value.onlinePayment) methods.push('online');
    if (paymentMethods.value.mobilePayment) methods.push('mobile');
    if (paymentMethods.value.cliQServices?.enabled) methods.push('cliq');
  }
  
  return methods;
});

// Generate dynamic payment note based on available payment methods and order type
const paymentNote = computed(() => {
  const orderType = checkoutForm.value.order_type || 'pickup';
  const methods = availablePaymentMethods.value;
  
  if (methods.length === 0) {
    return t('checkout.paymentNoteDefault');
  }
  
  const methodNames = [];
  if (methods.includes('cash')) {
    if (orderType === 'delivery') {
      methodNames.push(t('checkout.paymentCashOnDelivery'));
    } else {
      methodNames.push(t('checkout.paymentCashOnPickup'));
    }
  }
  if (methods.includes('card')) {
    methodNames.push(t('checkout.paymentCard'));
  }
  if (methods.includes('online')) {
    methodNames.push(t('checkout.paymentOnline'));
  }
  if (methods.includes('mobile')) {
    methodNames.push(t('checkout.paymentMobile'));
  }
  if (methods.includes('cliq')) {
    methodNames.push(t('checkout.paymentCliQ'));
  }
  
  if (methodNames.length === 0) {
    return t('checkout.paymentNoteDefault');
  }
  
  if (methodNames.length === 1) {
    if (orderType === 'delivery') {
      return t('checkout.paymentNoteSingleDelivery', { method: methodNames[0] });
    } else {
      return t('checkout.paymentNoteSingle', { method: methodNames[0] });
    }
  }
  
  // Multiple payment methods
  const lastMethod = methodNames.pop();
  const methodsList = methodNames.join(', ') + ' ' + t('checkout.paymentOr') + ' ' + lastMethod;
  
  if (orderType === 'delivery') {
    return t('checkout.paymentNoteMultipleDelivery', { methods: methodsList });
  } else {
    return t('checkout.paymentNoteMultiple', { methods: methodsList });
  }
});

const placeOrder = async () => {
  if (cart.value.length === 0) {
    alert(t('checkout.cartEmpty'));
    router.push(`/website/${route.params.id}`);
    return;
  }
  
  // Validate order type is enabled in database
  const selectedOrderType = checkoutForm.value.order_type || 'pickup';
  if (selectedOrderType === 'dine_in' && !isDineInEnabled.value) {
    alert(t('checkout.orderTypeNotEnabled'));
    return;
  }
  if (selectedOrderType === 'pickup' && !isPickupEnabled.value) {
    alert(t('checkout.orderTypeNotEnabled'));
    return;
  }
  if (selectedOrderType === 'delivery' && !isDeliveryEnabled.value) {
    alert(t('checkout.orderTypeNotEnabled'));
    return;
  }
  
  // Validate delivery address if delivery is selected
  if (selectedOrderType === 'delivery' && !checkoutForm.value.customer_address) {
    alert(t('checkout.deliveryAddressRequired'));
    return;
  }
  
  // Validate payment method is selected
  if (!checkoutForm.value.payment_method) {
    alert(t('checkout.paymentMethodRequired'));
    return;
  }
  
  placingOrder.value = true;
  stripeError.value = '';
  
  try {
    // Handle Stripe payment if card or online payment is selected
    let paymentIntentId = null;
    if (checkoutForm.value.payment_method === 'card' || checkoutForm.value.payment_method === 'online') {
      // Ensure Stripe is initialized
      if (!stripe.value || !elements.value || !stripeReady.value) {
        // Try to initialize if not ready
        if (!stripeReady.value && !stripeError.value) {
          await initializeStripe();
        }
        
        // Check again after initialization attempt
        if (!stripe.value || !elements.value || !stripeReady.value) {
          alert(t('checkout.stripeNotReady') + (stripeError.value ? ': ' + stripeError.value : ''));
          placingOrder.value = false;
          return;
        }
      }
      
      // Confirm payment with Stripe using Payment Element
      const { error: confirmError, paymentIntent } = await stripe.value.confirmPayment({
        elements: elements.value,
        confirmParams: {
          return_url: `${window.location.origin}/website/${route.params.id}/order-confirmation`,
          payment_method_data: {
            billing_details: {
              name: checkoutForm.value.customer_name,
              email: checkoutForm.value.customer_email,
              phone: checkoutForm.value.customer_phone,
              address: checkoutForm.value.customer_address ? {
                line1: checkoutForm.value.customer_address
              } : undefined
            }
          }
        },
        redirect: 'if_required'
      });
      
      if (confirmError) {
        stripeError.value = confirmError.message;
        alert(t('checkout.paymentFailed') + ': ' + confirmError.message);
        placingOrder.value = false;
        return;
      }
      
      if (paymentIntent && paymentIntent.status === 'succeeded') {
        paymentIntentId = paymentIntent.id;
      } else {
        alert(t('checkout.paymentNotCompleted'));
        placingOrder.value = false;
        return;
      }
    }
    
    const orderData = {
      website_id: parseInt(route.params.id),
      customer_name: checkoutForm.value.customer_name,
      customer_email: checkoutForm.value.customer_email,
      customer_phone: checkoutForm.value.customer_phone,
      customer_address: checkoutForm.value.customer_address,
      notes: checkoutForm.value.notes,
      order_type: selectedOrderType,
      payment_method: checkoutForm.value.payment_method,
      payment_intent_id: paymentIntentId,
      coupon_code: appliedCoupon.value ? appliedCoupon.value.code : null,
      items: cart.value.map(item => ({
        product_id: item.id,
        quantity: item.quantity
      }))
      // Note: total_amount is calculated on backend including delivery fee when order_type is 'delivery'
    };
    
    const order = await createOrder(orderData);
    
    if (!order || !order.order_number) {
      throw new Error('Order was created but order number is missing');
    }
    
    console.log('Order created successfully:', order);
    
    // Store order data in sessionStorage as fallback
    sessionStorage.setItem(`order_${order.order_number}`, JSON.stringify(order));
    
    // Clear cart from sessionStorage
    sessionStorage.removeItem(`cart_${route.params.id}`);
    
    // Small delay to ensure order is fully committed to database
    await new Promise(resolve => setTimeout(resolve, 300));
    
    // Redirect to order confirmation
    router.push(`/website/${route.params.id}/order/${order.order_number}`);
    
  } catch (error) {
    alert(t('checkout.orderFailed') + ': ' + error.message);
  } finally {
    placingOrder.value = false;
  }
};

// Initialize Stripe when payment method changes to card/online
watch(() => checkoutForm.value.payment_method, async (newMethod, oldMethod) => {
  // Reset Stripe state when switching away from card/online
  if (oldMethod === 'card' || oldMethod === 'online') {
    if (newMethod !== 'card' && newMethod !== 'online') {
      stripeReady.value = false;
      stripeError.value = '';
      if (cardElement.value) {
        cardElement.value.unmount();
        cardElement.value = null;
      }
      elements.value = null;
      paymentIntentClientSecret.value = null;
    }
  }
  
  // Initialize when switching to card/online
  if ((newMethod === 'card' || newMethod === 'online') && website.value && finalTotal.value > 0) {
    stripeReady.value = false;
    stripeError.value = '';
    await initializeStripe();
  }
}, { immediate: false });

// Also watch for total changes to reinitialize if needed
watch(() => finalTotal.value, async (newTotal) => {
  if ((checkoutForm.value.payment_method === 'card' || checkoutForm.value.payment_method === 'online') 
      && website.value && newTotal > 0 && !stripeReady.value && !stripeError.value) {
    await initializeStripe();
  }
});

// Initialize Stripe
const initializeStripe = async () => {
  try {
    stripeError.value = '';
    stripeReady.value = false;
    
    // Load Stripe.js script if not already loaded
    if (!window.Stripe) {
      const script = document.createElement('script');
      script.src = 'https://js.stripe.com/v3/';
      script.async = true;
      document.head.appendChild(script);
      await new Promise((resolve, reject) => {
        script.onload = resolve;
        script.onerror = () => reject(new Error('Failed to load Stripe.js'));
        setTimeout(() => reject(new Error('Stripe.js loading timeout')), 10000);
      });
    }
    
    // Get Stripe publishable key from backend (or use env variable)
    const stripeKey = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY || '';
    
    if (!stripeKey || stripeKey === 'pk_test_placeholder' || stripeKey.trim() === '') {
      stripeError.value = t('checkout.stripeNotConfigured');
      stripeReady.value = false;
      return;
    }
    
    // Initialize Stripe
    stripe.value = window.Stripe(stripeKey);
    
    // Create payment intent
    const amount = Math.round(finalTotal.value * 100); // Convert to cents
    if (amount <= 0) {
      stripeError.value = t('checkout.invalidAmount');
      stripeReady.value = false;
      return;
    }
    
    const paymentIntent = await createPaymentIntent({
      website_id: parseInt(route.params.id),
      amount: amount,
      currency: 'usd'
    });
    
    if (!paymentIntent || !paymentIntent.client_secret) {
      stripeError.value = t('checkout.paymentIntentFailed');
      stripeReady.value = false;
      return;
    }
    
    paymentIntentClientSecret.value = paymentIntent.client_secret;
    
    // Create and mount payment element with specific payment method types
    elements.value = stripe.value.elements({
      clientSecret: paymentIntentClientSecret.value,
      appearance: {
        theme: 'stripe',
        variables: {
          colorPrimary: website.value?.primary_color || '#4F46E5',
        }
      }
    });
    
    // Create payment element with only card-based payment methods
    const paymentElement = elements.value.create('payment', {
      paymentMethodTypes: ['card'],
      fields: {
        billingDetails: {
          name: 'auto',
          email: 'auto',
          phone: 'auto',
          address: {
            country: 'auto',
            line1: 'auto',
            city: 'auto',
            state: 'auto',
            postalCode: 'auto'
          }
        }
      }
    });
    
    // Wait for DOM to be ready and element to exist
    await nextTick();
    // Retry a few times if element is not found
    let cardElementContainer = document.getElementById('card-element');
    let retries = 0;
    while (!cardElementContainer && retries < 10) {
      await new Promise(resolve => setTimeout(resolve, 100));
      cardElementContainer = document.getElementById('card-element');
      retries++;
    }
    
    if (!cardElementContainer) {
      stripeError.value = t('checkout.paymentElementNotFound');
      stripeReady.value = false;
      return;
    }
    
    // Unmount previous element if exists
    if (cardElement.value) {
      try {
        cardElement.value.unmount();
      } catch (e) {
        // Ignore unmount errors
        console.warn('Error unmounting previous element:', e);
      }
    }
    
    // Mount the payment element
    paymentElement.mount('#card-element');
    cardElement.value = paymentElement;
    
    // Listen for errors
    paymentElement.on('change', (event) => {
      if (event.error) {
        stripeError.value = event.error.message;
      } else {
        stripeError.value = '';
      }
    });
    
    // Listen for ready event
    paymentElement.on('ready', () => {
      stripeReady.value = true;
      stripeError.value = '';
    });
    
    // Set ready immediately (ready event might not fire in all cases)
    stripeReady.value = true;
  } catch (error) {
    console.error('Failed to initialize Stripe:', error);
    stripeError.value = error.message || t('checkout.stripeInitializationFailed');
    stripeReady.value = false;
  }
};

// Watch for locale changes to update RTL
watch(locale, (newLocale) => {
  document.documentElement.setAttribute('lang', newLocale);
  document.documentElement.setAttribute('dir', newLocale === 'ar' ? 'rtl' : 'ltr');
});

onMounted(async () => {
  // Set initial locale and RTL
  const savedLang = localStorage.getItem('appLanguage') || 'en';
  locale.value = savedLang;
  document.documentElement.setAttribute('lang', savedLang);
  document.documentElement.setAttribute('dir', savedLang === 'ar' ? 'rtl' : 'ltr');
  
  try {
    // Load website
    website.value = await getWebsite(route.params.id);
    
    // Set default order type based on enabled order types
    if (website.value) {
      if (website.value.order_type_dine_in_enabled === 1 || website.value.order_type_dine_in_enabled === true) {
        checkoutForm.value.order_type = 'dine_in';
      } else if (website.value.order_type_pickup_enabled === 1 || website.value.order_type_pickup_enabled === true) {
        checkoutForm.value.order_type = 'pickup';
      } else if (website.value.order_type_delivery_enabled === 1 || website.value.order_type_delivery_enabled === true) {
        checkoutForm.value.order_type = 'delivery';
      }
      // If none are enabled (shouldn't happen), default to pickup
    }
    
    // Load cart from sessionStorage
    const cartData = sessionStorage.getItem(`cart_${route.params.id}`);
    if (cartData) {
      cart.value = JSON.parse(cartData);
    }
    
    // If cart is empty, redirect back
    if (cart.value.length === 0) {
      router.push(`/website/${route.params.id}`);
    }
    
    // Set default payment method based on available methods
    if (website.value && paymentMethods.value) {
      if (checkoutForm.value.order_type === 'delivery' && paymentMethods.value.cashOnDelivery) {
        checkoutForm.value.payment_method = 'cash';
      } else if ((checkoutForm.value.order_type === 'pickup' || checkoutForm.value.order_type === 'dine_in') && paymentMethods.value.cashOnPickup) {
        checkoutForm.value.payment_method = 'cash';
      } else if (paymentMethods.value.creditCard) {
        checkoutForm.value.payment_method = 'card';
      } else if (paymentMethods.value.onlinePayment) {
        checkoutForm.value.payment_method = 'online';
      } else if (paymentMethods.value.mobilePayment) {
        checkoutForm.value.payment_method = 'mobile';
      }
    }
  } catch (error) {
    console.error('Failed to load checkout data:', error);
    router.push(`/website/${route.params.id}`);
  }
});
</script>

