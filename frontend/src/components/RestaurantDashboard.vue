<template>
  <div class="min-h-screen bg-gray-50" :dir="$i18n.locale === 'ar' ? 'rtl' : 'ltr'">
    <!-- Header -->
    <header class="bg-white shadow-sm border-b border-gray-200">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
        <div class="flex items-center justify-between" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
          <div>
            <h1 class="text-2xl font-bold text-gray-900">{{ restaurantInfo?.restaurant_name || $t('restaurantDashboard.title') }}</h1>
            <p class="text-sm text-gray-600">{{ $t('restaurantDashboard.subtitle') }}</p>
          </div>
          <div class="flex items-center gap-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
            <LanguageSwitcher />
            <div class="flex items-center gap-2">
              <div 
                v-if="sseConnected" 
                class="flex items-center gap-1 text-green-600 text-sm"
                title="Real-time updates connected"
              >
                <div class="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
                <span class="hidden sm:inline">{{ $t('restaurantDashboard.live') }}</span>
              </div>
              <button
                @click="viewWebsite"
                class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold flex items-center gap-2 text-sm"
              >
                <svg 
                  class="w-4 h-4"
                  fill="none" 
                  stroke="currentColor" 
                  viewBox="0 0 24 24"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                </svg>
                <span>{{ $t('restaurantDashboard.viewWebsite') }}</span>
              </button>
              <button
                @click="goToAdminDashboard"
                class="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors font-semibold flex items-center gap-2 text-sm"
              >
                <svg 
                  class="w-4 h-4"
                  fill="none" 
                  stroke="currentColor" 
                  viewBox="0 0 24 24"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                </svg>
                <span>{{ $t('restaurantDashboard.adminDashboard') }}</span>
              </button>
              <button
                @click="loadOrders"
                :disabled="loadingOrders"
                class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2 text-sm"
              >
                <svg 
                  :class="['w-4 h-4', loadingOrders ? 'animate-spin' : '']"
                  fill="none" 
                  stroke="currentColor" 
                  viewBox="0 0 24 24"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                <span>{{ loadingOrders ? $t('restaurantDashboard.refreshing') : $t('websiteBuilder.refresh') }}</span>
              </button>
            </div>
            <span class="text-sm text-gray-600">{{ $t('restaurantDashboard.welcome') }}, {{ restaurantInfo?.name }}</span>
            <button
              @click="handleLogout"
              class="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors font-semibold text-sm"
            >
              {{ $t('restaurantDashboard.logout') }}
            </button>
          </div>
        </div>
      </div>
    </header>

    <!-- Tabs -->
    <div class="bg-white border-b border-gray-200">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <nav class="flex overflow-x-auto" :class="$i18n.locale === 'ar' ? 'space-x-reverse space-x-8' : 'space-x-8'">
          <button
            @click="activeTab = 'basic'"
            :class="[
              'py-4 px-1 border-b-2 font-medium text-sm transition-colors whitespace-nowrap',
              activeTab === 'basic'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            ]"
          >
            {{ $t('websiteBuilder.basicInformation') }}
          </button>
          <button
            @click="activeTab = 'menu'"
            :class="[
              'py-4 px-1 border-b-2 font-medium text-sm transition-colors whitespace-nowrap',
              activeTab === 'menu'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            ]"
          >
            {{ $t('websiteBuilder.menu') }}
          </button>
          <button
            @click="activeTab = 'businessHours'"
            :class="[
              'py-4 px-1 border-b-2 font-medium text-sm transition-colors whitespace-nowrap',
              activeTab === 'businessHours'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            ]"
          >
            {{ $t('restaurantDashboard.businessHours') }}
          </button>
          <button
            @click="activeTab = 'offers'"
            :class="[
              'py-4 px-1 border-b-2 font-medium text-sm transition-colors whitespace-nowrap',
              activeTab === 'offers'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            ]"
          >
            {{ $t('restaurantDashboard.offers') }}
          </button>
          <button
            @click="activeTab = 'orderType'"
            :class="[
              'py-4 px-1 border-b-2 font-medium text-sm transition-colors whitespace-nowrap',
              activeTab === 'orderType'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            ]"
          >
            {{ $t('websiteBuilder.orderType') }}
          </button>
          <button
            @click="activeTab = 'branches'"
            :class="[
              'py-4 px-1 border-b-2 font-medium text-sm transition-colors whitespace-nowrap',
              activeTab === 'branches'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            ]"
          >
            Branches
          </button>
          <button
            @click="activeTab = 'paymentMethods'"
            :class="[
              'py-4 px-1 border-b-2 font-medium text-sm transition-colors whitespace-nowrap',
              activeTab === 'paymentMethods'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            ]"
          >
            {{ $t('restaurantDashboard.paymentMethods') }}
          </button>
          <button
            @click="activeTab = 'taxRequirement'"
            :class="[
              'py-4 px-1 border-b-2 font-medium text-sm transition-colors whitespace-nowrap',
              activeTab === 'taxRequirement'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            ]"
          >
            {{ $t('restaurantDashboard.taxRequirement') }}
          </button>
          <button
            @click="activeTab = 'coupons'"
            :class="[
              'py-4 px-1 border-b-2 font-medium text-sm transition-colors whitespace-nowrap',
              activeTab === 'coupons'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            ]"
          >
            {{ $t('restaurantDashboard.coupons') }}
          </button>
          <button
            @click="activeTab = 'notifications'"
            :class="[
              'py-4 px-1 border-b-2 font-medium text-sm transition-colors whitespace-nowrap',
              activeTab === 'notifications'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            ]"
          >
            {{ $t('restaurantDashboard.notifications') }}
          </button>
          <button
            @click="activeTab = 'language'"
            :class="[
              'py-4 px-1 border-b-2 font-medium text-sm transition-colors whitespace-nowrap',
              activeTab === 'language'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            ]"
          >
            {{ $t('restaurantDashboard.language') }}
          </button>
          <button
            @click="activeTab = 'currency'"
            :class="[
              'py-4 px-1 border-b-2 font-medium text-sm transition-colors whitespace-nowrap',
              activeTab === 'currency'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            ]"
          >
            {{ $t('restaurantDashboard.currency') }}
          </button>
          <button
            @click="activeTab = 'settings'"
            :class="[
              'py-4 px-1 border-b-2 font-medium text-sm transition-colors whitespace-nowrap',
              activeTab === 'settings'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            ]"
          >
            {{ $t('restaurantDashboard.settings') }}
          </button>
          <button
            @click="activeTab = 'additional'"
            :class="[
              'py-4 px-1 border-b-2 font-medium text-sm transition-colors whitespace-nowrap',
              activeTab === 'additional'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            ]"
          >
            {{ $t('websiteBuilder.additional') }}
          </button>
          <button
            @click="activeTab = 'gallery'"
            :class="[
              'py-4 px-1 border-b-2 font-medium text-sm transition-colors whitespace-nowrap',
              activeTab === 'gallery'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            ]"
          >
            {{ $t('websiteBuilder.gallery') }}
          </button>
        </nav>
      </div>
    </div>

    <!-- Main Content -->
    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- Basic Information Tab -->
      <div v-show="activeTab === 'basic'" class="space-y-6">
        <div v-if="loadingWebsite" class="text-center py-12">
          <div class="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
          <p class="mt-4 text-gray-600">{{ $t('restaurantDashboard.loadingWebsite') }}</p>
        </div>
        
        <div v-else-if="website" class="bg-white rounded-xl shadow-md p-8">
          <h2 class="text-2xl font-bold text-gray-900 mb-6">{{ $t('restaurantDashboard.websiteSettings') }}</h2>
          
          <form @submit.prevent="saveWebsite" class="space-y-6">
            <!-- Restaurant Name (label uses i18n for current locale) -->
            <!-- Restaurant name: English and Arabic (stored separately in DB) -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">
                  {{ $t('restaurantDashboard.restaurantNameEn') }} <span class="text-red-500">{{ $t('websiteBuilder.required') }}</span>
                </label>
                <input
                  v-model="websiteForm.restaurant_name"
                  type="text"
                  required
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  :placeholder="$t('restaurantDashboard.restaurantNamePlaceholder')"
                />
              </div>
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.restaurantNameAr') }}</label>
                <input
                  v-model="websiteForm.restaurant_name_ar"
                  type="text"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  placeholder="اسم المطعم بالعربية"
                />
              </div>
            </div>

            <!-- Logo Upload -->
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('websiteBuilder.logo') }}</label>
              <div class="flex items-center gap-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <div v-if="websiteForm.logo_url || logoPreview" class="flex-shrink-0">
                  <img
                    :src="logoPreview || websiteForm.logo_url"
                    alt="Logo preview"
                    class="h-24 w-24 object-contain border border-gray-300 rounded-lg p-2 bg-gray-50"
                  />
                </div>
                <div class="flex-1">
                  <input
                    type="file"
                    accept="image/*"
                    @change="handleLogoUpload"
                    :class="['block w-full text-sm text-gray-500 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-semibold file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100', $i18n.locale === 'ar' ? 'file:ml-4' : 'file:mr-4']"
                  />
                  <p class="text-xs text-gray-500 mt-1">{{ $t('websiteBuilder.logoHint') }}</p>
                </div>
              </div>
            </div>

            <!-- Description: English and Arabic (stored separately in DB) -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.descriptionEn') }}</label>
                <textarea
                  v-model="websiteForm.description"
                  rows="4"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  :placeholder="$t('restaurantDashboard.descriptionPlaceholder')"
                ></textarea>
              </div>
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.descriptionAr') }}</label>
                <textarea
                  v-model="websiteForm.description_ar"
                  rows="4"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  placeholder="الوصف بالعربية"
                ></textarea>
              </div>
            </div>

            <!-- Contact Information: Address EN/AR + Phone, Email, etc. -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.addressEn') }}</label>
                <input
                  v-model="websiteForm.address"
                  type="text"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  :placeholder="$t('restaurantDashboard.addressPlaceholder')"
                />
              </div>
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.addressAr') }}</label>
                <input
                  v-model="websiteForm.address_ar"
                  type="text"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  placeholder="العنوان بالعربية"
                />
              </div>
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('websiteBuilder.phone') }}</label>
                <input
                  v-model="websiteForm.phone"
                  type="tel"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  :placeholder="$t('websiteBuilder.phonePlaceholder')"
                />
              </div>
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('websiteBuilder.email') }}</label>
                <input
                  v-model="websiteForm.email"
                  type="email"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  :placeholder="$t('websiteBuilder.emailPlaceholder')"
                />
              </div>
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('websiteBuilder.websiteUrl') }}</label>
                <input
                  v-model="websiteForm.website_url"
                  type="url"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  :placeholder="$t('websiteBuilder.websiteUrlPlaceholder')"
                />
              </div>
            </div>

            <!-- Color Scheme -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('websiteBuilder.primaryColor') }}</label>
                <div class="flex items-center gap-3" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <input
                    v-model="websiteForm.primary_color"
                    type="color"
                    class="h-10 w-20 border border-gray-300 rounded-lg cursor-pointer"
                  />
                  <input
                    v-model="websiteForm.primary_color"
                    type="text"
                    class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 font-mono text-sm"
                  />
                </div>
              </div>
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('websiteBuilder.secondaryColor') }}</label>
                <div class="flex items-center gap-3" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <input
                    v-model="websiteForm.secondary_color"
                    type="color"
                    class="h-10 w-20 border border-gray-300 rounded-lg cursor-pointer"
                  />
                  <input
                    v-model="websiteForm.secondary_color"
                    type="text"
                    class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 font-mono text-sm"
                  />
                </div>
              </div>
            </div>


            <div v-if="saveError" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
              {{ saveError }}
            </div>

            <div v-if="saveSuccess" class="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg text-sm">
              {{ saveSuccess }}
            </div>

            <button
              type="submit"
              :disabled="saving"
              class="px-6 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <span v-if="saving">{{ $t('websiteBuilder.saving') }}</span>
              <span v-else>{{ $t('websiteBuilder.saveChanges') }}</span>
            </button>
          </form>
        </div>
      </div>

      <!-- Menu Tab -->
      <div v-show="activeTab === 'menu'" class="space-y-6">
        <div class="bg-white rounded-xl shadow-md p-8">
          <div class="flex items-center justify-between mb-6" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
            <h2 class="text-2xl font-bold text-gray-900">{{ $t('websiteBuilder.products') }}</h2>
            <button
              @click="showProductForm = true; editingProduct = null"
              class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold"
            >
              {{ $t('websiteBuilder.addProduct') }}
            </button>
          </div>

          <div v-if="loadingProducts" class="text-center py-12">
            <div class="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
            <p class="mt-4 text-gray-600">{{ $t('restaurantDashboard.loadingProducts') }}</p>
          </div>

          <div v-else-if="products.length === 0" class="text-center py-12">
            <div class="text-4xl mb-4">🍽️</div>
            <p class="text-gray-600 font-semibold">{{ $t('websiteBuilder.noProducts') }}</p>
            <p class="text-sm text-gray-500 mt-2">{{ $t('websiteBuilder.noProductsHint') }}</p>
          </div>

          <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <div
              v-for="product in products"
              :key="product.id"
              class="bg-gray-50 rounded-lg p-4 border border-gray-200 hover:shadow-md transition-shadow"
            >
              <div v-if="product.image_url" class="mb-4">
                <img :src="product.image_url" :alt="product.name" class="w-full h-40 object-cover rounded-lg" />
              </div>
              <h3 class="font-bold text-gray-900 mb-1">{{ product.name }}</h3>
              <p class="text-sm text-gray-600 mb-2 line-clamp-2">{{ product.description }}</p>
              <div class="flex items-center justify-between mb-3" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <span class="text-lg font-bold text-indigo-600">${{ parseFloat(product.price).toFixed(2) }}</span>
                <span
                  :class="[
                    'px-2 py-1 text-xs rounded-full',
                    product.is_available ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                  ]"
                >
                  {{ product.is_available ? $t('websiteBuilder.available') : $t('websiteBuilder.unavailable') }}
                </span>
              </div>
              <div class="flex flex-wrap gap-2">
                <button
                  @click="openAddons(product)"
                  class="px-3 py-2 bg-amber-100 text-amber-800 rounded-lg hover:bg-amber-200 transition-colors text-sm font-semibold"
                  :title="$t('restaurantDashboard.manageAddOns')"
                >
                  {{ $t('restaurantDashboard.addOns') }}
                </button>
                <button
                  @click="editProduct(product)"
                  class="flex-1 min-w-0 px-3 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition-colors text-sm font-semibold"
                >
                  {{ $t('websiteBuilder.edit') }}
                </button>
                <button
                  @click="deleteProduct(product.id)"
                  class="px-3 py-2 bg-red-100 text-red-700 rounded-lg hover:bg-red-200 transition-colors text-sm font-semibold"
                >
                  {{ $t('websiteBuilder.delete') }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Add-ons Modal -->
      <div v-if="showAddonsModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
        <div class="bg-white rounded-xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
          <div class="p-6">
            <div class="flex items-center justify-between mb-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
              <h3 class="text-xl font-bold text-gray-900">
                {{ $t('restaurantDashboard.addOns') }} – {{ addonsProduct?.name }}
              </h3>
              <button @click="closeAddons" class="text-gray-400 hover:text-gray-600">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <div v-if="addonError && !showAddonForm" class="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 rounded-lg text-sm">{{ addonError }}</div>

            <!-- Add-on rule: Optional / Required -->
            <div class="mb-6 p-4 bg-gray-50 rounded-lg">
              <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.addonRule') }}</label>
              <div class="flex flex-wrap gap-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <label class="flex items-center cursor-pointer">
                  <input v-model="addonRequired" type="radio" :value="false" class="w-4 h-4 text-indigo-600" />
                  <span :class="['ml-2 text-sm', $i18n.locale === 'ar' ? 'mr-2 ml-0' : '']">{{ $t('restaurantDashboard.addonOptional') }}</span>
                </label>
                <label class="flex items-center cursor-pointer">
                  <input v-model="addonRequired" type="radio" :value="true" class="w-4 h-4 text-indigo-600" />
                  <span :class="['ml-2 text-sm', $i18n.locale === 'ar' ? 'mr-2 ml-0' : '']">{{ $t('restaurantDashboard.addonRequired') }}</span>
                </label>
              </div>
              <p class="text-xs text-gray-500 mt-1">{{ addonRequired ? $t('restaurantDashboard.addonRequiredHint') : $t('restaurantDashboard.addonOptionalHint') }}</p>
              <div v-if="addonRequired" class="mt-3 flex items-center gap-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <label class="text-sm font-medium text-gray-700">{{ $t('restaurantDashboard.addonRequiredMin') }}</label>
                <select v-model="addonRequiredMin" class="px-3 py-2 border border-gray-300 rounded-lg text-sm">
                  <option :value="1">1 {{ $t('restaurantDashboard.addonAtLeast') }}</option>
                  <option :value="2">2 {{ $t('restaurantDashboard.addonAtLeast') }}</option>
                  <option :value="3">3 {{ $t('restaurantDashboard.addonAtLeast') }}</option>
                  <option :value="-1">{{ $t('restaurantDashboard.addonAll') }}</option>
                </select>
                <button
                  type="button"
                  @click="saveAddonSettings"
                  class="px-3 py-2 bg-indigo-600 text-white rounded-lg text-sm font-semibold hover:bg-indigo-700"
                >
                  {{ $t('restaurantDashboard.saveAddonSettings') }}
                </button>
              </div>
            </div>

            <!-- List add-ons -->
            <div class="mb-4">
              <div class="flex items-center justify-between mb-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <span class="text-sm font-semibold text-gray-700">{{ $t('restaurantDashboard.addOnsFor') }} {{ addonsProduct?.name }}</span>
                <button
                  v-if="!showAddonForm"
                  type="button"
                  @click="showAddonForm = true; editingAddon = null; addonForm = { name: '', name_ar: '', description: '', description_ar: '', price: '0', is_required: false }; addonError = ''"
                  class="px-3 py-1.5 bg-indigo-600 text-white rounded-lg text-sm font-semibold hover:bg-indigo-700"
                >
                  {{ $t('restaurantDashboard.addAddon') }}
                </button>
              </div>

              <div v-if="showAddonForm" class="mb-4 p-4 border border-gray-200 rounded-lg bg-gray-50">
                <p class="text-sm font-semibold text-gray-700 mb-2">{{ editingAddon ? $t('websiteBuilder.editProduct') : $t('restaurantDashboard.addAddon') }}</p>
                <div class="space-y-2">
                  <div class="grid grid-cols-1 md:grid-cols-2 gap-2">
                    <div>
                      <label class="block text-xs font-medium text-gray-600 mb-0.5">{{ $t('restaurantDashboard.addonNameEn') }}</label>
                      <input
                        v-model="addonForm.name"
                        type="text"
                        :placeholder="$t('restaurantDashboard.addonNamePlaceholder')"
                        class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                      />
                    </div>
                    <div>
                      <label class="block text-xs font-medium text-gray-600 mb-0.5">{{ $t('restaurantDashboard.addonNameAr') }}</label>
                      <input
                        v-model="addonForm.name_ar"
                        type="text"
                        placeholder="اسم الإضافة بالعربية"
                        class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                      />
                    </div>
                  </div>
                  <div class="grid grid-cols-1 md:grid-cols-2 gap-2">
                    <div>
                      <label class="block text-xs font-medium text-gray-600 mb-0.5">{{ $t('restaurantDashboard.addonDescriptionEn') }}</label>
                      <textarea
                        v-model="addonForm.description"
                        rows="2"
                        :placeholder="$t('restaurantDashboard.addonDescription')"
                        class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                      />
                    </div>
                    <div>
                      <label class="block text-xs font-medium text-gray-600 mb-0.5">{{ $t('restaurantDashboard.addonDescriptionAr') }}</label>
                      <textarea
                        v-model="addonForm.description_ar"
                        rows="2"
                        placeholder="الوصف بالعربية"
                        class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                      />
                    </div>
                  </div>
                  <input
                    v-model="addonForm.price"
                    type="number"
                    step="0.01"
                    min="0"
                    :placeholder="$t('restaurantDashboard.addonPrice')"
                    class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                  />
                  <label class="flex items-center cursor-pointer gap-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                    <input v-model="addonForm.is_required" type="checkbox" class="w-4 h-4 text-indigo-600 rounded" />
                    <span class="text-sm text-gray-700">{{ $t('restaurantDashboard.addonItemRequired') }}</span>
                  </label>
                  <div>
                    <label class="block text-xs text-gray-600 mb-1">{{ $t('restaurantDashboard.addonImage') }}</label>
                    <input type="file" accept="image/*" @change="handleAddonImageUpload" class="w-full text-sm" />
                    <img v-if="addonForm.imagePreview" :src="addonForm.imagePreview" alt="" class="mt-2 h-20 w-20 object-cover rounded border" />
                  </div>
                </div>
                <div v-if="addonError" class="mt-2 text-sm text-red-600">{{ addonError }}</div>
                <div class="flex gap-2 mt-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <button
                    type="button"
                    @click="saveAddon"
                    :disabled="savingAddon"
                    class="px-3 py-2 bg-indigo-600 text-white rounded-lg text-sm font-semibold disabled:opacity-50"
                  >
                    {{ savingAddon ? $t('websiteBuilder.saving') : (editingAddon ? $t('websiteBuilder.updateProduct') : $t('websiteBuilder.createProduct')) }}
                  </button>
                  <button type="button" @click="showAddonForm = false; editingAddon = null" class="px-3 py-2 bg-gray-200 rounded-lg text-sm font-semibold">
                    {{ $t('websiteBuilder.cancel') }}
                  </button>
                </div>
              </div>

              <div v-if="loadingAddons" class="text-center py-4 text-gray-500 text-sm">{{ $t('restaurantDashboard.loadingProducts') }}</div>
              <div v-else-if="addonsList.length === 0" class="py-4 text-center text-gray-500 text-sm">
                <p>{{ $t('restaurantDashboard.noAddons') }}</p>
                <p class="text-xs mt-1">{{ $t('restaurantDashboard.noAddonsHint') }}</p>
              </div>
              <ul v-else class="space-y-2">
                <li
                  v-for="addon in addonsList"
                  :key="addon.id"
                  class="flex items-center justify-between p-3 bg-white border border-gray-200 rounded-lg"
                  :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''"
                >
                  <div class="flex items-center gap-3" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                    <img v-if="addon.image_url" :src="addon.image_url" :alt="getAddonDisplay(addon).name" class="h-12 w-12 object-cover rounded" />
                    <div class="h-12 w-12 rounded bg-gray-100 flex items-center justify-center text-gray-400 text-xs" v-else>—</div>
                    <div>
                      <p class="font-medium text-gray-900">{{ getAddonDisplay(addon).name }}</p>
                      <p class="text-xs text-gray-600">{{ getAddonDisplay(addon).description || '—' }}</p>
                      <p class="text-sm font-semibold text-indigo-600">${{ parseFloat(addon.price).toFixed(2) }}</p>
                      <span
                        :class="[
                          'inline-block mt-1 px-2 py-0.5 text-xs rounded',
                          addon.is_required ? 'bg-amber-100 text-amber-800' : 'bg-gray-100 text-gray-600'
                        ]"
                      >
                        {{ addon.is_required ? $t('restaurantDashboard.addonRequired') : $t('restaurantDashboard.addonOptional') }}
                      </span>
                    </div>
                  </div>
                  <div class="flex gap-1" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                    <button type="button" @click="editAddon(addon)" class="px-2 py-1 bg-gray-200 rounded text-sm font-medium hover:bg-gray-300">{{ $t('websiteBuilder.edit') }}</button>
                    <button type="button" @click="deleteAddon(addon.id)" class="px-2 py-1 bg-red-100 text-red-700 rounded text-sm font-medium hover:bg-red-200">{{ $t('websiteBuilder.delete') }}</button>
                  </div>
                </li>
              </ul>
            </div>

            <div class="flex justify-end">
              <button type="button" @click="closeAddons" class="px-4 py-2 bg-gray-200 rounded-lg font-semibold hover:bg-gray-300">{{ $t('websiteBuilder.cancel') }}</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Gallery Tab -->
      <div v-show="activeTab === 'gallery'" class="space-y-6">
        <div class="bg-white rounded-xl shadow-md p-8">
          <div class="flex items-center justify-between mb-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
            <div>
              <h2 class="text-2xl font-bold text-gray-900 mb-2">{{ $t('websiteBuilder.galleryImages') }}</h2>
              <p class="text-sm text-gray-500">{{ $t('websiteBuilder.galleryHint') }}</p>
            </div>
            <div class="flex gap-2">
              <label
                for="gallery-upload"
                class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors cursor-pointer inline-block text-sm font-semibold"
              >
                {{ $t('websiteBuilder.addImages') }}
              </label>
              <input
                id="gallery-upload"
                type="file"
                accept="image/*"
                multiple
                @change="handleGalleryUpload"
                class="hidden"
              />
              <button
                v-if="galleryFiles.length > 0"
                type="button"
                @click="uploadGalleryNow"
                :disabled="uploadingGallery"
                class="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors text-sm font-semibold disabled:opacity-50"
              >
                {{ uploadingGallery ? $t('websiteBuilder.uploading') : $t('websiteBuilder.uploadNow') }}
              </button>
            </div>
          </div>
          
          <div v-if="galleryImages.length > 0" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4 mt-4">
            <div
              v-for="(img, index) in galleryImages"
              :key="index"
              class="relative group aspect-square"
            >
              <img 
                :src="img.url || img" 
                :alt="`Gallery image ${index + 1}`" 
                class="w-full h-full object-cover rounded-lg border-2 border-gray-200 group-hover:border-indigo-500 transition-colors" 
              />
              <button
                type="button"
                @click="removeGalleryImage(index)"
                :class="['absolute bg-red-500 text-white rounded-full w-7 h-7 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity shadow-lg hover:bg-red-600', $i18n.locale === 'ar' ? 'top-2 left-2' : 'top-2 right-2']"
                :title="$t('restaurantDashboard.removeImage')"
              >
                ×
              </button>
            </div>
          </div>
          
          <div v-else class="border-2 border-dashed border-gray-300 rounded-lg p-12 text-center">
            <div class="text-4xl mb-4">📷</div>
            <p class="text-gray-500 mb-2">{{ $t('websiteBuilder.noGalleryImages') }}</p>
            <p class="text-sm text-gray-400">{{ $t('websiteBuilder.noGalleryImagesHint') }}</p>
          </div>
        </div>
      </div>

      <!-- Business hours Tab -->
      <div v-show="activeTab === 'businessHours'" class="space-y-6">
        <div class="bg-white rounded-xl shadow-md p-8">
          <h2 class="text-2xl font-bold text-gray-900 mb-2">{{ $t('restaurantDashboard.businessHoursTitle') }}</h2>
          <p class="text-gray-600 mb-6">{{ $t('restaurantDashboard.businessHoursSubtitle') }}</p>
          
          <form @submit.prevent="saveBusinessHours" class="space-y-4">
            <div
              v-for="(day, index) in businessHours"
              :key="day.day_of_week"
              class="flex flex-wrap items-center gap-4 p-4 border border-gray-200 rounded-lg"
              :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''"
            >
              <span class="w-28 font-medium text-gray-800">{{ dayNames[day.day_of_week] }}</span>
              <label class="flex items-center cursor-pointer" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <input
                  v-model="day.is_closed"
                  type="checkbox"
                  class="w-4 h-4 text-indigo-600 border-gray-300 rounded"
                />
                <span :class="['text-sm text-gray-600', $i18n.locale === 'ar' ? 'mr-2' : 'ml-2']">{{ $t('restaurantDashboard.businessHoursClosed') }}</span>
              </label>
              <template v-if="!day.is_closed">
                <label class="flex items-center gap-1" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <span class="text-sm text-gray-600">{{ $t('restaurantDashboard.businessHoursOpen') }}</span>
                  <input
                    v-model="day.open_time"
                    type="time"
                    class="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                  />
                </label>
                <label class="flex items-center gap-1" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <span class="text-sm text-gray-600">{{ $t('restaurantDashboard.businessHoursClose') }}</span>
                  <input
                    v-model="day.close_time"
                    type="time"
                    class="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                  />
                </label>
              </template>
            </div>

            <div v-if="businessHoursError" class="p-4 bg-red-50 border border-red-200 rounded-lg">
              <p class="text-sm text-red-600">{{ businessHoursError }}</p>
            </div>
            <div v-if="businessHoursSuccess" class="p-4 bg-green-50 border border-green-200 rounded-lg">
              <p class="text-sm text-green-600">{{ businessHoursSuccess }}</p>
            </div>

            <button
              type="submit"
              :disabled="savingBusinessHours"
              class="px-6 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <span v-if="savingBusinessHours">{{ $t('websiteBuilder.saving') }}</span>
              <span v-else>{{ $t('restaurantDashboard.businessHoursSave') }}</span>
            </button>
          </form>
        </div>
      </div>

      <!-- Offers Tab -->
      <div v-show="activeTab === 'offers'" class="space-y-6">
        <div class="bg-white rounded-xl shadow-md p-8">
          <div class="flex items-center justify-between mb-6" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
            <h2 class="text-2xl font-bold text-gray-900">{{ $t('restaurantDashboard.offers') }}</h2>
            <button
              @click="showOfferForm = true; editingOffer = null; resetOfferForm()"
              class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold flex items-center gap-2"
            >
              <span>+</span>
              <span>{{ $t('restaurantDashboard.addOffer') }}</span>
            </button>
          </div>
          <p class="text-gray-600 mb-6">{{ $t('restaurantDashboard.offersSubtitle') }}</p>

          <!-- Offer Form -->
          <div v-if="showOfferForm" class="mb-6 p-6 border-2 border-indigo-200 rounded-lg bg-indigo-50">
            <h3 class="text-xl font-bold text-gray-900 mb-4">
              {{ editingOffer ? $t('restaurantDashboard.editOffer') : $t('restaurantDashboard.addOffer') }}
            </h3>
            <form @submit.prevent="saveOffer" class="space-y-4">
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <!-- Offer Type -->
                <div>
                  <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.offerType') }} *</label>
                  <select
                    v-model="offerForm.offer_type"
                    required
                    class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  >
                    <option value="free_delivery">{{ $t('restaurantDashboard.offerFreeDelivery') }}</option>
                    <option value="free_delivery_over_x_jod">{{ $t('restaurantDashboard.offerFreeDeliveryOverX') }}</option>
                    <option value="percent_off">{{ $t('restaurantDashboard.offerPercentOff') }}</option>
                    <option value="minimum_order_value">{{ $t('restaurantDashboard.offerMinOrderValue') }}</option>
                  </select>
                </div>

                <!-- Title (auto-filled for percent_off and free_delivery_over_x_jod) -->
                <div>
                  <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.offerTitle') }} *</label>
                  <input
                    v-if="!offerTitleAuto"
                    v-model="offerForm.title"
                    type="text"
                    required
                    class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                    :placeholder="$t('restaurantDashboard.offerTitlePlaceholder')"
                  />
                  <div
                    v-else
                    class="w-full px-4 py-2 border border-gray-200 rounded-lg bg-gray-50 text-gray-800 font-medium"
                  >
                    {{ offerTitleAuto }}
                  </div>
                </div>

                <!-- Value (for free_delivery_over_x_jod, percent_off, minimum_order_value) -->
                <div v-if="offerForm.offer_type !== 'free_delivery'">
                  <label class="block text-sm font-semibold text-gray-700 mb-2">
                    {{ offerForm.offer_type === 'percent_off' ? $t('restaurantDashboard.offerPercentValue') : $t('restaurantDashboard.offerValueJOD') }} *
                  </label>
                  <div class="flex items-center gap-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                    <input
                      v-model="offerForm.value"
                      type="number"
                      step="0.01"
                      min="0"
                      :required="offerForm.offer_type !== 'free_delivery'"
                      class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      :placeholder="offerForm.offer_type === 'percent_off' ? '20' : '15.00'"
                    />
                    <span class="text-gray-600">{{ offerForm.offer_type === 'percent_off' ? '%' : 'JOD' }}</span>
                  </div>
                </div>

                <!-- For percent_off: Apply to All items or Selected items -->
                <div v-if="offerForm.offer_type === 'percent_off'" class="md:col-span-2">
                  <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.offerApplyTo') }} *</label>
                  <div class="flex flex-wrap gap-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                    <label class="flex items-center cursor-pointer" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                      <input
                        v-model="offerForm.offer_scope"
                        type="radio"
                        value="all_items"
                        class="w-4 h-4 text-indigo-600 border-gray-300 focus:ring-indigo-500"
                      />
                      <span :class="['text-sm font-medium text-gray-700', $i18n.locale === 'ar' ? 'mr-2' : 'ml-2']">{{ $t('restaurantDashboard.offerApplyToAllItems') }}</span>
                    </label>
                    <label class="flex items-center cursor-pointer" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                      <input
                        v-model="offerForm.offer_scope"
                        type="radio"
                        value="selected_items"
                        class="w-4 h-4 text-indigo-600 border-gray-300 focus:ring-indigo-500"
                      />
                      <span :class="['text-sm font-medium text-gray-700', $i18n.locale === 'ar' ? 'mr-2' : 'ml-2']">{{ $t('restaurantDashboard.offerApplyToSelectedItems') }}</span>
                    </label>
                  </div>
                </div>

                <!-- Selected items: products and addons (percent_off + selected_items) -->
                <div v-if="offerForm.offer_type === 'percent_off' && offerForm.offer_scope === 'selected_items'" class="md:col-span-2">
                  <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.offerSelectProductsAndAddons') }}</label>
                  <div class="border border-gray-200 rounded-lg p-4 bg-gray-50 max-h-80 overflow-y-auto">
                    <button
                      v-if="productsWithAddons.length === 0 && !loadingProductsWithAddons"
                      type="button"
                      @click="loadProductsWithAddonsForOffer()"
                      class="w-full py-3 text-indigo-600 border border-indigo-300 rounded-lg hover:bg-indigo-50 font-medium"
                    >
                      {{ $t('restaurantDashboard.offerLoadProducts') }}
                    </button>
                    <div v-else-if="loadingProductsWithAddons" class="py-4 text-center text-gray-500">
                      {{ $t('restaurantDashboard.loadingProducts') }}...
                    </div>
                    <div v-else class="space-y-4">
                      <div v-for="product in productsWithAddons" :key="product.id" class="border-b border-gray-200 pb-3 last:border-0 last:pb-0">
                        <label class="flex items-start gap-3 cursor-pointer" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                          <input
                            type="checkbox"
                            :checked="offerForm.selected_product_ids.includes(product.id)"
                            @change="toggleOfferProduct(product.id)"
                            class="mt-1 w-4 h-4 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                          />
                          <span class="font-medium text-gray-800">{{ product.name }}</span>
                        </label>
                        <div v-if="product.addons && product.addons.length" class="mt-2 ml-7 space-y-1" :class="$i18n.locale === 'ar' ? 'mr-7 ml-0' : ''">
                          <div v-for="addon in product.addons" :key="addon.id" class="flex items-center gap-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                            <input
                              type="checkbox"
                              :checked="offerForm.selected_addon_ids.includes(addon.id)"
                              @change="toggleOfferAddon(addon.id)"
                              class="w-4 h-4 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                            />
                            <span class="text-sm text-gray-700">{{ addon.name }}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Min order (for free_delivery_over_x_jod) -->
                <div v-if="offerForm.offer_type === 'free_delivery_over_x_jod'">
                  <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.minOrderValue') }}</label>
                  <div class="flex items-center gap-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                    <input
                      v-model="offerForm.min_order_value"
                      type="number"
                      step="0.01"
                      min="0"
                      class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      placeholder="0.00"
                    />
                    <span class="text-gray-600">JOD</span>
                  </div>
                </div>

                <!-- Valid From -->
                <div>
                  <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.validFrom') }} *</label>
                  <input
                    v-model="offerForm.valid_from"
                    type="date"
                    required
                    class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  />
                </div>

                <!-- Valid Until -->
                <div>
                  <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.validUntil') }} *</label>
                  <input
                    v-model="offerForm.valid_until"
                    type="date"
                    required
                    class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  />
                </div>

                <!-- Active -->
                <div>
                  <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('websiteBuilder.status') }}</label>
                  <label class="flex items-center cursor-pointer" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                    <input
                      v-model="offerForm.is_active"
                      type="checkbox"
                      class="w-5 h-5 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                    />
                    <span :class="['text-sm font-semibold text-gray-700', $i18n.locale === 'ar' ? 'mr-2' : 'ml-2']">{{ $t('websiteBuilder.active') }}</span>
                  </label>
                </div>
              </div>

              <!-- Description -->
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('websiteBuilder.description') }}</label>
                <textarea
                  v-model="offerForm.description"
                  rows="2"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  :placeholder="$t('restaurantDashboard.offerDescriptionPlaceholder')"
                ></textarea>
              </div>

              <div v-if="offerError" class="p-4 bg-red-50 border border-red-200 rounded-lg">
                <p class="text-sm text-red-600">{{ offerError }}</p>
              </div>
              <div v-if="offerSuccess" class="p-4 bg-green-50 border border-green-200 rounded-lg">
                <p class="text-sm text-green-600">{{ offerSuccess }}</p>
              </div>

              <div class="flex justify-end gap-3 pt-4 border-t border-gray-200" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <button
                  type="button"
                  @click="showOfferForm = false; editingOffer = null; resetOfferForm()"
                  class="px-6 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors font-semibold"
                >
                  {{ $t('websiteBuilder.cancel') }}
                </button>
                <button
                  type="submit"
                  :disabled="savingOffer"
                  class="px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <span v-if="savingOffer">{{ $t('websiteBuilder.saving') }}</span>
                  <span v-else>{{ $t('restaurantDashboard.save') }}</span>
                </button>
              </div>
            </form>
          </div>

          <!-- Offers List -->
          <div v-if="offers.length === 0 && !showOfferForm" class="text-center py-12">
            <p class="text-gray-500">{{ $t('restaurantDashboard.noOffers') }}</p>
          </div>
          <div v-else class="space-y-4">
            <div
              v-for="offer in offers"
              :key="offer.id"
              class="p-6 border-2 rounded-lg transition-colors"
              :class="offer.is_active ? 'border-amber-200 bg-amber-50' : 'border-gray-200 bg-gray-50'"
            >
              <div class="flex items-start justify-between" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <div class="flex-1">
                  <div class="flex items-center gap-3 mb-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                    <h3 class="text-xl font-bold text-gray-900">{{ offer.title }}</h3>
                    <span
                      class="px-3 py-1 rounded-full text-xs font-semibold"
                      :class="offer.is_active ? 'bg-amber-100 text-amber-800' : 'bg-gray-100 text-gray-800'"
                    >
                      {{ offer.is_active ? $t('websiteBuilder.active') : $t('websiteBuilder.inactive') }}
                    </span>
                    <span class="text-sm text-gray-500">{{ formatOfferType(offer.offer_type) }}</span>
                  </div>
                  <p v-if="offer.description" class="text-gray-600 mb-3">{{ offer.description }}</p>
                  <div class="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                    <div v-if="offer.value != null">
                      <span class="text-gray-500">{{ $t('restaurantDashboard.offerValue') }}:</span>
                      <span class="font-semibold text-gray-900 ml-2">
                        {{ offer.offer_type === 'percent_off' ? `${offer.value}%` : `${offer.value} JOD` }}
                      </span>
                    </div>
                    <div v-if="offer.min_order_value != null">
                      <span class="text-gray-500">{{ $t('restaurantDashboard.minOrderValue') }}:</span>
                      <span class="font-semibold text-gray-900 ml-2">{{ offer.min_order_value }} JOD</span>
                    </div>
                    <div>
                      <span class="text-gray-500">{{ $t('restaurantDashboard.validUntil') }}:</span>
                      <span class="font-semibold text-gray-900 ml-2">{{ new Date(offer.valid_until).toLocaleDateString() }}</span>
                    </div>
                  </div>
                </div>
                <div class="flex gap-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <button
                    @click="editOffer(offer)"
                    class="px-4 py-2 text-indigo-600 border border-indigo-300 rounded-lg hover:bg-indigo-50 transition-colors font-semibold text-sm"
                  >
                    {{ $t('websiteBuilder.edit') }}
                  </button>
                  <button
                    @click="deleteOffer(offer.id)"
                    class="px-4 py-2 text-red-600 border border-red-300 rounded-lg hover:bg-red-50 transition-colors font-semibold text-sm"
                  >
                    {{ $t('websiteBuilder.delete') }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Additional Tab -->
      <div v-show="activeTab === 'additional'" class="space-y-6">
        <div class="bg-white rounded-xl shadow-md p-8">
          <h2 class="text-2xl font-bold text-gray-900 mb-6">{{ $t('restaurantDashboard.additionalSettings') }}</h2>
          
          <form @submit.prevent="saveWebsite" class="space-y-6">
            <!-- App Download URL -->
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('websiteBuilder.appDownloadUrl') }}</label>
              <input
                v-model="websiteForm.app_download_url"
                type="url"
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                :placeholder="$t('websiteBuilder.appDownloadUrlPlaceholder')"
              />
            </div>

            <!-- Locations (JSON format) -->
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('websiteBuilder.locations') }}</label>
              <textarea
                v-model="locationsText"
                rows="4"
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 font-mono text-sm"
                :placeholder="$t('websiteBuilder.locationsPlaceholder')"
              ></textarea>
              <p class="text-xs text-gray-500 mt-1">{{ $t('websiteBuilder.locationsHint') }}</p>
            </div>

            <!-- Newsletter Toggle -->
            <div class="flex items-center gap-4">
              <label class="flex items-center cursor-pointer" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <input
                  v-model="websiteForm.newsletter_enabled"
                  type="checkbox"
                  class="w-5 h-5 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                />
                <span :class="['text-sm font-medium text-gray-700', $i18n.locale === 'ar' ? 'mr-2' : 'ml-2']">{{ $t('websiteBuilder.enableNewsletter') }}</span>
              </label>
            </div>

            <!-- Publish Toggle -->
            <div class="flex items-center gap-4">
              <label class="flex items-center cursor-pointer" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <input
                  v-model="websiteForm.is_published"
                  type="checkbox"
                  class="w-5 h-5 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                />
                <span :class="['text-sm font-medium text-gray-700', $i18n.locale === 'ar' ? 'mr-2' : 'ml-2']">{{ $t('websiteBuilder.publishWebsite') }}</span>
              </label>
            </div>

            <!-- Domain Configuration -->
            <div class="border-t border-gray-200 pt-6 mt-6">
              <h3 class="text-lg font-semibold text-gray-800 mb-4">{{ $t('websiteBuilder.domainConfiguration') }}</h3>
              <div class="bg-gradient-to-br from-blue-50 to-indigo-50 rounded-xl p-6 border border-blue-200 space-y-6">
                <!-- Subdomain -->
                <div>
                  <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('websiteBuilder.subdomain') }}</label>
                  <div class="flex items-center gap-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                    <input
                      v-model="websiteForm.subdomain"
                      type="text"
                      class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      :placeholder="$t('websiteBuilder.subdomainPlaceholder')"
                    />
                    <span class="text-gray-600 font-medium">.{{ baseDomain }}</span>
                  </div>
                </div>

                <!-- Custom Domain -->
                <div>
                  <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('websiteBuilder.customDomain') }}</label>
                  <input
                    v-model="websiteForm.custom_domain"
                    type="text"
                    class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                    :placeholder="$t('websiteBuilder.customDomainPlaceholder')"
                  />
                </div>
              </div>
            </div>

            <div v-if="saveError" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
              {{ saveError }}
            </div>

            <div v-if="saveSuccess" class="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg text-sm">
              {{ saveSuccess }}
            </div>

            <button
              type="submit"
              :disabled="saving"
              class="px-6 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <span v-if="saving">{{ $t('websiteBuilder.saving') }}</span>
              <span v-else>{{ $t('websiteBuilder.saveChanges') }}</span>
            </button>
          </form>
        </div>
      </div>

      <!-- Order Type Tab -->
      <div v-show="activeTab === 'orderType'" class="space-y-6">
        <div class="bg-white rounded-xl shadow-md p-8">
          <h2 class="text-2xl font-bold text-gray-900 mb-6">{{ $t('restaurantDashboard.orderTypeSettings') }}</h2>
          
          <form @submit.prevent="saveOrderTypeSettings" class="space-y-6">
            <!-- Dine-in -->
            <div class="flex items-center justify-between p-4 border-2 rounded-lg" :class="orderTypeSettings.dineInEnabled ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200'">
              <div class="flex items-center gap-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <label class="flex items-center cursor-pointer" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <input
                    v-model="orderTypeSettings.dineInEnabled"
                    type="checkbox"
                    class="w-5 h-5 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                  />
                  <span :class="['text-lg font-semibold text-gray-700', $i18n.locale === 'ar' ? 'mr-3' : 'ml-3']">
                    {{ $t('restaurantDashboard.dineIn') }}
                  </span>
                </label>
                <p class="text-sm text-gray-600">{{ $t('restaurantDashboard.dineInDescription') }}</p>
              </div>
            </div>

            <!-- Pickup -->
            <div class="flex items-center justify-between p-4 border-2 rounded-lg" :class="orderTypeSettings.pickupEnabled ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200'">
              <div class="flex items-center gap-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <label class="flex items-center cursor-pointer" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <input
                    v-model="orderTypeSettings.pickupEnabled"
                    type="checkbox"
                    class="w-5 h-5 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                  />
                  <span :class="['text-lg font-semibold text-gray-700', $i18n.locale === 'ar' ? 'mr-3' : 'ml-3']">
                    {{ $t('restaurantDashboard.pickup') }}
                  </span>
                </label>
                <p class="text-sm text-gray-600">{{ $t('restaurantDashboard.pickupDescription') }}</p>
              </div>
            </div>

            <!-- Delivery -->
            <div class="p-4 border-2 rounded-lg" :class="orderTypeSettings.deliveryEnabled ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200'">
              <div class="flex items-center justify-between mb-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <div class="flex items-center gap-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <label class="flex items-center cursor-pointer" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                    <input
                      v-model="orderTypeSettings.deliveryEnabled"
                      type="checkbox"
                      class="w-5 h-5 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                    />
                    <span :class="['text-lg font-semibold text-gray-700', $i18n.locale === 'ar' ? 'mr-3' : 'ml-3']">
                      {{ $t('restaurantDashboard.delivery') }}
                    </span>
                  </label>
                  <div class="flex flex-col" :class="$i18n.locale === 'ar' ? 'items-end' : 'items-start'">
                    <p class="text-sm text-gray-600">{{ $t('restaurantDashboard.deliveryDescription') }}</p>
                    <p class="text-xs text-gray-500 mt-1">{{ $t('restaurantDashboard.deliveryNote') }}</p>
                  </div>
                </div>
              </div>
              
              <!-- Delivery options (shown only when delivery is enabled) -->
              <div v-if="orderTypeSettings.deliveryEnabled" class="mt-4 pt-4 border-t border-gray-300">
                <!-- Delivery Time (always visible when delivery is enabled) -->
                <div class="mb-4">
                  <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.deliveryTimeLabel') }}</label>
                  <div class="flex items-center gap-2 flex-wrap" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                    <input v-model.number="orderTypeSettings.deliveryTimeMin" type="number" min="1" max="999" class="w-24 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500" :placeholder="15" />
                    <span class="text-gray-600">{{ $t('restaurantDashboard.deliveryTimeFrom') }}</span>
                    <input v-model.number="orderTypeSettings.deliveryTimeMax" type="number" min="1" max="999" class="w-24 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500" :placeholder="25" />
                    <span class="text-gray-600">{{ $t('restaurantDashboard.deliveryTimeTo') }}</span>
                  </div>
                  <p class="text-xs text-gray-500 mt-1">{{ $t('restaurantDashboard.deliveryTimeHint') }}</p>
                </div>

                <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.deliveryOption') }}</label>
                <div class="flex flex-col gap-3 mb-4" :class="$i18n.locale === 'ar' ? 'items-end' : 'items-start'">
                  <!-- Fixed Fee Radio with Input -->
                  <div class="flex flex-col gap-2 w-full" :class="$i18n.locale === 'ar' ? 'items-end' : 'items-start'">
                    <label class="flex items-center cursor-pointer">
                      <input v-model="orderTypeSettings.deliveryMode" type="radio" value="fixed_fee" class="w-4 h-4 text-indigo-600 border-gray-300" />
                      <span class="ml-2 text-gray-700">{{ $t('restaurantDashboard.fixDeliveryFee') }}</span>
                    </label>
                    <!-- Fixed Fee Input (shown when fixed_fee is selected) -->
                    <div v-if="orderTypeSettings.deliveryMode === 'fixed_fee'" class="ml-6" :class="$i18n.locale === 'ar' ? 'mr-6 ml-0' : ''">
                      <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.fixDeliveryFeeLabel') }}</label>
                      <div class="flex items-center gap-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                        <span class="text-gray-600 font-medium">$</span>
                        <input
                          v-model="orderTypeSettings.deliveryFee"
                          type="number"
                          step="0.01"
                          min="0"
                          class="w-32 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                          :placeholder="$t('restaurantDashboard.deliveryFeePlaceholder')"
                        />
                      </div>
                      <p class="text-xs text-gray-500 mt-1">{{ $t('restaurantDashboard.deliveryFeeHint') }}</p>
                    </div>
                  </div>
                  <!-- Delivery Company Radio -->
                  <label class="flex items-center cursor-pointer">
                    <input v-model="orderTypeSettings.deliveryMode" type="radio" value="delivery_company" class="w-4 h-4 text-indigo-600 border-gray-300" />
                    <span class="ml-2 text-gray-700">{{ $t('restaurantDashboard.deliveryCompany') }}</span>
                  </label>
                </div>

                <!-- Delivery Company: select + request + status -->
                <div v-if="orderTypeSettings.deliveryMode === 'delivery_company'" class="space-y-4 mt-4">
                  <div v-if="loadingStoreRequests" class="text-center py-6 text-gray-500">
                    <span class="inline-block animate-spin rounded-full h-5 w-5 border-2 border-indigo-500 border-t-transparent mr-2 align-middle"></span>
                    {{ $t('restaurantDashboard.loadingRequests') || 'Loading requests...' }}
                  </div>
                  <template v-else-if="deliveryCompanyRequest.approvedDeliveryCompany">
                    <div class="p-4 bg-green-50 border border-green-200 rounded-lg flex items-center justify-between" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                      <div>
                        <p class="text-sm font-semibold text-green-800">{{ deliveryCompanyRequest.approvedDeliveryCompany.company_name }}</p>
                        <span class="inline-block mt-1 px-2 py-0.5 text-xs font-medium bg-green-200 text-green-900 rounded">{{ $t('restaurantDashboard.approved') }}</span>
                      </div>
                      <button
                        type="button"
                        @click="resetDeliveryCompany"
                        :disabled="resettingDeliveryCompany"
                        class="px-3 py-1.5 bg-red-600 text-white text-sm rounded-lg hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
                      >
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                        </svg>
                        {{ resettingDeliveryCompany ? $t('restaurantDashboard.removing') || 'Removing...' : $t('websiteBuilder.remove') || 'Remove' }}
                      </button>
                    </div>
                  </template>
                  <template v-else>
                    <div>
                      <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.selectDeliveryCompany') }}</label>
                      <select
                        v-model="selectedDeliveryCompanyId"
                        class="w-full max-w-md px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                        :disabled="!!deliveryCompanyRequest.pendingRequest"
                      >
                        <option value="">{{ $t('restaurantDashboard.chooseCompany') }}</option>
                        <option v-for="c in deliveryCompaniesList" :key="c.id" :value="c.id">{{ c.company_name }}</option>
                      </select>
                    </div>
                    <div v-if="deliveryCompanyRequest.pendingRequest" class="p-4 bg-yellow-50 border border-yellow-200 rounded-lg flex items-center justify-between" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                      <p class="text-sm text-yellow-800">{{ $t('restaurantDashboard.requestPending') }}</p>
                      <button
                        type="button"
                        @click="resetDeliveryCompany"
                        :disabled="resettingDeliveryCompany"
                        class="px-3 py-1.5 bg-red-600 text-white text-sm rounded-lg hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
                      >
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                        </svg>
                        {{ resettingDeliveryCompany ? $t('restaurantDashboard.removing') || 'Removing...' : $t('websiteBuilder.remove') || 'Remove' }}
                      </button>
                    </div>
                    <div v-else-if="deliveryCompanyRequest.requests?.some(r => r.status === 'rejected')" class="p-4 bg-gray-50 border border-gray-200 rounded-lg">
                      <p class="text-sm text-gray-600">{{ $t('restaurantDashboard.requestRejectedHint') }}</p>
                    </div>
                    <button
                      type="button"
                      :disabled="!selectedDeliveryCompanyId || sendingDeliveryCompanyRequest"
                      @click="sendDeliveryCompanyRequest"
                      class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed text-sm font-medium"
                    >
                      {{ sendingDeliveryCompanyRequest ? $t('restaurantDashboard.sending') : $t('restaurantDashboard.sendRequest') }}
                    </button>
                  </template>
                </div>
              </div>
            </div>

            <!-- Error Message -->
            <div v-if="orderTypeError" class="p-4 bg-red-50 border border-red-200 rounded-lg">
              <p class="text-sm text-red-600">{{ orderTypeError }}</p>
            </div>

            <!-- Success Message -->
            <div v-if="orderTypeSuccess" class="p-4 bg-green-50 border border-green-200 rounded-lg">
              <p class="text-sm text-green-600">{{ orderTypeSuccess }}</p>
            </div>

            <!-- Save Button -->
            <div class="flex justify-end pt-4 border-t border-gray-200">
              <button
                type="submit"
                :disabled="savingOrderTypes"
                class="px-6 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
              >
                <span v-if="savingOrderTypes">{{ $t('websiteBuilder.saving') }}</span>
                <span v-else>{{ $t('websiteBuilder.saveChanges') }}</span>
              </button>
            </div>
          </form>
        </div>
      </div>

      <!-- Branches Tab -->
      <div v-show="activeTab === 'branches'" class="space-y-6">
        <div class="bg-white rounded-xl shadow-md p-8">
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-2xl font-bold text-gray-900">Branches</h2>
            <button
              @click="openBranchForm()"
              class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors flex items-center gap-2"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
              Add Branch
            </button>
          </div>

          <!-- Branches List -->
          <div v-if="loadingBranches" class="text-center py-12 text-gray-600">Loading branches...</div>
          <div v-else-if="branches.length === 0" class="text-center py-12 text-gray-500">
            <div class="text-4xl mb-4">🏢</div>
            <p class="font-semibold">No branches found.</p>
            <p class="text-sm mt-2">Add branches to manage multiple locations.</p>
          </div>
          <div v-else class="space-y-4">
            <div
              v-for="branch in branches"
              :key="branch.id"
              class="border border-gray-200 rounded-lg p-6 hover:shadow-md transition-shadow"
            >
              <div class="flex items-start justify-between">
                <div class="flex-1">
                  <div class="flex items-center gap-3 mb-2">
                    <span class="px-3 py-1 bg-indigo-100 text-indigo-800 rounded-full text-sm font-semibold">
                      Branch #{{ branch.branch_number }}
                    </span>
                    <span :class="[
                      'px-2 py-1 text-xs font-semibold rounded-full',
                      branch.status === 'active' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                    ]">
                      {{ branch.status }}
                    </span>
                  </div>
                  <h3 class="text-lg font-semibold text-gray-900 mb-1">
                    {{ branch.name_ar || branch.name || `Branch ${branch.branch_number}` }}
                  </h3>
                  <p v-if="branch.name && branch.name_ar" class="text-sm text-gray-600 mb-2">
                    {{ branch.name }}
                  </p>
                  <div class="space-y-1 text-sm text-gray-600">
                    <p>
                      <span class="font-semibold">Region:</span>
                      {{ branch.region_name_ar || branch.region_name }}
                      <span v-if="branch.region_name && branch.region_name_ar" class="text-gray-500">
                        ({{ branch.region_name }})
                      </span>
                    </p>
                    <p v-if="branch.city_name">
                      <span class="font-semibold">City:</span>
                      {{ branch.city_name_ar || branch.city_name }}
                    </p>
                    <p v-if="branch.address">
                      <span class="font-semibold">Address:</span> {{ branch.address }}
                    </p>
                    <p v-if="branch.phone">
                      <span class="font-semibold">Phone:</span> {{ branch.phone }}
                    </p>
                  </div>
                </div>
                <div class="flex gap-2 ml-4">
                  <button
                    @click="editBranch(branch)"
                    class="px-3 py-1 text-sm bg-blue-100 text-blue-700 rounded hover:bg-blue-200 transition-colors"
                  >
                    Edit
                  </button>
                  <button
                    @click="deleteBranch(branch.id)"
                    class="px-3 py-1 text-sm bg-red-100 text-red-700 rounded hover:bg-red-200 transition-colors"
                  >
                    Delete
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Tax Requirement Tab -->
      <div v-show="activeTab === 'taxRequirement'" class="space-y-6">
        <div class="bg-white rounded-xl shadow-md p-8">
          <h2 class="text-2xl font-bold text-gray-900 mb-6">{{ $t('restaurantDashboard.taxRequirement') }}</h2>
          
          <form @submit.prevent="saveTaxRequirement" class="space-y-6">
            <!-- Tax Enabled Toggle -->
            <div class="flex items-center gap-4">
              <label class="flex items-center cursor-pointer" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <input
                  v-model="taxRequirement.enabled"
                  type="checkbox"
                  class="w-5 h-5 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                />
                <span :class="['text-sm font-semibold text-gray-700', $i18n.locale === 'ar' ? 'mr-2' : 'ml-2']">
                  {{ $t('restaurantDashboard.enableTax') }}
                </span>
              </label>
            </div>

            <!-- Tax Rate -->
            <div v-if="taxRequirement.enabled">
              <label class="block text-sm font-semibold text-gray-700 mb-2">
                {{ $t('restaurantDashboard.taxRate') }} (%)
              </label>
              <div class="flex items-center gap-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <input
                  v-model="taxRequirement.rate"
                  type="number"
                  step="0.01"
                  min="0"
                  max="100"
                  class="w-32 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  :placeholder="$t('restaurantDashboard.taxRatePlaceholder')"
                />
                <span class="text-gray-600">%</span>
              </div>
              <p class="text-xs text-gray-500 mt-1">{{ $t('restaurantDashboard.taxRateHint') }}</p>
            </div>

            <!-- Error Message -->
            <div v-if="taxError" class="p-4 bg-red-50 border border-red-200 rounded-lg">
              <p class="text-sm text-red-600">{{ taxError }}</p>
            </div>

            <!-- Success Message -->
            <div v-if="taxSuccess" class="p-4 bg-green-50 border border-green-200 rounded-lg">
              <p class="text-sm text-green-600">{{ taxSuccess }}</p>
            </div>

            <!-- Save Button -->
            <div class="flex justify-end pt-4 border-t border-gray-200">
              <button
                type="submit"
                :disabled="savingTax"
                class="px-6 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
              >
                <span v-if="savingTax">{{ $t('websiteBuilder.saving') }}</span>
                <span v-else>{{ $t('websiteBuilder.saveChanges') }}</span>
              </button>
            </div>
          </form>
        </div>
      </div>

      <!-- Payment Methods Tab -->
      <div v-show="activeTab === 'paymentMethods'" class="space-y-6">
        <div class="bg-white rounded-xl shadow-md p-8">
          <h2 class="text-2xl font-bold text-gray-900 mb-6">{{ $t('restaurantDashboard.paymentMethods') }}</h2>
          
          <form @submit.prevent="savePaymentMethods" class="space-y-6">
            <!-- Cash on Pickup -->
            <div class="flex items-center justify-between p-4 border-2 rounded-lg" :class="paymentMethods.cashOnPickup ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200'">
              <div class="flex items-center gap-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <label class="flex items-center cursor-pointer" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <input
                    v-model="paymentMethods.cashOnPickup"
                    type="checkbox"
                    class="w-5 h-5 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                  />
                  <span :class="['text-lg font-semibold text-gray-700', $i18n.locale === 'ar' ? 'mr-3' : 'ml-3']">
                    {{ $t('restaurantDashboard.cashOnPickup') }}
                  </span>
                </label>
                <p class="text-sm text-gray-600">{{ $t('restaurantDashboard.cashOnPickupDescription') }}</p>
              </div>
            </div>

            <!-- Cash on Delivery -->
            <div class="flex items-center justify-between p-4 border-2 rounded-lg" :class="paymentMethods.cashOnDelivery ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200'">
              <div class="flex items-center gap-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <label class="flex items-center cursor-pointer" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <input
                    v-model="paymentMethods.cashOnDelivery"
                    type="checkbox"
                    class="w-5 h-5 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                  />
                  <span :class="['text-lg font-semibold text-gray-700', $i18n.locale === 'ar' ? 'mr-3' : 'ml-3']">
                    {{ $t('restaurantDashboard.cashOnDelivery') }}
                  </span>
                </label>
                <p class="text-sm text-gray-600">{{ $t('restaurantDashboard.cashOnDeliveryDescription') }}</p>
              </div>
            </div>

            <!-- Credit/Debit Card -->
            <div class="flex items-center justify-between p-4 border-2 rounded-lg" :class="paymentMethods.creditCard ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200'">
              <div class="flex items-center gap-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <label class="flex items-center cursor-pointer" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <input
                    v-model="paymentMethods.creditCard"
                    type="checkbox"
                    class="w-5 h-5 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                  />
                  <span :class="['text-lg font-semibold text-gray-700', $i18n.locale === 'ar' ? 'mr-3' : 'ml-3']">
                    {{ $t('restaurantDashboard.creditCard') }}
                  </span>
                </label>
                <p class="text-sm text-gray-600">{{ $t('restaurantDashboard.creditCardDescription') }}</p>
              </div>
            </div>

            <!-- Online Payment (Stripe/PayPal) -->
            <div class="flex items-center justify-between p-4 border-2 rounded-lg" :class="paymentMethods.onlinePayment ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200'">
              <div class="flex items-center gap-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <label class="flex items-center cursor-pointer" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <input
                    v-model="paymentMethods.onlinePayment"
                    type="checkbox"
                    class="w-5 h-5 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                  />
                  <span :class="['text-lg font-semibold text-gray-700', $i18n.locale === 'ar' ? 'mr-3' : 'ml-3']">
                    {{ $t('restaurantDashboard.onlinePayment') }}
                  </span>
                </label>
                <p class="text-sm text-gray-600">{{ $t('restaurantDashboard.onlinePaymentDescription') }}</p>
              </div>
            </div>

            <!-- Mobile Payment (Apple Pay, Google Pay) -->
            <div class="flex items-center justify-between p-4 border-2 rounded-lg" :class="paymentMethods.mobilePayment ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200'">
              <div class="flex items-center gap-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <label class="flex items-center cursor-pointer" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <input
                    v-model="paymentMethods.mobilePayment"
                    type="checkbox"
                    class="w-5 h-5 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                  />
                  <span :class="['text-lg font-semibold text-gray-700', $i18n.locale === 'ar' ? 'mr-3' : 'ml-3']">
                    {{ $t('restaurantDashboard.mobilePayment') }}
                  </span>
                </label>
                <p class="text-sm text-gray-600">{{ $t('restaurantDashboard.mobilePaymentDescription') }}</p>
              </div>
            </div>

            <!-- CliQ Services -->
            <div class="p-4 border-2 rounded-lg" :class="paymentMethods.cliQServices?.enabled ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200'">
              <!-- First checkbox: CliQ Services -->
              <div class="flex items-center justify-between mb-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <div class="flex items-center gap-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <label class="flex items-center cursor-pointer" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                    <input
                      v-model="paymentMethods.cliQServices.enabled"
                      type="checkbox"
                      class="w-5 h-5 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                      @change="onCliQEnabledChange($event)"
                    />
                    <span :class="['text-lg font-semibold text-gray-700', $i18n.locale === 'ar' ? 'mr-3' : 'ml-3']">
                      {{ $t('restaurantDashboard.cliQServices') }}
                    </span>
                  </label>
                  <p class="text-sm text-gray-600">{{ $t('restaurantDashboard.cliQServicesDescription') }}</p>
                </div>
              </div>
              <!-- Second checkbox: Allow entering CliQ Name and Phone (shown when CliQ Services is enabled) -->
              <div v-if="paymentMethods.cliQServices?.enabled" class="mt-4 pl-9" :class="$i18n.locale === 'ar' ? 'pr-9 pl-0' : ''">
                <label class="flex items-center cursor-pointer" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <input
                    v-model="paymentMethods.cliQServices.allowNamePhone"
                    type="checkbox"
                    class="w-5 h-5 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                    @change="onCliQAllowNamePhoneChange($event)"
                  />
                  <span :class="['text-base font-medium text-gray-700', $i18n.locale === 'ar' ? 'mr-3' : 'ml-3']">
                    {{ $t('restaurantDashboard.cliQAllowNamePhone') }}
                  </span>
                </label>
                <!-- CliQ Name and Phone inputs (shown when second checkbox is enabled) -->
                <div v-if="paymentMethods.cliQServices?.allowNamePhone" class="mt-4 space-y-4">
                  <div>
                    <label class="block text-sm font-medium text-gray-700 mb-2">
                      {{ $t('restaurantDashboard.cliQServicesName') }}
                    </label>
                    <input
                      v-model="paymentMethods.cliQServices.name"
                      type="text"
                      :placeholder="$t('restaurantDashboard.cliQServicesNamePlaceholder')"
                      class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                    />
                  </div>
                  <div>
                    <label class="block text-sm font-medium text-gray-700 mb-2">
                      {{ $t('restaurantDashboard.cliQServicesPhone') }}
                    </label>
                    <input
                      v-model="paymentMethods.cliQServices.phone"
                      type="text"
                      :placeholder="$t('restaurantDashboard.cliQServicesPhonePlaceholder')"
                      class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                    />
                  </div>
                </div>
              </div>
            </div>

            <!-- Error Message -->
            <div v-if="paymentMethodsError" class="p-4 bg-red-50 border border-red-200 rounded-lg">
              <p class="text-sm text-red-600">{{ paymentMethodsError }}</p>
            </div>

            <!-- Success Message -->
            <div v-if="paymentMethodsSuccess" class="p-4 bg-green-50 border border-green-200 rounded-lg">
              <p class="text-sm text-green-600">{{ paymentMethodsSuccess }}</p>
            </div>

            <!-- Save Button -->
            <div class="flex justify-end pt-4 border-t border-gray-200">
              <button
                type="submit"
                :disabled="savingPaymentMethods"
                class="px-6 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
              >
                <span v-if="savingPaymentMethods">{{ $t('websiteBuilder.saving') }}</span>
                <span v-else>{{ $t('websiteBuilder.saveChanges') }}</span>
              </button>
            </div>
          </form>
        </div>
      </div>

      <!-- Coupons Tab -->
      <div v-show="activeTab === 'coupons'" class="space-y-6">
        <div class="bg-white rounded-xl shadow-md p-8">
          <div class="flex items-center justify-between mb-6" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
            <h2 class="text-2xl font-bold text-gray-900">{{ $t('restaurantDashboard.coupons') }}</h2>
            <button
              @click="showCouponForm = true; editingCoupon = null; resetCouponForm()"
              class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold flex items-center gap-2"
            >
              <span>+</span>
              <span>{{ $t('restaurantDashboard.addCoupon') }}</span>
            </button>
          </div>

          <!-- Coupon Form (Modal/Inline) -->
          <div v-if="showCouponForm" class="mb-6 p-6 border-2 border-indigo-200 rounded-lg bg-indigo-50">
            <h3 class="text-xl font-bold text-gray-900 mb-4">
              {{ editingCoupon ? $t('restaurantDashboard.editCoupon') : $t('restaurantDashboard.addCoupon') }}
            </h3>
            
            <form @submit.prevent="saveCoupon" class="space-y-4">
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <!-- Coupon Code -->
                <div>
                  <label class="block text-sm font-semibold text-gray-700 mb-2">
                    {{ $t('restaurantDashboard.couponCode') }} *
                  </label>
                  <input
                    v-model="couponForm.code"
                    type="text"
                    required
                    class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 uppercase"
                    :placeholder="$t('restaurantDashboard.couponCodePlaceholder')"
                  />
                </div>

                <!-- Discount Type -->
                <div>
                  <label class="block text-sm font-semibold text-gray-700 mb-2">
                    {{ $t('restaurantDashboard.discountType') }} *
                  </label>
                  <select
                    v-model="couponForm.discount_type"
                    required
                    class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  >
                    <option value="percentage">{{ $t('restaurantDashboard.percentage') }}</option>
                    <option value="fixed">{{ $t('restaurantDashboard.fixedAmount') }}</option>
                  </select>
                </div>

                <!-- Discount Value -->
                <div>
                  <label class="block text-sm font-semibold text-gray-700 mb-2">
                    {{ $t('restaurantDashboard.discountValue') }} *
                  </label>
                  <div class="flex items-center gap-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                    <input
                      v-model="couponForm.discount_value"
                      type="number"
                      step="0.01"
                      min="0"
                      required
                      class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      :placeholder="couponForm.discount_type === 'percentage' ? '10' : '5.00'"
                    />
                    <span class="text-gray-600">
                      {{ couponForm.discount_type === 'percentage' ? '%' : '$' }}
                    </span>
                  </div>
                </div>

                <!-- Min Order Amount -->
                <div>
                  <label class="block text-sm font-semibold text-gray-700 mb-2">
                    {{ $t('restaurantDashboard.minOrderAmount') }}
                  </label>
                  <div class="flex items-center gap-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                    <input
                      v-model="couponForm.min_order_amount"
                      type="number"
                      step="0.01"
                      min="0"
                      class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      placeholder="0.00"
                    />
                    <span class="text-gray-600">$</span>
                  </div>
                </div>

                <!-- Max Discount Amount (for percentage) -->
                <div v-if="couponForm.discount_type === 'percentage'">
                  <label class="block text-sm font-semibold text-gray-700 mb-2">
                    {{ $t('restaurantDashboard.maxDiscountAmount') }}
                  </label>
                  <div class="flex items-center gap-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                    <input
                      v-model="couponForm.max_discount_amount"
                      type="number"
                      step="0.01"
                      min="0"
                      class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      placeholder="50.00"
                    />
                    <span class="text-gray-600">$</span>
                  </div>
                </div>

                <!-- Valid From -->
                <div>
                  <label class="block text-sm font-semibold text-gray-700 mb-2">
                    {{ $t('restaurantDashboard.validFrom') }} *
                  </label>
                  <input
                    v-model="couponForm.valid_from"
                    type="date"
                    required
                    class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  />
                </div>

                <!-- Valid Until -->
                <div>
                  <label class="block text-sm font-semibold text-gray-700 mb-2">
                    {{ $t('restaurantDashboard.validUntil') }} *
                  </label>
                  <input
                    v-model="couponForm.valid_until"
                    type="date"
                    required
                    class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  />
                </div>

                <!-- Usage Limit -->
                <div>
                  <label class="block text-sm font-semibold text-gray-700 mb-2">
                    {{ $t('restaurantDashboard.usageLimit') }}
                  </label>
                  <input
                    v-model="couponForm.usage_limit"
                    type="number"
                    min="1"
                    class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                    :placeholder="$t('restaurantDashboard.unlimited')"
                  />
                  <p class="text-xs text-gray-500 mt-1">{{ $t('restaurantDashboard.usageLimitHint') }}</p>
                </div>

                <!-- Active Status -->
                <div>
                  <label class="block text-sm font-semibold text-gray-700 mb-2">
                    {{ $t('websiteBuilder.status') }}
                  </label>
                  <div class="flex items-center gap-4 mt-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                    <label class="flex items-center cursor-pointer" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                      <input
                        v-model="couponForm.is_active"
                        type="checkbox"
                        class="w-5 h-5 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                      />
                      <span :class="['text-sm font-semibold text-gray-700', $i18n.locale === 'ar' ? 'mr-2' : 'ml-2']">
                        {{ $t('websiteBuilder.active') }}
                      </span>
                    </label>
                  </div>
                </div>
              </div>

              <!-- Description -->
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">
                  {{ $t('websiteBuilder.description') }}
                </label>
                <textarea
                  v-model="couponForm.description"
                  rows="3"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  :placeholder="$t('restaurantDashboard.couponDescriptionPlaceholder')"
                ></textarea>
              </div>

              <!-- Error Message -->
              <div v-if="couponError" class="p-4 bg-red-50 border border-red-200 rounded-lg">
                <p class="text-sm text-red-600">{{ couponError }}</p>
              </div>

              <!-- Success Message -->
              <div v-if="couponSuccess" class="p-4 bg-green-50 border border-green-200 rounded-lg">
                <p class="text-sm text-green-600">{{ couponSuccess }}</p>
              </div>

              <!-- Form Actions -->
              <div class="flex justify-end gap-3 pt-4 border-t border-gray-200" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <button
                  type="button"
                  @click="showCouponForm = false; editingCoupon = null; resetCouponForm()"
                  class="px-6 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors font-semibold"
                >
                  {{ $t('websiteBuilder.cancel') }}
                </button>
                <button
                  type="submit"
                  :disabled="savingCoupon"
                  class="px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <span v-if="savingCoupon">{{ $t('websiteBuilder.saving') }}</span>
                  <span v-else>{{ $t('restaurantDashboard.save') }}</span>
                </button>
              </div>
            </form>
          </div>

          <!-- Coupons List -->
          <div v-if="coupons.length === 0 && !showCouponForm" class="text-center py-12">
            <p class="text-gray-500">{{ $t('restaurantDashboard.noCoupons') }}</p>
          </div>

          <div v-else class="space-y-4">
            <div
              v-for="coupon in coupons"
              :key="coupon.id"
              class="p-6 border-2 rounded-lg transition-colors"
              :class="coupon.is_active ? 'border-green-200 bg-green-50' : 'border-gray-200 bg-gray-50'"
            >
              <div class="flex items-start justify-between" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <div class="flex-1">
                  <div class="flex items-center gap-3 mb-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                    <h3 class="text-xl font-bold text-gray-900">{{ coupon.code }}</h3>
                    <span
                      class="px-3 py-1 rounded-full text-xs font-semibold"
                      :class="coupon.is_active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'"
                    >
                      {{ coupon.is_active ? $t('websiteBuilder.active') : $t('websiteBuilder.inactive') }}
                    </span>
                  </div>
                  
                  <p v-if="coupon.description" class="text-gray-600 mb-3">{{ coupon.description }}</p>
                  
                  <div class="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                    <div>
                      <span class="text-gray-500">{{ $t('restaurantDashboard.discount') }}:</span>
                      <span class="font-semibold text-gray-900 ml-2">
                        {{ coupon.discount_type === 'percentage' 
                          ? `${coupon.discount_value}%` 
                          : `$${parseFloat(coupon.discount_value).toFixed(2)}` }}
                      </span>
                    </div>
                    <div>
                      <span class="text-gray-500">{{ $t('restaurantDashboard.minOrder') }}:</span>
                      <span class="font-semibold text-gray-900 ml-2">
                        ${{ parseFloat(coupon.min_order_amount || 0).toFixed(2) }}
                      </span>
                    </div>
                    <div>
                      <span class="text-gray-500">{{ $t('restaurantDashboard.validUntil') }}:</span>
                      <span class="font-semibold text-gray-900 ml-2">
                        {{ new Date(coupon.valid_until).toLocaleDateString() }}
                      </span>
                    </div>
                    <div>
                      <span class="text-gray-500">{{ $t('restaurantDashboard.usage') }}:</span>
                      <span class="font-semibold text-gray-900 ml-2">
                        {{ coupon.usage_count }} / {{ coupon.usage_limit || $t('restaurantDashboard.unlimited') }}
                      </span>
                    </div>
                  </div>
                </div>
                
                <div class="flex gap-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <button
                    @click="editCoupon(coupon)"
                    class="px-4 py-2 text-indigo-600 border border-indigo-300 rounded-lg hover:bg-indigo-50 transition-colors font-semibold text-sm"
                  >
                    {{ $t('websiteBuilder.edit') }}
                  </button>
                  <button
                    @click="deleteCoupon(coupon.id)"
                    class="px-4 py-2 text-red-600 border border-red-300 rounded-lg hover:bg-red-50 transition-colors font-semibold text-sm"
                  >
                    {{ $t('websiteBuilder.delete') }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Notifications Tab -->
      <div v-show="activeTab === 'notifications'" class="space-y-6">
        <div class="bg-white rounded-xl shadow-md p-8">
          <h2 class="text-2xl font-bold text-gray-900 mb-6">{{ $t('restaurantDashboard.notificationSettings') }}</h2>
          
          <form @submit.prevent="saveNotificationSettings" class="space-y-6">
            <!-- Enable/Disable Notifications -->
            <div class="flex items-center gap-4">
              <label class="flex items-center cursor-pointer" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <input
                  v-model="notificationSettings.notificationsEnabled"
                  type="checkbox"
                  class="w-5 h-5 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                />
                <span :class="['text-sm font-semibold text-gray-700', $i18n.locale === 'ar' ? 'mr-2' : 'ml-2']">
                  {{ $t('restaurantDashboard.enableNotifications') }}
                </span>
              </label>
            </div>

            <div v-if="notificationSettings.notificationsEnabled" class="bg-gray-50 rounded-lg p-6 space-y-6">
              <!-- Email Notifications -->
              <div>
                <div class="flex items-center justify-between mb-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <div>
                    <h4 class="text-md font-semibold text-gray-800">{{ $t('restaurantDashboard.emailNotifications') }}</h4>
                    <p class="text-sm text-gray-600 mt-1">{{ $t('restaurantDashboard.emailNotificationsDesc') }}</p>
                  </div>
                  <label class="relative inline-flex items-center cursor-pointer">
                    <input
                      v-model="notificationSettings.emailEnabled"
                      type="checkbox"
                      class="sr-only peer"
                    />
                    <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-indigo-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-indigo-600"></div>
                  </label>
                </div>
                <div v-if="notificationSettings.emailEnabled" class="mt-4">
                  <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.notificationEmail') }}</label>
                  <input
                    v-model="notificationSettings.notificationEmail"
                    type="email"
                    :placeholder="$t('restaurantDashboard.notificationEmailPlaceholder')"
                    class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  />
                  <p class="text-xs text-gray-500 mt-1">{{ $t('restaurantDashboard.notificationEmailHint') }}</p>
                </div>
              </div>

              <!-- SMS Notifications (Disabled) -->
              <div class="opacity-50">
                <div class="flex items-center justify-between mb-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <div>
                    <h4 class="text-md font-semibold text-gray-800">{{ $t('restaurantDashboard.smsNotifications') }}</h4>
                    <p class="text-sm text-gray-600 mt-1">{{ $t('restaurantDashboard.smsNotificationsDesc') }}</p>
                  </div>
                  <label class="relative inline-flex items-center cursor-not-allowed">
                    <input
                      v-model="notificationSettings.smsEnabled"
                      type="checkbox"
                      disabled
                      class="sr-only peer"
                    />
                    <div class="w-11 h-6 bg-gray-200 rounded-full peer peer-checked:after:translate-x-full after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-indigo-600"></div>
                  </label>
                </div>
                <p class="text-xs text-gray-500 italic">{{ $t('restaurantDashboard.comingSoon') }}</p>
              </div>

              <!-- Push Notifications -->
              <div>
                <div class="flex items-center justify-between mb-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <div>
                    <h4 class="text-md font-semibold text-gray-800">{{ $t('restaurantDashboard.pushNotifications') }}</h4>
                    <p class="text-sm text-gray-600 mt-1">{{ $t('restaurantDashboard.pushNotificationsDesc') }}</p>
                  </div>
                  <label class="relative inline-flex items-center cursor-pointer">
                    <input
                      v-model="notificationSettings.pushEnabled"
                      type="checkbox"
                      class="sr-only peer"
                    />
                    <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-indigo-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-indigo-600"></div>
                  </label>
                </div>
              </div>

              <!-- WhatsApp Notifications (Disabled) -->
              <div class="opacity-50">
                <div class="flex items-center justify-between mb-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <div>
                    <h4 class="text-md font-semibold text-gray-800">{{ $t('restaurantDashboard.whatsappNotifications') }}</h4>
                    <p class="text-sm text-gray-600 mt-1">{{ $t('restaurantDashboard.whatsappNotificationsDesc') }}</p>
                  </div>
                  <label class="relative inline-flex items-center cursor-not-allowed">
                    <input
                      v-model="notificationSettings.whatsappEnabled"
                      type="checkbox"
                      disabled
                      class="sr-only peer"
                    />
                    <div class="w-11 h-6 bg-gray-200 rounded-full peer peer-checked:after:translate-x-full after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-indigo-600"></div>
                  </label>
                </div>
                <p class="text-xs text-gray-500 italic">{{ $t('restaurantDashboard.comingSoon') }}</p>
              </div>
            </div>

            <div v-if="notificationError" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
              {{ notificationError }}
            </div>
            <div v-if="notificationSuccess" class="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg text-sm">
              {{ notificationSuccess }}
            </div>

            <button
              type="submit"
              :disabled="savingNotifications"
              class="px-6 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <span v-if="savingNotifications">{{ $t('websiteBuilder.saving') }}</span>
              <span v-else>{{ $t('restaurantDashboard.saveNotificationSettings') }}</span>
            </button>
          </form>
        </div>
      </div>

      <!-- Language Tab -->
      <div v-show="activeTab === 'language'" class="space-y-6">
        <div class="bg-white rounded-xl shadow-md p-8">
          <h2 class="text-2xl font-bold text-gray-900 mb-6">{{ $t('restaurantDashboard.languageSettings') }}</h2>
          
          <form @submit.prevent="saveLanguageSettings" class="space-y-6">
            <!-- Default Language -->
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-2">
                {{ $t('restaurantDashboard.defaultLanguage') }} *
              </label>
              <select
                v-model="languageSettings.default_language"
                required
                class="w-full md:w-64 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              >
                <option value="en" :disabled="!languageSettings.languages_enabled.english">
                  {{ $t('restaurantDashboard.english') }}
                </option>
                <option value="ar" :disabled="!languageSettings.languages_enabled.arabic">
                  {{ $t('restaurantDashboard.arabic') }}
                </option>
              </select>
              <p class="text-xs text-gray-500 mt-1">{{ $t('restaurantDashboard.defaultLanguageHint') }}</p>
            </div>

            <!-- Available Languages -->
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-4">
                {{ $t('restaurantDashboard.availableLanguages') }}
              </label>
              
              <div class="space-y-4">
                <!-- English -->
                <div class="flex items-center justify-between p-4 border-2 rounded-lg" :class="languageSettings.languages_enabled.english ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200'">
                  <div class="flex items-center gap-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                    <label class="flex items-center cursor-pointer" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                      <input
                        v-model="languageSettings.languages_enabled.english"
                        type="checkbox"
                        class="w-5 h-5 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                        @change="handleLanguageToggle('english')"
                      />
                      <span :class="['text-lg font-semibold text-gray-700', $i18n.locale === 'ar' ? 'mr-3' : 'ml-3']">
                        {{ $t('restaurantDashboard.english') }}
                      </span>
                    </label>
                    <p class="text-sm text-gray-600">{{ $t('restaurantDashboard.englishDescription') }}</p>
                  </div>
                </div>

                <!-- Arabic -->
                <div class="flex items-center justify-between p-4 border-2 rounded-lg" :class="languageSettings.languages_enabled.arabic ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200'">
                  <div class="flex items-center gap-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                    <label class="flex items-center cursor-pointer" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                      <input
                        v-model="languageSettings.languages_enabled.arabic"
                        type="checkbox"
                        class="w-5 h-5 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                        @change="handleLanguageToggle('arabic')"
                      />
                      <span :class="['text-lg font-semibold text-gray-700', $i18n.locale === 'ar' ? 'mr-3' : 'ml-3']">
                        {{ $t('restaurantDashboard.arabic') }}
                      </span>
                    </label>
                    <p class="text-sm text-gray-600">{{ $t('restaurantDashboard.arabicDescription') }}</p>
                  </div>
                </div>
              </div>
            </div>

            <!-- Error Message -->
            <div v-if="languageError" class="p-4 bg-red-50 border border-red-200 rounded-lg">
              <p class="text-sm text-red-600">{{ languageError }}</p>
            </div>

            <!-- Success Message -->
            <div v-if="languageSuccess" class="p-4 bg-green-50 border border-green-200 rounded-lg">
              <p class="text-sm text-green-600">{{ languageSuccess }}</p>
            </div>

            <!-- Save Button -->
            <div class="flex justify-end pt-4 border-t border-gray-200">
              <button
                type="submit"
                :disabled="savingLanguage"
                class="px-6 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
              >
                <span v-if="savingLanguage">{{ $t('websiteBuilder.saving') }}</span>
                <span v-else>{{ $t('websiteBuilder.saveChanges') }}</span>
              </button>
            </div>
          </form>
        </div>
      </div>

      <!-- Currency Tab -->
      <div v-show="activeTab === 'currency'" class="space-y-6">
        <div class="bg-white rounded-xl shadow-md p-8">
          <h2 class="text-2xl font-bold text-gray-900 mb-6">{{ $t('restaurantDashboard.currencySettings') }}</h2>
          
          <form @submit.prevent="saveCurrencySettings" class="space-y-6">
            <!-- Currency Code -->
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-2">
                {{ $t('restaurantDashboard.currencyCode') }} *
              </label>
              <select
                v-model="currencySettings.currency_code"
                required
                class="w-full md:w-64 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              >
                <option value="USD">USD - US Dollar ($)</option>
                <option value="JOD">JOD - Jordanian Dinar (د.ا)</option>
              </select>
              <p class="text-xs text-gray-500 mt-1">{{ $t('restaurantDashboard.currencyCodeHint') }}</p>
            </div>

            <!-- Currency Symbol Position -->
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-4">
                {{ $t('restaurantDashboard.currencySymbolPosition') }}
              </label>
              <div class="space-y-3">
                <label :class="[
                  'flex items-center cursor-pointer p-4 border-2 rounded-lg transition-colors',
                  currencySettings.symbol_position === 'before' ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200 hover:border-gray-300',
                  $i18n.locale === 'ar' ? 'flex-row-reverse' : ''
                ]">
                  <input
                    v-model="currencySettings.symbol_position"
                    type="radio"
                    value="before"
                    class="w-5 h-5 text-indigo-600 focus:ring-indigo-500"
                    :style="{ accentColor: website?.primary_color || '#4F46E5' }"
                  />
                  <div :class="['ml-3', $i18n.locale === 'ar' ? 'mr-3 ml-0' : '']">
                    <span class="text-lg font-semibold text-gray-700">{{ $t('restaurantDashboard.beforeAmount') }}</span>
                    <p class="text-sm text-gray-600 mt-1">{{ $t('restaurantDashboard.beforeAmountExample') }}</p>
                  </div>
                </label>
                
                <label :class="[
                  'flex items-center cursor-pointer p-4 border-2 rounded-lg transition-colors',
                  currencySettings.symbol_position === 'after' ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200 hover:border-gray-300',
                  $i18n.locale === 'ar' ? 'flex-row-reverse' : ''
                ]">
                  <input
                    v-model="currencySettings.symbol_position"
                    type="radio"
                    value="after"
                    class="w-5 h-5 text-indigo-600 focus:ring-indigo-500"
                    :style="{ accentColor: website?.primary_color || '#4F46E5' }"
                  />
                  <div :class="['ml-3', $i18n.locale === 'ar' ? 'mr-3 ml-0' : '']">
                    <span class="text-lg font-semibold text-gray-700">{{ $t('restaurantDashboard.afterAmount') }}</span>
                    <p class="text-sm text-gray-600 mt-1">{{ $t('restaurantDashboard.afterAmountExample') }}</p>
                  </div>
                </label>
              </div>
            </div>

            <!-- Currency Display Preview -->
            <div class="p-6 bg-gray-50 border-2 border-gray-200 rounded-lg">
              <label class="block text-sm font-semibold text-gray-700 mb-3">
                {{ $t('restaurantDashboard.preview') }}
              </label>
              <div class="text-3xl font-bold" :style="{ color: website?.primary_color || '#4F46E5' }">
                {{ formatCurrency(99.99) }}
              </div>
              <p class="text-sm text-gray-500 mt-2">{{ $t('restaurantDashboard.previewHint') }}</p>
            </div>

            <!-- Error Message -->
            <div v-if="currencyError" class="p-4 bg-red-50 border border-red-200 rounded-lg">
              <p class="text-sm text-red-600">{{ currencyError }}</p>
            </div>

            <!-- Success Message -->
            <div v-if="currencySuccess" class="p-4 bg-green-50 border border-green-200 rounded-lg">
              <p class="text-sm text-green-600">{{ currencySuccess }}</p>
            </div>

            <!-- Save Button -->
            <div class="flex justify-end pt-4 border-t border-gray-200">
              <button
                type="submit"
                :disabled="savingCurrency"
                class="px-6 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
              >
                <span v-if="savingCurrency">{{ $t('websiteBuilder.saving') }}</span>
                <span v-else>{{ $t('websiteBuilder.saveChanges') }}</span>
              </button>
            </div>
          </form>
        </div>
      </div>

      <!-- Settings Tab -->
      <div v-show="activeTab === 'settings'" class="space-y-6">
        <div class="bg-white rounded-xl shadow-md p-8">
          <h2 class="text-2xl font-bold text-gray-900 mb-6">{{ $t('restaurantDashboard.settings') }}</h2>
          
          <!-- Barcode/QR Code -->
          <div v-if="website?.barcode_code" class="mb-8">
            <h3 class="text-lg font-semibold text-gray-800 mb-4">{{ $t('websiteBuilder.restaurantQrCode') }}</h3>
            <div class="bg-gradient-to-br from-indigo-50 to-purple-50 rounded-xl p-6 border border-indigo-200">
              <div class="flex flex-col md:flex-row items-start md:items-center gap-6" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <div class="flex-shrink-0">
                  <div class="bg-white p-4 rounded-lg shadow-lg inline-block">
                    <qrcode-vue
                      :value="getBarcodeUrl()"
                      :size="200"
                      level="H"
                      render-as="svg"
                    ></qrcode-vue>
                  </div>
                </div>
                <div class="flex-1">
                  <div class="mb-4">
                    <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('websiteBuilder.barcodeCode') }}</label>
                    <div class="flex items-center gap-3" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                      <input
                        :value="website.barcode_code"
                        readonly
                        class="px-4 py-2 border border-gray-300 rounded-lg bg-gray-50 font-mono text-lg font-bold text-gray-900"
                      />
                      <button
                        @click="copyBarcodeCode"
                        class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors text-sm font-semibold"
                      >
                        {{ $t('websiteBuilder.copy') }}
                      </button>
                    </div>
                  </div>
                  <div>
                    <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('websiteBuilder.qrCodeUrl') }}</label>
                    <div class="flex items-center gap-3" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                      <input
                        :value="getBarcodeUrl()"
                        readonly
                        class="px-4 py-2 border border-gray-300 rounded-lg bg-gray-50 font-mono text-sm"
                      />
                      <button
                        @click="copyBarcodeUrl"
                        class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors text-sm font-semibold"
                      >
                        {{ $t('websiteBuilder.copyUrl') }}
                      </button>
                      <button
                        @click="openBarcodeUrl"
                        class="px-4 py-2 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition-colors text-sm font-semibold flex items-center gap-2"
                      >
                        {{ $t('websiteBuilder.viewWebsite') }}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Change Password -->
          <div class="border-t border-gray-200 pt-6">
            <h3 class="text-lg font-semibold text-gray-800 mb-4">{{ $t('restaurantDashboard.changePassword') }}</h3>
            <form @submit.prevent="changePassword" class="space-y-4">
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.currentPassword') }}</label>
                <input
                  v-model="passwordForm.currentPassword"
                  type="password"
                  required
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                />
              </div>
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.newPassword') }}</label>
                <input
                  v-model="passwordForm.newPassword"
                  type="password"
                  required
                  minlength="6"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                />
              </div>
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.confirmNewPassword') }}</label>
                <input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  required
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                />
              </div>
              <div v-if="passwordError" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
                {{ passwordError }}
              </div>
              <div v-if="passwordSuccess" class="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg text-sm">
                {{ passwordSuccess }}
              </div>
              <button
                type="submit"
                :disabled="changingPassword"
                class="px-6 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <span v-if="changingPassword">{{ $t('restaurantDashboard.changing') }}</span>
                <span v-else>{{ $t('restaurantDashboard.changePasswordButton') }}</span>
              </button>
            </form>
          </div>
        </div>
      </div>
    </main>

    <!-- Product Form Modal -->
    <div v-if="showProductForm" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div class="p-6">
          <div class="flex items-center justify-between mb-6" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
            <h3 class="text-2xl font-bold text-gray-900">
              {{ editingProduct ? $t('websiteBuilder.editProduct') : $t('websiteBuilder.addNewProduct') }}
            </h3>
            <button
              @click="closeProductForm"
              class="text-gray-400 hover:text-gray-600"
            >
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <form @submit.prevent="saveProduct" class="space-y-4">
            <!-- Product name: English and Arabic (stored separately in DB) -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">
                  {{ $t('restaurantDashboard.productNameEn') }} <span class="text-red-500">{{ $t('websiteBuilder.required') }}</span>
                </label>
                <input
                  v-model="productForm.name"
                  type="text"
                  required
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                />
              </div>
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.productNameAr') }}</label>
                <input
                  v-model="productForm.name_ar"
                  type="text"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  placeholder="اسم المنتج بالعربية"
                />
              </div>
            </div>

            <!-- Description: English and Arabic -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.productDescriptionEn') }}</label>
                <textarea
                  v-model="productForm.description"
                  rows="3"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                ></textarea>
              </div>
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.productDescriptionAr') }}</label>
                <textarea
                  v-model="productForm.description_ar"
                  rows="3"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  placeholder="الوصف بالعربية"
                ></textarea>
              </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">
                  {{ $t('websiteBuilder.price') }} <span class="text-red-500">{{ $t('websiteBuilder.required') }}</span>
                </label>
                <input
                  v-model="productForm.price"
                  type="number"
                  step="0.01"
                  min="0"
                  required
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                />
              </div>
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.categoryEn') }}</label>
                <input
                  v-model="productForm.category"
                  type="text"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  :placeholder="$t('websiteBuilder.categoryPlaceholder')"
                />
              </div>
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('restaurantDashboard.categoryAr') }}</label>
                <input
                  v-model="productForm.category_ar"
                  type="text"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  placeholder="الفئة بالعربية"
                />
              </div>
            </div>

            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-2">{{ $t('websiteBuilder.productImage') }}</label>
              <input
                type="file"
                accept="image/*"
                @change="handleProductImageUpload"
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              />
              <div v-if="productForm.imagePreview" class="mt-3">
                <img :src="productForm.imagePreview" alt="Product preview" class="h-32 w-32 object-cover rounded-lg border-2 border-gray-200" />
              </div>
              <p class="text-xs text-gray-500 mt-1">{{ $t('websiteBuilder.productImageHint') }}</p>
            </div>

            <div class="flex items-center gap-4">
              <label class="flex items-center cursor-pointer" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <input
                  v-model="productForm.is_available"
                  type="checkbox"
                  class="w-5 h-5 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                />
                <span :class="['text-sm font-medium text-gray-700', $i18n.locale === 'ar' ? 'mr-2' : 'ml-2']">{{ $t('websiteBuilder.availableForOrder') }}</span>
              </label>
            </div>

            <div v-if="productError" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
              {{ productError }}
            </div>

            <div class="flex gap-3" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
              <button
                type="submit"
                :disabled="savingProduct"
                class="flex-1 px-6 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <span v-if="savingProduct">{{ $t('websiteBuilder.saving') }}</span>
                <span v-else>{{ editingProduct ? $t('websiteBuilder.updateProduct') : $t('websiteBuilder.createProduct') }}</span>
              </button>
              <button
                type="button"
                @click="closeProductForm"
                class="px-6 py-3 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition-colors font-semibold"
              >
                {{ $t('websiteBuilder.cancel') }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- Notification Toast -->
    <TransitionGroup name="notification" tag="div" :class="['fixed z-50 space-y-2', $i18n.locale === 'ar' ? 'bottom-4 left-4' : 'bottom-4 right-4']">
      <div
        v-for="notification in notifications"
        :key="notification.id"
        :class="['bg-white rounded-lg shadow-2xl p-4 min-w-[320px] max-w-md animate-slide-in', $i18n.locale === 'ar' ? 'border-r-4 border-indigo-500' : 'border-l-4 border-indigo-500']"
      >
        <div class="flex items-start gap-3" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
          <div class="flex-shrink-0">
            <div class="w-10 h-10 bg-indigo-100 rounded-full flex items-center justify-center">
              <svg class="w-6 h-6 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
          </div>
          <div class="flex-1">
            <h4 class="text-sm font-bold text-gray-900 mb-1">{{ $t('restaurantDashboard.newOrderReceived') }}</h4>
            <p class="text-sm text-gray-600 mb-2">
              {{ $t('restaurantDashboard.orderNumber') }}{{ notification.orderNumber }} - ${{ parseFloat(notification.totalAmount).toFixed(2) }}
            </p>
            <p class="text-xs text-gray-500">
              {{ notification.customerName }} • {{ notification.itemCount }} {{ $t('restaurantDashboard.itemCount') }}
            </p>
          </div>
          <button
            @click="removeNotification(notification.id)"
            class="flex-shrink-0 text-gray-400 hover:text-gray-600 transition-colors"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>
    </TransitionGroup>

    <!-- Branch Form Modal -->
    <div v-if="showBranchForm" class="fixed inset-0 z-50 overflow-y-auto bg-black bg-opacity-50 flex items-center justify-center p-4">
      <div class="bg-white rounded-xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div class="p-6 border-b border-gray-200">
          <div class="flex items-center justify-between">
            <h3 class="text-xl font-bold text-gray-900">
              {{ editingBranch ? 'Edit Branch' : 'Add Branch' }}
            </h3>
            <button
              @click="closeBranchForm"
              class="text-gray-400 hover:text-gray-600 transition-colors"
            >
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>

        <form @submit.prevent="saveBranch" class="p-6 space-y-4">
          <!-- Branch Number -->
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-2">
              Branch Number <span class="text-red-500">*</span>
            </label>
            <input
              v-model.number="branchForm.branch_number"
              type="number"
              min="1"
              required
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              placeholder="e.g., 1, 2, 3"
            />
          </div>

          <!-- Region -->
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-2">
              Region <span class="text-red-500">*</span>
            </label>
            <select
              v-model.number="branchForm.region_id"
              required
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            >
              <option value="">Select a region</option>
              <option
                v-for="region in regions"
                :key="region.id"
                :value="region.id"
              >
                {{ region.name_ar || region.name }}
                <span v-if="region.name && region.name_ar"> ({{ region.name }})</span>
              </option>
            </select>
          </div>

          <!-- Branch Name (English) -->
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-2">
              Branch Name (English)
            </label>
            <input
              v-model="branchForm.name"
              type="text"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              placeholder="e.g., Main Branch"
            />
          </div>

          <!-- Branch Name (Arabic) -->
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-2">
              Branch Name (Arabic)
            </label>
            <input
              v-model="branchForm.name_ar"
              type="text"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              placeholder="e.g., الفرع الرئيسي"
            />
          </div>

          <!-- Address -->
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-2">
              Address
            </label>
            <textarea
              v-model="branchForm.address"
              rows="3"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              placeholder="Branch address"
            ></textarea>
          </div>

          <!-- Phone -->
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-2">
              Phone
            </label>
            <input
              v-model="branchForm.phone"
              type="tel"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              placeholder="e.g., +962 7 1234 5678"
            />
          </div>

          <!-- Status -->
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-2">
              Status
            </label>
            <select
              v-model="branchForm.status"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            >
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
            </select>
          </div>

          <!-- Error Message -->
          <div v-if="branchError" class="p-4 bg-red-50 border border-red-200 rounded-lg">
            <p class="text-sm text-red-600">{{ branchError }}</p>
          </div>

          <!-- Buttons -->
          <div class="flex gap-3 pt-4 border-t border-gray-200">
            <button
              type="submit"
              :disabled="savingBranch"
              class="flex-1 px-6 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <span v-if="savingBranch">Saving...</span>
              <span v-else>{{ editingBranch ? 'Update Branch' : 'Create Branch' }}</span>
            </button>
            <button
              type="button"
              @click="closeBranchForm"
              class="px-6 py-3 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition-colors font-semibold"
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, TransitionGroup } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import QrcodeVue from 'qrcode.vue';
import LanguageSwitcher from './LanguageSwitcher.vue';
import { 
  getAdminInfo, 
  getAdminOrders, 
  updateOrderStatusAdmin,
  getRestaurantWebsite,
  updateRestaurantWebsite,
  getRestaurantDeliveryCompanies,
  getRestaurantDeliveryCompanyRequest,
  createRestaurantDeliveryCompanyRequest,
  deleteRestaurantDeliveryCompanyRequest,
  getRestaurantProducts,
  createRestaurantProduct,
  updateRestaurantProduct,
  deleteRestaurantProduct,
  uploadLogo,
  uploadGalleryImages,
  deleteGalleryImage,
  getNotificationSettings,
  updateNotificationSettings,
  uploadProductImage,
  getRestaurantCoupons,
  createCoupon,
  updateCoupon,
  deleteCouponAPI,
  getProductAddons,
  createProductAddon,
  updateProductAddon,
  deleteProductAddon,
  uploadAddonImage,
  updateProductAddonSettings,
  getBusinessHours,
  updateBusinessHours,
  getRestaurantOffers,
  createOffer,
  updateOffer,
  deleteOfferAPI,
  getProductsWithAddons,
  getRestaurantRegions,
  getRestaurantBranches,
  createRestaurantBranch,
  updateRestaurantBranch,
  deleteRestaurantBranch
} from '../services/api.js';

const { t, locale } = useI18n();

const router = useRouter();
const restaurantInfo = ref(null);
const website = ref(null);
const activeTab = ref('basic');
const loadingWebsite = ref(true);
const loadingProducts = ref(false);
const loadingOrders = ref(false);
const saving = ref(false);
const saveError = ref('');
const saveSuccess = ref('');
const products = ref([]);
const orders = ref([]);
const showAllOrders = ref(false);
const showProductForm = ref(false);
const editingProduct = ref(null);
const savingProduct = ref(false);
const productError = ref('');
const showAddonsModal = ref(false);
const addonsProduct = ref(null);
const addonsList = ref([]);
const addonRequired = ref(false);
const addonRequiredMin = ref(1);
const loadingAddons = ref(false);
const addonError = ref('');
const addonForm = ref({ name: '', name_ar: '', description: '', description_ar: '', price: '0', is_required: false, imageFile: null, imagePreview: null });
const showAddonForm = ref(false);
const editingAddon = ref(null);

// Add-on display by locale (name, description from DB EN / _ar)
const getAddonDisplay = (addon) => {
  if (!addon) return { name: '', description: '' };
  const isAr = locale.value === 'ar';
  return {
    name: (isAr && addon.name_ar) ? addon.name_ar : (addon.name || ''),
    description: (isAr && addon.description_ar) ? addon.description_ar : (addon.description || '')
  };
};
const savingAddon = ref(false);
const updatingOrderId = ref(null);
const eventSource = ref(null);
const sseConnected = ref(false);
const notifications = ref([]);
let notificationIdCounter = 0;
const changingPassword = ref(false);
const passwordError = ref('');
const passwordSuccess = ref('');
const notificationSettings = ref({
  notificationsEnabled: true,
  emailEnabled: true,
  smsEnabled: false,
  pushEnabled: true,
  whatsappEnabled: false,
  notificationEmail: ''
});
const savingNotifications = ref(false);
const notificationError = ref('');
const notificationSuccess = ref('');

// Branches
const branches = ref([]);
const regions = ref([]);
const loadingBranches = ref(false);
const loadingRegions = ref(false);
const showBranchForm = ref(false);
const editingBranch = ref(null);
const savingBranch = ref(false);
const branchError = ref('');
const branchForm = ref({
  branch_number: '',
  region_id: '',
  name: '',
  name_ar: '',
  address: '',
  phone: '',
  status: 'active'
});

// Business hours (7 days: 0=Sunday .. 6=Saturday)
const businessHours = ref([
  { day_of_week: 0, open_time: null, close_time: null, is_closed: true },
  { day_of_week: 1, open_time: null, close_time: null, is_closed: true },
  { day_of_week: 2, open_time: null, close_time: null, is_closed: true },
  { day_of_week: 3, open_time: null, close_time: null, is_closed: true },
  { day_of_week: 4, open_time: null, close_time: null, is_closed: true },
  { day_of_week: 5, open_time: null, close_time: null, is_closed: true },
  { day_of_week: 6, open_time: null, close_time: null, is_closed: true }
]);
const savingBusinessHours = ref(false);
const businessHoursError = ref('');
const businessHoursSuccess = ref('');
// Offers
const offers = ref([]);
const showOfferForm = ref(false);
const editingOffer = ref(null);
const offerForm = ref({
  offer_type: 'free_delivery',
  offer_scope: 'all_items',
  selected_product_ids: [],
  selected_addon_ids: [],
  title: '',
  description: '',
  value: '',
  min_order_value: '',
  is_active: true,
  valid_from: '',
  valid_until: ''
});
const productsWithAddons = ref([]);
const loadingProductsWithAddons = ref(false);
const savingOffer = ref(false);
const offerError = ref('');
const offerSuccess = ref('');
const dayNames = computed(() => ({
  0: t('restaurantDashboard.daySunday'),
  1: t('restaurantDashboard.dayMonday'),
  2: t('restaurantDashboard.dayTuesday'),
  3: t('restaurantDashboard.dayWednesday'),
  4: t('restaurantDashboard.dayThursday'),
  5: t('restaurantDashboard.dayFriday'),
  6: t('restaurantDashboard.daySaturday')
}));

// Tax Requirement Settings
const taxRequirement = ref({
  enabled: false,
  rate: 0
});
const savingTax = ref(false);
const taxError = ref('');
const taxSuccess = ref('');

// Payment Methods Settings
const paymentMethods = ref({
  cashOnPickup: true,
  cashOnDelivery: true,
  creditCard: false,
  onlinePayment: false,
  mobilePayment: false,
  cliQServices: {
    enabled: false,
    allowNamePhone: false,
    phone: '',
    name: ''
  }
});
const savingPaymentMethods = ref(false);
const paymentMethodsError = ref('');
const paymentMethodsSuccess = ref('');

// Language Settings
const languageSettings = ref({
  default_language: 'en',
  languages_enabled: {
    english: true,
    arabic: true
  }
});
const savingLanguage = ref(false);
const languageError = ref('');
const languageSuccess = ref('');

// Currency Settings
const currencySettings = ref({
  currency_code: 'USD',
  symbol_position: 'before'
});
const savingCurrency = ref(false);
const currencyError = ref('');
const currencySuccess = ref('');

// Coupons
const coupons = ref([]);
const showCouponForm = ref(false);
const editingCoupon = ref(null);
const savingCoupon = ref(false);
const couponError = ref('');
const couponSuccess = ref('');
const couponForm = ref({
  code: '',
  description: '',
  discount_type: 'percentage',
  discount_value: '',
  min_order_amount: '',
  max_discount_amount: '',
  valid_from: '',
  valid_until: '',
  usage_limit: '',
  is_active: true
});

// Order Type Settings
const orderTypeSettings = ref({
  dineInEnabled: true,
  pickupEnabled: true,
  deliveryEnabled: true,
  deliveryFee: 0,
  deliveryTimeMin: null,
  deliveryTimeMax: null,
  deliveryMode: 'fixed_fee'
});
// Delivery company request (for "Delivery Company" option)
const deliveryCompaniesList = ref([]);
const deliveryCompanyRequest = ref({ requests: [], approvedDeliveryCompany: null, pendingRequest: null });
const loadingStoreRequests = ref(false);
const selectedDeliveryCompanyId = ref('');
const sendingDeliveryCompanyRequest = ref(false);
const resettingDeliveryCompany = ref(false);
const savingOrderTypes = ref(false);
const orderTypeError = ref('');
const orderTypeSuccess = ref('');

// Website form (restaurant_name/description/address = English; _ar = Arabic)
const websiteForm = ref({
  restaurant_name: '',
  restaurant_name_ar: '',
  logo_url: '',
  description: '',
  description_ar: '',
  address: '',
  address_ar: '',
  phone: '',
  email: '',
  website_url: '',
  primary_color: '#4F46E5',
  secondary_color: '#7C3AED',
  app_download_url: '',
  locations: [],
  newsletter_enabled: false,
  is_published: false,
  subdomain: '',
  custom_domain: ''
});

// Logo upload
const logoFile = ref(null);
const logoPreview = ref(null);

// Gallery
const galleryImages = ref([]);
const galleryFiles = ref([]);
const uploadingGallery = ref(false);

// Locations
const locationsText = ref('');

// Base domain
const baseDomain = import.meta.env.VITE_BASE_DOMAIN || 'yourplatform.com';

// Product form (name/description/category = English; _ar = Arabic)
const productForm = ref({
  name: '',
  name_ar: '',
  description: '',
  description_ar: '',
  price: '',
  category: '',
  category_ar: '',
  is_available: true,
  imageFile: null,
  imagePreview: null
});

// Password form
const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
});

// Check if an order is from today
const isToday = (dateString) => {
  const orderDate = new Date(dateString);
  const today = new Date();
  return (
    orderDate.getDate() === today.getDate() &&
    orderDate.getMonth() === today.getMonth() &&
    orderDate.getFullYear() === today.getFullYear()
  );
};

const todaysOrders = computed(() => {
  return orders.value.filter(order => isToday(order.created_at));
});

const todaysRevenue = computed(() => {
  return todaysOrders.value.reduce((sum, order) => {
    if (order.status !== 'cancelled') {
      return sum + parseFloat(order.total_amount || 0);
    }
    return sum;
  }, 0);
});

const getTodaysOrdersByStatus = (status) => {
  if (status === null) {
    return todaysOrders.value;
  }
  return todaysOrders.value.filter(order => order.status === status);
};

const getStatusClass = (status) => {
  const classes = {
    pending: 'bg-yellow-100 text-yellow-800',
    confirmed: 'bg-blue-100 text-blue-800',
    preparing: 'bg-purple-100 text-purple-800',
    ready: 'bg-green-100 text-green-800',
    completed: 'bg-gray-100 text-gray-800',
    cancelled: 'bg-red-100 text-red-800'
  };
  return classes[status] || 'bg-gray-100 text-gray-800';
};

const getOrderBorderClass = (status) => {
  const classes = {
    pending: 'border-l-yellow-500',
    confirmed: 'border-l-blue-500',
    preparing: 'border-l-purple-500',
    ready: 'border-l-green-500',
    completed: 'border-l-gray-500',
    cancelled: 'border-l-red-500'
  };
  return classes[status] || 'border-l-gray-300';
};


// Load restaurant info and website
const loadRestaurantData = async () => {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) {
      router.push('/restaurant/login');
      return;
    }

    // Load admin/restaurant info
    restaurantInfo.value = await getAdminInfo();
    
    // Load website data using restaurant-scoped API
    website.value = await getRestaurantWebsite();
    
    // Populate website form
    if (website.value) {
      websiteForm.value = {
        restaurant_name: website.value.restaurant_name || '',
        restaurant_name_ar: website.value.restaurant_name_ar || '',
        logo_url: website.value.logo_url || '',
        description: website.value.description || '',
        description_ar: website.value.description_ar || '',
        address: website.value.address || '',
        address_ar: website.value.address_ar || '',
        phone: website.value.phone || '',
        email: website.value.email || '',
        website_url: website.value.website_url || '',
        primary_color: website.value.primary_color || '#4F46E5',
        secondary_color: website.value.secondary_color || '#7C3AED',
        app_download_url: website.value.app_download_url || '',
        locations: website.value.locations || [],
        newsletter_enabled: website.value.newsletter_enabled || false,
        is_published: website.value.is_published || false,
        subdomain: website.value.subdomain || '',
        custom_domain: website.value.custom_domain || ''
      };
      
      // Parse locations for text input
      if (website.value.locations) {
        try {
          locationsText.value = typeof website.value.locations === 'string'
            ? website.value.locations
            : JSON.stringify(website.value.locations, null, 2);
        } catch (e) {
          locationsText.value = '';
        }
      }
      
      // Load gallery images
      if (website.value.gallery_images) {
        try {
          galleryImages.value = typeof website.value.gallery_images === 'string'
            ? JSON.parse(website.value.gallery_images)
            : website.value.gallery_images;
        } catch (e) {
          galleryImages.value = [];
        }
      }
      
      // Load order type settings from database columns
      // MySQL returns BOOLEAN as 0/1 (tinyint), so we need to convert them
      orderTypeSettings.value = {
        dineInEnabled: website.value.order_type_dine_in_enabled !== undefined && website.value.order_type_dine_in_enabled !== null
          ? (website.value.order_type_dine_in_enabled === 1 || website.value.order_type_dine_in_enabled === true)
          : true,
        pickupEnabled: website.value.order_type_pickup_enabled !== undefined && website.value.order_type_pickup_enabled !== null
          ? (website.value.order_type_pickup_enabled === 1 || website.value.order_type_pickup_enabled === true)
          : true,
        deliveryEnabled: website.value.order_type_delivery_enabled !== undefined && website.value.order_type_delivery_enabled !== null
          ? (website.value.order_type_delivery_enabled === 1 || website.value.order_type_delivery_enabled === true)
          : true,
        deliveryFee: website.value.delivery_fee !== undefined && website.value.delivery_fee !== null
          ? parseFloat(website.value.delivery_fee) || 0
          : 0,
        deliveryTimeMin: website.value.delivery_time_min != null && website.value.delivery_time_min !== ''
          ? parseInt(website.value.delivery_time_min, 10) : null,
        deliveryTimeMax: website.value.delivery_time_max != null && website.value.delivery_time_max !== ''
          ? parseInt(website.value.delivery_time_max, 10) : null,
        deliveryMode: (website.value.delivery_mode === 'delivery_company' ? 'delivery_company' : 'fixed_fee')
      };
      
      // Fallback: try parsing from order_types JSON field if columns don't exist
      if (website.value.order_types && 
          (website.value.order_type_dine_in_enabled === undefined || website.value.order_type_dine_in_enabled === null)) {
        try {
          const orderTypes = typeof website.value.order_types === 'string'
            ? JSON.parse(website.value.order_types)
            : website.value.order_types;
          orderTypeSettings.value = {
            dineInEnabled: orderTypes.dineInEnabled ?? true,
            pickupEnabled: orderTypes.pickupEnabled ?? true,
            deliveryEnabled: orderTypes.deliveryEnabled ?? true,
            deliveryFee: orderTypes.deliveryFee ?? 0,
            deliveryTimeMin: orderTypes.deliveryTimeMin ?? null,
            deliveryTimeMax: orderTypes.deliveryTimeMax ?? null,
            deliveryMode: orderTypes.deliveryMode ?? 'fixed_fee'
          };
        } catch (e) {
          // Use defaults if parsing fails
        }
      }
      await loadDeliveryCompanyRequest();
    }
  } catch (error) {
    console.error('Failed to load restaurant data:', error);
    if (error.message.includes('token') || error.message.includes('401')) {
      handleLogout();
    }
  } finally {
    loadingWebsite.value = false;
  }
};

// Load delivery company request status (for Order Type > Delivery Company)
const loadDeliveryCompanyRequest = async () => {
  loadingStoreRequests.value = true;
  try {
    const data = await getRestaurantDeliveryCompanyRequest();
    deliveryCompanyRequest.value = {
      requests: data.requests || [],
      approvedDeliveryCompany: data.approvedDeliveryCompany || null,
      pendingRequest: data.pendingRequest || null
    };
  } catch (e) {
    console.error('Failed to load delivery company request:', e);
    deliveryCompanyRequest.value = { requests: [], approvedDeliveryCompany: null, pendingRequest: null };
  } finally {
    loadingStoreRequests.value = false;
  }
};

// Load delivery companies list for dropdown
const loadDeliveryCompaniesList = async () => {
  try {
    deliveryCompaniesList.value = await getRestaurantDeliveryCompanies();
  } catch (e) {
    console.error('Failed to load delivery companies:', e);
    deliveryCompaniesList.value = [];
  }
};

const sendDeliveryCompanyRequest = async () => {
  if (!selectedDeliveryCompanyId.value) return;
  sendingDeliveryCompanyRequest.value = true;
  try {
    await createRestaurantDeliveryCompanyRequest(selectedDeliveryCompanyId.value);
    await loadDeliveryCompanyRequest();
  } catch (e) {
    alert(e.message || 'Failed to send request');
  } finally {
    sendingDeliveryCompanyRequest.value = false;
  }
};

const resetDeliveryCompany = async () => {
  if (!confirm(t('restaurantDashboard.confirmRemoveDeliveryCompany') || 'Are you sure you want to remove this delivery company? You can select a different one after removing.')) {
    return;
  }
  resettingDeliveryCompany.value = true;
  try {
    // Delete all requests (pending, approved, rejected) to ensure clean removal
    const requestsToDelete = deliveryCompanyRequest.value.requests || [];
    const deletePromises = requestsToDelete.map(request => 
      deleteRestaurantDeliveryCompanyRequest(request.id).catch(err => {
        console.warn(`Failed to delete request ${request.id}:`, err);
        // Continue even if one fails
      })
    );
    await Promise.all(deletePromises);
    
    // Clear delivery_company_id from website
    await updateRestaurantWebsite({
      delivery_company_id: null
    });
    
    // Update local website object
    if (website.value) {
      website.value.delivery_company_id = null;
    }
    
    // Reset local state
    selectedDeliveryCompanyId.value = '';
    // Reload delivery company request to refresh state
    await loadDeliveryCompanyRequest();
    orderTypeSuccess.value = t('restaurantDashboard.deliveryCompanyRemoved') || 'Delivery company removed successfully. You can now select a different one.';
    setTimeout(() => {
      orderTypeSuccess.value = '';
    }, 3000);
  } catch (e) {
    console.error('Failed to remove delivery company:', e);
    alert(e.message || 'Failed to remove delivery company');
  } finally {
    resettingDeliveryCompany.value = false;
  }
};

// Load products
const loadProducts = async () => {
  try {
    loadingProducts.value = true;
    products.value = await getRestaurantProducts();
  } catch (error) {
    console.error('Failed to load products:', error);
    productError.value = 'Failed to load products';
  } finally {
    loadingProducts.value = false;
  }
};

// Load orders
const loadOrders = async () => {
  try {
    loadingOrders.value = true;
    const loadedOrders = await getAdminOrders();
    orders.value = loadedOrders || [];
  } catch (error) {
    console.error('Failed to load orders:', error);
    orders.value = [];
    if (error.response?.status === 401) {
      handleLogout();
      return;
    }
    alert('Failed to load orders: ' + (error.response?.data?.error || error.message));
  } finally {
    loadingOrders.value = false;
  }
};

// Handle logo upload
const handleLogoUpload = (event) => {
  const file = event.target.files[0];
  if (file) {
    logoFile.value = file;
    const reader = new FileReader();
    reader.onload = (e) => {
      logoPreview.value = e.target.result;
    };
    reader.readAsDataURL(file);
  }
};

// Handle gallery upload
const handleGalleryUpload = (event) => {
  const files = Array.from(event.target.files);
  if (files.length > 0) {
    galleryFiles.value.push(...files);
  }
};

// Upload gallery images
const uploadGalleryNow = async () => {
  if (!website.value?.id || galleryFiles.value.length === 0) return;
  
  uploadingGallery.value = true;
  try {
    const updatedWebsite = await uploadGalleryImages(website.value.id, galleryFiles.value);
    website.value = updatedWebsite;
    galleryFiles.value = [];
    
    // Reload gallery images
    if (updatedWebsite.gallery_images) {
      try {
        galleryImages.value = typeof updatedWebsite.gallery_images === 'string'
          ? JSON.parse(updatedWebsite.gallery_images)
          : updatedWebsite.gallery_images;
      } catch (e) {
        galleryImages.value = [];
      }
    }
    
    saveSuccess.value = 'Gallery images uploaded successfully!';
  } catch (error) {
    saveError.value = error.message || 'Failed to upload gallery images';
  } finally {
    uploadingGallery.value = false;
  }
};

// Remove gallery image
const removeGalleryImage = async (index) => {
  if (!website.value?.id) return;
  
  try {
    await deleteGalleryImage(website.value.id, index);
    galleryImages.value.splice(index, 1);
    saveSuccess.value = 'Image removed successfully!';
  } catch (error) {
    saveError.value = error.message || 'Failed to remove image';
  }
};

// Save website
const saveWebsite = async () => {
  saving.value = true;
  saveError.value = '';
  saveSuccess.value = '';
  
  try {
    // Parse locations
    let locations = [];
    if (locationsText.value.trim()) {
      try {
        locations = JSON.parse(locationsText.value);
      } catch (e) {
        saveError.value = 'Invalid locations JSON format';
        saving.value = false;
        return;
      }
    }
    
    // Upload logo if new file selected
    if (logoFile.value && website.value?.id) {
      try {
        const updatedWebsite = await uploadLogo(website.value.id, logoFile.value);
        website.value = updatedWebsite;
        websiteForm.value.logo_url = updatedWebsite.logo_url;
        logoFile.value = null;
        logoPreview.value = null;
      } catch (error) {
        console.error('Failed to upload logo:', error);
      }
    }
    
    // Prepare website data
    const websiteData = {
      ...websiteForm.value,
      locations: locations
    };
    
    website.value = await updateRestaurantWebsite(websiteData);
    saveSuccess.value = 'Website updated successfully!';
    
    // Reload website data
    await loadRestaurantData();
  } catch (error) {
    saveError.value = error.message || 'Failed to save website';
  } finally {
    saving.value = false;
  }
};

// Product management
const handleProductImageUpload = (event) => {
  const file = event.target.files[0];
  if (file) {
    productForm.value.imageFile = file;
    const reader = new FileReader();
    reader.onload = (e) => {
      productForm.value.imagePreview = e.target.result;
    };
    reader.readAsDataURL(file);
  }
};

const editProduct = (product) => {
  editingProduct.value = product;
  productForm.value = {
    name: product.name || '',
    name_ar: product.name_ar || '',
    description: product.description || '',
    description_ar: product.description_ar || '',
    price: product.price || '',
    category: product.category || '',
    category_ar: product.category_ar || '',
    is_available: product.is_available !== false,
    imageFile: null,
    imagePreview: product.image_url || null
  };
  showProductForm.value = true;
};

const closeProductForm = () => {
  showProductForm.value = false;
  editingProduct.value = null;
  productForm.value = {
    name: '',
    name_ar: '',
    description: '',
    description_ar: '',
    price: '',
    category: '',
    category_ar: '',
    is_available: true,
    imageFile: null,
    imagePreview: null
  };
  productError.value = '';
};

const saveProduct = async () => {
  savingProduct.value = true;
  productError.value = '';
  
  try {
    let savedProduct;
    if (editingProduct.value) {
      savedProduct = await updateRestaurantProduct(editingProduct.value.id, productForm.value);
    } else {
      savedProduct = await createRestaurantProduct(productForm.value);
    }
    
    // Upload image if provided
    if (productForm.value.imageFile && savedProduct.id) {
      try {
        await uploadProductImage(savedProduct.id, productForm.value.imageFile);
      } catch (imageError) {
        console.error('Failed to upload product image:', imageError);
        // Don't fail the whole operation if image upload fails
      }
    }
    
    await loadProducts();
    closeProductForm();
  } catch (error) {
    productError.value = error.message || 'Failed to save product';
  } finally {
    savingProduct.value = false;
  }
};

const deleteProduct = async (productId) => {
  if (!confirm(t('restaurantDashboard.deleteConfirm'))) {
    return;
  }
  
  try {
    await deleteRestaurantProduct(productId);
    await loadProducts();
  } catch (error) {
    alert('Failed to delete product: ' + error.message);
  }
};

// Add-ons
const openAddons = async (product) => {
  addonsProduct.value = product;
  showAddonsModal.value = true;
  addonError.value = '';
  showAddonForm.value = false;
  editingAddon.value = null;
  await loadAddons();
  addonRequired.value = !!product.addon_required;
  addonRequiredMin.value = product.addon_required_min != null ? product.addon_required_min : 1;
};

const closeAddons = () => {
  showAddonsModal.value = false;
  addonsProduct.value = null;
  addonsList.value = [];
  addonError.value = '';
};

const loadAddons = async () => {
  if (!addonsProduct.value?.id) return;
  loadingAddons.value = true;
  addonError.value = '';
  try {
    const data = await getProductAddons(addonsProduct.value.id);
    addonsList.value = data.addons || [];
    addonRequired.value = !!data.addon_required;
    addonRequiredMin.value = data.addon_required_min != null ? data.addon_required_min : 1;
  } catch (error) {
    addonError.value = error.message || 'Failed to load add-ons';
  } finally {
    loadingAddons.value = false;
  }
};

const saveAddonSettings = async () => {
  if (!addonsProduct.value?.id) return;
  addonError.value = '';
  try {
    await updateProductAddonSettings(addonsProduct.value.id, {
      addon_required: addonRequired.value,
      addon_required_min: addonRequired.value ? addonRequiredMin.value : null
    });
    const idx = products.value.findIndex(p => p.id === addonsProduct.value.id);
    if (idx >= 0) {
      products.value[idx] = { ...products.value[idx], addon_required: addonRequired.value, addon_required_min: addonRequired.value ? addonRequiredMin.value : null };
    }
    addonsProduct.value = { ...addonsProduct.value, addon_required: addonRequired.value, addon_required_min: addonRequired.value ? addonRequiredMin.value : null };
  } catch (error) {
    addonError.value = error.message || 'Failed to save rule';
  }
};

const editAddon = (addon) => {
  editingAddon.value = addon;
  addonForm.value = {
    name: addon.name || '',
    name_ar: addon.name_ar || '',
    description: addon.description || '',
    description_ar: addon.description_ar || '',
    price: String(addon.price ?? 0),
    is_required: !!addon.is_required,
    imageFile: null,
    imagePreview: addon.image_url || null
  };
  showAddonForm.value = true;
  addonError.value = '';
};

const saveAddon = async () => {
  if (!addonsProduct.value?.id) return;
  if (!addonForm.value.name?.trim()) {
    addonError.value = 'Add-on name is required';
    return;
  }
  savingAddon.value = true;
  addonError.value = '';
  try {
    const payload = {
      name: addonForm.value.name.trim(),
      name_ar: addonForm.value.name_ar?.trim() || null,
      description: addonForm.value.description?.trim() || null,
      description_ar: addonForm.value.description_ar?.trim() || null,
      price: parseFloat(addonForm.value.price) || 0,
      is_required: !!addonForm.value.is_required
    };
    if (editingAddon.value) {
      await updateProductAddon(addonsProduct.value.id, editingAddon.value.id, payload);
    } else {
      const addon = await createProductAddon(addonsProduct.value.id, payload);
      if (addonForm.value.imageFile && addon.id) {
        try {
          await uploadAddonImage(addonsProduct.value.id, addon.id, addonForm.value.imageFile);
        } catch (e) {
          console.warn('Addon image upload failed', e);
        }
      }
    }
    if (editingAddon.value && addonForm.value.imageFile) {
      try {
        await uploadAddonImage(addonsProduct.value.id, editingAddon.value.id, addonForm.value.imageFile);
      } catch (e) {
        console.warn('Addon image upload failed', e);
      }
    }
    await loadAddons();
    showAddonForm.value = false;
    editingAddon.value = null;
    addonForm.value = { name: '', name_ar: '', description: '', description_ar: '', price: '0', is_required: false, imageFile: null, imagePreview: null };
    addonError.value = '';
  } catch (error) {
    addonError.value = error.message || 'Failed to save add-on';
  } finally {
    savingAddon.value = false;
  }
};

const deleteAddon = async (addonId) => {
  if (!confirm(t('restaurantDashboard.confirmDeleteAddon'))) return;
  if (!addonsProduct.value?.id) return;
  try {
    await deleteProductAddon(addonsProduct.value.id, addonId);
    await loadAddons();
  } catch (error) {
    addonError.value = error.message || 'Failed to delete add-on';
  }
};

const handleAddonImageUpload = (e) => {
  const file = e.target.files?.[0];
  if (!file) return;
  addonForm.value.imageFile = file;
  const reader = new FileReader();
  reader.onload = (ev) => {
    addonForm.value.imagePreview = ev.target?.result;
  };
  reader.readAsDataURL(file);
};

// Order management
const updateStatus = async (orderId, newStatus) => {
  try {
    updatingOrderId.value = orderId;
    await updateOrderStatusAdmin(orderId, newStatus);
    await loadOrders();
  } catch (error) {
    console.error('Failed to update order status:', error);
    alert('Failed to update order status: ' + error.message);
  } finally {
    updatingOrderId.value = null;
  }
};

// SSE for real-time updates
const setupSSE = () => {
  const token = localStorage.getItem('restaurantToken');
  if (!token) return;

  const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000';
  const sseUrl = `${API_BASE_URL}/api/admin/orders/stream`;

  try {
    eventSource.value = new EventSource(`${sseUrl}?token=${encodeURIComponent(token)}`);

    eventSource.value.onopen = () => {
      console.log('SSE connection opened');
      sseConnected.value = true;
    };

    eventSource.value.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        
        if (data.type === 'connected') {
          console.log('SSE connected:', data.message);
        } else if (data.type === 'new_order') {
          console.log('New order received via SSE:', data.order);
          showNewOrderNotification(data.order);
          loadOrders();
        } else if (data.type === 'order_status_update') {
          console.log('Order status updated via SSE:', data.orderId, data.status);
          const orderIndex = orders.value.findIndex(o => o.id === data.orderId);
          if (orderIndex !== -1) {
            orders.value[orderIndex].status = data.status;
          }
        }
      } catch (error) {
        console.error('Error parsing SSE message:', error);
      }
    };

    eventSource.value.onerror = (error) => {
      console.error('SSE error:', error);
      sseConnected.value = false;
      
      if (eventSource.value?.readyState === EventSource.CLOSED) {
        setTimeout(() => {
          console.log('Attempting to reconnect SSE...');
          closeSSE();
          setupSSE();
        }, 5000);
      }
    };
  } catch (error) {
    console.error('Failed to setup SSE:', error);
  }
};

const closeSSE = () => {
  if (eventSource.value) {
    eventSource.value.close();
    eventSource.value = null;
    sseConnected.value = false;
  }
};

const showNewOrderNotification = (order) => {
  const notification = {
    id: notificationIdCounter++,
    orderNumber: order.order_number,
    totalAmount: order.total_amount,
    customerName: order.customer_name,
    itemCount: order.items?.length || 0
  };
  
  notifications.value.push(notification);
  
  setTimeout(() => {
    removeNotification(notification.id);
  }, 8000);
};

const removeNotification = (id) => {
  const index = notifications.value.findIndex(n => n.id === id);
  if (index !== -1) {
    notifications.value.splice(index, 1);
  }
};

// Settings
const getBarcodeUrl = () => {
  if (!website.value?.barcode_code) return '';
  const baseUrl = window.location.origin;
  return `${baseUrl}/barcode/${website.value.barcode_code}`;
};

const copyBarcodeCode = () => {
  if (website.value?.barcode_code) {
    navigator.clipboard.writeText(website.value.barcode_code);
    alert('Barcode code copied to clipboard!');
  }
};

const copyBarcodeUrl = () => {
  const url = getBarcodeUrl();
  if (url) {
    navigator.clipboard.writeText(url);
    alert('QR Code URL copied to clipboard!');
  }
};

const openBarcodeUrl = () => {
  const url = getBarcodeUrl();
  if (url) {
    window.open(url, '_blank', 'noopener,noreferrer');
  }
};

const viewWebsite = () => {
  if (website.value?.id) {
    const url = router.resolve(`/website/${website.value.id}`).href;
    window.open(url, '_blank');
  } else if (website.value?.subdomain) {
    const url = `http://${website.value.subdomain}.${baseDomain}`;
    window.open(url, '_blank');
  } else if (website.value?.custom_domain) {
    const url = `http://${website.value.custom_domain}`;
    window.open(url, '_blank');
  }
};

const goToAdminDashboard = () => {
  router.push('/admin/dashboard');
};

const changePassword = async () => {
  passwordError.value = '';
  passwordSuccess.value = '';
  
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    passwordError.value = 'New passwords do not match';
    return;
  }
  
  if (passwordForm.value.newPassword.length < 6) {
    passwordError.value = 'Password must be at least 6 characters long';
    return;
  }
  
  // TODO: Implement password change API endpoint
  passwordError.value = 'Password change feature coming soon';
  
  // changingPassword.value = true;
  // try {
  //   await changeAdminPassword(passwordForm.value.currentPassword, passwordForm.value.newPassword);
  //   passwordSuccess.value = 'Password changed successfully!';
  //   passwordForm.value = {
  //     currentPassword: '',
  //     newPassword: '',
  //     confirmPassword: ''
  // };
  // } catch (error) {
  //   passwordError.value = error.message || 'Failed to change password';
  // } finally {
  //   changingPassword.value = false;
  // }
};

// Load notification settings
const loadNotificationSettings = async () => {
  try {
    const settings = await getNotificationSettings();
    notificationSettings.value = {
      notificationsEnabled: settings.notificationsEnabled ?? true,
      emailEnabled: settings.emailEnabled ?? true,
      smsEnabled: settings.smsEnabled ?? false,
      pushEnabled: settings.pushEnabled ?? true,
      whatsappEnabled: settings.whatsappEnabled ?? false,
      notificationEmail: settings.notificationEmail || ''
    };
  } catch (error) {
    console.error('Failed to load notification settings:', error);
    // Use defaults if loading fails
  }
};

// Save notification settings
const saveNotificationSettings = async () => {
  try {
    savingNotifications.value = true;
    notificationError.value = '';
    notificationSuccess.value = '';

    await updateNotificationSettings({
      notificationsEnabled: notificationSettings.value.notificationsEnabled,
      emailEnabled: notificationSettings.value.emailEnabled,
      smsEnabled: notificationSettings.value.smsEnabled,
      pushEnabled: notificationSettings.value.pushEnabled,
      whatsappEnabled: notificationSettings.value.whatsappEnabled,
      notificationEmail: notificationSettings.value.notificationEmail || null
    });

    notificationSuccess.value = t('restaurantDashboard.notificationSettingsSaved');
    setTimeout(() => {
      notificationSuccess.value = '';
    }, 3000);
  } catch (error) {
    console.error('Failed to save notification settings:', error);
    notificationError.value = error.message || t('restaurantDashboard.failedToSaveNotificationSettings');
  } finally {
    savingNotifications.value = false;
  }
};

// Load language settings
const loadLanguageSettings = async () => {
  try {
    if (website.value) {
      // Parse language settings from JSON or use defaults
      let languageData = {
        default_language: 'en',
        languages_enabled: {
          english: true,
          arabic: true
        }
      };

      // Load default_language
      if (website.value.default_language) {
        languageData.default_language = website.value.default_language;
      }

      // Load languages_enabled
      if (website.value.languages_enabled) {
        try {
          const parsed = typeof website.value.languages_enabled === 'string'
            ? JSON.parse(website.value.languages_enabled)
            : website.value.languages_enabled;
          if (parsed && typeof parsed === 'object') {
            languageData.languages_enabled = { ...languageData.languages_enabled, ...parsed };
          }
        } catch (e) {
          console.warn('Error parsing languages_enabled:', e);
        }
      }

      languageSettings.value = languageData;
    }
  } catch (error) {
    console.error('Failed to load language settings:', error);
  }
};

// Handle language toggle - ensure at least one language is enabled
const handleLanguageToggle = (language) => {
  const enabledCount = Object.values(languageSettings.value.languages_enabled).filter(Boolean).length;
  
  // If trying to disable the last enabled language, prevent it
  if (!languageSettings.value.languages_enabled[language] && enabledCount <= 1) {
    languageSettings.value.languages_enabled[language] = true;
    alert(t('restaurantDashboard.atLeastOneLanguageRequired'));
    return;
  }

  // If disabling the default language, switch to the other enabled language
  if (!languageSettings.value.languages_enabled[language]) {
    if (languageSettings.value.default_language === (language === 'english' ? 'en' : 'ar')) {
      const otherLanguage = language === 'english' ? 'arabic' : 'english';
      if (languageSettings.value.languages_enabled[otherLanguage]) {
        languageSettings.value.default_language = otherLanguage === 'english' ? 'en' : 'ar';
      }
    }
  }
};

// Save language settings
const saveLanguageSettings = async () => {
  try {
    savingLanguage.value = true;
    languageError.value = '';
    languageSuccess.value = '';

    // Ensure at least one language is enabled
    const enabledCount = Object.values(languageSettings.value.languages_enabled).filter(Boolean).length;
    if (enabledCount === 0) {
      languageError.value = t('restaurantDashboard.atLeastOneLanguageRequired');
      return;
    }

    // Ensure default language is enabled
    const defaultLangKey = languageSettings.value.default_language === 'en' ? 'english' : 'arabic';
    if (!languageSettings.value.languages_enabled[defaultLangKey]) {
      languageError.value = t('restaurantDashboard.defaultLanguageMustBeEnabled');
      return;
    }

    await updateRestaurantWebsite({
      default_language: languageSettings.value.default_language,
      languages_enabled: JSON.stringify(languageSettings.value.languages_enabled)
    });

    // Update local website object
    if (website.value) {
      website.value.default_language = languageSettings.value.default_language;
      website.value.languages_enabled = languageSettings.value.languages_enabled;
    }

    languageSuccess.value = t('restaurantDashboard.languageSettingsSaved');
    setTimeout(() => {
      languageSuccess.value = '';
    }, 3000);
  } catch (error) {
    console.error('Failed to save language settings:', error);
    languageError.value = error.message || t('restaurantDashboard.failedToSaveLanguageSettings');
  } finally {
    savingLanguage.value = false;
  }
};

// Currency symbol mapping
const currencySymbols = {
  USD: '$',
  JOD: 'د.ا'
};

// Format currency for preview
const formatCurrency = (amount) => {
  const symbol = currencySymbols[currencySettings.value.currency_code] || '$';
  const formattedAmount = parseFloat(amount).toFixed(2);
  
  if (currencySettings.value.symbol_position === 'before') {
    return `${symbol}${formattedAmount}`;
  } else {
    return `${formattedAmount} ${symbol}`;
  }
};

// Load currency settings
const loadCurrencySettings = async () => {
  try {
    if (website.value) {
      let currencyData = {
        currency_code: 'USD',
        symbol_position: 'before'
      };

      // Load currency_code
      if (website.value.currency_code) {
        currencyData.currency_code = website.value.currency_code;
      }

      // Load symbol_position
      if (website.value.currency_symbol_position) {
        currencyData.symbol_position = website.value.currency_symbol_position;
      }

      currencySettings.value = currencyData;
    }
  } catch (error) {
    console.error('Failed to load currency settings:', error);
  }
};

// Save currency settings
const saveCurrencySettings = async () => {
  try {
    savingCurrency.value = true;
    currencyError.value = '';
    currencySuccess.value = '';

    await updateRestaurantWebsite({
      currency_code: currencySettings.value.currency_code,
      currency_symbol_position: currencySettings.value.symbol_position
    });

    // Update local website object
    if (website.value) {
      website.value.currency_code = currencySettings.value.currency_code;
      website.value.currency_symbol_position = currencySettings.value.symbol_position;
    }

    currencySuccess.value = t('restaurantDashboard.currencySettingsSaved');
    setTimeout(() => {
      currencySuccess.value = '';
    }, 3000);
  } catch (error) {
    console.error('Failed to save currency settings:', error);
    currencyError.value = error.message || t('restaurantDashboard.failedToSaveCurrencySettings');
  } finally {
    savingCurrency.value = false;
  }
};

// Load tax requirement settings
const loadTaxRequirement = async () => {
  try {
    if (website.value) {
      taxRequirement.value = {
        enabled: website.value.tax_enabled === 1 || website.value.tax_enabled === true || false,
        rate: website.value.tax_rate ? parseFloat(website.value.tax_rate) || 0 : 0
      };
    }
  } catch (error) {
    console.error('Failed to load tax requirement:', error);
  }
};

// Business hours: load from API (7 days)
const loadBusinessHours = async () => {
  try {
    const hours = await getBusinessHours();
    if (Array.isArray(hours) && hours.length === 7) {
      businessHours.value = hours.map(h => ({
        id: h.id,
        day_of_week: h.day_of_week,
        open_time: h.open_time || null,
        close_time: h.close_time || null,
        is_closed: Boolean(h.is_closed)
      }));
    }
  } catch (error) {
    console.error('Failed to load business hours:', error);
    businessHoursError.value = error.message || t('restaurantDashboard.businessHoursError');
  }
};

const saveBusinessHours = async () => {
  try {
    savingBusinessHours.value = true;
    businessHoursError.value = '';
    businessHoursSuccess.value = '';
    const payload = businessHours.value.map(day => ({
      day_of_week: day.day_of_week,
      open_time: day.is_closed ? null : (day.open_time || null),
      close_time: day.is_closed ? null : (day.close_time || null),
      is_closed: day.is_closed
    }));
    await updateBusinessHours(payload);
    businessHoursSuccess.value = t('restaurantDashboard.businessHoursSaved');
    setTimeout(() => { businessHoursSuccess.value = ''; }, 3000);
  } catch (error) {
    console.error('Failed to save business hours:', error);
    businessHoursError.value = error.message || t('restaurantDashboard.businessHoursError');
  } finally {
    savingBusinessHours.value = false;
  }
};

// Offers – auto title for percent_off and free_delivery_over_x_jod
const offerTitleAuto = computed(() => {
  const type = offerForm.value.offer_type;
  if (type === 'percent_off') {
    const val = offerForm.value.value || '0';
    const scopeLabel = offerForm.value.offer_scope === 'selected_items'
      ? t('restaurantDashboard.offerTitleSelectedItems')
      : t('restaurantDashboard.offerTitleAllItems');
    return `${val}% ${t('restaurantDashboard.offerDiscountOn')} ${scopeLabel}`;
  }
  if (type === 'free_delivery_over_x_jod') {
    const val = offerForm.value.value || '0';
    return `${t('restaurantDashboard.offerTitleFreeDeliveryOver')} ${val} ${t('restaurantDashboard.offerTitleJOD')}`;
  }
  return '';
});
const loadOffers = async () => {
  try {
    offers.value = await getRestaurantOffers();
  } catch (error) {
    console.error('Failed to load offers:', error);
  }
};
const resetOfferForm = () => {
  offerForm.value = {
    offer_type: 'free_delivery',
    offer_scope: 'all_items',
    selected_product_ids: [],
    selected_addon_ids: [],
    title: '',
    description: '',
    value: '',
    min_order_value: '',
    is_active: true,
    valid_from: '',
    valid_until: ''
  };
  productsWithAddons.value = [];
  offerError.value = '';
  offerSuccess.value = '';
};
const loadProductsWithAddonsForOffer = async () => {
  try {
    loadingProductsWithAddons.value = true;
    productsWithAddons.value = await getProductsWithAddons();
  } catch (error) {
    console.error('Failed to load products with addons:', error);
    offerError.value = error.message || t('restaurantDashboard.offerError');
  } finally {
    loadingProductsWithAddons.value = false;
  }
};
const toggleOfferProduct = (productId) => {
  const ids = [...(offerForm.value.selected_product_ids || [])];
  const idx = ids.indexOf(productId);
  if (idx === -1) ids.push(productId);
  else ids.splice(idx, 1);
  offerForm.value.selected_product_ids = ids;
};
const toggleOfferAddon = (addonId) => {
  const ids = [...(offerForm.value.selected_addon_ids || [])];
  const idx = ids.indexOf(addonId);
  if (idx === -1) ids.push(addonId);
  else ids.splice(idx, 1);
  offerForm.value.selected_addon_ids = ids;
};
const formatOfferType = (type) => {
  const key = {
    free_delivery: 'restaurantDashboard.offerFreeDelivery',
    free_delivery_over_x_jod: 'restaurantDashboard.offerFreeDeliveryOverX',
    percent_off: 'restaurantDashboard.offerPercentOff',
    minimum_order_value: 'restaurantDashboard.offerMinOrderValue'
  }[type];
  return key ? t(key) : type;
};
const editOffer = (offer) => {
  editingOffer.value = offer;
  const productIds = Array.isArray(offer.selected_product_ids) ? offer.selected_product_ids : (typeof offer.selected_product_ids === 'string' ? (() => { try { return JSON.parse(offer.selected_product_ids || '[]'); } catch { return []; } })() : []);
  const addonIds = Array.isArray(offer.selected_addon_ids) ? offer.selected_addon_ids : (typeof offer.selected_addon_ids === 'string' ? (() => { try { return JSON.parse(offer.selected_addon_ids || '[]'); } catch { return []; } })() : []);
  offerForm.value = {
    offer_type: offer.offer_type,
    offer_scope: offer.offer_scope === 'selected_items' ? 'selected_items' : 'all_items',
    selected_product_ids: productIds,
    selected_addon_ids: addonIds,
    title: offer.title,
    description: offer.description || '',
    value: offer.value != null ? String(offer.value) : '',
    min_order_value: offer.min_order_value != null ? String(offer.min_order_value) : '',
    is_active: Boolean(offer.is_active),
    valid_from: offer.valid_from ? offer.valid_from.slice(0, 10) : '',
    valid_until: offer.valid_until ? offer.valid_until.slice(0, 10) : ''
  };
  showOfferForm.value = true;
  offerError.value = '';
  offerSuccess.value = '';
  if (offer.offer_type === 'percent_off' && offer.offer_scope === 'selected_items' && productsWithAddons.value.length === 0) {
    loadProductsWithAddonsForOffer();
  }
};
const saveOffer = async () => {
  try {
    savingOffer.value = true;
    offerError.value = '';
    offerSuccess.value = '';
    const payload = {
      offer_type: offerForm.value.offer_type,
      title: offerForm.value.title.trim(),
      description: offerForm.value.description?.trim() || null,
      value: offerForm.value.offer_type !== 'free_delivery' && offerForm.value.value ? parseFloat(offerForm.value.value) : null,
      min_order_value: offerForm.value.min_order_value ? parseFloat(offerForm.value.min_order_value) : null,
      is_active: offerForm.value.is_active,
      valid_from: offerForm.value.valid_from,
      valid_until: offerForm.value.valid_until
    };
    if (offerForm.value.offer_type === 'percent_off') {
      payload.offer_scope = offerForm.value.offer_scope || 'all_items';
      payload.selected_product_ids = Array.isArray(offerForm.value.selected_product_ids) ? offerForm.value.selected_product_ids : [];
      payload.selected_addon_ids = Array.isArray(offerForm.value.selected_addon_ids) ? offerForm.value.selected_addon_ids : [];
    }
    if (editingOffer.value) {
      await updateOffer(editingOffer.value.id, payload);
      offerSuccess.value = t('restaurantDashboard.offerUpdated');
    } else {
      await createOffer(payload);
      offerSuccess.value = t('restaurantDashboard.offerCreated');
    }
    await loadOffers();
    setTimeout(() => {
      showOfferForm.value = false;
      editingOffer.value = null;
      resetOfferForm();
      offerSuccess.value = '';
    }, 1500);
  } catch (error) {
    offerError.value = error.message || t('restaurantDashboard.offerError');
  } finally {
    savingOffer.value = false;
  }
};
const deleteOffer = async (id) => {
  if (!confirm(t('restaurantDashboard.deleteOfferConfirm'))) return;
  try {
    await deleteOfferAPI(id);
    await loadOffers();
  } catch (error) {
    alert('Failed to delete offer: ' + error.message);
  }
};

// Load payment methods settings
const loadPaymentMethods = async () => {
  try {
    if (website.value) {
      // Parse payment methods from JSON or use defaults
      let paymentMethodsData = {
        cashOnPickup: true,
        cashOnDelivery: true,
        creditCard: false,
        onlinePayment: false,
        mobilePayment: false,
        cliQServices: {
          enabled: false,
          allowNamePhone: false,
          phone: '',
          name: ''
        }
      };

      if (website.value.payment_methods) {
        try {
          const parsed = typeof website.value.payment_methods === 'string' 
            ? JSON.parse(website.value.payment_methods) 
            : website.value.payment_methods;
          const cs = parsed.cliQServices || {};
          paymentMethodsData = { 
            ...paymentMethodsData, 
            ...parsed,
            cliQServices: {
              enabled: !!cs.enabled,
              allowNamePhone: !!cs.allowNamePhone,
              name: cs.name || '',
              phone: cs.phone || ''
            }
          };
        } catch (e) {
          console.warn('Failed to parse payment methods:', e);
        }
      }

      paymentMethods.value = paymentMethodsData;
    }
  } catch (error) {
    console.error('Failed to load payment methods:', error);
  }
};

// When first checkbox (CliQ Services) is unchecked, clear second checkbox and name/phone
const onCliQEnabledChange = (e) => {
  if (!e.target.checked) {
    paymentMethods.value.cliQServices.allowNamePhone = false;
    paymentMethods.value.cliQServices.name = '';
    paymentMethods.value.cliQServices.phone = '';
  }
};

// When second checkbox (Allow name/phone) is unchecked, clear name and phone
const onCliQAllowNamePhoneChange = (e) => {
  if (!e.target.checked) {
    paymentMethods.value.cliQServices.name = '';
    paymentMethods.value.cliQServices.phone = '';
  }
};

// Save payment methods settings
const savePaymentMethods = async () => {
  try {
    savingPaymentMethods.value = true;
    paymentMethodsError.value = '';
    paymentMethodsSuccess.value = '';

    // If CliQ is disabled, clear all; if allowNamePhone is disabled, clear name and phone before saving
    const cs = paymentMethods.value.cliQServices || {};
    if (!cs.enabled) {
      paymentMethods.value.cliQServices = {
        enabled: false,
        allowNamePhone: false,
        name: '',
        phone: ''
      };
    } else if (!cs.allowNamePhone) {
      paymentMethods.value.cliQServices = {
        ...cs,
        name: '',
        phone: ''
      };
    }

    // Update website with payment methods settings
    await updateRestaurantWebsite({
      payment_methods: JSON.stringify(paymentMethods.value)
    });

    // Update local website object
    if (website.value) {
      website.value.payment_methods = JSON.stringify(paymentMethods.value);
    }

    paymentMethodsSuccess.value = t('restaurantDashboard.paymentMethodsSaved');
    setTimeout(() => {
      paymentMethodsSuccess.value = '';
    }, 3000);
  } catch (error) {
    console.error('Failed to save payment methods:', error);
    paymentMethodsError.value = error.message || t('restaurantDashboard.failedToSavePaymentMethods');
  } finally {
    savingPaymentMethods.value = false;
  }
};

// Load coupons
const loadCoupons = async () => {
  try {
    coupons.value = await getRestaurantCoupons();
  } catch (error) {
    console.error('Failed to load coupons:', error);
  }
};

// Reset coupon form
const resetCouponForm = () => {
  couponForm.value = {
    code: '',
    description: '',
    discount_type: 'percentage',
    discount_value: '',
    min_order_amount: '',
    max_discount_amount: '',
    valid_from: '',
    valid_until: '',
    usage_limit: '',
    is_active: true
  };
};

// Edit coupon
const editCoupon = (coupon) => {
  editingCoupon.value = coupon;
  couponForm.value = {
    code: coupon.code,
    description: coupon.description || '',
    discount_type: coupon.discount_type,
    discount_value: coupon.discount_value,
    min_order_amount: coupon.min_order_amount || '',
    max_discount_amount: coupon.max_discount_amount || '',
    valid_from: coupon.valid_from,
    valid_until: coupon.valid_until,
    usage_limit: coupon.usage_limit || '',
    is_active: coupon.is_active
  };
  showCouponForm.value = true;
};

// Save coupon (create or update)
const saveCoupon = async () => {
  try {
    savingCoupon.value = true;
    couponError.value = '';
    couponSuccess.value = '';

    const couponData = {
      code: couponForm.value.code,
      description: couponForm.value.description || null,
      discount_type: couponForm.value.discount_type,
      discount_value: parseFloat(couponForm.value.discount_value),
      min_order_amount: parseFloat(couponForm.value.min_order_amount) || 0,
      max_discount_amount: couponForm.value.max_discount_amount ? parseFloat(couponForm.value.max_discount_amount) : null,
      valid_from: couponForm.value.valid_from,
      valid_until: couponForm.value.valid_until,
      usage_limit: couponForm.value.usage_limit ? parseInt(couponForm.value.usage_limit) : null,
      is_active: couponForm.value.is_active
    };

    if (editingCoupon.value) {
      await updateCoupon(editingCoupon.value.id, couponData);
      couponSuccess.value = t('restaurantDashboard.couponUpdated');
    } else {
      await createCoupon(couponData);
      couponSuccess.value = t('restaurantDashboard.couponCreated');
    }

    await loadCoupons();
    setTimeout(() => {
      showCouponForm.value = false;
      editingCoupon.value = null;
      resetCouponForm();
      couponSuccess.value = '';
    }, 2000);
  } catch (error) {
    console.error('Failed to save coupon:', error);
    couponError.value = error.message || t('restaurantDashboard.failedToSaveCoupon');
  } finally {
    savingCoupon.value = false;
  }
};

// Delete coupon
const deleteCoupon = async (couponId) => {
  if (!confirm(t('restaurantDashboard.confirmDeleteCoupon'))) {
    return;
  }

  try {
    await deleteCouponAPI(couponId);
    await loadCoupons();
  } catch (error) {
    alert('Failed to delete coupon: ' + error.message);
  }
};

// Save tax requirement settings
const saveTaxRequirement = async () => {
  try {
    savingTax.value = true;
    taxError.value = '';
    taxSuccess.value = '';

    // Update website with tax settings
    await updateRestaurantWebsite({
      tax_enabled: taxRequirement.value.enabled,
      tax_rate: taxRequirement.value.enabled ? (taxRequirement.value.rate || 0) : 0
    });

    // Update local website object
    if (website.value) {
      website.value.tax_enabled = taxRequirement.value.enabled;
      website.value.tax_rate = taxRequirement.value.rate || 0;
    }

    taxSuccess.value = t('restaurantDashboard.taxRequirementSaved');
    setTimeout(() => {
      taxSuccess.value = '';
    }, 3000);
  } catch (error) {
    console.error('Failed to save tax requirement:', error);
    taxError.value = error.message || t('restaurantDashboard.failedToSaveTaxRequirement');
  } finally {
    savingTax.value = false;
  }
};

// Save order type settings
const saveOrderTypeSettings = async () => {
  try {
    savingOrderTypes.value = true;
    orderTypeError.value = '';
    orderTypeSuccess.value = '';

    // Save order types as part of website update
    const orderTypesData = {
      dineInEnabled: orderTypeSettings.value.dineInEnabled,
      pickupEnabled: orderTypeSettings.value.pickupEnabled,
      deliveryEnabled: orderTypeSettings.value.deliveryEnabled,
      deliveryFee: orderTypeSettings.value.deliveryFee || 0,
      deliveryTimeMin: orderTypeSettings.value.deliveryTimeMin ?? null,
      deliveryTimeMax: orderTypeSettings.value.deliveryTimeMax ?? null,
      deliveryMode: orderTypeSettings.value.deliveryMode || 'fixed_fee'
    };

    // Update website with order types, delivery fee, delivery time range, and delivery mode
    await updateRestaurantWebsite({
      order_types: orderTypesData,
      delivery_fee: orderTypeSettings.value.deliveryFee || 0,
      delivery_time_min: orderTypeSettings.value.deliveryTimeMin ?? null,
      delivery_time_max: orderTypeSettings.value.deliveryTimeMax ?? null,
      delivery_mode: orderTypeSettings.value.deliveryMode || 'fixed_fee',
      delivery_company_id: orderTypeSettings.value.deliveryMode === 'delivery_company' ? (website.value?.delivery_company_id ?? null) : null
    });

    // Update local website object
    if (website.value) {
      website.value.order_type_dine_in_enabled = orderTypesData.dineInEnabled;
      website.value.order_type_pickup_enabled = orderTypesData.pickupEnabled;
      website.value.order_type_delivery_enabled = orderTypesData.deliveryEnabled;
      website.value.delivery_fee = orderTypeSettings.value.deliveryFee || 0;
      website.value.delivery_time_min = orderTypeSettings.value.deliveryTimeMin ?? null;
      website.value.delivery_time_max = orderTypeSettings.value.deliveryTimeMax ?? null;
      website.value.delivery_mode = orderTypeSettings.value.deliveryMode || 'fixed_fee';
    }

    orderTypeSuccess.value = t('restaurantDashboard.orderTypeSettingsSaved');
    setTimeout(() => {
      orderTypeSuccess.value = '';
    }, 3000);
  } catch (error) {
    console.error('Failed to save order type settings:', error);
    orderTypeError.value = error.message || t('restaurantDashboard.failedToSaveOrderTypeSettings');
  } finally {
    savingOrderTypes.value = false;
  }
};

const handleLogout = () => {
  localStorage.removeItem('restaurantToken');
  localStorage.removeItem('restaurantRefreshToken');
  localStorage.removeItem('restaurantInfo');
  closeSSE();
  router.push('/restaurant/login');
};

// Branches functions
async function loadRegions() {
  loadingRegions.value = true;
  try {
    regions.value = await getRestaurantRegions();
  } catch (error) {
    console.error('Failed to load regions:', error);
    branchError.value = error.message || 'Failed to load regions';
  } finally {
    loadingRegions.value = false;
  }
}

async function loadBranches() {
  loadingBranches.value = true;
  try {
    branches.value = await getRestaurantBranches();
  } catch (error) {
    console.error('Failed to load branches:', error);
    branchError.value = error.message || 'Failed to load branches';
  } finally {
    loadingBranches.value = false;
  }
}

function openBranchForm(branch = null) {
  editingBranch.value = branch;
  if (branch) {
    branchForm.value = {
      branch_number: branch.branch_number,
      region_id: branch.region_id,
      name: branch.name || '',
      name_ar: branch.name_ar || '',
      address: branch.address || '',
      phone: branch.phone || '',
      status: branch.status || 'active'
    };
  } else {
    branchForm.value = {
      branch_number: '',
      region_id: '',
      name: '',
      name_ar: '',
      address: '',
      phone: '',
      status: 'active'
    };
  }
  showBranchForm.value = true;
  branchError.value = '';
}

function closeBranchForm() {
  showBranchForm.value = false;
  editingBranch.value = null;
  branchForm.value = {
    branch_number: '',
    region_id: '',
    name: '',
    name_ar: '',
    address: '',
    phone: '',
    status: 'active'
  };
  branchError.value = '';
}

async function saveBranch() {
  if (!branchForm.value.branch_number || !branchForm.value.region_id) {
    branchError.value = 'Branch number and region are required';
    return;
  }

  savingBranch.value = true;
  branchError.value = '';
  try {
    if (editingBranch.value) {
      await updateRestaurantBranch(editingBranch.value.id, branchForm.value);
    } else {
      await createRestaurantBranch(branchForm.value);
    }
    await loadBranches();
    closeBranchForm();
  } catch (error) {
    console.error('Failed to save branch:', error);
    branchError.value = error.message || 'Failed to save branch';
  } finally {
    savingBranch.value = false;
  }
}

async function deleteBranch(id) {
  if (!confirm('Are you sure you want to delete this branch?')) return;
  try {
    await deleteRestaurantBranch(id);
    await loadBranches();
  } catch (error) {
    console.error('Failed to delete branch:', error);
    alert(error.message || 'Failed to delete branch');
  }
}

function editBranch(branch) {
  openBranchForm(branch);
}

// Watch for tab changes to load products when needed
watch(activeTab, (newTab) => {
  if (newTab === 'menu' && products.value.length === 0 && !loadingProducts.value) {
    loadProducts();
  }
  if (newTab === 'businessHours') {
    loadBusinessHours();
  }
  if (newTab === 'offers') {
    loadOffers();
  }
  if (newTab === 'orderType') {
    loadDeliveryCompaniesList();
    loadDeliveryCompanyRequest();
  }
  if (newTab === 'branches') {
    if (branches.value.length === 0 && !loadingBranches.value) {
      loadBranches();
    }
    if (regions.value.length === 0 && !loadingRegions.value) {
      loadRegions();
    }
  }
});
watch(() => offerForm.value.offer_scope, (scope) => {
  if (scope === 'selected_items' && productsWithAddons.value.length === 0 && !loadingProductsWithAddons.value) {
    loadProductsWithAddonsForOffer();
  }
});
// Keep offer title in sync for percent_off and free_delivery_over_x_jod so save payload is correct
watch(
  () => [offerForm.value.offer_type, offerForm.value.value, offerForm.value.offer_scope],
  () => {
    if (offerForm.value.offer_type === 'percent_off' || offerForm.value.offer_type === 'free_delivery_over_x_jod') {
      offerForm.value.title = offerTitleAuto.value;
    }
  },
  { immediate: true }
);

onMounted(async () => {
  const token = localStorage.getItem('restaurantToken');
  if (!token) {
    router.push('/restaurant/login');
    return;
  }

  await loadRestaurantData();
  await loadOrders();
  await loadNotificationSettings();
  await loadTaxRequirement();
  await loadPaymentMethods();
  await loadLanguageSettings();
  await loadCurrencySettings();
  await loadCoupons();
  await loadOffers();
  setupSSE();
  
  // Load products if products tab is active
  if (activeTab.value === 'products') {
    loadProducts();
  }
});

onUnmounted(() => {
  closeSSE();
});
</script>

<style scoped>
.notification-enter-active {
  transition: all 0.3s ease-out;
}

.notification-leave-active {
  transition: all 0.3s ease-in;
}

.notification-enter-from {
  opacity: 0;
  transform: translateX(100%);
}

.notification-leave-to {
  opacity: 0;
  transform: translateX(100%);
}

@keyframes slide-in {
  from {
    opacity: 0;
    transform: translateX(100%);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.animate-slide-in {
  animation: slide-in 0.3s ease-out;
}
</style>

