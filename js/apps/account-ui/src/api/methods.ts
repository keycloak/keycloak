import {
  BaseEnvironment,
  type KeycloakContext,
} from "@keycloak/keycloak-ui-shared";

import OrganizationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationRepresentation";
import { parseResponse } from "./parse-response";
import {
  ClientRepresentation,
  CredentialContainer,
  DeviceRepresentation,
  Group,
  LinkedAccountRepresentation,
  Permission,
  UserRepresentation,
} from "./representations";
import { request } from "./request";

export type CallOptions = {
  context: KeycloakContext<BaseEnvironment>;
  signal?: AbortSignal;
};

export type PaginationParams = {
  first: number;
  max: number;
};

export async function getPersonalInfo({
  signal,
  context,
}: CallOptions): Promise<UserRepresentation> {
  const response = await request("/?userProfileMetadata=true", context, {
    signal,
  });
  return parseResponse<UserRepresentation>(response);
}

export async function getSupportedLocales({
  signal,
  context,
}: CallOptions): Promise<string[]> {
  const response = await request("/supportedLocales", context, { signal });
  return parseResponse<string[]>(response);
}

export async function savePersonalInfo(
  context: KeycloakContext<BaseEnvironment>,
  info: UserRepresentation,
): Promise<void> {
  const response = await request("/", context, { body: info, method: "POST" });
  if (!response.ok) {
    const { errors } = await response.json();
    throw errors;
  }
  return undefined;
}

export async function getPermissionRequests(
  resourceId: string,
  { signal, context }: CallOptions,
): Promise<Permission[]> {
  const response = await request(
    `/resources/${resourceId}/permissions/requests`,
    context,
    { signal },
  );

  return parseResponse<Permission[]>(response);
}

export async function getDevices({
  signal,
  context,
}: CallOptions): Promise<DeviceRepresentation[]> {
  const response = await request("/sessions/devices", context, { signal });
  return parseResponse<DeviceRepresentation[]>(response);
}

export async function getApplications({
  signal,
  context,
}: CallOptions): Promise<ClientRepresentation[]> {
  const response = await request("/applications", context, { signal });
  return parseResponse<ClientRepresentation[]>(response);
}

export async function deleteConsent(
  context: KeycloakContext<BaseEnvironment>,
  id: string,
) {
  return request(`/applications/${id}/consent`, context, { method: "DELETE" });
}

export async function deleteSession(
  context: KeycloakContext<BaseEnvironment>,
  id?: string,
) {
  return request(`/sessions${id ? `/${id}` : ""}`, context, {
    method: "DELETE",
  });
}

export async function getCredentials({ signal, context }: CallOptions) {
  const response = await request("/credentials", context, {
    signal,
  });
  return parseResponse<CredentialContainer[]>(response);
}

export type LinkedAccountQueryParams = PaginationParams & {
  search?: string;
  linked?: boolean;
};

export async function getLinkedAccounts(
  { signal, context }: CallOptions,
  query: LinkedAccountQueryParams,
) {
  const response = await request("/linked-accounts", context, {
    searchParams: Object.entries(query).reduce(
      (acc, [key, value]) => ({ ...acc, [key]: value.toString() }),
      {},
    ),
    signal,
  });
  return parseResponse<LinkedAccountRepresentation[]>(response);
}

export async function unLinkAccount(
  context: KeycloakContext<BaseEnvironment>,
  account: LinkedAccountRepresentation,
) {
  const response = await request(
    "/linked-accounts/" + account.providerName,
    context,
    {
      method: "DELETE",
    },
  );
  if (response.ok) return;
  return parseResponse(response);
}

export async function getGroups({ signal, context }: CallOptions) {
  const response = await request("/groups", context, {
    signal,
  });
  return parseResponse<Group[]>(response);
}

export async function getUserOrganizations({ signal, context }: CallOptions) {
  const response = await request("/organizations", context, { signal });
  return parseResponse<OrganizationRepresentation[]>(response);
}
