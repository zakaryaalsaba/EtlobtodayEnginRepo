<template>
  <div class="min-h-screen bg-gradient-to-br from-indigo-50 via-white to-purple-50 flex items-center justify-center px-4">
    <div class="max-w-md w-full">
      <div class="bg-white rounded-2xl shadow-xl p-8">
        <div class="text-center mb-8">
          <h1 class="text-3xl font-bold text-gray-900 mb-2">Super Admin Login</h1>
          <p class="text-gray-600">Platform Administrator Access</p>
        </div>

        <form @submit.prevent="handleLogin" class="space-y-6">
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-2">
              Email Address
            </label>
            <input
              v-model="formData.email"
              type="email"
              required
              class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              placeholder="admin@restaurantaai.com"
            />
          </div>

          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-2">
              Password
            </label>
            <input
              v-model="formData.password"
              type="password"
              required
              class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              placeholder="Enter your password"
            />
          </div>

          <div v-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
            {{ error }}
          </div>

          <button
            type="submit"
            :disabled="loading"
            class="w-full px-6 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <span v-if="loading">Logging in...</span>
            <span v-else>Login</span>
          </button>
        </form>

        <div class="mt-6 border-t border-gray-200 pt-6">
          <div class="text-center mb-4">
            <button
              type="button"
              @click="showCreate = !showCreate"
              class="text-sm text-indigo-600 hover:text-indigo-700 font-medium"
            >
              {{ showCreate ? 'Hide create super admin' : 'Create new super admin' }}
            </button>
          </div>

          <form
            v-if="showCreate"
            @submit.prevent="handleCreate"
            class="space-y-4 bg-indigo-50/60 border border-indigo-100 rounded-xl p-4"
          >
            <div class="text-sm text-gray-700 text-center mb-2">
              Use this form to create an additional super admin account.
            </div>
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-1">
                Name
              </label>
              <input
                v-model="createForm.name"
                type="text"
                required
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 text-sm"
              />
            </div>
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-1">
                Email
              </label>
              <input
                v-model="createForm.email"
                type="email"
                required
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 text-sm"
              />
            </div>
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-1">
                Password
              </label>
              <input
                v-model="createForm.password"
                type="password"
                required
                minlength="6"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 text-sm"
              />
            </div>

            <div v-if="createError" class="bg-red-50 border border-red-200 text-red-700 px-3 py-2 rounded-lg text-xs">
              {{ createError }}
            </div>
            <div v-if="createSuccess" class="bg-green-50 border border-green-200 text-green-700 px-3 py-2 rounded-lg text-xs">
              {{ createSuccess }}
            </div>

            <button
              type="submit"
              :disabled="createLoading"
              class="w-full px-4 py-2 bg-white text-indigo-700 border border-indigo-500 rounded-lg hover:bg-indigo-50 transition-colors font-semibold text-sm disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <span v-if="createLoading">Creating...</span>
              <span v-else>Create Super Admin</span>
            </button>
          </form>
        </div>

        <div class="mt-6 text-center">
          <router-link to="/" class="text-sm text-indigo-600 hover:text-indigo-700">
            ← Back to Home
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

const formData = ref({
  email: '',
  password: ''
});

const loading = ref(false);
const error = ref('');

const showCreate = ref(false);
const createForm = ref({
  name: '',
  email: '',
  password: ''
});
const createLoading = ref(false);
const createError = ref('');
const createSuccess = ref('');

const handleLogin = async () => {
  loading.value = true;
  error.value = '';

  try {
    const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000';
    const response = await fetch(`${API_BASE_URL}/api/super-admin/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(formData.value),
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
      const errorData = await response.json();
      throw new Error(errorData.error || 'Login failed');
    }

    const data = await response.json();
    
    // Store super admin token
    localStorage.setItem('superAdminToken', data.token);
    localStorage.setItem('superAdminInfo', JSON.stringify(data.superAdmin));
    
    // Redirect to builder
    router.push('/builder');
  } catch (err) {
    console.error('Login error:', err);
    error.value = err.message || 'Failed to login. Please try again.';
  } finally {
    loading.value = false;
  }
};

const handleCreate = async () => {
  createLoading.value = true;
  createError.value = '';
  createSuccess.value = '';

  try {
    const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000';
    const response = await fetch(`${API_BASE_URL}/api/super-admin/create`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(createForm.value),
    });

    const contentType = response.headers.get('content-type');
    if (!contentType || !contentType.includes('application/json')) {
      if (!response.ok) {
        throw new Error(`Server error: ${response.status} ${response.statusText}. Please check if the backend server is running.`);
      }
      throw new Error('Invalid response from server. Expected JSON but got HTML.');
    }

    const data = await response.json();

    if (!response.ok || !data.success) {
      throw new Error(data.error || 'Failed to create super admin');
    }

    createSuccess.value = 'Super admin created successfully. You can now log in with this email and password.';

    // Prefill login form with new account
    formData.value.email = createForm.value.email;
    formData.value.password = createForm.value.password;
  } catch (err) {
    console.error('Create super admin error:', err);
    createError.value = err.message || 'Failed to create super admin. Please try again.';
  } finally {
    createLoading.value = false;
  }
};
</script>

