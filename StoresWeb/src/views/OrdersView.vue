<template>
  <div class="animate-fade-in">
    <h1 class="text-2xl font-bold text-gray-900 mb-6">{{ t('orders.title') }}</h1>
    <div v-if="auth.isLoggedIn && loading" class="space-y-4">
      <div v-for="i in 3" :key="i" class="h-24 bg-gray-200 rounded-2xl animate-pulse" />
    </div>
    <div v-else-if="!auth.isLoggedIn" class="card p-8 text-center">
      <p class="text-gray-600">{{ t('auth.noAccount') }}</p>
      <div class="flex flex-wrap justify-center gap-3 mt-4">
        <router-link to="/login" class="btn-primary">{{ t('nav.login') }}</router-link>
        <router-link to="/register" class="btn-secondary">{{ t('nav.register') }}</router-link>
      </div>
    </div>
    <div v-else-if="orders.length === 0" class="card p-8 text-center">
      <p class="text-gray-600">{{ t('orders.empty') }}</p>
      <router-link to="/" class="btn-primary inline-block mt-4">{{ t('cart.continueShopping') }}</router-link>
    </div>
    <div v-else class="space-y-4">
      <router-link
        v-for="order in orders"
        :key="order.id"
        :to="{ name: 'OrderTrack', params: { orderNumber: order.order_number } }"
        class="card p-4 sm:p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:shadow-xl transition"
      >
        <div>
          <p class="font-semibold text-gray-900">{{ t('orders.orderNumber') }} #{{ order.order_number }}</p>
          <p class="text-sm text-gray-500 mt-1">{{ formatDate(order.created_at) }}</p>
          <p class="text-indigo-600 font-medium mt-1">{{ statusLabel(order.status) }}</p>
        </div>
        <p class="text-lg font-bold text-gray-900">{{ formatPrice(order.total_amount) }}</p>
        <span class="btn-secondary text-sm py-2">{{ t('orders.viewTrack') }}</span>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { fetchCustomerOrders } from '@/api/orders'
import { formatPrice as formatPriceUtil } from '@/utils/currency'

const { t } = useI18n()
const auth = useAuthStore()
const orders = ref([])
const loading = ref(true)

onMounted(async () => {
  if (!auth.isLoggedIn) {
    loading.value = false
    return
  }
  try {
    orders.value = await fetchCustomerOrders(auth.customer.id)
  } catch {
    orders.value = []
  } finally {
    loading.value = false
  }
})

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString(undefined, { dateStyle: 'medium' })
}

function formatPrice(value) {
  return formatPriceUtil(value, 'USD', 'before')
}

function statusLabel(status) {
  return t(`status.${status}`) || status
}
</script>
