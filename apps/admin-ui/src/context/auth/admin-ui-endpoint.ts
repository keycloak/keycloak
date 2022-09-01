import KeycloakAdminClient from "@keycloak/keycloak-admin-client";

import { getAuthorizationHeaders } from "../../utils/getAuthorizationHeaders";
import { joinPath } from "../../utils/joinPath";

export async function fetchAdminUI(
  adminClient: KeycloakAdminClient,
  endpoint: string,
  query?: Record<string, string>
) {
  const accessToken = await adminClient.getAccessToken();
  const baseUrl = adminClient.baseUrl;

  const response = await fetch(
    joinPath(baseUrl, "admin/realms", adminClient.realmName, endpoint) +
      (query ? "?" + new URLSearchParams(query) : ""),
    {
      method: "GET",
      headers: getAuthorizationHeaders(accessToken),
    }
  );

  return await response.json();
}
