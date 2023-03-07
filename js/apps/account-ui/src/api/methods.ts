import { environment } from "../environment";
import { joinPath } from "../utils/joinPath";
import { parseResponse } from "./parse-response";
import {
  ClientRepresentation,
  CredentialContainer,
  CredentialRepresentation,
  DeviceRepresentation,
  Group,
  LinkedAccountRepresentation,
  Permission,
  UserRepresentation,
} from "./representations";
import { request } from "./request";

export type CallOptions = {
  signal?: AbortSignal;
};

export type PaginationParams = {
  first: number;
  max: number;
};

export async function getPersonalInfo({
  signal,
}: CallOptions = {}): Promise<UserRepresentation> {
  const response = await request("/?userProfileMetadata=true", { signal });
  return parseResponse<UserRepresentation>(response);
}

export async function savePersonalInfo(
  info: UserRepresentation
): Promise<void> {
  const response = await request("/", { body: info, method: "POST" });
  if (!response.ok) {
    const { errors } = await response.json();
    throw errors;
  }
  return undefined;
}

export async function getPermissionRequests(
  resourceId: string,
  { signal }: CallOptions = {}
): Promise<Permission[]> {
  const response = await request(
    `/resources/${resourceId}/permissions/requests`,
    { signal }
  );

  return parseResponse<Permission[]>(response);
}

export async function getDevices({
  signal,
}: CallOptions): Promise<DeviceRepresentation[]> {
  const response = await request("/sessions/devices", { signal });
  return parseResponse<DeviceRepresentation[]>(response);
}

export async function getApplications({ signal }: CallOptions = {}): Promise<
  ClientRepresentation[]
> {
  const response = await request("/applications", { signal });
  return parseResponse<ClientRepresentation[]>(response);
}

export async function deleteConsent(id: string) {
  return request(`/applications/${id}/consent`, { method: "DELETE" });
}

export async function deleteSession(id?: string) {
  return request(`"/sessions${id ? `/${id}` : ""}`, {
    method: "DELETE",
  });
}

export async function getCredentials({ signal }: CallOptions) {
  const response = await request("/credentials", {
    signal,
  });
  return parseResponse<CredentialContainer[]>(response);
}

export async function deleteCredentials(credential: CredentialRepresentation) {
  return request("/credentials/" + credential.id, {
    method: "DELETE",
  });
}

export async function getLinkedAccounts({ signal }: CallOptions) {
  const response = await request("/linked-accounts", { signal });
  return parseResponse<LinkedAccountRepresentation[]>(response);
}

export async function unLinkAccount(account: LinkedAccountRepresentation) {
  const response = await request("/linked-accounts/" + account.providerName, {
    method: "DELETE",
  });
  return parseResponse(response);
}

export async function linkAccount(account: LinkedAccountRepresentation) {
  const redirectUri = encodeURIComponent(
    joinPath(
      environment.authServerUrl,
      "realms",
      environment.loginRealm,
      "account"
    )
  );
  const response = await request("/linked-accounts/" + account.providerName, {
    searchParams: { providerId: account.providerName, redirectUri },
  });
  return parseResponse<{ accountLinkUri: string }>(response);
}

export async function getGroups({ signal }: CallOptions) {
  const response = await request("/groups", {
    signal,
  });
  return parseResponse<Group[]>(response);
}
