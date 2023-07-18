import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";

import { fetchAdminUI } from "../../context/auth/admin-ui-endpoint";

type IDQuery = {
  id: string;
  type: string;
};

type PaginatingQuery = IDQuery & {
  first: number;
  max: number;
  search?: string;
};

type EffectiveClientRolesQuery = IDQuery;

type Query = Partial<Omit<PaginatingQuery, "adminClient">> & {
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
  id,
  type,
  first,
  max,
  search,
  endpoint,
}: Query): Promise<any> =>
  fetchAdminUI(`/ui-ext/${endpoint}/${type}/${id}`, {
    first: (first || 0).toString(),
    max: (max || 10).toString(),
    search: search || "",
  });

export const getAvailableClientRoles = (
  query: PaginatingQuery,
): Promise<ClientRole[]> =>
  fetchEndpoint({ ...query, endpoint: "available-roles" });

export const getEffectiveClientRoles = (
  query: EffectiveClientRolesQuery,
): Promise<ClientRole[]> =>
  fetchEndpoint({ ...query, endpoint: "effective-roles" });

type UserQuery = {
  lastName?: string;
  firstName?: string;
  email?: string;
  username?: string;
  emailVerified?: boolean;
  idpAlias?: string;
  idpUserId?: string;
  enabled?: boolean;
  briefRepresentation?: boolean;
  exact?: boolean;
  q?: string;
};

export type BruteUser = UserRepresentation & {
  bruteForceStatus?: Record<string, object>;
};

export const findUsers = (query: UserQuery): Promise<BruteUser[]> =>
  fetchAdminUI("ui-ext/brute-force-user", query as Record<string, string>);

export const fetchUsedBy = (query: PaginatingQuery): Promise<string[]> =>
  fetchEndpoint({ ...query, endpoint: "authentication-management" });
