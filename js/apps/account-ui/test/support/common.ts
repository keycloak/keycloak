import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation.js";
import { generatePath } from "react-router-dom";

export const SERVER_URL = "http://localhost:8080";
export const ACCOUNT_ROOT_PATH = "/realms/:realm/account" as const;
export const ADMIN_ROOT_PATH = "/admin/:realm/console" as const;
export const DEFAULT_REALM = "master";
export const ADMIN_CLIENT_ID = "security-admin-console";
export const ADMIN_USERNAME = "admin";
export const ADMIN_PASSWORD = "admin";
export const DEFAULT_USERNAME = "jdoe";
export const DEFAULT_PASSWORD = "jdoe";

export const DEFAULT_USER = {
  username: DEFAULT_USERNAME,
  firstName: "John",
  lastName: "Doe",
  email: "jdoe@keycloak.org",
  enabled: true,
  credentials: [
    {
      type: "password",
      value: DEFAULT_PASSWORD,
    },
  ],
  clientRoles: {
    account: ["manage-account"],
  },
} satisfies UserRepresentation;

export function getAccountUrl(realm: string): URL {
  return new URL(generatePath(ACCOUNT_ROOT_PATH, { realm }), SERVER_URL);
}

export function getAdminUrl(realm: string): URL {
  return new URL(generatePath(ADMIN_ROOT_PATH, { realm }), SERVER_URL);
}
