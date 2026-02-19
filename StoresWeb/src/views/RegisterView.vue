<template>
  <div class="min-h-screen bg-gray-50 flex flex-col">
    <header class="border-b border-gray-200 bg-white px-4 py-3 flex items-center justify-between">
      <router-link to="/" class="text-indigo-600 font-semibold">{{ t('common.back') }}</router-link>
      <LanguageSwitcher />
    </header>
    <main class="flex-1 flex items-center justify-center p-4">
      <div class="w-full max-w-md card p-8 animate-slide-up">
        <h1 class="text-2xl font-bold text-gray-900">{{ t('auth.registerTitle') }}</h1>
        <form class="mt-6 space-y-4" @submit.prevent="submit">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ t('auth.name') }}</label>
            <input v-model="form.name" type="text" required class="input-field" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ t('auth.email') }}</label>
            <input v-model="form.email" type="email" required class="input-field" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ t('auth.password') }}</label>
            <input v-model="form.password" type="password" required minlength="6" class="input-field" />
          </div>
          <div v-if="error" class="text-red-600 text-sm">{{ error }}</div>
          <button type="submit" class="btn-primary w-full py-3" :disabled="loading">
            {{ loading ? t('common.loading') : t('auth.submitRegister') }}
          </button>
        </form>
        <p class="mt-6 text-center text-gray-600">
          {{ t('auth.hasAccount') }}
          <router-link to="/login" class="text-indigo-600 font-medium">{{ t('nav.login') }}</router-link>
        </p>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const { t } = useI18n()
const auth = useAuthStore()

const form = reactive({
  name: '',
  email: '',
  password: ''
})
const loading = ref(false)
const error = ref(null)

async function submit() {
  loading.value = true
  error.value = null
  try {
    await auth.register(form)
    await router.replace('/')
  } catch (e) {
    error.value = e.body?.error || e.message || t('common.error')
  } finally {
    loading.value = false
  }
}
</script>
