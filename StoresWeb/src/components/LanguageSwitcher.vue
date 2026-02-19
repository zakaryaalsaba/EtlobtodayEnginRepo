<template>
  <div class="flex rounded-xl overflow-hidden border border-gray-200 bg-gray-100 p-0.5">
    <button
      type="button"
      :class="[
        'px-3 py-1.5 text-sm font-medium rounded-lg transition',
        locale === 'en' ? 'bg-white text-indigo-600 shadow' : 'text-gray-600 hover:text-gray-900'
      ]"
      @click="setEn"
    >
      EN
    </button>
    <button
      type="button"
      :class="[
        'px-3 py-1.5 text-sm font-medium rounded-lg transition',
        locale === 'ar' ? 'bg-white text-indigo-600 shadow' : 'text-gray-600 hover:text-gray-900'
      ]"
      @click="setAr"
    >
      ع
    </button>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'

const { locale } = useI18n({ useScope: 'global' })

function setEn() {
  locale.value = 'en'
}

function setAr() {
  locale.value = 'ar'
}
</script>
