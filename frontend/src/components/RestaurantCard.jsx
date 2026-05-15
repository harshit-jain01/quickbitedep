import { Link } from "react-router-dom";

const DEFAULT_RESTAURANT_IMAGE = "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=400&q=80";

export default function RestaurantCard({ restaurant }) {
  return (
    <Link className="restaurant-card" to={`/restaurants/${restaurant.id}`}>
      <img
        src={restaurant.imageUrl || DEFAULT_RESTAURANT_IMAGE}
        alt={restaurant.name}
        onError={(e) => {
          e.target.src = DEFAULT_RESTAURANT_IMAGE;
        }}
      />
      <div className="restaurant-card-body">
        <div className="restaurant-card-header">
          <h3>{restaurant.name}</h3>
          <span>{restaurant.rating?.toFixed(1) || "4.5"}</span>
        </div>
        <p>{restaurant.cuisines?.join(" • ") || "Multi-cuisine"}</p>
      </div>
    </Link>
  );
}
