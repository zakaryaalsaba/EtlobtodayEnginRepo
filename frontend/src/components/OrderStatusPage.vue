<template>
  <div v-if="loading" class="min-h-screen flex items-center justify-center bg-gray-50">
    <div class="text-center">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto"></div>
      <p class="mt-4 text-gray-600">{{ $t('orderConfirmation.loading') }}</p>
    </div>
  </div>

  <div v-else class="min-h-screen bg-gray-50 py-12 px-4 sm:px-6 lg:px-8" :dir="$i18n.locale === 'ar' ? 'rtl' : 'ltr'">
    <div class="max-w-4xl mx-auto">
      <div class="mb-6">
        <button
          type="button"
          @click="router.push(`/website/${route.params.id}`)"
          class="flex items-center gap-2 text-gray-600 hover:text-gray-800"
          :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''"
        >
          <span v-if="$i18n.locale === 'ar'">→</span>
          <span v-else>←</span>
          {{ $t('orderTracking.backToMenu') }}
        </button>
      </div>

      <div class="bg-white rounded-lg shadow-xl p-8 mb-8 text-center">
        <div class="mb-4">
          <div
            class="w-20 h-20 mx-auto mb-4 rounded-full flex items-center justify-center"
            :style="{ backgroundColor: (website?.primary_color || '#4F46E5') + '20' }"
          >
            <svg class="w-12 h-12" :style="{ color: website?.primary_color || '#4F46E5' }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <h1 class="text-3xl font-bold mb-2" :style="{ color: website?.primary_color || '#4F46E5' }">
            {{ $t('orderConfirmation.title') }}
          </h1>
          <p class="text-gray-600">{{ $t('orderConfirmation.thankYou') }}</p>
          <p class="text-sm text-gray-500 mt-2">{{ $t('orderStatus.mergeHint') }}</p>
        </div>
        <div class="bg-gray-50 rounded-lg p-6">
          <p class="text-sm text-gray-500 mb-2">{{ $t('orderConfirmation.orderNumber') }}</p>
          <p class="text-2xl font-bold" :style="{ color: website?.primary_color || '#4F46E5' }">
            {{ order?.order_number || route.params.orderNumber }}
          </p>
        </div>
      </div>

      <div
        v-if="firebaseRealtimeOn"
        class="mb-6 rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-900"
      >
        {{ $t('orderStatus.firebaseLive') }}
      </div>
      <div
        v-else-if="!firebaseConfigured"
        class="mb-6 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900"
      >
        {{ $t('orderStatus.firebaseNotConfigured') }}
      </div>
      <div
        v-if="liveFeedEnded"
        class="mb-6 rounded-lg border border-gray-200 bg-gray-50 px-4 py-3 text-sm text-gray-700"
      >
        {{ $t('orderStatus.liveFeedEnded') }}
      </div>

      <div v-if="order" class="space-y-6">
        <div class="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <p class="text-sm text-blue-800">
            <strong>{{ $t('orderConfirmation.whatsNext') }}</strong> {{ nextStepsMessage }}
          </p>
        </div>

        <div class="bg-white rounded-lg shadow-lg p-8">
          <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
            <div>
              <h2 class="text-2xl font-bold mb-2">{{ $t('orderTracking.order') }} #{{ order.order_number }}</h2>
              <p class="text-gray-500">{{ $t('orderTracking.placedOn') }} {{ formatDate(order.created_at) }}</p>
              <p class="text-xs text-gray-500 mt-2">
                {{ firebaseRealtimeOn ? $t('orderStatus.updatesRealtime') : $t('orderTracking.autoRefreshNotice') }}
              </p>
            </div>
            <div :class="$i18n.locale === 'ar' ? 'text-left' : 'text-right'">
              <button
                type="button"
                @click="manualRefresh"
                :disabled="refreshing"
                class="mb-3 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 text-sm font-semibold"
              >
                {{ refreshing ? $t('orderTracking.refreshing') : $t('orderTracking.refreshStatus') }}
              </button>
              <div class="text-sm text-gray-500 mb-1">{{ $t('orderTracking.status') }}</div>
              <span
                class="inline-block px-4 py-2 rounded-full text-white font-semibold text-sm"
                :style="{ backgroundColor: getStatusColor(order.status) }"
              >
                {{ formatStatus(order.status) }}
              </span>
            </div>
          </div>

          <div class="mb-6">
            <h3 class="text-lg font-semibold mb-4">{{ $t('orderTracking.orderProgress') }}</h3>
            <div class="space-y-4">
              <div
                v-for="(statusItem, index) in statusTimeline"
                :key="statusItem.key + index"
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
                    class="w-0.5 h-12"
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

          <div class="border-t border-gray-200 pt-4 flex justify-between items-center">
            <span class="text-gray-600">{{ $t('orderTracking.paymentStatus') }}:</span>
            <span
              class="px-3 py-1 rounded-full text-sm font-semibold"
              :class="order.payment_status === 'paid' ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'"
            >
              {{ order.payment_status === 'paid' ? $t('orderTracking.paid') : $t('orderTracking.payOnPickup') }}
            </span>
          </div>
        </div>

        <div class="bg-white rounded-lg shadow-lg p-8">
          <h3 class="text-xl font-bold mb-4">{{ $t('orderTracking.orderItems') }}</h3>
          <div class="space-y-4">
            <div
              v-for="(item, index) in normalizedItems"
              :key="'item-' + index"
              class="flex items-center gap-4 pb-4 border-b border-gray-200 last:border-0"
            >
              <div class="flex-1">
                <h4 class="font-semibold text-gray-800">{{ item.product_name }}</h4>
                <p class="text-sm text-gray-500">{{ $t('orderTracking.quantity') }}: {{ item.quantity }}</p>
              </div>
              <div :class="$i18n.locale === 'ar' ? 'text-left' : 'text-right'">
                <p class="font-semibold text-gray-800">{{ formatCurrency(item.subtotal) }}</p>
                <p class="text-sm text-gray-500">{{ formatCurrency(item.product_price) }} {{ $t('orderTracking.each') }}</p>
              </div>
            </div>
          </div>
          <div class="mt-6 pt-4 border-t border-gray-200 flex justify-between text-lg font-bold" :style="{ color: website?.primary_color || '#4F46E5' }">
            <span>{{ $t('orderTracking.total') }}</span>
            <span>{{ formatCurrency(order.total_amount) }}</span>
          </div>
        </div>

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

        <div class="flex flex-col sm:flex-row gap-4">
          <button
            type="button"
            @click="router.push(`/website/${route.params.id}/track`)"
            class="flex-1 px-6 py-3 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 font-semibold"
          >
            {{ $t('orderTracking.trackAnotherOrder') }}
          </button>
          <button
            type="button"
            @click="router.push(`/website/${route.params.id}`)"
            :style="{ backgroundColor: website?.primary_color || '#4F46E5' }"
            class="flex-1 px-6 py-3 text-white rounded-lg hover:opacity-90 font-semibold"
          >
            {{ $t('orderConfirmation.backToMenu') }}
          </button>
        </div>
      </div>

      <div v-else class="bg-white rounded-lg shadow p-8 text-center text-gray-600">
        {{ $t('orderConfirmation.orderNotLoaded') }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ref as dbRef, onValue } from 'firebase/database';
import { getWebsite, getOrderByNumber } from '../services/api.js';
import { getFirebaseRealtimeDb, isFirebaseRealtimeConfigured } from '../config/firebaseClient.js';

const { locale, t } = useI18n();
const route = useRoute();
const router = useRouter();

const website = ref(null);
const order = ref(null);
const loading = ref(true);
const refreshing = ref(false);
const liveFeedEnded = ref(false);
const firebaseRealtimeOn = ref(false);
const sawFirebasePayload = ref(false);

const firebaseConfigured = isFirebaseRealtimeConfigured();
let firebaseUnsubscribe = null;

const POLL_MS = 5 * 60 * 1000;
const STOP_POLL = new Set(['ready', 'completed', 'cancelled']);
let pollId = null;

const websiteIdNum = () => parseInt(String(route.params.id), 10);
const orderNum = () => String(route.params.orderNumber || '');

function normalizeItems(items) {
  if (!items) return [];
  if (Array.isArray(items)) return items;
  if (typeof items === 'object') {
    return Object.keys(items)
      .filter((k) => k !== 'length')
      .sort((a, b) => Number(a) - Number(b))
      .map((k) => items[k])
      .filter(Boolean);
  }
  return [];
}

function normalizeFirebaseOrder(data, wid) {
  if (!data || typeof data !== 'object') return null;
  const items = normalizeItems(data.items);
  return {
    id: data.id,
    website_id: data.website_id != null ? Number(data.website_id) : wid,
    order_number: data.order_number,
    customer_name: data.customer_name,
    customer_phone: data.customer_phone,
    customer_email: data.customer_email,
    customer_address: data.customer_address,
    order_type: data.order_type,
    status: data.status,
    total_amount: data.total_amount,
    payment_method: data.payment_method,
    payment_status: data.payment_status,
    notes: data.notes,
    created_at: data.created_at,
    updated_at: data.updated_at,
    items
  };
}

function mergeOrder(base, fb) {
  const b = base || {};
  if (!fb) return Object.keys(b).length ? b : null;
  const mergedItems = fb.items?.length ? fb.items : normalizeItems(b.items);
  return {
    ...b,
    ...fb,
    items: mergedItems
  };
}

const normalizedItems = computed(() => normalizeItems(order.value?.items));

const formatStatus = (status) => {
  const statusMap = {
    pending: t('orderTracking.statusPending'),
    confirmed: t('orderTracking.statusConfirmed'),
    preparing: t('orderTracking.statusPreparing'),
    ready: t('orderTracking.statusReady'),
    completed: t('orderTracking.statusCompleted'),
    cancelled: t('orderTracking.statusCancelled')
  };
  return statusMap[status] || status;
};

const getStatusColor = (status) => {
  const colorMap = {
    pending: '#F59E0B',
    confirmed: '#3B82F6',
    preparing: '#8B5CF6',
    ready: '#10B981',
    completed: '#059669',
    cancelled: '#EF4444'
  };
  return colorMap[status] || '#6B7280';
};

const formatDate = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  const loc = locale.value === 'ar' ? 'ar' : 'en-US';
  return date.toLocaleString(loc, {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const getCurrencySymbol = (currencyCode) => {
  const currentLang = locale.value || 'en';
  if (currencyCode === 'JOD') {
    return currentLang === 'ar' ? 'د.ا' : 'JD';
  }
  const symbols = { USD: '$', EUR: '€', GBP: '£', SAR: 'ر.س' };
  return symbols[currencyCode] || '$';
};

const formatCurrency = (amount) => {
  if (!website.value) {
    return `$${parseFloat(amount || 0).toFixed(2)}`;
  }
  const currencyCode = website.value.currency_code || 'USD';
  const symbolPosition = website.value.currency_symbol_position || 'before';
  const symbol = getCurrencySymbol(currencyCode);
  const formattedAmount = parseFloat(amount || 0).toFixed(2);
  return symbolPosition === 'after' ? `${formattedAmount} ${symbol}` : `${symbol}${formattedAmount}`;
};

const paymentMethods = computed(() => {
  if (!website.value?.payment_methods) {
    return {
      cashOnPickup: true,
      cashOnDelivery: true,
      creditCard: false,
      onlinePayment: false,
      mobilePayment: false
    };
  }
  try {
    const parsed =
      typeof website.value.payment_methods === 'string'
        ? JSON.parse(website.value.payment_methods)
        : website.value.payment_methods;
    return {
      cashOnPickup: parsed.cashOnPickup !== false,
      cashOnDelivery: parsed.cashOnDelivery !== false,
      creditCard: parsed.creditCard === true,
      onlinePayment: parsed.onlinePayment === true,
      mobilePayment: parsed.mobilePayment === true
    };
  } catch {
    return {
      cashOnPickup: true,
      cashOnDelivery: true,
      creditCard: false,
      onlinePayment: false,
      mobilePayment: false
    };
  }
});

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

const nextStepsMessage = computed(() => {
  const orderType = order.value?.order_type || 'pickup';
  const methods = availablePaymentMethods.value;
  let baseMessage =
    orderType === 'delivery' ? t('orderConfirmation.nextStepsDelivery') : t('orderConfirmation.nextStepsPickup');
  if (methods.length === 0) {
    return `${baseMessage} ${t('orderConfirmation.paymentNoteDefault')}`;
  }
  const methodNames = [];
  if (methods.includes('cash')) {
    methodNames.push(orderType === 'delivery' ? t('checkout.paymentCashOnDelivery') : t('checkout.paymentCashOnPickup'));
  }
  if (methods.includes('card')) methodNames.push(t('checkout.paymentCard'));
  if (methods.includes('online')) methodNames.push(t('checkout.paymentOnline'));
  if (methods.includes('mobile')) methodNames.push(t('checkout.paymentMobile'));
  if (methodNames.length === 0) {
    return `${baseMessage} ${t('orderConfirmation.paymentNoteDefault')}`;
  }
  if (methodNames.length === 1) {
    const paymentNote =
      orderType === 'delivery'
        ? t('orderConfirmation.paymentNoteSingleDelivery', { method: methodNames[0] })
        : t('orderConfirmation.paymentNoteSingle', { method: methodNames[0] });
    return `${baseMessage} ${paymentNote}`;
  }
  const lastMethod = methodNames.pop();
  const methodsList = `${methodNames.join(', ')} ${t('checkout.paymentOr')} ${lastMethod}`;
  const paymentNote =
    orderType === 'delivery'
      ? t('orderConfirmation.paymentNoteMultipleDelivery', { methods: methodsList })
      : t('orderConfirmation.paymentNoteMultiple', { methods: methodsList });
  return `${baseMessage} ${paymentNote}`;
});

const statusTimeline = computed(() => {
  if (!order.value) return [];
  if (order.value.status === 'cancelled') {
    return [
      {
        key: 'cancelled',
        label: t('orderTracking.statusCancelled'),
        description: t('orderTracking.statusCancelled'),
        completed: true,
        timestamp: order.value.updated_at || order.value.created_at
      }
    ];
  }
  const statuses = [
    { key: 'pending', label: t('orderTracking.statusReceived'), description: t('orderTracking.statusReceivedDesc') },
    { key: 'confirmed', label: t('orderTracking.statusConfirmed'), description: t('orderTracking.statusConfirmedDesc') },
    { key: 'preparing', label: t('orderTracking.statusPreparing'), description: t('orderTracking.statusPreparingDesc') },
    { key: 'ready', label: t('orderTracking.statusReady'), description: t('orderTracking.statusReadyDesc') },
    { key: 'completed', label: t('orderTracking.statusCompleted'), description: t('orderTracking.statusCompletedDesc') }
  ];
  const currentStatus = order.value.status;
  const statusOrder = ['pending', 'confirmed', 'preparing', 'ready', 'completed'];
  const currentIndex = statusOrder.indexOf(currentStatus);
  return statuses.map((status, index) => {
    const statusIndex = statusOrder.indexOf(status.key);
    return {
      ...status,
      completed: statusIndex <= currentIndex && currentIndex >= 0,
      timestamp: statusIndex <= currentIndex && currentIndex >= 0 ? order.value.updated_at : null
    };
  });
});

async function loadOrderFromApi() {
  const num = orderNum();
  if (!num) return;
  const o = await getOrderByNumber(num);
  if (o.website_id !== websiteIdNum()) {
    throw new Error(t('orderTracking.orderNotBelong'));
  }
  order.value = o;
  sessionStorage.setItem(`order_${num}`, JSON.stringify(o));
}

async function refreshFromApi() {
  try {
    await loadOrderFromApi();
  } catch (e) {
    console.warn('API refresh failed:', e);
  }
}

function startPoll() {
  stopPoll();
  if (firebaseConfigured) return;
  pollId = window.setInterval(async () => {
    if (order.value?.order_number && !STOP_POLL.has(order.value.status)) {
      await refreshFromApi();
    }
  }, POLL_MS);
}

function stopPoll() {
  if (pollId) {
    clearInterval(pollId);
    pollId = null;
  }
}

function attachFirebase() {
  if (!firebaseConfigured || !orderNum()) return;
  const db = getFirebaseRealtimeDb();
  if (!db) return;
  const path = `orders/${websiteIdNum()}/${orderNum()}`;
  const r = dbRef(db, path);
  firebaseUnsubscribe = onValue(r, (snap) => {
    const val = snap.val();
    firebaseRealtimeOn.value = true;
    if (val == null) {
      if (sawFirebasePayload.value) {
        liveFeedEnded.value = true;
        refreshFromApi();
      }
      return;
    }
    sawFirebasePayload.value = true;
    liveFeedEnded.value = false;
    const normalized = normalizeFirebaseOrder(val, websiteIdNum());
    order.value = mergeOrder(order.value, normalized);
  });
}

function detachFirebase() {
  if (typeof firebaseUnsubscribe === 'function') {
    firebaseUnsubscribe();
    firebaseUnsubscribe = null;
  }
}

const manualRefresh = async () => {
  if (refreshing.value) return;
  refreshing.value = true;
  try {
    await refreshFromApi();
  } finally {
    refreshing.value = false;
  }
};

watch(
  () => order.value?.status,
  (st) => {
    if (st && STOP_POLL.has(st)) {
      stopPoll();
    } else if (!firebaseConfigured) {
      startPoll();
    }
  }
);

onMounted(async () => {
  const savedLang = localStorage.getItem('appLanguage') || 'en';
  locale.value = savedLang;
  document.documentElement.setAttribute('lang', savedLang);
  document.documentElement.setAttribute('dir', savedLang === 'ar' ? 'rtl' : 'ltr');

  try {
    website.value = await getWebsite(route.params.id);
    const num = orderNum();
    if (!num) {
      loading.value = false;
      return;
    }
    const cached = sessionStorage.getItem(`order_${num}`);
    if (cached) {
      try {
        order.value = JSON.parse(cached);
      } catch {
        /* ignore */
      }
    }
    await new Promise((r) => setTimeout(r, 200));
    try {
      await loadOrderFromApi();
    } catch (e) {
      console.error(e);
      await new Promise((r) => setTimeout(r, 500));
      try {
        await loadOrderFromApi();
      } catch (e2) {
        console.error(e2);
      }
    }
    attachFirebase();
    if (!firebaseConfigured && order.value && !STOP_POLL.has(order.value.status)) {
      startPoll();
    }
  } catch (e) {
    console.error('Order status page load error', e);
  } finally {
    loading.value = false;
  }
});

onUnmounted(() => {
  detachFirebase();
  stopPoll();
});
</script>
