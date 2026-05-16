function normalizeBaseUrl(value, fallback = "") {
  const resolved = (value ?? fallback).trim();
  return resolved.replace(/\/+$/, "");
}

export const API_BASE_URL = normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL || "");

export const AUTH_API_BASE_URL = normalizeBaseUrl(
  import.meta.env.VITE_AUTH_API_BASE_URL || API_BASE_URL
);

export const ADMIN_API_BASE_URL = normalizeBaseUrl(
  import.meta.env.VITE_ADMIN_API_BASE_URL || API_BASE_URL
);

export const OAUTH_BASE_URL =
  normalizeBaseUrl(import.meta.env.VITE_OAUTH_BASE_URL || API_BASE_URL);
