<template>
  <div v-if="loading" class="min-h-screen flex items-center justify-center bg-gray-50">
    <div class="text-center">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto"></div>
      <p class="mt-4 text-gray-600">{{ $t('orderConfirmation.loading') }}</p>
    </div>
  </div>
  
  <div v-else class="min-h-screen bg-gray-50 py-12 px-4 sm:px-6 lg:px-8" :dir="$i18n.locale === 'ar' ? 'rtl' : 'ltr'">
    <div class="max-w-2xl mx-auto">
      <div class="bg-white rounded-lg shadow-xl p-8 text-center">
        <div class="flex justify-end mb-4">
          <LanguageSwitcher v-if="false" />
        </div>
        <div class="mb-6">
          <div class="w-20 h-20 mx-auto mb-4 rounded-full flex items-center justify-center" :style="{ backgroundColor: website?.primary_color + '20' }">
            <svg class="w-12 h-12" :style="{ color: website?.primary_color }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
            </svg>
          </div>
          <h1 class="text-3xl font-bold mb-2" :style="{ color: website?.primary_color || '#4F46E5' }">
            {{ $t('orderConfirmation.title') }}
          </h1>
          <p class="text-gray-600">{{ $t('orderConfirmation.thankYou') }}</p>
        </div>

        <div class="bg-gray-50 rounded-lg p-6 mb-6">
          <p class="text-sm text-gray-500 mb-2">{{ $t('orderConfirmation.orderNumber') }}</p>
          <p class="text-2xl font-bold" :style="{ color: website?.primary_color || '#4F46E5' }">
            {{ order?.order_number || route.params.orderNumber }}
          </p>
        </div>

        <div v-if="order" :class="['mb-6', $i18n.locale === 'ar' ? 'text-right' : 'text-left']">
          <h2 class="font-semibold mb-3">{{ $t('orderConfirmation.orderDetails') }}</h2>
          <div class="space-y-2 text-sm">
            <div class="flex justify-between">
              <span class="text-gray-600">{{ $t('orderConfirmation.status') }}:</span>
              <span class="font-semibold" :style="{ color: getStatusColor(order.status) }">
                {{ formatStatus(order.status) }}
              </span>
            </div>
            <div class="flex justify-between">
              <span class="text-gray-600">{{ $t('orderConfirmation.total') }}:</span>
              <span class="font-semibold">{{ formatCurrency(order.total_amount) }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-gray-600">{{ $t('orderConfirmation.payment') }}:</span>
              <span class="font-semibold">{{ order.payment_status === 'paid' ? $t('orderConfirmation.paid') : $t('orderConfirmation.payOnPickup') }}</span>
            </div>
          </div>
        </div>

        <!-- Registration Form -->
        <div v-if="!isRegistered && !registrationCompleted && !isReturningCustomer" class="bg-gradient-to-br from-indigo-50 to-purple-50 border-2 border-indigo-200 rounded-lg p-6 mb-6">
          <div class="text-center mb-4">
            <div class="w-16 h-16 mx-auto mb-3 rounded-full flex items-center justify-center bg-indigo-100">
              <svg class="w-8 h-8 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
            </div>
            <h3 class="text-xl font-bold text-gray-900 mb-2">{{ $t('orderConfirmation.registerTitle') }}</h3>
            <p class="text-sm text-gray-600 mb-4">
              {{ $t('orderConfirmation.registerDescription') }}
            </p>
          </div>

          <form @submit.prevent="handleRegistration" class="space-y-4">
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-2">
                {{ $t('orderConfirmation.fullName') }} <span class="text-red-500">*</span>
              </label>
              <input
                v-model="registrationForm.name"
                type="text"
                required
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                :placeholder="$t('orderConfirmation.fullNamePlaceholder')"
              />
            </div>

            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-2">
                {{ $t('orderConfirmation.emailAddress') }} <span class="text-gray-400 text-xs">({{ $t('orderConfirmation.optional') }})</span>
              </label>
              <input
                v-model="registrationForm.email"
                type="email"
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                :placeholder="$t('orderConfirmation.emailAddressPlaceholder')"
              />
              <p class="text-xs text-gray-500 mt-1">{{ $t('orderConfirmation.emailHint') }}</p>
            </div>

            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-2">
                {{ $t('orderConfirmation.phoneNumber') }} <span class="text-red-500">*</span>
              </label>
              <input
                v-model="registrationForm.phone"
                type="tel"
                required
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                :placeholder="$t('orderConfirmation.phoneNumberPlaceholder')"
              />
            </div>

            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-2">
                {{ $t('orderConfirmation.address') }} ({{ $t('orderConfirmation.optional') }})
              </label>
              <input
                v-model="registrationForm.address"
                type="text"
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                :placeholder="$t('orderConfirmation.addressPlaceholder')"
              />
            </div>

            <div v-if="registrationError" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
              {{ registrationError }}
            </div>

            <button
              type="submit"
              :disabled="registering"
              :style="{ backgroundColor: website?.primary_color || '#4F46E5' }"
              class="w-full px-6 py-3 text-white rounded-lg hover:opacity-90 transition-opacity font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <span v-if="registering">{{ $t('orderConfirmation.registering') }}</span>
              <span v-else>{{ $t('orderConfirmation.registerButton') }}</span>
            </button>

            <button
              type="button"
              @click="skipRegistration"
              class="w-full text-sm text-gray-600 hover:text-gray-800 transition-colors"
            >
              {{ $t('orderConfirmation.skip') }}
            </button>
          </form>
        </div>

        <!-- Returning Customer Welcome Message -->
        <div v-if="isReturningCustomer && registrationCompleted" class="bg-gradient-to-br from-green-50 to-emerald-50 border-2 border-green-300 rounded-lg p-8 mb-6 text-center">
          <div class="w-20 h-20 mx-auto mb-4 rounded-full flex items-center justify-center bg-green-100">
            <svg class="w-10 h-10 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
            </svg>
          </div>
          <h3 class="text-2xl font-bold text-green-900 mb-2">{{ $t('orderConfirmation.welcomeBack') }}, {{ order?.customer_name || customerInfo?.name || registrationForm.name }}! 👋</h3>
          <p class="text-green-700 mb-4">
            {{ $t('orderConfirmation.welcomeBackMessage') }}
          </p>
          <p class="text-sm text-green-600">
            {{ $t('orderConfirmation.welcomeBackHint') }}
          </p>
        </div>

        <!-- Registration Success (New Customer) -->
        <div v-if="registrationCompleted && isRegistered && !isReturningCustomer" class="bg-green-50 border border-green-200 rounded-lg p-4 mb-6">
          <div class="flex items-start gap-3">
            <div class="flex-shrink-0">
              <svg class="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <h4 class="font-semibold text-green-900 mb-1">{{ $t('orderConfirmation.registrationSuccess') }}</h4>
              <p class="text-sm text-green-700">
                {{ $t('orderConfirmation.registrationSuccessMessage') }}
              </p>
            </div>
          </div>
        </div>

        <div class="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
          <p class="text-sm text-blue-800">
            💡 <strong>{{ $t('orderConfirmation.whatsNext') }}</strong> {{ nextStepsMessage }}
          </p>
        </div>

        <div class="flex gap-4">
          <button
            @click="router.push(`/website/${route.params.id}`)"
            class="flex-1 px-6 py-3 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition-colors font-semibold"
          >
            {{ $t('orderConfirmation.backToMenu') }}
          </button>
          <button
            @click="router.push(`/website/${route.params.id}/track/${route.params.orderNumber}`)"
            :style="{ backgroundColor: website?.primary_color || '#4F46E5' }"
            class="flex-1 px-6 py-3 text-white rounded-lg hover:opacity-90 transition-opacity font-semibold"
          >
            {{ $t('orderConfirmation.trackOrder') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import LanguageSwitcher from './LanguageSwitcher.vue';
import { getWebsite, getOrderByNumber, registerCustomer, linkOrderToCustomer } from '../services/api.js';

const { locale, t } = useI18n();

const route = useRoute();
const router = useRouter();

const website = ref(null);
const order = ref(null);
const loading = ref(true);
const isRegistered = ref(false);
const registrationCompleted = ref(false);
const registering = ref(false);
const registrationError = ref('');
const isReturningCustomer = ref(false);
const customerInfo = ref(null);
const registrationForm = ref({
  name: '',
  email: '',
  phone: '',
  address: ''
});

const formatStatus = (status) => {
  const statusMap = {
    'pending': t('orderConfirmation.statusPending'),
    'confirmed': t('orderConfirmation.statusConfirmed'),
    'preparing': t('orderConfirmation.statusPreparing'),
    'ready': t('orderConfirmation.statusReady'),
    'completed': t('orderConfirmation.statusCompleted'),
    'cancelled': t('orderConfirmation.statusCancelled')
  };
  return statusMap[status] || status;
};

const getStatusColor = (status) => {
  const colorMap = {
    'pending': '#F59E0B',
    'confirmed': '#3B82F6',
    'preparing': '#8B5CF6',
    'ready': '#10B981',
    'completed': '#059669',
    'cancelled': '#EF4444'
  };
  return colorMap[status] || '#6B7280';
};

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

// Parse payment methods from website settings
const paymentMethods = computed(() => {
  if (!website.value || !website.value.payment_methods) {
    // Default payment methods if not set
    return {
      cashOnPickup: true,
      cashOnDelivery: true,
      creditCard: false,
      onlinePayment: false,
      mobilePayment: false
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
      mobilePayment: parsed.mobilePayment === true
    };
  } catch (e) {
    console.warn('Failed to parse payment methods:', e);
    return {
      cashOnPickup: true,
      cashOnDelivery: true,
      creditCard: false,
      onlinePayment: false,
      mobilePayment: false
    };
  }
});

// Get available payment methods based on order type
const availablePaymentMethods = computed(() => {
  const methods = [];
  const orderType = order.value?.order_type || 'pickup';
  
  if (orderType === 'pickup' || orderType === 'dine_in') {
    if (paymentMethods.value.cashOnPickup) methods.push('cash');
    if (paymentMethods.value.creditCard) methods.push('card');
    if (paymentMethods.value.onlinePayment) methods.push('online');
    if (paymentMethods.value.mobilePayment) methods.push('mobile');
  } else if (orderType === 'delivery') {
    if (paymentMethods.value.cashOnDelivery) methods.push('cash');
    if (paymentMethods.value.creditCard) methods.push('card');
    if (paymentMethods.value.onlinePayment) methods.push('online');
    if (paymentMethods.value.mobilePayment) methods.push('mobile');
  }
  
  return methods;
});

// Generate next steps message with dynamic payment information
const nextStepsMessage = computed(() => {
  const orderType = order.value?.order_type || 'pickup';
  const methods = availablePaymentMethods.value;
  
  // Base message
  let baseMessage = '';
  if (orderType === 'delivery') {
    baseMessage = t('orderConfirmation.nextStepsDelivery');
  } else {
    baseMessage = t('orderConfirmation.nextStepsPickup');
  }
  
  // Add payment information
  if (methods.length === 0) {
    return baseMessage + ' ' + t('orderConfirmation.paymentNoteDefault');
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
  
  if (methodNames.length === 0) {
    return baseMessage + ' ' + t('orderConfirmation.paymentNoteDefault');
  }
  
  let paymentNote = '';
  if (methodNames.length === 1) {
    if (orderType === 'delivery') {
      paymentNote = t('orderConfirmation.paymentNoteSingleDelivery', { method: methodNames[0] });
    } else {
      paymentNote = t('orderConfirmation.paymentNoteSingle', { method: methodNames[0] });
    }
  } else {
    const lastMethod = methodNames.pop();
    const methodsList = methodNames.join(', ') + ' ' + t('checkout.paymentOr') + ' ' + lastMethod;
    if (orderType === 'delivery') {
      paymentNote = t('orderConfirmation.paymentNoteMultipleDelivery', { methods: methodsList });
    } else {
      paymentNote = t('orderConfirmation.paymentNoteMultiple', { methods: methodsList });
    }
  }
  
  return baseMessage + ' ' + paymentNote;
});

const handleRegistration = async () => {
  if (!order.value || !website.value) {
    registrationError.value = t('orderConfirmation.orderNotLoaded');
    return;
  }

  registering.value = true;
  registrationError.value = '';
  isReturningCustomer.value = false;

  try {
    // Register the customer (or get existing customer)
    const result = await registerCustomer(website.value.id, {
      name: registrationForm.value.name,
      email: registrationForm.value.email,
      phone: registrationForm.value.phone,
      address: registrationForm.value.address
    });

    // Check if this is a returning customer
    if (result.isNew === false) {
      isReturningCustomer.value = true;
      customerInfo.value = result.customer;
    }

    // Link the current order to the customer
    if (result.customer && order.value.id) {
      try {
        await linkOrderToCustomer(result.customer.id, order.value.id);
      } catch (linkError) {
        console.error('Failed to link order to customer:', linkError);
        // Don't fail the registration if linking fails
      }
    }

    registrationCompleted.value = true;
    isRegistered.value = true;
    
    // Pre-fill form for future use
    localStorage.setItem(`customer_${website.value.id}`, JSON.stringify({
      name: registrationForm.value.name,
      email: registrationForm.value.email,
      phone: registrationForm.value.phone,
      address: registrationForm.value.address
    }));
  } catch (error) {
    console.error('Registration error:', error);
    registrationError.value = error.message || t('orderConfirmation.registrationFailed');
  } finally {
    registering.value = false;
  }
};

const skipRegistration = () => {
  isRegistered.value = true;
  registrationCompleted.value = false;
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
    website.value = await getWebsite(route.params.id);
    
    if (!route.params.orderNumber) {
      console.error('No order number in route params');
      return;
    }
    
    // Try to load from sessionStorage first (fallback)
    const cachedOrder = sessionStorage.getItem(`order_${route.params.orderNumber}`);
    if (cachedOrder) {
      try {
        order.value = JSON.parse(cachedOrder);
        console.log('Order loaded from cache:', order.value);
      } catch (e) {
        console.warn('Failed to parse cached order:', e);
      }
    }
    
    // Wait a bit to ensure order is committed to database
    await new Promise(resolve => setTimeout(resolve, 200));
    
    try {
      const fetchedOrder = await getOrderByNumber(route.params.orderNumber);
      
      if (fetchedOrder) {
        order.value = fetchedOrder;
        // Update cache with fresh data
        sessionStorage.setItem(`order_${route.params.orderNumber}`, JSON.stringify(fetchedOrder));
        console.log('Order loaded successfully from API:', order.value);
      } else if (!order.value) {
        console.error('Order not found, retrying...', route.params.orderNumber);
        // Try again after a short delay
        await new Promise(resolve => setTimeout(resolve, 500));
        const retryOrder = await getOrderByNumber(route.params.orderNumber);
        if (retryOrder) {
          order.value = retryOrder;
          sessionStorage.setItem(`order_${route.params.orderNumber}`, JSON.stringify(retryOrder));
        }
      }
    } catch (orderError) {
      console.error('Error fetching order:', orderError);
      // If we have cached order, use it
      if (!order.value && cachedOrder) {
        console.log('Using cached order due to API error');
      } else {
        // Try one more time after a longer delay
        await new Promise(resolve => setTimeout(resolve, 1000));
        try {
          const retryOrder = await getOrderByNumber(route.params.orderNumber);
          if (retryOrder) {
            order.value = retryOrder;
            sessionStorage.setItem(`order_${route.params.orderNumber}`, JSON.stringify(retryOrder));
          }
        } catch (retryError) {
          console.error('Failed to fetch order after retry:', retryError);
        }
      }
    }

    // Check if order already has a customer_id (user was already registered)
    if (order.value?.customer_id) {
      isRegistered.value = true;
      registrationCompleted.value = true;
    } else {
      // Pre-fill form with order information if available
      if (order.value) {
        registrationForm.value.name = order.value.customer_name || '';
        registrationForm.value.email = order.value.customer_email || '';
        registrationForm.value.phone = order.value.customer_phone || '';
        registrationForm.value.address = order.value.customer_address || '';

        // Try to load saved customer info from localStorage, but prioritize order data
        // Note: Email and Address should only come from order, not localStorage (since they're optional)
        const savedCustomer = localStorage.getItem(`customer_${website.value.id}`);
        if (savedCustomer) {
          try {
            const customerData = JSON.parse(savedCustomer);
            // Only use saved data if order data is missing (except email and address - always use order's values or empty)
            registrationForm.value.name = registrationForm.value.name || customerData.name || '';
            // Email should only come from order, not localStorage
            // registrationForm.value.email is already set from order.value.customer_email above
            registrationForm.value.phone = registrationForm.value.phone || customerData.phone || '';
            // Address should only come from order, not localStorage
            // registrationForm.value.address is already set from order.value.customer_address above
          } catch (e) {
            // Ignore parse errors
          }
        }
      }
    }
  } catch (error) {
    console.error('Failed to load order confirmation:', error);
  } finally {
    loading.value = false;
  }
});
</script>

