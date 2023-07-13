import { Links, parseLinks } from "./api/parse-links";
import { Permission, Resource, Scope } from "./api/representations";
import { environment } from "./environment";
import { keycloak } from "./keycloak";
import { joinPath } from "./utils/joinPath";

export const fetchResources = async (
  params: RequestInit,
  requestParams: Record<string, string>,
  shared: boolean | undefined = false,
): Promise<{ data: Resource[]; links: Links }> => {
  const response = await get(
    `/resources${shared ? "/shared-with-me?" : "?"}${
      shared ? "" : new URLSearchParams(requestParams)
    }`,
    params,
  );

  let links: Links;

  try {
    links = parseLinks(response);
  } catch (error) {
    links = {};
  }

  return {
    data: checkResponse(await response.json()),
    links,
  };
};

export const fetchPermission = async (
  params: RequestInit,
  resourceId: string,
): Promise<Permission[]> => {
  const response = await request<Permission[]>(
    `/resources/${resourceId}/permissions`,
    params,
  );
  return checkResponse(response);
};

export const updateRequest = (
  resourceId: string,
  username: string,
  scopes: Scope[] | string[],
) =>
  request(`/resources/${resourceId}/permissions`, {
    method: "put",
    body: JSON.stringify([{ username, scopes }]),
  });

export const updatePermissions = (
  resourceId: string,
  permissions: Permission[],
) =>
  request(`/resources/${resourceId}/permissions`, {
    method: "put",
    body: JSON.stringify(permissions),
  });

function checkResponse<T>(response: T) {
  if (!response) throw new Error("Could not fetch");
  return response;
}

async function get(path: string, params: RequestInit): Promise<Response> {
  const url = joinPath(
    environment.authUrl,
    "realms",
    environment.realm,
    "account",
    path,
  );

  const response = await fetch(url, {
    ...params,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${await getAccessToken()}`,
    },
  });

  if (!response.ok) {
    throw new Error(response.statusText);
  }
  return response;
}

async function request<T>(
  path: string,
  params: RequestInit,
): Promise<T | undefined> {
  const response = await get(path, params);
  if (response.status !== 204) return response.json();
}

async function getAccessToken() {
  try {
    await keycloak.updateToken(5);
  } catch (error) {
    keycloak.login();
  }

  return keycloak.token;
}
