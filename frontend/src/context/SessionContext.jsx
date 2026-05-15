import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { clearSession, getStoredUser, getToken, saveSession } from "../lib/auth";
import { getCart, getProfile } from "../lib/api";

const SessionContext = createContext(null);
const FIXED_ADMIN_EMAIL = "admin123@gmail.com";

export function SessionProvider({ children }) {
  const [token, setToken] = useState(getToken());
  const [user, setUser] = useState(getStoredUser());
  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(Boolean(getToken()));

  useEffect(() => {
    if (!token) {
      setLoading(false);
      setCart(null);
      return;
    }

    let active = true;
    setLoading(true);

    const hydrateSession = async () => {
      try {
        // If login/register already provided user payload, do not force-logout on a transient profile fetch failure.
        if (!user) {
          const profile = await getProfile(token);
          if (!active) {
            return;
          }
          setUser(profile);
          saveSession(token, profile);
        }

        try {
          const cartResponse = await getCart(token);
          if (active) {
            setCart(cartResponse);
          }
        } catch {
          if (active) {
            setCart(null);
          }
        }
      } catch {
        if (!active) {
          return;
        }
        if (user) {
          // Keep the optimistic session from login/register to avoid redirect loops on transient profile failures.
          setLoading(false);
          return;
        }
        clearSession();
        setToken("");
        setUser(null);
        setCart(null);
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    hydrateSession();

    return () => {
      active = false;
    };
  }, [token]);

   const value = useMemo(
     () => ({
       token,
       user,
       cart,
       loading,
       setUser,
       setCart,
       login: (authResponse) => {
         setToken(authResponse.token);
         setUser(authResponse.user);
         saveSession(authResponse.token, authResponse.user);
       },
       logout: () => {
         clearSession();
         setToken("");
         setUser(null);
         setCart(null);
       },
       isRestaurantOwner: () => user?.role === "RESTAURANT_OWNER",
        isAgent: () => user?.role === "AGENT",
        isCustomer: () => user?.role === "CUSTOMER",
        isAdmin: () =>
          user?.role === "ADMIN" && user?.email?.toLowerCase() === FIXED_ADMIN_EMAIL
     }),
     [token, user, cart, loading, setUser, setCart]
   );

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession() {
  const context = useContext(SessionContext);
  if (!context) {
    throw new Error("useSession must be used within SessionProvider");
  }
  return context;
}
