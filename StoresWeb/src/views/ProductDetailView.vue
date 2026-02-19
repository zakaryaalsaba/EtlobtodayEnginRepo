<template>
  <div class="animate-fade-in max-w-4xl mx-auto">
    <div v-if="loading" class="space-y-6">
      <div class="aspect-video bg-gray-200 rounded-2xl animate-pulse" />
      <div class="h-8 w-2/3 bg-gray-200 rounded animate-pulse" />
      <div class="h-4 w-full bg-gray-100 rounded animate-pulse" />
    </div>
    <div v-else-if="error" class="rounded-2xl bg-red-50 border border-red-200 p-6 text-red-700">
      {{ t('common.error') }}: {{ error }}
    </div>
    <template v-else-if="product">
      <div class="grid gap-8 md:grid-cols-2">
        <div class="aspect-square rounded-2xl overflow-hidden bg-gray-200">
          <img
            v-if="product.image_url"
            :src="imageUrl(product.image_url)"
            :alt="productName(product)"
            class="w-full h-full object-cover"
          />
          <div v-else class="w-full h-full flex items-center justify-center text-8xl text-gray-400">📦</div>
        </div>
        <div>
          <h1 class="text-2xl md:text-3xl font-bold text-gray-900">{{ productName(product) }}</h1>
          <p class="mt-4 text-2xl font-bold text-indigo-600">{{ formatPrice(product.price) }}</p>
          <p v-if="productDesc(product)" class="mt-4 text-gray-600">{{ productDesc(product) }}</p>
          <div class="mt-6 flex flex-wrap items-center gap-4">
            <div class="flex items-center rounded-xl border border-gray-200 bg-gray-50 overflow-hidden">
              <button
                type="button"
                class="w-12 h-12 flex items-center justify-center text-xl font-medium text-gray-600 hover:bg-gray-100"
                @click="quantity = Math.max(1, quantity - 1)"
              >
                −
              </button>
              <span class="w-12 text-center font-semibold">{{ quantity }}</span>
              <button
                type="button"
                class="w-12 h-12 flex items-center justify-center text-xl font-medium text-gray-600 hover:bg-gray-100"
                @click="quantity += 1"
              >
                +
              </button>
            </div>
            <button type="button" class="btn-primary flex-1 min-w-[200px]" @click="addToCart">
              {{ t('product.addToCart') }}
            </button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useCartStore } from '@/stores/cart'
import { useToastStore } from '@/stores/toast'

import { fetchProduct } from '@/api/products'
import { fetchStore } from '@/api/stores'
import { formatPrice as formatPriceUtil } from '@/utils/currency'

const route = useRoute()
const { t, locale } = useI18n()
const cart = useCartStore()
const toast = useToastStore()
const product = ref(null)
const store = ref(null)
const loading = ref(true)
const error = ref(null)
const quantity = ref(1)

const baseURL = import.meta.env.VITE_API_BASE_URL || ''

function imageUrl(path) {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return `${baseURL}${path.startsWith('/') ? '' : '/'}${path}`
}

function productName(p) {
  return locale.value === 'ar' && p.name_ar ? p.name_ar : (p.name || 'Product')
}

function productDesc(p) {
  return locale.value === 'ar' && p.description_ar ? p.description_ar : (p.description || '')
}

function formatPrice(price) {
  const code = store.value?.currency_code || 'USD'
  const pos = store.value?.currency_symbol_position || 'before'
  return formatPriceUtil(price, code, pos)
}

function addToCart() {
  for (let i = 0; i < quantity.value; i++) {
    cart.addItem(product.value, 1, product.value.website_id)
  }
  toast.show(t('cart.addedToCart'), { duration: 3500, cartLink: true })
}

onMounted(async () => {
  try {
    product.value = await fetchProduct(route.params.id)
    if (product.value?.website_id) {
      store.value = await fetchStore(product.value.website_id)
      cart.setStoreCurrency(store.value)
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})
</script>
