function normalizeBaseUrl(value, fallback = "") {
  const resolved = (value ?? fallback).trim();
  return resolved.replace(/\/+$/, "");
}

export const API_BASE_URL = normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL || "http://localhost:9090");

export const AUTH_API_BASE_URL = normalizeBaseUrl(
  import.meta.env.VITE_AUTH_API_BASE_URL || "http://localhost:8081"
);

export const ADMIN_API_BASE_URL = normalizeBaseUrl(
  import.meta.env.VITE_ADMIN_API_BASE_URL || API_BASE_URL || "http://localhost:9090"
);

export const OAUTH_BASE_URL =
  normalizeBaseUrl(import.meta.env.VITE_OAUTH_BASE_URL || "http://localhost:8081");
