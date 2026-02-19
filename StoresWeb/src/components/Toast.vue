<template>
  <Teleport to="body">
    <Transition name="toast">
      <div
        v-if="toast.visible"
        class="fixed bottom-6 left-1/2 -translate-x-1/2 z-[100] px-4 py-3 rounded-xl bg-gray-900 text-white text-sm font-medium shadow-lg flex items-center gap-2"
        role="alert"
      >
        <span>{{ toast.message }}</span>
        <router-link v-if="toast.showCartLink" to="/cart" class="underline font-semibold">
          {{ t('cart.checkout') }}
        </router-link>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { useToastStore } from '@/stores/toast'
import { useI18n } from 'vue-i18n'

const toast = useToastStore()
const { t } = useI18n()
</script>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translate(-50%, 10px);
}
</style>
