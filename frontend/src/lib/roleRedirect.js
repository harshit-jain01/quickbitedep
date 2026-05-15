const FIXED_ADMIN_EMAIL = "admin123@gmail.com";

export function getPostLoginPath(user, fallbackPath = "/home") {
  const role = user?.role;

  if (role === "ADMIN") {
    return user?.email?.toLowerCase() === FIXED_ADMIN_EMAIL ? "/admin" : "/home";
  }

  if (role === "RESTAURANT_OWNER") {
    return "/restaurant/dashboard";
  }

  if (role === "AGENT") {
    return "/delivery-agent/dashboard";
  }

  return fallbackPath;
}

