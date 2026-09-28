const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'
// The backend's root origin (without the /api/v1 prefix) — used to resolve
// image URLs returned by the local-storage upload fallback, which come back
// as root-relative paths like "/uploads/abc.png" rather than full URLs.
const API_ORIGIN = API_BASE.replace(/\/api\/v1\/?$/, '')

// Turns a possibly-relative media URL (e.g. "/uploads/abc.png") into an
// absolute one pointing at the backend, while leaving already-absolute URLs
// (http(s)://..., including third-party/Supabase URLs) untouched.
export function resolveMediaUrl(url) {
  if (!url) return url
  if (/^https?:\/\//i.test(url) || url.startsWith('data:')) return url
  return `${API_ORIGIN}${url.startsWith('/') ? '' : '/'}${url}`
}

const ACCESS_TOKEN_KEY = 'trape_access_token'
const REFRESH_TOKEN_KEY = 'trape_refresh_token'
const GUEST_TOKEN_KEY = 'trape_guest_token'
const USER_KEY = 'trape_user'

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function getGuestToken() {
  let token = localStorage.getItem(GUEST_TOKEN_KEY)
  if (!token) {
    token = crypto.randomUUID()
    localStorage.setItem(GUEST_TOKEN_KEY, token)
  }
  return token
}

export function getStoredUser() {
  const raw = localStorage.getItem(USER_KEY)
  return raw ? JSON.parse(raw) : null
}

export function storeSession({ accessToken, refreshToken, user }) {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  // Once logged in, the guest cart gets merged server-side — drop the guest token
  // so subsequent requests use the user's own cart.
  localStorage.removeItem(GUEST_TOKEN_KEY)
}

export function clearSession() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

async function request(path, { method = 'GET', body, auth = true, guestCart = false } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (auth) {
    const token = getAccessToken()
    if (token) headers['Authorization'] = `Bearer ${token}`
  }
  if (guestCart && !getAccessToken()) {
    headers['X-Guest-Token'] = getGuestToken()
  }

  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })

  const json = await res.json().catch(() => null)

  if (!res.ok) {
    const message = json?.error?.message || `Request failed (${res.status})`
    const err = new Error(message)
    err.code = json?.error?.code
    err.status = res.status
    throw err
  }
  return json?.data
}

// Separate helper for multipart/form-data uploads (image files from the
// admin's own device) — these must NOT set a JSON Content-Type header.
async function uploadRequest(path, formData) {
  const headers = {}
  const token = getAccessToken()
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    headers,
    body: formData,
  })

  const json = await res.json().catch(() => null)

  if (!res.ok) {
    const message = json?.error?.message || `Upload failed (${res.status})`
    const err = new Error(message)
    err.code = json?.error?.code
    err.status = res.status
    throw err
  }
  return json?.data
}

export const api = {
  // Auth
  register: (payload) => request('/auth/register', { method: 'POST', body: payload, auth: false }),
  login: (payload) => request('/auth/login', { method: 'POST', body: payload, auth: false, guestCart: true }),
  logout: (refreshToken) => request('/auth/logout', { method: 'POST', body: { refreshToken }, auth: false }),

  // Catalog
  listCategories: () => request('/categories', { auth: false }),
  listProducts: (params = {}) => {
    const qs = new URLSearchParams(Object.entries(params).filter(([, v]) => v !== undefined && v !== ''))
    return request(`/products?${qs.toString()}`, { auth: false })
  },
  getProduct: (slug) => request(`/products/${slug}`, { auth: false }),
  listReviews: (productId) => request(`/products/${productId}/reviews`, { auth: false }),
  addReview: (productId, payload) => request(`/products/${productId}/reviews`, { method: 'POST', body: payload }),

  // Cart (works for guest + logged-in)
  getCart: () => request('/cart', { auth: true, guestCart: true }),
  addToCart: (payload) => request('/cart/items', { method: 'POST', body: payload, guestCart: true }),
  updateCartItem: (itemId, payload) => request(`/cart/items/${itemId}`, { method: 'PATCH', body: payload, guestCart: true }),
  removeCartItem: (itemId) => request(`/cart/items/${itemId}`, { method: 'DELETE', guestCart: true }),
  applyCoupon: (code) => request('/cart/coupon', { method: 'POST', body: { code }, guestCart: true }),
  removeCoupon: () => request('/cart/coupon', { method: 'DELETE', guestCart: true }),

  // Profile / addresses
  getProfile: () => request('/me'),
  updateProfile: (payload) => request('/me', { method: 'PATCH', body: payload }),
  listAddresses: () => request('/me/addresses'),
  addAddress: (payload) => request('/me/addresses', { method: 'POST', body: payload }),
  updateAddress: (id, payload) => request(`/me/addresses/${id}`, { method: 'PATCH', body: payload }),
  deleteAddress: (id) => request(`/me/addresses/${id}`, { method: 'DELETE' }),

  // Wishlist
  getWishlist: () => request('/me/wishlist'),
  addToWishlist: (productId) => request(`/me/wishlist/${productId}`, { method: 'PUT' }),
  removeFromWishlist: (productId) => request(`/me/wishlist/${productId}`, { method: 'DELETE' }),

  // Orders / checkout
  checkout: (shippingAddressId) => request('/orders/checkout', { method: 'POST', body: { shippingAddressId } }),
  listOrders: () => request('/orders'),
  getOrder: (id) => request(`/orders/${id}`),
  cancelOrder: (id) => request(`/orders/${id}/cancel`, { method: 'POST' }),

  // Payment
  verifyPayment: (payload) => request('/payments/verify', { method: 'POST', body: payload }),

  // Admin
  adminCreateProduct: (payload) => request('/admin/products', { method: 'POST', body: payload }),
  adminUpdateProduct: (id, payload) => request(`/admin/products/${id}`, { method: 'PUT', body: payload }),
  adminArchiveProduct: (id) => request(`/admin/products/${id}`, { method: 'DELETE' }),
  adminCreateCategory: (payload) => request('/admin/categories', { method: 'POST', body: payload }),
  adminUpdateCategory: (id, payload) => request(`/admin/categories/${id}`, { method: 'PUT', body: payload }),
  adminDeleteCategory: (id) => request(`/admin/categories/${id}`, { method: 'DELETE' }),
  adminUploadImage: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return uploadRequest('/admin/media/upload', formData)
  },
  adminUploadImages: (files) => {
    const formData = new FormData()
    files.forEach((file) => formData.append('files', file))
    return uploadRequest('/admin/media/upload-multiple', formData)
  },
  adminSetStock: (payload) => request('/admin/inventory', { method: 'PUT', body: payload }),
  adminListOrders: (status) => request(`/admin/orders${status ? `?status=${status}` : ''}`),
  adminUpdateOrderStatus: (id, payload) => request(`/admin/orders/${id}/status`, { method: 'PATCH', body: payload }),
  adminListCoupons: () => request('/admin/coupons'),
  adminCreateCoupon: (payload) => request('/admin/coupons', { method: 'POST', body: payload }),
}
