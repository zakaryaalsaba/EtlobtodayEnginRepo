import { get } from './client'

export function fetchStores(openNow = false) {
  const q = openNow ? '?open_now=true' : ''
  return get(`/api/websites${q}`).then((r) => r.websites || [])
}

export function fetchStore(id) {
  return get(`/api/websites/${id}`).then((r) => r.website)
}
