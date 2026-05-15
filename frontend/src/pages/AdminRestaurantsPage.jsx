import { useEffect, useState } from "react";
import { deleteAdminRestaurant, getAdminRestaurants } from "../lib/api";
import { useSession } from "../context/SessionContext";

export default function AdminRestaurantsPage() {
  const { token } = useSession();
  const [restaurants, setRestaurants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadRestaurants = async () => {
    try {
      const response = await getAdminRestaurants(token);
      setRestaurants(Array.isArray(response) ? response : []);
    } catch (requestError) {
      setError(requestError.message || "Failed to load restaurants");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRestaurants();
  }, [token]);

  const handleDelete = async (restaurantId) => {
    if (!window.confirm("Delete this restaurant?")) {
      return;
    }
    try {
      await deleteAdminRestaurant(token, restaurantId);
      setRestaurants((current) => current.filter((item) => item.id !== restaurantId));
    } catch (requestError) {
      setError(requestError.message || "Failed to delete restaurant");
    }
  };

  if (loading) {
    return <div className="page-loader">Loading restaurants...</div>;
  }

  return (
    <section className="admin-page">
      <h1>Restaurants</h1>
      <p className="admin-page-subtitle">Manage restaurant catalog and owners</p>
      {error ? <div className="error-banner">{error}</div> : null}

      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Category</th>
              <th>Area</th>
              <th>Rating</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {restaurants.map((item) => (
              <tr key={item.id}>
                <td>{item.name}</td>
                <td>{item.category}</td>
                <td>{item.area}</td>
                <td>{item.rating}</td>
                <td>
                  <button className="admin-danger-btn" type="button" onClick={() => handleDelete(item.id)}>
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

