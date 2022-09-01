import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import { fetchAdminUI } from "../../context/auth/admin-ui-endpoint";

type BaseClientRolesQuery = {
  adminClient: KeycloakAdminClient;
  id: string;
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
  type,
  first,
  max,
  search,
  endpoint,
}: Query): Promise<ClientRole[]> => {
  return fetchAdminUI(adminClient, `/admin-ui-${endpoint}/${type}/${id}`, {
    first: (first || 0).toString(),
    max: (max || 10).toString(),
    search: search || "",
  });
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
