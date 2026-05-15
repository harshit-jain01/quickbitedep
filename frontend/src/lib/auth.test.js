import { clearSession, getStoredUser, getToken, saveSession } from "./auth";

describe("auth storage helpers", () => {
  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
  });

  it("saves session in sessionStorage and clears legacy localStorage", () => {
    localStorage.setItem("quickbite_token", "legacy-token");
    localStorage.setItem("quickbite_user", JSON.stringify({ email: "legacy@q.com" }));

    saveSession("new-token", { email: "test@q.com", role: "CUSTOMER" });

    expect(sessionStorage.getItem("quickbite_token")).toBe("new-token");
    expect(JSON.parse(sessionStorage.getItem("quickbite_user"))).toEqual({ email: "test@q.com", role: "CUSTOMER" });
    expect(localStorage.getItem("quickbite_token")).toBeNull();
    expect(localStorage.getItem("quickbite_user")).toBeNull();
  });

  it("returns empty token and null user when storage is missing or invalid", () => {
    expect(getToken()).toBe("");
    expect(getStoredUser()).toBeNull();

    sessionStorage.setItem("quickbite_user", "{invalid-json");
    expect(getStoredUser()).toBeNull();
  });

  it("clears persisted session keys", () => {
    sessionStorage.setItem("quickbite_token", "abc");
    sessionStorage.setItem("quickbite_user", JSON.stringify({ email: "x@q.com" }));

    clearSession();

    expect(sessionStorage.getItem("quickbite_token")).toBeNull();
    expect(sessionStorage.getItem("quickbite_user")).toBeNull();
  });
});
