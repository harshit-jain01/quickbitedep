export default function AuthShell({
  title,
  subtitle,
  brand = "QuickBite",
  heroTitle = "Order food effortlessly, anytime.",
  heroCopy = "Track your orders in real-time and enjoy a smooth, seamless experience.",
  heroImage =
    "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1600&q=80",
  children
}) {
  return (
    <div className="page-shell">
      <section className="hero-panel">
        <img className="hero-panel-image" src={heroImage} alt="" aria-hidden="true" />
        <div className="hero-panel-overlay" />
        <div className="hero-panel-content">
          <div className="hero-brand">
            <span className="brand-mark">QB</span>
            <span>{brand}</span>
          </div>
          <div className="hero-copy-block">
            <h1>{heroTitle}</h1>
            <p className="hero-copy">{heroCopy}</p>
          </div>
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-card auth-card-compact">
          <div className="panel-head">
            <p className="panel-brand">{brand}</p>
            <h2>{title}</h2>
            <p className="panel-copy">{subtitle}</p>
          </div>
          {children}
        </div>
      </section>
    </div>
  );
}
