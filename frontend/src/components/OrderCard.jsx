export default function OrderCard({ order }) {
  return (
    <article className="order-card">
      <div className="order-card-header">
        <div>
          <h3>{order.restaurantName}</h3>
          <p>{new Date(order.createdAt).toLocaleString()}</p>
        </div>
        <div className="order-status">
          <span>{order.paymentMode} - {order.paymentStatus}</span>
          <strong>{order.deliveryStatus}</strong>
        </div>
      </div>

      <div className="order-items">
        {order.items.map((item) => (
          <div key={`${order.orderReference}-${item.itemName}`} className="order-item-row">
            <span>
              {item.itemName} x {item.quantity}
            </span>
            <strong>Rs {item.lineTotal.toFixed(0)}</strong>
          </div>
        ))}
      </div>

      <div className="order-card-footer">
        <strong>Total: Rs {order.total.toFixed(0)}</strong>
      </div>
    </article>
  );
}
