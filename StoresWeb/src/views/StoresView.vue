<template>
  <div class="animate-fade-in">
    <div class="mb-8">
      <h1 class="text-3xl font-bold text-gray-900">{{ t('home.title') }}</h1>
      <p class="mt-1 text-gray-600">{{ t('home.subtitle') }}</p>
    </div>
    <div v-if="loading" class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      <div v-for="i in 6" :key="i" class="card h-48 animate-pulse bg-gray-200 rounded-2xl" />
    </div>
    <div v-else-if="error" class="rounded-2xl bg-red-50 border border-red-200 p-6 text-red-700">
      {{ t('common.error') }}: {{ error }}
    </div>
    <div v-else class="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
      <StoreCard
        v-for="store in stores"
        :key="store.id"
        :store="store"
        class="animate-slide-up"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import StoreCard from '@/components/StoreCard.vue'
import { fetchStores } from '@/api/stores'

const { t } = useI18n()
const stores = ref([])
const loading = ref(true)
const error = ref(null)

onMounted(async () => {
  try {
    const list = await fetchStores()
    stores.value = list.filter((s) => s.is_published !== 0 && s.is_published !== false)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})
</script>
