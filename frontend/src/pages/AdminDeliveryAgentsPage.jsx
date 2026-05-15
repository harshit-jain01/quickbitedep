import { useEffect, useState } from "react";
import { deleteAdminUser, getAdminUsers, getDeliveryAgents } from "../lib/api";
import { useSession } from "../context/SessionContext";

function toLookupKey(value) {
  return String(value || "").trim().toUpperCase();
}

function normalizePhone(value) {
  return String(value || "").replace(/\D+/g, "");
}

function phoneLast10(value) {
  const digits = normalizePhone(value);
  return digits.length >= 10 ? digits.slice(-10) : digits;
}

function cleanValue(value) {
  if (value === null || value === undefined) {
    return "";
  }
  const normalized = String(value).trim();
  return normalized === "-" ? "" : normalized;
}

function pickFirstValue(source, keys) {
  if (!source || typeof source !== "object") {
    return "";
  }
  for (const key of keys) {
    const value = cleanValue(source?.[key]);
    if (value) {
      return value;
    }
  }
  return "";
}

function formatVehicle(vehicleType, vehicleNumber) {
  // Handle the case where we get "-" as placeholder values
  const hasValidType = vehicleType && vehicleType !== "-";
  const hasValidNumber = vehicleNumber && vehicleNumber !== "-";
  
  if (hasValidType && hasValidNumber) {
    return `${vehicleType} (${vehicleNumber})`;
  }
  if (hasValidType) {
    return vehicleType;
  }
  if (hasValidNumber) {
    return vehicleNumber;
  }
  return "No data";
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
  if (Array.isArray(payload?.agents)) {
    return payload.agents;
  }
  if (Array.isArray(payload?.users)) {
    return payload.users;
  }
  return [];
}

function normalizeRole(role) {
  if (typeof role !== "string") {
    return "";
  }
  return role.trim().toUpperCase().replace(/^ROLE_/, "");
}

function collectNormalizedRoles(role) {
  if (role === null || role === undefined) {
    return [];
  }

  if (Array.isArray(role)) {
    return role.flatMap((entry) => collectNormalizedRoles(entry));
  }

  if (typeof role === "object") {
    return collectNormalizedRoles(role.name || role.value || role.authority || role.role);
  }

  if (typeof role === "string") {
    return role
      .split(",")
      .map((entry) => normalizeRole(entry))
      .filter(Boolean);
  }

  return [];
}

function resolveUserRole(user) {
  return user?.role ?? user?.userRole ?? user?.authorities ?? user?.roles;
}

function isDeliveryAgentUser(user) {
  const normalizedRoles = collectNormalizedRoles(resolveUserRole(user));
  return normalizedRoles.includes("DELIVERY_AGENT") || normalizedRoles.includes("AGENT");
}

function normalizeAgentRows(agentResponse, usersResponse) {
  const agents = extractList(agentResponse);
  const users = extractList(usersResponse);
  const deliveryUsers = users.filter((item) => isDeliveryAgentUser(item));

  const usersByPhone = new Map(
    deliveryUsers
      .filter((item) => item?.phone)
      .map((item) => [phoneLast10(item.phone), item])
  );

  const uniqueAgents = new Map();

  agents.forEach((agent) => {
    const linkedUser = usersByPhone.get(phoneLast10(agent?.phone));

    // Be resilient to different backend payload keys
    const vehicleType = pickFirstValue(agent, ["vehicleType", "vehicle_type", "vehicle", "vehicleKind"])
      || pickFirstValue(linkedUser, ["vehicleType", "vehicle_type", "vehicle", "vehicleKind"]);
    const vehicleNumber = pickFirstValue(agent, ["vehicleNumber", "vehicleNo", "vehicle_number", "vehicle_no", "registrationNumber"])
      || pickFirstValue(linkedUser, ["vehicleNumber", "vehicleNo", "vehicle_number", "vehicle_no", "registrationNumber"]);
    
    const row = {
      id: agent?.id || linkedUser?.userId || "-",
      userId: linkedUser?.userId || linkedUser?.id || null,
      name: agent?.name || linkedUser?.fullName || "-",
      email: linkedUser?.email || "-",
      phone: agent?.phone || linkedUser?.phone || "-",
      vehicleType: vehicleType || "-",
      vehicleNumber: vehicleNumber || "-",
      active: Boolean(agent?.active),
      online: Boolean(agent?.online),
      completedDeliveries: Number(agent?.completedDeliveries || 0)
    };

    const normalizedPhone = phoneLast10(row.phone);
    const normalizedName = toLookupKey(row.name);

    // Prefer phone-based identity to collapse duplicate records that carry different IDs.
    const rowKey = normalizedPhone
      ? `PHONE:${normalizedPhone}`
      : toLookupKey(row.id) !== "-"
      ? `ID:${toLookupKey(row.id)}`
      : `NAME:${normalizedName}`;

    if (!uniqueAgents.has(rowKey)) {
      uniqueAgents.set(rowKey, { ...row, rowKey });
      return;
    }

    const existing = uniqueAgents.get(rowKey);
    uniqueAgents.set(rowKey, {
      ...existing,
        userId: existing.userId || row.userId,
      email: existing.email === "-" ? row.email : existing.email,
      vehicleType: existing.vehicleType === "-" ? row.vehicleType : existing.vehicleType,
      vehicleNumber: existing.vehicleNumber === "-" ? row.vehicleNumber : existing.vehicleNumber,
      active: existing.active || row.active,
      online: existing.online || row.online,
      completedDeliveries: Math.max(existing.completedDeliveries, row.completedDeliveries)
    });
  });

  deliveryUsers.forEach((item, index) => {
    const userId = item?.userId || item?.id || null;
    const normalizedPhone = phoneLast10(item?.phone);
    const phoneRowKey = normalizedPhone ? `PHONE:${normalizedPhone}` : null;

    let existingRowKey = phoneRowKey && uniqueAgents.has(phoneRowKey) ? phoneRowKey : null;
    if (!existingRowKey && userId) {
      const matchedEntry = Array.from(uniqueAgents.entries())
        .find(([, row]) => String(row?.userId || "") === String(userId));
      existingRowKey = matchedEntry?.[0] || null;
    }

    const rowFromUser = {
      id: userId || `USER-${index + 1}`,
      userId,
      name: item?.fullName || item?.name || "-",
      email: item?.email || "-",
      phone: item?.phone || "-",
      vehicleType: item?.vehicleType || "-",
      vehicleNumber: item?.vehicleNumber || "-",
      active: Boolean(item?.active),
      online: Boolean(item?.online),
      completedDeliveries: Number(item?.completedDeliveries || 0)
    };

    if (!existingRowKey) {
      const rowKey = phoneRowKey || (userId ? `USER:${String(userId).toUpperCase()}` : `USER:${index}`);
      uniqueAgents.set(rowKey, { ...rowFromUser, rowKey });
      return;
    }

    const existing = uniqueAgents.get(existingRowKey);
    uniqueAgents.set(existingRowKey, {
      ...existing,
      userId: existing.userId || rowFromUser.userId,
      name: existing.name === "-" ? rowFromUser.name : existing.name,
      email: existing.email === "-" ? rowFromUser.email : existing.email,
      phone: existing.phone === "-" ? rowFromUser.phone : existing.phone,
      vehicleType: existing.vehicleType === "-" ? rowFromUser.vehicleType : existing.vehicleType,
      vehicleNumber: existing.vehicleNumber === "-" ? rowFromUser.vehicleNumber : existing.vehicleNumber,
      active: existing.active || rowFromUser.active,
      online: existing.online || rowFromUser.online,
      completedDeliveries: Math.max(existing.completedDeliveries, rowFromUser.completedDeliveries)
    });
  });

  return Array.from(uniqueAgents.values());
}

export default function AdminDeliveryAgentsPage() {
  const { token } = useSession();
  const [agents, setAgents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [deletingUserId, setDeletingUserId] = useState("");

  const handleDeleteAgent = async (agentRow) => {
    const userId = agentRow?.userId;
    if (!userId) {
      setError("This delivery agent is not linked to a removable user record");
      return;
    }
    if (!window.confirm("Delete this delivery agent?")) {
      return;
    }

    try {
      setDeletingUserId(String(userId));
      await deleteAdminUser(token, userId);
      setAgents((current) => current.filter((item) => item.userId !== userId));
      setError("");
    } catch (requestError) {
      setError(requestError?.message || "Failed to delete delivery agent");
    } finally {
      setDeletingUserId("");
    }
  };

  useEffect(() => {
    let isActive = true;

    const loadAgents = async () => {
      try {
        const [agentResult, usersResult] = await Promise.allSettled([
          getDeliveryAgents(token),
          getAdminUsers(token)
        ]);

        const agentResponse = agentResult.status === "fulfilled" ? agentResult.value : [];
        const usersResponse = usersResult.status === "fulfilled" ? usersResult.value : [];

        if (isActive) {
          setAgents(normalizeAgentRows(agentResponse, usersResponse));
          if (agentResult.status === "rejected" && usersResult.status === "rejected") {
            setError("Failed to load delivery agents");
          } else {
            setError("");
          }
        }
      } catch (requestError) {
        if (isActive) {
          setError(requestError.message || "Failed to load delivery agents");
        }
      } finally {
        if (isActive) {
          setLoading(false);
        }
      }
    };

    loadAgents();
    return () => {
      isActive = false;
    };
  }, [token]);

  if (loading) {
    return <div className="page-loader">Loading delivery agents...</div>;
  }

  return (
    <section className="admin-page">
      <h1>Delivery Agents</h1>
      <p className="admin-page-subtitle">View all delivery partners and their current status</p>
      {error ? <div className="error-banner">{error}</div> : null}

      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Phone</th>
              <th>Vehicle</th>
              <th>Status</th>
              <th>Online</th>
              <th>Completed</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {agents.length === 0 ? (
              <tr>
                <td colSpan={8} className="admin-empty-cell">No data</td>
              </tr>
            ) : (
              agents.map((item) => (
                <tr key={item.rowKey || item.id}>
                  <td>{item.name}</td>
                  <td>{item.email}</td>
                  <td>{item.phone}</td>
                  <td>{formatVehicle(item.vehicleType, item.vehicleNumber)}</td>
                  <td>{item.active ? "active" : "inactive"}</td>
                  <td>{item.online ? "online" : "offline"}</td>
                  <td>{item.completedDeliveries}</td>
                  <td>
                    <button
                      type="button"
                      className="admin-danger-btn"
                      onClick={() => handleDeleteAgent(item)}
                      disabled={!item?.userId || deletingUserId === String(item.userId)}
                    >
                      {deletingUserId === String(item.userId) ? "Deleting..." : "Delete"}
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
