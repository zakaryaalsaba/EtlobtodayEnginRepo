import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layouts/MainLayout.vue'

const routes = [
  {
    path: '/',
    component: Layout,
    children: [
      { path: '', name: 'Stores', component: () => import('@/views/StoresView.vue'), meta: { title: 'Stores' } },
      { path: 'store/:id', name: 'StoreDetail', component: () => import('@/views/StoreDetailView.vue'), meta: { title: 'Store' } },
      { path: 'product/:id', name: 'ProductDetail', component: () => import('@/views/ProductDetailView.vue'), meta: { title: 'Product' } },
      { path: 'cart', name: 'Cart', component: () => import('@/views/CartView.vue'), meta: { title: 'Cart' } },
      { path: 'checkout', name: 'Checkout', component: () => import('@/views/CheckoutView.vue'), meta: { title: 'Checkout' } },
      { path: 'place-order', name: 'PlaceOrder', component: () => import('@/views/PlaceOrderView.vue'), meta: { title: 'Place Order' } },
      { path: 'orders', name: 'Orders', component: () => import('@/views/OrdersView.vue'), meta: { title: 'My Orders' } },
      { path: 'order/:orderNumber', name: 'OrderTrack', component: () => import('@/views/OrderTrackView.vue'), meta: { title: 'Order Tracking' } }
    ]
  },
  { path: '/login', name: 'Login', component: () => import('@/views/LoginView.vue'), meta: { title: 'Login', layout: 'minimal' } },
  { path: '/register', name: 'Register', component: () => import('@/views/RegisterView.vue'), meta: { title: 'Register', layout: 'minimal' } }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

export default router
