import { Navigate, Route, Routes } from "react-router-dom";
import { SessionProvider } from "./context/SessionContext";
import { CartProvider } from "./context/CartContext";
import RequireAuth from "./components/RequireAuth";
import AdminLayout from "./components/AdminLayout";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import DeliveryAgentRegisterPage from "./pages/DeliveryAgentRegisterPage";
import OAuthCallbackPage from "./pages/OAuthCallbackPage";
import RestaurantPage from "./pages/RestaurantPage";
import CheckoutPage from "./pages/CheckoutPage";
import AccountPage from "./pages/AccountPage";
import RestaurantDashboard from "./pages/RestaurantDashboard";
import TrackingPage from "./pages/TrackingPage";
import OrderConfirmationPage from "./pages/OrderConfirmationPage";
import AdminDashboardPage from "./pages/AdminDashboardPage";
import AdminUsersPage from "./pages/AdminUsersPage";
import AdminOwnersPage from "./pages/AdminOwnersPage";
import AdminDeliveryAgentsPage from "./pages/AdminDeliveryAgentsPage";
import AdminRestaurantsPage from "./pages/AdminRestaurantsPage";
import AdminOrdersPage from "./pages/AdminOrdersPage";
import DeliveryAgentDashboardPage from "./pages/DeliveryAgentDashboardPage";

export default function App() {
  return (
    <SessionProvider>
      <CartProvider>
        <Routes>
          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/delivery-agent/register" element={<DeliveryAgentRegisterPage />} />
          <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
          <Route
            path="/home"
            element={
              <RequireAuth>
                <HomePage />
              </RequireAuth>
            }
          />
          <Route
            path="/restaurants/:restaurantId"
            element={
              <RequireAuth>
                <RestaurantPage />
              </RequireAuth>
            }
          />
          <Route
            path="/checkout"
            element={
              <RequireAuth>
                <CheckoutPage />
              </RequireAuth>
            }
          />
          <Route
            path="/account"
            element={
              <RequireAuth>
                <AccountPage />
              </RequireAuth>
            }
          />
          <Route
            path="/restaurant/dashboard"
            element={
              <RequireAuth requireRole="RESTAURANT_OWNER">
                <RestaurantDashboard />
              </RequireAuth>
            }
          />
          <Route
            path="/delivery-agent/dashboard"
            element={
              <RequireAuth requireRole="AGENT">
                <DeliveryAgentDashboardPage />
              </RequireAuth>
            }
          />
          <Route
            path="/agent"
            element={<Navigate to="/delivery-agent/dashboard" replace />}
          />
          <Route
            path="/tracking/:orderId"
            element={
              <RequireAuth>
                <TrackingPage />
              </RequireAuth>
            }
          />
          <Route
            path="/admin"
            element={
              <RequireAuth requireRole="ADMIN">
                <AdminLayout />
              </RequireAuth>
            }
          >
            <Route index element={<AdminDashboardPage />} />
            <Route
              path="customers"
              element={
                <AdminUsersPage
                  filterRole="CUSTOMER"
                  title="Customers"
                  subtitle="Manage all registered customer accounts"
                />
              }
            />
            <Route
              path="owners"
              element={<AdminOwnersPage />}
            />
            <Route path="agents" element={<AdminDeliveryAgentsPage />} />
            <Route path="users" element={<Navigate to="/admin/customers" replace />} />
            <Route path="restaurants" element={<AdminRestaurantsPage />} />
            <Route path="orders" element={<AdminOrdersPage />} />
          </Route>
          <Route
            path="/order-confirmation"
            element={
              <RequireAuth>
                <OrderConfirmationPage />
              </RequireAuth>
            }
          />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </CartProvider>
    </SessionProvider>
  );
}
