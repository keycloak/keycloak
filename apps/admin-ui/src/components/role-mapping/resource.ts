import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import { fetchAdminUI } from "../../context/auth/admin-ui-endpoint";

type BaseQuery = {
  adminClient: KeycloakAdminClient;
  id: string;
  type: string;
};

type PaginatingQuery = BaseQuery & {
  first: number;
  max: number;
  search?: string;
};

type EffectiveClientRolesQuery = BaseQuery;

type Query = Partial<Omit<PaginatingQuery, "adminClient">> & {
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

const fetchEndpoint = async ({
  adminClient,
  id,
  type,
  first,
  max,
  search,
  endpoint,
}: Query): Promise<any> => {
  return fetchAdminUI(adminClient, `/admin-ui-${endpoint}/${type}/${id}`, {
    first: (first || 0).toString(),
    max: (max || 10).toString(),
    search: search || "",
  });
};

export const getAvailableClientRoles = (
  query: PaginatingQuery
): Promise<ClientRole[]> =>
  fetchEndpoint({ ...query, endpoint: "available-roles" });

export const getEffectiveClientRoles = (
  query: EffectiveClientRolesQuery
): Promise<ClientRole[]> =>
  fetchEndpoint({ ...query, endpoint: "effective-roles" });

export const fetchUsedBy = (query: PaginatingQuery): Promise<string[]> =>
  fetchEndpoint({ ...query, endpoint: "authentication-management" });
