const svgDataUrl = (svg) => `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;

const FALLBACK_IMAGES = {
  all: svgDataUrl(
    "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 96 96'><defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'><stop offset='0%' stop-color='%23ff8a3d'/><stop offset='100%' stop-color='%23ffb07a'/></linearGradient></defs><rect width='96' height='96' rx='48' fill='url(%23g)'/><text x='48' y='58' text-anchor='middle' font-size='40'>🍽️</text></svg>"
  ),
  biryani: svgDataUrl(
    "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 96 96'><defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'><stop offset='0%' stop-color='%23f59e0b'/><stop offset='100%' stop-color='%23f97316'/></linearGradient></defs><rect width='96' height='96' rx='48' fill='url(%23g)'/><text x='48' y='58' text-anchor='middle' font-size='40'>🍛</text></svg>"
  ),
  default: svgDataUrl(
    "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 96 96'><defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'><stop offset='0%' stop-color='%2364748b'/><stop offset='100%' stop-color='%2394a3b8'/></linearGradient></defs><rect width='96' height='96' rx='48' fill='url(%23g)'/><text x='48' y='58' text-anchor='middle' font-size='40'>🍽️</text></svg>"
  )
};

function getFallbackImage(categoryName) {
  const key = (categoryName || "").trim().toLowerCase();
  return FALLBACK_IMAGES[key] || FALLBACK_IMAGES.default;
}

function handleImageError(event, categoryName) {
  event.currentTarget.onerror = null;
  event.currentTarget.src = getFallbackImage(categoryName);
}

export default function CategoryStrip({ categories, activeCategory, onSelect }) {
  return (
    <section className="section-block">
      <div className="section-heading">
        <h2>What's on your mind?</h2>
      </div>

      <div className="category-strip">
        <button
          type="button"
          className={!activeCategory ? "category-pill active" : "category-pill"}
          onClick={() => onSelect("")}
        >
          <img src={getFallbackImage("all")} alt="All" loading="lazy" />
          <span>All</span>
        </button>

        {categories.map((category) => (
          <button
            key={category.id}
            type="button"
            className={activeCategory === category.name ? "category-pill active" : "category-pill"}
            onClick={() => onSelect(category.name)}
          >
            <img
              src={category.imageUrl || getFallbackImage(category.name)}
              alt={category.name}
              loading="lazy"
              onError={(event) => handleImageError(event, category.name)}
            />
            <span>{category.name}</span>
          </button>
        ))}
      </div>
    </section>
  );
}
