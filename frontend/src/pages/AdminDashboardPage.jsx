import { useEffect, useState } from "react";
import { getAdminDashboard, getAdminOrders, getAdminRestaurants } from "../lib/api";
import { useSession } from "../context/SessionContext";

const currency = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 0
});

function toNumber(value) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function extractList(payload) {
  if (Array.isArray(payload)) {
    return payload;
  }
  if (Array.isArray(payload?.data)) {
    return payload.data;
  }
  if (Array.isArray(payload?.content)) {
    return payload.content;
  }
  if (Array.isArray(payload?.orders)) {
    return payload.orders;
  }
  if (Array.isArray(payload?.restaurants)) {
    return payload.restaurants;
  }
  return [];
}

function normalizeStatus(status) {
  return String(status || "")
    .trim()
    .replace(/[-\s]+/g, "_")
    .toUpperCase();
}

function getStatusClass(status) {
  const normalized = normalizeStatus(status);
  if (normalized === "DELIVERED") {
    return "delivered";
  }
  if (normalized === "OUT_FOR_DELIVERY" || normalized === "PICKED_UP") {
    return "out-for-delivery";
  }
  if (!normalized) {
    return "no-data";
  }
  return "preparing";
}

function getStatusLabel(status) {
  const normalized = normalizeStatus(status);
  if (normalized === "DELIVERED") {
    return "Delivered";
  }
  if (normalized === "OUT_FOR_DELIVERY" || normalized === "PICKED_UP") {
    return "Out for Delivery";
  }
  if (!normalized) {
    return "No data";
  }
  return "Preparing";
}

function getOrderTime(order) {
  const source = order?.createdAt || order?.created_at || order?.orderedAt || order?.placedAt;
  const parsed = source ? new Date(source).getTime() : 0;
  return Number.isFinite(parsed) ? parsed : 0;
}

function DashboardIcon({ type }) {
  if (type === "users") {
    return (
      <svg viewBox="0 0 24 24" focusable="false">
        <circle cx="9" cy="8" r="3" />
        <path d="M3.5 18.5c0-2.7 2.3-4.8 5.5-4.8s5.5 2.1 5.5 4.8" />
        <path d="M15 10.5c1.8 0.2 3.2 1.2 3.8 2.7" />
      </svg>
    );
  }
  if (type === "restaurants") {
    return (
      <svg viewBox="0 0 24 24" focusable="false">
        <path d="M5 10h14v8H5z" />
        <path d="M4 10l1.2-4h13.6L20 10" />
        <path d="M9 14h6" />
      </svg>
    );
  }
  if (type === "orders") {
    return (
      <svg viewBox="0 0 24 24" focusable="false">
        <rect x="5" y="4" width="14" height="16" rx="2" />
        <path d="M9 8h6M9 12h6M9 16h4" />
      </svg>
    );
  }
  return (
    <svg viewBox="0 0 24 24" focusable="false">
      <path d="M12 4v16" />
      <path d="M16.5 8.5c0-1.7-1.8-3-4-3s-4 1.3-4 3 1.8 3 4 3 4 1.3 4 3-1.8 3-4 3-4-1.3-4-3" />
    </svg>
  );
}

export default function AdminDashboardPage() {
  const { token } = useSession();
  const [data, setData] = useState({
    totalUsers: 0,
    totalRestaurants: 0,
    totalOrders: 0,
    revenue: 0
  });
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    const loadDashboard = async () => {
      try {
        setLoading(true);
        const [dashboardResult, ordersResult, restaurantsResult] = await Promise.allSettled([
          getAdminDashboard(token),
          getAdminOrders(token),
          getAdminRestaurants(token)
        ]);

        const dashboardResponse = dashboardResult.status === "fulfilled" ? dashboardResult.value : null;
        const ordersResponse = ordersResult.status === "fulfilled" ? ordersResult.value : null;
        const restaurantsResponse = restaurantsResult.status === "fulfilled" ? restaurantsResult.value : null;

        const normalizedOrders = extractList(ordersResponse)
          .slice()
          .sort((left, right) => getOrderTime(right) - getOrderTime(left));

        const revenueFromOrders = normalizedOrders.reduce(
          (sum, order) => sum + toNumber(order?.total ?? order?.amount ?? order?.grandTotal),
          0
        );

        const totalRestaurants =
          toNumber(dashboardResponse?.totalRestaurants) || extractList(restaurantsResponse).length;
        const totalOrders =
          toNumber(dashboardResponse?.totalOrders) || toNumber(dashboardResponse?.ordersCount) || normalizedOrders.length;
        const totalUsers =
          toNumber(dashboardResponse?.totalUsers) || toNumber(dashboardResponse?.usersCount);
        const revenue =
          toNumber(dashboardResponse?.revenue) ||
          toNumber(dashboardResponse?.totalRevenue) ||
          revenueFromOrders;

        if (active) {
          setData({
            totalUsers,
            totalRestaurants,
            totalOrders,
            revenue
          });
          setOrders(normalizedOrders.slice(0, 5));
          if (dashboardResult.status === "rejected" && ordersResult.status === "rejected") {
            setError("Failed to load dashboard");
          } else {
            setError("");
          }
        }
      } catch (requestError) {
        if (active) {
          setData({
            totalUsers: 0,
            totalRestaurants: 0,
            totalOrders: 0,
            revenue: 0
          });
          setOrders([]);
          setError(requestError.message || "Failed to load dashboard");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    loadDashboard();
    return () => {
      active = false;
    };
  }, [token]);

  if (loading) {
    return <div className="page-loader">Loading dashboard...</div>;
  }

  return (
    <section className="admin-page admin-dashboard-page">
      <h1>Dashboard</h1>
      <p className="admin-page-subtitle">Welcome back, Super Admin</p>

      {error ? <div className="error-banner">{error}</div> : null}

      <div className="admin-cards-grid">
        <article className="admin-card admin-card-users">
          <div>
            <h3>Total Users</h3>
            <strong>{data?.totalUsers ?? 0}</strong>
          </div>
          <span className="admin-card-icon" aria-hidden="true">
            <DashboardIcon type="users" />
          </span>
        </article>
        <article className="admin-card admin-card-restaurants">
          <div>
            <h3>Total Restaurants</h3>
            <strong>{data?.totalRestaurants ?? 0}</strong>
          </div>
          <span className="admin-card-icon" aria-hidden="true">
            <DashboardIcon type="restaurants" />
          </span>
        </article>
        <article className="admin-card admin-card-orders">
          <div>
            <h3>Total Orders</h3>
            <strong>{data?.totalOrders ?? 0}</strong>
          </div>
          <span className="admin-card-icon" aria-hidden="true">
            <DashboardIcon type="orders" />
          </span>
        </article>
        <article className="admin-card admin-card-revenue">
          <div>
            <h3>Revenue</h3>
            <strong>{currency.format(data?.revenue ?? 0)}</strong>
          </div>
          <span className="admin-card-icon" aria-hidden="true">
            <DashboardIcon type="revenue" />
          </span>
        </article>
      </div>

      <div className="admin-table-wrap admin-dashboard-table-wrap">
        <div className="admin-dashboard-table-head">
          <h2>Recent Orders</h2>
        </div>
        <table className="admin-table">
          <thead>
            <tr>
              <th>Order ID</th>
              <th>Restaurant</th>
              <th>Total</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {orders.length === 0 ? (
              <tr>
                <td colSpan={4} className="admin-empty-cell">No data</td>
              </tr>
            ) : (
              orders.map((item, index) => {
                const status = item?.deliveryStatus || item?.status;
                return (
                  <tr key={`${item?.orderReference || item?.displayOrderId || "order"}-${index}`}>
                    <td>{item?.orderReference || item?.displayOrderId || "No data"}</td>
                    <td>{item?.restaurantName || item?.restaurant?.name || "No data"}</td>
                    <td>{currency.format(toNumber(item?.total ?? item?.amount ?? item?.grandTotal))}</td>
                    <td>
                      <span className={`admin-status-pill ${getStatusClass(status)}`}>
                        {getStatusLabel(status)}
                      </span>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}

