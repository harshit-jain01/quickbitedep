import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import AppShell from "../components/AppShell";
import { useSession } from "../context/SessionContext";
import { useCart } from "../context/CartContext";
import { addCartItem, createReview, getMenu, getRestaurant, getReviews } from "../lib/api";

const DEFAULT_RESTAURANT_IMAGE = "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=800&q=80";
const DEFAULT_ITEM_IMAGE = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=400&q=80";

export default function RestaurantPage() {
  const { restaurantId } = useParams();
  const { token, setCart } = useSession();
  const { addToCart, currentRestaurantId, clearCart } = useCart();
  const [restaurant, setRestaurant] = useState(null);
  const [menuGroups, setMenuGroups] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [feedback, setFeedback] = useState("");
  const [showClearPrompt, setShowClearPrompt] = useState(false);
  const [pendingItem, setPendingItem] = useState(null);
  const [reviewForm, setReviewForm] = useState({ rating: 5, comment: "" });

  useEffect(() => {
    Promise.allSettled([
      getRestaurant(token, restaurantId),
      getMenu(token, restaurantId),
      getReviews(token, restaurantId)
    ]).then(([restaurantResult, menuResult, reviewResult]) => {
      if (restaurantResult.status === "rejected") {
        throw restaurantResult.reason;
      }

      setRestaurant(restaurantResult.value);
      setMenuGroups(menuResult.status === "fulfilled" ? menuResult.value : []);
      setReviews(reviewResult.status === "fulfilled" ? reviewResult.value : []);

      if (menuResult.status === "rejected") {
        console.error("Error loading menu:", menuResult.reason);
        setFeedback("Restaurant loaded, but menu is currently unavailable");
      }
    }).catch(error => {
      console.error("Error loading restaurant:", error);
      setFeedback("Failed to load restaurant details");
    });
  }, [token, restaurantId]);

  const handleAddToCart = async (item) => {
    const resolvedRestaurantId = Number(restaurant?.id ?? restaurantId);
    if (!Number.isFinite(resolvedRestaurantId) || resolvedRestaurantId <= 0) {
      setFeedback("Unable to add item: invalid restaurant ID");
      return;
    }

    // Check if adding from different restaurant
    if (currentRestaurantId && currentRestaurantId !== resolvedRestaurantId) {
      setPendingItem(item);
      setShowClearPrompt(true);
      return;
    }

    try {
      const response = await addCartItem(token, {
        restaurantId: resolvedRestaurantId,
        restaurantName: restaurant.name,
        menuItemId: item.id,
        itemName: item.name,
        imageUrl: item.imageUrl || DEFAULT_ITEM_IMAGE,
        unitPrice: item.price,
        quantity: 1
      });

      // Update backend cart state
      setCart(response);

      // Update local cart context
      addToCart({
        id: item.id,
        name: item.name,
        price: item.price,
        imageUrl: item.imageUrl || DEFAULT_ITEM_IMAGE,
        quantity: 1
      }, resolvedRestaurantId);

      setFeedback(`${item.name} added to cart`);
      setTimeout(() => setFeedback(""), 3000);
    } catch (error) {
      setFeedback("Error adding to cart: " + error.message);
    }
  };

  const handleClearAndAdd = async () => {
    const resolvedRestaurantId = Number(restaurant?.id ?? restaurantId);
    if (!Number.isFinite(resolvedRestaurantId) || resolvedRestaurantId <= 0) {
      setFeedback("Unable to add item: invalid restaurant ID");
      return;
    }

    clearCart();
    setShowClearPrompt(false);
    if (pendingItem) {
      try {
        const response = await addCartItem(token, {
          restaurantId: resolvedRestaurantId,
          restaurantName: restaurant.name,
          menuItemId: pendingItem.id,
          itemName: pendingItem.name,
          imageUrl: pendingItem.imageUrl || DEFAULT_ITEM_IMAGE,
          unitPrice: pendingItem.price,
          quantity: 1,
          replaceCart: true
        });

        setCart(response);

        addToCart({
          id: pendingItem.id,
          name: pendingItem.name,
          price: pendingItem.price,
          imageUrl: pendingItem.imageUrl || DEFAULT_ITEM_IMAGE,
          quantity: 1
        }, resolvedRestaurantId);

        setFeedback(`${pendingItem.name} added to cart (previous cart cleared)`);
        setPendingItem(null);
        setTimeout(() => setFeedback(""), 3000);
      } catch (error) {
        setFeedback("Error adding to cart: " + error.message);
      }
    }
  };

  const handleSubmitReview = async (event) => {
    event.preventDefault();
    try {
      const createdReview = await createReview(token, {
        restaurantId: Number(restaurantId),
        rating: Number(reviewForm.rating),
        comment: reviewForm.comment
      });
      setReviews((current) => [createdReview, ...current]);
      setReviewForm({ rating: 5, comment: "" });
      setFeedback("Review posted successfully");
      setTimeout(() => setFeedback(""), 3000);
    } catch (error) {
      setFeedback("Failed to post review: " + error.message);
    }
  };

  if (!restaurant) {
    return <div className="page-loader">Loading restaurant details...</div>;
  }

  return (
    <AppShell
      title={restaurant.name}
      subtitle={`${restaurant.cuisines?.join(" • ") || "Multi-cuisine"} • ${restaurant.deliveryTimeMinutes || 30} mins • ${restaurant.area || "Your area"}`}
    >
      {showClearPrompt && (
        <div style={{
          position: "fixed",
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: "rgba(0,0,0,0.5)",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          zIndex: 1000
        }}>
          <div style={{
            backgroundColor: "white",
            padding: "2rem",
            borderRadius: "8px",
            maxWidth: "400px",
            boxShadow: "0 4px 12px rgba(0,0,0,0.2)"
          }}>
            <h3>Switch Restaurant?</h3>
            <p>Your cart contains items from another restaurant. Clear your cart to add items from this restaurant?</p>
            <div style={{ display: "flex", gap: "1rem", marginTop: "1.5rem" }}>
              <button
                className="secondary-button"
                onClick={() => {
                  setShowClearPrompt(false);
                  setPendingItem(null);
                }}
              >
                Cancel
              </button>
              <button
                className="primary-button"
                onClick={handleClearAndAdd}
              >
                Clear & Add
              </button>
            </div>
          </div>
        </div>
      )}

      <section className="restaurant-hero-card">
        <img
          src={restaurant.imageUrl || DEFAULT_RESTAURANT_IMAGE}
          alt={restaurant.name}
          onError={(e) => { e.target.src = DEFAULT_RESTAURANT_IMAGE; }}
        />
        <div>
          <div className="rating-badge">{(restaurant.rating || 4.5).toFixed(1)} rating</div>
          <p>{restaurant.description || "Welcome to our restaurant"}</p>
          <div className="info-grid">
            <div>
              <span>Price for two</span>
              <strong>Rs {restaurant.priceForTwo || 500}</strong>
            </div>
            <div>
              <span>Distance</span>
              <strong>{restaurant.distanceKm || 2.5} km</strong>
            </div>
          </div>
        </div>
      </section>

      {feedback ? <div className="success-banner">{feedback}</div> : null}

      <section className="menu-layout">
        <div className="menu-column">
          {menuGroups && menuGroups.length > 0 ? (
            menuGroups.map((group) => (
              <section key={group.category} className="menu-group">
                <h2>{group.category}</h2>
                <div className="menu-items">
                  {group.items && group.items.map((item) => (
                    <article key={item.id} className="menu-item-card">
                      <div>
                        <h3>{item.name}</h3>
                        <p>{item.description || "Delicious item"}</p>
                        <strong>Rs {(item.price || 100).toFixed(0)}</strong>
                      </div>
                      <div className="menu-item-media">
                        <img
                          src={item.imageUrl || DEFAULT_ITEM_IMAGE}
                          alt={item.name}
                          onError={(e) => { e.target.src = DEFAULT_ITEM_IMAGE; }}
                        />
                        <button type="button" onClick={() => handleAddToCart(item)}>
                          Add
                        </button>
                      </div>
                    </article>
                  ))}
                </div>
              </section>
            ))
          ) : (
            <div className="empty-state">No menu items available</div>
          )}
        </div>

        <aside className="review-column">
          <section className="review-panel">
            <h2>Reviews</h2>
            <form className="review-form" onSubmit={handleSubmitReview}>
              <select
                value={reviewForm.rating}
                onChange={(event) => setReviewForm((current) => ({ ...current, rating: event.target.value }))}
              >
                {[5, 4, 3, 2, 1].map((value) => (
                  <option key={value} value={value}>
                    {value} stars
                  </option>
                ))}
              </select>
              <textarea
                placeholder="Share your experience"
                value={reviewForm.comment}
                onChange={(event) => setReviewForm((current) => ({ ...current, comment: event.target.value }))}
                required
              />
              <button type="submit">Post review</button>
            </form>

            <div className="review-list">
              {reviews && reviews.length > 0 ? (
                reviews.map((review) => (
                  <article key={review.id} className="review-card">
                    <div className="review-card-header">
                      <strong>{review.reviewerName || "Anonymous"}</strong>
                      <span>{review.rating}/5</span>
                    </div>
                    <p>{review.comment}</p>
                  </article>
                ))
              ) : (
                <div className="empty-state">No reviews yet</div>
              )}
            </div>
          </section>
        </aside>
      </section>
    </AppShell>
  );
}
