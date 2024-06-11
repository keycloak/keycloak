import {
  getInjectedEnvironment,
  type BaseEnvironment,
} from "@keycloak/keycloak-ui-shared";

export type Environment = BaseEnvironment & {
  /** The URL to the base of the Admin Console. */
  consoleBaseUrl: string;
  /** The name of the master realm. */
  masterRealm: string;
  /** The version hash of the auth server. */
  resourceVersion: string;
};

// During development the realm can be passed as a query parameter when redirecting back from Keycloak.
const realm =
  new URLSearchParams(window.location.search).get("realm") || "master";

const defaultEnvironment: Environment = {
  // Base environment variables
  authServerUrl: "http://localhost:8180",
  realm: realm,
  clientId: "security-admin-console-v2",
  resourceUrl: "http://localhost:8080",
  logo: "/logo.svg",
  logoUrl: "",
  // Admin Console specific environment variables
  consoleBaseUrl: "/admin/master/console/",
  masterRealm: "master",
  resourceVersion: "unknown",
};

export const environment = getInjectedEnvironment(defaultEnvironment);
