import * as api from "./api";

function okResponse(body = {}) {
  global.fetch = vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    text: vi.fn().mockResolvedValue(JSON.stringify(body))
  });
}

describe("api wrapper coverage", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(console, "log").mockImplementation(() => {});
    vi.spyOn(console, "error").mockImplementation(() => {});
    okResponse({});
  });

  it("calls all customer/order/tracking wrappers with correct routes", async () => {
    await api.getProfile("t");
    await api.updateProfile("t", { name: "A" });
    await api.updatePassword("t", { currentPassword: "Current@123", newPassword: "NewPass@123" });
    await api.getCategories("t");
    await api.getRestaurant("t", 9);
    await api.getMenu("t", 9);
    await api.getReviews("t", 9);
    await api.getCart("t");
    await api.addCartItem("t", { menuItemId: 1, quantity: 2 });
    await api.updateCartItem("t", 11, { quantity: 3 });
    await api.removeCartItem("t", 11);
    await api.getOrders("t");
    await api.checkout("t", { paymentMethod: "COD" });
    await api.getOrderTracking("t", "OD-1");

    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:8081/api/v1/auth/profile",
      expect.objectContaining({ headers: expect.objectContaining({ Authorization: "Bearer t" }) })
    );
    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:8081/api/v1/auth/password",
      expect.objectContaining({ method: "PUT" })
    );
    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:9090/api/v1/orders/checkout",
      expect.objectContaining({ method: "POST" })
    );
    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:9090/api/v1/tracking/orders/OD-1",
      expect.objectContaining({ headers: expect.objectContaining({ Authorization: "Bearer t" }) })
    );
  });

  it("calls delivery agent wrappers with expected methods", async () => {
    await api.getDeliveryAgents("t");
    await api.getCurrentDeliveryAgent("t");
    await api.getAssignedOrders("t", "AGT-1");
    await api.updateDeliveryAgentAvailability("t", "AGT-1", false);
    await api.updateAgentDeliveryStatus("t", "OD-2", "PICKED_UP");
    await api.acceptAssignedOrder("t", "AGT-1", "OD-2");
    await api.markAssignedOrderPickedUp("t", "AGT-1", "OD-2");
    await api.markAssignedOrderDelivered("t", "AGT-1", "OD-2");
    await api.getAgentEarnings("t", "AGT-1");
    await api.registerDeliveryAgent({
      name: "A",
      phone: "9999999999",
      vehicleType: "Bike",
      vehicleNumber: "MH12AA1111"
    });

    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:9090/api/v1/agents/AGT-1/availability",
      expect.objectContaining({ method: "PUT" })
    );
    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:9090/api/v1/orders/OD-2/delivery-status",
      expect.objectContaining({ method: "PUT" })
    );
  });

  it("calls payment wrappers with expected payload structure", async () => {
    await api.createRazorpayOrder("t", { amount: 100, currency: "INR", orderReference: "OD-3" });
    await api.verifyRazorpayPayment("t", {
      orderReference: "OD-3",
      razorpayOrderId: "order_123",
      razorpayPaymentId: "pay_123",
      razorpaySignature: "sig"
    });

    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:9090/api/v1/payments/razorpay/orders",
      expect.objectContaining({ method: "POST" })
    );
    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:9090/api/v1/payments/razorpay/verify",
      expect.objectContaining({ method: "POST" })
    );
  });

  it("calls restaurant owner wrappers including multipart upload contracts", async () => {
    const image = new File(["img"], "shop.jpg", { type: "image/jpeg" });

    await api.registerRestaurant("t", {
      restaurantName: "Food Point",
      cuisineType: "Indian",
      address: "Bhopal",
      imageFile: image
    });
    await api.getRestaurantOwnerProfile("t");
    await api.createMenuCategory("t", { restaurantId: 10, name: "Starters" });
    await api.getMenuCategories("t", 10);
    await api.addMenuItem("t", {
      restaurantId: 10,
      categoryId: 2,
      itemName: "Paneer",
      description: "",
      price: 199,
      isAvailable: true,
      vegetarian: true,
      bestseller: false,
      imageFile: image
    });
    await api.updateMenuItem("t", 1, {
      restaurantId: 10,
      categoryId: 2,
      itemName: "Paneer",
      description: "",
      price: 249,
      isAvailable: false,
      vegetarian: true,
      bestseller: true,
      imageFile: null
    });
    await api.deleteMenuItem("t", 1);
    await api.getRestaurantOwnerOrders("t");
    await api.updateOrderStatus("t", "OD-4", { deliveryStatus: "READY" });
    await api.getRestaurantOwnerAnalytics("t", { period: "weekly" });
    await api.getRestaurantOwnerReviews("t");

    const registerCall = fetch.mock.calls.find(([url]) => url.includes("/api/v1/restaurant-owner/register"));
    const addItemCall = fetch.mock.calls.find(([url]) => url.includes("/api/v1/menu/items") && !url.includes("/availability"));
    expect(registerCall[1].body).toBeInstanceOf(FormData);
    expect(addItemCall[1].body).toBeInstanceOf(FormData);
  });

  it("calls admin wrappers with admin base URL", async () => {
    await api.getAdminDashboard("admin-token");
    await api.deleteAdminUser("admin-token", "u-1");
    await api.getAdminRestaurants("admin-token");
    await api.getAdminOrders("admin-token");

    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:9090/api/v1/admin/dashboard",
      expect.objectContaining({ headers: expect.objectContaining({ Authorization: "Bearer admin-token" }) })
    );
    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:9090/api/v1/admin/users/u-1",
      expect.objectContaining({ method: "DELETE" })
    );
  });
});
