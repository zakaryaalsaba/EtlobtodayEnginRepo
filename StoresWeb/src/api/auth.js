import { get, post } from './client'

export function register(data) {
  return post('/api/auth/register', data).then((r) => ({
    customer: r.customer,
    token: r.token
  }))
}

export function login(email, password) {
  return post('/api/auth/login', { email, password }).then((r) => ({
    customer: r.customer,
    token: r.token
  }))
}

export function me() {
  return get('/api/auth/me').then((r) => r.customer)
}
