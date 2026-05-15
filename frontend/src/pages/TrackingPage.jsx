import { useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import AppShell from "../components/AppShell";
import { useSession } from "../context/SessionContext";
import { getOrderTracking } from "../lib/api";

const POLL_MS = 5000;

const ORDER_STEPS = [
  { id: 1, title: "Order", label: "Received", icon: "🧾" },
  { id: 2, title: "Order", label: "Processed", icon: "🍽️" },
  { id: 3, title: "Package", label: "Shipped", icon: "🛵" },
  { id: 4, title: "Package", label: "Arrived", icon: "🏠" }
];

function resolveStep(status) {
  switch ((status || "").toUpperCase()) {
    case "PLACED":
      return 1;
    case "CONFIRMED":
    case "PREPARING":
      return 2;
    case "PICKED_UP":
      return 3;
    case "DELIVERED":
      return 4;
    default:
      return 1;
  }
}

export default function TrackingPage() {
  const { orderId } = useParams();
  const { token } = useSession();
  const [tracking, setTracking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  const activeStep = useMemo(() => resolveStep(tracking?.status), [tracking?.status]);

  useEffect(() => {
    const fetchTracking = async (background = false) => {
      try {
        if (background) {
          setRefreshing(true);
        } else {
          setLoading(true);
        }
        const data = await getOrderTracking(token, orderId);
        setTracking(data);
        setError(null);
      } catch (err) {
        setError(err.message);
        if (!background) {
          setTracking(null);
        }
      } finally {
        if (background) {
          setRefreshing(false);
        } else {
          setLoading(false);
        }
      }
    };

    if (orderId && token) {
      fetchTracking(false);
      const interval = setInterval(() => fetchTracking(true), POLL_MS);
      return () => clearInterval(interval);
    }
  }, [orderId, token]);

  return (
    <AppShell title="Order Tracking" subtitle="Get simple order status updates">
      <div className="page-content">
        {loading && !tracking && (
          <div className="tracking-loading">
            <div className="spinner"></div>
            <p>Loading tracking information...</p>
          </div>
        )}

        {error && (
          <div className="error-banner">
            <span>⚠️</span>
            <div>
              <strong>Unable to track order</strong>
              <p>{error}</p>
            </div>
          </div>
        )}

        {tracking && (
          <div className="tracking-reference-wrap">
            <div className="tracking-reference-card">
              <div className="tracking-reference-head">
                <strong>ORDER STATUS</strong>
                <span>#{tracking.orderReference}</span>
              </div>

              <div className="tracking-reference-body">
                <div className="tracking-reference-steps">
                  {ORDER_STEPS.map((step) => {
                    const isComplete = activeStep >= step.id;
                    return (
                      <div key={step.id} className={`tracking-step-item ${isComplete ? "completed" : ""}`}>
                        <span className="tracking-step-icon" aria-hidden="true">{step.icon}</span>
                        <div className="tracking-step-text">
                          <small>{step.title}</small>
                          <strong>{step.label}</strong>
                        </div>
                      </div>
                    );
                  })}
                </div>

                <div className="tracking-step-rail" aria-hidden="true">
                  {ORDER_STEPS.map((step) => {
                    const isComplete = activeStep >= step.id;
                    return <span key={step.id} className={`tracking-step-dot ${isComplete ? "completed" : ""}`} />;
                  })}
                </div>
              </div>

              <div className="tracking-reference-meta">
                <p><strong>ETA:</strong> {tracking.estimatedDeliveryWindow || "Calculating..."}</p>
                <p><strong>Update:</strong> {tracking.message || "Status is being refreshed"}</p>
                {tracking.deliveryAgent ? <p><strong>Agent:</strong> {tracking.deliveryAgent}</p> : null}
                <p>
                  <strong>Last sync:</strong> {getTimeAgo(tracking.updatedAt)}
                  {refreshing ? <span className="tracking-refreshing"> refreshing...</span> : null}
                </p>
              </div>
            </div>
          </div>
        )}
      </div>
    </AppShell>
  );
}


function getTimeAgo(dateString) {
  const now = new Date();
  const then = new Date(dateString);
  const seconds = Math.floor((now - then) / 1000);
  
  if (seconds < 60) return 'just now';
  if (seconds < 3600) return `${Math.floor(seconds / 60)} min ago`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)} hours ago`;
  return `${Math.floor(seconds / 86400)} days ago`;
}

