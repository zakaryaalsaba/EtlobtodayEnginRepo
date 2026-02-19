import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useToastStore = defineStore('toast', () => {
  const message = ref(null)
  const showCartLink = ref(false)
  const visible = ref(false)
  let timeoutId = null

  function show(msg, options = {}) {
    const { duration = 3000, cartLink = false } = options
    if (timeoutId) clearTimeout(timeoutId)
    message.value = msg
    showCartLink.value = cartLink
    visible.value = true
    timeoutId = setTimeout(() => {
      visible.value = false
      message.value = null
      showCartLink.value = false
      timeoutId = null
    }, duration)
  }

  return { message, showCartLink, visible, show }
})
