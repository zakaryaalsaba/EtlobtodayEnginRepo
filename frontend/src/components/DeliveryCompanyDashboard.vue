<template>
  <div class="min-h-screen bg-gray-50">
    <header class="bg-white shadow-sm border-b border-gray-200">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-2xl font-bold text-gray-900">{{ company?.company_name || 'Delivery Company' }}</h1>
            <p class="text-sm text-gray-600">Manage your zones and captains</p>
          </div>
          <button
            @click="handleLogout"
            class="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors font-semibold text-sm"
          >
            Logout
          </button>
        </div>
      </div>
    </header>

    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div class="flex gap-2 border-b border-gray-200 mb-6">
        <button
          :class="[
            'px-4 py-2 font-medium rounded-t-lg transition-colors',
            activeTab === 'drivers'
              ? 'bg-white border border-b-0 border-gray-200 text-blue-600'
              : 'text-gray-600 hover:text-gray-900'
          ]"
          @click="activeTab = 'drivers'"
        >
          Captains / Drivers
        </button>
        <button
          :class="[
            'px-4 py-2 font-medium rounded-t-lg transition-colors',
            activeTab === 'zones'
              ? 'bg-white border border-b-0 border-gray-200 text-blue-600'
              : 'text-gray-600 hover:text-gray-900'
          ]"
          @click="activeTab = 'zones'"
        >
          Zones
        </button>
        <button
          :class="[
            'px-4 py-2 font-medium rounded-t-lg transition-colors',
            activeTab === 'stores'
              ? 'bg-white border border-b-0 border-gray-200 text-blue-600'
              : 'text-gray-600 hover:text-gray-900'
          ]"
          @click="activeTab = 'stores'"
        >
          Stores
        </button>
        <button
          :class="[
            'px-4 py-2 font-medium rounded-t-lg transition-colors',
            activeTab === 'orders'
              ? 'bg-white border border-b-0 border-gray-200 text-blue-600'
              : 'text-gray-600 hover:text-gray-900'
          ]"
          @click="activeTab = 'orders'"
        >
          Orders
        </button>
      </div>

      <!-- Drivers tab -->
      <div v-show="activeTab === 'drivers'" class="bg-white rounded-lg shadow p-6">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-lg font-semibold text-gray-900">Captains / Drivers</h2>
          <div class="flex gap-2">
            <button
              @click="loadDrivers(); loadStats();"
              :disabled="loadingDrivers"
              class="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors flex items-center gap-2 disabled:opacity-50"
            >
              <svg :class="['w-5 h-5', loadingDrivers ? 'animate-spin' : '']" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              Refresh
            </button>
            <button
              @click="openDriverForm()"
              class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" /></svg>
              Add Driver
            </button>
          </div>
        </div>

        <!-- Statistics Cards (same style as Manage Drivers in builder) -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <div class="bg-gradient-to-br from-indigo-500 to-indigo-600 rounded-xl p-6 text-white shadow-lg">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-indigo-100 text-sm font-medium mb-1">Total Drivers</p>
                <p class="text-3xl font-bold">{{ stats.totalDrivers }}</p>
              </div>
              <div class="bg-white/20 rounded-full p-3">
                <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                </svg>
              </div>
            </div>
          </div>
          <div class="bg-gradient-to-br from-green-500 to-green-600 rounded-xl p-6 text-white shadow-lg">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-green-100 text-sm font-medium mb-1">Online Drivers</p>
                <p class="text-3xl font-bold">{{ stats.onlineDrivers }}</p>
              </div>
              <div class="bg-white/20 rounded-full p-3">
                <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
            </div>
          </div>
          <div class="bg-gradient-to-br from-purple-500 to-purple-600 rounded-xl p-6 text-white shadow-lg">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-purple-100 text-sm font-medium mb-1">New (Last 7 Days)</p>
                <p class="text-3xl font-bold">{{ stats.newDriversLast7Days }}</p>
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
          <div class="flex gap-2 border-b border-gray-200">
            <button
              v-for="f in driverFilters"
              :key="f.value"
              @click="driverFilter = f.value"
              :class="[
                'px-4 py-2 text-sm font-medium border-b-2 transition-colors',
                driverFilter === f.value ? 'border-blue-500 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              ]"
            >
              {{ f.label }}
              <span v-if="f.count !== undefined" class="ml-1 px-2 py-0.5 bg-gray-100 text-gray-600 text-xs rounded-full">{{ f.count }}</span>
            </button>
          </div>
        </div>

        <div v-if="loadingDrivers" class="text-center py-12 text-gray-600">Loading drivers...</div>
        <div v-else-if="filteredDrivers.length === 0" class="text-center py-12 text-gray-500">
          <div class="text-4xl mb-4">🚗</div>
          <p class="font-semibold">No drivers found.</p>
          <p class="text-sm mt-2">Add a driver to get started, or adjust the filter.</p>
        </div>
        <div v-else class="space-y-4">
          <div
            v-for="d in filteredDrivers"
            :key="d.id"
            class="bg-gray-50 rounded-lg p-4 border border-gray-200 hover:shadow-md transition-shadow"
          >
            <div class="flex items-start justify-between mb-3">
              <div class="flex-1">
                <h3 class="text-lg font-semibold text-gray-900">{{ d.name }}</h3>
                <p class="text-sm text-gray-600">{{ d.email }}</p>
                <p v-if="d.phone" class="text-sm text-gray-600">{{ d.phone }}</p>
              </div>
              <span :class="['px-3 py-1 text-xs font-semibold rounded-full', d.status === 'approved' ? 'bg-green-100 text-green-800' : d.status === 'rejected' ? 'bg-red-100 text-red-800' : 'bg-yellow-100 text-yellow-800']">
                {{ d.status }}
              </span>
            </div>
            <div class="text-xs text-gray-500 mb-3">Registered: {{ new Date(d.created_at).toLocaleDateString() }} · Online: {{ d.is_online ? 'Yes' : 'No' }}</div>
            <div class="flex gap-2">
              <button @click="openDriverForm(d)" class="px-3 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700">Edit</button>
              <button @click="deleteDriver(d.id)" :disabled="processingDriver" class="px-3 py-2 bg-gray-200 text-gray-800 text-sm rounded-lg hover:bg-gray-300 disabled:opacity-50">Delete</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Zones tab -->
      <div v-show="activeTab === 'zones'" class="bg-white rounded-lg shadow p-6">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-lg font-semibold text-gray-900">Delivery Zones</h2>
          <div class="flex gap-2">
            <button
              @click="loadGroupedZones()"
              :disabled="loadingZones"
              class="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors flex items-center gap-2 disabled:opacity-50"
            >
              <svg :class="['w-5 h-5', loadingZones ? 'animate-spin' : '']" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              Refresh
            </button>
            <button
              @click="openZoneForm()"
              class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" /></svg>
              Add Zone
            </button>
          </div>
        </div>

        <div v-if="loadingZones" class="text-center py-12 text-gray-600">Loading zones...</div>
        <div v-else-if="hierarchicalZones.length === 0" class="text-center py-12 text-gray-500">
          <div class="text-4xl mb-4">📍</div>
          <p class="font-semibold">No zones found.</p>
          <p class="text-sm mt-2">Add zones to define delivery areas.</p>
        </div>
        <div v-else class="space-y-8">
          <!-- Group by City -->
          <div
            v-for="cityGroup in hierarchicalZones"
            :key="cityGroup.city_id"
            class="border border-gray-300 rounded-lg p-6 bg-gray-50"
          >
            <!-- City Header -->
            <div class="mb-6 pb-4 border-b-2 border-gray-300">
              <h2 class="text-xl font-bold text-gray-900">
                {{ cityGroup.city_name_ar || cityGroup.city_name }}
              </h2>
              <p v-if="cityGroup.city_name && cityGroup.city_name_ar" class="text-sm text-gray-600 mt-1">
                {{ cityGroup.city_name }}
              </p>
            </div>

            <!-- Regions within City -->
            <div class="space-y-6 ml-4">
              <div
                v-for="regionGroup in cityGroup.regions"
                :key="regionGroup.region_id"
                class="border border-gray-200 rounded-lg p-5 bg-white"
              >
                <!-- Region Header with Toggle -->
                <div 
                  class="mb-4 pb-3 border-b border-gray-200 cursor-pointer hover:bg-gray-50 -m-5 p-5 rounded-t-lg transition-colors"
                  @click="toggleRegion(regionGroup.region_id)"
                >
                  <div class="flex items-center justify-between">
                    <div class="flex-1">
                      <h3 class="text-lg font-semibold text-gray-800">
                        {{ regionGroup.region_name_ar || regionGroup.region_name }}
                      </h3>
                      <p v-if="regionGroup.region_name && regionGroup.region_name_ar" class="text-sm text-gray-600 mt-1">
                        {{ regionGroup.region_name }}
                      </p>
                      <p class="text-xs text-gray-500 mt-1">
                        {{ regionGroup.areas.reduce((sum, area) => sum + area.zones.length, 0) }} zone{{ regionGroup.areas.reduce((sum, area) => sum + area.zones.length, 0) !== 1 ? 's' : '' }} 
                        across {{ regionGroup.areas.length }} area{{ regionGroup.areas.length !== 1 ? 's' : '' }}
                      </p>
                    </div>
                    <div class="ml-4">
                      <svg 
                        :class="['w-6 h-6 text-gray-500 transition-transform', isRegionExpanded(regionGroup.region_id) ? 'rotate-180' : '']"
                        fill="none" 
                        stroke="currentColor" 
                        viewBox="0 0 24 24"
                      >
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                      </svg>
                    </div>
                  </div>
                </div>

                <!-- Areas within Region -->
                <div v-show="isRegionExpanded(regionGroup.region_id)" class="space-y-4 ml-4">
                  <div
                    v-for="areaGroup in regionGroup.areas"
                    :key="areaGroup.area_id"
                    class="border border-gray-200 rounded-lg p-4 bg-gray-50"
                  >
                    <!-- Area Header -->
                    <div class="mb-3 pb-2 border-b border-gray-200">
                      <h4 class="text-md font-semibold text-gray-700">
                        {{ areaGroup.area_name_ar || areaGroup.area_name }}
                      </h4>
                      <p v-if="areaGroup.area_name && areaGroup.area_name_ar" class="text-xs text-gray-500 mt-1">
                        {{ areaGroup.area_name }}
                      </p>
                      <p class="text-xs text-gray-500 mt-1">
                        {{ areaGroup.zones.length }} zone{{ areaGroup.zones.length !== 1 ? 's' : '' }}
                      </p>
                    </div>

                    <!-- Zones within Area -->
                    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                      <div
                        v-for="zone in areaGroup.zones"
                        :key="zone.id"
                        class="border border-gray-200 rounded-lg p-3 bg-white hover:shadow-md transition-shadow"
                      >
                        <div class="flex items-start justify-between mb-2">
                          <div class="flex-1">
                            <h5 class="font-semibold text-gray-900 text-sm">{{ zone.zone_name_ar || zone.zone_name_en }}</h5>
                            <p v-if="zone.zone_name_en && zone.zone_name_ar" class="text-xs text-gray-600 mt-1">
                              {{ zone.zone_name_en }}
                            </p>
                          </div>
                          <span :class="[
                            'px-2 py-1 text-xs font-semibold rounded-full',
                            zone.status === 'active' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                          ]">
                            {{ zone.status }}
                          </span>
                        </div>
                        <div class="mt-2 flex items-center justify-between">
                          <div>
                            <p class="text-xs text-gray-600">Price</p>
                            <p class="text-md font-bold text-gray-900">JOD {{ parseFloat(zone.price || 0).toFixed(2) }}</p>
                          </div>
                          <div class="flex gap-1">
                            <button
                              @click="editZone(zone)"
                              class="px-2 py-1 text-xs bg-blue-100 text-blue-700 rounded hover:bg-blue-200 transition-colors"
                            >
                              Edit
                            </button>
                            <button
                              @click="deleteZone(zone.id)"
                              class="px-2 py-1 text-xs bg-red-100 text-red-700 rounded hover:bg-red-200 transition-colors"
                            >
                              Delete
                            </button>
                          </div>
                        </div>
                        <div v-if="zone.note" class="mt-2 pt-2 border-t border-gray-100">
                          <p class="text-xs text-gray-500">{{ zone.note }}</p>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Stores tab -->
      <div v-show="activeTab === 'stores'" class="bg-white rounded-lg shadow p-6">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-lg font-semibold text-gray-900">Stores</h2>
          <button
            @click="loadStoreRequests()"
            :disabled="loadingStoreRequests"
            class="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors flex items-center gap-2 disabled:opacity-50"
          >
            <svg :class="['w-5 h-5', loadingStoreRequests ? 'animate-spin' : '']" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            Refresh
          </button>
        </div>
        <p class="text-sm text-gray-500 mb-6">Restaurant requests to work with your delivery company. Approve or reject below.</p>
        <div v-if="loadingStoreRequests" class="text-center py-12 text-gray-600">Loading requests...</div>
        <div v-else-if="storeRequests.length === 0" class="text-center py-12 text-gray-500">
          <div class="text-4xl mb-4">🏪</div>
          <p class="font-semibold">No store requests yet.</p>
          <p class="text-sm mt-2">Restaurants will appear here when they send a request from their dashboard.</p>
        </div>
        <div v-else class="space-y-4">
          <div
            v-for="r in storeRequests"
            :key="r.id"
            class="border border-gray-200 rounded-lg p-4 flex items-center justify-between flex-wrap gap-3"
          >
            <div>
              <h3 class="font-semibold text-gray-900">{{ r.restaurant_name || 'Restaurant' }}</h3>
              <p v-if="r.subdomain" class="text-sm text-gray-500">{{ r.subdomain }}</p>
              <p class="text-xs text-gray-500 mt-1">Requested {{ new Date(r.created_at).toLocaleDateString() }}</p>
            </div>
            <div class="flex items-center gap-2">
              <span :class="['px-3 py-1 text-xs font-semibold rounded-full', r.status === 'approved' ? 'bg-green-100 text-green-800' : r.status === 'rejected' ? 'bg-red-100 text-red-800' : 'bg-yellow-100 text-yellow-800']">
                {{ r.status }}
              </span>
              <template v-if="r.status === 'pending'">
                <button
                  @click="respondToStoreRequest(r.id, 'approved')"
                  :disabled="respondingStoreRequestId === r.id"
                  class="px-3 py-2 bg-green-600 text-white text-sm rounded-lg hover:bg-green-700 disabled:opacity-50"
                >
                  {{ respondingStoreRequestId === r.id ? '...' : 'Approve' }}
                </button>
                <button
                  @click="respondToStoreRequest(r.id, 'rejected')"
                  :disabled="respondingStoreRequestId === r.id"
                  class="px-3 py-2 bg-red-600 text-white text-sm rounded-lg hover:bg-red-700 disabled:opacity-50"
                >
                  Reject
                </button>
              </template>
            </div>
          </div>
        </div>

        <!-- Store Zones Card (shown when a store is selected) -->
        <div v-if="selectedStore && showStoreZonesCard" class="mt-6 bg-white rounded-lg shadow p-6">
          <div class="flex items-center justify-between mb-6">
            <div>
              <h3 class="text-xl font-bold text-gray-900">
                Zones for {{ selectedStore.restaurant_name || 'Store' }}
              </h3>
              <p class="text-sm text-gray-500 mt-1">Manage delivery zones for this restaurant</p>
            </div>
            <button
              @click="closeStoreZonesCard"
              class="text-gray-400 hover:text-gray-600 transition-colors"
            >
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- Add Zone Button -->
          <div class="mb-4">
            <button
              @click="openZoneFormForStore()"
              class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 flex items-center gap-2"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
              Add Zone
            </button>
          </div>

          <!-- Loading State -->
          <div v-if="loadingStoreZones" class="text-center py-12 text-gray-500">
            Loading zones...
          </div>

          <!-- Zones List -->
          <div v-else-if="storeZones.length === 0" class="text-center py-12 text-gray-500">
            <p class="font-semibold">No zones yet.</p>
            <p class="text-sm mt-2">Add zones to define delivery areas for {{ selectedStore.restaurant_name || 'this store' }}.</p>
          </div>

          <div v-else class="space-y-3">
            <div
              v-for="zone in storeZones"
              :key="zone.id"
              class="border border-gray-200 rounded-lg p-4 flex items-center justify-between hover:shadow-md transition-shadow"
            >
              <div class="flex-1">
                <h4 class="font-semibold text-gray-900">{{ zone.zone_name_en }}</h4>
                <p class="text-sm text-gray-600">{{ zone.zone_name_ar }}</p>
                <p class="text-sm text-gray-500 mt-1">Price: ${{ parseFloat(zone.price || 0).toFixed(2) }}</p>
                <span :class="['inline-block mt-2 px-2 py-1 text-xs font-semibold rounded-full', zone.status === 'active' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800']">
                  {{ zone.status }}
                </span>
              </div>
              <div class="flex items-center gap-2">
                <button
                  @click="editStoreZone(zone)"
                  class="px-3 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700"
                >
                  Edit
                </button>
                <button
                  @click="deleteStoreZone(zone.id)"
                  class="px-3 py-2 bg-red-600 text-white text-sm rounded-lg hover:bg-red-700"
                >
                  Delete
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Orders tab -->
      <div v-show="activeTab === 'orders'" class="bg-white rounded-lg shadow p-6">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-lg font-semibold text-gray-900">Orders</h2>
          <button
            @click="loadOrders()"
            :disabled="loadingOrders"
            class="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors flex items-center gap-2 disabled:opacity-50"
          >
            <svg :class="['w-5 h-5', loadingOrders ? 'animate-spin' : '']" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            Refresh
          </button>
        </div>
        <p class="text-sm text-gray-500 mb-6">Orders delivered by your drivers only.</p>

        <!-- Statistics Cards (same style as Manage Orders) -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          <div
            @click="orderFilter = 'pending'"
            class="bg-gradient-to-br from-yellow-500 to-yellow-600 rounded-xl p-6 text-white shadow-lg cursor-pointer hover:shadow-xl transition-shadow"
          >
            <div class="flex items-center justify-between">
              <div>
                <p class="text-yellow-100 text-sm font-medium mb-1">Pending Orders</p>
                <p class="text-3xl font-bold">{{ (orderStats && orderStats.pending) ?? 0 }}</p>
              </div>
              <div class="bg-white/20 rounded-full p-3">
                <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
            </div>
          </div>
          <div
            @click="orderFilter = 'completed'"
            class="bg-gradient-to-br from-green-500 to-green-600 rounded-xl p-6 text-white shadow-lg cursor-pointer hover:shadow-xl transition-shadow"
          >
            <div class="flex items-center justify-between">
              <div>
                <p class="text-green-100 text-sm font-medium mb-1">Completed Orders</p>
                <p class="text-3xl font-bold">{{ (orderStats && orderStats.completed) ?? 0 }}</p>
              </div>
              <div class="bg-white/20 rounded-full p-3">
                <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
            </div>
          </div>
          <div
            @click="orderFilter = 'today'"
            class="bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl p-6 text-white shadow-lg cursor-pointer hover:shadow-xl transition-shadow"
          >
            <div class="flex items-center justify-between">
              <div>
                <p class="text-blue-100 text-sm font-medium mb-1">Today's Revenue</p>
                <p class="text-3xl font-bold">${{ ((orderStats && orderStats.todayRevenue) ?? 0).toFixed(2) }}</p>
              </div>
              <div class="bg-white/20 rounded-full p-3">
                <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
            </div>
          </div>
          <div
            @click="orderFilter = 'last7days'"
            class="bg-gradient-to-br from-purple-500 to-purple-600 rounded-xl p-6 text-white shadow-lg cursor-pointer hover:shadow-xl transition-shadow"
          >
            <div class="flex items-center justify-between">
              <div>
                <p class="text-purple-100 text-sm font-medium mb-1">Last 7 Days Revenue</p>
                <p class="text-3xl font-bold">${{ ((orderStats && orderStats.last7DaysRevenue) ?? 0).toFixed(2) }}</p>
              </div>
              <div class="bg-white/20 rounded-full p-3">
                <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                </svg>
              </div>
            </div>
          </div>
        </div>

        <!-- Orders table -->
        <div v-if="orderFilter" class="mt-6">
          <h3 class="text-lg font-semibold text-gray-800 mb-4">{{ orderFilterTitle }}</h3>
          <div v-if="loadingOrders" class="text-center py-12 text-gray-600">Loading orders...</div>
          <div v-else-if="filteredOrders.length === 0" class="text-center py-12 text-gray-500">
            <div class="text-4xl mb-4">📦</div>
            <p class="font-semibold">No orders found.</p>
          </div>
          <div v-else class="overflow-x-auto">
            <table class="min-w-full divide-y divide-gray-200">
              <thead class="bg-gray-50">
                <tr>
                  <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Order #</th>
                  <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Restaurant</th>
                  <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Customer</th>
                  <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Driver</th>
                  <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                  <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Type</th>
                  <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Total</th>
                  <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Date</th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-gray-200">
                <tr v-for="order in filteredOrders" :key="order.id" class="hover:bg-gray-50">
                  <td class="px-4 py-3 text-sm font-medium text-gray-900">{{ order.order_number }}</td>
                  <td class="px-4 py-3 text-sm text-gray-600">{{ order.restaurant_name || `#${order.website_id}` }}</td>
                  <td class="px-4 py-3 text-sm text-gray-600">{{ order.customer_name || 'N/A' }}</td>
                  <td class="px-4 py-3 text-sm text-gray-600">{{ order.driver_name || 'N/A' }}</td>
                  <td class="px-4 py-3">
                    <span :class="orderStatusClass(order.status)" class="px-2 py-1 text-xs font-semibold rounded-full">{{ order.status }}</span>
                  </td>
                  <td class="px-4 py-3 text-sm text-gray-600">{{ order.order_type || 'N/A' }}</td>
                  <td class="px-4 py-3 text-sm font-semibold text-gray-900">${{ parseFloat(order.total_amount || 0).toFixed(2) }}</td>
                  <td class="px-4 py-3 text-sm text-gray-600">{{ formatOrderDate(order.created_at) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- Driver form modal -->
    <div v-if="showDriverModal" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex items-center justify-center min-h-screen px-4">
        <div class="fixed inset-0 bg-black/50" @click="showDriverModal = false"></div>
        <div class="relative bg-white rounded-lg shadow-xl max-w-md w-full p-6">
          <h3 class="text-lg font-semibold text-gray-900 mb-4">{{ editingDriver ? 'Edit Driver' : 'Add Driver' }}</h3>
          <form @submit.prevent="saveDriver" class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Name *</label>
              <input v-model="driverForm.name" type="text" required class="w-full px-3 py-2 border border-gray-300 rounded-lg" placeholder="Full name" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Email *</label>
              <input v-model="driverForm.email" type="email" required :readonly="!!editingDriver" class="w-full px-3 py-2 border border-gray-300 rounded-lg" placeholder="email@example.com" />
              <p v-if="editingDriver" class="text-xs text-gray-500 mt-1">Email cannot be changed.</p>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Phone</label>
              <input v-model="driverForm.phone" type="tel" class="w-full px-3 py-2 border border-gray-300 rounded-lg" placeholder="Optional" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Status</label>
              <select v-model="driverForm.status" class="w-full px-3 py-2 border border-gray-300 rounded-lg">
                <option value="pending">Pending</option>
                <option value="approved">Approved</option>
                <option value="rejected">Rejected</option>
              </select>
              <p class="text-xs text-gray-500 mt-1">Approved drivers can log in. Pending/rejected cannot.</p>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ editingDriver ? 'New password (leave blank to keep)' : 'Password *' }}</label>
              <input v-model="driverForm.password" type="password" :required="!editingDriver" class="w-full px-3 py-2 border border-gray-300 rounded-lg" :placeholder="editingDriver ? 'Leave blank to keep current' : 'Min 6 characters'" />
            </div>
            <div v-if="driverError" class="text-sm text-red-600">{{ driverError }}</div>
            <div class="flex gap-2 pt-2">
              <button type="submit" :disabled="savingDriver" class="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50">
                {{ savingDriver ? 'Saving...' : (editingDriver ? 'Update' : 'Create') }}
              </button>
              <button type="button" @click="showDriverModal = false" class="px-4 py-2 bg-gray-200 rounded-lg hover:bg-gray-300">Cancel</button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- Zone form modal -->
    <div v-if="showZoneModal" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex items-center justify-center min-h-screen px-4">
        <div class="fixed inset-0 bg-black/50" @click="showZoneModal = false"></div>
        <div class="relative bg-white rounded-lg shadow-xl max-w-md w-full p-6">
          <h3 class="text-lg font-semibold text-gray-900 mb-4">{{ editingZone ? 'Edit Zone' : 'Add Zone' }}</h3>
          <form @submit.prevent="saveZone" class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Zone name (English) *</label>
              <input v-model="zoneForm.zone_name_en" type="text" required class="w-full px-3 py-2 border border-gray-300 rounded-lg" placeholder="e.g. Downtown" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Zone name (Arabic) *</label>
              <input v-model="zoneForm.zone_name_ar" type="text" required class="w-full px-3 py-2 border border-gray-300 rounded-lg" placeholder="اسم المنطقة" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Area *</label>
              <select v-model="zoneForm.area_id" required class="w-full px-3 py-2 border border-gray-300 rounded-lg">
                <option value="">Select an area</option>
                <option v-for="area in areas" :key="area.id" :value="area.id">
                  {{ area.name_ar || area.name }} ({{ area.city_name_ar || area.city_name }})
                </option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Price *</label>
              <input v-model="zoneForm.price" type="number" step="0.01" min="0" required class="w-full px-3 py-2 border border-gray-300 rounded-lg" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Status</label>
              <select v-model="zoneForm.status" class="w-full px-3 py-2 border border-gray-300 rounded-lg">
                <option value="active">Active</option>
                <option value="inactive">Inactive</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Note</label>
              <textarea v-model="zoneForm.note" rows="2" class="w-full px-3 py-2 border border-gray-300 rounded-lg"></textarea>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Image</label>
              <input type="file" accept="image/*" @change="onZoneImageChange" class="w-full text-sm" />
              <button v-if="zoneForm.image_preview" type="button" @click="zoneForm.image = null; zoneForm.image_preview = null" class="mt-1 text-sm text-red-600">Remove</button>
            </div>
            <div v-if="zoneError" class="text-sm text-red-600">{{ zoneError }}</div>
            <div class="flex gap-2 pt-2">
              <button type="submit" :disabled="savingZone" class="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50">
                {{ savingZone ? 'Saving...' : (editingZone ? 'Update' : 'Create') }}
              </button>
              <button type="button" @click="showZoneModal = false" class="px-4 py-2 bg-gray-200 rounded-lg hover:bg-gray-300">Cancel</button>
            </div>
          </form>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import {
  deliveryCompanyGetMe,
  deliveryCompanyGetZones,
  deliveryCompanyGetAreas,
  deliveryCompanyCreateZone,
  deliveryCompanyUpdateZone,
  deliveryCompanyDeleteZone,
  deliveryCompanyGetStoreZones,
  deliveryCompanyGetDrivers,
  deliveryCompanyGetStats,
  deliveryCompanyCreateDriver,
  deliveryCompanyUpdateDriver,
  deliveryCompanyDeleteDriver,
  deliveryCompanyGetOrders,
  deliveryCompanyGetStoreRequests,
  deliveryCompanyRespondStoreRequest,
} from '../services/api.js';

const router = useRouter();
const company = ref(null);
const zones = ref([]);
const groupedZones = ref([]);
const drivers = ref([]);
const stats = ref({
  totalDrivers: 0,
  onlineDrivers: 0,
  newDriversLast7Days: 0,
  pending: 0,
  approved: 0,
  rejected: 0,
});
const loadingZones = ref(false);
const loadingDrivers = ref(false);
const loadingOrders = ref(false);
const orders = ref([]);
const orderFilter = ref('last7days');
const activeTab = ref('stores');
const storeRequests = ref([]);
const loadingStoreRequests = ref(false);
const respondingStoreRequestId = ref(null);
const driverFilter = ref('all');
const showStoreZonesCard = ref(false);
const selectedStore = ref(null);
const storeZones = ref([]);
const loadingStoreZones = ref(false);
const showZoneModal = ref(false);
const showDriverModal = ref(false);
const editingZone = ref(null);
const editingDriver = ref(null);
const savingZone = ref(false);
const savingDriver = ref(false);
const processingDriver = ref(false);
const zoneError = ref('');
const driverError = ref('');
const areas = ref([]);
const loadingAreas = ref(false);
const expandedRegions = ref(new Set()); // Track which regions are expanded
const zoneForm = ref({
  zone_name_en: '',
  zone_name_ar: '',
  area_id: '',
  price: '0.00',
  status: 'active',
  note: '',
  image: null,
  image_preview: null,
});
const driverForm = ref({ name: '', email: '', phone: '', password: '', status: 'approved' });

const driverFilters = computed(() => [
  { value: 'all', label: 'All', count: drivers.value.length },
  { value: 'pending', label: 'Pending', count: drivers.value.filter(d => d.status === 'pending').length },
  { value: 'approved', label: 'Approved', count: drivers.value.filter(d => d.status === 'approved').length },
  { value: 'rejected', label: 'Rejected', count: drivers.value.filter(d => d.status === 'rejected').length },
]);
const filteredDrivers = computed(() => {
  if (driverFilter.value === 'all') return drivers.value;
  return drivers.value.filter(d => d.status === driverFilter.value);
});

const orderStats = computed(() => {
  const list = Array.isArray(orders.value) ? orders.value : [];
  const now = new Date();
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const sevenDaysAgo = new Date(todayStart);
  sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
  const pending = list.filter(o => (o.status || '').toLowerCase() === 'pending').length;
  const completed = list.filter(o => (o.status || '').toLowerCase() === 'completed').length;
  const todayRevenue = list
    .filter(o => o.status === 'completed' && new Date(o.updated_at || o.created_at) >= todayStart)
    .reduce((sum, o) => sum + (Number(o.total_price) || 0), 0);
  const last7DaysRevenue = list
    .filter(o => o.status === 'completed' && new Date(o.updated_at || o.created_at) >= sevenDaysAgo)
    .reduce((sum, o) => sum + (Number(o.total_price) || 0), 0);
  return { pending, completed, todayRevenue, last7DaysRevenue };
});

const filteredOrders = computed(() => {
  const list = Array.isArray(orders.value) ? orders.value : [];
  const f = orderFilter.value;
  if (f === 'pending') return list.filter(o => (o.status || '').toLowerCase() === 'pending');
  if (f === 'completed') return list.filter(o => (o.status || '').toLowerCase() === 'completed');
  const now = new Date();
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const sevenDaysAgo = new Date(todayStart);
  sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
  if (f === 'today') return list.filter(o => new Date(o.updated_at || o.created_at) >= todayStart);
  if (f === 'last7days') return list.filter(o => new Date(o.updated_at || o.created_at) >= sevenDaysAgo);
  return list;
});

const orderFilterTitle = computed(() => {
  const t = { pending: 'Pending orders', completed: 'Completed orders', today: "Today's orders", last7days: 'Last 7 days' }[orderFilter.value];
  return t || 'Orders';
});

// Reorganize grouped zones into hierarchical structure: City → Region → Area → Zones
const hierarchicalZones = computed(() => {
  if (!Array.isArray(groupedZones.value) || groupedZones.value.length === 0) {
    return [];
  }

  // Create a nested structure: cities -> regions -> areas -> zones
  const citiesMap = new Map();

  groupedZones.value.forEach(areaGroup => {
    const cityId = areaGroup.city_id;
    const cityName = areaGroup.city_name;
    const cityNameAr = areaGroup.city_name_ar;
    const regionId = areaGroup.region_id;
    const regionName = areaGroup.region_name;
    const regionNameAr = areaGroup.region_name_ar;
    const areaId = areaGroup.area_id;
    const areaName = areaGroup.area_name;
    const areaNameAr = areaGroup.area_name_ar;

    // Initialize city if not exists
    if (!citiesMap.has(cityId)) {
      citiesMap.set(cityId, {
        city_id: cityId,
        city_name: cityName,
        city_name_ar: cityNameAr,
        regions: new Map()
      });
    }

    const city = citiesMap.get(cityId);

    // Initialize region if not exists
    if (!city.regions.has(regionId)) {
      city.regions.set(regionId, {
        region_id: regionId,
        region_name: regionName,
        region_name_ar: regionNameAr,
        areas: []
      });
    }

    const region = city.regions.get(regionId);

    // Add area with its zones
    region.areas.push({
      area_id: areaId,
      area_name: areaName,
      area_name_ar: areaNameAr,
      zones: areaGroup.zones || []
    });
  });

  // Convert Maps to Arrays and sort
  return Array.from(citiesMap.values())
    .map(city => ({
      ...city,
      regions: Array.from(city.regions.values())
        .map(region => ({
          ...region,
          areas: region.areas.sort((a, b) => {
            const nameA = a.area_name_ar || a.area_name || '';
            const nameB = b.area_name_ar || b.area_name || '';
            return nameA.localeCompare(nameB);
          })
        }))
        .sort((a, b) => {
          const nameA = a.region_name_ar || a.region_name || '';
          const nameB = b.region_name_ar || b.region_name || '';
          return nameA.localeCompare(nameB);
        })
    }))
    .sort((a, b) => {
      const nameA = a.city_name_ar || a.city_name || '';
      const nameB = b.city_name_ar || b.city_name || '';
      return nameA.localeCompare(nameB);
    });
});

async function loadCompany() {
  try {
    company.value = await deliveryCompanyGetMe();
  } catch (e) {
    console.error(e);
    router.push('/delivery-company/login');
  }
}

async function loadZones() {
  loadingZones.value = true;
  try {
    zones.value = await deliveryCompanyGetZones();
  } catch (e) {
    console.error(e);
  } finally {
    loadingZones.value = false;
  }
}

async function loadGroupedZones() {
  loadingZones.value = true;
  try {
    groupedZones.value = await deliveryCompanyGetZones(true);
    // Expand all regions by default when zones are loaded
    nextTick(() => {
      hierarchicalZones.value.forEach(city => {
        city.regions.forEach(region => {
          expandedRegions.value.add(region.region_id);
        });
      });
    });
  } catch (e) {
    console.error('Failed to load grouped zones:', e);
    alert(e.message || 'Failed to load zones');
    groupedZones.value = [];
  } finally {
    loadingZones.value = false;
  }
}

async function loadDrivers() {
  loadingDrivers.value = true;
  try {
    drivers.value = await deliveryCompanyGetDrivers();
  } catch (e) {
    console.error(e);
  } finally {
    loadingDrivers.value = false;
  }
}

async function loadStats() {
  try {
    const data = await deliveryCompanyGetStats();
    stats.value = data;
  } catch (e) {
    console.error(e);
  }
}

async function loadOrders() {
  loadingOrders.value = true;
  try {
    orders.value = await deliveryCompanyGetOrders();
  } catch (e) {
    console.error(e);
    orders.value = [];
  } finally {
    loadingOrders.value = false;
  }
}

async function loadStoreRequests() {
  loadingStoreRequests.value = true;
  try {
    storeRequests.value = await deliveryCompanyGetStoreRequests();
  } catch (e) {
    console.error(e);
    storeRequests.value = [];
  } finally {
    loadingStoreRequests.value = false;
  }
}

async function respondToStoreRequest(id, status) {
  respondingStoreRequestId.value = id;
  try {
    await deliveryCompanyRespondStoreRequest(id, status);
    await loadStoreRequests();
  } catch (e) {
    alert(e.message || 'Failed to respond');
  } finally {
    respondingStoreRequestId.value = null;
  }
}

async function toggleStoreZones(store) {
  // If clicking the same store, toggle it closed
  if (selectedStore.value?.website_id === store.website_id && showStoreZonesCard.value) {
    closeStoreZonesCard();
    return;
  }
  
  // Otherwise, open zones for the selected store
  selectedStore.value = store;
  showStoreZonesCard.value = true;
  await loadStoreZones();
}

function closeStoreZonesCard() {
  showStoreZonesCard.value = false;
  selectedStore.value = null;
  storeZones.value = [];
}

async function loadStoreZones() {
  if (!selectedStore.value?.website_id) return;
  loadingStoreZones.value = true;
  try {
    storeZones.value = await deliveryCompanyGetStoreZones(selectedStore.value.website_id);
  } catch (e) {
    console.error('Failed to load store zones:', e);
    alert(e.message || 'Failed to load zones');
    storeZones.value = [];
  } finally {
    loadingStoreZones.value = false;
  }
}

async function openZoneFormForStore() {
  // Load areas if not already loaded
  if (areas.value.length === 0) {
    await loadAreas();
  }
  
  editingZone.value = null;
  zoneForm.value = {
    zone_name_en: '',
    zone_name_ar: '',
    area_id: selectedStore.value?.area_id || '',
    price: '0.00',
    status: 'active',
    note: '',
    image: null,
    image_preview: null,
    website_id: selectedStore.value?.website_id
  };
  zoneError.value = '';
  showZoneModal.value = true;
}

async function editStoreZone(zone) {
  // Load areas if not already loaded
  if (areas.value.length === 0) {
    await loadAreas();
  }
  
  editingZone.value = zone;
  zoneForm.value = {
    zone_name_en: zone.zone_name_en || '',
    area_id: zone.area_id || '',
    zone_name_ar: zone.zone_name_ar || '',
    price: zone.price?.toString() || '0.00',
    status: zone.status || 'active',
    note: zone.note || '',
    image: null,
    image_preview: zone.image_url || null,
    website_id: zone.website_id || selectedStore.value?.website_id
  };
  zoneError.value = '';
  showZoneModal.value = true;
}

async function deleteStoreZone(zoneId) {
  if (!confirm('Are you sure you want to delete this zone?')) return;
  try {
    await deliveryCompanyDeleteZone(zoneId);
    // Reload store zones
    await loadStoreZones();
  } catch (e) {
    alert(e.message || 'Failed to delete zone');
  }
}

function openDriverForm(driver = null) {
  editingDriver.value = driver;
  driverForm.value = {
    name: driver?.name ?? '',
    email: driver?.email ?? '',
    phone: driver?.phone ?? '',
    password: '',
    status: driver?.status ?? 'approved',
  };
  driverError.value = '';
  showDriverModal.value = true;
}

async function saveDriver() {
  driverError.value = '';
  savingDriver.value = true;
  try {
    if (editingDriver.value) {
      const payload = { name: driverForm.value.name.trim(), phone: driverForm.value.phone || undefined, status: driverForm.value.status };
      if (driverForm.value.password && driverForm.value.password.length >= 6) payload.password = driverForm.value.password;
      await deliveryCompanyUpdateDriver(editingDriver.value.id, payload);
    } else {
      if (!driverForm.value.password || driverForm.value.password.length < 6) {
        driverError.value = 'Password must be at least 6 characters';
        return;
      }
      await deliveryCompanyCreateDriver({
        name: driverForm.value.name.trim(),
        email: driverForm.value.email.trim(),
        phone: driverForm.value.phone || undefined,
        password: driverForm.value.password,
        status: driverForm.value.status,
      });
    }
    showDriverModal.value = false;
    await loadDrivers();
    await loadStats();
  } catch (e) {
    driverError.value = e.message || 'Failed to save driver';
  } finally {
    savingDriver.value = false;
  }
}

async function deleteDriver(id) {
  if (!confirm('Delete this driver? They will no longer be able to log in.')) return;
  processingDriver.value = true;
  try {
    await deliveryCompanyDeleteDriver(id);
    await loadDrivers();
    await loadStats();
  } catch (e) {
    alert(e.message || 'Failed to delete driver');
  } finally {
    processingDriver.value = false;
  }
}

async function loadAreas() {
  try {
    loadingAreas.value = true;
    areas.value = await deliveryCompanyGetAreas();
  } catch (e) {
    console.error('Failed to load areas:', e);
    alert(e.message || 'Failed to load areas');
    areas.value = [];
  } finally {
    loadingAreas.value = false;
  }
}

async function openZoneForm(zone = null) {
  // Load areas if not already loaded
  if (areas.value.length === 0) {
    await loadAreas();
  }
  
  // For Zones tab, allow creating zones without a selected store
  // For Stores tab, require a selected store
  if (!zone && activeTab.value === 'stores' && !selectedStore.value) {
    alert('Please select a store first by clicking "Manage Zones" on an approved store.');
    return;
  }
  
  editingZone.value = zone;
  zoneForm.value = {
    zone_name_en: zone?.zone_name_en ?? '',
    zone_name_ar: zone?.zone_name_ar ?? '',
    area_id: zone?.area_id ?? '',
    price: zone?.price?.toString() ?? '0.00',
    status: zone?.status ?? 'active',
    note: zone?.note ?? '',
    image: null,
    image_preview: zone?.image_url || null,
    website_id: zone?.website_id || selectedStore.value?.website_id || null,
  };
  zoneError.value = '';
  showZoneModal.value = true;
}

function onZoneImageChange(e) {
  const file = e.target.files?.[0];
  if (file) {
    zoneForm.value.image = file;
    zoneForm.value.image_preview = URL.createObjectURL(file);
  }
}

async function saveZone() {
  zoneError.value = '';
  savingZone.value = true;
  try {
    const payload = {
      zone_name_en: zoneForm.value.zone_name_en,
      zone_name_ar: zoneForm.value.zone_name_ar,
      area_id: zoneForm.value.area_id,
      price: zoneForm.value.price,
      status: zoneForm.value.status,
      note: zoneForm.value.note || undefined,
    };
    // Include website_id if creating/editing a zone for a specific store (legacy support)
    if (zoneForm.value.website_id) {
      payload.website_id = zoneForm.value.website_id.toString();
    }
    if (zoneForm.value.image) payload.image = zoneForm.value.image;
    if (editingZone.value) {
      await deliveryCompanyUpdateZone(editingZone.value.id, payload);
    } else {
      await deliveryCompanyCreateZone(payload);
    }
    showZoneModal.value = false;
    // Reload zones based on active tab
    if (activeTab.value === 'zones') {
      await loadGroupedZones();
    } else if (showStoreZonesCard.value && selectedStore.value) {
      await loadStoreZones();
    }
  } catch (e) {
    zoneError.value = e.message || 'Failed to save zone';
  } finally {
    savingZone.value = false;
  }
}

async function deleteZone(id) {
  if (!confirm('Delete this zone?')) return;
  try {
    await deliveryCompanyDeleteZone(id);
    // Reload zones based on active tab
    if (activeTab.value === 'zones') {
      await loadGroupedZones();
    } else if (showStoreZonesCard.value && selectedStore.value) {
      await loadStoreZones();
    }
  } catch (e) {
    alert(e.message || 'Failed to delete zone');
  }
}

async function editZone(zone) {
  await openZoneForm(zone);
  // Ensure area_id is included when editing
  if (zone?.area_id) {
    zoneForm.value.area_id = zone.area_id;
  }
  // Ensure website_id is included when editing (legacy support)
  if (zone?.website_id) {
    zoneForm.value.website_id = zone.website_id;
  }
}

function handleLogout() {
  localStorage.removeItem('deliveryCompanyToken');
  localStorage.removeItem('deliveryCompanyRefreshToken');
  localStorage.removeItem('deliveryCompanyInfo');
  router.push('/delivery-company/login');
}

watch(activeTab, (tab) => {
  if (tab === 'drivers') {
    loadDrivers();
    loadStats();
  }
  if (tab === 'zones') {
    loadGroupedZones().then(() => {
      // Expand all regions by default when zones tab is opened
      hierarchicalZones.value.forEach(city => {
        city.regions.forEach(region => {
          expandedRegions.value.add(region.region_id);
        });
      });
    });
  }
  if (tab === 'stores') {
    loadStoreRequests();
  }
  if (tab === 'orders') loadOrders();
});

function toggleRegion(regionId) {
  if (expandedRegions.value.has(regionId)) {
    expandedRegions.value.delete(regionId);
  } else {
    expandedRegions.value.add(regionId);
  }
}

function isRegionExpanded(regionId) {
  return expandedRegions.value.has(regionId);
}

onMounted(async () => {
  await loadCompany();
  if (activeTab.value === 'drivers') {
    await loadDrivers();
    await loadStats();
  } else if (activeTab.value === 'zones') {
    await loadGroupedZones();
    // Expand all regions by default when zones are loaded
    hierarchicalZones.value.forEach(city => {
      city.regions.forEach(region => {
        expandedRegions.value.add(region.region_id);
      });
    });
  } else if (activeTab.value === 'stores') {
    await loadStoreRequests();
  } else if (activeTab.value === 'orders') await loadOrders();
});
</script>
