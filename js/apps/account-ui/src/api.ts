import { KeycloakContext } from "@keycloak/keycloak-ui-shared";
import { BaseEnvironment } from "@keycloak/keycloak-ui-shared/dist/context/environment";
import { CallOptions } from "./api/methods";
import { Links, parseLinks } from "./api/parse-links";
import { parseResponse } from "./api/parse-response";
import { Permission, Resource, Scope } from "./api/representations";
import { request } from "./api/request";

export const fetchResources = async (
  { signal, context }: CallOptions,
  requestParams: Record<string, string>,
  shared: boolean | undefined = false,
): Promise<{ data: Resource[]; links: Links }> => {
  const response = await request(
    `/resources${shared ? "/shared-with-me?" : "?"}`,
    context,
    { searchParams: shared ? requestParams : undefined, signal },
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
  { signal, context }: CallOptions,
  resourceId: string,
): Promise<Permission[]> => {
  const response = await request(
    `/resources/${resourceId}/permissions`,
    context,
    { signal },
  );
  return parseResponse<Permission[]>(response);
};

export const updateRequest = (
  context: KeycloakContext<BaseEnvironment>,
  resourceId: string,
  username: string,
  scopes: Scope[] | string[],
) =>
  request(`/resources/${resourceId}/permissions`, context, {
    method: "PUT",
    body: [{ username, scopes }],
  });

export const updatePermissions = (
  context: KeycloakContext<BaseEnvironment>,
  resourceId: string,
  permissions: Permission[],
) =>
  request(`/resources/${resourceId}/permissions`, context, {
    method: "PUT",
    body: permissions,
  });

function checkResponse<T>(response: T) {
  if (!response) throw new Error("Could not fetch");
  return response;
}
