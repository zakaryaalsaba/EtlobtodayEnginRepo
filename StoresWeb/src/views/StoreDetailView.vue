<template>
  <div class="animate-fade-in">
    <div v-if="loading" class="space-y-6">
      <div class="h-48 bg-gray-200 rounded-2xl animate-pulse" />
      <div class="h-8 w-2/3 bg-gray-200 rounded animate-pulse" />
      <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <div v-for="i in 6" :key="i" class="h-56 bg-gray-200 rounded-2xl animate-pulse" />
      </div>
    </div>
    <div v-else-if="error" class="rounded-2xl bg-red-50 border border-red-200 p-6 text-red-700">
      {{ t('common.error') }}: {{ error }}
    </div>
    <template v-else-if="store">
      <div class="flex flex-col sm:flex-row gap-4 mb-8">
        <div class="w-full sm:w-64 h-48 sm:h-40 rounded-2xl overflow-hidden bg-gray-200 shrink-0">
          <img
            v-if="store.logo_url"
            :src="imageUrl(store.logo_url)"
            :alt="storeName(store)"
            class="w-full h-full object-cover"
          />
          <div v-else class="w-full h-full flex items-center justify-center text-6xl text-gray-400">🏪</div>
        </div>
        <div class="flex-1">
          <h1 class="text-2xl sm:text-3xl font-bold text-gray-900">{{ storeName(store) }}</h1>
          <p v-if="store.description" class="mt-2 text-gray-600">{{ storeDesc(store) }}</p>
        </div>
      </div>
      <h2 class="text-xl font-semibold text-gray-900 mb-4">{{ t('store.products') }}</h2>
      <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <ProductCard
          v-for="product in products"
          :key="product.id"
          :product="product"
          :store-id="store.id"
          :currency-code="store?.currency_code"
          :symbol-position="store?.currency_symbol_position"
        />
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import ProductCard from '@/components/ProductCard.vue'
import { useCartStore } from '@/stores/cart'
import { fetchStore } from '@/api/stores'
import { fetchProductsByStore } from '@/api/products'

const route = useRoute()
const cart = useCartStore()
const { t, locale } = useI18n()
const store = ref(null)
const products = ref([])
const loading = ref(true)
const error = ref(null)

const baseURL = import.meta.env.VITE_API_BASE_URL || ''

function imageUrl(path) {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return `${baseURL}${path.startsWith('/') ? '' : '/'}${path}`
}

function storeName(s) {
  return locale.value === 'ar' && s.restaurant_name_ar ? s.restaurant_name_ar : (s.restaurant_name || 'Store')
}

function storeDesc(s) {
  return locale.value === 'ar' && s.description_ar ? s.description_ar : (s.description || '')
}

onMounted(async () => {
  const id = route.params.id
  try {
    store.value = await fetchStore(id)
    products.value = await fetchProductsByStore(id)
    cart.setStoreCurrency(store.value)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})
</script>
