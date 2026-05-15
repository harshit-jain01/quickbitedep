import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import AppShell from "../components/AppShell";
import { useSession } from "../context/SessionContext";
import {
  checkout,
  createRazorpayOrder,
  getCart,
  removeCartItem,
  updateCartItem,
  verifyRazorpayPayment
} from "../lib/api";

const PAYMENT_METHODS = {
  COD: "COD",
  UPI: "UPI",
  CARD: "CARD"
};

function buildClientOrderReference() {
  return `OD${Date.now()}${Math.floor(Math.random() * 900 + 100)}`;
}

function loadRazorpayScript() {
  if (window.Razorpay) {
    return Promise.resolve(true);
  }
  return new Promise((resolve) => {
    const script = document.createElement("script");
    script.src = "https://checkout.razorpay.com/v1/checkout.js";
    script.async = true;
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });
}

function getRazorpayMethodOptions(paymentMethod) {
  // Keep gateway methods flexible; strict filtering can cause "No appropriate payment method found".
  return {};
}

function isValidUpiId(upiId) {
  return /^[a-zA-Z0-9._-]{2,256}@[a-zA-Z]{2,64}$/.test(upiId);
}

export default function CheckoutPage() {
  const navigate = useNavigate();
  const { token, cart, setCart } = useSession();
  const [form, setForm] = useState({
    deliveryAddress: "",
    paymentMethod: PAYMENT_METHODS.COD,
    upiId: "",
    cardNumber: "",
    cardName: "",
    cardExpiry: "",
    cardCvv: "",
    notes: ""
  });
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState("success");

  const totalAmount = useMemo(() => Number(cart?.total || 0), [cart]);

  useEffect(() => {
    getCart(token).then(setCart);
  }, [token, setCart]);

  const handleQuantityChange = async (itemId, quantity) => {
    if (quantity < 1) {
      return;
    }
    const updatedCart = await updateCartItem(token, itemId, { quantity });
    setCart(updatedCart);
  };

  const handleRemoveItem = async (itemId) => {
    const updatedCart = await removeCartItem(token, itemId);
    setCart(updatedCart);
  };

  const handleCheckout = async (event) => {
    event.preventDefault();
    setMessage("");
    setSubmitting(true);

    try {
      let order;

      if (form.paymentMethod === PAYMENT_METHODS.COD) {
        order = await checkout(token, {
          deliveryAddress: form.deliveryAddress,
          paymentMethod: PAYMENT_METHODS.COD,
          notes: form.notes
        });
      } else {
        const scriptReady = await loadRazorpayScript();
        if (!scriptReady) {
          throw new Error("Unable to load Razorpay. Please try again.");
        }

        if (totalAmount <= 0) {
          throw new Error("Cart total must be greater than zero");
        }

        const normalizedUpiId = form.upiId.trim();
        if (form.paymentMethod === PAYMENT_METHODS.UPI && normalizedUpiId && !isValidUpiId(normalizedUpiId)) {
          throw new Error("Please enter a valid UPI ID (example: yourname@upi)");
        }

        const orderReference = buildClientOrderReference();
        const razorpayOrder = await createRazorpayOrder(token, {
          orderReference,
          customerEmail: cart?.userEmail || "",
          paymentMode: form.paymentMethod,
          amount: totalAmount,
          currency: "INR",
          notes: form.notes
        });

        const paymentResult = await new Promise((resolve, reject) => {
          const razorpayOptions = {
            key: razorpayOrder.keyId,
            amount: razorpayOrder.amount,
            currency: razorpayOrder.currency,
            name: "QuickBite",
            description: "Food order payment",
            order_id: razorpayOrder.razorpayOrderId,
            prefill: {
              email: cart?.userEmail || "",
              ...(form.paymentMethod === PAYMENT_METHODS.UPI && normalizedUpiId ? { vpa: normalizedUpiId } : {})
            },
            theme: {
              color: "#f47c2c"
            },
            handler: (response) => resolve(response),
            modal: {
              ondismiss: () => reject(new Error("Payment popup closed"))
            }
          };

          const razorpayInstance = new window.Razorpay({
            ...razorpayOptions,
            ...getRazorpayMethodOptions(form.paymentMethod)
          });
          razorpayInstance.open();
        });

        await verifyRazorpayPayment(token, {
          orderReference,
          customerEmail: cart?.userEmail || "",
          paymentMode: form.paymentMethod,
          amount: totalAmount,
          currency: "INR",
          razorpayOrderId: paymentResult.razorpay_order_id,
          razorpayPaymentId: paymentResult.razorpay_payment_id,
          razorpaySignature: paymentResult.razorpay_signature
        });

        order = await checkout(token, {
          deliveryAddress: form.deliveryAddress,
          paymentMethod: form.paymentMethod,
          notes: form.notes,
          orderReference,
          razorpayOrderId: paymentResult.razorpay_order_id,
          razorpayPaymentId: paymentResult.razorpay_payment_id
        });
      }

      setCart({
        userEmail: cart?.userEmail || "",
        restaurantId: null,
        restaurantName: null,
        items: [],
        itemCount: 0,
        subtotal: 0,
        deliveryFee: 0,
        taxes: 0,
        total: 0
      });
      navigate("/order-confirmation", {
        replace: true,
        state: { order }
      });
    } catch (error) {
      setMessageType("error");
      setMessage(`Error placing order: ${error.message}`);
      console.error("Checkout error:", error);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AppShell
      title="Secure checkout in seconds"
      subtitle="Review your cart, confirm delivery details, and place your order."
    >
      <div className="checkout-layout">
        <section className="checkout-card">
          <h2>Your cart</h2>
          {!cart?.items?.length ? <div className="empty-state">Your cart is empty.</div> : null}
          {cart?.items?.map((item) => (
            <div key={item.id} className="checkout-item">
              <img src={item.imageUrl} alt={item.itemName} />
              <div>
                <strong>{item.itemName}</strong>
                <span>Rs {item.unitPrice.toFixed(0)}</span>
              </div>
              <input
                type="number"
                min="1"
                value={item.quantity}
                onChange={(event) => handleQuantityChange(item.id, Number(event.target.value))}
              />
              <button type="button" className="ghost-button" onClick={() => handleRemoveItem(item.id)}>
                Remove
              </button>
            </div>
          ))}
        </section>

        <section className="checkout-card">
          <h2>Delivery & payment</h2>
          <form className="checkout-form" onSubmit={handleCheckout}>
            <textarea
              value={form.deliveryAddress}
              onChange={(event) => setForm((current) => ({ ...current, deliveryAddress: event.target.value }))}
              placeholder="Write your address"
              required
            />
            <select
              value={form.paymentMethod}
              onChange={(event) => setForm((current) => ({ ...current, paymentMethod: event.target.value }))}
            >
              <option value={PAYMENT_METHODS.COD}>Cash on Delivery</option>
              <option value={PAYMENT_METHODS.UPI}>UPI Payment</option>
              <option value={PAYMENT_METHODS.CARD}>Credit / Debit Card</option>
            </select>

            {form.paymentMethod === PAYMENT_METHODS.UPI ? (
              <div className="payment-extra-panel">
                <label htmlFor="upi-id">Enter UPI ID</label>
                <input
                  id="upi-id"
                  type="text"
                  placeholder="yourname@upi"
                  value={form.upiId}
                  onChange={(event) => setForm((current) => ({ ...current, upiId: event.target.value }))}
                />
                <small>Supported: Google Pay, PhonePe, Paytm, BHIM UPI</small>
              </div>
            ) : null}

            {form.paymentMethod === PAYMENT_METHODS.CARD ? (
              <div className="payment-extra-panel">
                <small>Your card details are entered in Razorpay's secure popup.</small>
                <input
                  type="text"
                  placeholder="Card Number"
                  value={form.cardNumber}
                  onChange={(event) => setForm((current) => ({ ...current, cardNumber: event.target.value }))}
                />
                <input
                  type="text"
                  placeholder="Cardholder Name"
                  value={form.cardName}
                  onChange={(event) => setForm((current) => ({ ...current, cardName: event.target.value }))}
                />
                <div className="card-inline-fields">
                  <input
                    type="text"
                    placeholder="MM/YY"
                    value={form.cardExpiry}
                    onChange={(event) => setForm((current) => ({ ...current, cardExpiry: event.target.value }))}
                  />
                  <input
                    type="password"
                    placeholder="CVV"
                    value={form.cardCvv}
                    onChange={(event) => setForm((current) => ({ ...current, cardCvv: event.target.value }))}
                  />
                </div>
              </div>
            ) : null}

            <input
              type="text"
              placeholder="Delivery notes"
              value={form.notes}
              onChange={(event) => setForm((current) => ({ ...current, notes: event.target.value }))}
            />

            <div className="bill-panel">
              <div><span>Subtotal</span><strong>Rs {cart?.subtotal?.toFixed(0) || 0}</strong></div>
              <div><span>Delivery fee</span><strong>Rs {cart?.deliveryFee?.toFixed(0) || 0}</strong></div>
              <div><span>Taxes</span><strong>Rs {cart?.taxes?.toFixed(0) || 0}</strong></div>
              <div className="bill-total"><span>Total</span><strong>Rs {cart?.total?.toFixed(0) || 0}</strong></div>
            </div>

            <button type="submit" disabled={!cart?.items?.length || submitting}>
              {submitting
                ? "Processing..."
                : form.paymentMethod === PAYMENT_METHODS.COD
                  ? `Place order - Rs ${totalAmount.toFixed(0)}`
                  : `Pay with Razorpay - Rs ${totalAmount.toFixed(0)}`}
            </button>
          </form>

           {message ? (
             <div className={messageType === "error" ? "error-banner" : "success-banner"}>
               {message}
             </div>
           ) : null}
        </section>
      </div>
    </AppShell>
  );
}
