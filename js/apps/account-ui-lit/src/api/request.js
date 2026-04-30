import { environment } from "../environment.js";

/**
 * Refresh the token if it expires within the next 5 seconds.
 * This ensures tokens are only refreshed when actively used.
 * @param {import("keycloak-js").default} keycloak
 * @returns {Promise<string | undefined>}
 */
async function getAccessToken(keycloak) {
  try {
    await keycloak.updateToken(5);
  } catch {
    await keycloak.login();
  }
  return keycloak.token;
}

/**
 * @param {string} path
 * @param {import("keycloak-js").default} keycloak
 * @param {RequestInit & {searchParams?: Record<string, string>}} [opts]
 * @returns {Promise<Response>}
 */
export async function request(path, keycloak, opts = {}) {
  const url = new URL(
    `${environment.serverBaseUrl}/realms/${environment.realm}/account${path}`,
  );

  if (opts.searchParams) {
    Object.entries(opts.searchParams).forEach(([key, value]) => {
      url.searchParams.set(key, value);
    });
  }

  const token = await getAccessToken(keycloak);

  const headers = {
    Authorization: `Bearer ${token}`,
    Accept: "application/json",
    ...(opts.body ? { "Content-Type": "application/json" } : {}),
  };

  return fetch(url.toString(), {
    ...opts,
    headers: { ...headers, ...opts.headers },
    body: opts.body ? JSON.stringify(opts.body) : undefined,
  });
}

/**
 * @template T
 * @param {Response} response
 * @returns {Promise<T>}
 */
export async function parseResponse(response) {
  if (!response.ok) {
    throw new Error(
      `Request failed: ${response.status} ${response.statusText}`,
    );
  }
  return response.json();
}
