import test from "node:test";
import assert from "node:assert/strict";
import { apiFetch } from "../../main/resources/static/js/services/httpClient.js";

function installBrowserStubs(token = null) {
  const values = new Map(token ? [["token", token]] : []);
  globalThis.localStorage = {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: (key) => values.delete(key),
    values,
  };
  globalThis.window = { location: { href: "" } };
  return values;
}

test("adds bearer header without putting token in URL and preserves JSON headers", async () => {
  installBrowserStubs("jwt-token");
  let request;
  globalThis.fetch = async (url, options) => {
    request = { url, options };
    return new Response("{}", { status: 200, headers: { "Content-Type": "application/json" } });
  };

  await apiFetch("/api/appointments", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ doctorId: 7 }),
  });

  assert.equal(request.url, "/api/appointments");
  assert.equal(request.options.headers.get("Authorization"), "Bearer jwt-token");
  assert.equal(request.options.headers.get("Content-Type"), "application/json");
  assert.equal(request.url.includes("jwt-token"), false);
});

test("clears session and redirects home on unauthorized response", async () => {
  const values = installBrowserStubs("jwt-token");
  for (const key of ["userRole", "accountId", "doctorId"]) values.set(key, "value");
  globalThis.fetch = async () => new Response("{}", { status: 401 });

  await apiFetch("/api/appointments");

  assert.deepEqual([...values.keys()], []);
  assert.equal(window.location.href, "/");
});
