import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";

import {
  fetchAdminUI,
  postAdminUI,
} from "../../context/auth/admin-ui-endpoint";
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

export type EffectiveRole = {
  id: string;
  name: string;
  description?: string;
  clientRole: boolean;
  client?: string;
  clientId?: string;
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

export const getAvailableClientRoles = (
  adminClient: KeycloakAdminClient,
  query: PaginatingQuery,
): Promise<ClientRole[]> =>
  fetchEndpoint(adminClient, { ...query, endpoint: "available-roles" });

export const getEffectiveClientRoles = (
  adminClient: KeycloakAdminClient,
  query: EffectiveClientRolesQuery,
): Promise<ClientRole[]> =>
  fetchEndpoint(adminClient, { ...query, endpoint: "effective-roles" });

export const getAllEffectiveRoles = (
  adminClient: KeycloakAdminClient,
  query: EffectiveClientRolesQuery,
): Promise<EffectiveRole[]> =>
  fetchEndpoint(adminClient, { ...query, endpoint: "effective-roles-all" });

type RoleRepresentation = {
  id: string;
  name: string;
  description?: string;
  composite: boolean;
  clientRole: boolean;
  containerId: string;
};

type ClientMappingRepresentation = {
  id: string;
  client: string;
  mappings: RoleRepresentation[];
};

export type RoleMappingRepresentation = {
  realmMappings?: RoleRepresentation[];
  clientMappings?: Record<string, ClientMappingRepresentation>;
};

export const getRoleMappings = async (
  adminClient: KeycloakAdminClient,
  id: string,
): Promise<RoleMappingRepresentation> =>
  fetchAdminUI(
    adminClient,
    `/ui-ext/role-mappings/roles/${encodeURIComponent(id)}`,
    {},
  );

export type RoleDeleteRequest = {
  roleId: string;
  roleName: string;
  clientId?: string;
};

export const deleteRoleMappings = async (
  adminClient: KeycloakAdminClient,
  type: string,
  id: string,
  roles: RoleDeleteRequest[],
): Promise<void> => {
  await postAdminUI(
    adminClient,
    `/ui-ext/role-mapping-delete/${type}/${encodeURIComponent(id)}`,
    roles,
  );
};

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

export const fetchUsedBy = (
  adminClient: KeycloakAdminClient,
  query: PaginatingQuery,
): Promise<string[]> =>
  fetchEndpoint(adminClient, {
    ...query,
    endpoint: "authentication-management",
  });
