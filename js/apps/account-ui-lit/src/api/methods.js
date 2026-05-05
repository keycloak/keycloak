import { request, parseResponse } from "./request.js";

/**
 * @typedef {Object} CallOptions
 * @property {import("keycloak-js").default} keycloak
 * @property {AbortSignal} [signal]
 */

/**
 * @param {CallOptions} options
 * @returns {Promise<import("./representations.js").UserRepresentation>}
 */
export async function getPersonalInfo({ keycloak, signal }) {
  const response = await request("/?userProfileMetadata=true", keycloak, {
    signal,
  });
  return parseResponse(response);
}

/**
 * @param {CallOptions} options
 * @returns {Promise<string[]>}
 */
export async function getSupportedLocales({ keycloak, signal }) {
  const response = await request("/supportedLocales", keycloak, { signal });
  return parseResponse(response);
}

/**
 * @param {import("keycloak-js").default} keycloak
 * @param {import("./representations.js").UserRepresentation} info
 * @returns {Promise<void>}
 */
export async function savePersonalInfo(keycloak, info) {
  const response = await request("/", keycloak, {
    body: info,
    method: "POST",
  });
  if (!response.ok) {
    const { errors } = await response.json();
    throw errors;
  }
}

/**
 * @param {CallOptions} options
 * @returns {Promise<import("./representations.js").DeviceRepresentation[]>}
 */
export async function getDevices({ keycloak, signal }) {
  const response = await request("/sessions/devices", keycloak, { signal });
  return parseResponse(response);
}

/**
 * @param {import("keycloak-js").default} keycloak
 * @param {string} [id]
 * @returns {Promise<void>}
 */
export async function deleteSession(keycloak, id) {
  await request(`/sessions${id ? `/${id}` : ""}`, keycloak, {
    method: "DELETE",
  });
}

/**
 * @param {CallOptions} options
 * @returns {Promise<import("./representations.js").ClientRepresentation[]>}
 */
export async function getApplications({ keycloak, signal }) {
  const response = await request("/applications", keycloak, { signal });
  return parseResponse(response);
}

/**
 * @param {import("keycloak-js").default} keycloak
 * @param {string} id
 * @returns {Promise<void>}
 */
export async function deleteConsent(keycloak, id) {
  await request(`/applications/${id}/consent`, keycloak, { method: "DELETE" });
}

/**
 * @param {CallOptions} options
 * @returns {Promise<import("./representations.js").CredentialContainer[]>}
 */
export async function getCredentials({ keycloak, signal }) {
  const response = await request("/credentials", keycloak, { signal });
  return parseResponse(response);
}

/**
 * @param {import("keycloak-js").default} keycloak
 * @param {string} id
 * @returns {Promise<void>}
 */
export async function deleteCredential(keycloak, id) {
  await request(`/credentials/${id}`, keycloak, { method: "DELETE" });
}

/**
 * @param {CallOptions} options
 * @returns {Promise<import("./representations.js").LinkedAccountRepresentation[]>}
 */
export async function getLinkedAccounts({ keycloak, signal }) {
  const response = await request("/linked-accounts", keycloak, { signal });
  return parseResponse(response);
}

/**
 * @param {import("keycloak-js").default} keycloak
 * @param {string} providerName
 * @param {string} redirectUri
 * @returns {Promise<{accountLinkUri: string}>}
 */
export async function linkAccount(keycloak, providerName, redirectUri) {
  const response = await request(`/linked-accounts/${providerName}`, keycloak, {
    searchParams: { redirectUri },
  });
  return parseResponse(response);
}

/**
 * @param {import("keycloak-js").default} keycloak
 * @param {string} providerName
 * @returns {Promise<void>}
 */
export async function unlinkAccount(keycloak, providerName) {
  await request(`/linked-accounts/${providerName}`, keycloak, {
    method: "DELETE",
  });
}

/**
 * @param {CallOptions} options
 * @returns {Promise<import("./representations.js").Group[]>}
 */
export async function getGroups({ keycloak, signal }) {
  const response = await request("/groups", keycloak, { signal });
  return parseResponse(response);
}

/**
 * @param {CallOptions} options
 * @returns {Promise<import("./representations.js").OrganizationRepresentation[]>}
 */
export async function getOrganizations({ keycloak, signal }) {
  const response = await request("/organizations", keycloak, { signal });
  return parseResponse(response);
}

/**
 * @param {CallOptions} options
 * @returns {Promise<import("./representations.js").Resource[]>}
 */
export async function getResources({ keycloak, signal }) {
  const response = await request("/resources", keycloak, { signal });
  return parseResponse(response);
}

/**
 * @param {import("keycloak-js").default} keycloak
 * @param {string} resourceId
 * @param {AbortSignal} [signal]
 * @returns {Promise<import("./representations.js").Permission[]>}
 */
export async function getPermissionRequests(keycloak, resourceId, signal) {
  const response = await request(
    `/resources/${resourceId}/permissions/requests`,
    keycloak,
    { signal },
  );
  return parseResponse(response);
}
