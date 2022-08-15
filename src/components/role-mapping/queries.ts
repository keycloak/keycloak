import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import type MappingsRepresentation from "@keycloak/keycloak-admin-client/lib/defs/mappingsRepresentation";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type { ClientScopes } from "@keycloak/keycloak-admin-client/lib/resources/clientScopes";
import type { Groups } from "@keycloak/keycloak-admin-client/lib/resources/groups";
import type { Roles } from "@keycloak/keycloak-admin-client/lib/resources/roles";
import type { Users } from "@keycloak/keycloak-admin-client/lib/resources/users";
import type { Clients } from "@keycloak/keycloak-admin-client/lib/resources/clients";
import type KeycloakAdminClient from "@keycloak/keycloak-admin-client";

import { Row } from "./RoleMapping";

export type ResourcesKey = keyof KeycloakAdminClient;

type DeleteFunctions =
  | keyof Pick<Groups, "delClientRoleMappings" | "delRealmRoleMappings">
  | keyof Pick<
      ClientScopes,
      "delClientScopeMappings" | "delRealmScopeMappings"
    >;

type ListEffectiveFunction =
  | keyof Pick<Groups, "listRoleMappings" | "listAvailableRealmRoleMappings">
  | keyof Pick<
      ClientScopes,
      | "listScopeMappings"
      | "listAvailableRealmScopeMappings"
      | "listCompositeClientScopeMappings"
    >
  | keyof Pick<Roles, "getCompositeRoles">
  | keyof Pick<
      Users,
      "listCompositeClientRoleMappings" | "listCompositeRealmRoleMappings"
    >;

type ListAvailableFunction =
  | keyof Pick<
      Groups,
      "listAvailableClientRoleMappings" | "listAvailableRealmRoleMappings"
    >
  | keyof Pick<
      ClientScopes,
      "listAvailableClientScopeMappings" | "listAvailableRealmScopeMappings"
    >
  | keyof Pick<Roles, "find">
  | keyof Pick<Clients, "listRoles">;

type FunctionMapping = {
  delete: DeleteFunctions[];
  listAvailable: ListAvailableFunction[];
  listEffective: ListEffectiveFunction[];
};

type ResourceMapping = Partial<Record<ResourcesKey, FunctionMapping>>;
const groupFunctions: FunctionMapping = {
  delete: ["delClientRoleMappings", "delRealmRoleMappings"],
  listEffective: [
    "listRoleMappings",
    "listCompositeRealmRoleMappings",
    "listCompositeClientRoleMappings",
  ],
  listAvailable: [
    "listAvailableClientRoleMappings",
    "listAvailableRealmRoleMappings",
  ],
};

const clientFunctions: FunctionMapping = {
  delete: ["delClientScopeMappings", "delRealmScopeMappings"],
  listEffective: [
    "listScopeMappings",
    "listAvailableRealmScopeMappings",
    "listCompositeClientScopeMappings",
  ],
  listAvailable: [
    "listAvailableClientScopeMappings",
    "listAvailableRealmScopeMappings",
  ],
};

const mapping: ResourceMapping = {
  groups: groupFunctions,
  users: groupFunctions,
  clientScopes: clientFunctions,
  clients: clientFunctions,
  roles: {
    delete: [],
    listEffective: ["getCompositeRoles", "getCompositeRoles"],
    listAvailable: ["listRoles", "find"],
  },
};

type queryType =
  | DeleteFunctions
  | ListAvailableFunction
  | ListEffectiveFunction;

const castAdminClient = (
  adminClient: KeycloakAdminClient,
  resource: ResourcesKey
) =>
  adminClient[resource] as unknown as {
    [index in queryType]: (...params: any) => Promise<RoleRepresentation[]>;
  };

const applyQuery = (
  adminClient: KeycloakAdminClient,
  type: ResourcesKey,
  query: queryType,
  ...params: object[]
): Promise<RoleRepresentation[]> =>
  castAdminClient(adminClient, type)[query](...params);

export const deleteMapping = (
  adminClient: KeycloakAdminClient,
  type: ResourcesKey,
  id: string,
  rows: Row[]
) =>
  rows.map((row) => {
    const role = { id: row.role.id!, name: row.role.name! };
    const query = mapping[type]?.delete[row.client ? 0 : 1]!;

    return applyQuery(
      adminClient,
      type,
      query,
      {
        id,
        clientUniqueId: row.client?.id,
        client: row.client?.id,
        roles: [role],
      },
      [role]
    );
  });

export const getMapping = async (
  adminClient: KeycloakAdminClient,
  type: ResourcesKey,
  id: string
): Promise<MappingsRepresentation> => {
  const query = mapping[type]!.listEffective[0];
  return applyQuery(adminClient, type, query, { id }) as MappingsRepresentation;
};

export const getEffectiveRoles = async (
  adminClient: KeycloakAdminClient,
  type: ResourcesKey,
  id: string
): Promise<Row[]> => {
  const query = mapping[type]!.listEffective[1];
  return (await applyQuery(adminClient, type, query, { id })).map((role) => ({
    role,
  }));
};

export const getEffectiveClientRoles = async (
  adminClient: KeycloakAdminClient,
  type: ResourcesKey,
  id: string,
  client: ClientRepresentation
): Promise<Row[]> => {
  const query = mapping[type]!.listEffective[2];
  return (
    await applyQuery(adminClient, type, query, {
      id,
      client: client.id,
      clientUniqueId: client.id,
    })
  ).map((role) => ({ role, client: { clientId: client.id, ...client } }));
};

export const getAvailableRoles = async (
  adminClient: KeycloakAdminClient,
  type: ResourcesKey,
  params: Record<string, string | number>
): Promise<Row[]> => {
  const query = mapping[type]!.listAvailable[1];
  return (await applyQuery(adminClient, type, query, params)).map((role) => ({
    role,
  }));
};

export const getAvailableClientRoles = async (
  adminClient: KeycloakAdminClient,
  type: ResourcesKey,
  id: string,
  client: ClientRepresentation
) => {
  const query = mapping[type]!.listAvailable[0];
  return (
    await applyQuery(adminClient, type === "roles" ? "clients" : type, query, {
      id: type === "roles" ? client.id : id,
      client: client.id,
      clientUniqueId: client.id,
    })
  ).map((role) => ({ role, client: { clientId: client.id, ...client } }));
};
