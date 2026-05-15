import { useState } from "react";
import { Link } from "react-router-dom";
import AuthForm from "../components/AuthForm";
import AuthShell from "../components/AuthShell";
import { register as registerUser, registerDeliveryAgent } from "../lib/api";

const NAME_REGEX = /^[A-Za-z ]{3,50}$/;
const EMAIL_REGEX = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/;
const PASSWORD_REGEX = /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/;
const INDIAN_PHONE_REGEX = /^[6-9]\d{9}$/;
const VEHICLE_NUMBER_REGEX = /^[A-Z]{2}[0-9]{2}[A-Z]{1,2}[0-9]{4}$/;

export default function DeliveryAgentRegisterPage() {
  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
    confirmPassword: "",
    phone: "",
    vehicleType: "Bike",
    vehicleNumber: ""
  });
  const [fieldErrors, setFieldErrors] = useState({});
  const [touchedFields, setTouchedFields] = useState({});
  const [submitAttempted, setSubmitAttempted] = useState(false);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const validate = (input = form) => {
    const nextErrors = {};

    if (!input.name.trim()) {
      nextErrors.name = "Name is required";
    } else if (!NAME_REGEX.test(input.name.trim())) {
      nextErrors.name = "Name must be 3-50 characters and contain only letters";
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
      nextErrors.confirmPassword = "Please confirm password";
    } else if (input.password !== input.confirmPassword) {
      nextErrors.confirmPassword = "Passwords do not match";
    }

    const normalizedPhone = input.phone.trim();
    if (!normalizedPhone) {
      nextErrors.phone = "Phone number is required";
    } else if (!INDIAN_PHONE_REGEX.test(normalizedPhone)) {
      nextErrors.phone = "Enter a valid 10-digit phone number";
    }

    if (!input.vehicleType.trim()) {
      nextErrors.vehicleType = "Vehicle type is required";
    }

    const normalizedVehicleNumber = input.vehicleNumber.trim().toUpperCase();
    if (!normalizedVehicleNumber) {
      nextErrors.vehicleNumber = "Vehicle number is required";
    } else if (!VEHICLE_NUMBER_REGEX.test(normalizedVehicleNumber)) {
      nextErrors.vehicleNumber = "Enter valid vehicle number (e.g., MP04AB1234)";
    }

    return nextErrors;
  };

  const visibleErrors = (allErrors, touched = touchedFields, submitted = submitAttempted) => {
    const next = {};
    Object.keys(allErrors).forEach((key) => {
      if (submitted || touched[key]) {
        next[key] = allErrors[key];
      }
    });
    return next;
  };

  const handleChange = (event) => {
    const { name, value } = event.target;
    const nextForm = { ...form, [name]: value };
    setForm(nextForm);
    setFieldErrors(visibleErrors(validate(nextForm)));
    setError("");
    setSuccessMessage("");
  };

  const handleBlur = (event) => {
    const { name } = event.target;
    const nextTouched = { ...touchedFields, [name]: true };
    setTouchedFields(nextTouched);
    setFieldErrors(visibleErrors(validate(), nextTouched));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitAttempted(true);
    const nextErrors = validate();

    if (Object.keys(nextErrors).length > 0) {
      setFieldErrors(visibleErrors(nextErrors, touchedFields, true));
      return;
    }

    setIsSubmitting(true);
    setError("");
    setSuccessMessage("");

    try {
      await registerUser({
        fullName: form.name.trim(),
        email: form.email.trim(),
        password: form.password,
        phone: form.phone.trim(),
        role: "AGENT"
      });

      await registerDeliveryAgent({
        name: form.name.trim(),
        phone: form.phone.trim(),
        vehicleType: form.vehicleType.trim(),
        vehicleNumber: form.vehicleNumber.trim().toUpperCase()
      });
      setSuccessMessage("Registration successful. You can now login using your email and password.");
      setForm({
        name: "",
        email: "",
        password: "",
        confirmPassword: "",
        phone: "",
        vehicleType: "Bike",
        vehicleNumber: ""
      });
      setTouchedFields({});
      setSubmitAttempted(false);
    } catch (requestError) {
      setError(requestError.message || "Failed to register delivery agent");
    } finally {
      setIsSubmitting(false);
    }
  };

  const validationErrors = validate();
  const isFormInvalid = Object.keys(validationErrors).length > 0;

  return (
    <AuthShell
      title="Delivery Agent Registration"
      subtitle="Create your delivery partner profile"
      heroTitle="Join QuickBite delivery network"
      heroCopy="Register once and start accepting orders right away."
    >
      <AuthForm
        fields={[
          {
            name: "name",
            label: "Full Name",
            placeholder: "Rahul Sharma",
            required: true,
            autoComplete: "name"
          },
          {
            name: "email",
            label: "Email",
            type: "email",
            placeholder: "rahul@example.com",
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
            label: "Confirm Password",
            type: "password",
            placeholder: "Re-enter password",
            required: true,
            minLength: 8,
            autoComplete: "new-password"
          },
          {
            name: "phone",
            label: "Phone",
            placeholder: "+919876543210",
            required: true,
            autoComplete: "tel"
          },
          {
            name: "vehicleType",
            label: "Vehicle Type",
            type: "select",
            required: true,
            options: [
              { value: "Bike", label: "Bike" },
              { value: "Scooter", label: "Scooter" },
              { value: "Bicycle", label: "Bicycle" }
            ]
          },
          {
            name: "vehicleNumber",
            label: "Vehicle Number",
            placeholder: "MP04AB1234",
            required: true
          }
        ]}
        values={form}
        errors={fieldErrors}
        onChange={handleChange}
        onBlur={handleBlur}
        onSubmit={handleSubmit}
        submitLabel="Register as Delivery Agent"
        isSubmitting={isSubmitting}
        isSubmitDisabled={isFormInvalid}
        apiError={error}
        footer={
          <>
            {successMessage ? <div className="success-banner">{successMessage}</div> : null}
            <p className="auth-helper">
              Already have a user account? <Link to="/login">Back to login</Link>
            </p>
          </>
        }
      />
    </AuthShell>
  );
}
