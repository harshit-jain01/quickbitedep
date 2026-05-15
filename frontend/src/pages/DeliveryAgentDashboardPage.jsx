import { useCallback, useEffect, useMemo, useState } from "react";
import { useSession } from "../context/SessionContext";
import {
  acceptAssignedOrder,
  getCurrentDeliveryAgent,
  getAgentEarnings,
  getAssignedOrders,
  getDeliveryAgents,
  getOrders,
  markAssignedOrderDelivered,
  markAssignedOrderPickedUp,
  updateDeliveryAgentAvailability
} from "../lib/api";

const REFRESH_MS = 3000;

function normalizePhone(value) {
  return String(value || "").replace(/\D/g, "");
}

function phoneMatches(left, right) {
  if (!left || !right) {
    return false;
  }
  if (left === right) {
    return true;
  }
  return left.length >= 10 && right.length >= 10 && left.slice(-10) === right.slice(-10);
}

function getNextAction(status) {
  if (status === "PENDING_ASSIGNMENT" || status === "ASSIGNED") {
    return { nextStatus: "ACCEPTED", label: "Accept Order" };
  }
  if (status === "ACCEPTED") {
    return { nextStatus: "PICKED_UP", label: "Mark Picked Up" };
  }
  if (status === "PICKED_UP") {
    return { nextStatus: "DELIVERED", label: "Mark Delivered" };
  }
  if (status === "DELIVERED") {
    return null;
  }
  return { nextStatus: "ACCEPTED", label: "Accept Order" };
}

export default function DeliveryAgentDashboardPage() {
  const { token, user, logout } = useSession();
  const [isOnline, setIsOnline] = useState(false);
  const [agentId, setAgentId] = useState("");
  const [orders, setOrders] = useState([]);
  const [loadingOrders, setLoadingOrders] = useState(false);
  const [error, setError] = useState("");
  const [updatingOrder, setUpdatingOrder] = useState("");
  const [resolvingAgent, setResolvingAgent] = useState(false);
  const [useFallbackOrders, setUseFallbackOrders] = useState(false);
  const [completedDeliveries, setCompletedDeliveries] = useState(0);

  const resolveAgent = useCallback(async () => {
    if (!token) {
      return;
    }

    try {
      setResolvingAgent(true);
      const currentAgent = await getCurrentDeliveryAgent(token);
      if (currentAgent?.id) {
        setAgentId(currentAgent.id);
        setIsOnline(Boolean(currentAgent.online));
        setUseFallbackOrders(false);
        setError("");
        setResolvingAgent(false);
        return;
      }
    } catch {
      // Fall back to list matching for older tokens or deployments without /me support.
    }

    if (!user?.phone) {
      setAgentId("");
      setOrders([]);
      setError("Delivery partner phone is missing from your profile. Please update your phone number and login again.");
      setResolvingAgent(false);
      return;
    }

    try {
      const agents = await getDeliveryAgents(token);
      const normalizedPhone = normalizePhone(user.phone);
      const matchedAgent = Array.isArray(agents)
        ? agents.find((agent) => {
          const agentPhone = normalizePhone(agent?.phone);
          if (phoneMatches(agentPhone, normalizedPhone)) {
            return true;
          }

          if (user?.fullName && agent?.name) {
            return String(agent.name).trim().toLowerCase() === String(user.fullName).trim().toLowerCase();
          }

          return false;
        })
        : null;

      if (!matchedAgent?.id) {
        setAgentId("");
        setOrders([]);
        setError("Delivery partner profile not found. Please register as a delivery agent first.");
        return;
      }

      setAgentId(matchedAgent.id);
      setIsOnline(Boolean(matchedAgent.online));
      setUseFallbackOrders(false);
      setError("");
    } catch (requestError) {
      if ((requestError.message || "").toLowerCase() === "forbidden") {
        // Some deployments keep `/api/v1/agents` admin-only; fallback to order-service feed.
        setUseFallbackOrders(true);
        setAgentId("");
        setError("");
        return;
      }
      setError(requestError.message || "Failed to load delivery profile");
    } finally {
      setResolvingAgent(false);
    }
  }, [token, user?.phone, user?.fullName]);

  const loadOrders = useCallback(async () => {
    if (!token || (!useFallbackOrders && !agentId)) {
      return;
    }

    try {
      setLoadingOrders(true);
      if (useFallbackOrders) {
        const data = await getOrders(token);
        const nextOrders = (Array.isArray(data) ? data : [])
          .filter((order) => order?.deliveryStatus !== "DELIVERED")
          .map((order) => ({
            orderReference: order.orderReference,
            restaurantName: order.restaurantName,
            deliveryAddress: order.deliveryAddress,
            assignmentStatus: order.deliveryStatus || "PENDING_ASSIGNMENT",
            deliveryAgentId: order.deliveryAgentId
          }));
        setOrders(nextOrders);
      } else {
        const data = await getAssignedOrders(token, agentId);
        const nextOrders = Array.isArray(data) ? data : [];
        setOrders(nextOrders.filter((order) => order?.assignmentStatus !== "DELIVERED"));
        const earnings = await getAgentEarnings(token, agentId);
        setCompletedDeliveries(Number(earnings?.completedDeliveries || 0));
      }
      setError("");
    } catch (requestError) {
      setError(requestError.message || "Failed to load orders");
    } finally {
      setLoadingOrders(false);
    }
  }, [token, agentId, useFallbackOrders]);

  useEffect(() => {
    resolveAgent();
  }, [resolveAgent]);

  useEffect(() => {
    loadOrders();
  }, [loadOrders]);

  useEffect(() => {
    if (!isOnline) {
      return;
    }

    const timer = setInterval(() => {
      loadOrders();
    }, REFRESH_MS);

    return () => clearInterval(timer);
  }, [isOnline, loadOrders]);

  const stats = useMemo(() => {
    const activeCount = orders.length;
    const inTransitCount = orders.filter((order) => order?.assignmentStatus === "PICKED_UP").length;
    const pickedUpCount = completedDeliveries + inTransitCount;
    const preparingCount = orders.filter(
      (order) => order?.assignmentStatus !== "PICKED_UP" && order?.assignmentStatus !== "DELIVERED"
    ).length;

    return {
      activeCount,
      pickedUpCount,
      preparingCount
    };
  }, [orders, completedDeliveries]);

  const showBlockingLoader = loadingOrders && orders.length === 0;

  const handleAdvanceStatus = async (orderReference, currentStatus, assignedAgentId) => {
    const transition = getNextAction(currentStatus);
    if (!transition) {
      return;
    }
    const effectiveAgentId = assignedAgentId || agentId;
    if (!effectiveAgentId) {
      setError("Delivery agent is not assigned yet");
      return;
    }

    try {
      setUpdatingOrder(orderReference);
      if (transition.nextStatus === "DELIVERED") {
        await markAssignedOrderDelivered(token, effectiveAgentId, orderReference);
        setOrders((current) => current.filter((order) => order.orderReference !== orderReference));
      } else if (transition.nextStatus === "PICKED_UP") {
        await markAssignedOrderPickedUp(token, effectiveAgentId, orderReference);
        setOrders((current) =>
          current.map((order) =>
            order.orderReference === orderReference
              ? { ...order, assignmentStatus: "PICKED_UP", deliveryAgentId: effectiveAgentId }
              : order
          )
        );
      } else {
        await acceptAssignedOrder(token, effectiveAgentId, orderReference);
        setOrders((current) =>
          current.map((order) =>
            order.orderReference === orderReference
              ? { ...order, assignmentStatus: "ACCEPTED", deliveryAgentId: effectiveAgentId }
              : order
          )
        );
      }
      await loadOrders();
    } catch (requestError) {
      setError(requestError.message || "Failed to update order status");
    } finally {
      setUpdatingOrder("");
    }
  };

  const handleToggleOnline = async () => {
    if (useFallbackOrders && !agentId) {
      setIsOnline((current) => !current);
      if (!isOnline) {
        await loadOrders();
      }
      return;
    }

    if (!agentId) {
      setError("Delivery partner profile not found");
      return;
    }

    const nextOnline = !isOnline;
    try {
      await updateDeliveryAgentAvailability(token, agentId, nextOnline);
      setIsOnline(nextOnline);
      if (nextOnline) {
        await loadOrders();
      } else {
        setOrders([]);
      }
      setError("");
    } catch (requestError) {
      setError(requestError.message || "Failed to update availability");
    }
  };

  return (
    <div className="delivery-agent-shell">
      <header className="delivery-agent-topbar">
        <div>
          <p className="eyebrow">Delivery Partner</p>
          <h1>Agent Dashboard</h1>
        </div>
        <div className="delivery-agent-topbar-actions">
          <button
            type="button"
            className={`agent-status-toggle ${isOnline ? "online" : "offline"}`}
              onClick={handleToggleOnline}
              disabled={(!agentId && !useFallbackOrders) || resolvingAgent}
          >
            {isOnline ? "Online" : "Offline"}
          </button>
          <button type="button" className="secondary-button" onClick={loadOrders} disabled={!isOnline || loadingOrders}>
            {loadingOrders ? "Refreshing..." : "Refresh"}
          </button>
          <button type="button" className="secondary-button" onClick={logout}>
            Logout
          </button>
        </div>
      </header>

      <main className="delivery-agent-content">
        <section className="agent-stats-grid">
          <article className="agent-stat-card">
            <p>{stats.activeCount}</p>
            <span>Live Orders</span>
          </article>
          <article className="agent-stat-card">
            <p>{stats.preparingCount}</p>
            <span>Pending Pickup</span>
          </article>
          <article className="agent-stat-card">
            <p>{stats.pickedUpCount}</p>
            <span>Picked Up</span>
          </article>
        </section>

        <section className="agent-orders-section">
          <div className="agent-section-head">
            <h2>Assigned Orders</h2>
            <small>Welcome, {user?.fullName || "Delivery Agent"}</small>
          </div>

          {resolvingAgent ? <div className="empty-state">Loading delivery profile...</div> : null}

          {!resolvingAgent && !agentId && !useFallbackOrders ? <div className="empty-state">Delivery partner profile not found.</div> : null}

          {useFallbackOrders ? <div className="empty-state">Using fallback order feed.</div> : null}

          {!resolvingAgent && agentId && !isOnline ? <div className="empty-state">Go online to receive orders.</div> : null}

          {(agentId || useFallbackOrders) && isOnline && showBlockingLoader ? <div className="empty-state">Loading orders...</div> : null}

          {error ? <div className="error-banner">{error}</div> : null}

          {(agentId || useFallbackOrders) && isOnline && !showBlockingLoader && orders.length === 0 ? <div className="empty-state">No active customer orders right now.</div> : null}

          {(agentId || useFallbackOrders) && isOnline && orders.length > 0 ? (
            <div className="agent-orders-grid">
              {orders.map((order) => {
                const transition = getNextAction(order.assignmentStatus);
                return (
                  <article key={order.orderReference} className="agent-order-card">
                    <div className="agent-order-head">
                      <strong>{order.orderReference}</strong>
                      <span className="agent-order-status">{(order.assignmentStatus || "ASSIGNED").replace("_", " ")}</span>
                    </div>
                    <p><strong>Restaurant:</strong> {order.restaurantName}</p>
                    <p><strong>Drop:</strong> {order.deliveryAddress}</p>
                    <button
                      type="button"
                      onClick={() => handleAdvanceStatus(order.orderReference, order.assignmentStatus, order.deliveryAgentId)}
                      disabled={!transition || updatingOrder === order.orderReference}
                    >
                      {updatingOrder === order.orderReference ? "Updating..." : transition?.label || "Delivered"}
                    </button>
                  </article>
                );
              })}
            </div>
          ) : null}
        </section>
      </main>
    </div>
  );
}

