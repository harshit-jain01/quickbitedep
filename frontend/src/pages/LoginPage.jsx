import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import AuthForm from "../components/AuthForm";
import AuthShell from "../components/AuthShell";
import GoogleButton from "../components/GoogleButton";
import { useSession } from "../context/SessionContext";
import { login } from "../lib/api";
import { getPostLoginPath } from "../lib/roleRedirect";

const EMAIL_REGEX = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/;

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login: onLogin, token, loading, user } = useSession();
  const [form, setForm] = useState({ email: "", password: "" });
  const [fieldErrors, setFieldErrors] = useState({});
  const [touchedFields, setTouchedFields] = useState({});
  const [submitAttempted, setSubmitAttempted] = useState(false);
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const redirectTo = location.state?.from?.pathname || "/home";

  useEffect(() => {
    if (loading || !token || !user) {
      return;
    }
    const targetPath = getPostLoginPath(user, "/home");
    navigate(targetPath, { replace: true });
  }, [loading, token, user, navigate]);

  const handleLoginSuccess = (response) => {
    onLogin(response);
    const targetPath = getPostLoginPath(response.user, redirectTo);
    navigate(targetPath, { replace: true });
  };

  const validate = (input = form) => {
    const nextErrors = {};

    if (!input.email.trim()) {
      nextErrors.email = "Email is required";
    } else if (!EMAIL_REGEX.test(input.email.trim())) {
      nextErrors.email = "Enter a valid email address";
    }

    if (!input.password) {
      nextErrors.password = "Password is required";
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
    const nextErrors = validate(nextForm);
    setFieldErrors(visibleErrors(nextErrors));
    setError("");
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

    try {
      const response = await login({
        email: form.email.trim(),
        password: form.password
      });
      handleLoginSuccess(response);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const validationErrors = validate();
  const isFormInvalid = Object.keys(validationErrors).length > 0;

  return (
    <AuthShell
      title="Login"
      subtitle="Welcome back! Please sign in to continue."
      heroTitle="Order food effortlessly, anytime."
      heroCopy="Track your orders in real-time and enjoy a smooth, seamless experience."
    >
      <AuthForm
        fields={[
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
            placeholder: "Enter your password",
            required: true,
            autoComplete: "current-password"
          }
        ]}
        values={form}
        errors={fieldErrors}
        onChange={handleChange}
        onBlur={handleBlur}
        onSubmit={handleSubmit}
        submitLabel="Continue"
        isSubmitting={isSubmitting}
        isSubmitDisabled={isFormInvalid}
        apiError={error}
        footer={
          <>
            <div className="auth-post-submit">
              <div className="auth-divider">or</div>
              <GoogleButton />
            </div>
            <p className="auth-helper">
              Don&apos;t have an account? <Link to="/register">Create one</Link>
            </p>
            <p className="auth-helper">
              Want to deliver with us? <Link to="/delivery-agent/register">Register as Delivery Agent</Link>
            </p>
          </>
        }
      />
    </AuthShell>
  );
}
