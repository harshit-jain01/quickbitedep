import { getPostLoginPath } from "./roleRedirect";

describe("getPostLoginPath", () => {
  it("returns admin dashboard only for fixed admin account", () => {
    expect(getPostLoginPath({ role: "ADMIN", email: "admin123@gmail.com" })).toBe("/admin");
    expect(getPostLoginPath({ role: "ADMIN", email: "another-admin@gmail.com" })).toBe("/home");
  });

  it("returns role-specific routes for owner and agent", () => {
    expect(getPostLoginPath({ role: "RESTAURANT_OWNER" })).toBe("/restaurant/dashboard");
    expect(getPostLoginPath({ role: "AGENT" })).toBe("/delivery-agent/dashboard");
  });

  it("falls back to provided path for customer or unknown role", () => {
    expect(getPostLoginPath({ role: "CUSTOMER" }, "/home")).toBe("/home");
    expect(getPostLoginPath(undefined, "/login")).toBe("/login");
  });
});
