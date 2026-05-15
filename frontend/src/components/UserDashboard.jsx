import { useEffect, useMemo, useState } from "react";
import { getOrders } from "../lib/api";

const currency = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 0
});

function toNumber(value) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function pickNumber(...values) {
  for (const value of values) {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) {
      return parsed;
    }
  }
  return null;
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
  if (normalized === "PICKED_UP" || normalized === "OUT_FOR_DELIVERY") {
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
  if (normalized === "PICKED_UP" || normalized === "OUT_FOR_DELIVERY") {
    return "Out for Delivery";
  }
  if (!normalized) {
    return "No data";
  }
  return "Preparing";
}

function getOrderTime(order) {
  const timeSource = order?.createdAt || order?.created_at || order?.orderedAt || order?.orderDate || order?.placedAt;
  const time = timeSource ? new Date(timeSource).getTime() : 0;
  return Number.isFinite(time) ? time : 0;
}

function extractOrders(payload) {
  if (Array.isArray(payload)) {
    return payload;
  }
  if (Array.isArray(payload?.orders)) {
    return payload.orders;
  }
  if (Array.isArray(payload?.data)) {
    return payload.data;
  }
  if (Array.isArray(payload?.content)) {
    return payload.content;
  }
  return [];
}

function getOrderAmount(order) {
  return toNumber(order?.total ?? order?.amount ?? order?.grandTotal);
}

function getRestaurantName(order) {
  return order?.restaurantName || order?.restaurant?.name || "No data";
}

function StatIcon({ children }) {
  return (
    <span className="user-dashboard-icon" aria-hidden="true">
      {children}
    </span>
  );
}

function DashboardIcon({ name }) {
  if (name === "orders") {
    return (
      <svg viewBox="0 0 24 24" focusable="false">
        <rect x="4" y="5" width="16" height="15" rx="3" />
        <path d="M8 9h8M8 13h8M8 17h5" />
      </svg>
    );
  }

  if (name === "spent") {
    return (
      <svg viewBox="0 0 24 24" focusable="false">
        <path d="M12 4v16" />
        <path d="M16.5 8.5c0-1.7-1.8-3-4-3s-4 1.3-4 3 1.8 3 4 3 4 1.3 4 3-1.8 3-4 3-4-1.3-4-3" />
      </svg>
    );
  }

  if (name === "active") {
    return (
      <svg viewBox="0 0 24 24" focusable="false">
        <circle cx="12" cy="12" r="8" />
        <path d="M12 8v4l2.8 2.8" />
      </svg>
    );
  }

  return (
    <svg viewBox="0 0 24 24" focusable="false">
      <path d="M5 7h14" />
      <path d="M7 4h10v16H7z" />
      <path d="M9.5 14.5h5" />
    </svg>
  );
}


export default function UserDashboard({ token }) {
  const [orders, setOrders] = useState([]);
  const [summary, setSummary] = useState({
    totalOrders: null,
    totalSpent: null
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    const loadOrders = async () => {
      try {
        setLoading(true);
        const response = await getOrders(token);
        if (!active) {
          return;
        }
        setOrders(extractOrders(response));
        setSummary({
          totalOrders: pickNumber(response?.totalOrders, response?.summary?.totalOrders),
          totalSpent: pickNumber(
            response?.totalSpent,
            response?.totalAmount,
            response?.summary?.totalSpent,
            response?.summary?.totalAmount
          )
        });
        setError("");
      } catch (requestError) {
        if (!active) {
          return;
        }
        setOrders([]);
        setSummary({
          totalOrders: null,
          totalSpent: null
        });
        setError(requestError?.message || "Unable to load dashboard data");
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    if (token) {
      loadOrders();
    } else {
      setOrders([]);
      setSummary({
        totalOrders: null,
        totalSpent: null
      });
      setLoading(false);
    }

    return () => {
      active = false;
    };
  }, [token]);

  const sortedOrders = useMemo(
    () => [...orders].sort((left, right) => getOrderTime(right) - getOrderTime(left)),
    [orders]
  );

  const metrics = useMemo(() => {
    const derivedTotalOrders = sortedOrders?.length || 0;
    const derivedTotalSpent = sortedOrders.reduce((sum, order) => sum + getOrderAmount(order), 0);
    const activeOrders = sortedOrders.filter(
      (order) => normalizeStatus(order?.deliveryStatus || order?.status) !== "DELIVERED"
    ).length;
    const lastOrderAmount = getOrderAmount(sortedOrders?.[0]);

    return {
      totalOrders: summary?.totalOrders ?? derivedTotalOrders,
      totalSpent: summary?.totalSpent ?? derivedTotalSpent,
      activeOrders,
      lastOrderAmount
    };
  }, [sortedOrders, summary]);

  const recentOrders = sortedOrders.slice(0, 6);

  const dashboardCards = [
    {
      key: "orders",
      title: "Total Orders",
      value: metrics?.totalOrders ?? 0,
      cardClass: "card-orders",
      iconName: "orders"
    },
    {
      key: "spent",
      title: "Total Spent",
      value: currency.format(metrics?.totalSpent ?? 0),
      cardClass: "card-spent",
      iconName: "spent"
    },
    {
      key: "active",
      title: "Active Orders",
      value: metrics?.activeOrders ?? 0,
      cardClass: "card-active",
      iconName: "active"
    },
    {
      key: "last",
      title: "Last Order Amount",
      value: currency.format(metrics?.lastOrderAmount ?? 0),
      cardClass: "card-last",
      iconName: "last"
    }
  ];

  return (
    <section className="user-dashboard">
      <div className="user-dashboard-header">
        <h2>Dashboard</h2>
        <p>Welcome back. Here is your latest order overview.</p>
      </div>

      {loading ? (
        <div className="user-dashboard-loading">
          <div className="user-dashboard-spinner" />
          <span>Loading your dashboard...</span>
        </div>
      ) : null}

      {!loading ? (
        <>
          {error ? <div className="error-banner">{error}</div> : null}

          <div className="user-dashboard-cards">
            {dashboardCards.map((card) => (
              <article className={`user-dashboard-card ${card.cardClass}`} key={card.key}>
                <div className="user-dashboard-card-head">
                  <div>
                    <h3>{card.title}</h3>
                    <strong>{card.value}</strong>
                  </div>
                  <StatIcon>
                    <DashboardIcon name={card.iconName} />
                  </StatIcon>
                </div>
              </article>
            ))}
          </div>

          <div className="user-dashboard-table-wrap">
            <div className="user-dashboard-table-head">
              <h3>Recent Orders</h3>
            </div>

            {recentOrders.length === 0 ? (
              <div className="empty-state">No data</div>
            ) : (
              <div className="user-dashboard-table-scroll">
                <table className="user-dashboard-table">
                  <thead>
                    <tr>
                      <th>Order ID</th>
                      <th>Restaurant Name</th>
                      <th>Amount</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {recentOrders.map((order, index) => {
                      const orderStatus = order?.deliveryStatus || order?.status;
                      return (
                        <tr key={`${order?.orderReference || order?.displayOrderId || "order"}-${index}`}>
                          <td>{order?.displayOrderId || order?.orderReference || "No data"}</td>
                          <td>{getRestaurantName(order)}</td>
                          <td>{currency.format(getOrderAmount(order))}</td>
                          <td>
                            <span className={`user-dashboard-status ${getStatusClass(orderStatus)}`}>
                              {getStatusLabel(orderStatus)}
                            </span>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      ) : null}
    </section>
  );
}

