<template>
  <div class="animate-fade-in max-w-2xl mx-auto">
    <h1 class="text-2xl font-bold text-gray-900 mb-6">{{ t('checkout.placeOrder') }}</h1>
    <div v-if="cart.isEmpty" class="card p-8 text-center">
      <p class="text-gray-600">{{ t('cart.empty') }}</p>
      <router-link to="/" class="btn-primary inline-block mt-4">{{ t('cart.continueShopping') }}</router-link>
    </div>
    <div v-else class="space-y-6">
      <div class="card p-6">
        <h2 class="font-semibold text-gray-900 mb-3">{{ t('checkout.customerDetails') }}</h2>
        <p class="text-gray-700">{{ checkout.form.name }}</p>
        <p class="text-gray-600 text-sm">{{ checkout.form.phone }}</p>
        <p v-if="checkout.form.email" class="text-gray-600 text-sm">{{ checkout.form.email }}</p>
        <p v-if="checkout.form.address" class="text-gray-600 text-sm mt-1">{{ checkout.form.address }}</p>
        <p v-if="checkout.form.order_type === 'delivery' && checkout.deliveryLat != null" class="text-gray-500 text-xs mt-1">
          📍 {{ checkout.deliveryLat.toFixed(5) }}, {{ checkout.deliveryLng.toFixed(5) }}
        </p>
      </div>
      <div v-if="checkout.form.order_type === 'delivery' && checkout.form.delivery_instructions" class="card p-6">
        <h2 class="font-semibold text-gray-900 mb-2">{{ t('checkout.deliveryInstructions') }}</h2>
        <p class="text-gray-600">{{ checkout.form.delivery_instructions }}</p>
      </div>
      <div class="card p-6">
        <h2 class="font-semibold text-gray-900 mb-3">{{ t('checkout.payWith') }}</h2>
        <p class="text-gray-700">Cash</p>
      </div>
      <div class="card p-6">
        <h2 class="font-semibold text-gray-900 mb-3">{{ t('checkout.paymentSummary') }}</h2>
        <div class="space-y-2">
          <div class="flex justify-between text-gray-700">
            <span>{{ t('cart.subtotal') }}</span>
            <span>{{ formatPrice(cart.subtotal) }}</span>
          </div>
          <div v-if="checkout.form.order_type === 'delivery' && deliveryFee > 0" class="flex justify-between text-gray-700">
            <span>Delivery</span>
            <span>{{ formatPrice(deliveryFee) }}</span>
          </div>
          <div class="flex justify-between text-lg font-bold text-gray-900 pt-2 border-t">
            <span>{{ t('common.total') }}</span>
            <span>{{ formatPrice(totalAmount) }}</span>
          </div>
        </div>
      </div>
      <div v-if="submitError" class="rounded-xl bg-red-50 border border-red-200 p-4 text-red-700 text-sm">
        {{ submitError }}
      </div>
      <div class="flex gap-3">
        <router-link to="/checkout" class="btn-secondary flex-1 text-center py-3">{{ t('common.back') }}</router-link>
        <button
          type="button"
          class="btn-primary flex-1 py-3"
          :disabled="submitting"
          @click="placeOrder"
        >
          {{ submitting ? t('common.loading') : t('checkout.placeOrder') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useCartStore } from '@/stores/cart'
import { useAuthStore } from '@/stores/auth'
import { useCheckoutStore } from '@/stores/checkout'
import { createOrder } from '@/api/orders'
import { fetchStore } from '@/api/stores'
import { formatPrice as formatPriceUtil } from '@/utils/currency'

const router = useRouter()
const { t } = useI18n()
const cart = useCartStore()
const auth = useAuthStore()
const checkout = useCheckoutStore()

const store = ref(null)
const submitting = ref(false)
const submitError = ref(null)

const currencyCode = computed(() => store.value?.currency_code || cart.storeCurrency?.currency_code || 'USD')
const symbolPosition = computed(() => store.value?.currency_symbol_position || cart.storeCurrency?.currency_symbol_position || 'before')
const deliveryFee = computed(() => {
  if (checkout.form.order_type !== 'delivery' || !store.value?.delivery_fee) return 0
  return Number(store.value.delivery_fee) || 0
})
const totalAmount = computed(() => cart.subtotal + (deliveryFee.value || 0))

function formatPrice(value) {
  return formatPriceUtil(value, currencyCode.value, symbolPosition.value)
}

onMounted(async () => {
  if (cart.storeId) {
    try {
      store.value = await fetchStore(cart.storeId)
      cart.setStoreCurrency(store.value)
    } catch (_) {}
  }
})

async function placeOrder() {
  if (!cart.storeId) {
    submitError.value = t('common.error')
    return
  }
  submitting.value = true
  submitError.value = null
  try {
    const payload = {
      website_id: cart.storeId,
      customer_id: auth.isLoggedIn ? auth.customer.id : null,
      customer_name: checkout.form.name,
      customer_email: checkout.form.email || null,
      customer_phone: checkout.form.phone,
      customer_address: checkout.form.address || null,
      order_type: checkout.form.order_type,
      payment_method: 'cash',
      items: cart.getItemsForOrder(),
      notes: checkout.form.notes || null,
      delivery_instructions: checkout.form.order_type === 'delivery' ? (checkout.form.delivery_instructions || null) : null,
      total_amount: totalAmount.value
    }
    if (checkout.form.order_type === 'delivery' && checkout.deliveryLat != null && checkout.deliveryLng != null) {
      payload.delivery_latitude = checkout.deliveryLat
      payload.delivery_longitude = checkout.deliveryLng
    }
    const order = await createOrder(payload)
    cart.clearCart()
    checkout.reset()
    await router.replace({ name: 'OrderTrack', params: { orderNumber: order.order_number } })
  } catch (e) {
    submitError.value = e.body?.error || e.message || t('common.error')
  } finally {
    submitting.value = false
  }
}
</script>
