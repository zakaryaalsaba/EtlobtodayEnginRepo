<template>
  <div class="min-h-screen bg-white" :dir="$i18n.locale === 'ar' ? 'rtl' : 'ltr'">
    <!-- Navigation -->
    <nav class="bg-white border-b border-gray-200 sticky top-0 z-50 backdrop-blur-sm bg-white/95">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between h-16" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
          <div class="flex items-center" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
            <div class="flex-shrink-0">
              <h1 class="text-2xl font-bold bg-gradient-to-r from-indigo-600 to-purple-600 bg-clip-text text-transparent">
                RestaurantAI
              </h1>
            </div>
          </div>
          <div class="hidden md:block">
            <div class="flex items-baseline gap-8" :class="$i18n.locale === 'ar' ? 'mr-10' : 'ml-10'">
              <a href="#features" class="text-gray-700 hover:text-indigo-600 transition-colors font-medium">{{ $t('nav.features') }}</a>
              <a href="#how-it-works" class="text-gray-700 hover:text-indigo-600 transition-colors font-medium">{{ $t('nav.howItWorks') }}</a>
              <a href="#pricing" class="text-gray-700 hover:text-indigo-600 transition-colors font-medium">{{ $t('nav.pricing') }}</a>
              <a href="#apply" class="text-gray-700 hover:text-indigo-600 transition-colors font-medium">{{ $t('nav.apply') }}</a>
            </div>
          </div>
          <div class="flex items-center gap-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
            <LanguageSwitcher />
            <button
              @click="scrollToApply"
              class="px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold"
            >
              {{ $t('nav.getStarted') }}
            </button>
          </div>
        </div>
      </div>
    </nav>

    <!-- Hero Section -->
    <section class="relative bg-gradient-to-br from-indigo-50 via-white to-purple-50 py-20 lg:py-32 overflow-hidden">
      <div class="absolute inset-0 bg-grid-pattern opacity-5"></div>
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        <div class="text-center">
          <h1 class="text-5xl md:text-7xl font-black text-gray-900 mb-6 leading-tight">
            {{ $t('hero.title') }}
            <span class="block bg-gradient-to-r from-indigo-600 via-purple-600 to-pink-600 bg-clip-text text-transparent animate-gradient">
              {{ heroText }}
            </span>
          </h1>
          <p class="text-xl md:text-2xl text-gray-600 mb-8 max-w-3xl mx-auto">
            {{ $t('hero.description') }}
          </p>
          <div class="flex flex-col sm:flex-row gap-4 justify-center items-center">
            <button
              @click="scrollToApply"
              class="px-8 py-4 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-all transform hover:scale-105 font-bold text-lg shadow-lg"
            >
              {{ $t('hero.startFree') }}
            </button>
            <button
              @click="scrollToFeatures"
              class="px-8 py-4 bg-white text-indigo-600 border-2 border-indigo-600 rounded-lg hover:bg-indigo-50 transition-all font-bold text-lg"
            >
              {{ $t('hero.learnMore') }}
            </button>
          </div>
          <p class="text-sm text-gray-500 mt-6">{{ $t('hero.noCreditCard') }}</p>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section id="features" class="py-20 bg-white">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-16">
          <h2 class="text-4xl md:text-5xl font-bold text-gray-900 mb-4">
            {{ $t('features.title') }}
          </h2>
          <p class="text-xl text-gray-600 max-w-2xl mx-auto">
            {{ $t('features.subtitle') }}
          </p>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          <!-- Feature 1 -->
          <div class="bg-gradient-to-br from-indigo-50 to-purple-50 rounded-2xl p-8 border border-indigo-100 hover:shadow-xl transition-shadow">
            <div class="w-12 h-12 bg-indigo-600 rounded-lg flex items-center justify-center mb-4">
              <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
              </svg>
            </div>
            <h3 class="text-xl font-bold text-gray-900 mb-2">{{ $t('features.beautifulWebsite.title') }}</h3>
            <p class="text-gray-600">
              {{ $t('features.beautifulWebsite.description') }}
            </p>
          </div>

          <!-- Feature 2 -->
          <div class="bg-gradient-to-br from-blue-50 to-cyan-50 rounded-2xl p-8 border border-blue-100 hover:shadow-xl transition-shadow">
            <div class="w-12 h-12 bg-blue-600 rounded-lg flex items-center justify-center mb-4">
              <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
              </svg>
            </div>
            <h3 class="text-xl font-bold text-gray-900 mb-2">{{ $t('features.onlineOrdering.title') }}</h3>
            <p class="text-gray-600">
              {{ $t('features.onlineOrdering.description') }}
            </p>
          </div>

          <!-- Feature 3 -->
          <div class="bg-gradient-to-br from-purple-50 to-pink-50 rounded-2xl p-8 border border-purple-100 hover:shadow-xl transition-shadow">
            <div class="w-12 h-12 bg-purple-600 rounded-lg flex items-center justify-center mb-4">
              <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
              </svg>
            </div>
            <h3 class="text-xl font-bold text-gray-900 mb-2">{{ $t('features.orderManagement.title') }}</h3>
            <p class="text-gray-600">
              {{ $t('features.orderManagement.description') }}
            </p>
          </div>

          <!-- Feature 4 -->
          <div class="bg-gradient-to-br from-green-50 to-emerald-50 rounded-2xl p-8 border border-green-100 hover:shadow-xl transition-shadow">
            <div class="w-12 h-12 bg-green-600 rounded-lg flex items-center justify-center mb-4">
              <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
              </svg>
            </div>
            <h3 class="text-xl font-bold text-gray-900 mb-2">{{ $t('features.qrCodeMenu.title') }}</h3>
            <p class="text-gray-600">
              {{ $t('features.qrCodeMenu.description') }}
            </p>
          </div>

          <!-- Feature 5 -->
          <div class="bg-gradient-to-br from-yellow-50 to-orange-50 rounded-2xl p-8 border border-yellow-100 hover:shadow-xl transition-shadow">
            <div class="w-12 h-12 bg-yellow-600 rounded-lg flex items-center justify-center mb-4">
              <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zm-7.5-3a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0z" />
              </svg>
            </div>
            <h3 class="text-xl font-bold text-gray-900 mb-2">{{ $t('features.customerManagement.title') }}</h3>
            <p class="text-gray-600">
              {{ $t('features.customerManagement.description') }}
            </p>
          </div>

          <!-- Feature 6 -->
          <div class="bg-gradient-to-br from-red-50 to-rose-50 rounded-2xl p-8 border border-red-100 hover:shadow-xl transition-shadow">
            <div class="w-12 h-12 bg-red-600 rounded-lg flex items-center justify-center mb-4">
              <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
              </svg>
            </div>
            <h3 class="text-xl font-bold text-gray-900 mb-2">{{ $t('features.secureReliable.title') }}</h3>
            <p class="text-gray-600">
              {{ $t('features.secureReliable.description') }}
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- How It Works Section -->
    <section id="how-it-works" class="py-20 bg-gray-50">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-16">
          <h2 class="text-4xl md:text-5xl font-bold text-gray-900 mb-4">{{ $t('howItWorks.title') }}</h2>
          <p class="text-xl text-gray-600">{{ $t('howItWorks.subtitle') }}</p>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-12">
          <div class="text-center">
            <div class="w-16 h-16 bg-indigo-600 rounded-full flex items-center justify-center mx-auto mb-6 text-white text-2xl font-bold">
              1
            </div>
            <h3 class="text-2xl font-bold text-gray-900 mb-4">{{ $t('howItWorks.step1.title') }}</h3>
            <p class="text-gray-600">
              {{ $t('howItWorks.step1.description') }}
            </p>
          </div>

          <div class="text-center">
            <div class="w-16 h-16 bg-indigo-600 rounded-full flex items-center justify-center mx-auto mb-6 text-white text-2xl font-bold">
              2
            </div>
            <h3 class="text-2xl font-bold text-gray-900 mb-4">{{ $t('howItWorks.step2.title') }}</h3>
            <p class="text-gray-600">
              {{ $t('howItWorks.step2.description') }}
            </p>
          </div>

          <div class="text-center">
            <div class="w-16 h-16 bg-indigo-600 rounded-full flex items-center justify-center mx-auto mb-6 text-white text-2xl font-bold">
              3
            </div>
            <h3 class="text-2xl font-bold text-gray-900 mb-4">{{ $t('howItWorks.step3.title') }}</h3>
            <p class="text-gray-600">
              {{ $t('howItWorks.step3.description') }}
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- Pricing Section -->
    <section id="pricing" class="py-20 bg-white">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-16">
          <h2 class="text-4xl md:text-5xl font-bold text-gray-900 mb-4">{{ $t('pricing.title') }}</h2>
          <p class="text-xl text-gray-600">{{ $t('pricing.subtitle') }}</p>
        </div>

        <div class="max-w-6xl mx-auto grid grid-cols-1 md:grid-cols-3 gap-8">
          <!-- Starter Plan -->
          <div 
            class="bg-white border-2 border-gray-200 rounded-2xl p-8 hover:border-indigo-500 transition-colors" 
            :dir="$i18n.locale === 'ar' ? 'rtl' : 'ltr'"
            :style="$i18n.locale === 'ar' ? { direction: 'rtl', textAlign: 'right' } : { direction: 'ltr', textAlign: 'left' }"
          >
            <h3 class="text-2xl font-bold text-gray-900 mb-2" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'">{{ $t('pricing.starter.title') }}</h3>
            <div class="mb-6" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'">
              <span class="text-5xl font-bold text-gray-900">{{ $t('pricing.starter.price') }}</span>
              <span class="text-gray-600" :class="$i18n.locale === 'ar' ? 'mr-2' : 'ml-2'">{{ $t('pricing.starter.period') }}</span>
            </div>
            <p class="text-sm text-gray-500 mb-6" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'">{{ $t('pricing.starter.upgrade') }}</p>
            <ul class="space-y-4 mb-8" :dir="$i18n.locale === 'ar' ? 'rtl' : 'ltr'">
              <li class="flex items-start" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''" :style="$i18n.locale === 'ar' ? { direction: 'rtl' } : { direction: 'ltr' }">
                <svg class="w-6 h-6 text-green-500 flex-shrink-0" :class="$i18n.locale === 'ar' ? 'ml-3' : 'mr-3'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                <span class="text-gray-700 flex-1" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'" :style="$i18n.locale === 'ar' ? { textAlign: 'right' } : { textAlign: 'left' }">{{ $t('pricing.starter.features.customWebsite') }}</span>
              </li>
              <li class="flex items-start" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''" :style="$i18n.locale === 'ar' ? { direction: 'rtl' } : { direction: 'ltr' }">
                <svg class="w-6 h-6 text-green-500 flex-shrink-0" :class="$i18n.locale === 'ar' ? 'ml-3' : 'mr-3'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                <span class="text-gray-700 flex-1" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'" :style="$i18n.locale === 'ar' ? { textAlign: 'right' } : { textAlign: 'left' }">{{ $t('pricing.starter.features.onlineOrdering') }}</span>
              </li>
              <li class="flex items-start" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''" :style="$i18n.locale === 'ar' ? { direction: 'rtl' } : { direction: 'ltr' }">
                <svg class="w-6 h-6 text-green-500 flex-shrink-0" :class="$i18n.locale === 'ar' ? 'ml-3' : 'mr-3'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                <span class="text-gray-700 flex-1" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'" :style="$i18n.locale === 'ar' ? { textAlign: 'right' } : { textAlign: 'left' }">{{ $t('pricing.starter.features.qrCode') }}</span>
              </li>
              <li class="flex items-start" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''" :style="$i18n.locale === 'ar' ? { direction: 'rtl' } : { direction: 'ltr' }">
                <svg class="w-6 h-6 text-green-500 flex-shrink-0" :class="$i18n.locale === 'ar' ? 'ml-3' : 'mr-3'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                <span class="text-gray-700 flex-1" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'" :style="$i18n.locale === 'ar' ? { textAlign: 'right' } : { textAlign: 'left' }">{{ $t('pricing.starter.features.orderManagement') }}</span>
              </li>
              <li class="flex items-start" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''" :style="$i18n.locale === 'ar' ? { direction: 'rtl' } : { direction: 'ltr' }">
                <svg class="w-6 h-6 text-green-500 flex-shrink-0" :class="$i18n.locale === 'ar' ? 'ml-3' : 'mr-3'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                <span class="text-gray-700 flex-1" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'" :style="$i18n.locale === 'ar' ? { textAlign: 'right' } : { textAlign: 'left' }">{{ $t('pricing.starter.features.customerManagement') }}</span>
              </li>
            </ul>
            <button
              @click="scrollToApply"
              class="w-full px-6 py-3 bg-gray-900 text-white rounded-lg hover:bg-gray-800 transition-colors font-semibold"
            >
              {{ $t('nav.getStarted') }}
            </button>
          </div>

          <!-- Standard Plan -->
          <div 
            class="bg-gradient-to-br from-indigo-500 to-purple-500 rounded-2xl p-8 text-white relative overflow-hidden border-2 border-indigo-400" 
            :dir="$i18n.locale === 'ar' ? 'rtl' : 'ltr'"
            :style="$i18n.locale === 'ar' ? { direction: 'rtl', textAlign: 'right' } : { direction: 'ltr', textAlign: 'left' }"
          >
            <div class="absolute" :class="$i18n.locale === 'ar' ? 'top-4 left-4' : 'top-4 right-4'">
              <span class="bg-white text-indigo-600 px-3 py-1 rounded-full text-xs font-bold">{{ $t('pricing.standard.popular') }}</span>
            </div>
            <h3 class="text-2xl font-bold mb-2" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'">{{ $t('pricing.standard.title') }}</h3>
            <div class="mb-6" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'">
              <span class="text-5xl font-bold">{{ $t('pricing.standard.price') }}</span>
              <span class="opacity-90" :class="$i18n.locale === 'ar' ? 'mr-2' : 'ml-2'">{{ $t('pricing.standard.month') }}</span>
            </div>
            <ul class="space-y-4 mb-8" :dir="$i18n.locale === 'ar' ? 'rtl' : 'ltr'">
              <li class="flex items-start" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''" :style="$i18n.locale === 'ar' ? { direction: 'rtl' } : { direction: 'ltr' }">
                <svg class="w-6 h-6 text-white flex-shrink-0" :class="$i18n.locale === 'ar' ? 'ml-3' : 'mr-3'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                <span class="flex-1" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'" :style="$i18n.locale === 'ar' ? { textAlign: 'right' } : { textAlign: 'left' }">{{ $t('pricing.standard.features.customWebsite') }}</span>
              </li>
              <li class="flex items-start" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''" :style="$i18n.locale === 'ar' ? { direction: 'rtl' } : { direction: 'ltr' }">
                <svg class="w-6 h-6 text-white flex-shrink-0" :class="$i18n.locale === 'ar' ? 'ml-3' : 'mr-3'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                <span class="flex-1" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'" :style="$i18n.locale === 'ar' ? { textAlign: 'right' } : { textAlign: 'left' }">{{ $t('pricing.standard.features.onlineOrdering') }}</span>
              </li>
              <li class="flex items-start" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''" :style="$i18n.locale === 'ar' ? { direction: 'rtl' } : { direction: 'ltr' }">
                <svg class="w-6 h-6 text-white flex-shrink-0" :class="$i18n.locale === 'ar' ? 'ml-3' : 'mr-3'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                <span class="flex-1" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'" :style="$i18n.locale === 'ar' ? { textAlign: 'right' } : { textAlign: 'left' }">{{ $t('pricing.standard.features.qrCode') }}</span>
              </li>
              <li class="flex items-start" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''" :style="$i18n.locale === 'ar' ? { direction: 'rtl' } : { direction: 'ltr' }">
                <svg class="w-6 h-6 text-white flex-shrink-0" :class="$i18n.locale === 'ar' ? 'ml-3' : 'mr-3'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                <span class="flex-1" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'" :style="$i18n.locale === 'ar' ? { textAlign: 'right' } : { textAlign: 'left' }">{{ $t('pricing.standard.features.orderManagement') }}</span>
              </li>
              <li class="flex items-start" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''" :style="$i18n.locale === 'ar' ? { direction: 'rtl' } : { direction: 'ltr' }">
                <svg class="w-6 h-6 text-white flex-shrink-0" :class="$i18n.locale === 'ar' ? 'ml-3' : 'mr-3'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                <span class="flex-1" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'" :style="$i18n.locale === 'ar' ? { textAlign: 'right' } : { textAlign: 'left' }">{{ $t('pricing.standard.features.customerManagement') }}</span>
              </li>
            </ul>
            <button
              @click="scrollToApply"
              class="w-full px-6 py-3 bg-white text-indigo-600 rounded-lg hover:bg-gray-100 transition-colors font-semibold"
            >
              {{ $t('nav.getStarted') }}
            </button>
          </div>

          <!-- Pro Plan -->
          <div 
            class="bg-gradient-to-br from-indigo-600 to-purple-600 rounded-2xl p-8 text-white relative overflow-hidden" 
            :dir="$i18n.locale === 'ar' ? 'rtl' : 'ltr'"
            :style="$i18n.locale === 'ar' ? { direction: 'rtl', textAlign: 'right' } : { direction: 'ltr', textAlign: 'left' }"
          >
            <div class="absolute" :class="$i18n.locale === 'ar' ? 'top-4 left-4' : 'top-4 right-4'">
              <span class="bg-white text-indigo-600 px-3 py-1 rounded-full text-xs font-bold">{{ $t('pricing.pro.popular') }}</span>
            </div>
            <h3 class="text-2xl font-bold mb-2" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'">{{ $t('pricing.pro.title') }}</h3>
            <div class="mb-6" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'">
              <span class="text-5xl font-bold">{{ $t('pricing.pro.price') }}</span>
              <span class="opacity-90" :class="$i18n.locale === 'ar' ? 'mr-2' : 'ml-2'">{{ $t('pricing.pro.month') }}</span>
            </div>
            <ul class="space-y-4 mb-8" :dir="$i18n.locale === 'ar' ? 'rtl' : 'ltr'">
              <li class="flex items-start" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''" :style="$i18n.locale === 'ar' ? { direction: 'rtl' } : { direction: 'ltr' }">
                <svg class="w-6 h-6 text-white flex-shrink-0" :class="$i18n.locale === 'ar' ? 'ml-3' : 'mr-3'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                <span class="flex-1" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'" :style="$i18n.locale === 'ar' ? { textAlign: 'right' } : { textAlign: 'left' }">{{ $t('pricing.pro.features.everything') }}</span>
              </li>
              <li class="flex items-start" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''" :style="$i18n.locale === 'ar' ? { direction: 'rtl' } : { direction: 'ltr' }">
                <svg class="w-6 h-6 text-white flex-shrink-0" :class="$i18n.locale === 'ar' ? 'ml-3' : 'mr-3'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                <span class="flex-1" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'" :style="$i18n.locale === 'ar' ? { textAlign: 'right' } : { textAlign: 'left' }">{{ $t('pricing.pro.features.customDomain') }}</span>
              </li>
              <li class="flex items-start" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''" :style="$i18n.locale === 'ar' ? { direction: 'rtl' } : { direction: 'ltr' }">
                <svg class="w-6 h-6 text-white flex-shrink-0" :class="$i18n.locale === 'ar' ? 'ml-3' : 'mr-3'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                <span class="flex-1" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'" :style="$i18n.locale === 'ar' ? { textAlign: 'right' } : { textAlign: 'left' }">{{ $t('pricing.pro.features.analytics') }}</span>
              </li>
              <li class="flex items-start" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''" :style="$i18n.locale === 'ar' ? { direction: 'rtl' } : { direction: 'ltr' }">
                <svg class="w-6 h-6 text-white flex-shrink-0" :class="$i18n.locale === 'ar' ? 'ml-3' : 'mr-3'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                <span class="flex-1" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'" :style="$i18n.locale === 'ar' ? { textAlign: 'right' } : { textAlign: 'left' }">{{ $t('pricing.pro.features.prioritySupport') }}</span>
              </li>
              <li class="flex items-start" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''" :style="$i18n.locale === 'ar' ? { direction: 'rtl' } : { direction: 'ltr' }">
                <svg class="w-6 h-6 text-white flex-shrink-0" :class="$i18n.locale === 'ar' ? 'ml-3' : 'mr-3'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                <span class="flex-1" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'" :style="$i18n.locale === 'ar' ? { textAlign: 'right' } : { textAlign: 'left' }">{{ $t('pricing.pro.features.emailMarketing') }}</span>
              </li>
            </ul>
            <button
              @click="scrollToApply"
              class="w-full px-6 py-3 bg-white text-indigo-600 rounded-lg hover:bg-gray-100 transition-colors font-semibold"
            >
              {{ $t('nav.getStarted') }}
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- Registration Form Section -->
    <section id="apply" class="py-20 bg-gradient-to-br from-indigo-600 via-purple-600 to-pink-600">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="bg-white rounded-2xl shadow-2xl p-8 md:p-12">
          <div class="text-center mb-8">
            <h2 class="text-4xl md:text-5xl font-bold text-gray-900 mb-4">{{ $t('registration.title') }}</h2>
            <p class="text-xl text-gray-600">
              {{ $t('registration.subtitle') }}
            </p>
          </div>

          <form @submit.prevent="submitRegistration" class="space-y-6">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">
                  {{ $t('registration.restaurantName') }} <span class="text-red-500">{{ $t('registration.required') }}</span>
                </label>
                <input
                  v-model="registrationForm.restaurant_name"
                  type="text"
                  required
                  class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  :placeholder="$t('registration.restaurantName')"
                />
              </div>

              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">
                  {{ $t('registration.yourName') }} <span class="text-red-500">{{ $t('registration.required') }}</span>
                </label>
                <input
                  v-model="registrationForm.owner_name"
                  type="text"
                  required
                  class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  :placeholder="$t('registration.yourName')"
                />
              </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">
                  {{ $t('registration.email') }} <span class="text-red-500">{{ $t('registration.required') }}</span>
                </label>
                <input
                  v-model="registrationForm.email"
                  type="email"
                  required
                  class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  placeholder="your@email.com"
                />
                <p class="text-xs text-gray-500 mt-1">{{ $t('registration.emailHint') }}</p>
              </div>

              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">
                  {{ $t('registration.phone') }} <span class="text-red-500">{{ $t('registration.required') }}</span>
                </label>
                <input
                  v-model="registrationForm.phone"
                  type="tel"
                  required
                  class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  :placeholder="$t('registration.phone')"
                />
              </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">
                  {{ $t('registration.password') }} <span class="text-red-500">{{ $t('registration.required') }}</span>
                </label>
                <input
                  v-model="registrationForm.password"
                  type="password"
                  required
                  minlength="6"
                  class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  :placeholder="$t('registration.password')"
                />
              </div>

              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">
                  {{ $t('registration.confirmPassword') }} <span class="text-red-500">{{ $t('registration.required') }}</span>
                </label>
                <input
                  v-model="registrationForm.confirmPassword"
                  type="password"
                  required
                  class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  :placeholder="$t('registration.confirmPassword')"
                />
              </div>
            </div>

            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-2">
                {{ $t('registration.address') }}
              </label>
              <input
                v-model="registrationForm.address"
                type="text"
                class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                :placeholder="$t('registration.address')"
              />
            </div>

            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-2">
                {{ $t('registration.description') }}
              </label>
              <textarea
                v-model="registrationForm.description"
                rows="3"
                class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                :placeholder="$t('registration.descriptionPlaceholder')"
              ></textarea>
            </div>

            <div v-if="registrationError" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
              {{ registrationError }}
            </div>

            <div v-if="registrationSuccess" class="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg text-sm">
              {{ registrationSuccess }}
            </div>

            <button
              type="submit"
              :disabled="submitting || !registrationForm.restaurant_name || !registrationForm.owner_name || !registrationForm.email || !registrationForm.password || !registrationForm.phone"
              class="w-full px-8 py-4 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-bold text-lg disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <span v-if="submitting">{{ $t('registration.submitting') }}</span>
              <span v-else>{{ $t('registration.submit') }}</span>
            </button>

            <p class="text-xs text-gray-500 text-center">
              {{ $t('registration.terms') }}
            </p>
          </form>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="bg-gray-900 text-white py-12">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="grid grid-cols-1 md:grid-cols-4 gap-8">
          <div>
            <h3 class="text-xl font-bold mb-4">RestaurantAI</h3>
            <p class="text-gray-400 text-sm">
              {{ $t('footer.tagline') }}
            </p>
          </div>
          <div>
            <h4 class="font-semibold mb-4">{{ $t('footer.product') }}</h4>
            <ul class="space-y-2 text-sm text-gray-400">
              <li><a href="#features" class="hover:text-white transition-colors">{{ $t('nav.features') }}</a></li>
              <li><a href="#pricing" class="hover:text-white transition-colors">{{ $t('nav.pricing') }}</a></li>
              <li><a href="#how-it-works" class="hover:text-white transition-colors">{{ $t('nav.howItWorks') }}</a></li>
            </ul>
          </div>
          <div>
            <h4 class="font-semibold mb-4">{{ $t('footer.company') }}</h4>
            <ul class="space-y-2 text-sm text-gray-400">
              <li><a href="#" class="hover:text-white transition-colors">{{ $t('footer.aboutUs') }}</a></li>
              <li><a href="#" class="hover:text-white transition-colors">{{ $t('footer.contact') }}</a></li>
              <li><router-link to="/super-admin/login" class="hover:text-white transition-colors">{{ $t('footer.superAdmin') }}</router-link></li>
            </ul>
          </div>
          <div>
            <h4 class="font-semibold mb-4">{{ $t('footer.legal') }}</h4>
            <ul class="space-y-2 text-sm text-gray-400">
              <li><a href="#" class="hover:text-white transition-colors">{{ $t('footer.terms') }}</a></li>
              <li><a href="#" class="hover:text-white transition-colors">{{ $t('footer.privacy') }}</a></li>
            </ul>
          </div>
        </div>
        <div class="border-t border-gray-800 mt-8 pt-8 text-center text-sm text-gray-400">
          <p>&copy; {{ new Date().getFullYear() }} RestaurantAI. {{ $t('footer.rights') }}</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import LanguageSwitcher from './LanguageSwitcher.vue';
import { useSEO } from '../composables/useSEO.js';

const { locale, t } = useI18n();
const router = useRouter();
const { setSEO, clearSEO } = useSEO();

const heroTexts = {
  en: [
    'restaurant they love',
    'local favorite',
    'success story',
    'go-to spot',
    'restaurant they line up for'
  ],
  ar: [
    'المطعم الذي يحبه الجميع',
    'المفضل المحلي',
    'قصة نجاح',
    'الوجهة المفضلة',
    'المطعم الذي يصطفون من أجله'
  ]
};
const heroText = ref(heroTexts[locale.value][0]);
let heroTextIndex = 0;

// Watch for locale changes to update hero text and RTL
watch(locale, (newLocale) => {
  heroText.value = heroTexts[newLocale][0];
  heroTextIndex = 0;
  
  // Update HTML dir and lang attributes
  document.documentElement.setAttribute('lang', newLocale);
  document.documentElement.setAttribute('dir', newLocale === 'ar' ? 'rtl' : 'ltr');
});

const registrationForm = ref({
  restaurant_name: '',
  owner_name: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
  address: '',
  description: ''
});

const submitting = ref(false);
const registrationError = ref('');
const registrationSuccess = ref('');

const scrollToApply = () => {
  const element = document.getElementById('apply');
  if (element) {
    element.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
};

const scrollToFeatures = () => {
  const element = document.getElementById('features');
  if (element) {
    element.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
};

const submitRegistration = async () => {
  // Validate passwords match
  if (registrationForm.value.password !== registrationForm.value.confirmPassword) {
    registrationError.value = 'Passwords do not match';
    return;
  }

  // Validate password length
  if (registrationForm.value.password.length < 6) {
    registrationError.value = 'Password must be at least 6 characters long';
    return;
  }

  submitting.value = true;
  registrationError.value = '';
  registrationSuccess.value = '';

  try {
    const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000';
    const response = await fetch(`${API_BASE_URL}/api/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        restaurant_name: registrationForm.value.restaurant_name,
        owner_name: registrationForm.value.owner_name,
        email: registrationForm.value.email,
        password: registrationForm.value.password,
        phone: registrationForm.value.phone,
        address: registrationForm.value.address,
        description: registrationForm.value.description
      }),
    });

    // Check if response is JSON
    const contentType = response.headers.get('content-type');
    if (!contentType || !contentType.includes('application/json')) {
      if (!response.ok) {
        throw new Error(`Server error: ${response.status} ${response.statusText}. Please check if the backend server is running.`);
      }
      throw new Error('Invalid response from server. Expected JSON but got HTML.');
    }

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to create account');
    }

    const data = await response.json();
    
    // Store access token, refresh token, and admin info
    localStorage.setItem('restaurantToken', data.token);
    if (data.refreshToken) localStorage.setItem('restaurantRefreshToken', data.refreshToken);
    localStorage.setItem('restaurantInfo', JSON.stringify(data.admin));
    
    registrationSuccess.value = 'Account created successfully! Redirecting to your dashboard...';
    
    // Redirect to restaurant dashboard after a short delay
    setTimeout(() => {
      router.push('/restaurant/dashboard');
    }, 1500);
  } catch (error) {
    console.error('Registration error:', error);
    registrationError.value = error.message || 'Failed to create account. Please try again.';
  } finally {
    submitting.value = false;
  }
};

// SEO Setup
onMounted(() => {
  setSEO({
    title: 'RestaurantAI - Restaurant Website Builder Platform | Online Ordering System',
    description: 'Create beautiful restaurant websites with online ordering, QR code menus, and order management. Start your restaurant website in minutes. Free for 3 months, then from $9.99/month.',
    keywords: ['restaurant website builder', 'online ordering system', 'restaurant management', 'QR code menu', 'food ordering platform', 'restaurant technology'],
    url: window.location.href,
    image: '/og-image.jpg',
    locale: locale.value === 'ar' ? 'ar_SA' : 'en_US',
    alternateLocale: locale.value === 'ar' ? 'en_US' : 'ar_SA',
    structuredData: {
      '@context': 'https://schema.org',
      '@type': 'SoftwareApplication',
      'name': 'RestaurantAI',
      'applicationCategory': 'BusinessApplication',
      'offers': {
        '@type': 'Offer',
        'price': '0',
        'priceCurrency': 'USD',
        'description': 'Free for 3 months, then from $9.99/month'
      },
      'aggregateRating': {
        '@type': 'AggregateRating',
        'ratingValue': '4.8',
        'ratingCount': '150'
      },
      'description': 'Create beautiful restaurant websites with online ordering, QR code menus, and order management.',
      'operatingSystem': 'Web',
      'url': window.location.origin
    }
  });

  // Animate hero text
  setInterval(() => {
    const currentTexts = heroTexts[locale.value];
    heroTextIndex = (heroTextIndex + 1) % currentTexts.length;
    heroText.value = currentTexts[heroTextIndex];
  }, 3000);
});

onUnmounted(() => {
  clearSEO();
});
</script>

<style scoped>
@keyframes gradient {
  0%, 100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

.animate-gradient {
  background-size: 200% 200%;
  animation: gradient 3s ease infinite;
}

.bg-grid-pattern {
  background-image: 
    linear-gradient(to right, rgba(0, 0, 0, 0.1) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(0, 0, 0, 0.1) 1px, transparent 1px);
  background-size: 20px 20px;
}

/* Force RTL for pricing cards when Arabic is selected */
[dir="rtl"] ul li {
  direction: rtl;
  text-align: right;
}

[dir="rtl"] ul li span {
  text-align: right !important;
}
</style>

