<template>
  <div class="min-h-screen bg-gray-50 flex flex-col">
    <header class="sticky top-0 z-40 bg-white/95 backdrop-blur border-b border-gray-200 shadow-sm">
      <div class="max-w-6xl mx-auto px-4 sm:px-6 flex items-center justify-between h-16">
        <router-link to="/" class="flex items-center gap-2 text-xl font-bold text-indigo-600 hover:text-indigo-700 transition">
          <span class="text-2xl">🛒</span>
          <span>{{ t('nav.stores') }}</span>
        </router-link>
        <nav class="flex items-center gap-2 sm:gap-4">
          <LanguageSwitcher />
          <router-link to="/cart" class="relative p-2 rounded-xl text-gray-600 hover:bg-gray-100 hover:text-indigo-600 transition">
            <span class="text-2xl">🛒</span>
            <span v-if="cart.count" class="absolute -top-0.5 -end-0.5 min-w-[1.25rem] h-5 px-1 flex items-center justify-center rounded-full bg-indigo-600 text-white text-xs font-bold">
              {{ cart.count }}
            </span>
          </router-link>
          <template v-if="auth.isLoggedIn">
            <router-link to="/orders" class="btn-secondary text-sm py-2">{{ t('nav.orders') }}</router-link>
            <button type="button" class="btn-secondary text-sm py-2" @click="auth.logout()">{{ t('nav.logout') }}</button>
          </template>
          <template v-else>
            <router-link to="/login" class="btn-secondary text-sm py-2">{{ t('nav.login') }}</router-link>
            <router-link to="/register" class="btn-primary text-sm py-2">{{ t('nav.register') }}</router-link>
          </template>
        </nav>
      </div>
    </header>
    <main class="flex-1 max-w-6xl w-full mx-auto px-4 sm:px-6 py-6 sm:py-8">
      <router-view />
    </main>
    <Toast />
    <footer class="border-t border-gray-200 bg-white py-6 mt-auto">
      <div class="max-w-6xl mx-auto px-4 sm:px-6 text-center text-gray-500 text-sm">
        © {{ new Date().getFullYear() }} Stores. {{ t('home.subtitle') }}
      </div>
    </footer>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import Toast from '@/components/Toast.vue'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'

const { t } = useI18n()
const auth = useAuthStore()
const cart = useCartStore()
</script>
