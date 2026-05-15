import { Link } from "react-router-dom";

export default function TrackingLink({ order }) {
  return (
    <Link
      to={`/tracking/${order.id || order.orderReference}`}
      className="tracking-link-button"
      title="Track your order in real-time"
    >
      <span className="tracking-icon">📍</span>
      <span className="tracking-text">Track Order</span>
      <span className="tracking-arrow">→</span>
    </Link>
  );
}



