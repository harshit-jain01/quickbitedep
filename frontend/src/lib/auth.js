const TOKEN_KEY = "quickbite_token";
const USER_KEY = "quickbite_user";

function getSessionStorage() {
  return window.sessionStorage;
}

function clearLegacyLocalStorage() {
  window.localStorage.removeItem(TOKEN_KEY);
  window.localStorage.removeItem(USER_KEY);
}

export function saveSession(token, user) {
  const storage = getSessionStorage();
  storage.setItem(TOKEN_KEY, token);
  storage.setItem(USER_KEY, JSON.stringify(user));
  clearLegacyLocalStorage();
}

export function clearSession() {
  const storage = getSessionStorage();
  storage.removeItem(TOKEN_KEY);
  storage.removeItem(USER_KEY);
  clearLegacyLocalStorage();
}

export function getToken() {
  const storage = getSessionStorage();
  const token = storage.getItem(TOKEN_KEY) || "";
  if (!token) {
    clearLegacyLocalStorage();
  }
  return token;
}

export function getStoredUser() {
  const storage = getSessionStorage();
  const raw = storage.getItem(USER_KEY);
  if (!raw) {
    clearLegacyLocalStorage();
    return null;
  }

  try {
    return JSON.parse(raw);
  } catch {
    storage.removeItem(USER_KEY);
    clearLegacyLocalStorage();
    return null;
  }
}
