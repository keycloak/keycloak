import type KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import type ClientMappingsRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientMappingsRepresentation";
import type MappingsRepresentation from "@keycloak/keycloak-admin-client/lib/defs/mappingsRepresentation";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import type { RoleMappingPayload } from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import type { Groups } from "@keycloak/keycloak-admin-client/lib/resources/groups";
import { Row } from "./RoleMapping";

/**
 * The kind of object roles are mapped to:
 * - `users` / `groups`: role mappings
 * - `clients` / `clientScopes`: scope mappings
 * - `roles`: composite roles
 * - `realms`: no mapping, just pick roles that are visible in the realm
 */
export type RoleMappingType =
  | "users"
  | "groups"
  | "clients"
  | "clientScopes"
  | "roles"
  | "realms";

export type ResourcesKey = Exclude<RoleMappingType, "realms">;

export type AvailableQuery = {
  id: string;
  first?: number;
  max?: number;
  search?: string;
};

const roleMapperResource = (
  adminClient: KeycloakAdminClient,
  type: "users" | "groups",
  groupsResource?: Groups,
) => (type === "groups" && groupsResource ? groupsResource : adminClient[type]);

const toClientRows = (clients: ClientMappingsRepresentation[]): Row[] =>
  clients.flatMap((client) =>
    (client.mappings || []).map((role) => ({
      client: { id: client.id, clientId: client.client },
      role,
      id: role.id,
    })),
  );

const toRealmRows = (roles: RoleRepresentation[] = []): Row[] =>
  roles.map((role) => ({ role, id: role.id }));

const toAvailableClientRows = (mappings: MappingsRepresentation): Row[] =>
  toClientRows(Object.values(mappings.clientMappings || {}));

const toRows = (mappings: MappingsRepresentation): Row[] => [
  ...toClientRows(Object.values(mappings.clientMappings || {})),
  ...toRealmRows(mappings.realmMappings),
];

const toRoleRows = async (
  adminClient: KeycloakAdminClient,
  roles: RoleRepresentation[],
): Promise<Row[]> => {
  const containerIds = new Set(
    roles.filter((r) => r.clientRole).map((r) => r.containerId!),
  );
  const clients = new Map(
    await Promise.all(
      [...containerIds].map(
        async (id) =>
          [
            id,
            await adminClient.clients.findOne({ id }).catch(() => undefined),
          ] as const,
      ),
    ),
  );
  return roles.map((role) => ({
    role,
    id: role.id,
    ...(role.clientRole
      ? {
          client: {
            id: role.containerId,
            clientId: clients.get(role.containerId!)?.clientId,
          },
        }
      : {}),
  }));
};

export const getMapping = async (
  adminClient: KeycloakAdminClient,
  type: ResourcesKey,
  id: string,
  groupsResource?: Groups,
): Promise<Row[]> => {
  switch (type) {
    case "users":
    case "groups":
      return toRows(
        await roleMapperResource(
          adminClient,
          type,
          groupsResource,
        ).listRoleMappings({ id }),
      );
    case "clients":
    case "clientScopes":
      return toRows(await adminClient[type].listScopeMappings({ id }));
    case "roles":
      return toRoleRows(
        adminClient,
        await adminClient.roles.getCompositeRoles({ id }),
      );
  }
};

export const getInheritedMapping = async (
  adminClient: KeycloakAdminClient,
  type: ResourcesKey,
  id: string,
  groupsResource?: Groups,
): Promise<Row[]> => {
  switch (type) {
    case "users":
    case "groups":
      return toRows(
        await roleMapperResource(
          adminClient,
          type,
          groupsResource,
        ).listRoleMappingsInherited({ id }),
      );
    case "clients":
    case "clientScopes":
      return toRows(await adminClient[type].listScopeMappingsInherited({ id }));
    case "roles":
      return toRows(await adminClient.roles.getCompositeRolesInherited({ id }));
  }
};

export const getAvailableRealmRoles = async (
  adminClient: KeycloakAdminClient,
  type: RoleMappingType,
  { id, ...params }: AvailableQuery,
  groupsResource?: Groups,
): Promise<Row[]> => {
  switch (type) {
    case "users":
    case "groups":
      return toRealmRows(
        await roleMapperResource(
          adminClient,
          type,
          groupsResource,
        ).listAvailableRealmRoleMappings({ id }),
      );
    case "clients":
    case "clientScopes":
      return toRealmRows(
        await adminClient[type].listAvailableRealmScopeMappings({ id }),
      );
    case "roles":
      return toRealmRows(
        (await adminClient.roles.getCompositeRolesAvailable({ id, ...params }))
          .realmMappings,
      );
    case "realms":
      return toRealmRows(await adminClient.roles.find(params));
  }
};

export const getAvailableClientRoles = async (
  adminClient: KeycloakAdminClient,
  type: RoleMappingType,
  { id, ...params }: AvailableQuery,
  groupsResource?: Groups,
): Promise<Row[]> => {
  switch (type) {
    case "users":
    case "groups":
      return toAvailableClientRows(
        await roleMapperResource(
          adminClient,
          type,
          groupsResource,
        ).listRoleMappingsAvailable({ id, ...params }),
      );
    case "clients":
    case "clientScopes":
      return toAvailableClientRows(
        await adminClient[type].listScopeMappingsAvailable({ id, ...params }),
      );
    case "roles":
      return toAvailableClientRows(
        await adminClient.roles.getCompositeRolesAvailable({ id, ...params }),
      );
    case "realms":
      return toClientRows(await adminClient.clients.findRoles(params));
  }
};

const groupByClient = (rows: Row[]) => {
  const realmRoles: RoleMappingPayload[] = [];
  const clientRoles = new Map<string, RoleMappingPayload[]>();
  rows.forEach(({ client, role }) => {
    // rows carry UI-only fields (such as `isInherited`) that the server rejects
    const payload: RoleMappingPayload = { id: role.id!, name: role.name! };
    if (client?.id) {
      clientRoles.set(client.id, [
        ...(clientRoles.get(client.id) || []),
        payload,
      ]);
    } else {
      realmRoles.push(payload);
    }
  });
  return { realmRoles, clientRoles };
};

export const deleteMapping = (
  adminClient: KeycloakAdminClient,
  type: ResourcesKey,
  id: string,
  rows: Row[],
  groupsResource?: Groups,
): Promise<unknown>[] => {
  const { realmRoles, clientRoles } = groupByClient(rows);
  switch (type) {
    case "users":
    case "groups": {
      const resource = roleMapperResource(adminClient, type, groupsResource);
      return [
        ...(realmRoles.length
          ? [resource.delRealmRoleMappings({ id, roles: realmRoles })]
          : []),
        ...[...clientRoles].map(([clientUniqueId, roles]) =>
          resource.delClientRoleMappings({ id, clientUniqueId, roles }),
        ),
      ];
    }
    case "clients":
    case "clientScopes": {
      const resource = adminClient[type];
      return [
        ...(realmRoles.length
          ? [resource.delRealmScopeMappings({ id }, realmRoles)]
          : []),
        ...[...clientRoles].map(([client, roles]) =>
          resource.delClientScopeMappings({ id, client }, roles),
        ),
      ];
    }
    case "roles":
      return [
        adminClient.roles.delCompositeRoles({ id }, [
          ...realmRoles,
          ...[...clientRoles.values()].flat(),
        ]),
      ];
  }
};
