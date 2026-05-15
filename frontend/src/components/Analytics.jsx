import { useState, useEffect } from "react";
import { getRestaurantOwnerAnalytics } from "../lib/api";

export default function Analytics({ token }) {
  const [analytics, setAnalytics] = useState(null);
  const [period, setPeriod] = useState("daily");
  const [error, setError] = useState("");

  useEffect(() => {
    loadAnalytics();
  }, [token, period]);

  const loadAnalytics = async () => {
    try {
      const data = await getRestaurantOwnerAnalytics(token, { period });
      setAnalytics(data || {});
    } catch (err) {
      setError(err.message);
    }
  };

  if (!analytics) {
    return <div className="page-loader">Loading analytics...</div>;
  }

  return (
    <div className="analytics-section">
      {error && <div className="error-banner">{error}</div>}

      <div className="analytics-controls">
        <label>Time Period:</label>
        <select value={period} onChange={(e) => setPeriod(e.target.value)}>
          <option value="daily">Daily</option>
          <option value="weekly">Weekly</option>
          <option value="monthly">Monthly</option>
        </select>
      </div>

      <div className="analytics-grid">
        <div className="metric-card">
          <h4>Total Revenue</h4>
          <p className="metric-value">₹{(analytics.totalRevenue || 0).toFixed(0)}</p>
        </div>

        <div className="metric-card">
          <h4>Total Orders</h4>
          <p className="metric-value">{analytics.totalOrders || 0}</p>
        </div>

        <div className="metric-card">
          <h4>Average Order Value</h4>
          <p className="metric-value">₹{(analytics.averageOrderValue || 0).toFixed(0)}</p>
        </div>

        <div className="metric-card">
          <h4>Peak Hours</h4>
          <p className="metric-value">{analytics.peakHours || "N/A"}</p>
        </div>
      </div>

      <div className="top-items-section">
        <h3>Top Selling Items</h3>
        {analytics.topSellingItems?.length > 0 ? (
          <div className="items-list">
            {analytics.topSellingItems.map((item, index) => (
              <div key={index} className="item-stat">
                <span>{item.itemName}</span>
                <div className="item-stats">
                  <span className="quantity">x {item.quantity} sold</span>
                  <span className="revenue">₹{(item.revenue || 0).toFixed(0)}</span>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p className="empty-state">No sales data available</p>
        )}
      </div>
    </div>
  );
}

