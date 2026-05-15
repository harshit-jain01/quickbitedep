import { OAUTH_BASE_URL } from "../config";

export default function GoogleButton() {
  const handleGoogleLogin = () => {
    window.location.href = `${OAUTH_BASE_URL}/oauth2/authorization/google`;
  };

  return (
    <button className="google-button" type="button" onClick={handleGoogleLogin}>
      <span className="google-icon">G</span>
      <span>Continue with Google</span>
    </button>
  );
}
