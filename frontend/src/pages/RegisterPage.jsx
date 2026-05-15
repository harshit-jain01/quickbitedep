import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import AuthForm from "../components/AuthForm";
import AuthShell from "../components/AuthShell";
import GoogleButton from "../components/GoogleButton";
import { useSession } from "../context/SessionContext";
import { register, registerRestaurant } from "../lib/api";
import { getPostLoginPath } from "../lib/roleRedirect";

const ALLOWED_IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];
const NAME_REGEX = /^[A-Za-z ]{3,50}$/;
const EMAIL_REGEX = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/;
const PASSWORD_REGEX = /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/;
const INDIAN_PHONE_REGEX = /^[6-9]\d{9}$/;

export default function RegisterPage() {
  const navigate = useNavigate();
  const { login, token, loading, user } = useSession();
  const [step, setStep] = useState("auth");
  const [authForm, setAuthForm] = useState({
    fullName: "",
    email: "",
    password: "",
    confirmPassword: "",
    phone: "",
    role: "CUSTOMER",
    profilePicUrl: ""
  });
  const [restaurantForm, setRestaurantForm] = useState({
    restaurantName: "",
    cuisineType: "",
    address: "",
    imageFile: null
  });
  const [fieldErrors, setFieldErrors] = useState({});
  const [authTouched, setAuthTouched] = useState({});
  const [restaurantTouched, setRestaurantTouched] = useState({});
  const [authSubmitAttempted, setAuthSubmitAttempted] = useState(false);
  const [restaurantSubmitAttempted, setRestaurantSubmitAttempted] = useState(false);
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [authResponse, setAuthResponse] = useState(null);

  useEffect(() => {
    if (loading || !token || !user) {
      return;
    }
    const targetPath = getPostLoginPath(user, "/home");
    navigate(targetPath, { replace: true });
  }, [loading, token, user, navigate]);

  const validateAuth = (input = authForm) => {
    const nextErrors = {};
    if (!input.role || !input.role.trim()) {
      nextErrors.role = "Please select account type";
    }
    if (!input.fullName.trim()) {
      nextErrors.fullName = "Full name is required";
    } else if (!NAME_REGEX.test(input.fullName.trim())) {
      nextErrors.fullName = "Name must be 3-50 characters and contain only letters";
    }
    if (!input.email.trim()) {
      nextErrors.email = "Email is required";
    } else if (!EMAIL_REGEX.test(input.email.trim())) {
      nextErrors.email = "Please enter a valid email address";
    }
    if (!input.password) {
      nextErrors.password = "Password is required";
    } else if (!PASSWORD_REGEX.test(input.password)) {
      nextErrors.password = "Password must include uppercase, lowercase, number, and special character";
    }
    if (!input.confirmPassword) {
      nextErrors.confirmPassword = "Please confirm your password";
    } else if (input.password !== input.confirmPassword) {
      nextErrors.confirmPassword = "Passwords do not match";
    }
    if (input.phone.trim() && !INDIAN_PHONE_REGEX.test(input.phone.trim())) {
      nextErrors.phone = "Enter a valid 10-digit phone number";
    }
    return nextErrors;
  };

  const validateRestaurant = (input = restaurantForm) => {
    const nextErrors = {};
    if (!input.restaurantName.trim()) {
      nextErrors.restaurantName = "Restaurant name is required";
    }
    if (!input.cuisineType.trim()) {
      nextErrors.cuisineType = "Cuisine type is required";
    }
    if (!input.address.trim()) {
      nextErrors.address = "Address is required";
    }
    if (!input.imageFile) {
      nextErrors.imageFile = "Restaurant image is required";
    }
    return nextErrors;
  };

  const visibleErrors = (allErrors, touched, submitted) => {
    const next = {};
    Object.keys(allErrors).forEach((key) => {
      if (submitted || touched[key]) {
        next[key] = allErrors[key];
      }
    });
    return next;
  };

  const handleAuthChange = (event) => {
    const { name, value } = event.target;
    const nextForm = { ...authForm, [name]: value };
    setAuthForm(nextForm);
    setFieldErrors(visibleErrors(validateAuth(nextForm), authTouched, authSubmitAttempted));
    setError("");
  };

  const handleAuthBlur = (event) => {
    const { name } = event.target;
    const nextTouched = { ...authTouched, [name]: true };
    setAuthTouched(nextTouched);
    setFieldErrors(visibleErrors(validateAuth(), nextTouched, authSubmitAttempted));
  };

  const handleRestaurantChange = (event) => {
    const { name, value } = event.target;
    const nextForm = { ...restaurantForm, [name]: value };
    setRestaurantForm(nextForm);
    setFieldErrors(visibleErrors(validateRestaurant(nextForm), restaurantTouched, restaurantSubmitAttempted));
    setError("");
  };

  const handleRestaurantBlur = (event) => {
    const { name } = event.target;
    const nextTouched = { ...restaurantTouched, [name]: true };
    setRestaurantTouched(nextTouched);
    setFieldErrors(visibleErrors(validateRestaurant(), nextTouched, restaurantSubmitAttempted));
  };

  const handleRestaurantImageChange = (event) => {
    const file = event.target.files && event.target.files.length > 0 ? event.target.files[0] : null;

    if (file && !ALLOWED_IMAGE_TYPES.includes(file.type)) {
      setRestaurantForm((current) => ({ ...current, imageFile: null }));
      setFieldErrors((current) => ({ ...current, imageFile: "Please upload JPG, PNG, or WEBP image." }));
      setError("");
      return;
    }

    setRestaurantForm((current) => ({ ...current, imageFile: file }));
    setFieldErrors(visibleErrors(validateRestaurant({ ...restaurantForm, imageFile: file }), restaurantTouched, restaurantSubmitAttempted));
    setError("");
  };

  const handleAuthSubmit = async (event) => {
    event.preventDefault();
    setAuthSubmitAttempted(true);
    const nextErrors = validateAuth();
    if (Object.keys(nextErrors).length > 0) {
      setFieldErrors(visibleErrors(nextErrors, authTouched, true));
      return;
    }

    setIsSubmitting(true);
    setError("");

    try {
      const response = await register({
        fullName: authForm.fullName.trim(),
        email: authForm.email.trim(),
        password: authForm.password,
        phone: authForm.phone.trim(),
        role: authForm.role,
        profilePicUrl: authForm.profilePicUrl
      });
      setAuthResponse(response);

      if (authForm.role === "RESTAURANT_OWNER") {
        setStep("restaurant");
        setFieldErrors({});
      } else {
        login(response);
        navigate(getPostLoginPath(response.user, "/home"), { replace: true });
      }
    } catch (requestError) {
      setError(requestError.message || "Registration failed");
    } finally {
      setIsSubmitting(false);
    }
  };

   const handleRestaurantSubmit = async (event) => {
     event.preventDefault();
     setRestaurantSubmitAttempted(true);
     const nextErrors = validateRestaurant();
     if (Object.keys(nextErrors).length > 0) {
       setFieldErrors(visibleErrors(nextErrors, restaurantTouched, true));
       return;
     }

     setIsSubmitting(true);
     setError("");

     try {
       // Register the restaurant
       await registerRestaurant(authResponse.token, {
         restaurantName: restaurantForm.restaurantName.trim(),
         cuisineType: restaurantForm.cuisineType.trim(),
         address: restaurantForm.address.trim(),
         imageFile: restaurantForm.imageFile
       });

       // Log in with the auth response (which has the token)
       login(authResponse);

       // Wait a brief moment to ensure state updates before navigation
       setTimeout(() => {
         navigate("/restaurant/dashboard", { replace: true });
       }, 100);
     } catch (requestError) {
       console.error("Restaurant registration error:", requestError);
       setError(requestError.message || "Restaurant registration failed");
     } finally {
       setIsSubmitting(false);
     }
   };

  const authValidationErrors = validateAuth();
  const isAuthInvalid = Object.keys(authValidationErrors).length > 0;
  const restaurantValidationErrors = validateRestaurant();
  const isRestaurantInvalid = Object.keys(restaurantValidationErrors).length > 0;

  if (step === "restaurant") {
    return (
      <AuthShell
        title="Register Your Restaurant"
        subtitle="Complete your restaurant registration to start managing orders"
        heroTitle="Build your restaurant presence"
        heroCopy="Register your restaurant details to begin serving customers"
      >
        <form className="auth-form" onSubmit={handleRestaurantSubmit}>
          <label className="field">
            <span>Restaurant Name *</span>
            <input
              name="restaurantName"
              type="text"
              placeholder="e.g., Taj Mahal Express"
              value={restaurantForm.restaurantName}
              onChange={handleRestaurantChange}
              onBlur={handleRestaurantBlur}
              style={fieldErrors.restaurantName ? { borderColor: "#ef4444" } : undefined}
              required
            />
            <small style={{ color: "#ef4444", fontSize: "0.8rem", minHeight: "1rem", display: "block" }}>
              {fieldErrors.restaurantName || " "}
            </small>
          </label>

          <label className="field">
            <span>Cuisine Type *</span>
            <input
              name="cuisineType"
              type="text"
              placeholder="e.g., Indian, Italian, Chinese"
              value={restaurantForm.cuisineType}
              onChange={handleRestaurantChange}
              onBlur={handleRestaurantBlur}
              style={fieldErrors.cuisineType ? { borderColor: "#ef4444" } : undefined}
              required
            />
            <small style={{ color: "#ef4444", fontSize: "0.8rem", minHeight: "1rem", display: "block" }}>
              {fieldErrors.cuisineType || " "}
            </small>
          </label>

          <label className="field">
            <span>Address *</span>
            <textarea
              name="address"
              placeholder="e.g., 123 Main St, Downtown, City"
              value={restaurantForm.address}
              onChange={handleRestaurantChange}
              onBlur={handleRestaurantBlur}
              required
              style={{ minHeight: "80px", ...(fieldErrors.address ? { borderColor: "#ef4444" } : {}) }}
            />
            <small style={{ color: "#ef4444", fontSize: "0.8rem", minHeight: "1rem", display: "block" }}>
              {fieldErrors.address || " "}
            </small>
          </label>

          <label className="field">
            <span>Restaurant Image *</span>
            <input
              name="imageFile"
              type="file"
              accept="image/jpeg,image/png,image/webp"
              onChange={handleRestaurantImageChange}
              required
            />
            {restaurantForm.imageFile ? <small>{restaurantForm.imageFile.name}</small> : null}
            <small style={{ color: "#ef4444", fontSize: "0.8rem", minHeight: "1rem", display: "block" }}>
              {fieldErrors.imageFile || " "}
            </small>
          </label>

          {error && <div className="error-banner">{error}</div>}

          <div style={{ display: "flex", gap: "1rem" }}>
            <button className="primary-button" type="submit" disabled={isSubmitting || isRestaurantInvalid} style={{ flex: 1 }}>
              {isSubmitting ? "Registering..." : "Register Restaurant"}
            </button>
            <button
              type="button"
              className="secondary-button"
              onClick={() => {
                setStep("auth");
                setError("");
                setFieldErrors({});
                setRestaurantTouched({});
                setRestaurantSubmitAttempted(false);
              }}
              disabled={isSubmitting}
              style={{ flex: 1 }}
            >
              Back
            </button>
          </div>
        </form>
      </AuthShell>
    );
  }

  return (
    <AuthShell
      title="Register"
      subtitle="Create your account to continue with QuickBite"
      heroTitle="Order food effortlessly, anytime"
      heroCopy="Track your orders in real-time and enjoy a smooth, seamless experience"
    >
      <AuthForm
        fields={[
          {
            name: "role",
            label: "Account Type",
            type: "select",
            required: true,
            options: [
              { value: "CUSTOMER", label: "Customer - Order food" },
              { value: "RESTAURANT_OWNER", label: "Restaurant Owner - Manage restaurant" }
            ]
          },
          {
            name: "fullName",
            label: "Full name",
            placeholder: "Harshit Jain",
            required: true,
            autoComplete: "name"
          },
          {
            name: "email",
            label: "Email",
            type: "email",
            placeholder: "harshit@gmail.com",
            required: true,
            autoComplete: "email"
          },
          {
            name: "password",
            label: "Password",
            type: "password",
            placeholder: "Minimum 8 characters",
            required: true,
            minLength: 8,
            autoComplete: "new-password"
          },
          {
            name: "confirmPassword",
            label: "Confirm password",
            type: "password",
            placeholder: "Re-enter your password",
            required: true,
            minLength: 8,
            autoComplete: "new-password"
          },
          {
            name: "phone",
            label: "Phone",
            type: "text",
            placeholder: "+91 98765 43210",
            autoComplete: "tel"
          }
        ]}
        values={authForm}
        errors={fieldErrors}
        onChange={handleAuthChange}
        onBlur={handleAuthBlur}
        onSubmit={handleAuthSubmit}
        submitLabel="Create account"
        isSubmitting={isSubmitting}
        isSubmitDisabled={isAuthInvalid}
        apiError={error}
        footer={
          <>
            <div className="auth-post-submit">
              <div className="auth-divider">or</div>
              <GoogleButton />
            </div>
            <p className="auth-helper">
              Already have an account? <Link to="/login">Login</Link>
            </p>
          </>
        }
      />
    </AuthShell>
  );
}
