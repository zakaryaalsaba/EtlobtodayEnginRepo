<template>
  <div class="min-h-screen bg-gradient-to-br from-indigo-50 via-white to-purple-50" :dir="$i18n.locale === 'ar' ? 'rtl' : 'ltr'">
    <!-- Header -->
    <header class="bg-white shadow-sm border-b border-gray-200">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
        <div class="flex items-center justify-between" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
          <div>
            <h1 class="text-3xl font-bold text-gray-900">{{ $t('websiteBuilder.title') }}</h1>
            <p class="text-gray-600 mt-1">{{ $t('websiteBuilder.subtitle') }}</p>
          </div>
          <div class="flex items-center gap-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
            <button
              @click="toggleSidebar"
              class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors flex items-center gap-2"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
              </svg>
              {{ sidebarOpen ? 'Hide Dashboard' : 'Show Dashboard' }}
              <span v-if="pendingDriversCount > 0" class="ml-1 px-2 py-0.5 bg-red-500 text-white text-xs rounded-full">
                {{ pendingDriversCount }}
              </span>
            </button>
            <button
              @click="refreshSite"
              class="px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors flex items-center gap-2"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              Refresh
            </button>
            <LanguageSwitcher />
          </div>
        </div>
      </div>
    </header>

    <div class="flex h-[calc(100vh-80px)]">
      <!-- Sidebar Dashboard -->
      <div
        :class="[
          'bg-white border-r border-gray-200 transition-all duration-300 ease-in-out overflow-y-auto',
          sidebarOpen ? 'w-80' : 'w-0 overflow-hidden',
          $i18n.locale === 'ar' ? 'border-l border-r-0' : ''
        ]"
      >
        <div v-if="sidebarOpen" class="p-6">
          <!-- Sidebar Header -->
          <div class="mb-6">
            <h2 class="text-xl font-bold text-gray-900 mb-2">Dashboard</h2>
            <p class="text-sm text-gray-600">Manage your platform</p>
          </div>

          <!-- Navigation Menu -->
          <nav class="space-y-2">
            <button
              @click="setDashboardView('manage-restaurants')"
              :class="[
                'w-full flex items-center justify-between px-4 py-3 rounded-lg transition-colors text-left',
                activeDashboardView === 'manage-restaurants'
                  ? 'bg-indigo-100 text-indigo-700 font-semibold'
                  : 'text-gray-700 hover:bg-gray-100'
              ]"
            >
              <div class="flex items-center gap-3">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                </svg>
                <span>Manage Restaurants</span>
              </div>
            </button>

            <button
              @click="setDashboardView('restaurants-table')"
              :class="[
                'w-full flex items-center justify-between px-4 py-3 rounded-lg transition-colors text-left',
                activeDashboardView === 'restaurants-table'
                  ? 'bg-indigo-100 text-indigo-700 font-semibold'
                  : 'text-gray-700 hover:bg-gray-100'
              ]"
            >
              <div class="flex items-center gap-3">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                </svg>
                <span>{{ $t('websiteBuilder.restaurantsPage', 'المطاعم') }}</span>
              </div>
              <span class="text-xs bg-gray-200 text-gray-600 px-2 py-1 rounded-full">
                {{ websites.length }}
              </span>
            </button>

            <button
              @click="setDashboardView('orders')"
              :class="[
                'w-full flex items-center justify-between px-4 py-3 rounded-lg transition-colors text-left',
                activeDashboardView === 'orders'
                  ? 'bg-indigo-100 text-indigo-700 font-semibold'
                  : 'text-gray-700 hover:bg-gray-100'
              ]"
            >
              <div class="flex items-center gap-3">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
                </svg>
                <span>{{ $t('websiteBuilder.manageOrders') }}</span>
              </div>
            </button>

            <button
              @click="setDashboardView('delivery-company')"
              :class="[
                'w-full flex items-center justify-between px-4 py-3 rounded-lg transition-colors text-left',
                activeDashboardView === 'delivery-company'
                  ? 'bg-indigo-100 text-indigo-700 font-semibold'
                  : 'text-gray-700 hover:bg-gray-100'
              ]"
            >
              <div class="flex items-center gap-3">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
                </svg>
                <span>{{ $t('websiteBuilder.manageDeliveryCompanies') }}</span>
              </div>
            </button>

            <button
              @click="setDashboardView('drivers')"
              :class="[
                'w-full flex items-center justify-between px-4 py-3 rounded-lg transition-colors text-left',
                activeDashboardView === 'drivers'
                  ? 'bg-indigo-100 text-indigo-700 font-semibold'
                  : 'text-gray-700 hover:bg-gray-100'
              ]"
            >
              <div class="flex items-center gap-3">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                <span>Manage Drivers</span>
              </div>
              <span v-if="pendingDriversCount > 0" class="text-xs bg-red-500 text-white px-2 py-1 rounded-full">
                {{ pendingDriversCount }}
              </span>
            </button>

            <button
              @click="setDashboardView('restaurants')"
              :class="[
                'w-full flex items-center justify-between px-4 py-3 rounded-lg transition-colors text-left',
                activeDashboardView === 'restaurants'
                  ? 'bg-indigo-100 text-indigo-700 font-semibold'
                  : 'text-gray-700 hover:bg-gray-100'
              ]"
            >
              <div class="flex items-center gap-3">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                </svg>
                <span>Manage Website</span>
              </div>
              <span class="text-xs bg-gray-200 text-gray-600 px-2 py-1 rounded-full">
                {{ websites.length }}
              </span>
            </button>
          </nav>
        </div>
      </div>

      <!-- Main Content Area -->
      <div class="flex-1 overflow-y-auto">
        <!-- Manage Restaurants Section -->
        <div 
          v-if="activeDashboardView === 'manage-restaurants'" 
          class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8"
        >
          <div class="mb-6 flex items-center justify-between" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
            <div>
              <h2 class="text-3xl font-bold text-gray-900">Manage Restaurants</h2>
              <p class="text-gray-600 mt-2">Today's statistics across all restaurants</p>
            </div>
            <button
              @click="async () => { await loadTodayStatistics(); if(selectedRestaurantCard) { await loadTodayOrders(selectedRestaurantCard); } }"
              :disabled="loadingStats || loadingTodayOrders"
              class="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <svg 
                :class="['w-5 h-5', (loadingStats || loadingTodayOrders) ? 'animate-spin' : '']"
                fill="none" 
                stroke="currentColor" 
                viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              <span>Refresh</span>
            </button>
          </div>

          <!-- Statistics Cards -->
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-6 mb-8">
            <!-- Orders Count Card -->
            <div 
              @click="loadTodayOrders('all-orders')"
              :class="[
                'bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl p-6 text-white shadow-lg hover:shadow-xl transition-shadow cursor-pointer',
                selectedRestaurantCard === 'all-orders' ? 'ring-4 ring-blue-300 ring-offset-2' : ''
              ]">
              <div class="flex items-center justify-between mb-4">
                <div class="bg-white/20 rounded-lg p-3">
                  <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                  </svg>
                </div>
              </div>
              <h3 class="text-blue-100 text-sm font-medium mb-1">Today's Orders</h3>
              <p v-if="loadingStats" class="text-4xl font-bold animate-pulse">...</p>
              <p v-else class="text-4xl font-bold">{{ todayStats.ordersCount || 0 }}</p>
              <p class="text-blue-100 text-sm mt-2">All restaurants</p>
            </div>

            <!-- Total Paid to Restaurants and Drivers Card -->
            <div 
              @click="loadTodayOrders('total-paid')"
              :class="[
                'bg-gradient-to-br from-red-500 to-red-600 rounded-xl p-6 text-white shadow-lg hover:shadow-xl transition-shadow cursor-pointer',
                selectedRestaurantCard === 'total-paid' ? 'ring-4 ring-red-300 ring-offset-2' : ''
              ]">
              <div class="flex items-center justify-between mb-4">
                <div class="bg-white/20 rounded-lg p-3">
                  <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z" />
                  </svg>
                </div>
              </div>
              <div class="flex items-center gap-2 mb-1">
                <h3 class="text-red-100 text-sm font-medium">Gross/Total Money</h3>
                <div class="group relative">
                  <svg class="w-4 h-4 text-red-100 cursor-help" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <div class="absolute bottom-full left-1/2 transform -translate-x-1/2 mb-2 w-64 p-3 bg-gray-900 text-white text-xs rounded-lg shadow-xl opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-10">
                    <p class="font-semibold mb-1">Calculation:</p>
                    <p>Sum of total amount (Restaurants + Drivers + Tax)</p>
                    <p class="mt-1 text-gray-300">This is the gross total money from all completed orders</p>
                    <div class="absolute bottom-0 left-1/2 transform -translate-x-1/2 translate-y-full w-0 h-0 border-l-4 border-r-4 border-t-4 border-transparent border-t-gray-900"></div>
                  </div>
                </div>
              </div>
              <p v-if="loadingStats" class="text-4xl font-bold animate-pulse">...</p>
              <p v-else class="text-4xl font-bold">${{ todayStats.totalPaidToRestaurantsAndDrivers || '0.00' }}</p>
              <p class="text-red-100 text-sm mt-2">Restaurants + Drivers + Tax</p>
            </div>

            <!-- Total Owed to Restaurants Card -->
            <div 
              @click="loadTodayOrders('total-owed')"
              :class="[
                'bg-gradient-to-br from-orange-500 to-orange-600 rounded-xl p-6 text-white shadow-lg hover:shadow-xl transition-shadow relative cursor-pointer',
                selectedRestaurantCard === 'total-owed' ? 'ring-4 ring-orange-300 ring-offset-2' : ''
              ]">
              <div class="flex items-center justify-between mb-4">
                <div class="bg-white/20 rounded-lg p-3">
                  <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z" />
                  </svg>
                </div>
              </div>
              <div class="flex items-center gap-2 mb-1">
                <h3 class="text-orange-100 text-sm font-medium">Total Owed</h3>
                <div class="group relative">
                  <svg class="w-4 h-4 text-orange-100 cursor-help" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <div class="absolute bottom-full left-1/2 transform -translate-x-1/2 mb-2 w-64 p-3 bg-gray-900 text-white text-xs rounded-lg shadow-xl opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-10">
                    <p class="font-semibold mb-1">Calculation:</p>
                    <p>Sum of (buying price + tax)</p>
                    <p class="mt-1 text-gray-300">Total owed to restaurants from all completed orders</p>
                    <div class="absolute bottom-0 left-1/2 transform -translate-x-1/2 translate-y-full w-0 h-0 border-l-4 border-r-4 border-t-4 border-transparent border-t-gray-900"></div>
                  </div>
                </div>
              </div>
              <p v-if="loadingStats" class="text-4xl font-bold animate-pulse">...</p>
              <p v-else class="text-4xl font-bold">${{ todayStats.totalOwedToRestaurants || '0.00' }}</p>
              <p class="text-orange-100 text-sm mt-2">Restaurant only</p>
            </div>

            <!-- Revenue Card -->
            <div 
              @click="loadTodayOrders('revenue')"
              :class="[
                'bg-gradient-to-br from-green-500 to-green-600 rounded-xl p-6 text-white shadow-lg hover:shadow-xl transition-shadow cursor-pointer',
                selectedRestaurantCard === 'revenue' ? 'ring-4 ring-green-300 ring-offset-2' : ''
              ]">
              <div class="flex items-center justify-between mb-4">
                <div class="bg-white/20 rounded-lg p-3">
                  <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
              </div>
              <div class="flex items-center gap-2 mb-1">
                <h3 class="text-green-100 text-sm font-medium">Today's Revenue</h3>
                <div class="group relative">
                  <svg class="w-4 h-4 text-green-100 cursor-help" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <div class="absolute bottom-full left-1/2 transform -translate-x-1/2 mb-2 w-64 p-3 bg-gray-900 text-white text-xs rounded-lg shadow-xl opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-10">
                    <p class="font-semibold mb-1">Calculation:</p>
                    <p>Gross/Total Money - (Total Owed + Delivery Fees)</p>
                    <p class="mt-1 text-gray-300">Platform profit from all completed orders today</p>
                    <div class="absolute bottom-0 left-1/2 transform -translate-x-1/2 translate-y-full w-0 h-0 border-l-4 border-r-4 border-t-4 border-transparent border-t-gray-900"></div>
                  </div>
                </div>
              </div>
              <p v-if="loadingStats" class="text-4xl font-bold animate-pulse">...</p>
              <p v-else class="text-4xl font-bold">${{ todayStats.revenue || '0.00' }}</p>
            </div>

            <!-- Delivery Fees Card -->
            <div 
              @click="loadTodayOrders('delivery-fees')"
              :class="[
                'bg-gradient-to-br from-purple-500 to-purple-600 rounded-xl p-6 text-white shadow-lg hover:shadow-xl transition-shadow cursor-pointer',
                selectedRestaurantCard === 'delivery-fees' ? 'ring-4 ring-purple-300 ring-offset-2' : ''
              ]">
              <div class="flex items-center justify-between mb-4">
                <div class="bg-white/20 rounded-lg p-3">
                  <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
                  </svg>
                </div>
              </div>
              <h3 class="text-purple-100 text-sm font-medium mb-1">Today's Delivery Fees</h3>
              <p v-if="loadingStats" class="text-4xl font-bold animate-pulse">...</p>
              <p v-else class="text-4xl font-bold">${{ todayStats.deliveryFees || '0.00' }}</p>
              <p class="text-purple-100 text-sm mt-2">Delivery orders</p>
            </div>
          </div>

          <!-- Orders Table (shown when a card is clicked) -->
          <div v-if="selectedRestaurantCard" class="mt-8 bg-white rounded-lg shadow-lg p-6">
            <div class="flex items-center justify-between mb-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
              <h3 class="text-xl font-semibold text-gray-800">
                {{ selectedRestaurantCard === 'all-orders' ? 'Today\'s Orders' : 
                   selectedRestaurantCard === 'total-paid' ? 'Orders - Gross/Total Money' :
                   selectedRestaurantCard === 'total-owed' ? 'Orders - Total Owed' :
                   selectedRestaurantCard === 'revenue' ? 'Orders - Revenue' :
                   selectedRestaurantCard === 'delivery-fees' ? 'Delivery Orders' : 'Orders' }}
              </h3>
              <button
                @click="selectedRestaurantCard = null; todayOrders = []; restaurantsOwed = []"
                class="text-gray-500 hover:text-gray-700"
              >
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <div v-if="loadingTodayOrders" class="text-center py-12">
              <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
              <p class="mt-4 text-gray-600">Loading orders...</p>
            </div>

            <div v-else-if="todayOrders.length === 0 && restaurantsOwed.length === 0 && selectedRestaurantCard !== 'delivery-fees' && selectedRestaurantCard !== 'total-owed'" class="text-center py-12">
              <div class="text-4xl mb-4">📦</div>
              <p class="text-gray-600 font-semibold">No orders found for today</p>
            </div>

            <!-- Restaurants Owed view for Total Owed card -->
            <div v-else-if="selectedRestaurantCard === 'total-owed'" class="overflow-x-auto">
              <div v-if="restaurantsOwed.length === 0" class="text-center py-12">
                <div class="text-4xl mb-4">🏪</div>
                <p class="text-gray-600 font-semibold">No restaurants with completed orders today</p>
              </div>
              <table v-else class="min-w-full divide-y divide-gray-200">
                <thead class="bg-gray-50">
                  <tr>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Restaurant</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Orders Count</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Total Buying Price</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Total Tax</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Total Owed</th>
                  </tr>
                </thead>
                <tbody class="bg-white divide-y divide-gray-200">
                  <tr v-for="restaurant in restaurantsOwed" :key="restaurant.website_id" class="hover:bg-gray-50">
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{{ restaurant.restaurant_name }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ restaurant.orders_count }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-700">${{ parseFloat(restaurant.total_buying_price || 0).toFixed(2) }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-700">${{ parseFloat(restaurant.total_tax || 0).toFixed(2) }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-semibold text-orange-600">${{ parseFloat(restaurant.total_owed || 0).toFixed(2) }}</td>
                  </tr>
                </tbody>
                <tfoot class="bg-gray-50">
                  <tr>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-bold text-gray-900" colspan="4">Total Owed to All Restaurants</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-bold text-orange-600">
                      ${{ restaurantsOwed.reduce((sum, r) => sum + parseFloat(r.total_owed || 0), 0).toFixed(2) }}
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>

            <!-- Driver-based view for delivery fees -->
            <div v-else-if="selectedRestaurantCard === 'delivery-fees'" class="space-y-6">
              <div v-for="driverGroup in ordersByDriver" :key="driverGroup.driver_id || 'unassigned'" class="border border-gray-200 rounded-lg overflow-hidden">
                <!-- Driver Header -->
                <div class="bg-gradient-to-r from-purple-50 to-purple-100 px-6 py-4 border-b border-gray-200">
                  <div class="flex items-center justify-between">
                    <div>
                      <h4 class="text-lg font-semibold text-gray-900">{{ driverGroup.driver_name }}</h4>
                      <div class="flex items-center gap-4 mt-1 text-sm text-gray-600">
                        <span>📞 {{ driverGroup.driver_phone }}</span>
                        <span v-if="driverGroup.driver_email && driverGroup.driver_email !== 'N/A'">✉️ {{ driverGroup.driver_email }}</span>
                      </div>
                    </div>
                    <div class="text-right">
                      <p class="text-sm text-gray-600">Total Delivery Fees</p>
                      <p class="text-2xl font-bold text-purple-600">${{ driverGroup.totalDeliveryFees.toFixed(2) }}</p>
                      <p class="text-xs text-gray-500 mt-1">{{ driverGroup.orders.length }} order(s)</p>
                    </div>
                  </div>
                </div>
                
                <!-- Driver's Orders Table -->
                <div class="overflow-x-auto">
                  <table class="min-w-full divide-y divide-gray-200">
                    <thead class="bg-gray-50">
                      <tr>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Order #</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Restaurant</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Customer</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Price</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Buying Price</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Tax</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Delivery Fees</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Date</th>
                      </tr>
                    </thead>
                    <tbody class="bg-white divide-y divide-gray-200">
                      <tr v-for="order in driverGroup.orders" :key="order.id" class="hover:bg-gray-50">
                        <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{{ order.order_number }}</td>
                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ getRestaurantName(order.website_id) }}</td>
                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ order.customer_name || 'N/A' }}</td>
                        <td class="px-6 py-4 whitespace-nowrap">
                          <span :class="getStatusBadgeClass(order.status)" class="px-2 py-1 text-xs font-semibold rounded-full">
                            {{ order.status }}
                          </span>
                        </td>
                        <td class="px-6 py-4 whitespace-nowrap text-sm font-semibold text-gray-900">${{ (parseFloat(order.total_amount || 0) - parseFloat(order.tax || 0) - parseFloat(order.delivery_fees || 0)).toFixed(2) }}</td>
                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-700">${{ parseFloat(order.total_original_amount || 0).toFixed(2) }}</td>
                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-700">${{ parseFloat(order.tax || 0).toFixed(2) }}</td>
                        <td class="px-6 py-4 whitespace-nowrap text-sm font-semibold text-purple-600">${{ parseFloat(order.delivery_fees || 0).toFixed(2) }}</td>
                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ formatDate(order.created_at) }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            <!-- Gross/Total Money breakdown view -->
            <div v-else-if="selectedRestaurantCard === 'total-paid'" class="overflow-x-auto">
              <div v-if="todayOrders.length === 0" class="text-center py-12">
                <div class="text-4xl mb-4">💵</div>
                <p class="text-gray-600 font-semibold">No completed orders found for today</p>
              </div>
              <table v-else class="min-w-full divide-y divide-gray-200">
                <thead class="bg-gray-50">
                  <tr>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Order #</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Restaurant</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Customer</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Gross/Total Money</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Total Owed</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Delivery Fees</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Revenue</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Date</th>
                  </tr>
                </thead>
                <tbody class="bg-white divide-y divide-gray-200">
                  <tr v-for="order in todayOrders" :key="order.id" class="hover:bg-gray-50">
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{{ order.order_number }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ getRestaurantName(order.website_id) }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ order.customer_name || 'N/A' }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ order.order_type || 'N/A' }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-bold text-red-600">${{ parseFloat(order.total_amount || 0).toFixed(2) }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-700">${{ (parseFloat(order.total_original_amount || 0) + parseFloat(order.tax || 0)).toFixed(2) }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-700">${{ parseFloat(order.delivery_fees || 0).toFixed(2) }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-bold text-green-600">
                      ${{ (parseFloat(order.total_amount || 0) - (parseFloat(order.total_original_amount || 0) + parseFloat(order.tax || 0) + parseFloat(order.delivery_fees || 0))).toFixed(2) }}
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ formatDate(order.created_at) }}</td>
                  </tr>
                </tbody>
                <tfoot class="bg-gray-50">
                  <tr>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-bold text-gray-900" colspan="4">Total Gross/Total Money</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-bold text-red-600">
                      ${{ todayOrders.reduce((sum, o) => sum + parseFloat(o.total_amount || 0), 0).toFixed(2) }}
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-semibold text-gray-700">
                      ${{ todayOrders.reduce((sum, o) => sum + parseFloat(o.total_original_amount || 0) + parseFloat(o.tax || 0), 0).toFixed(2) }}
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-semibold text-gray-700">
                      ${{ todayOrders.reduce((sum, o) => sum + parseFloat(o.delivery_fees || 0), 0).toFixed(2) }}
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-bold text-green-600">
                      ${{ todayOrders.reduce((sum, o) => sum + parseFloat(o.total_amount || 0) - (parseFloat(o.total_original_amount || 0) + parseFloat(o.tax || 0) + parseFloat(o.delivery_fees || 0)), 0).toFixed(2) }}
                    </td>
                    <td></td>
                  </tr>
                </tfoot>
              </table>
            </div>

            <!-- Revenue breakdown view for Revenue card -->
            <div v-else-if="selectedRestaurantCard === 'revenue'" class="overflow-x-auto">
              <div v-if="todayOrders.length === 0" class="text-center py-12">
                <div class="text-4xl mb-4">💰</div>
                <p class="text-gray-600 font-semibold">No completed orders found for today</p>
              </div>
              <table v-else class="min-w-full divide-y divide-gray-200">
                <thead class="bg-gray-50">
                  <tr>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Order #</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Restaurant</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Customer</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Gross/Total Money</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Total Owed</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Delivery Fees</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Revenue</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Date</th>
                  </tr>
                </thead>
                <tbody class="bg-white divide-y divide-gray-200">
                  <tr v-for="order in todayOrders" :key="order.id" class="hover:bg-gray-50">
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{{ order.order_number }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ getRestaurantName(order.website_id) }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ order.customer_name || 'N/A' }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ order.order_type || 'N/A' }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-semibold text-gray-900">${{ parseFloat(order.total_amount || 0).toFixed(2) }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-700">${{ (parseFloat(order.total_original_amount || 0) + parseFloat(order.tax || 0)).toFixed(2) }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-700">${{ parseFloat(order.delivery_fees || 0).toFixed(2) }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-bold text-green-600">
                      ${{ (parseFloat(order.total_amount || 0) - (parseFloat(order.total_original_amount || 0) + parseFloat(order.tax || 0) + parseFloat(order.delivery_fees || 0))).toFixed(2) }}
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ formatDate(order.created_at) }}</td>
                  </tr>
                </tbody>
                <tfoot class="bg-gray-50">
                  <tr>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-bold text-gray-900" colspan="4">Total Revenue</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-semibold text-gray-700">
                      ${{ todayOrders.reduce((sum, o) => sum + parseFloat(o.total_amount || 0), 0).toFixed(2) }}
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-semibold text-gray-700">
                      ${{ todayOrders.reduce((sum, o) => sum + parseFloat(o.total_original_amount || 0) + parseFloat(o.tax || 0), 0).toFixed(2) }}
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-semibold text-gray-700">
                      ${{ todayOrders.reduce((sum, o) => sum + parseFloat(o.delivery_fees || 0), 0).toFixed(2) }}
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-bold text-green-600">
                      ${{ todayOrders.reduce((sum, o) => sum + parseFloat(o.total_amount || 0) - (parseFloat(o.total_original_amount || 0) + parseFloat(o.tax || 0) + parseFloat(o.delivery_fees || 0)), 0).toFixed(2) }}
                    </td>
                    <td></td>
                  </tr>
                </tfoot>
              </table>
            </div>

            <!-- Regular table view for other cards -->
            <div v-else class="overflow-x-auto">
              <table class="min-w-full divide-y divide-gray-200">
                <thead class="bg-gray-50">
                  <tr>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Order #</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Restaurant</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Customer</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Price</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Buying Price</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Tax</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Delivery Fees</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Date</th>
                  </tr>
                </thead>
                <tbody class="bg-white divide-y divide-gray-200">
                  <tr v-for="order in todayOrders" :key="order.id" class="hover:bg-gray-50">
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{{ order.order_number }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ getRestaurantName(order.website_id) }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ order.customer_name || 'N/A' }}</td>
                    <td class="px-6 py-4 whitespace-nowrap">
                      <span :class="getStatusBadgeClass(order.status)" class="px-2 py-1 text-xs font-semibold rounded-full">
                        {{ order.status }}
                      </span>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ order.order_type || 'N/A' }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-semibold text-gray-900">${{ (parseFloat(order.total_amount || 0) - parseFloat(order.tax || 0) - parseFloat(order.delivery_fees || 0)).toFixed(2) }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-700">${{ parseFloat(order.total_original_amount || 0).toFixed(2) }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-700">${{ parseFloat(order.tax || 0).toFixed(2) }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-700">${{ parseFloat(order.delivery_fees || 0).toFixed(2) }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ formatDate(order.created_at) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Restaurants Table Section (المطاعم) -->
        <div
          v-if="activeDashboardView === 'restaurants-table'"
          class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8"
        >
          <div class="bg-white rounded-lg shadow-lg p-8">
            <div class="flex items-center justify-between mb-6" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
              <h2 class="text-2xl font-semibold text-gray-800">{{ $t('websiteBuilder.restaurantsPage', 'المطاعم') }}</h2>
              <button
                @click="openRestaurantModal()"
                class="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                </svg>
                <span>{{ $t('websiteBuilder.addRestaurant', 'إضافة مطعم جديد') }}</span>
              </button>
            </div>

            <div v-if="loadingRestaurantsTable" class="text-center py-12">
              <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
              <p class="mt-4 text-gray-600">{{ $t('websiteBuilder.loadingRestaurants', 'جاري تحميل المطاعم...') }}</p>
            </div>

            <div v-else-if="websites.length === 0" class="text-center py-12">
              <div class="text-4xl mb-4">🍽️</div>
              <p class="text-gray-600 font-semibold">{{ $t('websiteBuilder.noRestaurantsYet', 'لا توجد مطاعم بعد') }}</p>
              <p class="text-sm text-gray-500 mt-2">{{ $t('websiteBuilder.addFirstRestaurant', 'اضغط على "إضافة مطعم جديد" للبدء') }}</p>
              <button
                @click="openRestaurantModal()"
                class="mt-4 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
              >
                {{ $t('websiteBuilder.addRestaurant', 'إضافة مطعم جديد') }}
              </button>
            </div>

            <div v-else class="overflow-x-auto">
              <table class="min-w-full divide-y divide-gray-200">
                <thead class="bg-gray-50">
                  <tr>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('websiteBuilder.restaurantName', 'اسم المطعم') }}</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('websiteBuilder.locationLabel', 'الموقع') }}</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('websiteBuilder.phoneLabel', 'رقم الهاتف') }}</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('websiteBuilder.emailLabel', 'البريد الإلكتروني') }}</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('websiteBuilder.actions', 'إجراءات') }}</th>
                  </tr>
                </thead>
                <tbody class="bg-white divide-y divide-gray-200">
                  <tr v-for="site in websites" :key="site.id" class="hover:bg-gray-50">
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{{ site.restaurant_name || '—' }}</td>
                    <td class="px-6 py-4 text-sm text-gray-600 max-w-xs truncate">{{ getRestaurantLocation(site) || '—' }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">{{ site.phone || '—' }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">{{ site.email || '—' }}</td>
                    <td class="px-6 py-4 whitespace-nowrap">
                      <div class="flex gap-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                        <button
                          @click="openRestaurantModal(site)"
                          class="px-3 py-1.5 bg-indigo-100 text-indigo-700 text-sm rounded-lg hover:bg-indigo-200 transition-colors"
                        >
                          {{ $t('websiteBuilder.edit', 'تعديل') }}
                        </button>
                        <button
                          @click="deleteRestaurantFromTable(site.id)"
                          class="px-3 py-1.5 bg-red-100 text-red-700 text-sm rounded-lg hover:bg-red-200 transition-colors"
                        >
                          {{ $t('websiteBuilder.delete', 'حذف') }}
                        </button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Restaurant Add/Edit Modal (المطاعم) -->
        <Teleport to="body">
          <div
            v-if="showRestaurantModal"
            class="fixed inset-0 z-50 overflow-y-auto"
            aria-labelledby="modal-title"
            role="dialog"
            aria-modal="true"
          >
            <div class="flex items-center justify-center min-h-screen px-4 pt-4 pb-20 text-center sm:p-0">
              <div class="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity" aria-hidden="true" @click="closeRestaurantModal()"></div>
              <div class="relative inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'">
                <div class="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                  <h3 class="text-lg font-semibold text-gray-900 mb-4" id="modal-title">
                    {{ editingRestaurantForTable ? $t('websiteBuilder.editRestaurant', 'تعديل المطعم') : $t('websiteBuilder.addRestaurant', 'إضافة مطعم جديد') }}
                  </h3>
                  <div class="space-y-4">
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.restaurantName', 'اسم المطعم') }} <span class="text-red-500">*</span></label>
                      <input
                        v-model="restaurantForm.name"
                        type="text"
                        :placeholder="$t('websiteBuilder.enterRestaurantName', 'أدخل اسم المطعم')"
                        class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      />
                    </div>
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.locationLabel', 'الموقع') }}</label>
                      <input
                        v-model="restaurantForm.location"
                        type="text"
                        :placeholder="$t('websiteBuilder.addressPlaceholder', 'العنوان')"
                        class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      />
                    </div>
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.phoneLabel', 'رقم الهاتف') }}</label>
                      <input
                        v-model="restaurantForm.phone"
                        type="text"
                        :placeholder="$t('websiteBuilder.phonePlaceholder', '05xxxxxxxx')"
                        class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      />
                    </div>
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.emailLabel', 'البريد الإلكتروني') }}</label>
                      <input
                        v-model="restaurantForm.email"
                        type="email"
                        :placeholder="$t('websiteBuilder.emailPlaceholder', 'contact@restaurant.com')"
                        class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      />
                    </div>
                  </div>
                </div>
                <div class="bg-gray-50 px-4 py-3 sm:px-6 flex gap-3" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <button
                    type="button"
                    @click="closeRestaurantModal()"
                    class="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-gray-700 bg-white hover:bg-gray-50 transition-colors"
                  >
                    {{ $t('websiteBuilder.cancel', 'إلغاء') }}
                  </button>
                  <button
                    type="button"
                    @click="saveRestaurantModal()"
                    :disabled="savingRestaurantModal || !restaurantForm.name?.trim()"
                    class="flex-1 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {{ savingRestaurantModal ? $t('websiteBuilder.saving', 'جاري الحفظ...') : (editingRestaurantForTable ? $t('websiteBuilder.saveChanges', 'حفظ التغييرات') : $t('websiteBuilder.addRestaurant', 'إضافة مطعم جديد')) }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </Teleport>

        <!-- Orders Management Section -->
        <div 
          v-if="activeDashboardView === 'orders'" 
          class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8"
        >
          <div class="bg-white rounded-lg shadow-lg p-8">
            <div class="flex items-center justify-between mb-6" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
              <h2 class="text-2xl font-semibold text-gray-800">{{ $t('websiteBuilder.manageOrders', 'Manage Orders') }}</h2>
              <button
                @click="loadOrders()"
                :disabled="loadingOrders"
                class="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <svg 
                  :class="['w-5 h-5', loadingOrders ? 'animate-spin' : '']"
                  fill="none" 
                  stroke="currentColor" 
                  viewBox="0 0 24 24"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                <span>{{ $t('websiteBuilder.refresh', 'Refresh') }}</span>
              </button>
            </div>

            <!-- Statistics Cards -->
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
              <!-- Pending Orders Card -->
              <div 
                @click="selectedOrderFilter = 'pending'"
                class="bg-gradient-to-br from-yellow-500 to-yellow-600 rounded-xl p-6 text-white shadow-lg cursor-pointer hover:shadow-xl transition-shadow"
              >
                <div class="flex items-center justify-between">
                  <div>
                    <p class="text-yellow-100 text-sm font-medium mb-1">{{ $t('websiteBuilder.pendingOrders', 'Pending Orders') }}</p>
                    <p class="text-3xl font-bold">{{ pendingOrdersCount }}</p>
                  </div>
                  <div class="bg-white/20 rounded-full p-3">
                    <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                  </div>
                </div>
              </div>

              <!-- Completed Orders Card -->
              <div 
                @click="selectedOrderFilter = 'completed'"
                class="bg-gradient-to-br from-green-500 to-green-600 rounded-xl p-6 text-white shadow-lg cursor-pointer hover:shadow-xl transition-shadow"
              >
                <div class="flex items-center justify-between">
                  <div>
                    <p class="text-green-100 text-sm font-medium mb-1">{{ $t('websiteBuilder.completedOrders', 'Completed Orders') }}</p>
                    <p class="text-3xl font-bold">{{ completedOrdersCount }}</p>
                  </div>
                  <div class="bg-white/20 rounded-full p-3">
                    <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                  </div>
                </div>
              </div>

              <!-- Today's Revenue Card -->
              <div 
                @click="selectedOrderFilter = 'today'"
                class="bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl p-6 text-white shadow-lg cursor-pointer hover:shadow-xl transition-shadow"
              >
                <div class="flex items-center justify-between">
                  <div>
                    <p class="text-blue-100 text-sm font-medium mb-1">{{ $t('websiteBuilder.todayRevenue', 'Today\'s Revenue') }}</p>
                    <p class="text-3xl font-bold">${{ todayRevenue.toFixed(2) }}</p>
                  </div>
                  <div class="bg-white/20 rounded-full p-3">
                    <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                  </div>
                </div>
              </div>

              <!-- Last 7 Days Revenue Card -->
              <div 
                @click="selectedOrderFilter = 'last7days'"
                class="bg-gradient-to-br from-purple-500 to-purple-600 rounded-xl p-6 text-white shadow-lg cursor-pointer hover:shadow-xl transition-shadow"
              >
                <div class="flex items-center justify-between">
                  <div>
                    <p class="text-purple-100 text-sm font-medium mb-1">{{ $t('websiteBuilder.last7DaysRevenue', 'Last 7 Days Revenue') }}</p>
                    <p class="text-3xl font-bold">${{ last7DaysRevenue.toFixed(2) }}</p>
                  </div>
                  <div class="bg-white/20 rounded-full p-3">
                    <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                    </svg>
                  </div>
                </div>
              </div>
            </div>

            <!-- Orders Table -->
            <div v-if="selectedOrderFilter" class="mt-8">
              <h3 class="text-xl font-semibold text-gray-800 mb-4">
                {{ getOrderFilterTitle() }}
              </h3>
              
              <div v-if="loadingOrders" class="text-center py-12">
                <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
                <p class="mt-4 text-gray-600">{{ $t('websiteBuilder.loadingOrders', 'Loading orders...') }}</p>
              </div>

              <div v-else-if="filteredOrders.length === 0" class="text-center py-12">
                <div class="text-4xl mb-4">📦</div>
                <p class="text-gray-600 font-semibold">{{ $t('websiteBuilder.noOrdersFound', 'No orders found.') }}</p>
              </div>

              <div v-else class="overflow-x-auto">
                <table class="min-w-full divide-y divide-gray-200">
                  <thead class="bg-gray-50">
                    <tr>
                      <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('websiteBuilder.orderNumber', 'Order #') }}</th>
                      <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('websiteBuilder.restaurant', 'Restaurant') }}</th>
                      <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('websiteBuilder.customer', 'Customer') }}</th>
                      <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('websiteBuilder.status', 'Status') }}</th>
                      <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('websiteBuilder.orderType', 'Order Type') }}</th>
                      <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('websiteBuilder.total', 'Total') }}</th>
                      <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('websiteBuilder.date', 'Date') }}</th>
                    </tr>
                  </thead>
                  <tbody class="bg-white divide-y divide-gray-200">
                    <tr v-for="order in filteredOrders" :key="order.id" class="hover:bg-gray-50">
                      <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{{ order.order_number }}</td>
                      <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ getRestaurantName(order.website_id) }}</td>
                      <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ order.customer_name || 'N/A' }}</td>
                      <td class="px-6 py-4 whitespace-nowrap">
                        <span :class="getStatusBadgeClass(order.status)" class="px-2 py-1 text-xs font-semibold rounded-full">
                          {{ order.status }}
                        </span>
                      </td>
                      <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ order.order_type || 'N/A' }}</td>
                      <td class="px-6 py-4 whitespace-nowrap text-sm font-semibold text-gray-900">${{ parseFloat(order.total_amount || 0).toFixed(2) }}</td>
                      <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ formatDate(order.created_at) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>

        <!-- Drivers Management Section -->
        <div 
          v-if="activeDashboardView === 'drivers'" 
          class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8"
        >
          <div class="bg-white rounded-lg shadow-lg p-8">
            <div class="flex items-center justify-between mb-6" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
              <h2 class="text-2xl font-semibold text-gray-800">{{ $t('websiteBuilder.manageDrivers', 'Manage Drivers') }}</h2>
              <button
                @click="loadDrivers()"
                :disabled="loadingDrivers"
                class="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <svg 
                  :class="['w-5 h-5', loadingDrivers ? 'animate-spin' : '']"
                  fill="none" 
                  stroke="currentColor" 
                  viewBox="0 0 24 24"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                <span>{{ $t('websiteBuilder.refresh', 'Refresh') }}</span>
              </button>
            </div>

            <!-- Statistics Cards -->
            <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
              <!-- Total Drivers Card -->
              <div class="bg-gradient-to-br from-indigo-500 to-indigo-600 rounded-xl p-6 text-white shadow-lg">
                <div class="flex items-center justify-between">
                  <div>
                    <p class="text-indigo-100 text-sm font-medium mb-1">{{ $t('websiteBuilder.totalDrivers', 'Total Drivers') }}</p>
                    <p class="text-3xl font-bold">{{ drivers.length }}</p>
                  </div>
                  <div class="bg-white/20 rounded-full p-3">
                    <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                    </svg>
                  </div>
                </div>
              </div>

              <!-- Online Drivers Card -->
              <div class="bg-gradient-to-br from-green-500 to-green-600 rounded-xl p-6 text-white shadow-lg">
                <div class="flex items-center justify-between">
                  <div>
                    <p class="text-green-100 text-sm font-medium mb-1">{{ $t('websiteBuilder.onlineDrivers', 'Online Drivers') }}</p>
                    <p class="text-3xl font-bold">{{ onlineDriversCount }}</p>
                  </div>
                  <div class="bg-white/20 rounded-full p-3">
                    <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                  </div>
                </div>
              </div>

              <!-- New Drivers (Last 7 Days) Card -->
              <div class="bg-gradient-to-br from-purple-500 to-purple-600 rounded-xl p-6 text-white shadow-lg">
                <div class="flex items-center justify-between">
                  <div>
                    <p class="text-purple-100 text-sm font-medium mb-1">{{ $t('websiteBuilder.newDriversLast7Days', 'New Drivers (7 Days)') }}</p>
                    <p class="text-3xl font-bold">{{ newDriversLast7DaysCount }}</p>
                  </div>
                  <div class="bg-white/20 rounded-full p-3">
                    <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                  </div>
                </div>
              </div>
            </div>

            <!-- Filter Tabs -->
            <div class="mb-6">
              <div class="flex gap-2 border-b border-gray-200" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <button
                  v-for="filter in driverFilters"
                  :key="filter.value"
                  @click="driverFilter = filter.value"
                  :class="[
                    'px-4 py-2 text-sm font-medium border-b-2 transition-colors',
                    driverFilter === filter.value
                      ? 'border-indigo-500 text-indigo-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  ]"
                >
                  {{ filter.label }}
                  <span v-if="filter.count !== undefined" class="ml-1 px-2 py-0.5 bg-gray-100 text-gray-600 text-xs rounded-full">
                    {{ filter.count }}
                  </span>
                </button>
              </div>
            </div>

            <!-- Loading State -->
            <div v-if="loadingDrivers" class="text-center py-12">
              <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
              <p class="mt-4 text-gray-600">{{ $t('websiteBuilder.loadingDrivers', 'Loading drivers...') }}</p>
            </div>

            <!-- Empty State -->
            <div v-else-if="filteredDrivers.length === 0" class="text-center py-12">
              <div class="text-4xl mb-4">🚗</div>
              <p class="text-gray-600 font-semibold">{{ $t('websiteBuilder.noDriversFound', 'No drivers found.') }}</p>
              <p class="text-sm text-gray-500 mt-2">{{ $t('websiteBuilder.tryAdjustingDriverFilter', 'Try adjusting your filter.') }}</p>
            </div>

            <!-- Drivers List -->
            <div v-else class="space-y-4">
              <div
                v-for="driver in filteredDrivers"
                :key="driver.id"
                class="bg-gray-50 rounded-lg p-4 border border-gray-200 hover:shadow-md transition-shadow"
              >
                <div class="flex items-start justify-between mb-3">
                  <div class="flex-1">
                    <h3 class="text-lg font-semibold text-gray-900">{{ driver.name }}</h3>
                    <p class="text-sm text-gray-600">{{ driver.email }}</p>
                    <p v-if="driver.phone" class="text-sm text-gray-600">{{ driver.phone }}</p>
                  </div>
                  <span
                    :class="[
                      'px-3 py-1 text-xs font-semibold rounded-full',
                      driver.status === 'approved' ? 'bg-green-100 text-green-800' :
                      driver.status === 'rejected' ? 'bg-red-100 text-red-800' :
                      'bg-yellow-100 text-yellow-800'
                    ]"
                  >
                    {{ driver.status === 'approved' ? $t('websiteBuilder.approvedDrivers', 'Approved') :
                       driver.status === 'rejected' ? $t('websiteBuilder.rejectedDrivers', 'Rejected') :
                       $t('websiteBuilder.pendingDrivers', 'Pending') }}
                  </span>
                </div>

                <div class="text-xs text-gray-500 mb-3">
                  {{ $t('websiteBuilder.registeredOn', 'Registered On') }}: {{ new Date(driver.created_at).toLocaleDateString() }}
                </div>

                <!-- Actions -->
                <div class="flex gap-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <button
                    v-if="driver.status === 'pending'"
                    @click="approveDriver(driver.id)"
                    :disabled="processingDriver"
                    class="flex-1 px-3 py-2 bg-green-600 text-white text-sm rounded-lg hover:bg-green-700 transition-colors disabled:opacity-50"
                  >
                    {{ $t('websiteBuilder.approve', 'Approve') }}
                  </button>
                  <button
                    v-if="driver.status === 'pending'"
                    @click="rejectDriver(driver.id)"
                    :disabled="processingDriver"
                    class="flex-1 px-3 py-2 bg-red-600 text-white text-sm rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50"
                  >
                    {{ $t('websiteBuilder.reject', 'Reject') }}
                  </button>
                  <button
                    @click="deleteDriver(driver.id)"
                    :disabled="processingDriver"
                    class="px-3 py-2 bg-gray-200 text-gray-800 text-sm rounded-lg hover:bg-gray-300 transition-colors disabled:opacity-50"
                  >
                    {{ $t('websiteBuilder.delete', 'Delete') }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- Manage Delivery Company Section -->
        <div 
          v-if="activeDashboardView === 'delivery-company'" 
          class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8"
        >
          <div class="bg-white rounded-lg shadow-lg p-8">
            <div class="flex items-center justify-between mb-6" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
              <h2 class="text-2xl font-semibold text-gray-800">{{ $t('websiteBuilder.manageDeliveryCompanies') }}</h2>
              <button
                @click="showDeliveryCompanyForm = true; editingDeliveryCompany = null; resetDeliveryCompanyForm()"
                class="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                </svg>
                <span>{{ $t('websiteBuilder.addDeliveryCompany') }}</span>
              </button>
            </div>

            <!-- Loading State -->
            <div v-if="loadingDeliveryCompanies" class="text-center py-12">
              <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
              <p class="mt-4 text-gray-600">{{ $t('websiteBuilder.loadingDeliveryCompanies') }}</p>
            </div>

            <!-- Empty State -->
            <div v-else-if="deliveryCompanies.length === 0" class="text-center py-12">
              <div class="text-4xl mb-4">🚚</div>
              <p class="text-gray-600 font-semibold">{{ $t('websiteBuilder.noDeliveryCompaniesFound') }}</p>
              <p class="text-sm text-gray-500 mt-2">{{ $t('websiteBuilder.clickAddDeliveryCompany') }}</p>
            </div>

            <!-- Delivery Companies Table -->
            <div v-else class="overflow-x-auto">
              <table class="min-w-full divide-y divide-gray-200">
                <thead class="bg-gray-50">
                  <tr>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('websiteBuilder.company') }}</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('websiteBuilder.contact') }}</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('websiteBuilder.phone') }}</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('websiteBuilder.emails') }}</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('websiteBuilder.status') }}</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('websiteBuilder.actions') }}</th>
                  </tr>
                </thead>
                <tbody class="bg-white divide-y divide-gray-200">
                  <tr v-for="company in deliveryCompanies" :key="company.id" class="hover:bg-gray-50">
                    <td class="px-6 py-4 whitespace-nowrap">
                      <div class="flex items-center">
                        <img 
                          v-if="company.profile_image_url" 
                          :src="company.profile_image_url" 
                          :alt="company.company_name"
                          class="h-10 w-10 rounded-full object-cover mr-3"
                        />
                        <div v-else class="h-10 w-10 rounded-full bg-gray-200 flex items-center justify-center mr-3">
                          <svg class="w-6 h-6 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                          </svg>
                        </div>
                        <div>
                          <div class="text-sm font-medium text-gray-900">{{ company.company_name }}</div>
                          <div v-if="company.website" class="text-xs text-gray-500">{{ company.website }}</div>
                        </div>
                      </div>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{{ company.contact_name }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ company.phone || 'N/A' }}</td>
                    <td class="px-6 py-4 text-sm text-gray-500">
                      <div v-if="company.emails && company.emails.length > 0">
                        <div v-for="(email, idx) in company.emails.slice(0, 2)" :key="idx" class="text-xs">{{ email }}</div>
                        <div v-if="company.emails.length > 2" class="text-xs text-gray-400">+{{ company.emails.length - 2 }} {{ $t('websiteBuilder.more') }}</div>
                      </div>
                      <span v-else class="text-gray-400">N/A</span>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap">
                      <span
                        :class="[
                          'px-2 py-1 text-xs font-semibold rounded-full',
                          company.status === 'active' ? 'bg-green-100 text-green-800' :
                          company.status === 'inactive' ? 'bg-gray-100 text-gray-800' :
                          'bg-red-100 text-red-800'
                        ]"
                      >
                        {{ company.status === 'active' ? $t('websiteBuilder.active') : 
                           company.status === 'inactive' ? $t('websiteBuilder.inactive') : 
                           $t('websiteBuilder.suspended') }}
                      </span>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      <div class="flex items-center gap-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                        <button
                          @click="openZonesManagement(company)"
                          class="text-blue-600 hover:text-blue-900"
                        >
                          {{ $t('websiteBuilder.manageZone') }}
                        </button>
                        <button
                          @click="editDeliveryCompany(company)"
                          class="text-indigo-600 hover:text-indigo-900"
                        >
                          {{ $t('websiteBuilder.edit') }}
                        </button>
                        <button
                          @click="deleteDeliveryCompanyHandler(company.id)"
                          class="text-red-600 hover:text-red-900"
                        >
                          {{ $t('websiteBuilder.delete') }}
                        </button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Delivery Company Form Modal -->
          <div
            v-if="showDeliveryCompanyForm"
            class="fixed inset-0 z-50 overflow-y-auto"
            aria-labelledby="modal-title"
            role="dialog"
            aria-modal="true"
          >
            <div class="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
              <div class="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity" aria-hidden="true" @click="showDeliveryCompanyForm = false"></div>
              <span class="hidden sm:inline-block sm:align-middle sm:h-screen" aria-hidden="true">&#8203;</span>
              <div class="inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-2xl sm:w-full">
                <div class="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                  <h3 class="text-lg font-semibold text-gray-900 mb-4" id="modal-title">
                    {{ editingDeliveryCompany ? $t('websiteBuilder.editDeliveryCompany') : $t('websiteBuilder.addDeliveryCompanyTitle') }}
                  </h3>
                  
                  <form @submit.prevent="saveDeliveryCompany" class="space-y-4">
                    <!-- Company Name -->
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.companyName') }} <span class="text-red-500">*</span></label>
                      <input
                        v-model="deliveryCompanyForm.company_name"
                        type="text"
                        required
                        class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                        :placeholder="$t('websiteBuilder.companyNamePlaceholder')"
                      />
                    </div>

                    <!-- Contact Name -->
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.contactName') }} <span class="text-red-500">*</span></label>
                      <input
                        v-model="deliveryCompanyForm.contact_name"
                        type="text"
                        required
                        class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                        :placeholder="$t('websiteBuilder.contactNamePlaceholder')"
                      />
                    </div>

                    <!-- Phone -->
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.phoneNumber') }}</label>
                      <input
                        v-model="deliveryCompanyForm.phone"
                        type="tel"
                        class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                        :placeholder="$t('websiteBuilder.phoneNumberPlaceholder')"
                      />
                    </div>

                    <!-- Address -->
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.address') }}</label>
                      <textarea
                        v-model="deliveryCompanyForm.address"
                        rows="3"
                        class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                        :placeholder="$t('websiteBuilder.addressPlaceholder')"
                      ></textarea>
                    </div>

                    <!-- Emails -->
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.emails') }}</label>
                      <div class="space-y-2">
                        <div v-for="(email, index) in deliveryCompanyForm.emails" :key="index" class="flex items-center gap-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                          <input
                            v-model="deliveryCompanyForm.emails[index]"
                            type="email"
                            class="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                            :placeholder="$t('websiteBuilder.emailPlaceholder')"
                          />
                          <button
                            type="button"
                            @click="deliveryCompanyForm.emails.splice(index, 1)"
                            class="px-3 py-2 text-red-600 hover:text-red-800"
                          >
                            {{ $t('websiteBuilder.remove') }}
                          </button>
                        </div>
                        <button
                          type="button"
                          @click="deliveryCompanyForm.emails.push('')"
                          class="text-sm text-indigo-600 hover:text-indigo-800"
                        >
                          + {{ $t('websiteBuilder.addEmail') }}
                        </button>
                      </div>
                    </div>

                    <!-- Website -->
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.website') }}</label>
                      <input
                        v-model="deliveryCompanyForm.website"
                        type="url"
                        class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                        :placeholder="$t('websiteBuilder.websitePlaceholder')"
                      />
                    </div>

                    <!-- Status -->
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.status') }}</label>
                      <select
                        v-model="deliveryCompanyForm.status"
                        class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      >
                        <option value="active">{{ $t('websiteBuilder.active') }}</option>
                        <option value="inactive">{{ $t('websiteBuilder.inactive') }}</option>
                        <option value="suspended">{{ $t('websiteBuilder.suspended') }}</option>
                      </select>
                    </div>

                    <!-- Notes -->
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.notes') }}</label>
                      <textarea
                        v-model="deliveryCompanyForm.notes"
                        rows="3"
                        class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                        :placeholder="$t('websiteBuilder.notesPlaceholder')"
                      ></textarea>
                    </div>

                    <!-- Admin login (for delivery company dashboard) -->
                    <div class="border-t border-gray-200 pt-4 mt-4">
                      <p class="text-sm font-medium text-gray-700 mb-2">Dashboard login (admin)</p>
                      <div class="space-y-3">
                        <div>
                          <label class="block text-sm text-gray-600 mb-1">Username</label>
                          <input
                            v-model="deliveryCompanyForm.admin_username"
                            type="text"
                            class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                            placeholder="e.g. company_admin"
                          />
                        </div>
                        <div>
                          <label class="block text-sm text-gray-600 mb-1">Password {{ editingDeliveryCompany ? '(leave blank to keep current)' : '' }}</label>
                          <input
                            v-model="deliveryCompanyForm.admin_password"
                            type="password"
                            class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                            :placeholder="editingDeliveryCompany ? 'Leave blank to keep' : 'Min 6 characters'"
                          />
                        </div>
                      </div>
                    </div>

                    <!-- Profile Image -->
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.profileImage') }}</label>
                      <div v-if="deliveryCompanyForm.profile_image_preview" class="mb-2">
                        <img :src="deliveryCompanyForm.profile_image_preview" alt="Preview" class="h-20 w-20 rounded-full object-cover" />
                      </div>
                      <input
                        type="file"
                        accept="image/*"
                        @change="handleProfileImageChange"
                        class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      />
                      <button
                        v-if="editingDeliveryCompany && editingDeliveryCompany.profile_image_url"
                        type="button"
                        @click="deliveryCompanyForm.remove_profile_image = true; deliveryCompanyForm.profile_image_preview = null"
                        class="mt-2 text-sm text-red-600 hover:text-red-800"
                      >
                        {{ $t('websiteBuilder.removeImage') }}
                      </button>
                    </div>

                    <!-- Error Message -->
                    <div v-if="deliveryCompanyError" class="p-3 bg-red-50 border border-red-200 rounded-lg">
                      <p class="text-sm text-red-600">{{ deliveryCompanyError }}</p>
                    </div>

                    <!-- Form Actions -->
                    <div class="flex gap-3 pt-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                      <button
                        type="submit"
                        :disabled="savingDeliveryCompany"
                        class="flex-1 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                      >
                        {{ savingDeliveryCompany ? $t('websiteBuilder.saving') : (editingDeliveryCompany ? $t('websiteBuilder.update') : $t('websiteBuilder.create')) }}
                      </button>
                      <button
                        type="button"
                        @click="showDeliveryCompanyForm = false; resetDeliveryCompanyForm()"
                        class="flex-1 px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition-colors"
                      >
                        {{ $t('websiteBuilder.cancel') }}
                      </button>
                    </div>
                  </form>
                </div>
              </div>
            </div>

            <!-- Zones Management Card (Inline) -->
            <div v-if="showZonesManagement && selectedDeliveryCompanyForZones" class="mt-8 bg-white rounded-lg shadow-lg p-8">
              <div class="flex items-center justify-between mb-6" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <h3 class="text-xl font-semibold text-gray-900">
                  {{ $t('websiteBuilder.manageZones') }} - {{ selectedDeliveryCompanyForZones.company_name }}
                </h3>
                <button
                  @click="showZonesManagement = false; selectedDeliveryCompanyForZones = null"
                  class="text-gray-400 hover:text-gray-600"
                >
                  <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
              
              <!-- Add Zone Button -->
              <div class="mb-4" :class="$i18n.locale === 'ar' ? 'text-right' : 'text-left'">
                <button
                  @click="showZoneForm = true; editingZone = null; resetZoneForm()"
                  class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors flex items-center gap-2"
                >
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                  </svg>
                  <span>{{ $t('websiteBuilder.addZone') }}</span>
                </button>
              </div>

              <!-- Loading State -->
              <div v-if="loadingZones" class="text-center py-8">
                <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
                <p class="mt-4 text-gray-600">{{ $t('websiteBuilder.loadingZones') }}</p>
              </div>

              <!-- Zones List -->
              <div v-else-if="zones.length > 0" class="space-y-3 mb-4">
                <div
                  v-for="zone in zones"
                  :key="zone.id"
                  class="border border-gray-200 rounded-lg p-4 hover:bg-gray-50"
                >
                  <div class="flex items-start justify-between">
                    <div class="flex items-start gap-4 flex-1">
                      <img 
                        v-if="zone.image_url" 
                        :src="zone.image_url" 
                        :alt="zone.zone_name_en"
                        class="h-16 w-16 rounded-lg object-cover"
                      />
                      <div v-else class="h-16 w-16 rounded-lg bg-gray-200 flex items-center justify-center">
                        <svg class="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
                        </svg>
                      </div>
                      <div class="flex-1">
                        <div class="flex items-center gap-2 mb-1" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                          <h4 class="font-semibold text-gray-900">{{ zone.zone_name_en }}</h4>
                          <span class="text-sm text-gray-600">({{ zone.zone_name_ar }})</span>
                        </div>
                        <div class="flex items-center gap-4 text-sm text-gray-600" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                          <span>{{ $t('websiteBuilder.price') }}: ${{ parseFloat(zone.price || 0).toFixed(2) }}</span>
                          <span
                            :class="[
                              'px-2 py-1 text-xs font-semibold rounded-full',
                              zone.status === 'active' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                            ]"
                          >
                            {{ zone.status === 'active' ? $t('websiteBuilder.active') : $t('websiteBuilder.inactive') }}
                          </span>
                        </div>
                        <p v-if="zone.note" class="text-sm text-gray-500 mt-1">{{ zone.note }}</p>
                      </div>
                    </div>
                    <div class="flex items-center gap-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                      <button
                        @click="editZone(zone)"
                        class="text-indigo-600 hover:text-indigo-900 text-sm"
                      >
                        {{ $t('websiteBuilder.edit') }}
                      </button>
                      <button
                        @click="deleteZoneHandler(zone.id)"
                        class="text-red-600 hover:text-red-900 text-sm"
                      >
                        {{ $t('websiteBuilder.delete') }}
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Empty State -->
              <div v-else class="text-center py-8">
                <div class="text-4xl mb-4">🗺️</div>
                <p class="text-gray-600 font-semibold">{{ $t('websiteBuilder.noZonesFound') }}</p>
                <p class="text-sm text-gray-500 mt-2">{{ $t('websiteBuilder.clickAddZone') }}</p>
              </div>
            </div>
          </div>

          <!-- Zone Form Modal -->
          <div
            v-if="showZoneForm"
            class="fixed inset-0 z-50 overflow-y-auto"
            aria-labelledby="zone-form-title"
            role="dialog"
            aria-modal="true"
          >
            <div class="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
              <div class="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity" aria-hidden="true" @click="showZoneForm = false"></div>
              <span class="hidden sm:inline-block sm:align-middle sm:h-screen" aria-hidden="true">&#8203;</span>
              <div class="inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-2xl sm:w-full">
                <div class="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                  <h3 class="text-lg font-semibold text-gray-900 mb-4" id="zone-form-title">
                    {{ editingZone ? $t('websiteBuilder.editZone') : $t('websiteBuilder.addZoneTitle') }}
                  </h3>
                  
                  <form @submit.prevent="saveZone" class="space-y-4">
                    <!-- Zone Name (English) -->
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.zoneNameEn') }} <span class="text-red-500">*</span></label>
                      <input
                        v-model="zoneForm.zone_name_en"
                        type="text"
                        required
                        class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                        :placeholder="$t('websiteBuilder.zoneNameEnPlaceholder')"
                      />
                    </div>

                    <!-- Zone Name (Arabic) -->
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.zoneNameAr') }} <span class="text-red-500">*</span></label>
                      <input
                        v-model="zoneForm.zone_name_ar"
                        type="text"
                        required
                        class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                        :placeholder="$t('websiteBuilder.zoneNameArPlaceholder')"
                      />
                    </div>

                    <!-- Price -->
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.price') }} <span class="text-red-500">*</span></label>
                      <input
                        v-model="zoneForm.price"
                        type="number"
                        step="0.01"
                        min="0"
                        required
                        class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                        :placeholder="$t('websiteBuilder.pricePlaceholder')"
                      />
                    </div>

                    <!-- Status -->
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.status') }}</label>
                      <select
                        v-model="zoneForm.status"
                        class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      >
                        <option value="active">{{ $t('websiteBuilder.active') }}</option>
                        <option value="inactive">{{ $t('websiteBuilder.inactive') }}</option>
                      </select>
                    </div>

                    <!-- Note -->
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.notes') }}</label>
                      <textarea
                        v-model="zoneForm.note"
                        rows="3"
                        class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                        :placeholder="$t('websiteBuilder.notesPlaceholder')"
                      ></textarea>
                    </div>

                    <!-- Zone Image -->
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.zoneImage') }}</label>
                      <div v-if="zoneForm.image_preview" class="mb-2">
                        <img :src="zoneForm.image_preview" alt="Preview" class="h-32 w-32 rounded-lg object-cover" />
                      </div>
                      <input
                        type="file"
                        accept="image/*"
                        @change="handleZoneImageChange"
                        class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      />
                      <button
                        v-if="editingZone && editingZone.image_url"
                        type="button"
                        @click="zoneForm.remove_image = true; zoneForm.image_preview = null"
                        class="mt-2 text-sm text-red-600 hover:text-red-800"
                      >
                        {{ $t('websiteBuilder.removeImage') }}
                      </button>
                    </div>

                    <!-- Error Message -->
                    <div v-if="zoneError" class="p-3 bg-red-50 border border-red-200 rounded-lg">
                      <p class="text-sm text-red-600">{{ zoneError }}</p>
                    </div>

                    <!-- Form Actions -->
                    <div class="flex gap-3 pt-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                      <button
                        type="submit"
                        :disabled="savingZone"
                        class="flex-1 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                      >
                        {{ savingZone ? $t('websiteBuilder.saving') : (editingZone ? $t('websiteBuilder.update') : $t('websiteBuilder.create')) }}
                      </button>
                      <button
                        type="button"
                        @click="showZoneForm = false; resetZoneForm()"
                        class="flex-1 px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition-colors"
                      >
                        {{ $t('websiteBuilder.cancel') }}
                      </button>
                    </div>
                  </form>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- Show restaurants management when restaurants view is active -->
        <div v-show="activeDashboardView === 'restaurants'" class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- Existing Websites List -->
      <div v-if="websites.length > 0" class="mb-8">
        <div class="flex items-center justify-between mb-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
          <h2 class="text-2xl font-semibold text-gray-800">{{ $t('websiteBuilder.yourWebsites') }}</h2>
          <div class="text-sm text-gray-600">
            {{ filteredWebsites.length }} {{ $t('websiteBuilder.restaurantsCount') }} {{ websites.length }}
          </div>
        </div>
        
        <!-- Search Bar -->
        <div class="mb-6">
          <div class="relative">
            <div :class="['absolute inset-y-0 flex items-center pointer-events-none', $i18n.locale === 'ar' ? 'right-0 pr-3' : 'left-0 pl-3']">
              <svg class="h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            <input
              v-model="searchQuery"
              type="text"
              :placeholder="$t('websiteBuilder.searchPlaceholder')"
              :class="['block w-full py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 text-sm', $i18n.locale === 'ar' ? 'pr-10 pl-3' : 'pl-10 pr-3']"
            />
            <button
              v-if="searchQuery"
              @click="searchQuery = ''"
              :class="['absolute inset-y-0 flex items-center', $i18n.locale === 'ar' ? 'left-0 pl-3' : 'right-0 pr-3']"
            >
              <svg class="h-5 w-5 text-gray-400 hover:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>
        
        <div v-if="filteredWebsites.length === 0" class="text-center py-12 bg-white rounded-lg border-2 border-dashed border-gray-300">
          <div class="text-4xl mb-4">🔍</div>
          <p class="text-gray-600 font-semibold">{{ $t('websiteBuilder.noRestaurantsFound') }}</p>
          <p class="text-sm text-gray-500 mt-2">{{ $t('websiteBuilder.tryAdjustingSearch') }}</p>
          <button
            @click="searchQuery = ''"
            class="mt-4 px-4 py-2 text-sm text-indigo-600 hover:text-indigo-700 font-semibold"
          >
            {{ $t('websiteBuilder.clearSearch') }}
          </button>
        </div>
        
        <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <div
            v-for="website in filteredWebsites"
            :key="website.id"
            class="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition-shadow"
          >
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-xl font-semibold text-gray-800">{{ website.restaurant_name }}</h3>
              <span
                :class="[
                  'px-2 py-1 text-xs rounded-full',
                  website.is_published ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                ]"
              >
                {{ website.is_published ? $t('websiteBuilder.published') : $t('websiteBuilder.draft') }}
              </span>
            </div>
            
            <!-- Completion Progress -->
            <div class="mb-4">
              <div class="flex items-center justify-between mb-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <span class="text-xs font-semibold text-gray-600">{{ $t('websiteBuilder.completion') }}</span>
                <span class="text-xs font-bold" :style="{ color: getCompletionColor(calculateCompletion(website)) }">
                  {{ calculateCompletion(website) }}%
                </span>
              </div>
              <div class="w-full bg-gray-200 rounded-full h-2 overflow-hidden">
                <div
                  class="h-2 rounded-full transition-all duration-500"
                  :style="{
                    width: `${calculateCompletion(website)}%`,
                    backgroundColor: getCompletionColor(calculateCompletion(website))
                  }"
                ></div>
              </div>
            </div>
            
            <div v-if="website.logo_url" class="mb-4">
              <img :src="website.logo_url" :alt="website.restaurant_name" class="h-16 object-contain" />
            </div>
            <p v-if="website.description" class="text-gray-600 text-sm mb-4 line-clamp-2">
              {{ website.description }}
            </p>
            <div class="flex gap-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
              <button
                @click="viewWebsite(website.id)"
                class="flex-1 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
              >
                {{ $t('websiteBuilder.view') }}
              </button>
              <button
                @click="editWebsite(website)"
                class="flex-1 px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition-colors"
              >
                {{ $t('websiteBuilder.edit') }}
              </button>
              <button
                @click="handleDeleteWebsite(website.id)"
                class="px-4 py-2 bg-red-100 text-red-700 rounded-lg hover:bg-red-200 transition-colors"
              >
                {{ $t('websiteBuilder.delete') }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Create/Edit Form -->
      <div id="website-form" class="bg-white rounded-lg shadow-lg p-8">
        <h2 class="text-2xl font-semibold text-gray-800 mb-6">
          {{ editingWebsite ? $t('websiteBuilder.editWebsite') : $t('websiteBuilder.createNewWebsite') }}
        </h2>

        <!-- Tabs -->
        <div class="border-b border-gray-200 mb-6">
          <nav class="flex overflow-x-auto" :class="$i18n.locale === 'ar' ? 'space-x-reverse space-x-8' : 'space-x-8'" aria-label="Tabs">
            <button
              type="button"
              @click="activeTab = 'basic'"
              :class="[
                'py-4 px-1 border-b-2 font-medium text-sm transition-colors whitespace-nowrap',
                activeTab === 'basic'
                  ? 'border-indigo-500 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              ]"
            >
              {{ $t('websiteBuilder.basicInformation') }}
            </button>
            <button
              type="button"
              @click="handleMenuTabClick"
              :class="[
                'py-4 px-1 border-b-2 font-medium text-sm transition-colors whitespace-nowrap',
                activeTab === 'menu'
                  ? 'border-indigo-500 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              ]"
              :disabled="!editingWebsite && !formData.restaurant_name"
            >
              {{ $t('websiteBuilder.menu') }}
              <span v-if="!editingWebsite && !formData.restaurant_name" :class="['text-xs text-gray-400', $i18n.locale === 'ar' ? 'mr-1' : 'ml-1']">{{ $t('websiteBuilder.saveFirst') }}</span>
            </button>
            <button
              type="button"
              @click="activeTab = 'gallery'"
              :class="[
                'py-4 px-1 border-b-2 font-medium text-sm transition-colors whitespace-nowrap',
                activeTab === 'gallery'
                  ? 'border-indigo-500 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              ]"
            >
              {{ $t('websiteBuilder.gallery') }}
            </button>
            <button
              type="button"
              @click="activeTab = 'additional'"
              :class="[
                'py-4 px-1 border-b-2 font-medium text-sm transition-colors whitespace-nowrap',
                activeTab === 'additional'
                  ? 'border-indigo-500 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              ]"
            >
              {{ $t('websiteBuilder.additional') }}
            </button>
            <button
              type="button"
              v-if="editingWebsite || (savedWebsiteId && !editingWebsite)"
              @click="handleAdminTabClick"
              :class="[
                'py-4 px-1 border-b-2 font-medium text-sm transition-colors whitespace-nowrap',
                activeTab === 'admin'
                  ? 'border-indigo-500 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              ]"
            >
              {{ $t('websiteBuilder.adminAccount') }}
            </button>
          </nav>
        </div>

        <form @submit.prevent="saveWebsite" class="space-y-6">
          <!-- Tab 1: Basic Information -->
          <div v-show="activeTab === 'basic'" class="space-y-6">
            <!-- Restaurant Name -->
            <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">
              {{ $t('websiteBuilder.restaurantName') }} <span class="text-red-500">{{ $t('websiteBuilder.required') }}</span>
            </label>
            <input
              v-model="formData.restaurant_name"
              type="text"
              required
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              :placeholder="$t('websiteBuilder.enterRestaurantName')"
            />
          </div>

          <!-- Logo Upload -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">{{ $t('websiteBuilder.logo') }}</label>
            <div class="flex items-center gap-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
              <div v-if="formData.logo_url || logoPreview" class="flex-shrink-0">
                <img
                  :src="logoPreview || formData.logo_url"
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

          <!-- Description -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">{{ $t('websiteBuilder.description') }}</label>
            <textarea
              v-model="formData.description"
              rows="4"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              :placeholder="$t('websiteBuilder.enterRestaurantDescription')"
            ></textarea>
          </div>

          <!-- Contact Information -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">{{ $t('websiteBuilder.address') }}</label>
              <input
                v-model="formData.address"
                type="text"
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                :placeholder="$t('websiteBuilder.addressPlaceholder')"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">{{ $t('websiteBuilder.phone') }}</label>
              <input
                v-model="formData.phone"
                type="tel"
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                :placeholder="$t('websiteBuilder.phonePlaceholder')"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">{{ $t('websiteBuilder.email') }}</label>
              <input
                v-model="formData.email"
                type="email"
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                :placeholder="$t('websiteBuilder.emailPlaceholder')"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">{{ $t('websiteBuilder.websiteUrl') }}</label>
              <input
                v-model="formData.website_url"
                type="url"
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                :placeholder="$t('websiteBuilder.websiteUrlPlaceholder')"
              />
            </div>
          </div>

          <!-- Color Scheme -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">{{ $t('websiteBuilder.primaryColor') }}</label>
              <div class="flex gap-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <input
                  v-model="formData.primary_color"
                  type="color"
                  class="h-10 w-20 border border-gray-300 rounded-lg cursor-pointer"
                />
                <input
                  v-model="formData.primary_color"
                  type="text"
                  class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  placeholder="#4F46E5"
                />
              </div>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">{{ $t('websiteBuilder.secondaryColor') }}</label>
              <div class="flex gap-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                <input
                  v-model="formData.secondary_color"
                  type="color"
                  class="h-10 w-20 border border-gray-300 rounded-lg cursor-pointer"
                />
                <input
                  v-model="formData.secondary_color"
                  type="text"
                  class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  placeholder="#7C3AED"
                />
              </div>
            </div>
          </div>
          </div>

          <!-- Tab 2: Menu -->
          <div v-show="activeTab === 'menu'" class="space-y-6">
            <div v-if="!editingWebsite && !formData.restaurant_name" class="border-2 border-dashed border-gray-300 rounded-lg p-12 text-center">
              <div class="text-4xl mb-4">📋</div>
              <p class="text-gray-500 mb-2">{{ $t('websiteBuilder.saveFirst') }}</p>
              <p class="text-sm text-gray-400 mb-4">{{ $t('websiteBuilder.enterRestaurantName') }} {{ $t('websiteBuilder.saveChanges') }}</p>
              <button
                type="button"
                @click="activeTab = 'basic'"
                class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
              >
                {{ $t('websiteBuilder.basicInformation') }}
              </button>
            </div>
            
            <div v-else>
              <!-- Product Management Section -->
              <div class="mb-6">
                <div class="flex items-center justify-between mb-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <div>
                    <h3 class="text-lg font-semibold text-gray-800">{{ $t('websiteBuilder.products') }}</h3>
                    <p class="text-sm text-gray-500">{{ $t('websiteBuilder.noProductsHint') }}</p>
                  </div>
                  <button
                    type="button"
                    @click="showProductForm = true; editingProduct = null"
                    class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors text-sm font-semibold"
                  >
                    {{ $t('websiteBuilder.addProduct') }}
                  </button>
                </div>

                <!-- Product Form Modal -->
                <div v-if="showProductForm" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                  <div class="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
                    <div class="p-6">
                      <div class="flex items-center justify-between mb-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                        <h3 class="text-xl font-semibold">{{ editingProduct ? $t('websiteBuilder.editProduct') : $t('websiteBuilder.addNewProduct') }}</h3>
                        <button @click="closeProductForm" class="text-gray-400 hover:text-gray-600">×</button>
                      </div>
                      
                      <form @submit.prevent="saveProduct" class="space-y-4">
                        <div>
                          <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.productName') }} <span class="text-red-500">{{ $t('websiteBuilder.required') }}</span></label>
                          <input
                            v-model="productForm.name"
                            type="text"
                            required
                            class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                            placeholder="e.g., Margherita Pizza"
                          />
                        </div>
                        
                        <div>
                          <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.productDescription') }}</label>
                          <textarea
                            v-model="productForm.description"
                            rows="3"
                            class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                            :placeholder="$t('websiteBuilder.productDescription')"
                          ></textarea>
                        </div>
                        
                        <div class="grid grid-cols-2 gap-4">
                          <div>
                            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.sellingPrice') }} <span class="text-red-500">{{ $t('websiteBuilder.required') }}</span></label>
                            <input
                              v-model="productForm.price"
                              type="number"
                              step="0.01"
                              min="0"
                              required
                              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                              placeholder="0.00"
                            />
                            <p class="text-xs text-gray-500 mt-1">{{ $t('websiteBuilder.sellingPriceHint') }}</p>
                          </div>
                          
                          <div>
                            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.originalPrice') }}</label>
                            <input
                              v-model="productForm.original_price"
                              type="number"
                              step="0.01"
                              min="0"
                              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                              placeholder="0.00"
                            />
                            <p class="text-xs text-gray-500 mt-1">{{ $t('websiteBuilder.originalPriceHint') }}</p>
                          </div>
                        </div>
                        
                        <div>
                          <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.category') }}</label>
                          <input
                            v-model="productForm.category"
                            type="text"
                            class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                            :placeholder="$t('websiteBuilder.categoryPlaceholder')"
                          />
                        </div>
                        
                        <div>
                          <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('websiteBuilder.logo') }}</label>
                          <input
                            type="file"
                            accept="image/*"
                            @change="handleProductImageUpload"
                            class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                          />
                          <div v-if="productForm.imagePreview" class="mt-2">
                            <img :src="productForm.imagePreview" alt="Preview" class="h-32 w-32 object-cover rounded-lg" />
                          </div>
                        </div>
                        
                        <div class="flex items-center" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                          <input
                            v-model="productForm.is_available"
                            type="checkbox"
                            id="is_available"
                            class="w-4 h-4 text-indigo-600 border-gray-300 rounded"
                          />
                          <label for="is_available" :class="['text-sm text-gray-700', $i18n.locale === 'ar' ? 'mr-2' : 'ml-2']">{{ $t('websiteBuilder.availableForOrder') }}</label>
                        </div>
                        
                        <div v-if="productError" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
                          {{ productError }}
                        </div>
                        
                        <div class="flex gap-3 pt-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                          <button
                            type="submit"
                            :disabled="savingProduct"
                            class="flex-1 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50"
                          >
                            {{ savingProduct ? $t('websiteBuilder.saving') : (editingProduct ? $t('websiteBuilder.updateProduct') : $t('websiteBuilder.createProduct')) }}
                          </button>
                          <button
                            type="button"
                            @click="closeProductForm"
                            class="px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300"
                          >
                            {{ $t('websiteBuilder.cancel') }}
                          </button>
                        </div>
                      </form>
                    </div>
                  </div>
                </div>

                <!-- Products List -->
                <div v-if="products.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mt-4">
                  <div
                    v-for="product in products"
                    :key="product.id"
                    class="border border-gray-200 rounded-lg p-4 hover:shadow-lg transition-shadow"
                  >
                    <div class="flex items-start justify-between mb-2">
                      <div class="flex-1">
                        <h4 class="font-semibold text-gray-800">{{ product.name }}</h4>
                        <p class="text-sm text-gray-500">{{ product.category || $t('websiteBuilder.uncategorized') }}</p>
                      </div>
                      <span :class="product.is_available ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'" class="px-2 py-1 text-xs rounded">
                        {{ product.is_available ? $t('websiteBuilder.available') : $t('websiteBuilder.unavailable') }}
                      </span>
                    </div>
                    
                    <div v-if="product.image_url" class="mb-2">
                      <img :src="product.image_url" :alt="product.name" class="w-full h-32 object-cover rounded" />
                    </div>
                    
                    <p v-if="product.description" class="text-sm text-gray-600 mb-2 line-clamp-2">{{ product.description }}</p>
                    
                    <div class="flex items-center justify-between" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                      <span class="text-lg font-bold text-indigo-600">${{ parseFloat(product.price).toFixed(2) }}</span>
                      <div class="flex gap-2">
                        <button
                          type="button"
                          @click="editProduct(product)"
                          class="px-3 py-1 text-sm bg-indigo-100 text-indigo-700 rounded hover:bg-indigo-200"
                        >
                          {{ $t('websiteBuilder.edit') }}
                        </button>
                        <button
                          type="button"
                          @click="deleteProduct(product.id)"
                          class="px-3 py-1 text-sm bg-red-100 text-red-700 rounded hover:bg-red-200"
                        >
                          {{ $t('websiteBuilder.delete') }}
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
                
                <div v-else class="text-center py-12 border-2 border-dashed border-gray-300 rounded-lg">
                  <div class="text-4xl mb-4">🍽️</div>
                  <p class="text-gray-500 mb-2">{{ $t('websiteBuilder.noProducts') }}</p>
                  <p class="text-sm text-gray-400">{{ $t('websiteBuilder.noProductsHint') }}</p>
                </div>
              </div>
            </div>
          </div>

          <!-- Tab 3: Gallery -->
          <div v-show="activeTab === 'gallery'" class="space-y-6">
            <!-- Gallery Images Section -->
            <div>
            <div class="flex items-center justify-between mb-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">
                  {{ $t('websiteBuilder.galleryImages') }}
                  <span v-if="galleryImages.length > 0" class="text-indigo-600">
                    ({{ galleryImages.length }}/20)
                  </span>
                </label>
                <p class="text-xs text-gray-500">{{ $t('websiteBuilder.galleryHint') }}</p>
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
                  v-if="editingWebsite && galleryFiles.length > 0"
                  type="button"
                  @click="uploadGalleryNow"
                  class="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors text-sm font-semibold"
                >
                  {{ $t('websiteBuilder.uploadNow') }}
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
                  :title="$t('websiteBuilder.removeImage')"
                >
                  ×
                </button>
                <div class="absolute bottom-0 left-0 right-0 bg-black/50 text-white text-xs p-1 text-center rounded-b-lg opacity-0 group-hover:opacity-100 transition-opacity">
                  {{ $t('websiteBuilder.galleryImages') }} {{ index + 1 }}
                </div>
              </div>
            </div>
            
            <div v-else class="border-2 border-dashed border-gray-300 rounded-lg p-12 text-center">
              <div class="text-4xl mb-4">📷</div>
              <p class="text-gray-500 mb-2">{{ $t('websiteBuilder.noGalleryImages') }}</p>
              <p class="text-sm text-gray-400">{{ $t('websiteBuilder.noGalleryImagesHint') }}</p>
              <p v-if="!editingWebsite" class="text-xs text-gray-400 mt-2">
                {{ $t('websiteBuilder.saving') }}
              </p>
            </div>
            
            <div v-if="galleryFiles.length > 0 && !editingWebsite" class="mt-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
              <p class="text-sm text-blue-700">
                <strong>{{ galleryFiles.length }}</strong> {{ $t('websiteBuilder.galleryImages') }}. {{ $t('websiteBuilder.saving') }}
              </p>
            </div>
            </div>
          </div>

          <!-- Tab 5: Admin Account -->
          <div v-show="activeTab === 'admin'" class="space-y-6">
            <div v-if="!editingWebsite && !savedWebsiteId" class="border-2 border-dashed border-gray-300 rounded-lg p-12 text-center">
              <div class="text-4xl mb-4">👤</div>
              <p class="text-gray-500 mb-2">{{ $t('websiteBuilder.saveFirst') }}</p>
              <p class="text-sm text-gray-400 mb-4">{{ $t('websiteBuilder.saveFirst') }}</p>
              <button
                type="button"
                @click="activeTab = 'basic'"
                class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
              >
                {{ $t('websiteBuilder.basicInformation') }}
              </button>
            </div>
            
            <!-- Loading State -->
            <div v-else-if="loadingAdmin" class="text-center py-8">
              <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
              <p class="mt-2 text-gray-600">{{ $t('websiteBuilder.saving') }}</p>
            </div>
            
            <!-- Existing Admin Info -->
            <div v-else-if="existingAdmin" class="space-y-6">
              <div class="bg-green-50 border border-green-200 rounded-lg p-6">
                <h3 class="text-lg font-semibold text-green-900 mb-2">✓ {{ $t('websiteBuilder.adminAccount') }}</h3>
                <div class="bg-white rounded-lg p-4 space-y-2">
                  <div>
                    <span class="text-sm font-semibold text-gray-700">{{ $t('websiteBuilder.adminName') }}:</span>
                    <span :class="['text-gray-900', $i18n.locale === 'ar' ? 'mr-2' : 'ml-2']">{{ existingAdmin.name }}</span>
                  </div>
                  <div>
                    <span class="text-sm font-semibold text-gray-700">{{ $t('websiteBuilder.adminEmail') }}:</span>
                    <span :class="['text-gray-900', $i18n.locale === 'ar' ? 'mr-2' : 'ml-2']">{{ existingAdmin.email }}</span>
                  </div>
                </div>
                <div class="mt-4">
                  <a
                    :href="($router.resolve('/restaurant/dashboard').href)"
                    target="_blank"
                    class="inline-block px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors text-sm font-semibold"
                  >
                    {{ $t('websiteBuilder.goToRestaurantDashboard') }}
                  </a>
                </div>
              </div>
            </div>
            
            <!-- Create New Admin Form -->
            <div v-else class="space-y-6">
              <div class="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <h3 class="text-lg font-semibold text-blue-900 mb-2">{{ $t('websiteBuilder.createAdminAccount') }}</h3>
              </div>

              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">
                  {{ $t('websiteBuilder.adminName') }} <span class="text-red-500">{{ $t('websiteBuilder.required') }}</span>
                </label>
                <input
                  v-model="adminForm.name"
                  type="text"
                  required
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  :placeholder="$t('websiteBuilder.adminName')"
                />
              </div>

              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">
                  {{ $t('websiteBuilder.adminEmail') }} <span class="text-red-500">{{ $t('websiteBuilder.required') }}</span>
                </label>
                <input
                  v-model="adminForm.email"
                  type="email"
                  required
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  :placeholder="$t('websiteBuilder.emailPlaceholder')"
                />
              </div>

              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">
                  {{ $t('websiteBuilder.adminPassword') }} <span class="text-red-500">{{ $t('websiteBuilder.required') }}</span>
                </label>
                <input
                  v-model="adminForm.password"
                  type="password"
                  required
                  minlength="6"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  :placeholder="$t('websiteBuilder.adminPassword')"
                />
              </div>

              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">
                  {{ $t('websiteBuilder.confirmPassword') }} <span class="text-red-500">{{ $t('websiteBuilder.required') }}</span>
                </label>
                <input
                  v-model="adminForm.confirmPassword"
                  type="password"
                  required
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  :placeholder="$t('websiteBuilder.confirmPassword')"
                />
              </div>

              <div v-if="adminError" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
                {{ adminError }}
              </div>

              <div v-if="adminSuccess" class="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg text-sm">
                {{ adminSuccess }}
              </div>

              <button
                type="button"
                @click="createAdminAccount"
                :disabled="creatingAdmin || !adminForm.name || !adminForm.email || !adminForm.password"
                class="w-full px-6 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <span v-if="creatingAdmin">{{ $t('websiteBuilder.creating') }}</span>
                <span v-else>{{ $t('websiteBuilder.createAdminAccount') }}</span>
              </button>

              <div v-if="adminAccountCreated" class="mt-4 p-4 bg-green-50 border border-green-200 rounded-lg">
                <p class="text-sm text-green-700 font-semibold mb-2">✓ {{ $t('websiteBuilder.adminAccountCreated') }}</p>
                <a
                  :href="($router.resolve('/restaurant/dashboard').href)"
                  target="_blank"
                  class="inline-block px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors text-sm font-semibold"
                >
                  {{ $t('websiteBuilder.goToRestaurantDashboard') }}
                </a>
              </div>
            </div>
          </div>

          <!-- Tab 4: Additional -->
          <div v-show="activeTab === 'additional'" class="space-y-6">
            <!-- App Download URL -->
            <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">{{ $t('websiteBuilder.appDownloadUrl') }}</label>
            <input
              v-model="formData.app_download_url"
              type="url"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              :placeholder="$t('websiteBuilder.appDownloadUrlPlaceholder')"
            />
          </div>

          <!-- Locations (JSON format) -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">{{ $t('websiteBuilder.locations') }}</label>
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
                v-model="formData.newsletter_enabled"
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
                v-model="formData.is_published"
                type="checkbox"
                class="w-5 h-5 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
              />
              <span :class="['text-sm font-medium text-gray-700', $i18n.locale === 'ar' ? 'mr-2' : 'ml-2']">{{ $t('websiteBuilder.publishWebsite') }}</span>
            </label>
          </div>

          <!-- Domain Configuration Section -->
          <div v-if="editingWebsite || savedWebsiteId" class="border-t border-gray-200 pt-6 mt-6">
            <h3 class="text-lg font-semibold text-gray-800 mb-4">{{ $t('websiteBuilder.domainConfiguration') }}</h3>
            <div class="bg-gradient-to-br from-blue-50 to-indigo-50 rounded-xl p-6 border border-blue-200 space-y-6">
              
              <!-- Subdomain -->
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">
                  {{ $t('websiteBuilder.subdomain') }}
                </label>
                <div class="flex items-center gap-2" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
                  <input
                    v-model="formData.subdomain"
                    type="text"
                    pattern="[a-z0-9]([a-z0-9-]*[a-z0-9])?"
                    class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                    :placeholder="$t('websiteBuilder.subdomainPlaceholder')"
                  />
                  <span class="text-gray-600 font-medium">.{{ getBaseDomain() }}</span>
                </div>
              </div>

              <!-- Custom Domain -->
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">
                  {{ $t('websiteBuilder.customDomain') }}
                </label>
                <input
                  v-model="formData.custom_domain"
                  type="text"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  :placeholder="$t('websiteBuilder.customDomainPlaceholder')"
                />
                <p class="text-xs text-gray-500 mt-1">
                  Enter your custom domain (e.g., restaurant.com, www.restaurant.com)
                </p>
                <div v-if="formData.custom_domain" class="mt-3 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
                  <p class="text-xs text-yellow-800 font-semibold mb-2">📋 DNS Configuration Required:</p>
                  <ol class="text-xs text-yellow-700 space-y-1 list-decimal list-inside">
                    <li>Add a CNAME record: <span class="font-mono bg-yellow-100 px-1 rounded">{{ formData.custom_domain }}</span> → <span class="font-mono bg-yellow-100 px-1 rounded">{{ getBaseDomain() }}</span></li>
                    <li>Or add an A record pointing to your server IP</li>
                    <li>Wait for DNS propagation (can take up to 48 hours)</li>
                    <li>SSL certificate will be automatically provisioned</li>
                  </ol>
                </div>
                <div v-if="editingWebsite?.domain_verified" class="mt-2 flex items-center gap-2 text-green-600">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <span class="text-xs font-semibold">Domain verified and active</span>
                </div>
              </div>

            </div>
          </div>

          <!-- Barcode/QR Code Section -->
          <div v-if="editingWebsite?.barcode_code || (savedWebsiteId && currentWebsite?.barcode_code)" class="border-t border-gray-200 pt-6 mt-6">
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
                        :value="editingWebsite?.barcode_code || currentWebsite?.barcode_code || 'Not generated'"
                        readonly
                        class="px-4 py-2 border border-gray-300 rounded-lg bg-gray-50 font-mono text-lg font-bold text-gray-900"
                      />
                      <button
                        type="button"
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
                        class="flex-1 px-4 py-2 border border-gray-300 rounded-lg bg-gray-50 text-sm text-gray-600"
                      />
                      <button
                        type="button"
                        @click="copyBarcodeUrl"
                        class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors text-sm font-semibold"
                      >
                        {{ $t('websiteBuilder.copyUrl') }}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          </div>

          <!-- Action Buttons (shown on all tabs) -->
          <div class="flex gap-4 pt-4" :class="$i18n.locale === 'ar' ? 'flex-row-reverse' : ''">
            <button
              type="submit"
              :disabled="saving"
              class="px-6 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors font-medium"
            >
              {{ saving ? $t('websiteBuilder.saving') : (editingWebsite ? $t('websiteBuilder.saveChanges') : $t('websiteBuilder.saveChanges')) }}
            </button>
            <button
              v-if="editingWebsite"
              type="button"
              @click="resetForm"
              class="px-6 py-3 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition-colors font-medium"
            >
              {{ $t('websiteBuilder.cancel') }}
            </button>
            <button
              v-if="editingWebsite"
              type="button"
              @click="viewWebsite(editingWebsite.id)"
              class="px-6 py-3 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors font-medium"
            >
              {{ $t('websiteBuilder.view') }}
            </button>
          </div>
        </form>
        </div>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import QrcodeVue from 'qrcode.vue';
import LanguageSwitcher from './LanguageSwitcher.vue';
import { getWebsites, createWebsite, updateWebsite, deleteWebsite as deleteWebsiteAPI, uploadLogo, uploadGalleryImages, deleteGalleryImage, uploadMenuImage, getProducts, createProduct, updateProduct, deleteProduct as deleteProductAPI, uploadProductImage, registerAdmin, getAdminByWebsiteId, generateBarcodeCode, getTodayStatistics, getDeliveryCompanies, createDeliveryCompany, updateDeliveryCompany, deleteDeliveryCompany, getDeliveryZones, createDeliveryZone, updateDeliveryZone, deleteDeliveryZone } from '../services/api.js';

const router = useRouter();
const { t } = useI18n();

const websites = ref([]);
const editingWebsite = ref(null);
const saving = ref(false);
const activeTab = ref('basic');
const currentWebsite = ref(null);
const websiteProductsCount = ref({}); // Store product counts for each website
const websiteAdmins = ref({}); // Store admin info for each website
const searchQuery = ref(''); // Search query for filtering restaurants
const logoPreview = ref(null);
const logoFile = ref(null);
const galleryFiles = ref([]);
const savedWebsiteId = ref(null);
const adminForm = ref({
  name: '',
  email: '',
  password: '',
  confirmPassword: ''
});
const creatingAdmin = ref(false);
const adminError = ref('');
const adminSuccess = ref('');
const adminAccountCreated = ref(false);
const existingAdmin = ref(null);
const loadingAdmin = ref(false);
const galleryImages = ref([]);
const locationsText = ref('');
const menuImageFile = ref(null);
const menuImagePreview = ref(null);
const processingMenu = ref(false);
const extractedMenuItems = ref([]);
const showMenuPrompt = ref(false);

// Dashboard drawer

// Driver management
const drivers = ref([]);
const loadingDrivers = ref(false);
const processingDriver = ref(false);
const driverFilter = ref('all');

// Delivery Companies management
const deliveryCompanies = ref([]);
const loadingDeliveryCompanies = ref(false);
const showDeliveryCompanyForm = ref(false);
const editingDeliveryCompany = ref(null);
const savingDeliveryCompany = ref(false);
const deliveryCompanyError = ref('');

// Zones management
const showZonesManagement = ref(false);
const selectedDeliveryCompanyForZones = ref(null);
const zones = ref([]);
const loadingZones = ref(false);
const showZoneForm = ref(false);
const editingZone = ref(null);
const savingZone = ref(false);
const zoneError = ref('');
const zoneForm = ref({
  zone_name_ar: '',
  zone_name_en: '',
  price: '0.00',
  status: 'active',
  note: '',
  image: null,
  image_preview: null,
  remove_image: false
});

const deliveryCompanyForm = ref({
  company_name: '',
  contact_name: '',
  phone: '',
  address: '',
  emails: [''],
  website: '',
  status: 'active',
  notes: '',
  profile_image: null,
  profile_image_preview: null,
  remove_profile_image: false
});

// Orders management
const orders = ref([]);
const loadingOrders = ref(false);
const selectedOrderFilter = ref(null);

// Today's statistics
const todayStats = ref({
  ordersCount: 0,
  revenue: '0.00',
  deliveryFees: '0.00',
  totalOwedToRestaurants: '0.00',
  totalPaidToRestaurantsAndDrivers: '0.00'
});
const loadingStats = ref(false);

// Manage Restaurants section - selected card and orders
const selectedRestaurantCard = ref(null);
const todayOrders = ref([]);
const loadingTodayOrders = ref(false);
const restaurantsOwed = ref([]); // For Total Owed card - restaurants with their total owed amounts

// Group orders by driver for delivery fees view
const ordersByDriver = computed(() => {
  if (selectedRestaurantCard.value !== 'delivery-fees') {
    return [];
  }
  
  const grouped = {};
  todayOrders.value.forEach(order => {
    const driverId = order.driver_id || 'unassigned';
    const driverKey = driverId === 'unassigned' ? 'unassigned' : driverId;
    
    if (!grouped[driverKey]) {
      grouped[driverKey] = {
        driver_id: order.driver_id,
        driver_name: order.driver_name || 'Unassigned',
        driver_phone: order.driver_phone || 'N/A',
        driver_email: order.driver_email || 'N/A',
        orders: [],
        totalDeliveryFees: 0
      };
    }
    
    grouped[driverKey].orders.push(order);
    grouped[driverKey].totalDeliveryFees += parseFloat(order.delivery_fees || 0);
  });
  
  return Object.values(grouped);
});
const driverFilters = computed(() => [
  { value: 'all', label: t('websiteBuilder.allDrivers'), count: drivers.value.length },
  { value: 'pending', label: t('websiteBuilder.pendingDrivers'), count: drivers.value.filter(d => d.status === 'pending').length },
  { value: 'approved', label: t('websiteBuilder.approvedDrivers'), count: drivers.value.filter(d => d.status === 'approved').length },
  { value: 'rejected', label: t('websiteBuilder.rejectedDrivers'), count: drivers.value.filter(d => d.status === 'rejected').length },
  { value: 'online', label: t('websiteBuilder.onlineDrivers'), count: onlineDriversCount.value }
]);

// Product management
const products = ref([]);
const showProductForm = ref(false);
const editingProduct = ref(null);
const savingProduct = ref(false);
const productError = ref('');
const productForm = ref({
  name: '',
  description: '',
  price: '',
  original_price: '',
  category: '',
  is_available: true,
  imageFile: null,
  imagePreview: null
});

const formData = ref({
  restaurant_name: '',
  logo_url: '',
  description: '',
  address: '',
  phone: '',
  email: '',
  website_url: '',
  primary_color: '#4F46E5',
  secondary_color: '#7C3AED',
  font_family: 'Inter, sans-serif',
  app_download_url: '',
  newsletter_enabled: false,
  is_published: false,
});


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

const handleGalleryUpload = async (event) => {
  const files = Array.from(event.target.files);
  if (files.length === 0) return;
  
  // Limit to 20 images total
  const remainingSlots = 20 - galleryImages.value.length;
  if (remainingSlots <= 0) {
    alert('Maximum 20 gallery images allowed. Please remove some images first.');
    event.target.value = ''; // Reset input
    return;
  }
  
  const filesToAdd = files.slice(0, remainingSlots);
  if (files.length > remainingSlots) {
    alert(`Only ${remainingSlots} more images can be added. Maximum 20 images allowed.`);
  }
  
  // Add files to galleryFiles array
  galleryFiles.value = [...galleryFiles.value, ...filesToAdd];
  
  // Create previews
  filesToAdd.forEach(file => {
    const reader = new FileReader();
    reader.onload = (e) => {
      galleryImages.value.push({ 
        url: e.target.result, 
        file: file,
        isNew: true 
      });
    };
    reader.readAsDataURL(file);
  });
  
  // Reset input to allow selecting same files again
  event.target.value = '';
};

const handleMenuImageUpload = (event) => {
  const file = event.target.files[0];
  if (file) {
    menuImageFile.value = file;
    const reader = new FileReader();
    reader.onload = (e) => {
      menuImagePreview.value = e.target.result;
    };
    reader.readAsDataURL(file);
    extractedMenuItems.value = []; // Reset extracted items
  }
};

const processMenuImage = async () => {
  if (!menuImageFile.value || !editingWebsite.value) {
    return;
  }

  processingMenu.value = true;
  extractedMenuItems.value = [];

  try {
    const response = await uploadMenuImage(editingWebsite.value.id, menuImageFile.value);
    
    // Check if OpenAI is available from response
    hasOpenAI.value = response.hasOpenAI !== false;
    
    if (response.menuItems && response.menuItems.length > 0) {
      extractedMenuItems.value = response.menuItems;
      alert(`Successfully extracted ${response.menuItems.length} menu items from the image!`);
    } else if (response.itemsExtracted === 0 && hasOpenAI.value) {
      alert('Menu image uploaded, but no items could be extracted. You can manually add menu items.');
    } else {
      // Menu image uploaded without AI extraction
      alert('Menu image uploaded successfully! You can now add menu items manually.');
    }
    
    // Reload websites to get updated data
    await loadWebsites();
    
    // Update editing website with new data
    const updatedWebsite = websites.value.find(w => w.id === editingWebsite.value.id);
    if (updatedWebsite) {
      editingWebsite.value = updatedWebsite;
      menuImagePreview.value = updatedWebsite.menu_image_url || menuImagePreview.value;
    }
    
  } catch (error) {
    console.error('Error processing menu image:', error);
    // Check if error is about OpenAI
    if (error.message.includes('OPENAI') || error.message.includes('openai')) {
      hasOpenAI.value = false;
      alert('Menu image uploaded! OpenAI is not configured, so items were not extracted. You can add menu items manually.');
    } else {
      alert('Failed to process menu image: ' + error.message);
    }
  } finally {
    processingMenu.value = false;
  }
};

const uploadMenuImageOnly = async () => {
  if (!menuImageFile.value || !editingWebsite.value) {
    return;
  }

  processingMenu.value = true;

  try {
    const response = await uploadMenuImage(editingWebsite.value.id, menuImageFile.value);
    
    // Reload websites to get updated data
    await loadWebsites();
    
    // Update editing website with new data
    const updatedWebsite = websites.value.find(w => w.id === editingWebsite.value.id);
    if (updatedWebsite) {
      editingWebsite.value = updatedWebsite;
      menuImagePreview.value = updatedWebsite.menu_image_url || menuImagePreview.value;
    }
    
    alert('Menu image uploaded successfully! You can now add menu items manually.');
    menuImageFile.value = null; // Clear file input
    
  } catch (error) {
    console.error('Error uploading menu image:', error);
    alert('Failed to upload menu image: ' + error.message);
  } finally {
    processingMenu.value = false;
  }
};

const applyExtractedMenuItems = async () => {
  if (extractedMenuItems.value.length > 0 && editingWebsite.value) {
    try {
      await updateWebsite(editingWebsite.value.id, {
        menu_items: extractedMenuItems.value
      });
      alert('Menu items applied successfully!');
      extractedMenuItems.value = [];
      await loadWebsites();
      
      // Refresh the editing website data
      const updatedWebsite = websites.value.find(w => w.id === editingWebsite.value.id);
      if (updatedWebsite) {
        editingWebsite.value = updatedWebsite;
      }
    } catch (error) {
      alert('Failed to apply menu items: ' + error.message);
    }
  }
};

const uploadGalleryNow = async () => {
  if (!editingWebsite.value || galleryFiles.value.length === 0) {
    return;
  }
  
  try {
    saving.value = true;
    await uploadGalleryImages(editingWebsite.value.id, galleryFiles.value);
    galleryFiles.value = [];
    
    // Reload websites to get updated gallery images
    await loadWebsites();
    
    // Update gallery images display
    const updatedWebsite = websites.value.find(w => w.id === editingWebsite.value.id);
    if (updatedWebsite) {
      try {
        const images = updatedWebsite.gallery_images 
          ? (typeof updatedWebsite.gallery_images === 'string' 
              ? JSON.parse(updatedWebsite.gallery_images) 
              : updatedWebsite.gallery_images)
          : [];
        galleryImages.value = images.map(img => ({
          ...img,
          isNew: false
        }));
      } catch (e) {
        galleryImages.value = [];
      }
    }
    
    alert('Gallery images uploaded successfully!');
  } catch (error) {
    console.error('Failed to upload gallery images:', error);
    alert('Failed to upload gallery images: ' + error.message);
  } finally {
    saving.value = false;
  }
};

const removeGalleryImage = async (index) => {
  if (!confirm(t('websiteBuilder.removeImageConfirm'))) {
    return;
  }
  
  const image = galleryImages.value[index];
  
  // If it's a new image (not yet uploaded), just remove from arrays
  if (image.isNew && image.file) {
    galleryFiles.value = galleryFiles.value.filter(f => f !== image.file);
    galleryImages.value.splice(index, 1);
    return;
  }
  
  // If it's an existing image on the server, delete it
  if (editingWebsite.value && image.url && !image.isNew) {
    try {
      // Find the actual index in the server's gallery_images array
      const websitesList = await getWebsites({ all: true });
      const currentWebsite = websitesList.find(w => w.id === editingWebsite.value.id);
      if (currentWebsite) {
        let serverImages = [];
        try {
          serverImages = currentWebsite.gallery_images 
            ? (typeof currentWebsite.gallery_images === 'string' 
                ? JSON.parse(currentWebsite.gallery_images) 
                : currentWebsite.gallery_images)
            : [];
        } catch (e) {
          serverImages = [];
        }
        
        // Find the matching image in server array
        const serverIndex = serverImages.findIndex(img => img.url === image.url);
        if (serverIndex !== -1) {
          await deleteGalleryImage(editingWebsite.value.id, serverIndex);
        }
      }
      
      // Reload to get updated website data
      await loadWebsites();
      
      // Update local gallery images
      if (editingWebsite.value) {
        await loadWebsites();
        const updatedWebsite = websites.value.find(w => w.id === editingWebsite.value.id);
        if (updatedWebsite) {
          try {
            const images = updatedWebsite.gallery_images 
              ? (typeof updatedWebsite.gallery_images === 'string' 
                  ? JSON.parse(updatedWebsite.gallery_images) 
                  : updatedWebsite.gallery_images)
              : [];
            galleryImages.value = images.map(img => ({
              ...img,
              isNew: false
            }));
          } catch (e) {
            galleryImages.value = [];
          }
        }
      }
    } catch (error) {
      console.error('Failed to delete image:', error);
      alert('Failed to delete image: ' + error.message);
    }
  } else {
    // Just remove from local array
    galleryImages.value.splice(index, 1);
  }
};

const saveWebsite = async () => {
  if (!formData.value.restaurant_name) {
    alert('Restaurant name is required');
    return;
  }

  saving.value = true;
  try {
    // Parse locations JSON
    let locations = null;
    if (locationsText.value.trim()) {
      try {
        locations = JSON.parse(locationsText.value);
      } catch (e) {
        alert('Invalid JSON format for locations. Please check your input.');
        saving.value = false;
        return;
      }
    }

    // Prepare website data (exclude gallery_images - it's handled separately)
    const { gallery_images, ...formDataWithoutGallery } = formData.value;
    const websiteData = {
      ...formDataWithoutGallery,
      locations: locations,
    };

    let website;
    if (editingWebsite.value) {
      website = await updateWebsite(editingWebsite.value.id, websiteData);
    } else {
      website = await createWebsite(websiteData);
    }

    // Upload logo if selected
    if (logoFile.value && website.id) {
      await uploadLogo(website.id, logoFile.value);
    }

    // Upload gallery images if selected
    if (galleryFiles.value.length > 0 && website.id) {
      try {
        await uploadGalleryImages(website.id, galleryFiles.value);
        galleryFiles.value = [];
        // Reload websites to get updated gallery images
        await loadWebsites();
        // Update gallery images display
        if (editingWebsite.value) {
          const updatedWebsite = websites.value.find(w => w.id === website.id);
          if (updatedWebsite) {
            try {
              galleryImages.value = updatedWebsite.gallery_images 
                ? (typeof updatedWebsite.gallery_images === 'string' 
                    ? JSON.parse(updatedWebsite.gallery_images) 
                    : updatedWebsite.gallery_images)
                : [];
            } catch (e) {
              galleryImages.value = [];
            }
          }
        }
      } catch (error) {
        console.error('Failed to upload gallery images:', error);
        alert('Failed to upload some gallery images: ' + error.message);
      }
    }

    await loadWebsites();
    
    // After creating a new website, ask if user wants to work on menu
    const wasNewWebsite = !editingWebsite.value;
    
    if (wasNewWebsite) {
      // This was a new website creation
      const createdWebsite = websites.value.find(w => w.id === website.id);
        if (createdWebsite) {
          editingWebsite.value = createdWebsite;
          currentWebsite.value = createdWebsite;
          savedWebsiteId.value = createdWebsite.id;
          
          // Load website data into form
        formData.value = {
          restaurant_name: createdWebsite.restaurant_name || '',
          logo_url: createdWebsite.logo_url || '',
          description: createdWebsite.description || '',
          address: createdWebsite.address || '',
          phone: createdWebsite.phone || '',
          email: createdWebsite.email || '',
          website_url: createdWebsite.website_url || '',
          primary_color: createdWebsite.primary_color || '#4F46E5',
          secondary_color: createdWebsite.secondary_color || '#7C3AED',
          font_family: createdWebsite.font_family || 'Inter, sans-serif',
          app_download_url: createdWebsite.app_download_url || '',
          newsletter_enabled: createdWebsite.newsletter_enabled || false,
          is_published: createdWebsite.is_published || false,
          subdomain: createdWebsite.subdomain || '',
          custom_domain: createdWebsite.custom_domain || '',
        };
        
        // Load menu image
        menuImagePreview.value = createdWebsite.menu_image_url || null;
        
        // Check if menu items exist
        let hasMenuItems = false;
        try {
          const menuItems = createdWebsite.menu_items 
            ? (typeof createdWebsite.menu_items === 'string' 
                ? JSON.parse(createdWebsite.menu_items) 
                : createdWebsite.menu_items)
            : [];
          hasMenuItems = menuItems.length > 0;
        } catch (e) {
          hasMenuItems = false;
        }
        
        if (!hasMenuItems) {
          // Ask user if they want to work on menu
          if (confirm(t('websiteBuilder.websiteCreatedSuccess') + ' ' + t('websiteBuilder.workOnMenuNow'))) {
            activeTab.value = 'menu';
          }
        } else {
          alert(t('websiteBuilder.websiteCreatedSuccess'));
        }
      } else {
        alert(t('websiteBuilder.websiteCreatedSuccess'));
        resetForm();
      }
    } else {
      // Website was updated
      alert(t('websiteBuilder.websiteUpdatedSuccess'));
      // Reload editing website data
      const updatedWebsite = websites.value.find(w => w.id === website.id);
      if (updatedWebsite) {
        editingWebsite.value = updatedWebsite;
        currentWebsite.value = updatedWebsite;
        // Reload menu image
        menuImagePreview.value = updatedWebsite.menu_image_url || null;
      }
    }
  } catch (error) {
    console.error('Save website error:', error);
    const errorMessage = error.response?.data?.error || error.response?.data?.message || error.message || 'Failed to save website';
    console.error('Error details:', {
      message: errorMessage,
      response: error.response?.data,
      status: error.response?.status
    });
    alert('Failed to save website: ' + errorMessage);
  } finally {
    saving.value = false;
  }
};

const editWebsite = async (website) => {
  editingWebsite.value = website;
  currentWebsite.value = website;
  activeTab.value = 'basic'; // Reset to basic tab when editing
  formData.value = {
    restaurant_name: website.restaurant_name || '',
    logo_url: website.logo_url || '',
    description: website.description || '',
    address: website.address || '',
    phone: website.phone || '',
    email: website.email || '',
    website_url: website.website_url || '',
    primary_color: website.primary_color || '#4F46E5',
    secondary_color: website.secondary_color || '#7C3AED',
    font_family: website.font_family || 'Inter, sans-serif',
    app_download_url: website.app_download_url || '',
    newsletter_enabled: website.newsletter_enabled || false,
    is_published: website.is_published || false,
    subdomain: website.subdomain || '',
    custom_domain: website.custom_domain || '',
  };
  
  // Generate barcode code if it doesn't exist
  if (!website.barcode_code && website.id) {
    try {
      const updatedWebsite = await generateBarcodeCode(website.id);
      editingWebsite.value = updatedWebsite;
      currentWebsite.value = updatedWebsite;
      // Reload websites list to get updated data (this will also reload product counts and admin info)
      await loadWebsites();
      // After reloading websites, update editingWebsite with the latest data
      const refreshedWebsite = websites.value.find(w => w.id === website.id);
      if (refreshedWebsite) {
        editingWebsite.value = refreshedWebsite;
        currentWebsite.value = refreshedWebsite;
      }
    } catch (error) {
      console.error('Failed to generate barcode code:', error);
      // Continue with editing even if barcode generation fails
    }
  }
  
  // Load gallery images
  try {
    const images = (editingWebsite.value || website).gallery_images 
      ? (typeof (editingWebsite.value || website).gallery_images === 'string' 
          ? JSON.parse((editingWebsite.value || website).gallery_images) 
          : (editingWebsite.value || website).gallery_images)
      : [];
    // Mark existing images as not new
    galleryImages.value = images.map(img => ({
      ...img,
      isNew: false
    }));
  } catch (e) {
    galleryImages.value = [];
  }
  
  // Load menu image
  menuImagePreview.value = (editingWebsite.value || website).menu_image_url || null;
  menuImageFile.value = null;
  extractedMenuItems.value = [];
  
  // Reset to basic tab when editing
  activeTab.value = 'basic';
  savedWebsiteId.value = (editingWebsite.value || website).id;
  
  // Load locations
  try {
    locationsText.value = (editingWebsite.value || website).locations 
      ? JSON.stringify(typeof (editingWebsite.value || website).locations === 'string' 
          ? JSON.parse((editingWebsite.value || website).locations) 
          : (editingWebsite.value || website).locations, null, 2)
      : '';
  } catch (e) {
    locationsText.value = '';
  }
  
  // Load products and admin info - use the website ID directly
  const websiteId = editingWebsite.value?.id || website.id;
  if (websiteId) {
    // Load products and admin info in parallel
    await Promise.all([
      loadProducts(websiteId),
      loadAdminInfo(websiteId)
    ]);
    console.log('Products loaded:', products.value.length);
    console.log('Admin info loaded:', existingAdmin.value ? 'Yes' : 'No');
  }
  
  logoPreview.value = null;
  logoFile.value = null;
  galleryFiles.value = [];
  
  // Wait for Vue to update the DOM and all async operations
  await nextTick();
  await new Promise(resolve => setTimeout(resolve, 200));
  
  // Scroll to form section
  const scrollToForm = () => {
    const formElement = document.getElementById('website-form');
    if (formElement) {
      const headerOffset = 100;
      const elementPosition = formElement.getBoundingClientRect().top + window.pageYOffset;
      const offsetPosition = elementPosition - headerOffset;
      
      window.scrollTo({
        top: Math.max(0, offsetPosition),
        behavior: 'smooth'
      });
      return true;
    }
    return false;
  };
  
  // Try scrolling immediately
  if (!scrollToForm()) {
    // If element not found, try again after a short delay
    setTimeout(() => {
      if (!scrollToForm()) {
        // Final fallback: scroll to approximate position
        const approximatePosition = 250;
        window.scrollTo({
          top: approximatePosition,
          behavior: 'smooth'
        });
      }
    }, 200);
  }
};

const loadAdminInfo = async (websiteId) => {
  if (!websiteId) return;
  
  try {
    loadingAdmin.value = true;
    existingAdmin.value = await getAdminByWebsiteId(websiteId);
  } catch (error) {
    console.error('Failed to load admin info:', error);
    existingAdmin.value = null;
  } finally {
    loadingAdmin.value = false;
  }
};

const handleMenuTabClick = async () => {
  activeTab.value = 'menu';
  // Always load products when tab is clicked to ensure it's up to date
  const websiteId = editingWebsite.value?.id || savedWebsiteId.value;
  if (websiteId) {
    await loadProducts(websiteId);
  }
};

const handleAdminTabClick = async () => {
  activeTab.value = 'admin';
  // Always load admin info when tab is clicked to ensure it's up to date
  const websiteId = editingWebsite.value?.id || savedWebsiteId.value;
  if (websiteId) {
    await loadAdminInfo(websiteId);
  }
};

const loadProducts = async (websiteId = null) => {
  const id = websiteId || editingWebsite.value?.id;
  if (id) {
    try {
      console.log('Loading products for website ID:', id);
      products.value = await getProducts(id);
      console.log('Products loaded:', products.value.length);
    } catch (error) {
      console.error('Failed to load products:', error);
      products.value = [];
    }
  } else {
    products.value = [];
  }
};

const closeProductForm = () => {
  showProductForm.value = false;
  editingProduct.value = null;
  productForm.value = {
    name: '',
    description: '',
    price: '',
    original_price: '',
    category: '',
    is_available: true,
    imageFile: null,
    imagePreview: null
  };
};

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
    name: product.name,
    description: product.description || '',
    price: product.price,
    original_price: product.original_price || '',
    category: product.category || '',
    is_available: product.is_available,
    imageFile: null,
    imagePreview: product.image_url || null
  };
  showProductForm.value = true;
};

const saveProduct = async () => {
  if (!editingWebsite.value || !editingWebsite.value.id) {
    const errorMsg = t('websiteBuilder.pleaseSelectWebsite') || 'Please select a website first';
    productError.value = errorMsg;
    alert(errorMsg);
    return;
  }
  
  savingProduct.value = true;
  productError.value = '';
  
  try {
    // Validate required fields
    if (!productForm.value.name || !productForm.value.name.trim()) {
      productError.value = t('websiteBuilder.productNameRequired') || 'Product name is required';
      savingProduct.value = false;
      return;
    }
    
    // Validate and parse price
    const priceValue = parseFloat(productForm.value.price);
    if (!productForm.value.price || isNaN(priceValue) || priceValue <= 0) {
      productError.value = t('websiteBuilder.productPriceRequired') || 'Valid product price is required';
      savingProduct.value = false;
      return;
    }
    
    // Ensure website_id is a number
    const websiteId = parseInt(editingWebsite.value.id);
    if (isNaN(websiteId)) {
      productError.value = 'Invalid website ID';
      savingProduct.value = false;
      return;
    }
    
    // Validate and parse original_price if provided
    let originalPriceValue = null;
    if (productForm.value.original_price !== undefined && productForm.value.original_price !== null && productForm.value.original_price !== '') {
      const originalPriceStr = String(productForm.value.original_price).trim();
      if (originalPriceStr) {
        const parsedOriginalPrice = parseFloat(originalPriceStr);
        if (!isNaN(parsedOriginalPrice) && parsedOriginalPrice >= 0) {
          originalPriceValue = parsedOriginalPrice;
        }
      }
    }
    
    const productData = {
      website_id: websiteId,
      name: productForm.value.name.trim(),
      description: productForm.value.description?.trim() || null,
      price: priceValue,
      original_price: originalPriceValue,
      category: productForm.value.category?.trim() || null,
      is_available: productForm.value.is_available !== false
    };
    
    console.log('Creating/updating product with data:', productData);
    console.log('Original price value:', originalPriceValue);
    console.log('Original price type:', typeof originalPriceValue);
    console.log('Editing website:', editingWebsite.value);
    console.log('Website ID:', websiteId);
    
    let savedProduct;
    if (editingProduct.value) {
      console.log('Updating existing product:', editingProduct.value.id);
      savedProduct = await updateProduct(editingProduct.value.id, productData);
    } else {
      console.log('Creating new product');
      savedProduct = await createProduct(productData);
    }
    
    // Upload image if provided
    if (productForm.value.imageFile && savedProduct.id) {
      await uploadProductImage(savedProduct.id, productForm.value.imageFile);
    }
    
    await loadProducts();
    closeProductForm();
    alert(editingProduct.value ? t('websiteBuilder.updateProduct') + ' ' + t('websiteBuilder.saveChanges') : t('websiteBuilder.createProduct') + ' ' + t('websiteBuilder.saveChanges'));
    
    // Update product count for completion percentage
    if (editingWebsite.value?.id) {
      try {
        const updatedProducts = await getProducts(editingWebsite.value.id);
        websiteProductsCount.value[editingWebsite.value.id] = updatedProducts.length;
      } catch (error) {
        console.error('Failed to update product count:', error);
      }
    }
  } catch (error) {
    console.error('Failed to save product:', error);
    console.error('Error response:', error.response?.data);
    console.error('Error status:', error.response?.status);
    console.error('Error message:', error.message);
    console.error('Product data:', productData);
    console.error('Editing website:', editingWebsite.value);
    
    const errorMessage = error.response?.data?.error || 
                        error.response?.data?.message || 
                        error.message || 
                        t('websiteBuilder.failedToSaveProduct') || 
                        'Failed to save product';
    
    productError.value = errorMessage;
    
    // Show detailed error in alert
    const detailedError = `Failed to save product: ${errorMessage}${error.response?.data?.details ? ` (${error.response.data.details})` : ''}`;
    alert(detailedError);
  } finally {
    savingProduct.value = false;
  }
};

const deleteProduct = async (productId) => {
  if (!confirm(t('websiteBuilder.deleteProductConfirm'))) {
    return;
  }
  
  try {
    await deleteProductAPI(productId);
    await loadProducts();
    alert(t('websiteBuilder.deleteProduct') + ' ' + t('websiteBuilder.saveChanges'));
    
    // Update product count for completion percentage
    if (editingWebsite.value?.id) {
      try {
        const updatedProducts = await getProducts(editingWebsite.value.id);
        websiteProductsCount.value[editingWebsite.value.id] = updatedProducts.length;
      } catch (error) {
        console.error('Failed to update product count:', error);
      }
    }
  } catch (error) {
    alert(t('websiteBuilder.failedToDeleteProduct') + ': ' + error.message);
  }
};

const handleDeleteWebsite = async (id) => {
  if (!confirm(t('websiteBuilder.deleteWebsiteConfirm'))) {
    return;
  }

  try {
    await deleteWebsiteAPI(id);
    await loadWebsites();
    alert(t('websiteBuilder.websiteDeletedSuccess'));
  } catch (error) {
    alert(t('websiteBuilder.failedToDeleteWebsite') + ': ' + error.message);
  }
};

const createAdminAccount = async () => {
  adminError.value = '';
  adminSuccess.value = '';
  
  // Validate passwords match
  if (adminForm.value.password !== adminForm.value.confirmPassword) {
    adminError.value = 'Passwords do not match';
    return;
  }
  
  // Validate password length
  if (adminForm.value.password.length < 6) {
    adminError.value = 'Password must be at least 6 characters long';
    return;
  }
  
  const websiteId = editingWebsite.value?.id || savedWebsiteId.value;
  if (!websiteId) {
    adminError.value = 'Website ID not found. Please save the website first.';
    return;
  }
  
  try {
    creatingAdmin.value = true;
    await registerAdmin({
      website_id: websiteId,
      email: adminForm.value.email,
      password: adminForm.value.password,
      name: adminForm.value.name
    });
    
    adminSuccess.value = t('websiteBuilder.adminAccountCreated');
    adminAccountCreated.value = true;
    
    // Reload admin info to show it exists
    await loadAdminInfo(websiteId);
    
    // Update admin status for completion percentage
    websiteAdmins.value[websiteId] = true;
    
    // Clear form
    adminForm.value = {
      name: '',
      email: '',
      password: '',
      confirmPassword: ''
    };
  } catch (error) {
    console.error('Failed to create admin account:', error);
    adminError.value = error.message || 'Failed to create admin account';
  } finally {
    creatingAdmin.value = false;
  }
};

const getBaseDomain = () => {
  // Get base domain from environment variable or use default
  return import.meta.env.VITE_BASE_DOMAIN || 'restaurantaai.com';
};

const getBarcodeUrl = () => {
  const code = editingWebsite.value?.barcode_code || currentWebsite.value?.barcode_code;
  if (!code) return '';
  const baseUrl = window.location.origin;
  return `${baseUrl}/barcode/${code}`;
};

const copyBarcodeCode = () => {
  const code = editingWebsite.value?.barcode_code || currentWebsite.value?.barcode_code;
  if (code) {
    navigator.clipboard.writeText(code);
    alert(t('websiteBuilder.copy') + ' ' + t('websiteBuilder.barcodeCode'));
  }
};

const copyBarcodeUrl = () => {
  const url = getBarcodeUrl();
  if (url) {
    navigator.clipboard.writeText(url);
    alert(t('websiteBuilder.copyUrl'));
  }
};

const viewWebsite = (id) => {
  // Open website in a new tab
  const url = router.resolve(`/website/${id}`).href;
  window.open(url, '_blank');
};

const resetForm = () => {
  editingWebsite.value = null;
  currentWebsite.value = null;
  activeTab.value = 'basic';
  showMenuPrompt.value = false;
  savedWebsiteId.value = null;
  adminAccountCreated.value = false;
  existingAdmin.value = null;
  adminForm.value = {
    name: '',
    email: '',
    password: '',
    confirmPassword: ''
  };
  adminError.value = '';
  adminSuccess.value = '';
    formData.value = {
      restaurant_name: '',
      logo_url: '',
      description: '',
      address: '',
      phone: '',
      email: '',
      website_url: '',
      primary_color: '#4F46E5',
      secondary_color: '#7C3AED',
      font_family: 'Inter, sans-serif',
      app_download_url: '',
      newsletter_enabled: false,
      is_published: false,
      subdomain: '',
      custom_domain: '',
    };
  logoPreview.value = null;
  logoFile.value = null;
  galleryFiles.value = [];
  galleryImages.value = [];
  locationsText.value = '';
  menuImageFile.value = null;
  menuImagePreview.value = null;
  extractedMenuItems.value = [];
  products.value = [];
  showProductForm.value = false;
  editingProduct.value = null;
};

const calculateCompletion = (website) => {
  if (!website) return 0;
  
  let completion = 0;
  
  // Restaurant name (required) - 10%
  if (website.restaurant_name && website.restaurant_name.trim()) {
    completion += 10;
  }
  
  // Logo - 5%
  if (website.logo_url) {
    completion += 5;
  }
  
  // Description - 10%
  if (website.description && website.description.trim()) {
    completion += 10;
  }
  
  // Contact Information - 15% (5% each)
  if (website.address && website.address.trim()) completion += 5;
  if (website.phone && website.phone.trim()) completion += 5;
  if (website.email && website.email.trim()) completion += 5;
  
  // Products - 20% (check if products exist)
  const productCount = websiteProductsCount.value[website.id] || 0;
  if (productCount > 0) {
    completion += 20;
  }
  
  // Gallery Images - 20%
  try {
    const galleryImages = website.gallery_images 
      ? (typeof website.gallery_images === 'string' 
          ? JSON.parse(website.gallery_images) 
          : website.gallery_images)
      : [];
    if (Array.isArray(galleryImages) && galleryImages.length > 0) {
      completion += 20;
    }
  } catch (e) {
    // Ignore parse errors
  }
  
  // Additional Info - 10% (locations, app_download_url, etc.)
  try {
    const locations = website.locations 
      ? (typeof website.locations === 'string' 
          ? JSON.parse(website.locations) 
          : website.locations)
      : [];
    if (Array.isArray(locations) && locations.length > 0) {
      completion += 5;
    }
  } catch (e) {
    // Ignore parse errors
  }
  if (website.app_download_url && website.app_download_url.trim()) {
    completion += 2.5;
  }
  if (website.social_links) {
    try {
      const socialLinks = typeof website.social_links === 'string' 
        ? JSON.parse(website.social_links) 
        : website.social_links;
      if (socialLinks && Object.keys(socialLinks).length > 0) {
        completion += 2.5;
      }
    } catch (e) {
      // Ignore parse errors
    }
  }
  
  // Colors/Styling - 5% (if custom colors are set)
  if (website.primary_color && website.primary_color !== '#4F46E5') {
    completion += 2.5;
  }
  if (website.secondary_color && website.secondary_color !== '#7C3AED') {
    completion += 2.5;
  }
  
  // Admin Account - 5%
  if (websiteAdmins.value[website.id]) {
    completion += 5;
  }
  
  return Math.min(100, Math.round(completion));
};

const getCompletionColor = (percentage) => {
  if (percentage >= 80) return '#10b981'; // green
  if (percentage >= 60) return '#3b82f6'; // blue
  if (percentage >= 40) return '#f59e0b'; // amber
  if (percentage >= 20) return '#ef4444'; // red
  return '#9ca3af'; // gray
};

const loadWebsiteProductsCount = async () => {
  // Load product counts for all websites
  for (const website of websites.value) {
    if (website.id) {
      try {
        const products = await getProducts(website.id);
        websiteProductsCount.value[website.id] = products.length;
      } catch (error) {
        websiteProductsCount.value[website.id] = 0;
      }
    }
  }
};

const loadWebsiteAdmins = async () => {
  // Load admin info for all websites
  for (const website of websites.value) {
    if (website.id) {
      try {
        const admin = await getAdminByWebsiteId(website.id);
        websiteAdmins.value[website.id] = admin ? true : false;
      } catch (error) {
        websiteAdmins.value[website.id] = false;
      }
    }
  }
};

// Filtered websites based on search query
const filteredWebsites = computed(() => {
  if (!searchQuery.value.trim()) {
    return websites.value;
  }
  
  const query = searchQuery.value.toLowerCase().trim();
  return websites.value.filter(website => {
    // Search by restaurant name
    if (website.restaurant_name?.toLowerCase().includes(query)) {
      return true;
    }
    
    // Search by subdomain
    if (website.subdomain?.toLowerCase().includes(query)) {
      return true;
    }
    
    // Search by custom domain
    if (website.custom_domain?.toLowerCase().includes(query)) {
      return true;
    }
    
    // Search by barcode code
    if (website.barcode_code?.toLowerCase().includes(query)) {
      return true;
    }
    
    // Search by email
    if (website.email?.toLowerCase().includes(query)) {
      return true;
    }
    
    // Search by phone
    if (website.phone?.includes(query)) {
      return true;
    }
    
    return false;
  });
});

const loadWebsites = async () => {
  try {
    websites.value = await getWebsites({ all: true });
    // Load product counts and admin info for all websites
    await Promise.all([
      loadWebsiteProductsCount(),
      loadWebsiteAdmins()
    ]);
  } catch (error) {
    alert('Failed to load websites: ' + error.message);
  }
};


// Dashboard sidebar
const sidebarOpen = ref(true);
const activeDashboardView = ref('manage-restaurants'); // 'manage-restaurants', 'restaurants-table', 'restaurants', 'drivers', 'orders', or 'delivery-company'

// Restaurants table (المطاعم) – modal and form
const showRestaurantModal = ref(false);
const editingRestaurantForTable = ref(null);
const restaurantForm = ref({ name: '', location: '', phone: '', email: '' });
const loadingRestaurantsTable = ref(false);
const savingRestaurantModal = ref(false);

// Dashboard sidebar functions
const toggleSidebar = () => {
  sidebarOpen.value = !sidebarOpen.value;
};

const refreshSite = () => {
  window.location.reload();
};

const loadTodayStatistics = async () => {
  try {
    loadingStats.value = true;
    const stats = await getTodayStatistics();
    todayStats.value = {
      ordersCount: stats.ordersCount || 0,
      revenue: stats.revenue || '0.00',
      deliveryFees: stats.deliveryFees || '0.00',
      totalOwedToRestaurants: stats.totalOwedToRestaurants || '0.00',
      totalPaidToRestaurantsAndDrivers: stats.totalPaidToRestaurantsAndDrivers || '0.00'
    };
  } catch (error) {
    console.error('Error loading today\'s statistics:', error);
    // Set default values on error
    todayStats.value = {
      ordersCount: 0,
      revenue: '0.00',
      deliveryFees: '0.00',
      totalOwedToRestaurants: '0.00',
      totalPaidToRestaurantsAndDrivers: '0.00'
    };
  } finally {
    loadingStats.value = false;
  }
};

const setDashboardView = async (view) => {
  console.log('Setting dashboard view to:', view);
  activeDashboardView.value = view;
  if (view === 'restaurants-table') {
    loadingRestaurantsTable.value = true;
    try {
      await loadWebsites();
    } finally {
      loadingRestaurantsTable.value = false;
    }
  } else if (view === 'delivery-company') {
    await loadDeliveryCompanies();
  }
  console.log('activeDashboardView is now:', activeDashboardView.value);
  if (view === 'drivers') {
    console.log('Loading drivers...');
    loadDrivers();
  } else if (view === 'orders') {
    console.log('Loading orders...');
    loadOrders();
  } else if (view === 'manage-restaurants') {
    console.log('Loading today\'s statistics...');
    loadTodayStatistics();
    selectedRestaurantCard.value = null;
    todayOrders.value = [];
    restaurantsOwed.value = [];
  }
};

// Restaurants table (المطاعم) – modal and CRUD
const openRestaurantModal = (site = null) => {
  editingRestaurantForTable.value = site || null;
  if (site) {
    restaurantForm.value = {
      name: site.restaurant_name || '',
      location: site.address || (Array.isArray(site.locations) && site.locations[0] ? (site.locations[0].address || site.locations[0].name || '') : (typeof site.locations === 'string' ? '' : '')),
      phone: site.phone || '',
      email: site.email || ''
    };
    try {
      if (typeof site.locations === 'string' && site.locations) {
        const loc = JSON.parse(site.locations);
        if (Array.isArray(loc) && loc[0]) {
          restaurantForm.value.location = loc[0].address || loc[0].name || restaurantForm.value.location;
        }
      }
    } catch (_) {}
  } else {
    restaurantForm.value = { name: '', location: '', phone: '', email: '' };
  }
  showRestaurantModal.value = true;
};

const closeRestaurantModal = () => {
  showRestaurantModal.value = false;
  editingRestaurantForTable.value = null;
  restaurantForm.value = { name: '', location: '', phone: '', email: '' };
};

const saveRestaurantModal = async () => {
  const name = restaurantForm.value.name?.trim();
  if (!name) return;
  try {
    savingRestaurantModal.value = true;
    const payload = {
      restaurant_name: name,
      address: restaurantForm.value.location?.trim() || null,
      phone: restaurantForm.value.phone?.trim() || null,
      email: restaurantForm.value.email?.trim() || null
    };
    if (editingRestaurantForTable.value?.id) {
      await updateWebsite(editingRestaurantForTable.value.id, payload);
      alert(t('websiteBuilder.restaurantUpdated', 'تم تحديث المطعم بنجاح'));
    } else {
      await createWebsite(payload);
      alert(t('websiteBuilder.restaurantCreated', 'تم إضافة المطعم بنجاح'));
    }
    await loadWebsites();
    closeRestaurantModal();
  } catch (err) {
    alert(err.message || t('websiteBuilder.saveFailed', 'فشل الحفظ'));
  } finally {
    savingRestaurantModal.value = false;
  }
};

const getRestaurantLocation = (site) => {
  if (site.address) return site.address;
  try {
    const loc = typeof site.locations === 'string' ? JSON.parse(site.locations || '[]') : (site.locations || []);
    if (Array.isArray(loc) && loc[0]) return loc[0].address || loc[0].name || '';
  } catch (_) {}
  return '';
};

const deleteRestaurantFromTable = async (id) => {
  if (!confirm(t('websiteBuilder.deleteRestaurantConfirm', 'هل أنت متأكد من حذف هذا المطعم؟'))) return;
  try {
    await deleteWebsiteAPI(id);
    alert(t('websiteBuilder.restaurantDeleted', 'تم حذف المطعم بنجاح'));
    await loadWebsites();
  } catch (err) {
    alert(err.message || t('websiteBuilder.deleteFailed', 'فشل الحذف'));
  }
};

// Load today's orders based on selected card
const loadTodayOrders = async (cardType) => {
  try {
    loadingTodayOrders.value = true;
    selectedRestaurantCard.value = cardType;
    
    const token = localStorage.getItem('superAdminToken');
    if (!token) {
      alert('Super admin authentication required');
      return;
    }
    
    const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000';
    
    // For Total Owed card, fetch restaurant-level data
    if (cardType === 'total-owed') {
      const response = await fetch(`${API_BASE_URL}/api/admin/statistics/today/restaurants-owed`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to load restaurants owed');
      }
      
      const data = await response.json();
      restaurantsOwed.value = Array.isArray(data.restaurants) ? data.restaurants : [];
      todayOrders.value = []; // Clear orders for this view
      return;
    }
    
    // For other cards, fetch order-level data
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);
    
    let url = `${API_BASE_URL}/api/orders?dateFrom=${today.toISOString()}&dateTo=${tomorrow.toISOString()}`;
    
    // Add filters based on card type
    if (cardType === 'delivery-fees') {
      url += '&orderType=delivery';
    } else if (cardType === 'revenue' || cardType === 'total-paid') {
      // For revenue and gross/total money, only show completed orders (same as statistics calculation)
      url += '&status=completed';
    } else if (cardType === 'all-orders') {
      // No additional filter - show all today's orders
    }
    
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to load orders');
    }
    
    const data = await response.json();
    let orders = Array.isArray(data) ? data : [];
    
    // Show all orders except cancelled ones (unless already filtered by status)
    if (cardType !== 'revenue') {
      orders = orders.filter(order => order.status !== 'cancelled');
    }
    
    todayOrders.value = orders;
    restaurantsOwed.value = []; // Clear restaurants for other views
  } catch (error) {
    console.error('Error loading today\'s orders:', error);
    alert('Failed to load orders: ' + error.message);
    todayOrders.value = [];
    restaurantsOwed.value = [];
  } finally {
    loadingTodayOrders.value = false;
  }
};

const getDriverFilterLabel = (filterValue) => {
  const keyMap = {
    'all': 'websiteBuilder.allDrivers',
    'pending': 'websiteBuilder.pendingDrivers',
    'approved': 'websiteBuilder.approvedDrivers',
    'rejected': 'websiteBuilder.rejectedDrivers'
  };
  return t(keyMap[filterValue] || 'websiteBuilder.allDrivers');
};

const loadDrivers = async () => {
  try {
    loadingDrivers.value = true;
    const token = localStorage.getItem('superAdminToken');
    if (!token) {
      alert('Super admin authentication required. Please log in at /super-admin/login using admin@restaurantaai.com');
      closeDriverDrawer();
      return;
    }
    
    const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000';
    const url = driverFilter.value === 'all' 
      ? `${API_BASE_URL}/api/super-admin/drivers`
      : `${API_BASE_URL}/api/super-admin/drivers?status=${driverFilter.value}`;
    
    console.log('Loading drivers from:', url);
    console.log('Token exists:', !!token);
    
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });
    
    console.log('Response status:', response.status);
    console.log('Response headers:', Object.fromEntries(response.headers.entries()));
    
    if (!response.ok) {
      let errorData;
      try {
        errorData = await response.json();
      } catch (e) {
        errorData = { error: response.statusText || 'Unknown error' };
      }
      console.error('Error response:', errorData);
      
      let errorMessage = errorData.error || `HTTP ${response.status}: ${response.statusText}`;
      
      if (response.status === 401) {
        errorMessage = 'Authentication failed. Please log in again as super admin.';
        localStorage.removeItem('superAdminToken');
      } else if (response.status === 403) {
        errorMessage = 'Access denied. Super admin privileges required.';
      } else if (response.status === 500) {
        errorMessage = 'Server error. Please check the backend logs.';
      }
      
      throw new Error(errorMessage);
    }
    
    const data = await response.json();
    console.log('Drivers loaded:', data);
    console.log('Drivers data type:', typeof data);
    console.log('Is array?', Array.isArray(data));
    drivers.value = Array.isArray(data) ? data : [];
    console.log('drivers.value after assignment:', drivers.value);
    console.log('drivers.value.length:', drivers.value.length);
    // Debug: Check online status fields
    if (drivers.value.length > 0) {
      console.log('Sample driver object:', drivers.value[0]);
      console.log('Online status fields for all drivers:', drivers.value.map(d => ({
        id: d.id,
        name: d.name,
        is_online: d.is_online,
        isOnline: d.isOnline,
        allKeys: Object.keys(d)
      })));
    }
  } catch (error) {
    console.error('Error loading drivers:', error);
    console.error('Error stack:', error.stack);
    alert('Failed to load drivers: ' + (error.message || 'Unknown error. Please check the console for details.'));
  } finally {
    loadingDrivers.value = false;
  }
};

const approveDriver = async (driverId) => {
  if (!confirm(t('websiteBuilder.approveDriverConfirm'))) {
    return;
  }
  
  try {
    processingDriver.value = true;
    const token = localStorage.getItem('superAdminToken');
    if (!token) {
      alert('Super admin authentication required');
      return;
    }
    
    const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000';
    const response = await fetch(`${API_BASE_URL}/api/super-admin/drivers/${driverId}/approve`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to approve driver');
    }
    
    await loadDrivers();
    alert(t('websiteBuilder.driverApproved'));
  } catch (error) {
    console.error('Error approving driver:', error);
    alert('Failed to approve driver: ' + error.message);
  } finally {
    processingDriver.value = false;
  }
};

const rejectDriver = async (driverId) => {
  if (!confirm(t('websiteBuilder.rejectDriverConfirm'))) {
    return;
  }
  
  try {
    processingDriver.value = true;
    const token = localStorage.getItem('superAdminToken');
    if (!token) {
      alert('Super admin authentication required');
      return;
    }
    
    const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000';
    const response = await fetch(`${API_BASE_URL}/api/super-admin/drivers/${driverId}/reject`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to reject driver');
    }
    
    await loadDrivers();
    alert(t('websiteBuilder.driverRejected'));
  } catch (error) {
    console.error('Error rejecting driver:', error);
    alert('Failed to reject driver: ' + error.message);
  } finally {
    processingDriver.value = false;
  }
};

const deleteDriver = async (driverId) => {
  if (!confirm(t('websiteBuilder.deleteDriverConfirm'))) {
    return;
  }
  
  try {
    processingDriver.value = true;
    const token = localStorage.getItem('superAdminToken');
    if (!token) {
      alert('Super admin authentication required');
      return;
    }
    
    const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000';
    const response = await fetch(`${API_BASE_URL}/api/super-admin/drivers/${driverId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to delete driver');
    }
    
    await loadDrivers();
    alert(t('websiteBuilder.driverDeleted'));
  } catch (error) {
    console.error('Error deleting driver:', error);
    alert('Failed to delete driver: ' + error.message);
  } finally {
    processingDriver.value = false;
  }
};

const filteredDrivers = computed(() => {
  if (driverFilter.value === 'all') {
    return drivers.value;
  }
  if (driverFilter.value === 'online') {
    // Filter for online drivers (handle both boolean and number)
    return drivers.value.filter(d => {
      const isOnline = d.is_online !== undefined ? d.is_online : d.isOnline;
      return isOnline === true || isOnline === 1 || isOnline === "1" || isOnline === "true";
    });
  }
  return drivers.value.filter(d => d.status === driverFilter.value);
});

const pendingDriversCount = computed(() => {
  return drivers.value.filter(d => d.status === 'pending').length;
});

// Delivery Companies Functions
const loadDeliveryCompanies = async () => {
  try {
    loadingDeliveryCompanies.value = true;
    deliveryCompanies.value = await getDeliveryCompanies();
  } catch (error) {
    console.error('Error loading delivery companies:', error);
    alert('Failed to load delivery companies: ' + error.message);
    deliveryCompanies.value = [];
  } finally {
    loadingDeliveryCompanies.value = false;
  }
};

const resetDeliveryCompanyForm = () => {
  deliveryCompanyForm.value = {
    company_name: '',
    contact_name: '',
    phone: '',
    address: '',
    emails: [''],
    website: '',
    status: 'active',
    notes: '',
    admin_username: '',
    admin_password: '',
    profile_image: null,
    profile_image_preview: null,
    remove_profile_image: false
  };
  deliveryCompanyError.value = '';
};

const handleProfileImageChange = (event) => {
  const file = event.target.files[0];
  if (file) {
    deliveryCompanyForm.value.profile_image = file;
    const reader = new FileReader();
    reader.onload = (e) => {
      deliveryCompanyForm.value.profile_image_preview = e.target.result;
    };
    reader.readAsDataURL(file);
  }
};

const editDeliveryCompany = (company) => {
  editingDeliveryCompany.value = company;
  deliveryCompanyForm.value = {
    company_name: company.company_name || '',
    contact_name: company.contact_name || '',
    phone: company.phone || '',
    address: company.address || '',
    emails: company.emails && company.emails.length > 0 ? company.emails : [''],
    website: company.website || '',
    status: company.status || 'active',
    notes: company.notes || '',
    admin_username: company.admin_username || '',
    admin_password: '',
    profile_image: null,
    profile_image_preview: company.profile_image_url || null,
    remove_profile_image: false
  };
  showDeliveryCompanyForm.value = true;
};

const saveDeliveryCompany = async () => {
  try {
    savingDeliveryCompany.value = true;
    deliveryCompanyError.value = '';
    
    // Filter out empty emails
    const validEmails = deliveryCompanyForm.value.emails.filter(email => email.trim() !== '');
    
    const companyData = {
      company_name: deliveryCompanyForm.value.company_name,
      contact_name: deliveryCompanyForm.value.contact_name,
      phone: deliveryCompanyForm.value.phone || null,
      address: deliveryCompanyForm.value.address || null,
      emails: validEmails,
      website: deliveryCompanyForm.value.website || null,
      status: deliveryCompanyForm.value.status,
      notes: deliveryCompanyForm.value.notes || null,
      admin_username: deliveryCompanyForm.value.admin_username || undefined,
      admin_password: deliveryCompanyForm.value.admin_password || undefined,
      remove_profile_image: deliveryCompanyForm.value.remove_profile_image
    };
    
    if (deliveryCompanyForm.value.profile_image) {
      companyData.profile_image = deliveryCompanyForm.value.profile_image;
    }
    
    if (editingDeliveryCompany.value) {
      await updateDeliveryCompany(editingDeliveryCompany.value.id, companyData);
    } else {
      await createDeliveryCompany(companyData);
    }
    
    await loadDeliveryCompanies();
    showDeliveryCompanyForm.value = false;
    resetDeliveryCompanyForm();
    editingDeliveryCompany.value = null;
  } catch (error) {
    console.error('Error saving delivery company:', error);
    deliveryCompanyError.value = error.message || 'Failed to save delivery company';
  } finally {
    savingDeliveryCompany.value = false;
  }
};

const deleteDeliveryCompanyHandler = async (id) => {
  if (!confirm(t('websiteBuilder.confirmDeleteCompany'))) {
    return;
  }
  
  try {
    await deleteDeliveryCompany(id);
    await loadDeliveryCompanies();
    alert(t('websiteBuilder.companyDeleted'));
  } catch (error) {
    console.error('Error deleting delivery company:', error);
    alert('Failed to delete delivery company: ' + error.message);
  }
};

// Zones Management Functions
const openZonesManagement = async (company) => {
  // Toggle: if clicking the same company, close it; otherwise, open/switch to the clicked company
  if (selectedDeliveryCompanyForZones.value?.id === company.id && showZonesManagement.value) {
    showZonesManagement.value = false;
    selectedDeliveryCompanyForZones.value = null;
  } else {
    selectedDeliveryCompanyForZones.value = company;
    showZonesManagement.value = true;
    await loadZones(company.id);
  }
};

const loadZones = async (companyId) => {
  try {
    loadingZones.value = true;
    zones.value = await getDeliveryZones(companyId);
  } catch (error) {
    console.error('Error loading zones:', error);
    alert('Failed to load zones: ' + error.message);
    zones.value = [];
  } finally {
    loadingZones.value = false;
  }
};

const resetZoneForm = () => {
  zoneForm.value = {
    zone_name_ar: '',
    zone_name_en: '',
    price: '0.00',
    status: 'active',
    note: '',
    image: null,
    image_preview: null,
    remove_image: false
  };
  zoneError.value = '';
};

const handleZoneImageChange = (event) => {
  const file = event.target.files[0];
  if (file) {
    zoneForm.value.image = file;
    const reader = new FileReader();
    reader.onload = (e) => {
      zoneForm.value.image_preview = e.target.result;
    };
    reader.readAsDataURL(file);
  }
};

const editZone = (zone) => {
  editingZone.value = zone;
  zoneForm.value = {
    zone_name_ar: zone.zone_name_ar || '',
    zone_name_en: zone.zone_name_en || '',
    price: zone.price ? parseFloat(zone.price).toFixed(2) : '0.00',
    status: zone.status || 'active',
    note: zone.note || '',
    image: null,
    image_preview: zone.image_url || null,
    remove_image: false
  };
  showZoneForm.value = true;
};

const saveZone = async () => {
  try {
    savingZone.value = true;
    zoneError.value = '';
    
    const zoneData = {
      zone_name_ar: zoneForm.value.zone_name_ar,
      zone_name_en: zoneForm.value.zone_name_en,
      price: zoneForm.value.price,
      status: zoneForm.value.status,
      note: zoneForm.value.note || null,
      remove_image: zoneForm.value.remove_image
    };
    
    if (zoneForm.value.image) {
      zoneData.image = zoneForm.value.image;
    }
    
    if (editingZone.value) {
      await updateDeliveryZone(editingZone.value.id, zoneData);
    } else {
      await createDeliveryZone(selectedDeliveryCompanyForZones.value.id, zoneData);
    }
    
    await loadZones(selectedDeliveryCompanyForZones.value.id);
    showZoneForm.value = false;
    resetZoneForm();
    editingZone.value = null;
  } catch (error) {
    console.error('Error saving zone:', error);
    zoneError.value = error.message || 'Failed to save zone';
  } finally {
    savingZone.value = false;
  }
};

const deleteZoneHandler = async (id) => {
  if (!confirm(t('websiteBuilder.confirmDeleteZone'))) {
    return;
  }
  
  try {
    await deleteDeliveryZone(id);
    await loadZones(selectedDeliveryCompanyForZones.value.id);
    alert(t('websiteBuilder.zoneDeleted'));
  } catch (error) {
    console.error('Error deleting zone:', error);
    alert('Failed to delete zone: ' + error.message);
  }
};

const onlineDriversCount = computed(() => {
  // MySQL returns is_online as 0 or 1 (number), not boolean
  // Check for both boolean true and number 1
  return drivers.value.filter(d => {
    const isOnline = d.is_online !== undefined ? d.is_online : d.isOnline;
    // Handle boolean true, number 1, or string "1"/"true"
    return isOnline === true || isOnline === 1 || isOnline === "1" || isOnline === "true";
  }).length;
});

const newDriversLast7DaysCount = computed(() => {
  const sevenDaysAgo = new Date();
  sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
  
  return drivers.value.filter(driver => {
    if (!driver.created_at) return false;
    const createdAt = new Date(driver.created_at);
    return createdAt >= sevenDaysAgo;
  }).length;
});

// Orders management
const loadOrders = async () => {
  try {
    loadingOrders.value = true;
    const token = localStorage.getItem('superAdminToken');
    if (!token) {
      alert('Super admin authentication required. Please log in at /super-admin/login using admin@restaurantaai.com');
      return;
    }
    
    const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000';
    const response = await fetch(`${API_BASE_URL}/api/orders`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to load orders');
    }
    
    const data = await response.json();
    orders.value = Array.isArray(data) ? data : [];
    console.log('Orders loaded:', orders.value.length);
  } catch (error) {
    console.error('Error loading orders:', error);
    alert('Failed to load orders: ' + error.message);
  } finally {
    loadingOrders.value = false;
  }
};

const pendingOrdersCount = computed(() => {
  return orders.value.filter(o => o.status === 'pending' || o.status === 'confirmed').length;
});

const completedOrdersCount = computed(() => {
  return orders.value.filter(o => o.status === 'completed' || o.status === 'delivered').length;
});

const todayRevenue = computed(() => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  
  return orders.value
    .filter(order => {
      const orderDate = new Date(order.created_at);
      orderDate.setHours(0, 0, 0, 0);
      return orderDate.getTime() === today.getTime() && 
             (order.status === 'completed' || order.status === 'delivered');
    })
    .reduce((sum, order) => sum + parseFloat(order.total_amount || 0), 0);
});

const last7DaysRevenue = computed(() => {
  const sevenDaysAgo = new Date();
  sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
  sevenDaysAgo.setHours(0, 0, 0, 0);
  
  return orders.value
    .filter(order => {
      const orderDate = new Date(order.created_at);
      return orderDate >= sevenDaysAgo && 
             (order.status === 'completed' || order.status === 'delivered');
    })
    .reduce((sum, order) => sum + parseFloat(order.total_amount || 0), 0);
});

const filteredOrders = computed(() => {
  if (!selectedOrderFilter.value) return [];
  
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const sevenDaysAgo = new Date();
  sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
  sevenDaysAgo.setHours(0, 0, 0, 0);
  
  switch (selectedOrderFilter.value) {
    case 'pending':
      return orders.value.filter(o => o.status === 'pending' || o.status === 'confirmed');
    case 'completed':
      return orders.value.filter(o => o.status === 'completed' || o.status === 'delivered');
    case 'today':
      return orders.value.filter(o => {
        const orderDate = new Date(o.created_at);
        orderDate.setHours(0, 0, 0, 0);
        return orderDate.getTime() === today.getTime();
      });
    case 'last7days':
      return orders.value.filter(o => {
        const orderDate = new Date(o.created_at);
        return orderDate >= sevenDaysAgo;
      });
    default:
      return [];
  }
});

const getRestaurantName = (websiteId) => {
  const website = websites.value.find(w => w.id === websiteId);
  return website ? website.restaurant_name : `Website #${websiteId}`;
};

const getStatusBadgeClass = (status) => {
  const statusMap = {
    'pending': 'bg-yellow-100 text-yellow-800',
    'confirmed': 'bg-blue-100 text-blue-800',
    'preparing': 'bg-orange-100 text-orange-800',
    'ready': 'bg-purple-100 text-purple-800',
    'completed': 'bg-green-100 text-green-800',
    'delivered': 'bg-green-100 text-green-800',
    'cancelled': 'bg-red-100 text-red-800'
  };
  return statusMap[status] || 'bg-gray-100 text-gray-800';
};

const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  const date = new Date(dateString);
  return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
};

const getOrderFilterTitle = () => {
  const titles = {
    'pending': t('websiteBuilder.pendingOrders', 'Pending Orders'),
    'completed': t('websiteBuilder.completedOrders', 'Completed Orders'),
    'today': t('websiteBuilder.todayOrders', 'Today\'s Orders'),
    'last7days': t('websiteBuilder.last7DaysOrders', 'Last 7 Days Orders')
  };
  return titles[selectedOrderFilter.value] || '';
};

onMounted(() => {
  loadWebsites();
  // Load statistics if manage-restaurants view is active
  if (activeDashboardView.value === 'manage-restaurants') {
    loadTodayStatistics();
  }
});
</script>

