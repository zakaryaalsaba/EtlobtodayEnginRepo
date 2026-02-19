<template>
  <div class="min-h-screen bg-gray-50 py-12 px-4 sm:px-6 lg:px-8" :dir="$i18n.locale === 'ar' ? 'rtl' : 'ltr'">
    <div class="max-w-4xl mx-auto">
      <!-- Header -->
      <div class="mb-8">
        <div class="flex items-center justify-between mb-4">
          <button
            @click="$router.push(`/website/${route.params.id}`)"
            class="flex items-center gap-2 text-gray-600 hover:text-gray-800"
            :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''"
          >
            <span v-if="$i18n.locale === 'ar'">→</span>
            <span v-else>←</span>
            {{ $t('orderTracking.backToMenu') }}
          </button>
          <LanguageSwitcher v-if="false" />
        </div>
        <h1 class="text-4xl font-bold mb-2" :style="{ color: website?.primary_color || '#4F46E5' }">
          {{ $t('orderTracking.title') }}
        </h1>
        <p class="text-gray-600">{{ $t('orderTracking.subtitle') }}</p>
      </div>

      <!-- Order Number Input -->
      <div v-if="!order" class="bg-white rounded-lg shadow-lg p-8 mb-8">
        <form @submit.prevent="trackOrder" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">
              {{ $t('orderTracking.orderNumber') }}
            </label>
            <input
              v-model="orderNumberInput"
              type="text"
              required
              :placeholder="$t('orderTracking.orderNumberPlaceholder')"
              class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 text-lg"
            />
            <p class="text-xs text-gray-500 mt-2">{{ $t('orderTracking.orderNumberHint') }}</p>
          </div>
          <button
            type="submit"
            :disabled="loading"
            :style="{ backgroundColor: website?.primary_color || '#4F46E5' }"
            class="w-full px-6 py-3 text-white rounded-lg font-semibold hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed transition-opacity"
          >
            {{ loading ? $t('orderTracking.tracking') : $t('orderTracking.trackOrder') }}
          </button>
        </form>
      </div>

      <!-- Order Details -->
      <div v-if="order" class="space-y-6">
        <!-- Order Status Card -->
        <div class="bg-white rounded-lg shadow-lg p-8">
          <div class="flex items-center justify-between mb-6">
            <div>
              <h2 class="text-2xl font-bold mb-2">{{ $t('orderTracking.order') }} #{{ order.order_number }}</h2>
              <p class="text-gray-500">{{ $t('orderTracking.placedOn') }} {{ formatDate(order.created_at) }}</p>
            </div>
            <div :class="$i18n.locale === 'ar' ? 'text-left' : 'text-right'">
              <div class="text-sm text-gray-500 mb-1">{{ $t('orderTracking.status') }}</div>
              <span
                class="px-4 py-2 rounded-full text-white font-semibold text-sm"
                :style="{ backgroundColor: getStatusColor(order.status) }"
              >
                {{ formatStatus(order.status) }}
              </span>
            </div>
          </div>

          <!-- Status Timeline -->
          <div class="mb-6">
            <h3 class="text-lg font-semibold mb-4">{{ $t('orderTracking.orderProgress') }}</h3>
            <div class="space-y-4">
              <div
                v-for="(statusItem, index) in statusTimeline"
                :key="index"
                class="flex items-start gap-4"
                :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''"
              >
                <div class="flex flex-col items-center">
                  <div
                    class="w-10 h-10 rounded-full flex items-center justify-center font-semibold transition-all"
                    :class="statusItem.completed ? 'text-white shadow-lg' : 'text-gray-400 border-2 border-gray-300'"
                    :style="statusItem.completed ? { backgroundColor: website?.primary_color || '#4F46E5' } : {}"
                  >
                    <span v-if="statusItem.completed">✓</span>
                    <span v-else>{{ index + 1 }}</span>
                  </div>
                  <div
                    v-if="index < statusTimeline.length - 1"
                    class="w-0.5 h-12 transition-colors"
                    :class="statusItem.completed ? 'bg-indigo-500' : 'bg-gray-300'"
                  ></div>
                </div>
                <div class="flex-1 pb-4" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'">
                  <div class="font-semibold text-gray-800">{{ statusItem.label }}</div>
                  <div class="text-sm text-gray-500">{{ statusItem.description }}</div>
                  <div v-if="statusItem.completed && statusItem.timestamp" class="text-xs text-gray-400 mt-1">
                    {{ formatDate(statusItem.timestamp) }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Payment Status -->
          <div class="border-t border-gray-200 pt-4">
              <div class="flex items-center justify-between">
              <span class="text-gray-600">{{ $t('orderTracking.paymentStatus') }}:</span>
              <span
                class="px-3 py-1 rounded-full text-sm font-semibold"
                :class="order.payment_status === 'paid' ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'"
              >
                {{ order.payment_status === 'paid' ? $t('orderTracking.paid') : $t('orderTracking.payOnPickup') }}
              </span>
            </div>
          </div>
        </div>

        <!-- Order Items -->
        <div class="bg-white rounded-lg shadow-lg p-8">
          <h3 class="text-xl font-bold mb-4">{{ $t('orderTracking.orderItems') }}</h3>
          <div class="space-y-4">
            <div
              v-for="(item, index) in order.items"
              :key="index"
              class="flex items-center gap-4 pb-4 border-b border-gray-200 last:border-0"
            >
              <div class="flex-1">
                <h4 class="font-semibold text-gray-800">{{ item.product_name }}</h4>
                <p class="text-sm text-gray-500">{{ $t('orderTracking.quantity') }}: {{ item.quantity }}</p>
              </div>
              <div class="text-right">
                <p class="font-semibold text-gray-800">${{ parseFloat(item.subtotal).toFixed(2) }}</p>
                <p class="text-sm text-gray-500">${{ parseFloat(item.product_price).toFixed(2) }} {{ $t('orderTracking.each') }}</p>
              </div>
            </div>
          </div>
          <div class="mt-6 pt-4 border-t border-gray-200">
            <div class="flex items-center justify-between text-lg font-bold" :style="{ color: website?.primary_color || '#4F46E5' }">
              <span>{{ $t('orderTracking.total') }}</span>
              <span>${{ parseFloat(order.total_amount).toFixed(2) }}</span>
            </div>
          </div>
        </div>

        <!-- Customer Information -->
        <div class="bg-white rounded-lg shadow-lg p-8">
          <h3 class="text-xl font-bold mb-4">{{ $t('orderTracking.customerInformation') }}</h3>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <p class="text-sm text-gray-500 mb-1">{{ $t('orderTracking.name') }}</p>
              <p class="font-semibold">{{ order.customer_name }}</p>
            </div>
            <div v-if="order.customer_phone">
              <p class="text-sm text-gray-500 mb-1">{{ $t('orderTracking.phone') }}</p>
              <p class="font-semibold">{{ order.customer_phone }}</p>
            </div>
            <div v-if="order.customer_email">
              <p class="text-sm text-gray-500 mb-1">{{ $t('orderTracking.email') }}</p>
              <p class="font-semibold">{{ order.customer_email }}</p>
            </div>
            <div v-if="order.customer_address">
              <p class="text-sm text-gray-500 mb-1">{{ $t('orderTracking.address') }}</p>
              <p class="font-semibold">{{ order.customer_address }}</p>
            </div>
          </div>
          <div v-if="order.notes" class="mt-4">
            <p class="text-sm text-gray-500 mb-1">{{ $t('orderTracking.specialInstructions') }}</p>
            <p class="font-semibold">{{ order.notes }}</p>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="flex gap-4">
          <button
            @click="order = null; orderNumberInput = ''"
            class="flex-1 px-6 py-3 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition-colors font-semibold"
          >
            {{ $t('orderTracking.trackAnotherOrder') }}
          </button>
          <button
            @click="$router.push(`/website/${route.params.id}`)"
            :style="{ backgroundColor: website?.primary_color || '#4F46E5' }"
            class="flex-1 px-6 py-3 text-white rounded-lg hover:opacity-90 transition-opacity font-semibold"
          >
            {{ $t('orderTracking.backToMenu') }}
          </button>
        </div>
      </div>

      <!-- Error Message -->
      <div v-if="error" class="bg-red-50 border border-red-200 rounded-lg p-4 mb-4">
        <p class="text-red-800">{{ error }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import LanguageSwitcher from './LanguageSwitcher.vue';
import { getWebsite, getOrderByNumber } from '../services/api.js';

const { locale, t } = useI18n();

const route = useRoute();
const router = useRouter();

const website = ref(null);
const order = ref(null);
const orderNumberInput = ref('');
const loading = ref(false);
const error = ref('');

const statusTimeline = computed(() => {
  if (!order.value) return [];
  
  const statuses = [
    { key: 'pending', label: t('orderTracking.statusReceived'), description: t('orderTracking.statusReceivedDesc'), completed: false },
    { key: 'confirmed', label: t('orderTracking.statusConfirmed'), description: t('orderTracking.statusConfirmedDesc'), completed: false },
    { key: 'preparing', label: t('orderTracking.statusPreparing'), description: t('orderTracking.statusPreparingDesc'), completed: false },
    { key: 'ready', label: t('orderTracking.statusReady'), description: t('orderTracking.statusReadyDesc'), completed: false },
    { key: 'completed', label: t('orderTracking.statusCompleted'), description: t('orderTracking.statusCompletedDesc'), completed: false },
  ];

  const currentStatus = order.value.status;
  const statusOrder = ['pending', 'confirmed', 'preparing', 'ready', 'completed'];
  const currentIndex = statusOrder.indexOf(currentStatus);

  return statuses.map((status, index) => {
    const statusIndex = statusOrder.indexOf(status.key);
    return {
      ...status,
      completed: statusIndex <= currentIndex,
      timestamp: statusIndex <= currentIndex ? order.value.updated_at : null
    };
  }).filter(status => status.key !== 'cancelled');
});

const formatStatus = (status) => {
  const statusMap = {
    'pending': t('orderTracking.statusPending'),
    'confirmed': t('orderTracking.statusConfirmed'),
    'preparing': t('orderTracking.statusPreparing'),
    'ready': t('orderTracking.statusReady'),
    'completed': t('orderTracking.statusCompleted'),
    'cancelled': t('orderTracking.statusCancelled')
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

const formatDate = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const trackOrder = async () => {
  if (!orderNumberInput.value.trim()) {
    error.value = t('orderTracking.pleaseEnterOrderNumber');
    return;
  }

  loading.value = true;
  error.value = '';

  try {
    order.value = await getOrderByNumber(orderNumberInput.value.trim());
    
    // Verify the order belongs to this website
    if (order.value.website_id !== parseInt(route.params.id)) {
      error.value = t('orderTracking.orderNotBelong');
      order.value = null;
    }
  } catch (err) {
    error.value = err.message || t('orderTracking.orderNotFound');
    order.value = null;
  } finally {
    loading.value = false;
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
    website.value = await getWebsite(route.params.id);
    
    // If order number is in route params, auto-track
    if (route.params.orderNumber) {
      orderNumberInput.value = route.params.orderNumber;
      await trackOrder();
    }
  } catch (error) {
    console.error('Failed to load website:', error);
  }
});
</script>

