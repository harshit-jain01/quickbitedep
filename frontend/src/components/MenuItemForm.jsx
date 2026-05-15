import { useState } from "react";

export default function MenuItemForm({ onSubmit, existingItem = null, categories = [] }) {
  const [form, setForm] = useState({
    categoryId: existingItem?.categoryId || "",
    itemName: existingItem?.itemName || "",
    description: existingItem?.description || "",
    price: existingItem?.price || "",
    imageFile: null,
    isAvailable: existingItem?.isAvailable !== false
  });
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value
    }));
    setError("");
  };

  const handleFileChange = (e) => {
    const file = e.target.files?.[0] || null;
    setForm((prev) => ({
      ...prev,
      imageFile: file
    }));
    setError("");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!form.categoryId || !form.itemName || !form.price) {
      setError("Category, name, and price are required");
      return;
    }

    if (!existingItem && !form.imageFile) {
      setError("Please select an image from your device");
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit({
        ...form,
        price: parseFloat(form.price)
      });
      setForm({
        categoryId: "",
        itemName: "",
        description: "",
        price: "",
        imageFile: null,
        isAvailable: true
      });
    } catch (err) {
      setError(err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form className="stack-form menu-item-form" onSubmit={handleSubmit}>
      <label className="field">
        <span>Category</span>
        <select
          name="categoryId"
          value={form.categoryId}
          onChange={handleChange}
          required
        >
          <option value="">Select a category</option>
          {categories.map((cat) => (
            <option key={cat.categoryId} value={cat.categoryId}>
              {cat.categoryName}
            </option>
          ))}
        </select>
      </label>

      <label className="field">
        <span>Item Name</span>
        <input
          type="text"
          name="itemName"
          placeholder="e.g., Paneer Butter Masala"
          value={form.itemName}
          onChange={handleChange}
          required
        />
      </label>

      <label className="field">
        <span>Description</span>
        <textarea
          name="description"
          placeholder="Describe your item..."
          value={form.description}
          onChange={handleChange}
          rows="3"
        />
      </label>

      <label className="field">
        <span>Price (₹)</span>
        <input
          type="number"
          name="price"
          placeholder="299"
          value={form.price}
          onChange={handleChange}
          step="0.01"
          required
        />
      </label>

      <label className="field">
        <span>Image {existingItem ? "(optional)" : "*"}</span>
        <input
          type="file"
          name="imageFile"
          accept="image/png,image/jpeg,image/webp"
          onChange={handleFileChange}
          required={!existingItem}
        />
        {existingItem?.imageUrl && (
          <small>Leave empty to keep current image.</small>
        )}
      </label>

      <label className="field checkbox-field">
        <input
          type="checkbox"
          name="isAvailable"
          checked={form.isAvailable}
          onChange={handleChange}
        />
        <span>Item is available</span>
      </label>

      {error && <div className="error-banner">{error}</div>}

      <button className="primary-button" type="submit" disabled={isSubmitting}>
        {isSubmitting ? "Saving..." : existingItem ? "Update Item" : "Add Item"}
      </button>
    </form>
  );
}

