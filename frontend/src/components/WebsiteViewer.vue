<template>
  <div v-if="loading" class="min-h-screen flex items-center justify-center bg-gray-50">
    <div class="text-center">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto"></div>
      <p class="mt-4 text-gray-600">{{ $t('website.loading') }}</p>
    </div>
  </div>

  <div v-else-if="website" class="min-h-screen bg-white" :style="websiteStyles" :dir="$i18n.locale === 'ar' ? 'rtl' : 'ltr'" :lang="$i18n.locale">
    <!-- Navigation Header -->
    <header class="fixed top-0 left-0 right-0 z-30 bg-white/98 backdrop-blur-md shadow-lg border-b border-gray-100" role="banner">
      <nav class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8" role="navigation" aria-label="Main navigation">
        <div class="flex items-center justify-between h-20">
          <a href="#" @click.prevent="scrollToTop" class="flex items-center gap-3 hover:opacity-80 transition-all group">
            <div class="relative">
              <img
                v-if="website.logo_url"
                :src="website.logo_url"
                :alt="displayRestaurantName"
                class="h-14 w-14 object-contain transition-transform group-hover:scale-110"
              />
              <div v-else class="h-14 w-14 rounded-full flex items-center justify-center text-2xl font-bold text-white shadow-lg" :style="{ backgroundColor: website.primary_color }">
                {{ (displayRestaurantName || ' ').charAt(0) }}
              </div>
            </div>
            <h1 class="text-base font-semibold bg-gradient-to-r bg-clip-text text-transparent" :style="{ backgroundImage: `linear-gradient(135deg, ${website.primary_color}, ${website.secondary_color})` }">
              {{ displayRestaurantName }}
            </h1>
          </a>
          <div class="hidden md:flex items-center gap-8" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
            <LanguageSwitcher v-if="shouldShowLanguageSwitcher" :available-languages="availableLanguages" />
            <a v-if="menuItems && menuItems.length > 0 || products.length > 0" href="#menu" class="text-gray-700 hover:text-gray-900 font-medium transition-all relative group">
              {{ $t('website.menu') }}
              <span class="absolute bottom-0 left-0 w-0 h-0.5 transition-all group-hover:w-full" :style="{ backgroundColor: website.primary_color }"></span>
            </a>
            <a v-if="displayDescription" href="#about" class="text-gray-700 hover:text-gray-900 font-medium transition-all relative group">
              {{ $t('website.about') }}
              <span class="absolute bottom-0 left-0 w-0 h-0.5 transition-all group-hover:w-full" :style="{ backgroundColor: website.primary_color }"></span>
            </a>
            <a v-if="galleryImages && galleryImages.length > 0" href="#gallery" class="text-gray-700 hover:text-gray-900 font-medium transition-all relative group">
              {{ $t('website.gallery') }}
              <span class="absolute bottom-0 left-0 w-0 h-0.5 transition-all group-hover:w-full" :style="{ backgroundColor: website.primary_color }"></span>
            </a>
            <a v-if="locations && locations.length > 0" href="#locations" class="text-gray-700 hover:text-gray-900 font-medium transition-all relative group">
              {{ $t('website.locations') }}
              <span class="absolute bottom-0 left-0 w-0 h-0.5 transition-all group-hover:w-full" :style="{ backgroundColor: website.primary_color }"></span>
            </a>
            <a v-if="displayAddress || website.phone || website.email" href="#contact" class="text-gray-700 hover:text-gray-900 font-medium transition-all relative group">
              {{ $t('website.contact') }}
              <span class="absolute bottom-0 left-0 w-0 h-0.5 transition-all group-hover:w-full" :style="{ backgroundColor: website.primary_color }"></span>
            </a>
            <router-link
              :to="`/website/${route.params.id}/track`"
              class="text-gray-700 hover:text-gray-900 font-medium transition-all relative group"
            >
              {{ $t('website.trackOrder') }}
              <span class="absolute bottom-0 left-0 w-0 h-0.5 transition-all group-hover:w-full" :style="{ backgroundColor: website.primary_color }"></span>
            </router-link>
            <button
              @click="scrollToMenu"
              :style="{ 
                background: `linear-gradient(135deg, ${website.primary_color}, ${website.secondary_color})`,
                boxShadow: `0 4px 15px ${website.primary_color}40`
              }"
              class="px-6 py-2.5 text-white rounded-full font-semibold hover:scale-105 transition-all shadow-lg"
            >
              {{ $t('website.orderNow') }}
            </button>
          </div>
          <div class="md:hidden flex items-center gap-2">
            <LanguageSwitcher v-if="shouldShowLanguageSwitcher" :available-languages="availableLanguages" />
            <button
              @click="$router.push('/')"
              class="px-4 py-2 text-sm bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors"
            >
              {{ $t('website.back') }}
            </button>
          </div>
        </div>
      </nav>
    </header>
    
    <!-- Shopping Cart Sidebar -->
    <aside
      :class="[
        'fixed top-0 h-full w-96 bg-white shadow-2xl z-50 transform transition-transform duration-300 ease-in-out overflow-hidden border-gray-200',
        $i18n.locale === 'ar' 
          ? (showCart ? 'left-0 translate-x-0 border-r' : '-left-full -translate-x-full')
          : (showCart ? 'right-0 translate-x-0 border-l' : '-right-full translate-x-full')
      ]"
    >
      <div class="h-full flex flex-col">
        <div class="p-6 border-b border-gray-200 flex items-center justify-between bg-gradient-to-r flex-shrink-0" :style="{ background: `linear-gradient(135deg, ${website.primary_color} 0%, ${website.secondary_color} 100%)` }">
          <h3 class="text-2xl font-bold text-white">{{ $t('website.shoppingCart') }}</h3>
          <button @click="showCart = false" class="text-white hover:text-gray-200 text-2xl font-bold w-8 h-8 flex items-center justify-center rounded-full hover:bg-white/20 transition-colors">×</button>
        </div>
        
        <div class="flex-1 overflow-y-auto p-6 bg-gray-50">
          <div v-if="cart.length === 0" class="text-center py-12">
            <div class="text-6xl mb-4">🛒</div>
            <p class="text-gray-500 text-lg mb-2">{{ $t('website.cartEmpty') }}</p>
            <p class="text-sm text-gray-400">{{ $t('website.cartEmptyHint') }}</p>
            <button
              @click="showCart = false"
              :style="{ backgroundColor: website.primary_color }"
              class="mt-4 px-6 py-2 text-white rounded-lg font-semibold hover:opacity-90 transition-opacity"
            >
              {{ $t('website.browseMenu') }}
            </button>
          </div>
          
          <div v-else class="space-y-4">
            <div
              v-for="(item, index) in cart"
              :key="index"
              class="bg-white rounded-lg shadow-md p-4 border border-gray-200 hover:shadow-lg transition-shadow"
            >
              <div class="flex gap-4">
                <div v-if="item.image_url" class="w-20 h-20 flex-shrink-0">
                  <img :src="item.image_url" :alt="`${getProductDisplay(item).name} - ${displayRestaurantName}`" class="w-full h-full object-cover rounded-lg" loading="lazy" />
                </div>
                <div class="flex-1 min-w-0">
                  <h4 class="font-semibold text-gray-800 mb-1">{{ getProductDisplay(item).name }}</h4>
                  <p class="text-sm text-gray-500 mb-2">{{ formatCurrency(item.price) }} {{ $t('website.each') }}</p>
                  <div class="flex items-center gap-3">
                    <div class="flex items-center gap-2 border border-gray-300 rounded-lg">
                      <button
                        @click="updateQuantity(index, item.quantity - 1)"
                        class="w-8 h-8 flex items-center justify-center hover:bg-gray-100 rounded-l-lg transition-colors"
                        :style="{ color: website.primary_color }"
                      >
                        −
                      </button>
                      <span class="w-8 text-center font-semibold">{{ item.quantity }}</span>
                      <button
                        @click="updateQuantity(index, item.quantity + 1)"
                        class="w-8 h-8 flex items-center justify-center hover:bg-gray-100 rounded-r-lg transition-colors"
                        :style="{ color: website.primary_color }"
                      >
                        +
                      </button>
                    </div>
                    <button
                      @click="removeFromCart(index)"
                      class="ml-auto text-red-500 hover:text-red-700 text-sm font-medium"
                    >
                      {{ $t('website.remove') }}
                    </button>
                  </div>
                  <p class="text-sm font-semibold mt-2" :style="{ color: website.primary_color }">
                    {{ $t('website.subtotal') }}: {{ formatCurrency(parseFloat(item.price) * item.quantity) }}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div v-if="cart.length > 0" class="p-6 border-t border-gray-200 bg-white flex-shrink-0">
          <div class="space-y-3 mb-4">
            <div class="flex items-center justify-between text-gray-600">
              <span>{{ $t('website.subtotal') }}</span>
              <span>{{ formatCurrency(cartTotal) }}</span>
            </div>
            <div v-if="website.tax_enabled && taxAmount > 0" class="flex items-center justify-between text-gray-600">
              <span>{{ $t('website.tax') }}</span>
              <span>{{ formatCurrency(taxAmount) }}</span>
            </div>
            <div class="flex items-center justify-between text-xl font-bold pt-3 border-t border-gray-200" :style="{ color: website.primary_color }">
              <span>{{ $t('website.total') }}</span>
              <span>{{ formatCurrency(finalTotal) }}</span>
            </div>
          </div>
          <button
            @click="goToCheckout"
            :style="{ backgroundColor: website.primary_color }"
            class="w-full px-6 py-4 text-white rounded-lg font-semibold hover:opacity-90 transition-opacity shadow-lg"
          >
            {{ $t('website.proceedToCheckout') }} →
          </button>
        </div>
      </div>
    </aside>
    
    <!-- Overlay when cart is open (mobile only) -->
    <div
      v-if="showCart && windowWidth < 640"
      class="fixed inset-0 bg-black bg-opacity-50 z-40"
      @click="showCart = false"
    ></div>
    
    <!-- Main Content Area (Full Width - Not Affected by Sidebar) -->
    <div class="w-full">

    <!-- Main Content -->
    <main role="main">
    <!-- Hero Section -->
    <section class="relative pt-32 pb-24 px-4 sm:px-6 lg:px-8 overflow-hidden min-h-[90vh] flex items-center" aria-label="Hero section">
      <!-- Animated Background -->
      <div class="absolute inset-0 overflow-hidden">
        <div class="absolute inset-0 bg-gradient-to-br opacity-10" :style="{ 
          background: `linear-gradient(135deg, ${website.primary_color} 0%, ${website.secondary_color} 100%)` 
        }"></div>
        <div class="absolute top-20 left-10 w-72 h-72 rounded-full blur-3xl opacity-20 animate-pulse" :style="{ backgroundColor: website.primary_color }"></div>
        <div class="absolute bottom-20 right-10 w-96 h-96 rounded-full blur-3xl opacity-20 animate-pulse delay-1000" :style="{ backgroundColor: website.secondary_color }"></div>
      </div>
      
      <div class="max-w-7xl mx-auto relative z-10 w-full">
        <div class="text-center max-w-5xl mx-auto">
          <div class="mb-8 animate-fade-in">
            <h2 class="text-7xl md:text-8xl lg:text-9xl font-black mb-6 leading-tight bg-gradient-to-r bg-clip-text text-transparent animate-gradient" :style="{ 
              backgroundImage: `linear-gradient(135deg, ${website.primary_color}, ${website.secondary_color})`
            }">
              {{ displayRestaurantName }}
            </h2>
            <p v-if="shortDescription" class="text-2xl md:text-3xl lg:text-4xl text-gray-700 mb-6 font-light leading-relaxed">
              {{ shortDescription }}
            </p>
            <a
              v-if="hasLongDescription"
              href="#about"
              class="inline-flex items-center gap-2 text-lg font-semibold mb-10 hover:gap-4 transition-all group"
              :style="{ color: website.primary_color }"
            >
              {{ $t('website.readMore') }}
              <span class="transition-transform group-hover:translate-x-1" :class="$i18n.locale === 'ar' ? 'group-hover:-translate-x-1' : 'group-hover:translate-x-1'">→</span>
            </a>
          </div>
          
          <div class="flex flex-col sm:flex-row gap-5 justify-center items-center animate-fade-in-up">
            <button
              @click="scrollToMenu"
              :style="{ 
                background: `linear-gradient(135deg, ${website.primary_color}, ${website.secondary_color})`,
                boxShadow: `0 10px 30px ${website.primary_color}50`
              }"
              class="px-10 py-5 text-white rounded-full text-lg font-bold hover:scale-110 transition-all shadow-2xl relative overflow-hidden group"
            >
              <span class="relative z-10">{{ $t('website.orderOnline') }}</span>
              <div class="absolute inset-0 bg-white opacity-0 group-hover:opacity-20 transition-opacity"></div>
            </button>
            <button
              @click="scrollToMenu"
              :style="{ 
                borderColor: website.primary_color,
                color: website.primary_color,
                borderWidth: '3px'
              }"
              class="px-10 py-5 rounded-full text-lg font-bold hover:scale-110 transition-all bg-white/80 backdrop-blur-sm shadow-xl hover:shadow-2xl"
            >
              {{ $t('website.viewMenu') }}
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- Menu Section -->
    <section id="menu" class="py-24 px-4 sm:px-6 lg:px-8 bg-gradient-to-b from-gray-50 to-white relative overflow-hidden" aria-label="Menu section">
      <div class="absolute inset-0 opacity-5">
        <div class="absolute top-0 left-0 w-full h-full" :style="{ 
          backgroundImage: `radial-gradient(circle at 20% 50%, ${website.primary_color} 0%, transparent 50%),
                          radial-gradient(circle at 80% 80%, ${website.secondary_color} 0%, transparent 50%)`
        }"></div>
      </div>
      
      <div class="max-w-7xl mx-auto relative z-10">
        <div class="text-center mb-20">
          <div class="inline-block mb-4">
            <span class="text-sm font-semibold uppercase tracking-wider px-4 py-2 rounded-full" :style="{ 
              backgroundColor: `${website.primary_color}15`,
              color: website.primary_color
            }">
              {{ $t('website.ourMenu') }}
            </span>
          </div>
          <h3 class="text-6xl md:text-7xl font-black mb-6 bg-gradient-to-r bg-clip-text text-transparent" :style="{ 
            backgroundImage: `linear-gradient(135deg, ${website.primary_color}, ${website.secondary_color})`
          }">
            {{ $t('website.deliciousSelection') }}
          </h3>
          <p class="text-xl md:text-2xl text-gray-600 max-w-2xl mx-auto">{{ $t('website.deliciousSelectionDesc') }}</p>
        </div>
        
        <!-- Products by Category -->
        <div v-if="products.length > 0">
          <div v-for="(categoryProducts, category) in orderedProductsByCategory" :key="category" class="mb-20">
            <div class="flex items-center gap-4 mb-10">
              <div class="h-1 flex-1 rounded-full" :style="{ backgroundColor: `${website.primary_color}30` }"></div>
              <h4 class="text-4xl md:text-5xl font-black whitespace-nowrap" :style="{ color: website.primary_color }">
                {{ category }}
              </h4>
              <div class="h-1 flex-1 rounded-full" :style="{ backgroundColor: `${website.primary_color}30` }"></div>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
              <div
                v-for="product in categoryProducts"
                :key="product.id"
                class="group bg-white rounded-3xl shadow-xl overflow-hidden hover:shadow-2xl transition-all duration-500 transform hover:-translate-y-3 border border-gray-100"
              >
                <div v-if="product.image_url" class="h-64 overflow-hidden relative">
                  <img
                    :src="product.image_url"
                    :alt="`${getProductDisplay(product).name} - ${displayRestaurantName}`"
                    class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
                    loading="lazy"
                  />
                  <div class="absolute inset-0 bg-gradient-to-t from-black/20 to-transparent opacity-0 group-hover:opacity-100 transition-opacity"></div>
                </div>
                <div class="p-6">
                  <div class="flex items-start justify-between mb-3">
                    <h4 class="text-2xl font-bold group-hover:scale-105 transition-transform" :style="{ color: website.primary_color }">
                      {{ getProductDisplay(product).name }}
                    </h4>
                    <span class="text-2xl font-black whitespace-nowrap ml-4" :style="{ color: website.secondary_color }">
                      {{ formatCurrency(product.price) }}
                    </span>
                  </div>
                  <p v-if="getProductDisplay(product).description" class="text-gray-600 text-base leading-relaxed mb-5 min-h-[3rem]">
                    {{ getProductDisplay(product).description }}
                  </p>
                  <button
                    @click="addToCart(product)"
                    :style="{ 
                      background: `linear-gradient(135deg, ${website.primary_color}, ${website.secondary_color})`,
                      boxShadow: `0 4px 15px ${website.primary_color}40`
                    }"
                    class="w-full px-6 py-3.5 text-white rounded-xl font-bold hover:scale-105 transition-all shadow-lg relative overflow-hidden group"
                  >
                    <span class="relative z-10 flex items-center justify-center gap-2">
                      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"></path>
                      </svg>
                      {{ $t('website.addToCart') }}
                    </span>
                    <div class="absolute inset-0 bg-white opacity-0 group-hover:opacity-20 transition-opacity"></div>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- Fallback to old menu items if no products -->
        <div v-else-if="menuItems && menuItems.length > 0">
          <template v-if="hasMenuCategories">
            <div v-for="(category, catIndex) in menuCategories" :key="catIndex" class="mb-12">
              <h4 v-if="category" class="text-3xl font-bold mb-6" :style="{ color: website.primary_color }">
                {{ category }}
              </h4>
              <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                <div
                  v-for="(item, index) in getMenuItemsByCategory(category)"
                  :key="index"
                  class="bg-white rounded-2xl shadow-lg overflow-hidden hover:shadow-2xl transition-all duration-300 transform hover:-translate-y-2"
                >
                  <div class="p-6">
                    <div class="flex items-start justify-between mb-3">
                      <h4 class="text-2xl font-bold" :style="{ color: website.primary_color }">
                        {{ item.name }}
                      </h4>
                      <span v-if="item.price" class="text-xl font-bold" :style="{ color: website.secondary_color }">
                        {{ formatCurrency(parseFloat(item.price) || 0) }}
                      </span>
                    </div>
                    <p v-if="item.description" class="text-gray-600 text-lg leading-relaxed">
                      {{ item.description }}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </template>
          <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            <div
              v-for="(item, index) in menuItems"
              :key="index"
              class="bg-white rounded-2xl shadow-lg overflow-hidden hover:shadow-2xl transition-all duration-300 transform hover:-translate-y-2"
            >
              <div class="p-6">
                <div class="flex items-start justify-between mb-3">
                  <h4 class="text-2xl font-bold" :style="{ color: website.primary_color }">
                    {{ item.name }}
                  </h4>
                  <span v-if="item.price" class="text-xl font-bold" :style="{ color: website.secondary_color }">
                    {{ item.price }}
                  </span>
                </div>
                <p v-if="item.description" class="text-gray-600 text-lg leading-relaxed">
                  {{ item.description }}
                </p>
              </div>
            </div>
          </div>
        </div>
        
        <!-- Only show "coming soon" if there's no menu image and no menu items -->
        <div v-else-if="!website.menu_image_url" class="text-center py-12">
          <p class="text-gray-500 text-lg">{{ $t('website.menuComingSoon') }}</p>
        </div>
      </div>
    </section>
    
    <!-- Cart Toggle Button (Floating) - Always visible when cart has items -->
    <button
      v-if="cart.length > 0"
      @click="showCart = !showCart"
      :style="{ backgroundColor: website.primary_color }"
      :class="[
        'fixed bottom-6 z-40 px-6 py-4 text-white rounded-full shadow-2xl hover:shadow-3xl hover:scale-105 transition-all flex items-center gap-3 font-semibold',
        $i18n.locale === 'ar' 
          ? (showCart && windowWidth >= 640 ? 'left-[400px]' : 'left-6')
          : (showCart && windowWidth >= 640 ? 'right-[400px]' : 'right-6')
      ]"
    >
      <div class="relative">
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"></path>
        </svg>
        <span class="absolute -top-2 -right-2 bg-red-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center font-bold">{{ cart.length }}</span>
      </div>
      <span class="hidden sm:inline">{{ showCart && windowWidth >= 640 ? $t('website.hideCart') : $t('website.viewCart') }}</span>
      <span class="font-bold text-lg">{{ formatCurrency(finalTotal) }}</span>
    </button>

    <!-- About Section -->
    <section id="about" v-if="displayDescription" class="py-20 px-4 sm:px-6 lg:px-8" aria-label="About section">
      <div class="max-w-7xl mx-auto">
        <div class="grid md:grid-cols-2 gap-12 items-center">
          <div>
            <h3 class="text-5xl md:text-6xl font-black mb-6" :style="{ color: website.primary_color }">
              {{ $t('website.aboutUs') }}
            </h3>
            <p class="text-xl text-gray-700 leading-relaxed">
              {{ displayDescription }}
            </p>
          </div>
          <div class="relative">
            <div class="aspect-square rounded-3xl overflow-hidden shadow-2xl" :style="{ 
              background: `linear-gradient(135deg, ${website.primary_color} 0%, ${website.secondary_color} 100%)` 
            }">
              <div class="w-full h-full flex items-center justify-center text-white text-9xl">
                🍽️
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Contact Section -->
    <section id="contact" v-if="displayAddress || website.phone || website.email" class="py-24 px-4 sm:px-6 lg:px-8 bg-gradient-to-b from-white to-gray-50 relative overflow-hidden" aria-label="Contact section">
      <div class="absolute inset-0 opacity-5">
        <div class="absolute top-0 right-0 w-96 h-96 rounded-full blur-3xl" :style="{ backgroundColor: website.secondary_color }"></div>
        <div class="absolute bottom-0 left-0 w-96 h-96 rounded-full blur-3xl" :style="{ backgroundColor: website.primary_color }"></div>
      </div>
      
      <div class="max-w-7xl mx-auto relative z-10">
        <div class="text-center mb-20">
          <div class="inline-block mb-4">
            <span class="text-sm font-semibold uppercase tracking-wider px-4 py-2 rounded-full" :style="{ 
              backgroundColor: `${website.primary_color}15`,
              color: website.primary_color
            }">
              {{ $t('website.contactUs') }}
            </span>
          </div>
          <h3 class="text-6xl md:text-7xl font-black mb-6 bg-gradient-to-r bg-clip-text text-transparent" :style="{ 
            backgroundImage: `linear-gradient(135deg, ${website.primary_color}, ${website.secondary_color})`
          }">
            {{ $t('website.getInTouch') }}
          </h3>
          <p class="text-xl md:text-2xl text-gray-600 max-w-2xl mx-auto">{{ $t('website.getInTouchDesc') }}</p>
        </div>
        
        <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div v-if="displayAddress" class="group bg-white rounded-3xl p-10 shadow-xl text-center hover:shadow-2xl transition-all duration-500 transform hover:-translate-y-2 border border-gray-100 relative overflow-hidden">
            <div class="absolute inset-0 bg-gradient-to-br opacity-0 group-hover:opacity-5 transition-opacity" :style="{ 
              background: `linear-gradient(135deg, ${website.primary_color}, ${website.secondary_color})` 
            }"></div>
            <div class="text-6xl mb-6 transform group-hover:scale-110 transition-transform duration-500">📍</div>
            <h4 class="text-2xl font-bold text-gray-800 mb-4" :style="{ color: website.primary_color }">{{ $t('website.address') }}</h4>
            <p class="text-gray-600 text-lg leading-relaxed">{{ displayAddress }}</p>
          </div>
          
          <div v-if="website.phone" class="group bg-white rounded-3xl p-10 shadow-xl text-center hover:shadow-2xl transition-all duration-500 transform hover:-translate-y-2 border border-gray-100 relative overflow-hidden">
            <div class="absolute inset-0 bg-gradient-to-br opacity-0 group-hover:opacity-5 transition-opacity" :style="{ 
              background: `linear-gradient(135deg, ${website.primary_color}, ${website.secondary_color})` 
            }"></div>
            <div class="text-6xl mb-6 transform group-hover:scale-110 transition-transform duration-500">📞</div>
            <h4 class="text-2xl font-bold text-gray-800 mb-4" :style="{ color: website.primary_color }">{{ $t('website.phone') }}</h4>
            <a :href="`tel:${website.phone}`" class="text-xl font-semibold hover:underline transition-all inline-block" :style="{ color: website.secondary_color }">
              {{ website.phone }}
            </a>
          </div>
          
          <div v-if="website.email" class="group bg-white rounded-3xl p-10 shadow-xl text-center hover:shadow-2xl transition-all duration-500 transform hover:-translate-y-2 border border-gray-100 relative overflow-hidden">
            <div class="absolute inset-0 bg-gradient-to-br opacity-0 group-hover:opacity-5 transition-opacity" :style="{ 
              background: `linear-gradient(135deg, ${website.primary_color}, ${website.secondary_color})` 
            }"></div>
            <div class="text-6xl mb-6 transform group-hover:scale-110 transition-transform duration-500">✉️</div>
            <h4 class="text-2xl font-bold text-gray-800 mb-4" :style="{ color: website.primary_color }">{{ $t('website.email') }}</h4>
            <a :href="`mailto:${website.email}`" class="text-xl font-semibold hover:underline transition-all inline-block break-all" :style="{ color: website.secondary_color }">
              {{ website.email }}
            </a>
          </div>
        </div>
      </div>
    </section>

    <!-- Gallery Section -->
    <section id="gallery" v-if="galleryImages && galleryImages.length > 0" class="py-20 px-4 sm:px-6 lg:px-8 bg-gray-50" aria-label="Gallery section">
      <div class="max-w-7xl mx-auto">
        <div class="text-center mb-16">
          <h3 class="text-5xl md:text-6xl font-black mb-4" :style="{ color: website.primary_color }">
            {{ $t('website.ourGallery') }}
          </h3>
          <p class="text-xl text-gray-600">{{ $t('website.seeWhatServing') }}</p>
        </div>
        <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
          <div
            v-for="(image, index) in galleryImages"
            :key="index"
            class="relative group overflow-hidden rounded-lg aspect-square cursor-pointer"
          >
            <img
              :src="image.url || image"
              :alt="`${displayRestaurantName} - Gallery image ${index + 1}`"
              class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-300"
              loading="lazy"
            />
            <div class="absolute inset-0 bg-black/0 group-hover:bg-black/20 transition-colors"></div>
          </div>
        </div>
      </div>
    </section>

    <!-- App Download Section -->
    <section v-if="website.app_download_url" class="py-24 px-4 sm:px-6 lg:px-8 relative overflow-hidden">
      <div class="absolute inset-0 opacity-10" :style="{ 
        background: `linear-gradient(135deg, ${website.primary_color} 0%, ${website.secondary_color} 100%)` 
      }"></div>
      <div class="max-w-5xl mx-auto text-center relative z-10">
        <div class="inline-block mb-6">
          <div class="w-20 h-20 rounded-3xl mx-auto flex items-center justify-center text-4xl transform hover:scale-110 transition-transform shadow-2xl" :style="{ 
            background: `linear-gradient(135deg, ${website.primary_color}, ${website.secondary_color})`
          }">
            📱
          </div>
        </div>
        <h3 class="text-5xl md:text-6xl font-black mb-6 bg-gradient-to-r bg-clip-text text-transparent" :style="{ 
          backgroundImage: `linear-gradient(135deg, ${website.primary_color}, ${website.secondary_color})`
        }">
          {{ $t('website.downloadOurApp') }}
        </h3>
        <p class="text-xl md:text-2xl text-gray-700 mb-10 max-w-2xl mx-auto">
          {{ $t('website.downloadAppDesc') }}
        </p>
        <a
          :href="website.app_download_url"
          target="_blank"
          rel="noopener noreferrer"
          :style="{ 
            background: `linear-gradient(135deg, ${website.primary_color}, ${website.secondary_color})`,
            boxShadow: `0 10px 30px ${website.primary_color}50`
          }"
          class="inline-block px-10 py-5 text-white rounded-full text-lg font-bold hover:scale-110 transition-all shadow-2xl"
        >
          {{ $t('website.downloadNow') }} <span :class="$i18n.locale === 'ar' ? 'inline-block transform rotate-180' : ''">→</span>
        </a>
      </div>
    </section>

    <!-- Locations Section -->
    <section v-if="locations && locations.length > 0" id="locations" class="py-24 px-4 sm:px-6 lg:px-8 bg-gradient-to-b from-gray-50 to-white relative overflow-hidden">
      <div class="absolute inset-0 opacity-5">
        <div class="absolute top-0 right-0 w-96 h-96 rounded-full blur-3xl" :style="{ backgroundColor: website.primary_color }"></div>
      </div>
      
      <div class="max-w-7xl mx-auto relative z-10">
        <div class="text-center mb-20">
          <div class="inline-block mb-4">
            <span class="text-sm font-semibold uppercase tracking-wider px-4 py-2 rounded-full" :style="{ 
              backgroundColor: `${website.primary_color}15`,
              color: website.primary_color
            }">
              Locations
            </span>
          </div>
          <h3 class="text-6xl md:text-7xl font-black mb-6 bg-gradient-to-r bg-clip-text text-transparent" :style="{ 
            backgroundImage: `linear-gradient(135deg, ${website.primary_color}, ${website.secondary_color})`
          }">
            {{ $t('website.findNearestLocation') }}
          </h3>
          <p class="text-xl md:text-2xl text-gray-600 max-w-2xl mx-auto">{{ $t('website.visitLocations') }}</p>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          <div
            v-for="(location, index) in locations"
            :key="index"
            class="group bg-white rounded-3xl p-10 shadow-xl hover:shadow-2xl transition-all duration-500 transform hover:-translate-y-3 border border-gray-100 relative overflow-hidden"
          >
            <div class="absolute inset-0 bg-gradient-to-br opacity-0 group-hover:opacity-5 transition-opacity" :style="{ 
              background: `linear-gradient(135deg, ${website.primary_color}, ${website.secondary_color})` 
            }"></div>
            <div class="relative z-10">
              <div class="w-16 h-16 rounded-2xl mb-6 flex items-center justify-center text-3xl transform group-hover:scale-110 transition-transform" :style="{ 
                background: `linear-gradient(135deg, ${website.primary_color}20, ${website.secondary_color}20)`
              }">
                📍
              </div>
              <h4 class="text-2xl font-bold mb-5" :style="{ color: website.primary_color }">
                {{ location.name || `Location ${index + 1}` }}
              </h4>
              <p v-if="location.address" class="text-gray-600 mb-4 text-lg leading-relaxed">
                {{ location.address }}
              </p>
              <a
                v-if="location.phone"
                :href="`tel:${location.phone}`"
                class="text-lg font-semibold block mb-6 hover:underline transition-all inline-block"
                :style="{ color: website.secondary_color }"
              >
                📞 {{ location.phone }}
              </a>
              <a
                v-if="location.address"
                :href="`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(location.address)}`"
                target="_blank"
                rel="noopener noreferrer"
                :style="{ 
                  background: `linear-gradient(135deg, ${website.primary_color}, ${website.secondary_color})`,
                  boxShadow: `0 4px 15px ${website.primary_color}40`
                }"
                class="inline-block mt-4 px-6 py-3 rounded-xl text-white font-bold hover:scale-105 transition-all shadow-lg"
              >
                View on Map →
              </a>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Newsletter Section -->
    <section v-if="website.newsletter_enabled" class="py-24 px-4 sm:px-6 lg:px-8 relative overflow-hidden" :style="{ 
      background: `linear-gradient(135deg, ${website.primary_color} 0%, ${website.secondary_color} 100%)` 
    }">
      <div class="absolute inset-0 opacity-10">
        <div class="absolute top-0 left-0 w-96 h-96 rounded-full blur-3xl bg-white"></div>
        <div class="absolute bottom-0 right-0 w-96 h-96 rounded-full blur-3xl bg-white"></div>
      </div>
      <div class="max-w-5xl mx-auto text-center relative z-10">
        <div class="inline-block mb-6">
          <div class="w-20 h-20 rounded-3xl mx-auto flex items-center justify-center text-4xl bg-white/20 backdrop-blur-sm">
            ✉️
          </div>
        </div>
        <h3 class="text-5xl md:text-6xl font-black mb-6 text-white">
          {{ $t('website.newsletter') }}
        </h3>
        <p class="text-xl md:text-2xl text-white/90 mb-10 max-w-2xl mx-auto">
          {{ $t('website.newsletterDesc') }}
        </p>
        <form @submit.prevent="handleNewsletterSubmit" class="flex flex-col sm:flex-row gap-4 max-w-lg mx-auto" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
          <input
            v-model="newsletterEmail"
            type="email"
            required
            :placeholder="$t('website.enterYourEmail')"
            class="flex-1 px-6 py-5 rounded-full text-gray-800 focus:outline-none focus:ring-4 focus:ring-white/50 shadow-xl text-lg"
          />
          <button
            type="submit"
            class="px-10 py-5 bg-white text-gray-900 rounded-full font-bold hover:scale-105 transition-all shadow-xl hover:shadow-2xl"
            :style="{ color: website.primary_color }"
          >
            {{ $t('website.subscribeNow') }}
          </button>
        </form>
      </div>
    </section>

    <!-- Social Links Section -->
    <section v-if="socialLinks && Object.keys(socialLinks).length > 0" class="py-16 px-4 sm:px-6 lg:px-8">
      <div class="max-w-7xl mx-auto text-center">
        <h3 class="text-4xl font-bold mb-8" :style="{ color: website.primary_color }">
          {{ $t('website.followUs') }}
        </h3>
        <div class="flex justify-center gap-6 flex-wrap">
          <a
            v-for="(url, platform) in socialLinks"
            :key="platform"
            :href="url"
            target="_blank"
            rel="noopener noreferrer"
            class="w-16 h-16 rounded-full flex items-center justify-center text-3xl hover:scale-110 transition-transform shadow-lg hover:shadow-xl"
            :style="{ backgroundColor: website.primary_color + '20' }"
          >
            {{ getSocialIcon(platform) }}
          </a>
        </div>
      </div>
    </section>

    </main>
    <!-- Footer -->
    <footer class="bg-gradient-to-b from-gray-900 to-black text-white py-16 px-4 sm:px-6 lg:px-8 relative overflow-hidden" role="contentinfo">
      <div class="absolute inset-0 opacity-10">
        <div class="absolute top-0 left-0 w-full h-full" :style="{ 
          backgroundImage: `radial-gradient(circle at 50% 0%, ${website.primary_color} 0%, transparent 50%)`
        }"></div>
      </div>
      
      <div class="max-w-7xl mx-auto relative z-10">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-12 mb-12">
          <div>
            <div class="flex items-center gap-3 mb-6">
              <img
                v-if="website.logo_url"
                :src="website.logo_url"
                :alt="displayRestaurantName"
                class="h-12 w-12 object-contain"
              />
              <h4 class="text-3xl font-bold bg-gradient-to-r bg-clip-text text-transparent" :style="{ 
                backgroundImage: `linear-gradient(135deg, ${website.primary_color}, ${website.secondary_color})`
              }">
                {{ displayRestaurantName }}
              </h4>
            </div>
            <p v-if="displayDescription" class="text-gray-400 text-lg leading-relaxed">
              {{ shortDescription }}
            </p>
          </div>
          
          <div>
            <h5 class="text-xl font-bold mb-6" :style="{ color: website.primary_color }">{{ $t('website.quickLinks') }}</h5>
            <ul class="space-y-3">
              <li v-if="menuItems && menuItems.length > 0 || products.length > 0">
                <a href="#menu" class="text-gray-400 hover:text-white transition-all flex items-center gap-2 group" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <span class="w-0 group-hover:w-2 h-0.5 transition-all" :style="{ backgroundColor: website.primary_color }"></span>
                  {{ $t('website.menu') }}
                </a>
              </li>
              <li v-if="displayDescription">
                <a href="#about" class="text-gray-400 hover:text-white transition-all flex items-center gap-2 group" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <span class="w-0 group-hover:w-2 h-0.5 transition-all" :style="{ backgroundColor: website.primary_color }"></span>
                  {{ $t('website.about') }}
                </a>
              </li>
              <li v-if="galleryImages && galleryImages.length > 0">
                <a href="#gallery" class="text-gray-400 hover:text-white transition-all flex items-center gap-2 group" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <span class="w-0 group-hover:w-2 h-0.5 transition-all" :style="{ backgroundColor: website.primary_color }"></span>
                  {{ $t('website.gallery') }}
                </a>
              </li>
              <li v-if="locations && locations.length > 0">
                <a href="#locations" class="text-gray-400 hover:text-white transition-all flex items-center gap-2 group" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <span class="w-0 group-hover:w-2 h-0.5 transition-all" :style="{ backgroundColor: website.primary_color }"></span>
                  {{ $t('website.locations') }}
                </a>
              </li>
              <li v-if="displayAddress || website.phone || website.email">
                <a href="#contact" class="text-gray-400 hover:text-white transition-all flex items-center gap-2 group" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <span class="w-0 group-hover:w-2 h-0.5 transition-all" :style="{ backgroundColor: website.primary_color }"></span>
                  {{ $t('website.contact') }}
                </a>
              </li>
            </ul>
          </div>
          
          <div>
            <h5 class="text-xl font-bold mb-6" :style="{ color: website.primary_color }">{{ $t('website.contactInfo') }}</h5>
            <ul class="space-y-4 text-gray-400">
              <li v-if="displayAddress" class="flex items-start gap-3">
                <span class="text-xl">📍</span>
                <span class="text-lg">{{ displayAddress }}</span>
              </li>
              <li v-if="website.phone" class="flex items-center gap-3">
                <span class="text-xl">📞</span>
                <a :href="`tel:${website.phone}`" class="text-lg hover:text-white transition-colors">{{ website.phone }}</a>
              </li>
              <li v-if="website.email" class="flex items-center gap-3">
                <span class="text-xl">✉️</span>
                <a :href="`mailto:${website.email}`" class="text-lg hover:text-white transition-colors break-all">{{ website.email }}</a>
              </li>
            </ul>
          </div>
        </div>
        
        <div class="border-t border-gray-800 pt-8 flex flex-col md:flex-row justify-between items-center gap-4">
          <p class="text-gray-400 text-center md:text-left">
            © {{ new Date().getFullYear() }} {{ displayRestaurantName }}. All rights reserved.
          </p>
          <div v-if="socialLinks && Object.keys(socialLinks).length > 0" class="flex gap-4">
            <a
              v-for="(url, platform) in socialLinks"
              :key="platform"
              :href="url"
              target="_blank"
              rel="noopener noreferrer"
              class="w-12 h-12 rounded-full flex items-center justify-center text-xl hover:scale-125 transition-all hover:shadow-lg"
              :style="{ 
                backgroundColor: `${website.primary_color}20`,
                color: website.primary_color
              }"
              :title="platform"
            >
              {{ getSocialIcon(platform) }}
            </a>
          </div>
        </div>
      </div>
    </footer>
    </div>
  </div>

  <div v-else class="min-h-screen flex items-center justify-center bg-gray-50">
    <div class="text-center">
      <p class="text-xl text-gray-600 mb-4">{{ $t('website.notFound') }}</p>
      <button
        @click="$router.push('/')"
        class="px-6 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
      >
        {{ $t('website.backToBuilder') }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import LanguageSwitcher from './LanguageSwitcher.vue';
import { getWebsite, getProducts, getWebsiteByBarcode, getWebsiteByDomain } from '../services/api.js';
import { useSEO } from '../composables/useSEO.js';

const { locale, t } = useI18n();
const { setSEO, clearSEO } = useSEO();

const route = useRoute();
const router = useRouter();

const website = ref(null);
const loading = ref(true);
const newsletterEmail = ref('');

// Locale-based display values (DB stores EN + _ar; pick by current language)
const displayRestaurantName = computed(() => {
  if (!website.value) return '';
  return locale.value === 'ar' && website.value.restaurant_name_ar
    ? website.value.restaurant_name_ar
    : (website.value.restaurant_name || '');
});
const displayDescription = computed(() => {
  if (!website.value) return '';
  return locale.value === 'ar' && website.value.description_ar
    ? website.value.description_ar
    : (website.value.description || '');
});
const displayAddress = computed(() => {
  if (!website.value) return '';
  return locale.value === 'ar' && website.value.address_ar
    ? website.value.address_ar
    : (website.value.address || '');
});

// Computed property for short description (first 150 characters)
const shortDescription = computed(() => {
  const desc = displayDescription.value;
  if (!desc) return '';
  if (desc.length <= 150) return desc;
  const truncated = desc.substring(0, 150);
  const lastSpace = truncated.lastIndexOf(' ');
  return lastSpace > 100 ? truncated.substring(0, lastSpace) + '...' : truncated + '...';
});

// Check if description is long enough to show "Read More"
const hasLongDescription = computed(() => {
  return displayDescription.value && displayDescription.value.length > 150;
});

// Product display by locale (name, description, category from DB EN / _ar)
const getProductDisplay = (product) => {
  if (!product) return { name: '', description: '', category: '' };
  const isAr = locale.value === 'ar';
  return {
    name: (isAr && product.name_ar) ? product.name_ar : (product.name || ''),
    description: (isAr && product.description_ar) ? product.description_ar : (product.description || ''),
    category: (isAr && product.category_ar) ? product.category_ar : (product.category || 'Other')
  };
};

const menuItems = computed(() => {
  if (!website.value?.menu_items) return [];
  try {
    return typeof website.value.menu_items === 'string'
      ? JSON.parse(website.value.menu_items)
      : website.value.menu_items;
  } catch {
    return [];
  }
});

// Cart functionality
const cart = ref([]);
const showCart = ref(false);
const windowWidth = ref(window.innerWidth);
const products = ref([]);
const productsByCategory = computed(() => {
  const _locale = locale.value; // depend on locale so grouping by display category updates
  const grouped = {};
  products.value.forEach(product => {
    if (product.is_available) {
      const category = getProductDisplay(product).category || 'Other';
      if (!grouped[category]) {
        grouped[category] = [];
      }
      grouped[category].push(product);
    }
  });
  return grouped;
});

const categoryOrderMap = computed(() => {
  if (!website.value || !website.value.menu_category_order) return {};
  const raw = website.value.menu_category_order;
  try {
    if (typeof raw === 'string') {
      const parsed = JSON.parse(raw);
      return parsed && typeof parsed === 'object' ? parsed : {};
    }
    if (typeof raw === 'object' && raw !== null && !Array.isArray(raw)) {
      return raw;
    }
  } catch {
    return {};
  }
  return {};
});

const orderedProductsByCategory = computed(() => {
  const entries = Object.entries(productsByCategory.value || {});
  const order = categoryOrderMap.value || {};

  entries.sort(([catA], [catB]) => {
    const va = order[catA];
    const vb = order[catB];
    const oa = Number.isFinite(va) && va > 0 ? va : 9999;
    const ob = Number.isFinite(vb) && vb > 0 ? vb : 9999;
    if (oa !== ob) return oa - ob;
    const la = (catA || '').toLowerCase();
    const lb = (catB || '').toLowerCase();
    if (la < lb) return -1;
    if (la > lb) return 1;
    return 0;
  });

  const ordered = {};
  for (const [cat, items] of entries) {
    ordered[cat] = items;
  }
  return ordered;
});

const galleryImages = computed(() => {
  if (!website.value?.gallery_images) return [];
  try {
    return typeof website.value.gallery_images === 'string'
      ? JSON.parse(website.value.gallery_images)
      : website.value.gallery_images;
  } catch {
    return [];
  }
});

const locations = computed(() => {
  if (!website.value?.locations) return [];
  try {
    return typeof website.value.locations === 'string'
      ? JSON.parse(website.value.locations)
      : website.value.locations;
  } catch {
    return [];
  }
});

// Menu categories for grouping
const hasMenuCategories = computed(() => {
  if (!menuItems.value || menuItems.value.length === 0) return false;
  return menuItems.value.some(item => item.category && item.category.trim() !== '');
});

const menuCategories = computed(() => {
  if (!hasMenuCategories.value) return [];
  const categories = [...new Set(menuItems.value.map(item => item.category).filter(cat => cat && cat.trim() !== ''))];
  return categories;
});

const getMenuItemsByCategory = (category) => {
  return menuItems.value.filter(item => item.category === category);
};

// Cart functions
const cartTotal = computed(() => {
  return cart.value.reduce((total, item) => {
    return total + (parseFloat(item.price) * item.quantity);
  }, 0);
});

// Tax calculation
const taxAmount = computed(() => {
  if (!website.value || !website.value.tax_enabled) {
    return 0;
  }
  const taxRate = parseFloat(website.value.tax_rate) || 0;
  return (cartTotal.value * taxRate) / 100;
});

// Final total including tax
const finalTotal = computed(() => {
  return cartTotal.value + taxAmount.value;
});

const addToCart = (product) => {
  const existingItem = cart.value.find(item => item.id === product.id);
  if (existingItem) {
    existingItem.quantity += 1;
  } else {
    cart.value.push({
      ...product,
      quantity: 1
    });
  }
  showCart.value = true;
};

const updateQuantity = (index, newQuantity) => {
  if (newQuantity <= 0) {
    removeFromCart(index);
  } else {
    cart.value[index].quantity = newQuantity;
    // Save cart to sessionStorage
    if (website.value?.id) {
      sessionStorage.setItem(`cart_${website.value.id}`, JSON.stringify(cart.value));
    }
  }
};

const removeFromCart = (index) => {
  cart.value.splice(index, 1);
  if (cart.value.length === 0) {
    showCart.value = false;
  }
  // Save cart to sessionStorage
  if (website.value?.id) {
    sessionStorage.setItem(`cart_${website.value.id}`, JSON.stringify(cart.value));
  }
};

const goToCheckout = () => {
  // Save cart to sessionStorage
  if (website.value?.id) {
    sessionStorage.setItem(`cart_${website.value.id}`, JSON.stringify(cart.value));
    router.push(`/website/${website.value.id}/checkout`);
  }
};

const scrollToMenu = () => {
  const menuSection = document.getElementById('menu');
  if (menuSection) {
    menuSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
};

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

// Load products when website is loaded
const loadProducts = async () => {
  if (website.value?.id) {
    try {
      console.log('Loading products for website:', website.value.id);
      products.value = await getProducts(website.value.id);
      console.log('Products loaded:', products.value.length, 'products');
      
      // Load cart from sessionStorage
      const cartData = sessionStorage.getItem(`cart_${website.value.id}`);
      if (cartData) {
        try {
          cart.value = JSON.parse(cartData);
          // Auto-open cart on desktop if items exist
          if (cart.value.length > 0 && window.innerWidth >= 640) {
            showCart.value = true;
          }
        } catch (e) {
          console.warn('Failed to load cart from sessionStorage:', e);
          cart.value = [];
        }
      }
    } catch (error) {
      console.error('Failed to load products:', error);
      products.value = [];
    }
  } else {
    console.log('No website ID, cannot load products');
  }
};

// Update SEO when website is loaded
const updateSEO = () => {
  if (!website.value) return;
  
  const restaurantName = displayRestaurantName.value || 'Restaurant';
  const description = displayDescription.value || shortDescription.value || `Order online from ${restaurantName}. Browse our menu and place your order today.`;
  const logoUrl = website.value.logo_url || '';
  const websiteUrl = window.location.href;
  
  // Build menu items for structured data
  const menuItems = products.value.map(p => ({
    '@type': 'MenuItem',
    'name': p.name,
    'description': p.description || '',
    'offers': {
      '@type': 'Offer',
      'price': p.price.toString(),
      'priceCurrency': website.value?.currency_code || 'USD'
    }
  }));
  
  setSEO({
    title: `${restaurantName} - Order Online | RestaurantAI`,
    description: description,
    keywords: [restaurantName, 'restaurant', 'online ordering', 'food delivery', 'menu'],
    image: logoUrl || '/og-image.jpg',
    url: websiteUrl,
    locale: locale.value === 'ar' ? 'ar_SA' : 'en_US',
    alternateLocale: locale.value === 'ar' ? 'en_US' : 'ar_SA',
    structuredData: {
      '@context': 'https://schema.org',
      '@type': 'Restaurant',
      'name': restaurantName,
      'description': description,
      'image': logoUrl,
      'url': websiteUrl,
      'address': {
        '@type': 'PostalAddress',
        'streetAddress': displayAddress.value || '',
        'addressLocality': '',
        'addressRegion': '',
        'postalCode': '',
        'addressCountry': ''
      },
      'telephone': website.value.phone || '',
      'email': website.value.email || '',
      'servesCuisine': 'Various',
      'priceRange': '$$',
      'menu': menuItems.length > 0 ? {
        '@type': 'Menu',
        'hasMenuSection': [{
          '@type': 'MenuSection',
          'name': 'Main Menu',
          'hasMenuItem': menuItems
        }]
      } : undefined
    }
  });
};

// Watch for website changes and load products
watch(() => website.value?.id, (newId) => {
  if (newId) {
    loadProducts();
    updateSEO();
  }
}, { immediate: true });

// Watch for products to update SEO with menu items
watch(() => products.value.length, () => {
  if (website.value) {
    updateSEO();
  }
});

// Watch for website to set default language
watch(() => website.value?.default_language, (newDefaultLang) => {
  if (newDefaultLang && availableLanguages.value.some(lang => lang.code === newDefaultLang)) {
    const savedLang = localStorage.getItem('appLanguage');
    // Only set default if no saved language or saved language is not available
    if (!savedLang || !availableLanguages.value.some(lang => lang.code === savedLang)) {
      locale.value = newDefaultLang;
      localStorage.setItem('appLanguage', newDefaultLang);
      document.documentElement.setAttribute('lang', newDefaultLang);
      document.documentElement.setAttribute('dir', newDefaultLang === 'ar' ? 'rtl' : 'ltr');
    }
  }
}, { immediate: true });

// Watch for locale changes to update RTL
watch(locale, (newLocale) => {
  document.documentElement.setAttribute('lang', newLocale);
  document.documentElement.setAttribute('dir', newLocale === 'ar' ? 'rtl' : 'ltr');
});

const socialLinks = computed(() => {
  if (!website.value?.social_links) return {};
  try {
    return typeof website.value.social_links === 'string'
      ? JSON.parse(website.value.social_links)
      : website.value.social_links;
  } catch {
    return {};
  }
});

const websiteStyles = computed(() => {
  if (!website.value) return {};
  return {
    '--primary-color': website.value.primary_color || '#4F46E5',
    '--secondary-color': website.value.secondary_color || '#7C3AED',
    fontFamily: website.value.font_family || 'Inter, sans-serif',
  };
});

// Available languages based on restaurant settings
const availableLanguages = computed(() => {
  if (!website.value) return [];
  
  const languages = [];
  let languagesEnabled = {
    english: true,
    arabic: true
  };

  // Parse languages_enabled from database
  if (website.value.languages_enabled) {
    try {
      const parsed = typeof website.value.languages_enabled === 'string'
        ? JSON.parse(website.value.languages_enabled)
        : website.value.languages_enabled;
      if (parsed && typeof parsed === 'object') {
        languagesEnabled = { ...languagesEnabled, ...parsed };
      }
    } catch (e) {
      console.warn('Error parsing languages_enabled:', e);
    }
  }

  if (languagesEnabled.english) {
    languages.push({ code: 'en', name: 'English', flag: '🇺🇸' });
  }
  if (languagesEnabled.arabic) {
    languages.push({ code: 'ar', name: 'العربية', flag: '🇸🇦' });
  }

  return languages;
});

// Show language switcher only if multiple languages are enabled
const shouldShowLanguageSwitcher = computed(() => {
  return availableLanguages.value.length > 1;
});

// Currency symbol mapping - language-aware
const getCurrencySymbol = (currencyCode) => {
  const currentLang = locale.value || 'en';
  
  if (currencyCode === 'JOD') {
    // JOD: Show "JD" in English, "د.ا" in Arabic
    return currentLang === 'ar' ? 'د.ا' : 'JD';
  }
  
  // Default currency symbols (same for all languages)
  const symbols = {
    USD: '$',
    JOD: currentLang === 'ar' ? 'د.ا' : 'JD'
  };
  
  return symbols[currencyCode] || '$';
};

// Format currency based on restaurant settings
const formatCurrency = (amount) => {
  if (!website.value) {
    return `$${parseFloat(amount).toFixed(2)}`;
  }

  const currencyCode = website.value.currency_code || 'USD';
  const symbolPosition = website.value.currency_symbol_position || 'before';
  const symbol = getCurrencySymbol(currencyCode);
  const formattedAmount = parseFloat(amount).toFixed(2);

  if (symbolPosition === 'before') {
    return `${symbol}${formattedAmount}`;
  } else {
    return `${formattedAmount} ${symbol}`;
  }
};

const getSocialIcon = (platform) => {
  const icons = {
    facebook: '📘',
    instagram: '📷',
    twitter: '🐦',
    youtube: '📺',
    linkedin: '💼',
    tiktok: '🎵',
  };
  return icons[platform.toLowerCase()] || '🔗';
};

const handleNewsletterSubmit = () => {
  // In a real app, this would send to your backend
  alert(`${t('website.thankYouSubscribing')} ${newsletterEmail.value}`);
  newsletterEmail.value = '';
};

// Handle window resize for responsive cart
const handleResize = () => {
  windowWidth.value = window.innerWidth;
  // On desktop (sm and up), auto-open cart if there are items
  if (windowWidth.value >= 640 && cart.value.length > 0) {
    showCart.value = true;
  }
  // On mobile, close cart overlay when resizing to desktop
  if (windowWidth.value >= 640 && showCart.value) {
    // Keep it open on desktop
  }
};

onMounted(async () => {
  // Set initial locale and RTL - will be updated after website loads with restaurant's default language
  const savedLang = localStorage.getItem('appLanguage') || 'en';
  locale.value = savedLang;
  document.documentElement.setAttribute('lang', savedLang);
  document.documentElement.setAttribute('dir', savedLang === 'ar' ? 'rtl' : 'ltr');
  
  try {
    // Check if we're accessing via domain (no route params or catch-all route)
    const hostname = window.location.hostname;
    const baseDomain = import.meta.env.VITE_BASE_DOMAIN || 'localhost';
    
    // If hostname is not the base domain and not localhost, try domain-based lookup
    if (hostname !== baseDomain && hostname !== 'localhost' && !hostname.includes('127.0.0.1')) {
      // Extract subdomain or use full hostname for custom domain
      let domainToCheck = hostname;
      
      // If it's a subdomain of base domain
      if (hostname.endsWith('.' + baseDomain)) {
        domainToCheck = hostname.split('.')[0];
      }
      
      try {
        const fetchedWebsite = await getWebsiteByDomain(domainToCheck);
        if (fetchedWebsite) {
          website.value = fetchedWebsite;
          // Products will be loaded automatically via the watch when website.value.id is set
          
          // On desktop (sm and up), auto-open cart if there are items
          if (window.innerWidth >= 640 && cart.value.length > 0) {
            showCart.value = true;
          }
          
          // Listen for window resize
          window.addEventListener('resize', handleResize);
          windowWidth.value = window.innerWidth;
          loading.value = false;
          return;
        }
      } catch (domainError) {
        console.log('Domain-based lookup failed, trying route params:', domainError);
      }
    }
    
    // Fallback to route-based access
    // Check if route is for barcode or regular ID
    if (route.path.startsWith('/barcode/')) {
      website.value = await getWebsiteByBarcode(route.params.code);
    } else if (route.params.id) {
      website.value = await getWebsite(route.params.id);
    } else {
      throw new Error('Website not found - no domain or ID provided');
    }
    
    // Set default language from restaurant settings
    if (website.value.default_language && availableLanguages.value.some(lang => lang.code === website.value.default_language)) {
      const savedLang = localStorage.getItem('appLanguage');
      if (!savedLang || !availableLanguages.value.some(lang => lang.code === savedLang)) {
        locale.value = website.value.default_language;
        localStorage.setItem('appLanguage', website.value.default_language);
        document.documentElement.setAttribute('lang', website.value.default_language);
        document.documentElement.setAttribute('dir', website.value.default_language === 'ar' ? 'rtl' : 'ltr');
      }
    }
    
    // Products will be loaded automatically via the watch when website.value.id is set
    
    // On desktop (sm and up), auto-open cart if there are items
    if (window.innerWidth >= 640 && cart.value.length > 0) {
      showCart.value = true;
    }
    
    // Listen for window resize
    window.addEventListener('resize', handleResize);
    
    // Initialize window width
    windowWidth.value = window.innerWidth;
  } catch (error) {
    console.error('Failed to load website:', error);
  } finally {
    loading.value = false;
  }
});

  // Cleanup
  onUnmounted(() => {
    window.removeEventListener('resize', handleResize);
    clearSEO();
  });
</script>

<style scoped>
/* Smooth scrolling */
html {
  scroll-behavior: smooth;
}

/* Custom scrollbar */
::-webkit-scrollbar {
  width: 10px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
}

::-webkit-scrollbar-thumb {
  background: var(--primary-color);
  border-radius: 5px;
}

::-webkit-scrollbar-thumb:hover {
  opacity: 0.8;
}

/* Smooth transitions for sidebar */
.transition-transform {
  transition-property: transform;
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
  transition-duration: 300ms;
}

/* Animations */
@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fade-in-up {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes gradient {
  0%, 100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

.animate-fade-in {
  animation: fade-in 0.8s ease-out;
}

.animate-fade-in-up {
  animation: fade-in-up 1s ease-out;
}

.animate-gradient {
  background-size: 200% 200%;
  animation: gradient 3s ease infinite;
}

.delay-1000 {
  animation-delay: 1s;
}
</style>
