import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import HomePage from "./HomePage";

const mockNavigate = vi.fn();
const mockGetCategories = vi.fn();
const mockGetRestaurants = vi.fn();
const mockSession = {
  token: "jwt-token",
  user: { role: "CUSTOMER", email: "user@quickbite.com" },
  isRestaurantOwner: vi.fn(() => false),
  isAgent: vi.fn(() => false)
};

vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useNavigate: () => mockNavigate
  };
});

vi.mock("../context/SessionContext", () => ({
  useSession: vi.fn(() => mockSession)
}));

vi.mock("../lib/api", () => ({
  getCategories: (...args) => mockGetCategories(...args),
  getRestaurants: (...args) => mockGetRestaurants(...args)
}));

describe("HomePage integration flow", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockSession.isRestaurantOwner.mockReturnValue(false);
    mockSession.isAgent.mockReturnValue(false);
    mockGetCategories.mockResolvedValue([
      { id: 1, name: "Pizza", imageUrl: "pizza.png" },
      { id: 2, name: "Biryani", imageUrl: "biryani.png" }
    ]);
    mockGetRestaurants.mockResolvedValue([
      { id: 11, name: "Firestone Pizza", cuisines: ["Pizza"], rating: 4.5, imageUrl: "img" }
    ]);
  });

  it("loads categories/restaurants and renders restaurant cards", async () => {
    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>
    );

    expect(screen.getByText("Loading restaurants...")).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText("Firestone Pizza")).toBeInTheDocument();
    });

    expect(mockGetCategories).toHaveBeenCalledWith("jwt-token");
    expect(mockGetRestaurants).toHaveBeenCalledWith("jwt-token", { category: "", search: "" });
  });

  it("refetches restaurants when search term changes", async () => {
    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>
    );

    await screen.findByText("Firestone Pizza");
    fireEvent.change(screen.getByPlaceholderText("Search for biryani, pizza, desserts..."), {
      target: { value: "burger" }
    });

    await waitFor(() => {
      expect(mockGetRestaurants).toHaveBeenLastCalledWith("jwt-token", { category: "", search: "burger" });
    });
  });

  it("redirects role-specific users before rendering normal flow", async () => {
    mockSession.isRestaurantOwner.mockReturnValue(true);

    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith("/restaurant/dashboard", { replace: true });
    });
  });
});
