import { useEffect, useState } from "react";
import { getAdminOwners, getAdminUsers } from "../lib/api";
import { useSession } from "../context/SessionContext";

function formatDate(value) {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }
  return date.toLocaleDateString();
}

export default function AdminOwnersPage() {
  const { token } = useSession();
  const [owners, setOwners] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let isActive = true;

    const loadOwners = async () => {
      try {
        const [ownersResponse, usersResponse] = await Promise.all([
          getAdminOwners(token),
          getAdminUsers(token)
        ]);
        const usersById = new Map(
          (Array.isArray(usersResponse) ? usersResponse : [])
            .filter((user) => user?.userId)
            .map((user) => [String(user.userId), user])
        );
        const mergedOwners = (Array.isArray(ownersResponse) ? ownersResponse : []).map((owner) => {
          const authUser = usersById.get(String(owner?.userId || ""));
          return {
            ...owner,
            email: owner?.email || authUser?.email || "",
            phone: owner?.phone || authUser?.phone || ""
          };
        });
        if (isActive) {
          setOwners(mergedOwners);
        }
      } catch (requestError) {
        if (isActive) {
          setError(requestError.message || "Failed to load restaurant owners");
        }
      } finally {
        if (isActive) {
          setLoading(false);
        }
      }
    };

    loadOwners();
    return () => {
      isActive = false;
    };
  }, [token]);

  if (loading) {
    return <div className="page-loader">Loading restaurant owners...</div>;
  }

  return (
    <section className="admin-page">
      <h1>Restaurant Owners</h1>
      <p className="admin-page-subtitle">Manage onboarding and access for restaurant owners</p>
      {error ? <div className="error-banner">{error}</div> : null}

      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Restaurant</th>
              <th>Cuisine</th>
              <th>Address</th>
              <th>Email</th>
              <th>Phone</th>
              <th>Status</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {owners.map((item) => (
              <tr key={item.ownerId || item.userId}>
                <td>{item.restaurantName || "-"}</td>
                <td>{item.cuisineType || "-"}</td>
                <td>{item.address || "-"}</td>
                <td>{item.email || "-"}</td>
                <td>{item.phone || "-"}</td>
                <td>{item.active ? "active" : "inactive"}</td>
                <td>{formatDate(item.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

