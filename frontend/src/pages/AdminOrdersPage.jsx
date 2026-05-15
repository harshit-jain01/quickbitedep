import { useEffect, useState } from "react";
import { getAdminOrders, updateAdminOrderStatus } from "../lib/api";
import { useSession } from "../context/SessionContext";

const STATUS_OPTIONS = ["PLACED", "CONFIRMED", "PREPARING", "PICKED_UP", "DELIVERED"];
const ADMIN_EDITABLE_STATUSES = new Set(["CONFIRMED", "PREPARING"]);

export default function AdminOrdersPage() {
  const { token } = useSession();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadOrders = async () => {
    try {
      const response = await getAdminOrders(token);
      setOrders(Array.isArray(response) ? response : []);
    } catch (requestError) {
      setError(requestError.message || "Failed to load orders");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadOrders();
  }, [token]);

  const handleStatusChange = async (orderReference, nextStatus) => {
    try {
      const updated = await updateAdminOrderStatus(token, orderReference, nextStatus);
      setOrders((current) =>
        current.map((item) =>
          item.orderReference === orderReference
            ? { ...item, deliveryStatus: updated.deliveryStatus }
            : item
        )
      );
    } catch (requestError) {
      setError(requestError.message || "Failed to update status");
    }
  };

  if (loading) {
    return <div className="page-loader">Loading orders...</div>;
  }

  return (
    <section className="admin-page">
      <h1>Orders</h1>
      <p className="admin-page-subtitle">Track all orders and update delivery status</p>
      {error ? <div className="error-banner">{error}</div> : null}

      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Order ID</th>
              <th>Restaurant</th>
              <th>Total</th>
              <th>Created</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((item) => (
              <tr key={item.orderReference}>
                <td>{item.orderReference}</td>
                <td>{item.restaurantName}</td>
                <td>INR {Number(item.total || 0).toFixed(2)}</td>
                <td>{item.createdAt ? new Date(item.createdAt).toLocaleDateString() : "-"}</td>
                <td>
                  <select
                    value={item.deliveryStatus}
                    onChange={(event) => handleStatusChange(item.orderReference, event.target.value)}
                  >
                    {STATUS_OPTIONS.map((status) => (
                      <option key={status} value={status} disabled={!ADMIN_EDITABLE_STATUSES.has(status)}>
                        {status}
                      </option>
                    ))}
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

