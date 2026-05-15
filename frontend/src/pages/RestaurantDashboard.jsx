import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useSession } from "../context/SessionContext";
import { getRestaurantOwnerProfile, registerRestaurant } from "../lib/api";
import MenuManagement from "../components/MenuManagement";
import OrdersManagement from "../components/OrdersManagement";
import ReviewsManagement from "../components/ReviewsManagement";

export default function RestaurantDashboard() {
  const navigate = useNavigate();
  const { token, user, logout } = useSession();
  const [activeTab, setActiveTab] = useState("menu");
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showRegistration, setShowRegistration] = useState(false);
  const [registrationForm, setRegistrationForm] = useState({
    restaurantName: "",
    cuisineType: "",
    address: "",
    imageFile: null
  });
  const [registrationError, setRegistrationError] = useState("");
  const [isRegistering, setIsRegistering] = useState(false);

  useEffect(() => {
    if (user?.role !== "RESTAURANT_OWNER") {
      navigate("/home", { replace: true });
      return;
    }

    loadProfile();
  }, [token, user, navigate]);

  const loadProfile = async () => {
    try {
      const data = await getRestaurantOwnerProfile(token);
      setProfile(data);
      setShowRegistration(false);
    } catch (err) {
      // If 404, restaurant not yet registered
      if (err.message.includes("not found")) {
        setShowRegistration(true);
        setError("");
      } else {
        setError(err.message);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleRegistrationChange = (e) => {
    const { name, value } = e.target;
    setRegistrationForm(prev => ({ ...prev, [name]: value }));
    setRegistrationError("");
  };

  const handleRegistrationImageChange = (e) => {
    const file = e.target.files && e.target.files.length > 0 ? e.target.files[0] : null;
    setRegistrationForm(prev => ({ ...prev, imageFile: file }));
    setRegistrationError("");
  };

  const handleRegistrationSubmit = async (e) => {
    e.preventDefault();

    if (!registrationForm.restaurantName.trim() || !registrationForm.cuisineType.trim() || !registrationForm.address.trim() || !registrationForm.imageFile) {
      setRegistrationError("All fields are required, including restaurant image");
      return;
    }

    setIsRegistering(true);
    setRegistrationError("");

    try {
      await registerRestaurant(token, {
        restaurantName: registrationForm.restaurantName,
        cuisineType: registrationForm.cuisineType,
        address: registrationForm.address,
        imageFile: registrationForm.imageFile
      });
      await loadProfile();
    } catch (err) {
      setRegistrationError(err.message);
    } finally {
      setIsRegistering(false);
    }
  };

  if (loading) {
    return <div className="page-loader">Loading dashboard...</div>;
  }

  if (showRegistration) {
    return (
      <div className="restaurant-dashboard">
        <header className="dashboard-header">
          <div className="header-content">
            <div className="restaurant-info">
              <h1>Register Your Restaurant</h1>
              <p>Complete your restaurant registration to start managing your menu</p>
            </div>
            <div className="header-actions">
              <button
                className="secondary-button"
                onClick={() => {
                  logout();
                  navigate("/login", { replace: true });
                }}
              >
                Logout
              </button>
            </div>
          </div>
        </header>

        <div className="registration-container" style={{ padding: "2rem", maxWidth: "500px", margin: "2rem auto" }}>
          <form onSubmit={handleRegistrationSubmit} className="auth-form">
            {registrationError && <div className="error-banner">{registrationError}</div>}

            <label className="field">
              <span>Restaurant Name *</span>
              <input
                name="restaurantName"
                type="text"
                placeholder="e.g., Taj Mahal Express"
                value={registrationForm.restaurantName}
                onChange={handleRegistrationChange}
                required
              />
            </label>

            <label className="field">
              <span>Cuisine Type *</span>
              <input
                name="cuisineType"
                type="text"
                placeholder="e.g., Indian, Italian, Chinese"
                value={registrationForm.cuisineType}
                onChange={handleRegistrationChange}
                required
              />
            </label>

            <label className="field">
              <span>Address *</span>
              <textarea
                name="address"
                placeholder="e.g., 123 Main St, Downtown"
                value={registrationForm.address}
                onChange={handleRegistrationChange}
                required
                style={{ minHeight: "100px" }}
              />
            </label>

            <label className="field">
              <span>Restaurant Image *</span>
              <input
                name="imageFile"
                type="file"
                accept="image/*"
                onChange={handleRegistrationImageChange}
                required
              />
              {registrationForm.imageFile ? <small>{registrationForm.imageFile.name}</small> : null}
            </label>

            <button className="primary-button" type="submit" disabled={isRegistering}>
              {isRegistering ? "Registering..." : "Register Restaurant"}
            </button>
          </form>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="error-banner" style={{ margin: "2rem" }}>
        {error}
      </div>
    );
  }

  return (
    <div className="restaurant-dashboard">
      <header className="dashboard-header">
        <div className="header-content">
          <div className="restaurant-info">
            <h1>{profile?.restaurantName || user?.fullName}</h1>
            <p>{profile?.cuisineType || "Restaurant"}</p>
          </div>
          <div className="header-actions">
            <button
              className="secondary-button"
              onClick={() => {
                logout();
                navigate("/login", { replace: true });
              }}
            >
              Logout
            </button>
          </div>
        </div>
      </header>

      <nav className="dashboard-nav">
        <button
          className={`nav-item ${activeTab === "menu" ? "active" : ""}`}
          onClick={() => setActiveTab("menu")}
        >
          📋 Menu Management
        </button>
        <button
          className={`nav-item ${activeTab === "orders" ? "active" : ""}`}
          onClick={() => setActiveTab("orders")}
        >
          📦 Orders
        </button>
        <button
          className={`nav-item ${activeTab === "reviews" ? "active" : ""}`}
          onClick={() => setActiveTab("reviews")}
        >
          ⭐ Reviews
        </button>
      </nav>

      <div className="dashboard-content">
        {activeTab === "menu" && profile && (
          <div className="tab-content">
            <h2>Manage Your Menu</h2>
            <MenuManagement
              token={token}
              restaurantId={profile.restaurantId}
            />
          </div>
        )}

        {activeTab === "orders" && (
          <div className="tab-content">
            <h2>Incoming Orders</h2>
            <OrdersManagement token={token} />
          </div>
        )}

        {activeTab === "reviews" && (
          <div className="tab-content">
            <h2>Customer Reviews</h2>
            <ReviewsManagement token={token} />
          </div>
        )}
      </div>
    </div>
  );
}


