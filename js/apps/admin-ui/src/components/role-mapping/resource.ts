import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";

import { fetchAdminUI } from "../../context/auth/admin-ui-endpoint";
import KeycloakAdminClient from "@keycloak/keycloak-admin-client";

type IDQuery = {
  id: string;
  type: string;
};

type PaginatingQuery = IDQuery & {
  first: number;
  max: number;
  search?: string;
};

type Query = Partial<Omit<PaginatingQuery, "adminClient">> & {
  endpoint: string;
};

const fetchEndpoint = async (
  adminClient: KeycloakAdminClient,
  { id, type, first, max, search, endpoint }: Query,
): Promise<any> =>
  fetchAdminUI(
    adminClient,
    `/ui-ext/${endpoint}/${type}/${encodeURIComponent(id!)}`,
    {
      first: (first || 0).toString(),
      max: (max || 10).toString(),
      search: search || "",
    },
  );

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

export const findUsers = (
  adminClient: KeycloakAdminClient,
  query: UserQuery,
): Promise<BruteUser[]> =>
  fetchAdminUI(
    adminClient,
    "ui-ext/brute-force-user",
    query as Record<string, string>,
  );

export type UsedByClientRef = {
  id?: string | null;
  clientId: string;
};

export type UsedByReference = {
  id?: string | null;
  label: string;
};

export const fetchUsedBy = (
  adminClient: KeycloakAdminClient,
  query: PaginatingQuery,
): Promise<UsedByReference[]> =>
  fetchEndpoint(adminClient, {
    ...query,
    endpoint: "authentication-management",
  });
