<template>
  <div class="animate-fade-in max-w-2xl mx-auto">
    <h1 class="text-2xl font-bold text-gray-900 mb-6">{{ t('orderTrack.title') }}</h1>
    <div v-if="loading" class="card p-8">
      <p class="text-gray-600">{{ t('common.loading') }}</p>
    </div>
    <div v-else-if="error" class="rounded-2xl bg-red-50 border border-red-200 p-6 text-red-700">
      {{ error }}
    </div>
    <div v-else-if="order" class="card p-6 space-y-6">
      <div>
        <p class="text-gray-600">{{ t('orderTrack.orderNumber') }}</p>
        <p class="text-xl font-bold text-gray-900">#{{ order.order_number }}</p>
      </div>
      <div>
        <p class="text-gray-600">{{ t('orderTrack.status') }}</p>
        <p class="text-lg font-semibold text-indigo-600">{{ statusLabel(order.status) }}</p>
      </div>
      <div class="border-t border-gray-200 pt-4">
        <p class="text-gray-600 mb-2">{{ t('common.total') }}</p>
        <p class="text-2xl font-bold text-gray-900">{{ formatPrice(order.total_amount) }}</p>
      </div>
      <div v-if="order.items?.length" class="border-t border-gray-200 pt-4">
        <p class="font-medium text-gray-900 mb-2">Items</p>
        <ul class="space-y-1">
          <li v-for="item in order.items" :key="item.id" class="flex justify-between text-sm">
            <span>{{ item.product_name }} × {{ item.quantity }}</span>
            <span>{{ formatPrice(item.subtotal) }}</span>
          </li>
        </ul>
      </div>
      <router-link to="/" class="btn-primary block text-center w-full">{{ t('nav.stores') }}</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { fetchOrderByNumber } from '@/api/orders'
import { formatPrice as formatPriceUtil } from '@/utils/currency'

const route = useRoute()
const { t } = useI18n()
const order = ref(null)
const loading = ref(true)
const error = ref(null)

onMounted(async () => {
  try {
    order.value = await fetchOrderByNumber(route.params.orderNumber)
  } catch (e) {
    error.value = e.body?.error || e.message || t('common.error')
  } finally {
    loading.value = false
  }
})

function formatPrice(value) {
  const o = order.value
  const code = o?.currency_code || 'USD'
  const pos = o?.currency_symbol_position || 'before'
  return formatPriceUtil(value, code, pos)
}

function statusLabel(status) {
  return t(`status.${status}`) || status
}
</script>
