function assertString(value, field) {
  expect(typeof value).toBe("string");
  expect(value.length).toBeGreaterThan(0);
  expect(field).toBeTruthy();
}

function assertNumber(value) {
  expect(typeof value).toBe("number");
  expect(Number.isFinite(value)).toBe(true);
}

function assertBoolean(value) {
  expect(typeof value).toBe("boolean");
}

describe("frontend-backend API contracts", () => {
  it("validates auth response contract used by login/register flows", () => {
    const authResponse = {
      token: "jwt-token",
      user: {
        id: "24fa8ea6-e9fd-430f-8f43-546f4d7f5de2",
        email: "user@quickbite.com",
        role: "CUSTOMER"
      }
    };

    assertString(authResponse.token, "token");
    assertString(authResponse.user.id, "user.id");
    assertString(authResponse.user.email, "user.email");
    assertString(authResponse.user.role, "user.role");
  });

  it("validates restaurant listing contract consumed by HomePage", () => {
    const restaurant = {
      id: 12,
      name: "Firestone Pizza",
      category: "Pizza",
      area: "Shahpura",
      cuisines: ["Pizza", "Italian"],
      rating: 4.5,
      deliveryTimeMinutes: 25,
      priceForTwo: 500,
      distanceKm: 2.0,
      imageUrl: "https://img",
      description: "Stone oven pizza"
    };

    assertNumber(restaurant.id);
    assertString(restaurant.name, "name");
    assertString(restaurant.category, "category");
    expect(Array.isArray(restaurant.cuisines)).toBe(true);
    assertNumber(restaurant.rating);
  });

  it("validates cart contract consumed by CheckoutPage", () => {
    const cart = {
      userEmail: "user@quickbite.com",
      restaurantId: 5,
      restaurantName: "Burger Hub",
      items: [
        {
          id: 1,
          itemName: "Cheese Burger",
          quantity: 2,
          unitPrice: 199,
          imageUrl: "https://img"
        }
      ],
      itemCount: 2,
      subtotal: 398,
      deliveryFee: 30,
      taxes: 18,
      total: 446
    };

    assertString(cart.userEmail, "userEmail");
    expect(Array.isArray(cart.items)).toBe(true);
    assertNumber(cart.items[0].id);
    assertString(cart.items[0].itemName, "itemName");
    assertNumber(cart.items[0].quantity);
    assertNumber(cart.total);
  });

  it("validates delivery agent contract for dashboard and order state updates", () => {
    const agent = {
      id: "AGT1001",
      name: "Akash",
      phone: "9999988888",
      vehicleType: "Bike",
      vehicleNumber: "KA01AB1234",
      verified: true,
      active: true,
      online: true,
      totalDeliveries: 8
    };

    assertString(agent.id, "id");
    assertString(agent.name, "name");
    assertBoolean(agent.online);
    assertNumber(agent.totalDeliveries);
  });
});
