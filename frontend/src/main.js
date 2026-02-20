import { createApp } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';
import App from './App.vue';
import './style.css';
import i18n from './i18n/index.js';
import LandingPage from './components/LandingPage.vue';
import WebsiteBuilder from './components/WebsiteBuilder.vue';
import WebsiteViewer from './components/WebsiteViewer.vue';
import Checkout from './components/Checkout.vue';
import OrderConfirmation from './components/OrderConfirmation.vue';
import OrderTracking from './components/OrderTracking.vue';
import AdminLogin from './components/AdminLogin.vue';
import AdminDashboard from './components/AdminDashboard.vue';
import RestaurantLogin from './components/RestaurantLogin.vue';
import RestaurantDashboard from './components/RestaurantDashboard.vue';
import SuperAdminLogin from './components/SuperAdminLogin.vue';
import DeliveryCompanyLogin from './components/DeliveryCompanyLogin.vue';
import DeliveryCompanyDashboard from './components/DeliveryCompanyDashboard.vue';

const routes = [
  { path: '/', component: LandingPage },
  { path: '/builder', component: WebsiteBuilder },
  { path: '/super-admin/login', component: SuperAdminLogin },
  { path: '/barcode/:code', component: WebsiteViewer, props: true },
  { path: '/website/:id', component: WebsiteViewer, props: true },
  { path: '/website/:id/checkout', component: Checkout, props: true },
  { path: '/website/:id/order/:orderNumber', component: OrderConfirmation, props: true },
  { path: '/website/:id/track', component: OrderTracking, props: true },
  { path: '/website/:id/track/:orderNumber', component: OrderTracking, props: true },
  { path: '/admin/login', component: AdminLogin },
  { path: '/admin/dashboard', component: AdminDashboard },
  { path: '/restaurant/login', component: RestaurantLogin },
  { path: '/restaurant/dashboard', component: RestaurantDashboard },
  { path: '/delivery-company/login', component: DeliveryCompanyLogin },
  { path: '/delivery-company/dashboard', component: DeliveryCompanyDashboard },
  // Catch-all route for domain-based access (must be last)
  { path: '/:pathMatch(.*)*', component: WebsiteViewer, props: true },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
});

// Route guards
router.beforeEach((to, from, next) => {
  // Super admin builder guard
  if (to.path === '/builder') {
    const token = localStorage.getItem('superAdminToken');
    if (!token) {
      next('/super-admin/login');
    } else {
      next();
    }
  }
  // Admin dashboard guard
  else if (to.path === '/admin/dashboard') {
    const token = localStorage.getItem('adminToken');
    if (!token) {
      next('/admin/login');
    } else {
      next();
    }
  }
  // Restaurant dashboard guard
  else if (to.path === '/restaurant/dashboard') {
    const token = localStorage.getItem('restaurantToken');
    if (!token) {
      next('/restaurant/login');
    } else {
      next();
    }
  }
  // Delivery company dashboard guard
  else if (to.path === '/delivery-company/dashboard') {
    const token = localStorage.getItem('deliveryCompanyToken');
    if (!token) {
      next('/delivery-company/login');
    } else {
      next();
    }
  } else {
    next();
  }
});

createApp(App).use(router).use(i18n).mount('#app');

