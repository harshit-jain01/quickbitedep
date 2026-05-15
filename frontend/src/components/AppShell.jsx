import { useState } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { useSession } from "../context/SessionContext";

export default function AppShell({ title, subtitle, children }) {
  const navigate = useNavigate();
  const { user, cart, logout } = useSession();
  const [menuOpen, setMenuOpen] = useState(false);
  const cartCount = cart?.itemCount || 0;

  const handleLogout = () => {
    logout();
    setMenuOpen(false);
    navigate("/login", { replace: true });
  };

  const closeMenu = () => setMenuOpen(false);

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="topbar-inner">
          <Link className="brand" to="/home" onClick={closeMenu}>
            <span className="brand-mark">Q</span>
            <div>
              <strong>QuickBite</strong>
              <span>Food & Groceries</span>
            </div>
          </Link>

          <nav className="main-nav" aria-label="Primary navigation">
            <NavLink to="/home">Discover</NavLink>
            <NavLink to="/checkout" className="nav-cart-link" aria-label={`Cart ${cartCount} items`}>
              <svg className="nav-cart-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M7 4h-2l-1 2v2h2l2.4 7.2c.2.6.8 1 1.4 1h7.8c.7 0 1.3-.4 1.5-1l2-6H8.3l-.5-1.5H20V6H7.7L7 4zm3 14a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3zm7 0a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3z" />
              </svg>
              {cartCount > 0 ? <span className="nav-cart-badge">({cartCount})</span> : null}
            </NavLink>
            <NavLink to="/account">Account</NavLink>
          </nav>

          <div className="topbar-user">
            <div>
              <strong>{user?.fullName || "Guest"}</strong>
              <span>{user?.email}</span>
            </div>
            <button className="ghost-button" type="button" onClick={handleLogout}>
              Logout
            </button>
          </div>

          <div className="topbar-mobile-actions">
            <NavLink to="/checkout" className="nav-cart-link" aria-label={`Cart ${cartCount} items`} onClick={closeMenu}>
              <svg className="nav-cart-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M7 4h-2l-1 2v2h2l2.4 7.2c.2.6.8 1 1.4 1h7.8c.7 0 1.3-.4 1.5-1l2-6H8.3l-.5-1.5H20V6H7.7L7 4zm3 14a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3zm7 0a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3z" />
              </svg>
              {cartCount > 0 ? <span className="nav-cart-badge">({cartCount})</span> : null}
            </NavLink>
            <button
              className="hamburger-button"
              type="button"
              onClick={() => setMenuOpen((current) => !current)}
              aria-label="Toggle menu"
              aria-expanded={menuOpen}
            >
              <span />
              <span />
              <span />
            </button>
          </div>
        </div>

        {menuOpen ? (
          <div className="mobile-menu-panel">
            <NavLink to="/home" className="mobile-menu-link" onClick={closeMenu}>Discover</NavLink>
            <NavLink to="/account" className="mobile-menu-link" onClick={closeMenu}>Account</NavLink>
            <div className="mobile-user-block">
              <strong>{user?.fullName || "Guest"}</strong>
              <span>{user?.email}</span>
            </div>
            <button className="ghost-button" type="button" onClick={handleLogout}>Logout</button>
          </div>
        ) : null}
      </header>

      <section className="page-hero">
        <div>
          <p className="eyebrow">Fresh food, delivered fast</p>
          <h1>{title}</h1>
          <p>{subtitle}</p>
        </div>
      </section>

      <main className="page-content">{children}</main>
    </div>
  );
}
