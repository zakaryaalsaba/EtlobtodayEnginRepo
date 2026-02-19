import { get } from './client'

export function fetchProductsByStore(websiteId) {
  return get(`/api/products/website/${websiteId}`).then((r) => r.products || [])
}

export function fetchProduct(id) {
  return get(`/api/products/${id}`).then((r) => r.product)
}

export function fetchProductAddons(productId) {
  return get(`/api/products/${productId}/addons`).then((r) => ({
    addons: r.addons || [],
    addon_required: r.addon_required ?? false,
    addon_required_min: r.addon_required_min ?? null
  }))
}
