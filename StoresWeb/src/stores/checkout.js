import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'

export const useCheckoutStore = defineStore('checkout', () => {
  const step = ref(1)
  const form = reactive({
    name: '',
    email: '',
    phone: '',
    address: '',
    order_type: 'pickup',
    notes: '',
    delivery_instructions: ''
  })
  const deliveryLat = ref(null)
  const deliveryLng = ref(null)
  const locationError = ref(null)

  function setForm(data) {
    Object.assign(form, data)
  }

  function setDeliveryLocation(lat, lng) {
    deliveryLat.value = lat
    deliveryLng.value = lng
    locationError.value = null
  }

  function setLocationError(err) {
    locationError.value = err
    deliveryLat.value = null
    deliveryLng.value = null
  }

  function reset() {
    step.value = 1
    form.name = ''
    form.email = ''
    form.phone = ''
    form.address = ''
    form.order_type = 'pickup'
    form.notes = ''
    form.delivery_instructions = ''
    deliveryLat.value = null
    deliveryLng.value = null
    locationError.value = null
  }

  return {
    step,
    form,
    deliveryLat,
    deliveryLng,
    locationError,
    setForm,
    setDeliveryLocation,
    setLocationError,
    reset
  }
})
