import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useSession } from "../context/SessionContext";

const navItems = [
  {
    to: "/admin",
    label: "Dashboard",
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <rect x="3" y="3" width="7" height="7" rx="1.5" />
        <rect x="14" y="3" width="7" height="7" rx="1.5" />
        <rect x="3" y="14" width="7" height="7" rx="1.5" />
        <rect x="14" y="14" width="7" height="7" rx="1.5" />
      </svg>
    ),
    end: true
  },
  {
    to: "/admin/customers",
    label: "Customers",
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="9" cy="8" r="3" />
        <path d="M4 18c0-2.6 2-4 5-4s5 1.4 5 4" />
        <circle cx="17" cy="9" r="2" />
        <path d="M15 18c.4-1.6 1.6-2.7 3.6-3" />
      </svg>
    )
  },
  {
    to: "/admin/owners",
    label: "Restaurant Owners",
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="9" cy="8" r="3" />
        <path d="M4 18c0-2.6 2-4 5-4s5 1.4 5 4" />
        <path d="M18 7l2 2-4 4-2 .5.5-2z" />
      </svg>
    )
  },
  {
    to: "/admin/agents",
    label: "Delivery Agents",
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="8" cy="8" r="3" />
        <path d="M3.5 18c0-2.8 2-4.5 4.5-4.5S12.5 15.2 12.5 18" />
        <rect x="13" y="14" width="8" height="4" rx="1" />
        <circle cx="15" cy="19" r="1.5" />
        <circle cx="19" cy="19" r="1.5" />
      </svg>
    )
  },
  {
    to: "/admin/restaurants",
    label: "Restaurants",
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M4 8h16" />
        <path d="M6 8V6h12v2" />
        <path d="M7 8v10h10V8" />
        <path d="M10 18v-4h4v4" />
      </svg>
    )
  },
  {
    to: "/admin/orders",
    label: "Orders",
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <rect x="5" y="4" width="14" height="16" rx="2" />
        <path d="M9 4.5h6" />
        <path d="M8.5 10h7" />
      </svg>
    )
  }
];

export default function AdminLayout() {
  const navigate = useNavigate();
  const { logout } = useSession();

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <div className="admin-brand">
          <span className="admin-brand-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <path d="M12 3l7 3v6c0 4.6-3 7.5-7 9-4-1.5-7-4.4-7-9V6l7-3z" />
            </svg>
          </span>
          <strong>QuickBite Admin</strong>
        </div>

        <nav className="admin-nav">
          {navItems.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end}>
              <span className="admin-nav-icon">{item.icon}</span>
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <button className="admin-logout" type="button" onClick={handleLogout}>
          <span className="admin-nav-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <path d="M10 5H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h4" />
              <path d="M13 16l4-4-4-4" />
              <path d="M8 12h9" />
            </svg>
          </span>
          Logout
        </button>
      </aside>

      <main className="admin-content">
        <Outlet />
      </main>
    </div>
  );
}

