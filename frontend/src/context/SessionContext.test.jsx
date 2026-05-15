import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { SessionProvider, useSession } from "./SessionContext";

const mockGetToken = vi.fn();
const mockGetStoredUser = vi.fn();
const mockSaveSession = vi.fn();
const mockClearSession = vi.fn();
const mockGetProfile = vi.fn();
const mockGetCart = vi.fn();

vi.mock("../lib/auth", () => ({
  getToken: () => mockGetToken(),
  getStoredUser: () => mockGetStoredUser(),
  saveSession: (...args) => mockSaveSession(...args),
  clearSession: () => mockClearSession()
}));

vi.mock("../lib/api", () => ({
  getProfile: (...args) => mockGetProfile(...args),
  getCart: (...args) => mockGetCart(...args)
}));

function SessionProbe() {
  const session = useSession();
  return (
    <div>
      <span data-testid="loading">{String(session.loading)}</span>
      <span data-testid="token">{session.token || ""}</span>
      <span data-testid="email">{session.user?.email || ""}</span>
      <span data-testid="cart-items">{String(session.cart?.itemCount ?? -1)}</span>
      <span data-testid="is-admin">{String(session.isAdmin())}</span>
      <button type="button" onClick={() => session.login({ token: "new-token", user: { email: "admin123@gmail.com", role: "ADMIN" } })}>
        login
      </button>
      <button type="button" onClick={session.logout}>
        logout
      </button>
    </div>
  );
}

describe("SessionProvider", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("starts idle when there is no token", async () => {
    mockGetToken.mockReturnValue("");
    mockGetStoredUser.mockReturnValue(null);

    render(
      <SessionProvider>
        <SessionProbe />
      </SessionProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId("loading")).toHaveTextContent("false");
    });
    expect(screen.getByTestId("token")).toHaveTextContent("");
    expect(screen.getByTestId("cart-items")).toHaveTextContent("-1");
  });

  it("hydrates profile and cart when token exists", async () => {
    mockGetToken.mockReturnValue("jwt-token");
    mockGetStoredUser.mockReturnValue(null);
    mockGetProfile.mockResolvedValue({ email: "user@quickbite.com", role: "CUSTOMER" });
    mockGetCart.mockResolvedValue({ itemCount: 3 });

    render(
      <SessionProvider>
        <SessionProbe />
      </SessionProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId("loading")).toHaveTextContent("false");
    });

    expect(mockGetProfile).toHaveBeenCalledWith("jwt-token");
    expect(mockGetCart).toHaveBeenCalledWith("jwt-token");
    expect(mockSaveSession).toHaveBeenCalledWith("jwt-token", { email: "user@quickbite.com", role: "CUSTOMER" });
    expect(screen.getByTestId("email")).toHaveTextContent("user@quickbite.com");
    expect(screen.getByTestId("cart-items")).toHaveTextContent("3");
  });

  it("clears session when profile fetch fails and no optimistic user exists", async () => {
    mockGetToken.mockReturnValue("jwt-token");
    mockGetStoredUser.mockReturnValue(null);
    mockGetProfile.mockRejectedValue(new Error("unauthorized"));

    render(
      <SessionProvider>
        <SessionProbe />
      </SessionProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId("loading")).toHaveTextContent("false");
    });

    expect(mockClearSession).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId("email")).toHaveTextContent("");
    expect(screen.getByTestId("token")).toHaveTextContent("");
  });

  it("keeps optimistic user when profile fetch fails transiently", async () => {
    mockGetToken.mockReturnValue("jwt-token");
    mockGetStoredUser.mockReturnValue({ email: "optimistic@quickbite.com", role: "CUSTOMER" });
    mockGetProfile.mockRejectedValue(new Error("temporary"));
    mockGetCart.mockResolvedValue({ itemCount: 0 });

    render(
      <SessionProvider>
        <SessionProbe />
      </SessionProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId("loading")).toHaveTextContent("false");
    });

    expect(mockClearSession).not.toHaveBeenCalled();
    expect(screen.getByTestId("email")).toHaveTextContent("optimistic@quickbite.com");
  });

  it("supports login/logout helpers and role predicates", async () => {
    mockGetToken.mockReturnValue("");
    mockGetStoredUser.mockReturnValue(null);

    render(
      <SessionProvider>
        <SessionProbe />
      </SessionProvider>
    );

    fireEvent.click(screen.getByRole("button", { name: "login" }));
    await waitFor(() => {
      expect(screen.getByTestId("token")).toHaveTextContent("new-token");
    });
    expect(screen.getByTestId("is-admin")).toHaveTextContent("true");
    expect(mockSaveSession).toHaveBeenCalledWith("new-token", { email: "admin123@gmail.com", role: "ADMIN" });

    fireEvent.click(screen.getByRole("button", { name: "logout" }));
    await waitFor(() => {
      expect(screen.getByTestId("token")).toHaveTextContent("");
    });
    expect(mockClearSession).toHaveBeenCalled();
  });
});
