import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useCartStore = defineStore('cart', () => {
  const items = ref([])
  const storeId = ref(null)
  const storeCurrency = ref({ currency_code: 'USD', currency_symbol_position: 'before' })

  const count = computed(() => items.value.reduce((n, i) => n + i.quantity, 0))
  const subtotal = computed(() =>
    items.value.reduce((sum, i) => sum + Number(i.product_price) * i.quantity, 0)
  )
  const isEmpty = computed(() => items.value.length === 0)

  function addItem(product, quantity = 1, websiteId = null) {
    if (websiteId != null && storeId.value != null && storeId.value !== websiteId) {
      clearCart()
    }
    if (websiteId != null) storeId.value = websiteId
    const id = product.id
    const existing = items.value.find((i) => i.product_id === id)
    if (existing) {
      existing.quantity += quantity
    } else {
      items.value.push({
        product_id: id,
        product_name: product.name,
        product_price: product.price,
        quantity,
        product
      })
    }
  }

  function setStoreId(id) {
    storeId.value = id
  }

  function setStoreCurrency(currency) {
    if (currency) {
      storeCurrency.value = {
        currency_code: currency.currency_code || 'USD',
        currency_symbol_position: currency.currency_symbol_position || 'before'
      }
    }
  }

  function updateQuantity(productId, quantity) {
    const item = items.value.find((i) => i.product_id === productId)
    if (!item) return
    if (quantity <= 0) {
      items.value = items.value.filter((i) => i.product_id !== productId)
      return
    }
    item.quantity = quantity
  }

  function removeItem(productId) {
    items.value = items.value.filter((i) => i.product_id !== productId)
  }

  function clearCart() {
    items.value = []
    storeId.value = null
  }

  function getItemsForOrder() {
    return items.value.map((i) => ({
      product_id: i.product_id,
      quantity: i.quantity
    }))
  }

  return {
    items,
    storeId,
    storeCurrency,
    count,
    subtotal,
    isEmpty,
    addItem,
    setStoreId,
    setStoreCurrency,
    updateQuantity,
    removeItem,
    clearCart,
    getItemsForOrder
  }
})
