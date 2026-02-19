<template>
  <div class="card overflow-hidden group">
    <router-link :to="{ name: 'ProductDetail', params: { id: product.id } }" class="block">
      <div class="aspect-[4/3] bg-gray-200 overflow-hidden">
        <img
          v-if="product.image_url"
          :src="imageUrl(product.image_url)"
          :alt="productName(product)"
          class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
        />
        <div v-else class="w-full h-full flex items-center justify-center text-5xl text-gray-400">📦</div>
      </div>
      <div class="p-4">
        <h3 class="font-semibold text-gray-900">{{ productName(product) }}</h3>
        <p class="mt-1 text-indigo-600 font-bold">{{ formatPrice(product.price, currencyCode, symbolPosition) }}</p>
      </div>
    </router-link>
    <div class="px-4 pb-4">
      <button
        type="button"
        class="w-full btn-primary py-2.5 text-sm"
        @click.prevent="addToCart"
      >
        {{ t('product.addToCart') }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { useCartStore } from '@/stores/cart'
import { useToastStore } from '@/stores/toast'

const props = defineProps({
  product: { type: Object, required: true },
  storeId: { type: [Number, String], default: null },
  currencyCode: { type: String, default: 'USD' },
  symbolPosition: { type: String, default: 'before' }
})

const { t, locale } = useI18n()
const cart = useCartStore()
const toast = useToastStore()

const baseURL = import.meta.env.VITE_API_BASE_URL || ''

function imageUrl(path) {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return `${baseURL}${path.startsWith('/') ? '' : '/'}${path}`
}

function productName(p) {
  return locale.value === 'ar' && p.name_ar ? p.name_ar : (p.name || 'Product')
}

import { formatPrice as formatPriceUtil } from '@/utils/currency'

function formatPrice(price, code = props.currencyCode, pos = props.symbolPosition) {
  return formatPriceUtil(price, code, pos)
}

function addToCart() {
  cart.addItem(props.product, 1, props.storeId ? Number(props.storeId) : null)
  toast.show(t('cart.addedToCart'), { duration: 3500, cartLink: true })
}
</script>
