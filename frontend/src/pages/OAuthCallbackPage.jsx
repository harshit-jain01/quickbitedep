import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useSession } from "../context/SessionContext";
import { getProfile } from "../lib/api";
import { getPostLoginPath } from "../lib/roleRedirect";

export default function OAuthCallbackPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const { login } = useSession();

  useEffect(() => {
    const token = params.get("token");
    if (!token) {
      navigate("/login", { replace: true });
      return;
    }

    getProfile(token)
      .then((profile) => {
        login({ token, user: profile });
        navigate(getPostLoginPath(profile, "/home"), { replace: true });
      })
      .catch(() => navigate("/login", { replace: true }));
  }, [params, login, navigate]);

  return <div className="page-loader">Completing OAuth sign-in...</div>;
}
