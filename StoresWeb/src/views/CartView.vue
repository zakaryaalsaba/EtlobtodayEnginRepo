<template>
  <div class="animate-fade-in">
    <h1 class="text-2xl font-bold text-gray-900 mb-6">{{ t('cart.title') }}</h1>
    <div v-if="cart.isEmpty" class="card p-12 text-center">
      <p class="text-gray-600 text-lg">{{ t('cart.empty') }}</p>
      <router-link to="/" class="btn-primary inline-block mt-6">{{ t('cart.continueShopping') }}</router-link>
    </div>
    <div v-else class="grid gap-8 lg:grid-cols-3">
      <div class="lg:col-span-2 space-y-4">
        <CartItem
          v-for="item in cart.items"
          :key="item.product_id"
          :item="item"
          :currency-code="currencyCode"
          :symbol-position="symbolPosition"
          @update-quantity="(q) => cart.updateQuantity(item.product_id, q)"
          @remove="cart.removeItem(item.product_id)"
        />
      </div>
      <div>
        <div class="card p-6 sticky top-24">
          <p class="text-gray-600">{{ t('cart.subtotal') }}</p>
          <p class="text-2xl font-bold text-gray-900 mt-1">{{ formatPrice(cart.subtotal) }}</p>
          <router-link to="/checkout" class="btn-primary w-full mt-6 block text-center">
            {{ t('cart.checkout') }}
          </router-link>
          <router-link to="/" class="btn-secondary w-full mt-3 block text-center">
            {{ t('cart.continueShopping') }}
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import CartItem from '@/components/CartItem.vue'
import { useCartStore } from '@/stores/cart'
import { fetchStore } from '@/api/stores'
import { formatPrice as formatPriceUtil } from '@/utils/currency'

const { t } = useI18n()
const cart = useCartStore()

const currencyCode = computed(() => cart.storeCurrency.currency_code || 'USD')
const symbolPosition = computed(() => cart.storeCurrency.currency_symbol_position || 'before')

function formatPrice(value) {
  return formatPriceUtil(value, currencyCode.value, symbolPosition.value)
}

onMounted(async () => {
  if (cart.storeId) {
    try {
      const store = await fetchStore(cart.storeId)
      cart.setStoreCurrency(store)
    } catch (_) {}
  }
})
</script>
