<template>
  <div class="animate-fade-in max-w-2xl mx-auto">
    <h1 class="text-2xl font-bold text-gray-900 mb-6">{{ t('checkout.title') }}</h1>
    <div v-if="cart.isEmpty" class="card p-8 text-center">
      <p class="text-gray-600">{{ t('cart.empty') }}</p>
      <router-link to="/" class="btn-primary inline-block mt-4">{{ t('cart.continueShopping') }}</router-link>
    </div>
    <form v-else class="space-y-6" @submit.prevent="proceedToReview">
      <div class="card p-6 space-y-4">
        <h2 class="font-semibold text-gray-900">{{ t('checkout.customerDetails') }}</h2>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">{{ t('checkout.name') }} *</label>
          <input v-model="checkout.form.name" type="text" required class="input-field" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">{{ t('checkout.email') }}</label>
          <input v-model="checkout.form.email" type="email" class="input-field" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">{{ t('checkout.phone') }} *</label>
          <input v-model="checkout.form.phone" type="tel" required class="input-field" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">{{ t('checkout.address') }}</label>
          <input v-model="checkout.form.address" type="text" class="input-field" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">{{ t('checkout.orderType') }}</label>
          <select v-model="checkout.form.order_type" class="input-field" @change="onOrderTypeChange">
            <option value="pickup">{{ t('checkout.pickup') }}</option>
            <option value="delivery">{{ t('checkout.delivery') }}</option>
          </select>
        </div>
        <template v-if="checkout.form.order_type === 'delivery'">
          <div>
            <button
              type="button"
              class="btn-secondary w-full flex items-center justify-center gap-2"
              :disabled="gettingLocation"
              @click="requestLocation"
            >
              <span v-if="gettingLocation">{{ t('common.loading') }}</span>
              <span v-else>📍 {{ t('checkout.useMyLocation') }}</span>
            </button>
            <p v-if="checkout.deliveryLat != null" class="mt-1 text-sm text-green-600">
              ✓ {{ checkout.deliveryLat.toFixed(5) }}, {{ checkout.deliveryLng.toFixed(5) }}
            </p>
            <p v-if="checkout.locationError" class="mt-1 text-sm text-red-600">{{ checkout.locationError }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ t('checkout.deliveryInstructions') }}</label>
            <textarea v-model="checkout.form.delivery_instructions" rows="2" class="input-field" />
          </div>
        </template>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">{{ t('checkout.orderNotes') }}</label>
          <textarea v-model="checkout.form.notes" rows="2" class="input-field" />
        </div>
      </div>
      <div class="card p-6">
        <p class="text-gray-600">{{ t('cart.subtotal') }}</p>
        <p class="text-2xl font-bold text-gray-900 mt-1">{{ formatPrice(cart.subtotal) }}</p>
      </div>
      <div v-if="submitError" class="rounded-xl bg-red-50 border border-red-200 p-4 text-red-700 text-sm">
        {{ submitError }}
      </div>
      <button type="submit" class="btn-primary w-full py-3 text-lg" :disabled="submitting">
        {{ submitting ? t('common.loading') : t('checkout.proceedToReview') }}
      </button>
    </form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useCartStore } from '@/stores/cart'
import { useAuthStore } from '@/stores/auth'
import { useCheckoutStore } from '@/stores/checkout'
import { fetchStore } from '@/api/stores'
import { formatPrice as formatPriceUtil } from '@/utils/currency'

const router = useRouter()
const { t } = useI18n()
const cart = useCartStore()
const auth = useAuthStore()
const checkout = useCheckoutStore()

const submitting = ref(false)
const submitError = ref(null)
const gettingLocation = ref(false)

const currencyCode = computed(() => cart.storeCurrency?.currency_code || 'USD')
const symbolPosition = computed(() => cart.storeCurrency?.currency_symbol_position || 'before')

function formatPrice(value) {
  return formatPriceUtil(value, currencyCode.value, symbolPosition.value)
}

function onOrderTypeChange() {
  if (checkout.form.order_type !== 'delivery') {
    checkout.setLocationError(null)
  }
}

function requestLocation() {
  if (!navigator.geolocation) {
    checkout.setLocationError('Geolocation is not supported')
    return
  }
  gettingLocation.value = true
  checkout.setLocationError(null)
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      checkout.setDeliveryLocation(pos.coords.latitude, pos.coords.longitude)
      gettingLocation.value = false
    },
    (err) => {
      checkout.setLocationError(err.message || t('common.error'))
      gettingLocation.value = false
    },
    { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
  )
}

onMounted(async () => {
  if (auth.customer) {
    checkout.setForm({
      name: auth.customer.name || '',
      email: auth.customer.email || '',
      phone: auth.customer.phone || '',
      address: auth.customer.address || ''
    })
  }
  if (cart.storeId) {
    try {
      const store = await fetchStore(cart.storeId)
      cart.setStoreCurrency(store)
    } catch (_) {}
  }
})

async function proceedToReview() {
  if (checkout.form.order_type === 'delivery' && (checkout.deliveryLat == null || checkout.deliveryLng == null)) {
    submitError.value = t('checkout.locationRequired')
    return
  }
  submitError.value = null
  submitting.value = true
  try {
    await router.push({ name: 'PlaceOrder' })
  } finally {
    submitting.value = false
  }
}
</script>
