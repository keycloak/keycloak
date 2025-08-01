import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation.js";
import { generatePath } from "react-router-dom";

export const SERVER_URL = new URL("http://localhost:8080");
export const ROOT_PATH = "/realms/:realm/account" as const;
export const ADMIN_PASSWORD = "admin";
export const ADMIN_USERNAME = "admin";
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
} satisfies UserRepresentation;

export function getAccountUrl(realm: string): URL {
  return new URL(generatePath(ROOT_PATH, { realm }), SERVER_URL);
}
