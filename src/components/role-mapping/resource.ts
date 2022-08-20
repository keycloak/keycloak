import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import { addTrailingSlash } from "../../util";
import { getAuthorizationHeaders } from "../../utils/getAuthorizationHeaders";

type AvailableClientRolesQuery = {
  adminClient: KeycloakAdminClient;
  id: string;
  realm: string;
  type: string;
  first: number;
  max: number;
  search?: string;
};

type ClientRole = {
  id: string;
  role: string;
  description?: string;
  client?: string;
};

export const getAvailableClientRoles = async ({
  adminClient,
  id,
  realm,
  type,
  first,
  max,
  search,
}: AvailableClientRolesQuery): Promise<ClientRole[]> => {
  const accessToken = await adminClient.getAccessToken();
  const baseUrl = adminClient.baseUrl;

  const response = await fetch(
    `${addTrailingSlash(
      baseUrl
    )}admin/realms/${realm}/admin-ui/${type}/${id}?first=${first}&max=${max}${
      search ? "&search=" + search : ""
    }`,
    {
      method: "GET",
      headers: getAuthorizationHeaders(accessToken),
    }
  );

  return await response.json();
};
