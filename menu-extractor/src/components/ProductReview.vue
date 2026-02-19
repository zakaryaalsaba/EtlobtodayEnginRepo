<template>
  <div class="product-review">
    <div class="header-section">
      <h2>{{ isAddMode ? `Add products to ${selectedRestaurantName}` : 'Review & Edit Extracted Products' }}</h2>
      <button @click="$emit('back')" class="btn btn-secondary">← Back</button>
    </div>

    <!-- Restaurant Info Form (only when creating new) -->
    <div v-if="!isAddMode" class="restaurant-form">
      <h3>Restaurant Information</h3>
      <div class="form-grid">
        <div class="form-group">
          <label>Restaurant Name (English) *</label>
          <input
            v-model="localRestaurantInfo.restaurant_name"
            type="text"
            required
          />
        </div>
        <div class="form-group">
          <label>Restaurant Name (Arabic)</label>
          <input
            v-model="localRestaurantInfo.restaurant_name_ar"
            type="text"
          />
        </div>
        <div class="form-group">
          <label>Phone *</label>
          <input
            v-model="localRestaurantInfo.phone"
            type="tel"
            required
          />
        </div>
        <div class="form-group">
          <label>Email</label>
          <input
            v-model="localRestaurantInfo.email"
            type="email"
          />
        </div>
        <div class="form-group full-width">
          <label>Address</label>
          <input
            v-model="localRestaurantInfo.address"
            type="text"
          />
        </div>
        <div class="form-group full-width">
          <label>Address (Arabic)</label>
          <input
            v-model="localRestaurantInfo.address_ar"
            type="text"
          />
        </div>
        <div class="form-group full-width">
          <label>Description</label>
          <textarea
            v-model="localRestaurantInfo.description"
            rows="3"
          ></textarea>
        </div>
      </div>
    </div>

    <!-- Products List -->
    <div class="products-section">
      <h3>Products ({{ products.length }})</h3>
      <div class="products-list">
        <div
          v-for="(product, index) in products"
          :key="index"
          class="product-item"
        >
          <div class="product-content">
            <div class="product-main">
              <input
                v-model="product.name"
                class="product-name"
              />
              <input
                v-model="product.name_ar"
                class="product-name-ar"
              />
            </div>
            <div class="product-details">
              <input
                v-model="product.category"
                class="product-category"
                :title="'Category (EN)'"
              />
              <input
                v-model="product.category_ar"
                class="product-category"
                :title="'Category (AR)'"
              />
              <input
                v-model.number="product.price"
                type="number"
                step="0.01"
                min="0"
                class="product-price"
              />
              <textarea
                v-model="product.description"
                class="product-description"
                rows="2"
              ></textarea>
            </div>
          </div>
          <button @click="removeProduct(index)" class="remove-product-btn">×</button>
        </div>
      </div>

      <button @click="addProduct" class="btn btn-secondary add-product-btn">
        + Add Product
      </button>
    </div>

    <!-- Save to Database -->
    <div class="actions">
      <button
        @click="handleAction"
        class="btn btn-save btn-large"
        :disabled="creating || !canSubmit"
      >
        {{ creating ? 'Saving...' : submitButtonLabel }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  products: Array,
  restaurantInfo: Object,
  creating: Boolean,
  mode: { type: String, default: 'create' },
  selectedRestaurant: { type: Object, default: null }
})

const emit = defineEmits(['update-products', 'update-restaurant-info', 'back', 'create-restaurant', 'add-products'])

const isAddMode = computed(() => props.mode === 'add')
const selectedRestaurantName = computed(() => props.selectedRestaurant?.name ?? 'this restaurant')
const submitButtonLabel = computed(() =>
  isAddMode.value ? `Add products to ${selectedRestaurantName.value}` : 'Save to Database'
)

const localProducts = ref([])
const localRestaurantInfo = ref({
  restaurant_name: '',
  restaurant_name_ar: '',
  phone: '',
  email: '',
  address: '',
  address_ar: '',
  description: '',
  description_ar: ''
})

// Sync from props when they change (e.g. after extraction)
watch(() => [props.products, props.restaurantInfo], () => {
  if (props.products && props.products.length > 0) {
    localProducts.value = props.products.map(p => ({
      name: p.name ?? '',
      name_ar: p.name_ar ?? p.name ?? '',
      description: p.description ?? '',
      description_ar: p.description_ar ?? p.description ?? '',
      price: p.price ?? '',
      category: p.category ?? '',
      category_ar: p.category_ar ?? p.category ?? '',
      is_available: p.is_available !== false
    }))
  }
  if (props.restaurantInfo) {
    localRestaurantInfo.value = {
      restaurant_name: props.restaurantInfo.restaurant_name ?? '',
      restaurant_name_ar: props.restaurantInfo.restaurant_name_ar ?? '',
      phone: props.restaurantInfo.phone ?? '',
      email: props.restaurantInfo.email ?? '',
      address: props.restaurantInfo.address ?? '',
      address_ar: props.restaurantInfo.address_ar ?? '',
      description: props.restaurantInfo.description ?? '',
      description_ar: props.restaurantInfo.description_ar ?? ''
    }
  }
}, { immediate: true })

watch(localProducts, (newProducts) => {
  emit('update-products', newProducts)
}, { deep: true })

watch(localRestaurantInfo, (newInfo) => {
  emit('update-restaurant-info', newInfo)
}, { deep: true })

const canCreate = computed(() => {
  return localRestaurantInfo.value.restaurant_name &&
         localRestaurantInfo.value.phone &&
         localProducts.value.length > 0 &&
         localProducts.value.every(p => p.name && (p.price !== '' && p.price !== null && Number(p.price) >= 0))
})

const canAddProducts = computed(() => {
  return localProducts.value.length > 0 &&
         localProducts.value.every(p => p.name && (p.price !== '' && p.price !== null && Number(p.price) >= 0))
})

const canSubmit = computed(() => isAddMode.value ? canAddProducts.value : canCreate.value)

function addProduct() {
  localProducts.value.push({
    name: '',
    name_ar: '',
    description: '',
    description_ar: '',
    price: '',
    category: '',
    category_ar: '',
    is_available: true
  })
}

function removeProduct(index) {
  localProducts.value.splice(index, 1)
}

function handleAction() {
  if (isAddMode.value) {
    if (canAddProducts.value) emit('add-products')
  } else {
    if (canCreate.value) emit('create-restaurant')
  }
}
</script>

<style scoped>
.product-review {
  width: 100%;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.header-section h2 {
  margin: 0;
  color: #1f2937;
}

.restaurant-form,
.products-section {
  margin-bottom: 2rem;
  padding: 1.5rem;
  background: #f9fafb;
  border-radius: 8px;
}

.restaurant-form h3,
.products-section h3 {
  margin-top: 0;
  margin-bottom: 1.5rem;
  color: #374151;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group.full-width {
  grid-column: 1 / -1;
}

.form-group label {
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #374151;
}

.form-group input,
.form-group textarea {
  padding: 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 1rem;
  font-family: inherit;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.products-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 1rem;
}

.product-item {
  display: flex;
  gap: 1rem;
  padding: 1rem;
  background: white;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.product-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.product-main {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
}

.product-name,
.product-name-ar {
  padding: 0.5rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 1rem;
}

.product-details {
  display: grid;
  grid-template-columns: 1fr 1fr 100px;
  gap: 0.5rem;
  align-items: start;
}

.product-category,
.product-price {
  padding: 0.5rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.9rem;
}

.product-description {
  grid-column: 1 / -1;
  padding: 0.5rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.9rem;
  resize: vertical;
}

.remove-product-btn {
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 6px;
  width: 32px;
  height: 32px;
  font-size: 1.5rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  transition: all 0.2s;
  flex-shrink: 0;
}

.remove-product-btn:hover {
  background: #dc2626;
  transform: scale(1.1);
}

.add-product-btn {
  width: 100%;
  margin-top: 1rem;
}

.actions {
  display: flex;
  justify-content: center;
  margin-top: 2rem;
}

.btn {
  padding: 0.75rem 2rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary,
.btn-save {
  background: #667eea;
  color: white;
}

.btn-primary:hover:not(:disabled),
.btn-save:hover:not(:disabled) {
  background: #5568d3;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-save {
  background: #059669;
  font-weight: 600;
}

.btn-save:hover:not(:disabled) {
  background: #047857;
  box-shadow: 0 4px 12px rgba(5, 150, 105, 0.4);
}

.btn-secondary {
  background: #e5e7eb;
  color: #374151;
}

.btn-secondary:hover {
  background: #d1d5db;
}

.btn-large {
  padding: 1rem 3rem;
  font-size: 1.1rem;
}
</style>
