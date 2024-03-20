import { getRootPath } from "../src/utils/getRootPath";

function getTestServerUrl(): string {
  return process.env.KEYCLOAK_SERVER ?? "http://localhost:8080";
}

export function getKeycloakServerUrl(): string {
  // In CI, the Keycloak server is running in the same server as tested console, while in dev, it is running on a different port
  return process.env.CI ? getTestServerUrl() : "http://localhost:8180";
}

export function getAccountUrl() {
  return getTestServerUrl() + getRootPath();
}

export function getAdminUrl() {
  return getKeycloakServerUrl() + "/admin/master/console/";
}
