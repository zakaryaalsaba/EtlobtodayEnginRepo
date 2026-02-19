<template>
  <router-view v-slot="{ Component }">
    <transition name="page" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<script setup>
import { watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { setDocumentDirection } from '@/i18n'

const { locale } = useI18n()

watch(
  locale,
  (newLocale) => {
    setDocumentDirection(newLocale)
    localStorage.setItem('locale', newLocale)
  },
  { immediate: true }
)
</script>

<style>
.page-enter-active,
.page-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.page-enter-from {
  opacity: 0;
  transform: translateY(4px);
}
.page-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
