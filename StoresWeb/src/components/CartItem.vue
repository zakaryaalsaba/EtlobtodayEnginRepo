<template>
  <div class="card p-4 flex flex-col sm:flex-row gap-4">
    <div class="w-full sm:w-24 h-24 rounded-xl bg-gray-200 shrink-0 overflow-hidden">
      <img
        v-if="item.product?.image_url"
        :src="imageUrl(item.product.image_url)"
        :alt="item.product_name"
        class="w-full h-full object-cover"
      />
      <div v-else class="w-full h-full flex items-center justify-center text-3xl text-gray-400">📦</div>
    </div>
    <div class="flex-1 min-w-0">
      <h3 class="font-semibold text-gray-900">{{ item.product_name }}</h3>
      <p class="text-indigo-600 font-medium">{{ formatPrice(item.product_price) }} × {{ item.quantity }}</p>
    </div>
    <div class="flex items-center gap-2">
      <div class="flex items-center rounded-lg border border-gray-200 bg-gray-50 overflow-hidden">
        <button
          type="button"
          class="w-10 h-10 flex items-center justify-center text-gray-600 hover:bg-gray-100"
          @click="$emit('update-quantity', item.quantity - 1)"
        >
          −
        </button>
        <span class="w-10 text-center font-medium">{{ item.quantity }}</span>
        <button
          type="button"
          class="w-10 h-10 flex items-center justify-center text-gray-600 hover:bg-gray-100"
          @click="$emit('update-quantity', item.quantity + 1)"
        >
          +
        </button>
      </div>
      <button
        type="button"
        class="p-2 text-red-600 hover:bg-red-50 rounded-lg transition"
        :aria-label="t('cart.remove')"
        @click="$emit('remove')"
      >
        🗑️
      </button>
    </div>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'

const props = defineProps({
  item: { type: Object, required: true },
  currencyCode: { type: String, default: 'USD' },
  symbolPosition: { type: String, default: 'before' }
})

defineEmits(['update-quantity', 'remove'])

const { t } = useI18n()
const baseURL = import.meta.env.VITE_API_BASE_URL || ''

import { formatPrice as formatPriceUtil } from '@/utils/currency'

function imageUrl(path) {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return `${baseURL}${path.startsWith('/') ? '' : '/'}${path}`
}

function formatPrice(price) {
  return formatPriceUtil(price, props.currencyCode, props.symbolPosition)
}
</script>
