import type { ConfigPropertyRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigInfoRepresentation";
import { matchPath } from "react-router-dom";

export type StorageType =
  | "COMPONENT"
  | "CLIENT"
  | "USER"
  | "IDENTITY_PROVIDER"
  | "CUSTOM";

export type ConfigMapTarget = "string-map" | "list-map";

export function resolveTabParams(
  pathname: string,
  metadataPath: string | undefined,
  routeParams: Record<string, string | undefined>,
): Record<string, string> {
  const resolved: Record<string, string> = {};

  for (const [key, value] of Object.entries(routeParams)) {
    if (value !== undefined) {
      resolved[key] = value;
    }
  }

  if (metadataPath) {
    const match = matchPath({ path: metadataPath, end: false }, pathname);
    if (match?.params) {
      for (const [key, value] of Object.entries(match.params)) {
        if (value !== undefined) {
          resolved[key] = value;
        }
      }
    }
  }

  return resolved;
}

export function getEntityId(
  storageType: StorageType,
  params: Record<string, string | undefined>,
): string | undefined {
  switch (storageType) {
    case "CLIENT":
      return params.clientId!;
    case "USER":
      return params.id!;
    case "IDENTITY_PROVIDER":
      return params.alias!;
    default:
      return undefined;
  }
}

export function isEntityStorageType(
  storageType: StorageType,
): storageType is "CLIENT" | "USER" | "IDENTITY_PROVIDER" {
  return (
    storageType === "CLIENT" ||
    storageType === "USER" ||
    storageType === "IDENTITY_PROVIDER"
  );
}

function isClearedValue(value: unknown): boolean {
  if (value === undefined || value === null || value === "") {
    return true;
  }

  if (Array.isArray(value)) {
    return value.length === 0 || value.every((item) => isClearedValue(item));
  }

  return false;
}

export function pickDeclaredConfig(
  config: Record<string, unknown> | undefined,
  properties: ConfigPropertyRepresentation[],
): Record<string, unknown> {
  const result: Record<string, unknown> = {};

  for (const property of properties) {
    const key = property.name!;
    const value = config?.[key];
    if (value !== undefined && value !== null) {
      result[key] = value;
    }
  }

  return result;
}

export function isStringMapStorageType(storageType: StorageType): boolean {
  return storageType === "CLIENT" || storageType === "IDENTITY_PROVIDER";
}

export function mergeEntityConfig(
  existing: Record<string, unknown> | undefined,
  config: Record<string, unknown> | undefined,
  properties: ConfigPropertyRepresentation[],
  target: ConfigMapTarget,
): Record<string, unknown> {
  const result: Record<string, unknown> = { ...(existing || {}) };

  for (const property of properties) {
    const key = property.name!;
    const value = config?.[key];

    if (isClearedValue(value)) {
      delete result[key];
      continue;
    }

    if (target === "list-map") {
      result[key] = Array.isArray(value) ? value : [value];
    } else {
      result[key] = Array.isArray(value) ? value.join("##") : String(value);
    }
  }

  return result;
}

export function interpolateEndpoint(
  endpoint: string,
  params: Record<string, string | undefined>,
): string {
  const missing: string[] = [];
  const result = endpoint.replace(/\{(\w+)\}/g, (_, key) => {
    const value = params[key];
    if (value == null || value === "") {
      missing.push(key);
      return `{${key}}`;
    }
    return encodeURIComponent(value);
  });
  if (missing.length > 0) {
    throw new Error(`Missing endpoint parameter(s): ${missing.join(", ")}`);
  }
  return result;
}
