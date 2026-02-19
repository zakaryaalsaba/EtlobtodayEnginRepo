const baseURL = import.meta.env.VITE_API_BASE_URL || ''

function getAuthHeader() {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

export async function api(method, path, data = null) {
  const url = path.startsWith('http') ? path : `${baseURL}${path}`
  const options = {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeader()
    }
  }
  if (data && (method === 'POST' || method === 'PUT' || method === 'PATCH')) {
    options.body = JSON.stringify(data)
  }
  const res = await fetch(url, options)
  const text = await res.text()
  let json
  try {
    json = text ? JSON.parse(text) : null
  } catch {
    throw new Error(text || res.statusText)
  }
  if (!res.ok) {
    const err = new Error(json?.error || json?.message || res.statusText)
    err.status = res.status
    err.body = json
    throw err
  }
  return json
}

export const get = (path) => api('GET', path)
export const post = (path, data) => api('POST', path, data)
export const put = (path, data) => api('PUT', path, data)
export const del = (path) => api('DELETE', path)
