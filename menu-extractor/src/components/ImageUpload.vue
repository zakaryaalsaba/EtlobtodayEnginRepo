<template>
  <div class="image-upload">
    <h2>Upload Menu Images</h2>
    <p class="subtitle">Drag and drop menu images or click to browse</p>

    <div
      class="upload-area"
      :class="{ 'drag-over': isDragOver, 'has-images': uploadedImages.length > 0 }"
      @drop="handleDrop"
      @dragover.prevent="isDragOver = true"
      @dragleave="isDragOver = false"
      @click="triggerFileInput"
    >
      <input
        ref="fileInput"
        type="file"
        multiple
        accept="image/*"
        @change="handleFileSelect"
        style="display: none"
      />

      <div v-if="uploadedImages.length === 0" class="upload-placeholder">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
          <polyline points="17 8 12 3 7 8"></polyline>
          <line x1="12" y1="3" x2="12" y2="15"></line>
        </svg>
        <p>Drop images here or click to browse</p>
        <p class="hint">Supports PNG, JPG, JPEG, WEBP</p>
      </div>

      <div v-else class="image-preview-grid">
        <div
          v-for="(image, index) in uploadedImages"
          :key="index"
          class="image-preview-item"
        >
          <img :src="getImageUrl(image)" alt="Menu preview" />
          <button @click.stop="removeImage(index)" class="remove-btn">×</button>
        </div>
      </div>
    </div>

    <div v-if="uploadedImages.length > 0" class="actions">
      <button @click="clearImages" class="btn btn-secondary">Clear All</button>
      <button @click="processImages" class="btn btn-primary" :disabled="loading">
        {{ loading ? 'Processing...' : `Process ${uploadedImages.length} Image(s)` }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  loading: Boolean
})

const emit = defineEmits(['images-uploaded'])

const fileInput = ref(null)
const uploadedImages = ref([])
const isDragOver = ref(false)

function triggerFileInput() {
  fileInput.value?.click()
}

function handleFileSelect(event) {
  const files = Array.from(event.target.files)
  addImages(files)
}

function handleDrop(event) {
  isDragOver.value = false
  const files = Array.from(event.dataTransfer.files).filter(file =>
    file.type.startsWith('image/')
  )
  addImages(files)
}

function addImages(files) {
  const imageFiles = files.filter(file => file.type.startsWith('image/'))
  uploadedImages.value.push(...imageFiles)
}

function removeImage(index) {
  uploadedImages.value.splice(index, 1)
}

function clearImages() {
  uploadedImages.value = []
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

function getImageUrl(file) {
  return URL.createObjectURL(file)
}

async function processImages() {
  if (uploadedImages.value.length > 0) {
    // First verify API is accessible
    try {
      const testResponse = await fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:3000/api'}/menu-extractor/test`)
      if (!testResponse.ok) {
        console.warn('API test endpoint not accessible')
      }
    } catch (error) {
      console.warn('Could not reach API test endpoint:', error)
    }
    
    emit('images-uploaded', uploadedImages.value)
  }
}
</script>

<style scoped>
.image-upload {
  width: 100%;
}

.image-upload h2 {
  margin-bottom: 0.5rem;
  color: #1f2937;
}

.subtitle {
  color: #6b7280;
  margin-bottom: 2rem;
}

.upload-area {
  border: 3px dashed #d1d5db;
  border-radius: 12px;
  padding: 3rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  background: #f9fafb;
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-area.drag-over {
  border-color: #667eea;
  background: #eef2ff;
}

.upload-area.has-images {
  padding: 1.5rem;
  min-height: auto;
}

.upload-placeholder {
  color: #6b7280;
}

.upload-placeholder svg {
  margin-bottom: 1rem;
  color: #9ca3af;
}

.upload-placeholder p {
  margin: 0.5rem 0;
  font-size: 1.1rem;
}

.hint {
  font-size: 0.9rem;
  color: #9ca3af;
}

.image-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 1rem;
  width: 100%;
}

.image-preview-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.image-preview-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-btn {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  background: rgba(239, 68, 68, 0.9);
  color: white;
  border: none;
  border-radius: 50%;
  width: 28px;
  height: 28px;
  font-size: 1.2rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  transition: all 0.2s;
}

.remove-btn:hover {
  background: rgba(220, 38, 38, 1);
  transform: scale(1.1);
}

.actions {
  display: flex;
  gap: 1rem;
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

.btn-primary {
  background: #667eea;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #5568d3;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-secondary {
  background: #e5e7eb;
  color: #374151;
}

.btn-secondary:hover {
  background: #d1d5db;
}
</style>
