import {
  BaseEnvironment,
  type KeycloakContext,
} from "@keycloak/keycloak-ui-shared";

import { joinPath } from "../utils/joinPath";
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

export async function getLinkedAccounts({ signal, context }: CallOptions) {
  const response = await request("/linked-accounts", context, { signal });
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

export async function linkAccount(
  context: KeycloakContext<BaseEnvironment>,
  account: LinkedAccountRepresentation,
) {
  const redirectUri = encodeURIComponent(
    joinPath(
      context.environment.serverBaseUrl,
      "realms",
      context.environment.realm,
      "account",
    ),
  );
  const response = await request(
    "/linked-accounts/" + account.providerName,
    context,
    {
      searchParams: { providerId: account.providerName, redirectUri },
    },
  );
  return parseResponse<{ accountLinkUri: string }>(response);
}

export async function getGroups({ signal, context }: CallOptions) {
  const response = await request("/groups", context, {
    signal,
  });
  return parseResponse<Group[]>(response);
}
