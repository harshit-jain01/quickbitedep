import { Navigate, useLocation } from "react-router-dom";
import { useSession } from "../context/SessionContext";

export default function RequireAuth({ children, requireRole = null }) {
  const location = useLocation();
  const { token, loading, user } = useSession();

  if (loading) {
    return <div className="page-loader">Loading your QuickBite feed...</div>;
  }

  if (!token) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (requireRole && user?.role !== requireRole) {
    return <Navigate to="/home" replace />;
  }

  if (requireRole === "ADMIN" && user?.email?.toLowerCase() !== "admin123@gmail.com") {
    return <Navigate to="/home" replace />;
  }

  return children;
}
