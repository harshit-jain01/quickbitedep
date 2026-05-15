import { useMemo } from "react";
import { useLocation, useNavigate } from "react-router-dom";

function formatCurrency(value) {
  const amount = Number(value || 0);
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0
  }).format(amount);
}

function resolveEta(order) {
  if (order?.estimatedDeliveryWindow) {
    return order.estimatedDeliveryWindow;
  }
  const eta = Number(order?.etaMinutes || 30);
  const lower = Math.max(eta - 5, 5);
  const upper = eta + 5;
  return `${lower}-${upper} min`;
}

export default function OrderConfirmationPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const order = location.state?.order || null;

  const details = useMemo(() => {
    if (!order) {
      return null;
    }
    return {
      orderId: order.displayOrderId || order.orderReference || "--",
      orderReference: order.orderReference || order.displayOrderId || null,
      total: formatCurrency(order.total),
      eta: resolveEta(order),
      paymentMode: order.paymentMode || "--",
      paymentStatus: order.paymentStatus || "--"
    };
  }, [order]);

  if (!details) {
    return (
      <main className="order-confirmation-page">
        <section className="order-confirmation-card">
          <h1>Order confirmation not found</h1>
          <p>Please check your recent orders in your account.</p>
          <button type="button" onClick={() => navigate("/account", { replace: true })}>
            Go to Account
          </button>
        </section>
      </main>
    );
  }

  return (
    <main className="order-confirmation-page">
      <section className="order-confirmation-card" aria-live="polite">
        <div className="order-success-icon">✓</div>
        <h1>Order Placed!</h1>
        <p>
          {details.paymentMode === "COD"
            ? "Your order is confirmed. Payment will be collected on delivery."
            : "Your payment is successful and order is being prepared."}
        </p>

        <div className="order-summary-box">
          <div>
            <span>Order ID</span>
            <strong>{details.orderId}</strong>
          </div>
          <div>
            <span>Total Amount</span>
            <strong>{details.total}</strong>
          </div>
          <div>
            <span>Estimated Delivery</span>
            <strong className="eta-highlight">{details.eta}</strong>
          </div>
          <div>
            <span>Payment</span>
            <strong>{details.paymentMode} ({details.paymentStatus})</strong>
          </div>
        </div>

        <div className="order-confirmation-actions">
          <button type="button" className="back-home-button" onClick={() => navigate("/home", { replace: true })}>
            Back to Home
          </button>
          {details.orderReference ? (
            <button
              type="button"
              className="back-home-button secondary-track-button"
              onClick={() => navigate(`/tracking/${details.orderReference}`)}
            >
              Track Order
            </button>
          ) : null}
        </div>
      </section>
    </main>
  );
}

