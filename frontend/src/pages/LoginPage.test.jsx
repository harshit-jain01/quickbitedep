import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import LoginPage from "./LoginPage";

const mockNavigate = vi.fn();
const mockLoginApi = vi.fn();
const mockSessionLogin = vi.fn();

vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useLocation: () => ({ state: null })
  };
});

vi.mock("../context/SessionContext", () => ({
  useSession: vi.fn()
}));

vi.mock("../lib/api", () => ({
  login: (...args) => mockLoginApi(...args)
}));

import { useSession } from "../context/SessionContext";

function renderPage() {
  return render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>
  );
}

describe("LoginPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useSession.mockReturnValue({
      login: mockSessionLogin,
      token: "",
      loading: false,
      user: null
    });
  });

  it("shows validation errors for invalid input", async () => {
    renderPage();

    fireEvent.blur(screen.getByPlaceholderText("harshit@gmail.com"));
    fireEvent.change(screen.getByPlaceholderText("harshit@gmail.com"), { target: { value: "invalid-email" } });
    fireEvent.blur(screen.getByPlaceholderText("harshit@gmail.com"));
    fireEvent.blur(screen.getByPlaceholderText("Enter your password"));

    expect(await screen.findByText("Enter a valid email address")).toBeInTheDocument();
    expect(screen.getByText("Password is required")).toBeInTheDocument();
    expect(mockLoginApi).not.toHaveBeenCalled();
  });

  it("submits login and redirects based on role", async () => {
    mockLoginApi.mockResolvedValueOnce({
      token: "jwt-token",
      user: { role: "CUSTOMER", email: "user@quickbite.com" }
    });

    renderPage();

    fireEvent.change(screen.getByPlaceholderText("harshit@gmail.com"), { target: { value: "user@quickbite.com" } });
    fireEvent.change(screen.getByPlaceholderText("Enter your password"), { target: { value: "pass@123" } });
    fireEvent.submit(screen.getByRole("button", { name: "Continue" }).closest("form"));

    await waitFor(() => {
      expect(mockLoginApi).toHaveBeenCalledWith({
        email: "user@quickbite.com",
        password: "pass@123"
      });
    });

    expect(mockSessionLogin).toHaveBeenCalledWith({
      token: "jwt-token",
      user: { role: "CUSTOMER", email: "user@quickbite.com" }
    });
    expect(mockNavigate).toHaveBeenCalledWith("/home", { replace: true });
  });

  it("shows API error on failed login", async () => {
    mockLoginApi.mockRejectedValueOnce(new Error("Invalid credentials"));
    renderPage();

    fireEvent.change(screen.getByPlaceholderText("harshit@gmail.com"), { target: { value: "user@quickbite.com" } });
    fireEvent.change(screen.getByPlaceholderText("Enter your password"), { target: { value: "bad-pass" } });
    fireEvent.submit(screen.getByRole("button", { name: "Continue" }).closest("form"));

    expect(await screen.findByText("Invalid credentials")).toBeInTheDocument();
    expect(mockSessionLogin).not.toHaveBeenCalled();
  });

  it("redirects already logged-in users to role route", async () => {
    useSession.mockReturnValue({
      login: mockSessionLogin,
      token: "token",
      loading: false,
      user: { role: "AGENT", email: "agent@quickbite.com" }
    });

    renderPage();

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith("/delivery-agent/dashboard", { replace: true });
    });
  });
});
