import {
  addMenuItem,
  createReview,
  deleteAdminRestaurant,
  getAdminOwners,
  getAdminUsers,
  getRestaurants,
  login,
  register,
  toggleMenuItemAvailability,
  updateAdminOrderStatus
} from "./api";

function mockFetchResponse({ ok = true, status = 200, body = {} } = {}) {
  global.fetch = vi.fn().mockResolvedValue({
    ok,
    status,
    text: vi.fn().mockResolvedValue(typeof body === "string" ? body : JSON.stringify(body))
  });
}

describe("api client", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(console, "log").mockImplementation(() => {});
    vi.spyOn(console, "error").mockImplementation(() => {});
  });

  it("sends login to auth service with JSON body and bearer header when token exists", async () => {
    mockFetchResponse({ body: { token: "jwt", user: { email: "u@q.com" } } });

    await login({ email: "u@q.com", password: "pass123" });

    expect(fetch).toHaveBeenCalledTimes(1);
    const [url, options] = fetch.mock.calls[0];
    expect(url).toBe("http://localhost:8081/api/v1/auth/login");
    expect(options.method).toBe("POST");
    expect(options.headers["Content-Type"]).toBe("application/json");
    expect(options.body).toBe(JSON.stringify({ email: "u@q.com", password: "pass123" }));
  });

  it("adds category/search query params for restaurants", async () => {
    mockFetchResponse({ body: [] });

    await getRestaurants("token-123", { category: "Pizza", search: "stone oven" });

    const [url, options] = fetch.mock.calls[0];
    expect(url).toContain("http://localhost:9090/api/v1/restaurants?");
    expect(url).toContain("category=Pizza");
    expect(url).toContain("search=stone+oven");
    expect(options.headers.Authorization).toBe("Bearer token-123");
  });

  it("creates review with expected payload contract", async () => {
    mockFetchResponse({ status: 201, body: { id: 11, rating: 5 } });
    await createReview("token-x", { restaurantId: 2, rating: 5, comment: "Great" });

    const [url, options] = fetch.mock.calls[0];
    expect(url).toBe("http://localhost:9090/api/v1/reviews");
    expect(options.method).toBe("POST");
    expect(JSON.parse(options.body)).toEqual({
      restaurantId: 2,
      rating: 5,
      comment: "Great"
    });
  });

  it("sends form-data for add menu item and does not force JSON content-type", async () => {
    mockFetchResponse({ status: 201, body: { id: 4 } });
    const file = new File(["img"], "menu.jpg", { type: "image/jpeg" });

    await addMenuItem("owner-token", {
      restaurantId: 101,
      categoryId: 9,
      itemName: "Paneer Tikka",
      description: "Smoky starter",
      price: 219,
      isAvailable: true,
      vegetarian: true,
      bestseller: false,
      imageFile: file
    });

    const [url, options] = fetch.mock.calls[0];
    expect(url).toBe("http://localhost:9090/api/v1/menu/items");
    expect(options.method).toBe("POST");
    expect(options.body).toBeInstanceOf(FormData);
    expect(options.headers.Authorization).toBe("Bearer owner-token");
    expect(options.headers["Content-Type"]).toBeUndefined();
    expect(options.body.get("restaurantId")).toBe("101");
    expect(options.body.get("categoryId")).toBe("9");
    expect(options.body.get("itemName")).toBe("Paneer Tikka");
  });

  it("normalizes admin users/owners list from multiple backend response envelopes", async () => {
    mockFetchResponse({ body: { users: [{ id: "u-1" }] } });
    await expect(getAdminUsers("admin-token")).resolves.toEqual([{ id: "u-1" }]);

    mockFetchResponse({ body: { content: [{ id: "o-1" }] } });
    await expect(getAdminOwners("admin-token")).resolves.toEqual([{ id: "o-1" }]);

    mockFetchResponse({ body: { data: [{ id: "o-2" }] } });
    await expect(getAdminOwners("admin-token")).resolves.toEqual([{ id: "o-2" }]);

    mockFetchResponse({ body: { unexpected: true } });
    await expect(getAdminUsers("admin-token")).resolves.toEqual([]);
  });

  it("throws meaningful error message from backend message, error, or plain text", async () => {
    mockFetchResponse({ ok: false, status: 400, body: { message: "Invalid credentials" } });
    await expect(login({ email: "a@b.com", password: "bad" })).rejects.toThrow("Invalid credentials");

    mockFetchResponse({ ok: false, status: 500, body: { error: "Internal boom" } });
    await expect(register({})).rejects.toThrow("Internal boom");

    mockFetchResponse({ ok: false, status: 404, body: "not found" });
    await expect(deleteAdminRestaurant("admin-token", 99)).rejects.toThrow("not found");
  });

  it("uses correct admin endpoints and payload structure for admin order status update", async () => {
    mockFetchResponse({ body: { id: "OD-1", status: "DELIVERED" } });
    await updateAdminOrderStatus("admin-token", "OD-1", "DELIVERED");

    const [url, options] = fetch.mock.calls[0];
    expect(url).toBe("http://localhost:9090/api/v1/admin/orders/OD-1/status");
    expect(options.method).toBe("PUT");
    expect(JSON.parse(options.body)).toEqual({ status: "DELIVERED" });
  });

  it("sends availability patch body contract for menu item", async () => {
    mockFetchResponse({ body: { id: 10, available: false } });
    await toggleMenuItemAvailability("owner-token", 10, { isAvailable: false });

    const [url, options] = fetch.mock.calls[0];
    expect(url).toBe("http://localhost:9090/api/v1/menu/items/10/availability");
    expect(options.method).toBe("PATCH");
    expect(JSON.parse(options.body)).toEqual({ isAvailable: false });
  });
});
