<template>
  <div class="min-h-screen bg-gradient-to-br from-indigo-50 via-white to-purple-50 flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full space-y-8">
      <div class="text-center">
        <h2 class="text-4xl font-extrabold text-gray-900 mb-2">Restaurant Dashboard</h2>
        <p class="text-gray-600">Sign in to manage your restaurant</p>
      </div>
      
      <div class="bg-white rounded-xl shadow-2xl p-8">
        <form @submit.prevent="handleLogin" class="space-y-6">
          <div v-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
            {{ error }}
          </div>
          
          <div>
            <label for="email" class="block text-sm font-semibold text-gray-700 mb-2">
              Email Address
            </label>
            <input
              id="email"
              v-model="email"
              type="email"
              required
              autocomplete="email"
              class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              placeholder="admin@restaurant.com"
            />
          </div>
          
          <div>
            <label for="password" class="block text-sm font-semibold text-gray-700 mb-2">
              Password
            </label>
            <input
              id="password"
              v-model="password"
              type="password"
              required
              autocomplete="current-password"
              class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              placeholder="Enter your password"
            />
          </div>
          
          <button
            type="submit"
            :disabled="loading"
            class="w-full px-6 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <span v-if="loading">Signing in...</span>
            <span v-else>Sign In</span>
          </button>
        </form>
        
        <div class="mt-6 text-center">
          <p class="text-sm text-gray-600">
            Need help? Contact your platform administrator
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { adminLogin } from '../services/api.js';

const router = useRouter();
const email = ref('');
const password = ref('');
const loading = ref(false);
const error = ref('');

const handleLogin = async () => {
  error.value = '';
  loading.value = true;
  
  try {
    const response = await adminLogin(email.value, password.value);
    
    // Store access token, refresh token, and info (refresh token used to get new access token when expired)
    localStorage.setItem('restaurantToken', response.token);
    if (response.refreshToken) localStorage.setItem('restaurantRefreshToken', response.refreshToken);
    localStorage.setItem('restaurantInfo', JSON.stringify(response.admin));
    
    // Redirect to restaurant dashboard
    router.push('/restaurant/dashboard');
  } catch (err) {
    error.value = err.message || 'Invalid email or password';
  } finally {
    loading.value = false;
  }
};
</script>

