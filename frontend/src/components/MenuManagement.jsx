import { useState, useEffect } from "react";
import MenuItemForm from "./MenuItemForm";
import {
  addMenuItem,
  updateMenuItem,
  deleteMenuItem,
  toggleMenuItemAvailability,
  createMenuCategory,
  getMenuCategories
} from "../lib/api";

const DEFAULT_ITEM_IMAGE = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=300&q=80";

export default function MenuManagement({ token, restaurantId }) {
  const [categories, setCategories] = useState([]);
  const [newCategoryName, setNewCategoryName] = useState("");
  const [editingItem, setEditingItem] = useState(null);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  useEffect(() => {
    loadCategories();
  }, [token, restaurantId]);

  const loadCategories = async () => {
    try {
      setError("");  // Clear error before loading
      if (!token || !restaurantId) {
        setError("Missing authentication or restaurant ID");
        setCategories([]);
        return;
      }
      const data = await getMenuCategories(token, restaurantId);
      // Handle both array and object responses
      const categoriesArray = Array.isArray(data) ? data : data?.categories || [];
      setCategories(categoriesArray);
    } catch (err) {
      console.error("Failed to load categories:", err);
      setError(err.message || "Failed to load categories");
      setCategories([]);  // Reset categories on error
    }
  };

  const handleAddCategory = async (e) => {
    e.preventDefault();
    if (!newCategoryName.trim()) {
      setError("Please enter a category name");
      return;
    }

    try {
      setError("");  // Clear previous errors
      await createMenuCategory(token, {
        restaurantId,
        categoryName: newCategoryName.trim()
      });
      setNewCategoryName("");
      setSuccessMessage("Category created successfully");
      await loadCategories();
      setTimeout(() => setSuccessMessage(""), 3000);
    } catch (err) {
      console.error("Failed to create category:", err);
      setError(err.message || "Failed to create category");
    }
  };

  const handleAddMenuItem = async (itemData) => {
    try {
      setError("");  // Clear previous errors
      console.log("Adding/Updating menu item with data:", itemData);
      console.log("Token present:", !!token);
      console.log("Restaurant ID:", restaurantId);

      if (!token) {
        setError("Authentication token missing. Please login again.");
        return;
      }

      if (editingItem) {
        await updateMenuItem(token, editingItem.itemId, {
          ...itemData,
          restaurantId
        });
        setSuccessMessage("Item updated successfully");
      } else {
        await addMenuItem(token, {
          ...itemData,
          restaurantId
        });
        setSuccessMessage("Item added successfully");
      }
      setEditingItem(null);
      await loadCategories();
      setTimeout(() => setSuccessMessage(""), 3000);
    } catch (err) {
      console.error("Failed to add/update menu item:", err);
      if (err.message.includes("Forbidden") || err.message.includes("403")) {
        setError("Access denied. Make sure you're logged in as a restaurant owner.");
      } else {
        setError(err.message || "Failed to save menu item");
      }
    }
  };

  const handleDeleteItem = async (itemId) => {
    if (!window.confirm("Are you sure you want to delete this item?")) return;

    try {
      setError("");  // Clear previous errors
      await deleteMenuItem(token, itemId);
      setSuccessMessage("Item deleted successfully");
      await loadCategories();
      setTimeout(() => setSuccessMessage(""), 3000);
    } catch (err) {
      console.error("Failed to delete menu item:", err);
      setError(err.message || "Failed to delete menu item");
    }
  };

  const handleToggleAvailability = async (itemId, currentStatus) => {
    try {
      setError("");  // Clear previous errors
      await toggleMenuItemAvailability(token, itemId, {
        isAvailable: !currentStatus
      });
      await loadCategories();
      setSuccessMessage("Availability updated");
      setTimeout(() => setSuccessMessage(""), 3000);
    } catch (err) {
      console.error("Failed to toggle availability:", err);
      setError(err.message || "Failed to update availability");
    }
  };

  return (
    <div className="menu-management">
      {error && <div className="error-banner">{error}</div>}
      {successMessage && <div className="success-banner">{successMessage}</div>}

      <div className="menu-section">
        <h3>Create New Category</h3>
        <form className="category-form" onSubmit={handleAddCategory}>
          <input
            type="text"
            placeholder="Enter category name (e.g., Appetizers)"
            value={newCategoryName}
            onChange={(e) => setNewCategoryName(e.target.value)}
          />
          <button className="primary-button" type="submit">
            Add Category
          </button>
        </form>
      </div>

      <div className="menu-section">
        <h3>Add or Edit Menu Items</h3>
        <MenuItemForm
          key={editingItem?.itemId || "new-item"}
          token={token}
          onSubmit={handleAddMenuItem}
          existingItem={editingItem}
          categories={categories}
        />
        {editingItem && (
          <button
            className="secondary-button"
            onClick={() => setEditingItem(null)}
          >
            Cancel Edit
          </button>
        )}
      </div>

      <div className="menu-section">
        <h3>Your Menu Items</h3>
        {categories.length === 0 ? (
          <div className="empty-state">No categories yet. Create one to add items.</div>
        ) : (
          categories.map((category) => (
            <div key={category.categoryId} className="category-items">
              <h4>{category.categoryName}</h4>
              {category.items?.length > 0 ? (
                <div className="items-grid">
                  {category.items.map((item) => (
                    <div key={item.itemId} className="menu-item-card">
                      <img
                        src={item.imageUrl || DEFAULT_ITEM_IMAGE}
                        alt={item.itemName}
                        onError={(e) => {
                          e.target.src = DEFAULT_ITEM_IMAGE;
                        }}
                      />
                      <div className="item-content">
                        <h5>{item.itemName}</h5>
                        {item.description && <p>{item.description}</p>}
                        <div className="item-footer">
                          <span className="price">₹{item.price}</span>
                          <span
                            className={`badge ${item.isAvailable ? "available" : "unavailable"}`}
                          >
                            {item.isAvailable ? "Available" : "Out of Stock"}
                          </span>
                        </div>
                      </div>
                      <div className="item-actions">
                        <button
                          className="icon-button"
                          onClick={() =>
                            handleToggleAvailability(item.itemId, item.isAvailable)
                          }
                          title={item.isAvailable ? "Mark unavailable" : "Mark available"}
                        >
                          {item.isAvailable ? "📍" : "⭕"}
                        </button>
                        <button
                          className="icon-button"
                          onClick={() => setEditingItem({ ...item, categoryId: category.categoryId })}
                          title="Edit item"
                        >
                          ✏️
                        </button>
                        <button
                          className="icon-button danger"
                          onClick={() => handleDeleteItem(item.itemId)}
                          title="Delete item"
                        >
                          🗑️
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="empty-state">No items in this category</p>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
}
