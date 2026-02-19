import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const customer = ref(null)
  const token = ref(localStorage.getItem('token') || null)

  const isLoggedIn = computed(() => !!token.value)

  function setAuth(customerData, authToken) {
    customer.value = customerData
    token.value = authToken
    if (authToken) localStorage.setItem('token', authToken)
  }

  function logout() {
    customer.value = null
    token.value = null
    localStorage.removeItem('token')
  }

  async function fetchMe() {
    if (!token.value) return null
    try {
      const c = await authApi.me()
      customer.value = c
      return c
    } catch {
      logout()
      return null
    }
  }

  async function login(email, password) {
    const { customer: c, token: t } = await authApi.login(email, password)
    setAuth(c, t)
    return c
  }

  async function register(data) {
    const { customer: c, token: t } = await authApi.register(data)
    setAuth(c, t)
    return c
  }

  return {
    customer,
    token,
    isLoggedIn,
    setAuth,
    logout,
    fetchMe,
    login,
    register
  }
})
