const SESSION_KEYS = ["token", "userRole", "accountId", "doctorId"];

function storage() {
  return globalThis.sessionStorage;
}

function clearSession() {
  const currentStorage = storage();
  if (currentStorage) SESSION_KEYS.forEach((key) => currentStorage.removeItem(key));
  if (globalThis.window?.location) globalThis.window.location.href = "/";
}

/** Shared same-origin fetch wrapper for API calls. Tokens never become URL data. */
export async function apiFetch(path, options = {}) {
  const headers = new Headers(options.headers ?? {});
  const token = storage()?.getItem("token");
  if (token && options.auth !== false) headers.set("Authorization", `Bearer ${token}`);
  if (options.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
  if (!headers.has("Accept")) headers.set("Accept", "application/json");

  const response = await fetch(path, { ...options, headers });
  if (response.status === 401) clearSession();
  return response;
}

export async function readJson(path, options = {}) {
  const response = await apiFetch(path, options);
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(body.detail || body.message || "Request failed");
  return body;
}

export { clearSession };
