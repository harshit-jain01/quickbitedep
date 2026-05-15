import { useState, useEffect } from "react";
import { getRestaurantOwnerReviews } from "../lib/api";

export default function ReviewsManagement({ token }) {
  const [reviews, setReviews] = useState([]);
  const [filter, setFilter] = useState("all");
  const [error, setError] = useState("");

  useEffect(() => {
    loadReviews();
  }, [token]);

  const loadReviews = async () => {
    try {
      const data = await getRestaurantOwnerReviews(token);
      setReviews(data || []);
    } catch (err) {
      setError(err.message);
    }
  };

  const filteredReviews = reviews.filter((review) => {
    const rating = Number(review.rating || 0);
    if (filter === "all") return true;
    if (filter === "positive") return rating >= 4;
    if (filter === "negative") return rating < 3;
    return true;
  });

  const averageRating =
    reviews.length > 0
      ? (reviews.reduce((sum, r) => sum + Number(r.rating || 0), 0) / reviews.length).toFixed(1)
      : 0;

  return (
    <div className="reviews-section">
      {error && <div className="error-banner">{error}</div>}

      <div className="reviews-header">
        <div className="rating-summary">
          <h3>Customer Reviews</h3>
          <div className="avg-rating">
            <span className="stars">{"\u2B50".repeat(Math.round(averageRating))}</span>
            <strong>{averageRating} / 5</strong>
            <small>({reviews.length} reviews)</small>
          </div>
        </div>

        <div className="review-filters">
          <label>Filter:</label>
          <select value={filter} onChange={(e) => setFilter(e.target.value)}>
            <option value="all">All Reviews</option>
            <option value="positive">Positive (4-5 stars)</option>
            <option value="negative">Negative (&lt;3 stars)</option>
          </select>
        </div>
      </div>

      {filteredReviews.length === 0 ? (
        <div className="empty-state">
          {reviews.length === 0 ? "No reviews yet" : `No ${filter} reviews found`}
        </div>
      ) : (
        <div className="reviews-list">
          {filteredReviews.map((review) => (
            <div key={review.reviewId || review.id} className="review-card">
              <div className="review-header">
                <div>
                  <h4>{review.customerName || review.reviewerName || "Customer"}</h4>
                  <small>{new Date(review.createdAt).toLocaleDateString()}</small>
                </div>
                <span className="rating">{"\u2B50".repeat(Number(review.rating || 0))}</span>
              </div>
              <p className="review-text">{review.reviewText || review.comment || ""}</p>
              {review.itemName && (
                <p className="item-reference">
                  <small>About: {review.itemName}</small>
                </p>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
