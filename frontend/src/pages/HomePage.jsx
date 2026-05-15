import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import AppShell from "../components/AppShell";
import CategoryStrip from "../components/CategoryStrip";
import RestaurantCard from "../components/RestaurantCard";
import { useSession } from "../context/SessionContext";
import { getCategories, getRestaurants } from "../lib/api";

export default function HomePage() {
  const navigate = useNavigate();
  const { token, user, isRestaurantOwner, isAgent } = useSession();

  useEffect(() => {
    if (isRestaurantOwner?.()) {
      navigate("/restaurant/dashboard", { replace: true });
      return;
    }

    if (isAgent?.()) {
      navigate("/delivery-agent/dashboard", { replace: true });
    }
  }, [user, navigate, isRestaurantOwner, isAgent]);
  const [categories, setCategories] = useState([]);
  const [restaurants, setRestaurants] = useState([]);
  const [activeCategory, setActiveCategory] = useState("");
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      getCategories(token),
      getRestaurants(token, { category: activeCategory, search })
    ])
      .then(([categoryData, restaurantData]) => {
        setCategories(categoryData);
        setRestaurants(restaurantData);
      })
      .finally(() => setLoading(false));
  }, [token, activeCategory, search]);

  return (
    <AppShell
      title="Order food, groceries and comfort meals in minutes"
      subtitle="Browse top-rated kitchens, discover curated offers, and check out in one smooth flow."
    >
      <section className="search-banner">
        <input
          type="search"
          placeholder="Search for biryani, pizza, desserts..."
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
      </section>

      <CategoryStrip categories={categories} activeCategory={activeCategory} onSelect={setActiveCategory} />

      <section className="section-block">
        <div className="section-heading">
          <h2>Discover best restaurants on QuickBite</h2>
        </div>

        {loading ? <div className="empty-state">Loading restaurants...</div> : null}

        <div className="restaurant-grid">
          {restaurants.map((restaurant) => (
            <RestaurantCard key={restaurant.id} restaurant={restaurant} />
          ))}
        </div>
      </section>
    </AppShell>
  );
}
