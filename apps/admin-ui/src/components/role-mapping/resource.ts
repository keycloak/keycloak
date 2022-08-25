import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import { addTrailingSlash } from "../../util";
import { getAuthorizationHeaders } from "../../utils/getAuthorizationHeaders";

type BaseClientRolesQuery = {
  adminClient: KeycloakAdminClient;
  id: string;
  realm: string;
  type: string;
};

type AvailableClientRolesQuery = BaseClientRolesQuery & {
  first: number;
  max: number;
  search?: string;
};

type EffectiveClientRolesQuery = BaseClientRolesQuery;

type Query = Partial<Omit<AvailableClientRolesQuery, "adminClient">> & {
  adminClient: KeycloakAdminClient;
  endpoint: string;
};

type ClientRole = {
  id: string;
  role: string;
  description?: string;
  client: string;
  clientId: string;
};

const fetchRoles = async ({
  adminClient,
  id,
  realm,
  type,
  first,
  max,
  search,
  endpoint,
}: Query): Promise<ClientRole[]> => {
  const accessToken = await adminClient.getAccessToken();
  const baseUrl = adminClient.baseUrl;

  const response = await fetch(
    `${addTrailingSlash(
      baseUrl
    )}admin/realms/${realm}/admin-ui-${endpoint}/${type}/${id}?first=${
      first || 0
    }&max=${max || 10}${search ? "&search=" + search : ""}`,
    {
      method: "GET",
      headers: getAuthorizationHeaders(accessToken),
    }
  );

  return await response.json();
};

export const getAvailableClientRoles = async (
  query: AvailableClientRolesQuery
): Promise<ClientRole[]> => {
  return fetchRoles({ ...query, endpoint: "available-roles" });
};

export const getEffectiveClientRoles = async (
  query: EffectiveClientRolesQuery
): Promise<ClientRole[]> => {
  return fetchRoles({ ...query, endpoint: "effective-roles" });
};
