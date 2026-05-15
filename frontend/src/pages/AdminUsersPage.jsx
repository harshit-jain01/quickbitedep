import { useEffect, useState } from "react";
import { deleteAdminUser, getAdminUsers } from "../lib/api";
import { useSession } from "../context/SessionContext";

const ROLE_ALIASES = {
  CUSTOMER: ["CUSTOMER", "USER"],
  RESTAURANT_OWNER: ["RESTAURANT_OWNER", "OWNER", "RESTAURANTOWNER"]
};

function normalizeRole(role) {
  if (role && typeof role === "object") {
    const nestedRole = role.name || role.value || role.authority;
    return normalizeRole(nestedRole);
  }

  if (Array.isArray(role) && role.length > 0) {
    return normalizeRole(role[0]);
  }

  if (typeof role !== "string") {
    return "";
  }
  return role.trim().toUpperCase().replace(/^ROLE_/, "");
}

function resolveUserRole(user) {
  return user?.role ?? user?.userRole ?? user?.authorities ?? user?.roles;
}

function roleMatches(user, filterRole) {
  const normalizedUserRole = normalizeRole(resolveUserRole(user));
  const normalizedFilter = normalizeRole(filterRole);
  const acceptedRoles = ROLE_ALIASES[normalizedFilter] || [normalizedFilter];
  return acceptedRoles.includes(normalizedUserRole);
}

export default function AdminUsersPage({ filterRole = "CUSTOMER", title = "Customers", subtitle = "Manage all registered users" }) {
  const { token, user } = useSession();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadUsers = async () => {
    try {
      const response = await getAdminUsers(token);
      const allUsers = Array.isArray(response) ? response : [];
      setUsers(allUsers.filter((item) => roleMatches(item, filterRole)));
    } catch (requestError) {
      setError(requestError.message || "Failed to load users");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
  }, [token, filterRole]);

  const handleDelete = async (userId) => {
    if (!window.confirm("Delete this user?")) {
      return;
    }
    try {
      await deleteAdminUser(token, userId);
      setUsers((current) => current.filter((item) => item.userId !== userId));
    } catch (requestError) {
      setError(requestError.message || "Failed to delete user");
    }
  };

  if (loading) {
    return <div className="page-loader">Loading users...</div>;
  }

  return (
    <section className="admin-page">
      <h1>{title}</h1>
      <p className="admin-page-subtitle">{subtitle}</p>
      {error ? <div className="error-banner">{error}</div> : null}

      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Phone</th>
              <th>Role</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map((item) => (
              <tr key={item.userId}>
                <td>{item.fullName}</td>
                <td>{item.email}</td>
                <td>{item.phone || "-"}</td>
                <td>{item.role}</td>
                <td>{item.active ? "active" : "blocked"}</td>
                <td>
                  <button
                    className="admin-danger-btn"
                    type="button"
                    disabled={item.email?.toLowerCase() === user?.email?.toLowerCase()}
                    onClick={() => handleDelete(item.userId)}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

