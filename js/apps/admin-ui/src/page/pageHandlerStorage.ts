import type { ConfigPropertyRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigInfoRepresentation";
import { matchPath } from "react-router-dom";

const MULTIVALUED_DELIMITER = "##";

const MULTIVALUED_TYPES = new Set(["MultivaluedString", "MultivaluedList"]);

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

function isMultivaluedProperty(
  property: ConfigPropertyRepresentation,
): boolean {
  return !!property.type && MULTIVALUED_TYPES.has(property.type);
}

export function normalizeConfig(
  config: Record<string, unknown> | undefined,
  properties: ConfigPropertyRepresentation[],
  direction: "load" | "save",
  target: ConfigMapTarget,
): Record<string, unknown> {
  const result: Record<string, unknown> = {};
  const multivaluedNames = new Set(
    properties.filter(isMultivaluedProperty).map((property) => property.name!),
  );

  for (const [key, value] of Object.entries(config || {})) {
    if (value === undefined || value === null) {
      continue;
    }

    if (direction === "load") {
      if (
        target === "string-map" &&
        multivaluedNames.has(key) &&
        typeof value === "string"
      ) {
        result[key] = value.split(MULTIVALUED_DELIMITER);
      } else {
        result[key] = Array.isArray(value) ? value : [value];
      }
    } else if (target === "string-map") {
      result[key] = Array.isArray(value)
        ? value.join(MULTIVALUED_DELIMITER)
        : String(value);
    } else {
      result[key] = Array.isArray(value) ? value : [value];
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
