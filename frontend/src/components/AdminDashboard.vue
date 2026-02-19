<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Header -->
    <header class="bg-white shadow-sm border-b border-gray-200">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-2xl font-bold text-gray-900">{{ adminInfo?.restaurant_name || 'Admin Dashboard' }}</h1>
            <p class="text-sm text-gray-600">Order Management</p>
          </div>
          <div class="flex items-center gap-4">
            <div class="flex items-center gap-2">
              <div 
                v-if="sseConnected" 
                class="flex items-center gap-1 text-green-600 text-sm"
                title="Real-time updates connected"
              >
                <div class="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
                <span class="hidden sm:inline">Live</span>
              </div>
              <button
                @click="loadOrders"
                :disabled="loading"
                class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
              >
                <svg 
                  :class="['w-5 h-5', loading ? 'animate-spin' : '']"
                  fill="none" 
                  stroke="currentColor" 
                  viewBox="0 0 24 24"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                <span>{{ loading ? 'Refreshing...' : 'Refresh' }}</span>
              </button>
            </div>
            <span class="text-sm text-gray-600">Welcome, {{ adminInfo?.name }}</span>
            <button
              @click="goToRestaurantDashboard"
              class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold text-sm"
            >
              Manage Website
            </button>
            <button
              @click="handleLogout"
              class="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors font-semibold"
            >
              Logout
            </button>
          </div>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- Stats -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <!-- Revenue Stats -->
        <div class="bg-gradient-to-br from-indigo-500 to-indigo-600 rounded-xl shadow-md p-6 text-white">
          <div class="text-sm font-semibold text-indigo-100 mb-1">Today's Revenue</div>
          <div class="text-3xl font-bold">{{ formatCurrency(todaysRevenue) }}</div>
          <div class="text-xs text-indigo-100 mt-2">{{ todaysOrders.length }} order(s)</div>
        </div>
        <div class="bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl shadow-md p-6 text-white">
          <div class="text-sm font-semibold text-blue-100 mb-1">Last 7 Days</div>
          <div class="text-3xl font-bold">{{ formatCurrency(last7DaysRevenue) }}</div>
          <div class="text-xs text-blue-100 mt-2">{{ last7DaysOrders.length }} order(s)</div>
        </div>
        <div class="bg-gradient-to-br from-purple-500 to-purple-600 rounded-xl shadow-md p-6 text-white">
          <div class="text-sm font-semibold text-purple-100 mb-1">Last 30 Days</div>
          <div class="text-3xl font-bold">{{ formatCurrency(last30DaysRevenue) }}</div>
          <div class="text-xs text-purple-100 mt-2">{{ last30DaysOrders.length }} order(s)</div>
        </div>
        <div class="bg-white rounded-xl shadow-md p-6">
          <div class="text-sm font-semibold text-gray-600 mb-1">Today's Orders</div>
          <div class="text-3xl font-bold text-indigo-600">{{ todaysOrders.length }}</div>
          <div class="text-xs text-gray-500 mt-2">
            <span class="text-yellow-600">{{ getTodaysOrdersByStatus('pending').length }} pending</span> • 
            <span class="text-blue-600">{{ getTodaysOrdersByStatus('preparing').length }} preparing</span> • 
            <span class="text-green-600">{{ getTodaysOrdersByStatus('ready').length }} ready</span>
          </div>
        </div>
      </div>

      <!-- Today's Orders Section -->
      <div class="mb-8">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-2xl font-bold text-gray-900">Today's Orders</h2>
          <span class="px-3 py-1 bg-indigo-100 text-indigo-800 rounded-full text-sm font-semibold">
            {{ todaysOrders.length }} orders
          </span>
        </div>

        <!-- Filter Tabs for Today's Orders -->
        <div class="bg-white rounded-xl shadow-md mb-6">
          <div class="border-b border-gray-200">
            <nav class="flex -mb-px overflow-x-auto">
              <button
                v-for="tab in statusTabs"
                :key="tab.status"
                @click="selectedStatus = tab.status"
                :class="[
                  'px-6 py-4 text-sm font-semibold border-b-2 transition-colors whitespace-nowrap',
                  selectedStatus === tab.status
                    ? 'border-indigo-600 text-indigo-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                ]"
              >
                {{ tab.label }}
                <span class="ml-2 bg-gray-100 text-gray-600 px-2 py-1 rounded-full text-xs">
                  {{ getTodaysOrdersByStatus(tab.status).length }}
                </span>
              </button>
            </nav>
          </div>
        </div>

        <!-- Today's Orders List -->
        <div v-if="loading" class="text-center py-12">
          <div class="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
          <p class="mt-4 text-gray-600">Loading orders...</p>
        </div>

        <div v-else-if="filteredTodaysOrders.length === 0" class="text-center py-12 bg-white rounded-xl shadow-md">
          <div class="text-6xl mb-4">📦</div>
          <p class="text-gray-600 text-lg font-semibold">No orders for today</p>
          <p class="text-gray-500 text-sm mt-2">All caught up! 🎉</p>
        </div>

        <div v-else class="space-y-4">
          <div
            v-for="order in filteredTodaysOrders"
            :key="order.id"
            class="bg-white rounded-xl shadow-md p-6 hover:shadow-lg transition-shadow border-l-4"
            :class="getOrderBorderClass(order.status)"
          >
          <div class="flex items-start justify-between mb-4">
            <div>
              <div class="flex items-center gap-3 mb-2">
                <h3 class="text-xl font-bold text-gray-900">Order #{{ order.order_number }}</h3>
                <span
                  :class="[
                    'px-3 py-1 rounded-full text-xs font-semibold',
                    getStatusClass(order.status)
                  ]"
                >
                  {{ order.status.toUpperCase() }}
                </span>
              </div>
              <p class="text-sm text-gray-600">
                {{ new Date(order.created_at).toLocaleString() }}
              </p>
            </div>
            <div class="text-right">
              <div class="text-2xl font-bold text-gray-900">${{ parseFloat(order.total_amount).toFixed(2) }}</div>
            </div>
          </div>

          <!-- Customer Info -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4 pb-4 border-b border-gray-200">
            <div>
              <p class="text-sm font-semibold text-gray-700 mb-1">Customer</p>
              <p class="text-gray-900">{{ order.customer_name }}</p>
              <p class="text-sm text-gray-600">{{ order.customer_phone }}</p>
              <p v-if="order.customer_email" class="text-sm text-gray-600">{{ order.customer_email }}</p>
            </div>
            <div>
              <div v-if="order.customer_address" class="mb-3">
                <p class="text-sm font-semibold text-gray-700 mb-1">Address</p>
                <p class="text-sm text-gray-600">{{ order.customer_address }}</p>
              </div>
              <div v-if="order.payment_method">
                <p class="text-sm font-semibold text-gray-700 mb-1">Payment Method</p>
                <p class="text-sm text-gray-900 font-medium">{{ formatPaymentMethod(order.payment_method) }}</p>
              </div>
            </div>
          </div>

          <!-- Order Items -->
          <div class="mb-4">
            <p class="text-sm font-semibold text-gray-700 mb-2">Items:</p>
            <div class="space-y-2">
              <div
                v-for="item in order.items"
                :key="item.id"
                class="flex items-center justify-between text-sm bg-gray-50 rounded-lg p-3"
              >
                <div>
                  <span class="font-semibold text-gray-900">{{ item.product_name }}</span>
                  <span class="text-gray-600 ml-2">x{{ item.quantity }}</span>
                </div>
                <span class="font-semibold text-gray-900">${{ parseFloat(item.subtotal).toFixed(2) }}</span>
              </div>
            </div>
          </div>

          <!-- Notes -->
          <div v-if="order.notes" class="mb-4 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
            <p class="text-sm font-semibold text-yellow-800 mb-1">Special Instructions:</p>
            <p class="text-sm text-yellow-700">{{ order.notes }}</p>
          </div>

          <!-- Status Update -->
          <div class="flex items-center gap-3 pt-4 border-t border-gray-200">
            <label class="text-sm font-semibold text-gray-700">Update Status:</label>
            <select
              :value="order.status"
              @change="updateStatus(order.id, $event.target.value)"
              class="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            >
              <option value="pending">Pending</option>
              <option value="confirmed">Confirmed</option>
              <option value="preparing">Preparing</option>
              <option value="ready">Ready</option>
              <option value="completed">Completed</option>
              <option value="cancelled">Cancelled</option>
            </select>
            <span v-if="updatingOrderId === order.id" class="text-sm text-gray-600">Updating...</span>
          </div>
          </div>
        </div>
      </div>

      <!-- Archive Section -->
      <div class="mt-12">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-2xl font-bold text-gray-900">Archive</h2>
          <button
            @click="showArchive = !showArchive"
            class="px-4 py-2 text-sm font-semibold text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
          >
            {{ showArchive ? 'Hide' : 'Show' }} Archive
            <span class="ml-2 px-2 py-1 bg-gray-200 text-gray-700 rounded-full text-xs">
              {{ archivedOrders.length }}
            </span>
          </button>
        </div>

        <div v-if="showArchive" class="bg-white rounded-xl shadow-md">
          <div v-if="archivedOrders.length === 0" class="text-center py-12">
            <div class="text-6xl mb-4">📚</div>
            <p class="text-gray-600 text-lg font-semibold">No archived orders</p>
            <p class="text-gray-500 text-sm mt-2">Previous orders will appear here</p>
          </div>

          <div v-else class="divide-y divide-gray-200">
            <div
              v-for="order in archivedOrders"
              :key="order.id"
              class="p-6 hover:bg-gray-50 transition-colors"
            >
              <div class="flex items-start justify-between mb-4">
                <div>
                  <div class="flex items-center gap-3 mb-2">
                    <h3 class="text-lg font-bold text-gray-900">Order #{{ order.order_number }}</h3>
                    <span
                      :class="[
                        'px-3 py-1 rounded-full text-xs font-semibold',
                        getStatusClass(order.status)
                      ]"
                    >
                      {{ order.status.toUpperCase() }}
                    </span>
                  </div>
                  <p class="text-sm text-gray-600">
                    {{ new Date(order.created_at).toLocaleString() }}
                  </p>
                </div>
                <div class="text-right">
                  <div class="text-xl font-bold text-gray-900">${{ parseFloat(order.total_amount).toFixed(2) }}</div>
                </div>
              </div>

              <!-- Customer Info -->
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                <div>
                  <p class="text-sm font-semibold text-gray-700 mb-1">Customer</p>
                  <p class="text-gray-900 text-sm">{{ order.customer_name }}</p>
                  <p class="text-sm text-gray-600">{{ order.customer_phone }}</p>
                </div>
                <div>
                  <p class="text-sm font-semibold text-gray-700 mb-1">Items</p>
                  <p class="text-sm text-gray-600">{{ order.items?.length || 0 }} item(s)</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- Notification Toast -->
    <TransitionGroup name="notification" tag="div" class="fixed bottom-4 right-4 z-50 space-y-2">
      <div
        v-for="notification in notifications"
        :key="notification.id"
        class="bg-white rounded-lg shadow-2xl border-l-4 border-indigo-500 p-4 min-w-[320px] max-w-md animate-slide-in"
      >
        <div class="flex items-start gap-3">
          <div class="flex-shrink-0">
            <div class="w-10 h-10 bg-indigo-100 rounded-full flex items-center justify-center">
              <svg class="w-6 h-6 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
          </div>
          <div class="flex-1">
            <h4 class="text-sm font-bold text-gray-900 mb-1">New Order Received!</h4>
            <p class="text-sm text-gray-600 mb-2">
              Order #{{ notification.orderNumber }} - ${{ parseFloat(notification.totalAmount).toFixed(2) }}
            </p>
            <p class="text-xs text-gray-500">
              {{ notification.customerName }} • {{ notification.itemCount }} item(s)
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, TransitionGroup } from 'vue';
import { useRouter } from 'vue-router';
import { getAdminInfo, getAdminOrders, updateOrderStatusAdmin } from '../services/api.js';

const router = useRouter();
const adminInfo = ref(null);
const orders = ref([]);
const loading = ref(true);
const selectedStatus = ref(null);
const updatingOrderId = ref(null);
const showArchive = ref(false);
const eventSource = ref(null);
const sseConnected = ref(false);
const notifications = ref([]);
let notificationIdCounter = 0;

const statusTabs = [
  { status: null, label: 'All Orders' },
  { status: 'pending', label: 'Pending' },
  { status: 'confirmed', label: 'Confirmed' },
  { status: 'preparing', label: 'Preparing' },
  { status: 'ready', label: 'Ready' },
  { status: 'completed', label: 'Completed' },
  { status: 'cancelled', label: 'Cancelled' }
];

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

// Separate today's orders from archived orders
const todaysOrders = computed(() => {
  return orders.value.filter(order => isToday(order.created_at));
});

const archivedOrders = computed(() => {
  return orders.value.filter(order => !isToday(order.created_at));
});

// Calculate revenue for different time periods
const todaysRevenue = computed(() => {
  return todaysOrders.value.reduce((sum, order) => {
    // Count all orders except cancelled ones
    if (order.status !== 'cancelled') {
      return sum + parseFloat(order.total_amount || 0);
    }
    return sum;
  }, 0);
});

const last7DaysOrders = computed(() => {
  const sevenDaysAgo = new Date();
  sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
  sevenDaysAgo.setHours(0, 0, 0, 0);
  
  return orders.value.filter(order => {
    const orderDate = new Date(order.created_at);
    return orderDate >= sevenDaysAgo;
  });
});

const last7DaysRevenue = computed(() => {
  return last7DaysOrders.value.reduce((sum, order) => {
    // Count all orders except cancelled ones
    if (order.status !== 'cancelled') {
      return sum + parseFloat(order.total_amount || 0);
    }
    return sum;
  }, 0);
});

const last30DaysOrders = computed(() => {
  const thirtyDaysAgo = new Date();
  thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
  thirtyDaysAgo.setHours(0, 0, 0, 0);
  
  return orders.value.filter(order => {
    const orderDate = new Date(order.created_at);
    return orderDate >= thirtyDaysAgo;
  });
});

const last30DaysRevenue = computed(() => {
  return last30DaysOrders.value.reduce((sum, order) => {
    // Count all orders except cancelled ones
    if (order.status !== 'cancelled') {
      return sum + parseFloat(order.total_amount || 0);
    }
    return sum;
  }, 0);
});

// Format currency
const formatCurrency = (amount) => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(amount);
};

// Format payment method for display
const formatPaymentMethod = (method) => {
  if (!method) return 'N/A';
  
  const paymentMethodMap = {
    'cash': 'Cash',
    'card': 'Credit/Debit Card',
    'online': 'Online Payment',
    'mobile': 'Mobile Payment',
    'cliq': 'CliQ Services'
  };
  
  return paymentMethodMap[method.toLowerCase()] || method.charAt(0).toUpperCase() + method.slice(1);
};

// Filter today's orders by status
const filteredTodaysOrders = computed(() => {
  let filtered = todaysOrders.value;
  if (selectedStatus.value !== null) {
    filtered = filtered.filter(order => order.status === selectedStatus.value);
  }
  // Sort by created_at descending (newest first)
  return filtered.sort((a, b) => new Date(b.created_at) - new Date(a.created_at));
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

const loadOrders = async () => {
  try {
    loading.value = true;
    orders.value = await getAdminOrders();
  } catch (error) {
    console.error('Failed to load orders:', error);
    alert('Failed to load orders: ' + error.message);
  } finally {
    loading.value = false;
  }
};

// Setup SSE connection for real-time updates
const setupSSE = () => {
  const token = localStorage.getItem('adminToken');
  if (!token) {
    console.warn('No admin token found, skipping SSE setup');
    return;
  }

  const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000';
  const sseUrl = `${API_BASE_URL}/api/admin/orders/stream`;

  try {
    // Create EventSource with token as query parameter (EventSource doesn't support custom headers)
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
          
          // Show notification
          showNewOrderNotification(data.order);
          
          // Reload orders to get the latest data
          loadOrders();
        } else if (data.type === 'order_status_update') {
          console.log('Order status updated via SSE:', data.orderId, data.status);
          // Update the specific order's status
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
      
      // Attempt to reconnect after 5 seconds if connection is closed
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

// Close SSE connection
const closeSSE = () => {
  if (eventSource.value) {
    eventSource.value.close();
    eventSource.value = null;
    sseConnected.value = false;
  }
};

// Show notification for new order
const showNewOrderNotification = (order) => {
  const notification = {
    id: notificationIdCounter++,
    orderNumber: order.order_number,
    totalAmount: order.total_amount,
    customerName: order.customer_name,
    itemCount: order.items?.length || 0
  };
  
  notifications.value.push(notification);
  
  // Auto-dismiss after 8 seconds
  setTimeout(() => {
    removeNotification(notification.id);
  }, 8000);
};

// Remove notification
const removeNotification = (id) => {
  const index = notifications.value.findIndex(n => n.id === id);
  if (index !== -1) {
    notifications.value.splice(index, 1);
  }
};

const updateStatus = async (orderId, newStatus) => {
  try {
    updatingOrderId.value = orderId;
    await updateOrderStatusAdmin(orderId, newStatus);
    
    // Reload orders
    await loadOrders();
  } catch (error) {
    console.error('Failed to update order status:', error);
    alert('Failed to update order status: ' + error.message);
  } finally {
    updatingOrderId.value = null;
  }
};

const goToRestaurantDashboard = () => {
  // Check if restaurant token exists, if not, redirect to restaurant login
  const restaurantToken = localStorage.getItem('restaurantToken');
  if (restaurantToken) {
    router.push('/restaurant/dashboard');
  } else {
    // If no restaurant token, redirect to restaurant login
    // The login will use the same admin credentials
    router.push('/restaurant/login');
  }
};

const handleLogout = () => {
  localStorage.removeItem('adminToken');
  localStorage.removeItem('adminRefreshToken');
  localStorage.removeItem('adminInfo');
  localStorage.removeItem('restaurantToken');
  localStorage.removeItem('restaurantRefreshToken');
  localStorage.removeItem('restaurantInfo');
  closeSSE();
  router.push('/admin/login');
};

onMounted(async () => {
  // Check if admin is logged in
  const token = localStorage.getItem('adminToken');
  if (!token) {
    router.push('/admin/login');
    return;
  }

  try {
    // Load admin info
    adminInfo.value = await getAdminInfo();
    
    // Load orders
    await loadOrders();
    
    // Setup SSE for real-time updates
    setupSSE();
  } catch (error) {
    console.error('Failed to load admin data:', error);
    // If token is invalid, redirect to login
    if (error.message.includes('token') || error.message.includes('401')) {
      handleLogout();
    } else {
      alert('Failed to load admin data: ' + error.message);
    }
  }
});

onUnmounted(() => {
  // Clean up SSE connection when component is unmounted
  closeSSE();
});
</script>

<style scoped>
/* Notification animations */
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

.notification-move {
  transition: transform 0.3s ease;
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

