import { useState, useEffect } from "react";
import { getRestaurantOwnerOrders, updateOrderStatus } from "../lib/api";

export default function OrdersManagement({ token }) {
  const [orders, setOrders] = useState([]);
  const [filter, setFilter] = useState("ALL");
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  useEffect(() => {
    loadOrders();
    const interval = setInterval(loadOrders, 10000); // Refresh every 10 seconds
    return () => clearInterval(interval);
  }, [token]);

  const loadOrders = async () => {
    try {
      const data = await getRestaurantOwnerOrders(token);
      setOrders(data || []);
    } catch (err) {
      setError(err.message);
    }
  };

  const handleStatusUpdate = async (orderReference, newStatus) => {
    try {
      setError("");
      await updateOrderStatus(token, orderReference, {
        deliveryStatus: newStatus
      });
      setSuccessMessage("Order status updated");
      await loadOrders();
      setTimeout(() => setSuccessMessage(""), 3000);
    } catch (err) {
      setError(err.message);
    }
  };

  const filteredOrders = orders.filter((order) => {
    if (filter === "ALL") return true;
    return String(order.deliveryStatus || "").toUpperCase() === filter;
  });

  const statusFlow = ["CONFIRMED", "PREPARING"];

  const isStatusActionAllowed = (currentStatus, nextStatus) => {
    const normalizedCurrent = String(currentStatus || "").toUpperCase();
    const normalizedNext = String(nextStatus || "").toUpperCase();
    return (
      (normalizedCurrent === "PLACED" && normalizedNext === "CONFIRMED") ||
      (normalizedCurrent === "CONFIRMED" && normalizedNext === "PREPARING")
    );
  };

  return (
    <div className="orders-management">
      {error && <div className="error-banner">{error}</div>}
      {successMessage && <div className="success-banner">{successMessage}</div>}

      <div className="filter-bar">
        <label>Filter by status:</label>
        <select value={filter} onChange={(e) => setFilter(e.target.value)}>
          <option value="ALL">All Orders</option>
          {["PLACED", ...statusFlow, "PICKED_UP", "DELIVERED"].map((status) => (
            <option key={status} value={status}>
              {status}
            </option>
          ))}
        </select>
      </div>

      {filteredOrders.length === 0 ? (
        <div className="empty-state">{filter === "ALL" ? "No incoming orders right now." : `No orders with status: ${filter}`}</div>
      ) : (
        <div className="orders-grid">
          {filteredOrders.map((order) => (
            <div key={order.orderReference} className="order-card-restaurant">
              <div className="order-header">
                <div>
                  <h4>Order #{order.orderReference}</h4>
                  <p>{order.customerName || order.userEmail || "Customer"}</p>
                  <small>{order.createdAt ? new Date(order.createdAt).toLocaleString() : "--"}</small>
                </div>
                <div className="order-status-badge">
                  <strong>{order.deliveryStatus}</strong>
                </div>
              </div>

              <div className="order-items-list">
                {order.items?.map((item) => (
                  <div key={`${order.orderReference}-${item.itemName}`} className="item-row">
                    <span>
                      {item.itemName} x {item.quantity}
                    </span>
                    <span>₹{(item.lineTotal || 0).toFixed(0)}</span>
                  </div>
                ))}
              </div>

              <div className="order-total">
                <strong>Total: ₹{Number(order.total || 0).toFixed(0)}</strong>
              </div>

              <div className="delivery-info">
                <p>
                  <strong>Delivery Time:</strong> {order.etaMinutes} mins
                </p>
              </div>

              <div className="status-actions">
                {statusFlow.map((status) => {
                  const isAvailable = isStatusActionAllowed(order.deliveryStatus, status);
                  return (
                    <button
                      key={status}
                      className={`status-button ${isAvailable ? "" : "disabled"}`}
                      onClick={() => handleStatusUpdate(order.orderReference, status)}
                      disabled={!isAvailable}
                    >
                      {status}
                    </button>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

