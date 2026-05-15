import { API_BASE_URL, AUTH_API_BASE_URL, ADMIN_API_BASE_URL } from "../config";

async function request(path, options = {}) {
  const baseUrl = options.baseUrl || API_BASE_URL;
  const normalizedBaseUrl = (baseUrl || "").replace(/\/+$/, "");
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  const requestUrl = `${normalizedBaseUrl}${normalizedPath}`;
  const token = options.token;
  const isFormData = typeof FormData !== "undefined" && options.body instanceof FormData;
  const requestHeaders = {
    ...(isFormData ? {} : { "Content-Type": "application/json" }),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers || {})
  };
  if (isFormData && requestHeaders["Content-Type"]) {
    delete requestHeaders["Content-Type"];
  }

  console.log(`[REQUEST] ${options.method || 'GET'} ${requestUrl}`);
  console.log(`[AUTH] Token present: ${!!token}`);
  if (token) {
    console.log(`[AUTH] Token length: ${token.length}`);
    console.log(`[AUTH] Token start: ${token.substring(0, 20)}...`);
  }

  const response = await fetch(requestUrl, {
    cache: "no-store",
    headers: requestHeaders,
    ...options
  });

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = { message: text };
    }
  }

  if (!response.ok) {
    const errorMessage = data?.message || data?.error || text || "Request failed";
    console.error(`[${response.status}] ${path}:`, errorMessage);
    if (response.status === 403) {
      console.error("[FORBIDDEN] Access denied - check token validity and user role");
    }
    throw new Error(errorMessage);
  }

  return data;
}

function requestAuth(path, options = {}) {
  return request(path, {
    ...options,
    baseUrl: AUTH_API_BASE_URL
  });
}

function requestAdmin(path, options = {}) {
  return request(path, {
    ...options,
    baseUrl: ADMIN_API_BASE_URL
  });
}

export function login(payload) {
  return requestAuth("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function register(payload) {
  return requestAuth("/api/v1/auth/register", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function getProfile(token) {
  return requestAuth("/api/v1/auth/profile", { token });
}

export function updateProfile(token, payload) {
  return requestAuth("/api/v1/auth/profile", {
    method: "PUT",
    token,
    body: JSON.stringify(payload)
  });
}

export function updatePassword(token, payload) {
  return requestAuth("/api/v1/auth/password", {
    method: "PUT",
    token,
    body: JSON.stringify(payload)
  });
}

export function getCategories(token) {
  return request("/api/v1/restaurants/categories", { token });
}

export function getRestaurants(token, params = {}) {
  const query = new URLSearchParams();
  if (params.category) {
    query.set("category", params.category);
  }
  if (params.search) {
    query.set("search", params.search);
  }
  return request(`/api/v1/restaurants${query.toString() ? `?${query.toString()}` : ""}`, { token });
}

export function getRestaurant(token, restaurantId) {
  return request(`/api/v1/restaurants/${restaurantId}`, { token });
}

export function getMenu(token, restaurantId) {
  return request(`/api/v1/menu/restaurants/${restaurantId}`, { token });
}

export function getReviews(token, restaurantId) {
  return request(`/api/v1/reviews/restaurants/${restaurantId}`, { token });
}

export function createReview(token, payload) {
  return request("/api/v1/reviews", {
    method: "POST",
    token,
    body: JSON.stringify(payload)
  });
}

export function getCart(token) {
  return request("/api/v1/cart", { token });
}

export function addCartItem(token, payload) {
  return request("/api/v1/cart/items", {
    method: "POST",
    token,
    body: JSON.stringify(payload)
  });
}

export function updateCartItem(token, itemId, payload) {
  return request(`/api/v1/cart/items/${itemId}`, {
    method: "PATCH",
    token,
    body: JSON.stringify(payload)
  });
}

export function removeCartItem(token, itemId) {
  return request(`/api/v1/cart/items/${itemId}`, {
    method: "DELETE",
    token
  });
}

export function getOrders(token) {
  return request("/api/v1/orders", { token });
}

export function getDeliveryAgents(token) {
  return request("/api/v1/agents", { token });
}

export function getCurrentDeliveryAgent(token) {
  return request("/api/v1/agents/me", { token });
}

export function getAssignedOrders(token, agentId) {
  return request(`/api/v1/agents/${agentId}/orders`, { token });
}

export function updateDeliveryAgentAvailability(token, agentId, online) {
  return request(`/api/v1/agents/${agentId}/availability`, {
    method: "PUT",
    token,
    body: JSON.stringify({ online: Boolean(online) })
  });
}

export function updateAgentDeliveryStatus(token, orderReference, deliveryStatus) {
  return request(`/api/v1/orders/${orderReference}/delivery-status`, {
    method: "PUT",
    token,
    body: JSON.stringify({ deliveryStatus })
  });
}

export function acceptAssignedOrder(token, agentId, orderReference) {
  return request(`/api/v1/agents/${agentId}/orders/${orderReference}/accept`, {
    method: "PUT",
    token
  });
}

export function markAssignedOrderPickedUp(token, agentId, orderReference) {
  return request(`/api/v1/agents/${agentId}/orders/${orderReference}/picked-up`, {
    method: "PUT",
    token
  });
}

export function markAssignedOrderDelivered(token, agentId, orderReference) {
  return request(`/api/v1/agents/${agentId}/orders/${orderReference}/delivered`, {
    method: "PUT",
    token
  });
}

export function getAgentEarnings(token, agentId) {
  return request(`/api/v1/agents/${agentId}/earnings`, { token });
}

export function checkout(token, payload) {
  return request("/api/v1/orders/checkout", {
    method: "POST",
    token,
    body: JSON.stringify(payload)
  });
}

export function registerDeliveryAgent(payload) {
  return request("/api/v1/agents", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function createRazorpayOrder(token, payload) {
  return request("/api/v1/payments/razorpay/orders", {
    method: "POST",
    token,
    body: JSON.stringify(payload)
  });
}

export function verifyRazorpayPayment(token, payload) {
  return request("/api/v1/payments/razorpay/verify", {
    method: "POST",
    token,
    body: JSON.stringify(payload)
  });
}

// Restaurant Owner APIs
export function registerRestaurant(token, payload) {
  const formData = new FormData();
  formData.append("restaurantName", payload.restaurantName);
  formData.append("cuisineType", payload.cuisineType);
  formData.append("address", payload.address);
  formData.append("imageFile", payload.imageFile);

  return request("/api/v1/restaurant-owner/register", {
    method: "POST",
    token,
    body: formData
  });
}

export function getRestaurantOwnerProfile(token) {
   return request("/api/v1/restaurant-owner/profile", { token });
}

export function createMenuCategory(token, payload) {
  return request("/api/v1/menu/categories", {
    method: "POST",
    token,
    body: JSON.stringify(payload)
  });
}

export function getMenuCategories(token, restaurantId) {
  return request(`/api/v1/menu/restaurants/${restaurantId}/categories`, { token });
}

export function addMenuItem(token, payload) {
  const formData = new FormData();
  formData.append("restaurantId", String(payload.restaurantId));
  if (payload.categoryId !== undefined && payload.categoryId !== null && payload.categoryId !== "") {
    formData.append("categoryId", String(payload.categoryId));
  }
  formData.append("itemName", payload.itemName);
  formData.append("description", payload.description || "");
  formData.append("price", String(payload.price));
  formData.append("isAvailable", String(payload.isAvailable ?? true));
  formData.append("vegetarian", String(payload.vegetarian ?? false));
  formData.append("bestseller", String(payload.bestseller ?? false));
  formData.append("imageFile", payload.imageFile);

  return request("/api/v1/menu/items", {
    method: "POST",
    token,
    body: formData
  });
}

export function updateMenuItem(token, itemId, payload) {
  const formData = new FormData();
  formData.append("restaurantId", String(payload.restaurantId));
  if (payload.categoryId !== undefined && payload.categoryId !== null && payload.categoryId !== "") {
    formData.append("categoryId", String(payload.categoryId));
  }
  formData.append("itemName", payload.itemName);
  formData.append("description", payload.description || "");
  formData.append("price", String(payload.price));
  formData.append("isAvailable", String(payload.isAvailable ?? true));
  formData.append("vegetarian", String(payload.vegetarian ?? false));
  formData.append("bestseller", String(payload.bestseller ?? false));
  if (payload.imageFile) {
    formData.append("imageFile", payload.imageFile);
  }

  return request(`/api/v1/menu/items/${itemId}`, {
    method: "PUT",
    token,
    body: formData
  });
}

export function deleteMenuItem(token, itemId) {
  return request(`/api/v1/menu/items/${itemId}`, {
    method: "DELETE",
    token
  });
}

export function toggleMenuItemAvailability(token, itemId, payload) {
  return request(`/api/v1/menu/items/${itemId}/availability`, {
    method: "PATCH",
    token,
    body: JSON.stringify(payload)
  });
}

export function getRestaurantOwnerOrders(token) {
  return request("/api/v1/restaurant-owner/orders", { token });
}

export function updateOrderStatus(token, orderId, payload) {
  return request(`/api/v1/restaurant-owner/orders/${orderId}`, {
    method: "PATCH",
    token,
    body: JSON.stringify(payload)
  });
}

export function getRestaurantOwnerAnalytics(token, params = {}) {
  const query = new URLSearchParams();
  if (params.period) {
    query.set("period", params.period);
  }
  return request(`/api/v1/restaurant-owner/analytics${query.toString() ? `?${query.toString()}` : ""}`, { token });
}

export function getRestaurantOwnerReviews(token) {
  return request("/api/v1/restaurant-owner/reviews", { token });
}

// Tracking APIs
export function getOrderTracking(token, orderReference) {
  return request(`/api/v1/tracking/orders/${orderReference}`, { token });
}

// Admin APIs
export function getAdminDashboard(token) {
  return requestAdmin("/api/v1/admin/dashboard", { token });
}

export function getAdminUsers(token) {
  return requestAdmin("/api/v1/admin/users", { token }).then((response) => {
    if (Array.isArray(response)) {
      return response;
    }
    if (Array.isArray(response?.users)) {
      return response.users;
    }
    if (Array.isArray(response?.content)) {
      return response.content;
    }
    if (Array.isArray(response?.data)) {
      return response.data;
    }
    return [];
  });
}

export function getAdminOwners(token) {
  return requestAdmin("/api/v1/admin/owners", { token }).then((response) => {
    if (Array.isArray(response)) {
      return response;
    }
    if (Array.isArray(response?.owners)) {
      return response.owners;
    }
    if (Array.isArray(response?.content)) {
      return response.content;
    }
    if (Array.isArray(response?.data)) {
      return response.data;
    }
    return [];
  });
}

export function deleteAdminUser(token, userId) {
  return requestAdmin(`/api/v1/admin/users/${userId}`, {
    method: "DELETE",
    token
  });
}

export function getAdminRestaurants(token) {
  return requestAdmin("/api/v1/admin/restaurants", { token });
}

export function deleteAdminRestaurant(token, restaurantId) {
  return requestAdmin(`/api/v1/admin/restaurants/${restaurantId}`, {
    method: "DELETE",
    token
  });
}

export function getAdminOrders(token) {
  return requestAdmin("/api/v1/admin/orders", { token });
}

export function updateAdminOrderStatus(token, orderId, status) {
  return requestAdmin(`/api/v1/admin/orders/${orderId}/status`, {
    method: "PUT",
    token,
    body: JSON.stringify({ status })
  });
}
