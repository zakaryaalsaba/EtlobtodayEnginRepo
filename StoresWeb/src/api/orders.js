import { get, post } from './client'

export function createOrder(payload) {
  return post('/api/orders', payload).then((r) => r.order)
}

export function fetchOrderByNumber(orderNumber) {
  return get(`/api/orders/${orderNumber}`).then((r) => r.order)
}

export function fetchCustomerOrders(customerId) {
  return get(`/api/customers/${customerId}/orders`).then((r) => r.orders || [])
}
