import { useEffect, useState } from "react";
import AppShell from "../components/AppShell";
import UserDashboard from "../components/UserDashboard";
import { useSession } from "../context/SessionContext";
import { updatePassword, updateProfile } from "../lib/api";

export default function AccountPage() {
  const { token, user, setUser } = useSession();
  const [form, setForm] = useState({
    fullName: user?.fullName || "",
    phone: user?.phone || ""
  });
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: ""
  });
  const [message, setMessage] = useState("");
  const [passwordMessage, setPasswordMessage] = useState("");
  const [passwordError, setPasswordError] = useState("");

  useEffect(() => {
    setForm({
      fullName: user?.fullName || "",
      phone: user?.phone || ""
    });
  }, [user]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    const updatedUser = await updateProfile(token, form);
    setUser(updatedUser);
    setMessage("Profile updated");
  };

  const handlePasswordSubmit = async (event) => {
    event.preventDefault();
    setPasswordMessage("");
    setPasswordError("");

    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setPasswordError("New password and confirm password must match.");
      return;
    }

    try {
      await updatePassword(token, {
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword
      });
      setPasswordForm({
        currentPassword: "",
        newPassword: "",
        confirmPassword: ""
      });
      setPasswordMessage("Password updated successfully");
    } catch (error) {
      setPasswordError(error.message || "Unable to update password.");
    }
  };

  return (
    <AppShell
      title="Your account dashboard"
      subtitle="Manage your profile and monitor your latest order activity."
    >
      <div className="account-layout">
        <section className="account-panel">
          <h2>Edit profile</h2>
          <form className="stack-form" onSubmit={handleSubmit}>
            <input
              type="text"
              value={form.fullName}
              onChange={(event) => setForm((current) => ({ ...current, fullName: event.target.value }))}
              placeholder="Full name"
              required
            />
            <input
              type="text"
              value={form.phone}
              onChange={(event) => setForm((current) => ({ ...current, phone: event.target.value }))}
              placeholder="Phone number"
            />
            <button type="submit">Save profile</button>
          </form>
          {message ? <div className="success-banner">{message}</div> : null}
        </section>

        <section className="account-panel">
          <h2>Change password</h2>
          <form className="stack-form" onSubmit={handlePasswordSubmit}>
            <input
              type="password"
              value={passwordForm.currentPassword}
              onChange={(event) => setPasswordForm((current) => ({ ...current, currentPassword: event.target.value }))}
              placeholder="Current password"
              required
            />
            <input
              type="password"
              value={passwordForm.newPassword}
              onChange={(event) => setPasswordForm((current) => ({ ...current, newPassword: event.target.value }))}
              placeholder="New password"
              required
            />
            <input
              type="password"
              value={passwordForm.confirmPassword}
              onChange={(event) => setPasswordForm((current) => ({ ...current, confirmPassword: event.target.value }))}
              placeholder="Confirm new password"
              required
            />
            <button type="submit">Change password</button>
          </form>
          {passwordMessage ? <div className="success-banner">{passwordMessage}</div> : null}
          {passwordError ? <div className="error-banner">{passwordError}</div> : null}
        </section>

        <section className="account-panel">
          <UserDashboard token={token} />
        </section>
      </div>
    </AppShell>
  );
}
