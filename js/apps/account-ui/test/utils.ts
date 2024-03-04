import { getRootPath } from "../src/utils/getRootPath";

export function getAccountUrl() {
  return getBaseUrl() + getRootPath();
}

export function getAdminUrl() {
  return getBaseUrl() + "/admin/master/console/";
}

function getBaseUrl(): string {
  return process.env.KEYCLOAK_SERVER ?? "http://localhost:8080";
}
