const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:3000/api'

export async function processMenuImages(formData) {
  try {
    const response = await fetch(`${API_BASE_URL}/menu-extractor/process`, {
      method: 'POST',
      body: formData
    })

    const contentType = response.headers.get('content-type')
    
    if (!response.ok) {
      // Check if response is JSON
      if (contentType && contentType.includes('application/json')) {
        const error = await response.json()
        return { success: false, error: error.error || error.message || 'Unknown error' }
      } else {
        // Response is HTML (error page)
        const text = await response.text()
        console.error('Server returned HTML instead of JSON:', text.substring(0, 200))
        return { 
          success: false, 
          error: `Server error (${response.status}): ${response.statusText}. Check if backend is running and endpoint exists.` 
        }
      }
    }

    // Check if response is JSON before parsing
    if (!contentType || !contentType.includes('application/json')) {
      const text = await response.text()
      console.error('Unexpected response type:', contentType, text.substring(0, 200))
      return { 
        success: false, 
        error: 'Server returned non-JSON response. Check backend logs.' 
      }
    }

    const data = await response.json()
    return { success: true, ...data }
  } catch (error) {
    console.error('API Error:', error)
    return { success: false, error: error.message || 'Network error. Check if backend is running.' }
  }
}

export async function createRestaurantWithProducts({ restaurant, products }) {
  try {
    const response = await fetch(`${API_BASE_URL}/menu-extractor/create`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        restaurant,
        products
      })
    })

    const contentType = response.headers.get('content-type')
    
    if (!response.ok) {
      // Check if response is JSON
      if (contentType && contentType.includes('application/json')) {
        const error = await response.json()
        return { success: false, error: error.error || error.message || 'Unknown error' }
      } else {
        // Response is HTML (error page)
        const text = await response.text()
        console.error('Server returned HTML instead of JSON:', text.substring(0, 200))
        return { 
          success: false, 
          error: `Server error (${response.status}): ${response.statusText}. Check backend logs.` 
        }
      }
    }

    // Check if response is JSON before parsing
    if (!contentType || !contentType.includes('application/json')) {
      const text = await response.text()
      console.error('Unexpected response type:', contentType, text.substring(0, 200))
      return { 
        success: false, 
        error: 'Server returned non-JSON response. Check backend logs.' 
      }
    }

    const data = await response.json()
    return { success: true, ...data }
  } catch (error) {
    console.error('API Error:', error)
    return { success: false, error: error.message || 'Network error. Check if backend is running.' }
  }
}

/** Get all restaurants (for "Add to existing" mode). Pass { all: true } to include unpublished. */
export async function getRestaurants(opts = {}) {
  try {
    const qs = opts.all ? '?all=true' : ''
    const response = await fetch(`${API_BASE_URL}/websites${qs}`)
    const contentType = response.headers.get('content-type')
    if (!response.ok) {
      if (contentType && contentType.includes('application/json')) {
        const err = await response.json()
        return { success: false, error: err.error || err.message }
      }
      return { success: false, error: `Server error (${response.status})` }
    }
    if (!contentType || !contentType.includes('application/json')) {
      return { success: false, error: 'Invalid response' }
    }
    const data = await response.json()
    return { success: true, websites: data.websites || [] }
  } catch (error) {
    return { success: false, error: error.message }
  }
}

/** Add products to an existing restaurant */
export async function addProductsToRestaurant({ website_id, products }) {
  try {
    const response = await fetch(`${API_BASE_URL}/menu-extractor/add-products`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ website_id, products })
    })
    const contentType = response.headers.get('content-type')
    if (!response.ok) {
      if (contentType && contentType.includes('application/json')) {
        const err = await response.json()
        return { success: false, error: err.error || err.message }
      }
      return { success: false, error: `Server error (${response.status})` }
    }
    if (!contentType || !contentType.includes('application/json')) {
      return { success: false, error: 'Invalid response' }
    }
    const data = await response.json()
    return { success: true, ...data }
  } catch (error) {
    return { success: false, error: error.message }
  }
}
