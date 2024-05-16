import KeycloakAdminClient, {
  fetchWithError,
} from "@keycloak/keycloak-admin-client";
import { getAuthorizationHeaders } from "../../utils/getAuthorizationHeaders";
import { joinPath } from "../../utils/joinPath";

export async function fetchAdminUI<T>(
  adminClient: KeycloakAdminClient,
  endpoint: string,
  query?: Record<string, string>,
): Promise<T> {
  const accessToken = await adminClient.getAccessToken();
  const baseUrl = adminClient.baseUrl;

  const response = await fetchWithError(
    joinPath(
      baseUrl,
      "admin/realms",
      encodeURIComponent(adminClient.realmName),
      endpoint,
    ) + (query ? "?" + new URLSearchParams(query) : ""),
    {
      method: "GET",
      headers: getAuthorizationHeaders(accessToken),
    },
  );

  return await response.json();
}
