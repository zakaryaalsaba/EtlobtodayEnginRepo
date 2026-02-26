<template>
  <div class="app">
    <header class="app-header">
      <h1>🍽️ Menu Extractor</h1>
      <p>Upload menu images to create a restaurant or add products to an existing one</p>
    </header>

    <main class="app-main">
      <!-- Step 0: Choose mode -->
      <section v-if="step === 'mode'" class="step-section mode-section">
        <h2>What would you like to do?</h2>
        <div class="mode-cards">
          <button class="mode-card" @click="setMode('create')">
            <span class="mode-icon">🆕</span>
            <span class="mode-title">Create new restaurant</span>
            <span class="mode-desc">Upload menu images to create a new restaurant and its products.</span>
          </button>
          <button class="mode-card" @click="setMode('add')">
            <span class="mode-icon">➕</span>
            <span class="mode-title">Add to existing restaurant</span>
            <span class="mode-desc">Select a restaurant, then upload images to add more products.</span>
          </button>
        </div>
        <div v-if="mode === 'add'" class="restaurant-picker">
          <p class="picker-label">Select restaurant</p>
          <select v-model="selectedWebsiteId" class="restaurant-select">
            <option :value="null">— Choose restaurant —</option>
            <option v-for="w in restaurants" :key="w.id" :value="w.id">
              {{ w.restaurant_name || `Restaurant #${w.id}` }}
            </option>
          </select>
          <p v-if="restaurantsLoadError" class="error-msg">{{ restaurantsLoadError }}</p>
          <button
            class="btn btn-primary continue-btn"
            :disabled="!selectedWebsiteId || loadingRestaurants"
            @click="continueToUpload"
          >
            {{ loadingRestaurants ? 'Loading...' : 'Continue to upload' }}
          </button>
        </div>
        <div v-else-if="mode === 'create'" class="continue-wrap">
          <button class="btn btn-primary" @click="step = 'upload'">Continue to upload</button>
        </div>
      </section>

      <!-- Step 1: Upload Images or Paste Text -->
      <section v-if="step === 'upload'" class="step-section">
        <p v-if="mode === 'add' && selectedRestaurantName" class="context-msg">
          Adding products to: <strong>{{ selectedRestaurantName }}</strong>
        </p>
        <div class="upload-grid">
          <div class="upload-card">
            <h3 class="upload-title">Option 1: Upload menu images</h3>
            <p class="upload-subtitle">Drag & drop or select images of your menu.</p>
            <ImageUpload 
              @images-uploaded="handleImagesUploaded"
              :loading="processing"
            />
          </div>

          <div class="upload-card">
            <h3 class="upload-title">Option 2: Paste menu text</h3>
            <p class="upload-subtitle">
              Paste the full menu text here. The system will extract products and categories similar to image extraction.
            </p>
            <textarea
              v-model="menuText"
              class="menu-textarea"
              rows="10"
              placeholder="Paste your menu text here (for example, copy from a PDF or website)..."
            ></textarea>
            <button
              class="btn btn-primary extract-text-btn"
              :disabled="processing || !menuText.trim()"
              @click="handleTextExtraction"
            >
              {{ processing ? 'Processing...' : 'Extract products from text' }}
            </button>
          </div>
        </div>
        <button @click="step = 'mode'" class="btn btn-secondary back-mode-btn">← Change action</button>
      </section>

      <!-- Step 2: Review Extracted Products -->
      <section v-if="step === 'review'" class="step-section">
        <ProductReview
          :products="extractedProducts"
          :restaurant-info="restaurantInfo"
          :mode="mode"
          :selected-restaurant="mode === 'add' ? { id: selectedWebsiteId, name: selectedRestaurantName } : null"
          @update-products="updateProducts"
          @update-restaurant-info="updateRestaurantInfo"
          @back="step = 'upload'"
          @create-restaurant="createRestaurant"
          @add-products="addProductsToRestaurantHandler"
          :creating="creating"
        />
      </section>

      <!-- Step 3: Success -->
      <section v-if="step === 'success'" class="step-section success-section">
        <div class="success-content">
          <template v-if="mode === 'add'">
            <h2>✅ Products Added Successfully!</h2>
            <p>{{ addProductsCount }} product(s) added to <strong>{{ selectedRestaurantName }}</strong>.</p>
            <div class="success-actions">
              <button @click="resetToUpload" class="btn btn-primary">Add more products</button>
              <button @click="reset" class="btn btn-secondary">Start over</button>
            </div>
          </template>
          <template v-else>
            <h2>✅ Restaurant Created Successfully!</h2>
            <p>Your restaurant website has been created with {{ extractedProducts.length }} products.</p>
            <div class="success-info">
              <p><strong>Restaurant Name:</strong> {{ restaurantInfo.restaurant_name }}</p>
              <p><strong>Website ID:</strong> {{ createdRestaurantId }}</p>
              <p v-if="restaurantInfo.subdomain">
                <strong>Subdomain:</strong> {{ restaurantInfo.subdomain }}
              </p>
            </div>
            <button @click="reset" class="btn btn-primary">Create Another Restaurant</button>
          </template>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import ImageUpload from './components/ImageUpload.vue'
import ProductReview from './components/ProductReview.vue'
import { processMenuImages, createRestaurantWithProducts, getRestaurants, addProductsToRestaurant } from './services/api.js'

const step = ref('mode')
const mode = ref('create')
const processing = ref(false)
const creating = ref(false)
const extractedProducts = ref([])
const menuText = ref('')
const restaurantInfo = ref({
  restaurant_name: '',
  restaurant_name_ar: '',
  phone: '',
  email: '',
  address: '',
  address_ar: '',
  description: '',
  description_ar: ''
})
const createdRestaurantId = ref(null)
const restaurants = ref([])
const loadingRestaurants = ref(false)
const restaurantsLoadError = ref('')
const selectedWebsiteId = ref(null)
const selectedRestaurantName = ref('')
const addProductsCount = ref(0)

watch(selectedWebsiteId, (id) => {
  const w = restaurants.value.find(r => r.id === id)
  selectedRestaurantName.value = w ? (w.restaurant_name || `Restaurant #${w.id}`) : ''
})

async function setMode(m) {
  mode.value = m
  if (m === 'add') {
    loadingRestaurants.value = true
    restaurantsLoadError.value = ''
    const res = await getRestaurants({ all: true })
    loadingRestaurants.value = false
    if (res.success && res.websites) {
      restaurants.value = res.websites
      if (restaurants.value.length === 0) restaurantsLoadError.value = 'No restaurants found. Create one first.'
      else selectedWebsiteId.value = restaurants.value[0].id
    } else {
      restaurantsLoadError.value = res.error || 'Failed to load restaurants'
    }
  }
}

function continueToUpload() {
  if (mode.value === 'add' && selectedWebsiteId.value) step.value = 'upload'
}

async function handleImagesUploaded(images) {
  processing.value = true
  try {
    const formData = new FormData()
    images.forEach((image, index) => {
      formData.append('images', image)
    })

    const response = await processMenuImages(formData)
    
    console.log('Processing response:', response)
    
    if (response.success) {
      extractedProducts.value = response.products || []
      
      // Set restaurant name if extracted
      if (response.restaurant_name) {
        restaurantInfo.value.restaurant_name = response.restaurant_name
      }
      if (response.restaurant_name_ar) {
        restaurantInfo.value.restaurant_name_ar = response.restaurant_name_ar
      }
      
      // Show warning if no products extracted
      if (extractedProducts.value.length === 0) {
        const proceed = confirm(
          'No products were extracted from the images. ' +
          'This might be because:\n' +
          '1. OpenAI API is not configured\n' +
          '2. Images are not clear enough\n' +
          '3. Menu format is not recognized\n\n' +
          'Would you like to proceed anyway and add products manually?'
        )
        if (!proceed) {
          return
        }
      }
      
      step.value = 'review'
    } else {
      alert('Error processing images: ' + (response.error || 'Unknown error'))
    }
  } catch (error) {
    console.error('Error:', error)
    alert('Failed to process images: ' + error.message)
  } finally {
    processing.value = false
  }
}

function guessRestaurantNameFromText(text) {
  const lines = text
    .split('\n')
    .map(l => l.trim())
    .filter(l => l.length > 0)

  for (const line of lines) {
    const hasPrice = /[\d]+\s*(?:\.|,)?\d*\s*(?:\$|USD|SAR|AED|JOD|EGP|ريال|درهم|دينار|جنيه)/i.test(line)
    const hasPhone = /(\+?\d[\d\s-]{6,})/.test(line)
    if (hasPrice || hasPhone) continue

    if (line.toUpperCase() === line || /^[A-Z][A-Za-z\s&'-]+$/.test(line)) {
      return line
    }
  }

  return lines[0] || ''
}

function parseMenuText(text) {
  const lines = text
    .split('\n')
    .map(l => l.trim())
    .filter(l => l.length > 0)

  const products = []
  let currentCategory = 'General'

  const knownCategories = [
    'Main Dishes',
    'Sides',
    'Sides / Add-On',
    'Add-On',
    'Meals',
    'Sandwiches',
    'Drinks',
    'Appetizers',
    'Desserts',
    'Starters'
  ]

  for (const rawLine of lines) {
    const line = rawLine.replace(/^[🍽🥘🍴🥪🥤📌⭐\-•\s]+/, '').trim()
    if (!line) continue

    const looksLikeCategory =
      knownCategories.some(c => line.toLowerCase().includes(c.toLowerCase())) ||
      (!/[0-9]/.test(line) && line === line.toUpperCase() && line.length >= 3)

    if (looksLikeCategory) {
      currentCategory = line
      continue
    }

    const priceMatch =
      line.match(/^(.*?)[–-]\s*\$?\s*([\d]+(?:[.,]\d+)?)/) ||
      line.match(/^(.+?)\s+\$?\s*([\d]+(?:[.,]\d+)?)/)

    if (priceMatch) {
      const name = priceMatch[1].trim()
      const priceStr = priceMatch[2].replace(',', '.')
      const price = parseFloat(priceStr) || 0

      if (name) {
        products.push({
          name,
          name_ar: name,
          description: '',
          description_ar: '',
          price,
          category: currentCategory || 'General',
          category_ar: currentCategory || 'عام',
          is_available: true
        })
      }
    }
  }

  return products
}

async function handleTextExtraction() {
  const raw = menuText.value || ''
  if (!raw.trim()) return

  processing.value = true
  try {
    const products = parseMenuText(raw)

    if (!products.length) {
      alert('Could not detect any products in the text. Please check the format or try with images.')
      return
    }

    extractedProducts.value = products

    if (!restaurantInfo.value.restaurant_name) {
      restaurantInfo.value.restaurant_name = guessRestaurantNameFromText(raw)
    }

    step.value = 'review'
  } catch (error) {
    console.error('Error extracting from text:', error)
    alert('Failed to extract products from text: ' + error.message)
  } finally {
    processing.value = false
  }
}

function updateProducts(products) {
  extractedProducts.value = products
}

function updateRestaurantInfo(info) {
  restaurantInfo.value = { ...restaurantInfo.value, ...info }
}

async function createRestaurant() {
  creating.value = true
  try {
    const response = await createRestaurantWithProducts({
      restaurant: restaurantInfo.value,
      products: extractedProducts.value
    })

    if (response.success) {
      createdRestaurantId.value = response.website_id
      step.value = 'success'
    } else {
      alert('Error creating restaurant: ' + (response.error || 'Unknown error'))
    }
  } catch (error) {
    console.error('Error:', error)
    alert('Failed to create restaurant: ' + error.message)
  } finally {
    creating.value = false
  }
}

async function addProductsToRestaurantHandler() {
  if (mode.value !== 'add' || !selectedWebsiteId.value) return
  creating.value = true
  try {
    const response = await addProductsToRestaurant({
      website_id: selectedWebsiteId.value,
      products: extractedProducts.value
    })
    if (response.success) {
      addProductsCount.value = response.products_added ?? extractedProducts.value.length
      step.value = 'success'
    } else {
      alert('Error adding products: ' + (response.error || 'Unknown error'))
    }
  } catch (error) {
    console.error('Error:', error)
    alert('Failed to add products: ' + error.message)
  } finally {
    creating.value = false
  }
}

function resetToUpload() {
  step.value = 'upload'
  extractedProducts.value = []
  addProductsCount.value = 0
}

function reset() {
  step.value = 'mode'
  mode.value = 'create'
  selectedWebsiteId.value = null
  selectedRestaurantName.value = ''
  extractedProducts.value = []
  restaurantInfo.value = {
    restaurant_name: '',
    restaurant_name_ar: '',
    phone: '',
    email: '',
    address: '',
    address_ar: '',
    description: '',
    description_ar: ''
  }
  createdRestaurantId.value = null
  addProductsCount.value = 0
}
</script>

<style scoped>
.app {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 2rem;
}

.app-header {
  text-align: center;
  color: white;
  margin-bottom: 3rem;
}

.app-header h1 {
  font-size: 2.5rem;
  margin-bottom: 0.5rem;
  text-shadow: 2px 2px 4px rgba(0,0,0,0.2);
}

.app-header p {
  font-size: 1.1rem;
  opacity: 0.9;
}

.app-main {
  max-width: 1200px;
  margin: 0 auto;
}

.step-section {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  box-shadow: 0 10px 40px rgba(0,0,0,0.1);
}

.success-section {
  text-align: center;
}

.success-content h2 {
  color: #10b981;
  margin-bottom: 1rem;
}

.success-info {
  background: #f3f4f6;
  padding: 1.5rem;
  border-radius: 8px;
  margin: 2rem 0;
  text-align: left;
  max-width: 500px;
  margin-left: auto;
  margin-right: auto;
}

.success-info p {
  margin: 0.5rem 0;
}

.btn {
  padding: 0.75rem 2rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-primary {
  background: #667eea;
  color: white;
}

.btn-primary:hover {
  background: #5568d3;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.mode-section h2 {
  margin-top: 0;
  margin-bottom: 1.5rem;
  color: #1f2937;
  text-align: center;
}

.mode-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.mode-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 2rem 1.5rem;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.2s;
}

.mode-card:hover {
  border-color: #667eea;
  background: #f5f3ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.mode-icon {
  font-size: 2.5rem;
  margin-bottom: 0.75rem;
}

.mode-title {
  font-weight: 600;
  font-size: 1.1rem;
  color: #1f2937;
  margin-bottom: 0.5rem;
}

.mode-desc {
  font-size: 0.9rem;
  color: #6b7280;
  line-height: 1.4;
}

.restaurant-picker,
.continue-wrap {
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e5e7eb;
}

.picker-label {
  font-weight: 500;
  color: #374151;
  margin-bottom: 0.5rem;
}

.restaurant-select {
  width: 100%;
  max-width: 400px;
  padding: 0.75rem 1rem;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 1rem;
  margin-bottom: 0.5rem;
}

.error-msg {
  color: #dc2626;
  font-size: 0.9rem;
  margin: 0.5rem 0;
}

.continue-btn {
  margin-top: 1rem;
}

.context-msg {
  margin-bottom: 1rem;
  color: #374151;
}

.upload-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1.5rem;
  align-items: flex-start;
}

.upload-card {
  background: #f9fafb;
  border-radius: 12px;
  padding: 1.5rem;
  border: 1px solid #e5e7eb;
}

.upload-title {
  margin: 0 0 0.5rem;
  font-size: 1.1rem;
  font-weight: 600;
  color: #111827;
}

.upload-subtitle {
  margin: 0 0 1rem;
  font-size: 0.9rem;
  color: #6b7280;
}

.menu-textarea {
  width: 100%;
  padding: 0.75rem 1rem;
  border-radius: 8px;
  border: 1px solid #d1d5db;
  font-size: 0.95rem;
  font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  resize: vertical;
  min-height: 200px;
  box-sizing: border-box;
}

.menu-textarea:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 1px rgba(102, 126, 234, 0.3);
}

.extract-text-btn {
  margin-top: 0.75rem;
}

.back-mode-btn {
  margin-top: 1rem;
}

.success-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
  flex-wrap: wrap;
  margin-top: 1.5rem;
}
</style>
