import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

/** Token/key pairs: refresh token in localStorage -> access token key to update after refresh */
const REFRESH_TOKEN_KEYS = [
  { refreshKey: 'restaurantRefreshToken', tokenKey: 'restaurantToken' },
  { refreshKey: 'adminRefreshToken', tokenKey: 'adminToken' },
  { refreshKey: 'deliveryCompanyRefreshToken', tokenKey: 'deliveryCompanyToken' },
];

/**
 * Call backend to exchange a refresh token for a new access token (ID token).
 */
export async function refreshAccessToken(refreshToken) {
  const response = await api.post('/api/auth/refresh', { refreshToken });
  return response.data.token;
}

/**
 * On 401 (e.g. token expired), try to refresh using stored refresh token and retry the request.
 */
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status !== 401 || originalRequest._retryAfterRefresh) {
      return Promise.reject(error);
    }

    const isExpired =
      error.response?.data?.expired === true ||
      (error.response?.data?.error && String(error.response.data.error).toLowerCase().includes('expired'));

    if (!isExpired) {
      return Promise.reject(error);
    }

    originalRequest._retryAfterRefresh = true;

    for (const { refreshKey, tokenKey } of REFRESH_TOKEN_KEYS) {
      const refreshToken = localStorage.getItem(refreshKey);
      if (!refreshToken) continue;
      try {
        const newToken = await refreshAccessToken(refreshToken);
        localStorage.setItem(tokenKey, newToken);
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return api(originalRequest);
      } catch (e) {
        // This refresh token didn't work; clear it and try next
        localStorage.removeItem(refreshKey);
        localStorage.removeItem(tokenKey);
      }
    }

    return Promise.reject(error);
  }
);

/**
 * Get all restaurant websites.
 * @param {Object} opts - Optional. Pass { all: true } to include unpublished restaurants (e.g. for builder "Your Websites").
 */
export async function getWebsites(opts = {}) {
  try {
    const params = opts.all ? { all: 'true' } : {};
    const response = await api.get('/api/websites', { params });
    return response.data.websites;
  } catch (error) {
    console.error('Error fetching websites:', error);
    console.error('Error response:', error.response?.data);
    console.error('Error status:', error.response?.status);
    
    if (error.response) {
      // Server responded with error
      const errorMessage = error.response.data?.error || 
                          error.response.data?.message || 
                          `Server error: ${error.response.status} ${error.response.statusText}`;
      throw new Error(errorMessage);
    } else if (error.request) {
      // Request was made but no response received
      console.error('No response received from server');
      throw new Error('No response from server. Please check if the backend is running on port 3000.');
    } else {
      // Something else happened
      throw new Error(error.message || 'Failed to fetch websites');
    }
  }
}

/**
 * Get a specific restaurant website
 */
export async function getWebsite(id) {
  try {
    const response = await api.get(`/api/websites/${id}`);
    return response.data.website;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch website');
  }
}

/**
 * Get website by domain or subdomain
 */
export async function getWebsiteByDomain(domain) {
  try {
    const response = await api.get(`/api/websites/domain/${domain}`);
    return response.data.website;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch website by domain');
  }
}

/**
 * Get website by barcode code
 */
export async function getWebsiteByBarcode(code) {
  try {
    const response = await api.get(`/api/websites/barcode/${code}`);
    return response.data.website;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch website');
  }
}

/**
 * Generate barcode code for an existing website
 */
export async function generateBarcodeCode(websiteId) {
  try {
    const response = await api.post(`/api/websites/${websiteId}/generate-barcode`);
    return response.data.website;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to generate barcode code');
  }
}

/**
 * Create a new restaurant website
 */
export async function createWebsite(websiteData) {
  try {
    const response = await api.post('/api/websites', websiteData);
    return response.data.website;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to create website');
  }
}

/**
 * Update a restaurant website
 */
export async function updateWebsite(id, websiteData) {
  try {
    const response = await api.put(`/api/websites/${id}`, websiteData);
    return response.data.website;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to update website');
  }
}

/**
 * Delete a restaurant website
 */
export async function deleteWebsite(id) {
  try {
    await api.delete(`/api/websites/${id}`);
    return true;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to delete website');
  }
}

/**
 * Upload logo for a restaurant website
 */
export async function uploadLogo(id, file) {
  try {
    const formData = new FormData();
    formData.append('logo', file);
    
    const response = await api.post(`/api/websites/${id}/logo`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data.website;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to upload logo');
  }
}

/**
 * Upload gallery images for a restaurant website
 */
export async function uploadGalleryImages(id, files) {
  try {
    if (!files || files.length === 0) {
      throw new Error('No files provided');
    }
    
    const formData = new FormData();
    files.forEach(file => {
      formData.append('images', file);
    });
    
    const response = await api.post(`/api/websites/${id}/gallery`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data.website;
  } catch (error) {
    const errorMessage = error.response?.data?.error || error.response?.data?.message || error.message || 'Failed to upload gallery images';
    console.error('Gallery upload error:', {
      error,
      response: error.response?.data,
      status: error.response?.status
    });
    throw new Error(errorMessage);
  }
}

/**
 * Delete a gallery image
 */
export async function deleteGalleryImage(id, imageIndex) {
  try {
    await api.delete(`/api/websites/${id}/gallery/${imageIndex}`);
    return true;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to delete image');
  }
}

/**
 * Upload menu image and extract menu items
 */
export async function uploadMenuImage(id, file) {
  try {
    if (!file) {
      throw new Error('No file provided');
    }
    
    const formData = new FormData();
    formData.append('menu_image', file);
    
    const response = await api.post(`/api/websites/${id}/menu-image`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  } catch (error) {
    const errorMessage = error.response?.data?.error || error.response?.data?.message || error.message || 'Failed to upload menu image';
    console.error('Menu image upload error:', {
      error,
      response: error.response?.data,
      status: error.response?.status
    });
    throw new Error(errorMessage);
  }
}

// ========== PRODUCTS API ==========

/**
 * Get all products for a website
 */
export async function getProducts(websiteId) {
  try {
    const response = await api.get(`/api/products/website/${websiteId}`);
    return response.data.products;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch products');
  }
}

/**
 * Create a product
 */
export async function createProduct(productData) {
  try {
    console.log('API: Creating product with data:', productData);
    console.log('API: Request URL:', `${API_BASE_URL}/api/products`);
    console.log('API: Request payload:', JSON.stringify(productData, null, 2));
    
    const response = await api.post('/api/products', productData);
    
    console.log('API: Product created successfully:', response.data);
    console.log('API: Response status:', response.status);
    
    return response.data.product;
  } catch (error) {
    console.error('API: Error creating product:', error);
    console.error('API: Error response:', error.response?.data);
    console.error('API: Error status:', error.response?.status);
    console.error('API: Error message:', error.message);
    console.error('API: Full error:', JSON.stringify(error, null, 2));
    
    if (error.response) {
      // Server responded with error
      const errorMessage = error.response.data?.error || 
                          error.response.data?.message || 
                          `Server error: ${error.response.status} ${error.response.statusText}`;
      throw new Error(errorMessage);
    } else if (error.request) {
      // Request was made but no response received
      console.error('API: No response received from server');
      throw new Error('No response from server. Please check if the backend is running.');
    } else {
      // Something else happened
      throw new Error(error.message || 'Failed to create product');
    }
  }
}

/**
 * Update a product
 */
export async function updateProduct(id, productData) {
  try {
    const response = await api.put(`/api/products/${id}`, productData);
    return response.data.product;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to update product');
  }
}

/**
 * Delete a product
 */
export async function deleteProduct(id) {
  try {
    await api.delete(`/api/products/${id}`);
    return true;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to delete product');
  }
}

/**
 * Upload product image
 */
export async function uploadProductImage(id, file) {
  try {
    const formData = new FormData();
    formData.append('image', file);
    
    const response = await api.post(`/api/products/${id}/image`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data.product;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to upload product image');
  }
}

// ========== ORDERS API ==========

/**
 * Create an order
 */
/**
 * Create Stripe payment intent
 */
export async function createPaymentIntent(paymentData) {
  try {
    const response = await api.post('/api/payments/create-intent', paymentData);
    return response.data;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to create payment intent');
  }
}

export async function createOrder(orderData) {
  try {
    const response = await api.post('/api/orders', orderData);
    return response.data.order;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to create order');
  }
}

/**
 * Get orders for a website (restaurant view)
 */
export async function getOrders(websiteId, status = null) {
  try {
    const url = status 
      ? `/api/orders/website/${websiteId}?status=${status}`
      : `/api/orders/website/${websiteId}`;
    const response = await api.get(url);
    return response.data.orders;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch orders');
  }
}

/**
 * Get order by order number (customer tracking)
 */
export async function getOrderByNumber(orderNumber) {
  try {
    const response = await api.get(`/api/orders/${orderNumber}`);
    if (!response.data || !response.data.order) {
      throw new Error('Order not found in response');
    }
    console.log('Order fetched from API:', response.data.order);
    return response.data.order;
  } catch (error) {
    console.error('Error in getOrderByNumber:', error);
    throw new Error(error.response?.data?.error || error.message || 'Failed to fetch order');
  }
}

/**
 * Update order status
 */
export async function updateOrderStatus(orderId, status) {
  try {
    const response = await api.put(`/api/orders/${orderId}/status`, { status });
    return response.data.order;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to update order status');
  }
}

/**
 * Update payment status
 */
export async function updatePaymentStatus(orderId, paymentStatus) {
  try {
    const response = await api.put(`/api/orders/${orderId}/payment`, { payment_status: paymentStatus });
    return response.data.order;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to update payment status');
  }
}

// ========== ADMIN API ==========

/**
 * Register a new admin
 */
export async function registerAdmin(adminData) {
  try {
    const response = await api.post('/api/admin/register', adminData);
    return response.data;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to register admin');
  }
}

/**
 * Admin login
 */
export async function adminLogin(email, password) {
  try {
    const response = await api.post('/api/admin/login', { email, password });
    return response.data;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to login');
  }
}

/**
 * Get current admin info
 */
export async function getAdminInfo() {
  try {
    // Try restaurant token first, then admin token
    const token = localStorage.getItem('restaurantToken') || localStorage.getItem('adminToken');
    if (!token) {
      throw new Error('No token found');
    }
    
    const response = await api.get('/api/admin/me', {
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
    return response.data.admin;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch admin info');
  }
}

/**
 * Get orders for admin's restaurant
 */
export async function getAdminOrders(status = null) {
  try {
    // Try restaurant token first, then admin token
    const token = localStorage.getItem('restaurantToken') || localStorage.getItem('adminToken');
    if (!token) {
      throw new Error('No token found');
    }
    
    const url = status 
      ? `/api/admin/orders?status=${status}`
      : '/api/admin/orders';
    
    const response = await api.get(url, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
    return response.data.orders;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch orders');
  }
}

/**
 * Update order status (admin)
 */
export async function updateOrderStatusAdmin(orderId, status) {
  try {
    // Try restaurant token first, then admin token
    const token = localStorage.getItem('restaurantToken') || localStorage.getItem('adminToken');
    if (!token) {
      throw new Error('No token found');
    }
    
    const response = await api.put(`/api/admin/orders/${orderId}/status`, { status }, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
    return response.data.order;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to update order status');
  }
}

/**
 * Register a customer
 */
export async function registerCustomer(websiteId, customerData) {
  try {
    const response = await api.post('/api/customers', {
      website_id: websiteId,
      ...customerData
    });
    return response.data;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to register customer');
  }
}

/**
 * Link an order to a customer
 */
export async function linkOrderToCustomer(customerId, orderId) {
  try {
    const response = await api.post(`/api/customers/${customerId}/link-order`, {
      order_id: orderId
    });
    return response.data;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to link order to customer');
  }
}

/**
 * Get customer orders
 */
export async function getCustomerOrders(customerId) {
  try {
    const response = await api.get(`/api/customers/${customerId}/orders`);
    return response.data.orders;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch customer orders');
  }
}

/**
 * Restaurant-scoped API functions
 */

/**
 * Get restaurant's own website
 */
export async function getRestaurantWebsite() {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) {
      throw new Error('No token found');
    }
    
    const response = await api.get('/api/restaurant/website', {
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
    return response.data.website;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch website');
  }
}

/**
 * Update menu category order for the logged-in restaurant
 * payload example: { "Main Dishes": 1, "Drinks": 2 }
 */
export async function updateRestaurantMenuCategoryOrder(orderMap) {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) {
      throw new Error('No token found');
    }

    const response = await api.put(
      '/api/restaurant/menu-category-order',
      { order: orderMap || {} },
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );

    return response.data.order;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to update category order');
  }
}

/**
 * Update restaurant's own website
 */
export async function updateRestaurantWebsite(websiteData) {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) {
      throw new Error('No token found');
    }
    
    const response = await api.put('/api/restaurant/website', websiteData, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
    return response.data.website;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to update website');
  }
}

/**
 * List delivery companies for restaurant (dropdown)
 */
export async function getRestaurantDeliveryCompanies() {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) throw new Error('No token found');
    const response = await api.get('/api/restaurant/delivery-companies', {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.companies;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch delivery companies');
  }
}

/**
 * Create or re-send delivery company request
 */
export async function createRestaurantDeliveryCompanyRequest(deliveryCompanyId) {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) throw new Error('No token found');
    const response = await api.post('/api/restaurant/delivery-company-request', { delivery_company_id: deliveryCompanyId }, {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.request;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to send request');
  }
}

/**
 * Get current delivery company request and approved company
 */
export async function getRestaurantDeliveryCompanyRequest() {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) throw new Error('No token found');
    const response = await api.get('/api/restaurant/delivery-company-request', {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch request');
  }
}

/**
 * Delete/cancel a delivery company request
 */
export async function deleteRestaurantDeliveryCompanyRequest(requestId) {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) throw new Error('No token found');
    const response = await api.delete(`/api/restaurant/delivery-company-request/${requestId}`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to delete request');
  }
}

/**
 * Get restaurant's own products
 */
export async function getRestaurantProducts() {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) {
      throw new Error('No token found');
    }
    
    const response = await api.get('/api/restaurant/products', {
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
    return response.data.products;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch products');
  }
}

/**
 * Get restaurant products with addons nested (for offer percent_off selected items)
 */
export async function getProductsWithAddons() {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) throw new Error('No token found');
    const response = await api.get('/api/restaurant/products-with-addons', {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.products;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch products');
  }
}

/**
 * Create product for restaurant
 */
export async function createRestaurantProduct(productData) {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) {
      throw new Error('No token found');
    }
    
    const response = await api.post('/api/restaurant/products', productData, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
    return response.data.product;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to create product');
  }
}

/**
 * Update product for restaurant
 */
export async function updateRestaurantProduct(productId, productData) {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) {
      throw new Error('No token found');
    }
    
    const response = await api.put(`/api/restaurant/products/${productId}`, productData, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
    return response.data.product;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to update product');
  }
}

/**
 * Delete product for restaurant
 */
export async function deleteRestaurantProduct(productId) {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) {
      throw new Error('No token found');
    }
    
    await api.delete(`/api/restaurant/products/${productId}`, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to delete product');
  }
}

// ----- Product Add-ons (restaurant dashboard) -----
export async function getProductAddons(productId) {
  const token = localStorage.getItem('restaurantToken');
  if (!token) throw new Error('No token found');
  const response = await api.get(`/api/restaurant/products/${productId}/addons`, {
    headers: { Authorization: `Bearer ${token}` }
  });
  return response.data;
}

export async function createProductAddon(productId, data) {
  const token = localStorage.getItem('restaurantToken');
  if (!token) throw new Error('No token found');
  const response = await api.post(`/api/restaurant/products/${productId}/addons`, data, {
    headers: { Authorization: `Bearer ${token}` }
  });
  return response.data.addon;
}

export async function updateProductAddon(productId, addonId, data) {
  const token = localStorage.getItem('restaurantToken');
  if (!token) throw new Error('No token found');
  const response = await api.put(`/api/restaurant/products/${productId}/addons/${addonId}`, data, {
    headers: { Authorization: `Bearer ${token}` }
  });
  return response.data.addon;
}

export async function deleteProductAddon(productId, addonId) {
  const token = localStorage.getItem('restaurantToken');
  if (!token) throw new Error('No token found');
  await api.delete(`/api/restaurant/products/${productId}/addons/${addonId}`, {
    headers: { Authorization: `Bearer ${token}` }
  });
}

export async function uploadAddonImage(productId, addonId, file) {
  const token = localStorage.getItem('restaurantToken');
  if (!token) throw new Error('No token found');
  const formData = new FormData();
  formData.append('image', file);
  const response = await api.post(`/api/restaurant/products/${productId}/addons/${addonId}/image`, formData, {
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'multipart/form-data' }
  });
  return response.data.addon;
}

export async function updateProductAddonSettings(productId, { addon_required, addon_required_min }) {
  const token = localStorage.getItem('restaurantToken');
  if (!token) throw new Error('No token found');
  const response = await api.put(`/api/restaurant/products/${productId}/addon-settings`, {
    addon_required,
    addon_required_min: addon_required_min === '' || addon_required_min === null ? null : addon_required_min
  }, {
    headers: { Authorization: `Bearer ${token}` }
  });
  return response.data.product;
}

/**
 * Get admin info by website ID (for website builder)
 */
export async function getAdminByWebsiteId(websiteId) {
  try {
    const response = await api.get(`/api/admin/website/${websiteId}`);
    return response.data.admin;
  } catch (error) {
    // If admin doesn't exist, return null instead of throwing
    if (error.response?.status === 404) {
      return null;
    }
    throw new Error(error.response?.data?.error || 'Failed to fetch admin info');
  }
}

/**
 * Get notification settings for restaurant
 */
export async function getNotificationSettings() {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) {
      throw new Error('No token found');
    }
    
    const response = await api.get('/api/restaurant/notifications/settings', {
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
    return response.data;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch notification settings');
  }
}

/**
 * Update notification settings for restaurant
 */
export async function updateNotificationSettings(settings) {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) {
      throw new Error('No token found');
    }
    
    const response = await api.put('/api/restaurant/notifications/settings', settings, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
    return response.data;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to update notification settings');
  }
}

/**
 * Get all coupons for restaurant
 */
export async function getRestaurantCoupons() {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) {
      throw new Error('No token found');
    }
    
    const response = await api.get('/api/restaurant/coupons', {
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
    return response.data.coupons;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch coupons');
  }
}

/**
 * Create a new coupon
 */
export async function createCoupon(couponData) {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) {
      throw new Error('No token found');
    }
    
    const response = await api.post('/api/restaurant/coupons', couponData, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
    return response.data.coupon;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to create coupon');
  }
}

/**
 * Update a coupon
 */
export async function updateCoupon(couponId, couponData) {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) {
      throw new Error('No token found');
    }
    
    const response = await api.put(`/api/restaurant/coupons/${couponId}`, couponData, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
    return response.data.coupon;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to update coupon');
  }
}

/**
 * Delete a coupon
 */
export async function deleteCouponAPI(couponId) {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) {
      throw new Error('No token found');
    }
    
    await api.delete(`/api/restaurant/coupons/${couponId}`, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to delete coupon');
  }
}

// ========== OFFERS (restaurant dashboard) ==========

/**
 * Get all offers for the restaurant (dashboard)
 */
export async function getRestaurantOffers() {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) throw new Error('No token found');
    const response = await api.get('/api/restaurant/offers', {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.offers;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch offers');
  }
}

/**
 * Create an offer
 */
export async function createOffer(offerData) {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) throw new Error('No token found');
    const response = await api.post('/api/restaurant/offers', offerData, {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.offer;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to create offer');
  }
}

/**
 * Update an offer
 */
export async function updateOffer(offerId, offerData) {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) throw new Error('No token found');
    const response = await api.put(`/api/restaurant/offers/${offerId}`, offerData, {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.offer;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to update offer');
  }
}

/**
 * Delete an offer
 */
export async function deleteOfferAPI(offerId) {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) throw new Error('No token found');
    await api.delete(`/api/restaurant/offers/${offerId}`, {
      headers: { Authorization: `Bearer ${token}` }
    });
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to delete offer');
  }
}

/**
 * Get active offers for a website (public, for customers)
 */
export async function getOffersByWebsiteId(websiteId) {
  try {
    const response = await api.get(`/api/websites/${websiteId}/offers`);
    return response.data.offers;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch offers');
  }
}

/**
 * Validate a coupon code (public endpoint)
 */
export async function validateCoupon(websiteId, code, orderAmount) {
  try {
    const response = await api.post('/api/coupons/validate', {
      website_id: websiteId,
      code: code,
      order_amount: orderAmount
    });
    return response.data;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to validate coupon');
  }
}

/**
 * Get today's statistics for all restaurants
 */
export async function getTodayStatistics() {
  try {
    const response = await api.get('/api/admin/statistics/today');
    return response.data;
  } catch (error) {
    console.error('Error fetching today\'s statistics:', error);
    throw new Error(error.response?.data?.error || 'Failed to fetch statistics');
  }
}

// ========== BUSINESS HOURS (restaurant dashboard) ==========

/**
 * Get business hours for restaurant (7 days)
 */
export async function getBusinessHours() {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) throw new Error('No token found');
    const response = await api.get('/api/restaurant/business-hours', {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.business_hours;
  } catch (error) {
    throw new Error(error.response?.data?.error || error.response?.data?.message || 'Failed to fetch business hours');
  }
}

/**
 * Update business hours (array of 7 items: { day_of_week, open_time, close_time, is_closed })
 */
export async function updateBusinessHours(hours) {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) throw new Error('No token found');
    const response = await api.put('/api/restaurant/business-hours', { hours }, {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.business_hours;
  } catch (error) {
    throw new Error(error.response?.data?.error || error.response?.data?.message || 'Failed to update business hours');
  }
}

// ========== DELIVERY COMPANIES (super admin) ==========

/**
 * Get all delivery companies
 */
export async function getDeliveryCompanies() {
  try {
    const token = localStorage.getItem('superAdminToken') || localStorage.getItem('adminToken');
    if (!token) throw new Error('No token found');
    const response = await api.get('/api/delivery-companies', {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.companies;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch delivery companies');
  }
}

/**
 * Get a single delivery company by ID
 */
export async function getDeliveryCompany(id) {
  try {
    const token = localStorage.getItem('superAdminToken') || localStorage.getItem('adminToken');
    if (!token) throw new Error('No token found');
    const response = await api.get(`/api/delivery-companies/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.company;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch delivery company');
  }
}

/**
 * Create a new delivery company
 */
export async function createDeliveryCompany(companyData) {
  try {
    const token = localStorage.getItem('superAdminToken') || localStorage.getItem('adminToken');
    if (!token) throw new Error('No token found');
    
    const formData = new FormData();
    Object.keys(companyData).forEach(key => {
      if (key === 'profile_image' && companyData[key] instanceof File) {
        formData.append('profile_image', companyData[key]);
      } else if (key === 'emails' && Array.isArray(companyData[key])) {
        formData.append('emails', JSON.stringify(companyData[key]));
      } else if (companyData[key] !== null && companyData[key] !== undefined) {
        formData.append(key, companyData[key]);
      }
    });
    
    const response = await api.post('/api/delivery-companies', formData, {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'multipart/form-data'
      }
    });
    return response.data.company;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to create delivery company');
  }
}

/**
 * Update a delivery company
 */
export async function updateDeliveryCompany(id, companyData) {
  try {
    const token = localStorage.getItem('superAdminToken') || localStorage.getItem('adminToken');
    if (!token) throw new Error('No token found');
    
    const formData = new FormData();
    Object.keys(companyData).forEach(key => {
      if (key === 'profile_image' && companyData[key] instanceof File) {
        formData.append('profile_image', companyData[key]);
      } else if (key === 'emails' && Array.isArray(companyData[key])) {
        formData.append('emails', JSON.stringify(companyData[key]));
      } else if (companyData[key] !== null && companyData[key] !== undefined) {
        formData.append(key, companyData[key]);
      }
    });
    
    const response = await api.put(`/api/delivery-companies/${id}`, formData, {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'multipart/form-data'
      }
    });
    return response.data.company;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to update delivery company');
  }
}

/**
 * Delete a delivery company
 */
export async function deleteDeliveryCompany(id) {
  try {
    const token = localStorage.getItem('superAdminToken') || localStorage.getItem('adminToken');
    if (!token) throw new Error('No token found');
    const response = await api.delete(`/api/delivery-companies/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to delete delivery company');
  }
}

// ========== DELIVERY ZONES (super admin) ==========

/**
 * Get all zones for a delivery company
 */
export async function getDeliveryZones(companyId) {
  try {
    const token = localStorage.getItem('superAdminToken') || localStorage.getItem('adminToken');
    if (!token) throw new Error('No token found');
    const response = await api.get(`/api/delivery-companies/${companyId}/zones`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.zones;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch delivery zones');
  }
}

/**
 * Get a single zone by ID
 */
export async function getDeliveryZone(id) {
  try {
    const token = localStorage.getItem('superAdminToken') || localStorage.getItem('adminToken');
    if (!token) throw new Error('No token found');
    const response = await api.get(`/api/delivery-zones/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.zone;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch delivery zone');
  }
}

/**
 * Create a new zone for a delivery company
 */
export async function createDeliveryZone(companyId, zoneData) {
  try {
    const token = localStorage.getItem('superAdminToken') || localStorage.getItem('adminToken');
    if (!token) throw new Error('No token found');
    
    const formData = new FormData();
    Object.keys(zoneData).forEach(key => {
      if (key === 'image' && zoneData[key] instanceof File) {
        formData.append('image', zoneData[key]);
      } else if (zoneData[key] !== null && zoneData[key] !== undefined) {
        formData.append(key, zoneData[key]);
      }
    });
    
    const response = await api.post(`/api/delivery-companies/${companyId}/zones`, formData, {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'multipart/form-data'
      }
    });
    return response.data.zone;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to create delivery zone');
  }
}

/**
 * Update a delivery zone
 */
export async function updateDeliveryZone(id, zoneData) {
  try {
    const token = localStorage.getItem('superAdminToken') || localStorage.getItem('adminToken');
    if (!token) throw new Error('No token found');
    
    const formData = new FormData();
    Object.keys(zoneData).forEach(key => {
      if (key === 'image' && zoneData[key] instanceof File) {
        formData.append('image', zoneData[key]);
      } else if (zoneData[key] !== null && zoneData[key] !== undefined) {
        formData.append(key, zoneData[key]);
      }
    });
    
    const response = await api.put(`/api/delivery-zones/${id}`, formData, {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'multipart/form-data'
      }
    });
    return response.data.zone;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to update delivery zone');
  }
}

/**
 * Delete a delivery zone
 */
export async function deleteDeliveryZone(id) {
  try {
    const token = localStorage.getItem('superAdminToken') || localStorage.getItem('adminToken');
    if (!token) throw new Error('No token found');
    const response = await api.delete(`/api/delivery-zones/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to delete delivery zone');
  }
}

// ========== Delivery Company Admin (dashboard for delivery company admins) ==========

const getDeliveryCompanyToken = () => localStorage.getItem('deliveryCompanyToken');

export async function deliveryCompanyLogin(username, password) {
  try {
    const response = await api.post('/api/auth/delivery-company/login', { username, password });
    return response.data;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Login failed');
  }
}

export async function deliveryCompanyGetMe() {
  try {
    const token = getDeliveryCompanyToken();
    if (!token) throw new Error('No token found');
    const response = await api.get('/api/delivery-company/me', {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.company;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch profile');
  }
}

export async function deliveryCompanyGetZones(grouped = false) {
  try {
    const token = getDeliveryCompanyToken();
    if (!token) throw new Error('No token found');
    const response = await api.get('/api/delivery-company/zones', {
      params: { grouped: grouped ? 'true' : 'false' },
      headers: { Authorization: `Bearer ${token}` }
    });
    return grouped ? response.data.groupedZones : response.data.zones;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch zones');
  }
}

export async function deliveryCompanyGetAreas() {
  try {
    const token = getDeliveryCompanyToken();
    if (!token) throw new Error('No token found');
    const response = await api.get('/api/delivery-company/areas', {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.areas;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch areas');
  }
}

export async function deliveryCompanyCreateZone(zoneData) {
  try {
    const token = getDeliveryCompanyToken();
    if (!token) throw new Error('No token found');
    const formData = new FormData();
    Object.keys(zoneData).forEach(key => {
      if (key === 'image' && zoneData[key] instanceof File) {
        formData.append('image', zoneData[key]);
      } else if (zoneData[key] !== null && zoneData[key] !== undefined) {
        formData.append(key, zoneData[key]);
      }
    });
    const response = await api.post('/api/delivery-company/zones', formData, {
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'multipart/form-data' }
    });
    return response.data.zone;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to create zone');
  }
}

/**
 * Get zones for a specific store/restaurant
 */
export async function deliveryCompanyGetStoreZones(websiteId) {
  try {
    const token = getDeliveryCompanyToken();
    if (!token) throw new Error('No token found');
    const response = await api.get(`/api/delivery-company/stores/${websiteId}/zones`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.zones;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch store zones');
  }
}

export async function deliveryCompanyUpdateZone(id, zoneData) {
  try {
    const token = getDeliveryCompanyToken();
    if (!token) throw new Error('No token found');
    const formData = new FormData();
    Object.keys(zoneData).forEach(key => {
      if (key === 'image' && zoneData[key] instanceof File) {
        formData.append('image', zoneData[key]);
      } else if (zoneData[key] !== null && zoneData[key] !== undefined) {
        formData.append(key, zoneData[key]);
      }
    });
    const response = await api.put(`/api/delivery-company/zones/${id}`, formData, {
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'multipart/form-data' }
    });
    return response.data.zone;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to update zone');
  }
}

export async function deliveryCompanyDeleteZone(id) {
  try {
    const token = getDeliveryCompanyToken();
    if (!token) throw new Error('No token found');
    await api.delete(`/api/delivery-company/zones/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    });
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to delete zone');
  }
}

export async function deliveryCompanyGetDrivers() {
  try {
    const token = getDeliveryCompanyToken();
    if (!token) throw new Error('No token found');
    const response = await api.get('/api/delivery-company/drivers', {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.drivers;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch drivers');
  }
}

export async function deliveryCompanyGetStats() {
  try {
    const token = getDeliveryCompanyToken();
    if (!token) throw new Error('No token found');
    const response = await api.get('/api/delivery-company/stats', {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch stats');
  }
}

export async function deliveryCompanyCreateDriver(data) {
  try {
    const token = getDeliveryCompanyToken();
    if (!token) throw new Error('No token found');
    const response = await api.post('/api/delivery-company/drivers', data, {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.driver;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to create driver');
  }
}

export async function deliveryCompanyUpdateDriver(id, data) {
  try {
    const token = getDeliveryCompanyToken();
    if (!token) throw new Error('No token found');
    const response = await api.put(`/api/delivery-company/drivers/${id}`, data, {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.driver;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to update driver');
  }
}

export async function deliveryCompanyDeleteDriver(id) {
  try {
    const token = getDeliveryCompanyToken();
    if (!token) throw new Error('No token found');
    await api.delete(`/api/delivery-company/drivers/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    });
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to delete driver');
  }
}

export async function deliveryCompanyGetOrders(params = {}) {
  try {
    const token = getDeliveryCompanyToken();
    if (!token) throw new Error('No token found');
    const qs = new URLSearchParams(params).toString();
    const url = qs ? `/api/delivery-company/orders?${qs}` : '/api/delivery-company/orders';
    const response = await api.get(url, {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.orders;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch orders');
  }
}

export async function deliveryCompanyGetStoreRequests() {
  try {
    const token = getDeliveryCompanyToken();
    if (!token) throw new Error('No token found');
    const response = await api.get('/api/delivery-company/store-requests', {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.requests;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch store requests');
  }
}

export async function deliveryCompanyRespondStoreRequest(id, status) {
  try {
    const token = getDeliveryCompanyToken();
    if (!token) throw new Error('No token found');
    const response = await api.patch(`/api/delivery-company/store-requests/${id}`, { status }, {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.request;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to respond to request');
  }
}

// ========== BRANCHES (restaurant dashboard) ==========

/**
 * Get all regions (for branch selection)
 */
export async function getRestaurantRegions() {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) throw new Error('No token found');
    const response = await api.get('/api/restaurant/regions', {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.regions;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch regions');
  }
}

/**
 * Get all branches for the restaurant
 */
export async function getRestaurantBranches() {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) throw new Error('No token found');
    const response = await api.get('/api/restaurant/branches', {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.branches;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to fetch branches');
  }
}

/**
 * Create a branch
 */
export async function createRestaurantBranch(branchData) {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) throw new Error('No token found');
    const response = await api.post('/api/restaurant/branches', branchData, {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.branch;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to create branch');
  }
}

/**
 * Update a branch
 */
export async function updateRestaurantBranch(id, branchData) {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) throw new Error('No token found');
    const response = await api.put(`/api/restaurant/branches/${id}`, branchData, {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data.branch;
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to update branch');
  }
}

/**
 * Delete a branch
 */
export async function deleteRestaurantBranch(id) {
  try {
    const token = localStorage.getItem('restaurantToken');
    if (!token) throw new Error('No token found');
    await api.delete(`/api/restaurant/branches/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    });
  } catch (error) {
    throw new Error(error.response?.data?.error || 'Failed to delete branch');
  }
}