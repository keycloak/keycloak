import { parseResponse } from "./parse-response";
import {
  ClientRepresentation,
  DeviceRepresentation,
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
  const response = await request("/", { signal });
  return parseResponse<UserRepresentation>(response);
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
