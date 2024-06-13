import ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { joinPath } from "./joinPath";

export const convertClientToUrl = (
  { rootUrl, baseUrl }: ClientRepresentation,
  adminClientBaseUrl: string,
) => {
  // absolute base url configured, use base url is
  if (baseUrl?.startsWith("http")) {
    return baseUrl;
  }

  if (
    (rootUrl === "${authBaseUrl}" || rootUrl === "${authAdminUrl}") &&
    baseUrl
  ) {
    return rootUrl.replace(
      /\$\{(authAdminUrl|authBaseUrl)\}/,
      joinPath(adminClientBaseUrl, baseUrl),
    );
  }

  if (rootUrl?.startsWith("http")) {
    if (baseUrl) {
      return joinPath(rootUrl, baseUrl);
    }
    return rootUrl;
  }

  return baseUrl;
};
