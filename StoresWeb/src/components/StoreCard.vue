<template>
  <router-link
    :to="{ name: 'StoreDetail', params: { id: store.id } }"
    class="card block p-0 hover:scale-[1.02] active:scale-[0.99] transition-transform duration-200"
  >
    <div class="aspect-[4/3] bg-gray-200 overflow-hidden">
      <img
        v-if="store.logo_url"
        :src="imageUrl(store.logo_url)"
        :alt="storeName(store)"
        class="w-full h-full object-cover"
      />
      <div v-else class="w-full h-full flex items-center justify-center text-5xl text-gray-400">🏪</div>
    </div>
    <div class="p-5">
      <h2 class="text-xl font-bold text-gray-900">{{ storeName(store) }}</h2>
      <p v-if="store.description" class="mt-1 text-gray-600 line-clamp-2">{{ storeDesc(store) }}</p>
      <p class="mt-3 text-indigo-600 font-medium">{{ t('store.viewStore') }} →</p>
    </div>
  </router-link>
</template>

<script setup>
import { useI18n } from 'vue-i18n'

const props = defineProps({
  store: { type: Object, required: true }
})

const { t, locale } = useI18n()

const baseURL = import.meta.env.VITE_API_BASE_URL || ''

function imageUrl(path) {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return `${baseURL}${path.startsWith('/') ? '' : '/'}${path}`
}

function storeName(store) {
  return locale.value === 'ar' && store.restaurant_name_ar ? store.restaurant_name_ar : (store.restaurant_name || 'Store')
}

function storeDesc(store) {
  return locale.value === 'ar' && store.description_ar ? store.description_ar : (store.description || '')
}
</script>
