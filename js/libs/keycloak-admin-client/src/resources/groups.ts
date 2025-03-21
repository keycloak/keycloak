import type { KeycloakAdminClient } from "../client.js";
import type GroupRepresentation from "../defs/groupRepresentation.js";
import type { ManagementPermissionReference } from "../defs/managementPermissionReference.js";
import type MappingsRepresentation from "../defs/mappingsRepresentation.js";
import type RoleRepresentation from "../defs/roleRepresentation.js";
import type { RoleMappingPayload } from "../defs/roleRepresentation.js";
import type UserRepresentation from "../defs/userRepresentation.js";
import Resource from "./resource.js";

interface Query {
  q?: string;
  search?: string;
  exact?: boolean;
}

interface PaginatedQuery {
  first?: number;
  max?: number;
}

interface SummarizedQuery {
  briefRepresentation?: boolean;
  populateHierarchy?: boolean;
}

export type GroupQuery = Query & PaginatedQuery & SummarizedQuery;
export type SubGroupQuery = Query &
  PaginatedQuery &
  SummarizedQuery & {
    parentId: string;
  };

export interface GroupCountQuery {
  search?: string;
  top?: boolean;
}

export class Groups extends Resource<{ realm?: string }> {
  public find = this.makeRequest<GroupQuery, GroupRepresentation[]>({
    method: "GET",
    queryParamKeys: [
      "search",
      "q",
      "exact",
      "briefRepresentation",
      "populateHierarchy",
      "first",
      "max",
    ],
  });

  public create = this.makeRequest<GroupRepresentation, { id: string }>({
    method: "POST",
    returnResourceIdInLocationHeader: { field: "id" },
  });

  public updateRoot = this.makeRequest<GroupRepresentation, void>({
    method: "POST",
  });

  /**
   * Single user
   */

  public findOne = this.makeRequest<
    { id: string },
    GroupRepresentation | undefined
  >({
    method: "GET",
    path: "/{id}",
    urlParamKeys: ["id"],
    catchNotFound: true,
  });

  public update = this.makeUpdateRequest<
    { id: string },
    GroupRepresentation,
    void
  >({
    method: "PUT",
    path: "/{id}",
    urlParamKeys: ["id"],
  });

  public del = this.makeRequest<{ id: string }, void>({
    method: "DELETE",
    path: "/{id}",
    urlParamKeys: ["id"],
  });

  public count = this.makeRequest<GroupCountQuery, { count: number }>({
    method: "GET",
    path: "/count",
  });

  /**
   * Creates a child group on the specified parent group. If the group already exists, then an error is returned.
   */
  public createChildGroup = this.makeUpdateRequest<
    { id: string },
    Omit<GroupRepresentation, "id">,
    { id: string }
  >({
    method: "POST",
    path: "/{id}/children",
    urlParamKeys: ["id"],
    returnResourceIdInLocationHeader: { field: "id" },
  });

  /**
   * Updates a child group on the specified parent group. If the group doesn’t exist, then an error is returned.
   * Can be used to move a group from one parent to another.
   */
  public updateChildGroup = this.makeUpdateRequest<
    { id: string },
    GroupRepresentation,
    void
  >({
    method: "POST",
    path: "/{id}/children",
    urlParamKeys: ["id"],
  });

  /**
   * Finds all subgroups on the specified parent group matching the provided parameters.
   */
  public listSubGroups = this.makeRequest<SubGroupQuery, GroupRepresentation[]>(
    {
      method: "GET",
      path: "/{parentId}/children",
      urlParamKeys: ["parentId"],
      queryParamKeys: ["search", "first", "max", "briefRepresentation"],
      catchNotFound: true,
    },
  );

  /**
   * Members
   */

  public listMembers = this.makeRequest<
    { id: string; first?: number; max?: number; briefRepresentation?: boolean },
    UserRepresentation[]
  >({
    method: "GET",
    path: "/{id}/members",
    urlParamKeys: ["id"],
    catchNotFound: true,
  });

  /**
   * Role mappings
   * https://www.keycloak.org/docs-api/11.0/rest-api/#_role_mapper_resource
   */

  public listRoleMappings = this.makeRequest<
    { id: string },
    MappingsRepresentation
  >({
    method: "GET",
    path: "/{id}/role-mappings",
    urlParamKeys: ["id"],
  });

  public addRealmRoleMappings = this.makeRequest<
    { id: string; roles: RoleMappingPayload[] },
    void
  >({
    method: "POST",
    path: "/{id}/role-mappings/realm",
    urlParamKeys: ["id"],
    payloadKey: "roles",
  });

  public listRealmRoleMappings = this.makeRequest<
    { id: string },
    RoleRepresentation[]
  >({
    method: "GET",
    path: "/{id}/role-mappings/realm",
    urlParamKeys: ["id"],
  });

  public delRealmRoleMappings = this.makeRequest<
    { id: string; roles: RoleMappingPayload[] },
    void
  >({
    method: "DELETE",
    path: "/{id}/role-mappings/realm",
    urlParamKeys: ["id"],
    payloadKey: "roles",
  });

  public listAvailableRealmRoleMappings = this.makeRequest<
    { id: string },
    RoleRepresentation[]
  >({
    method: "GET",
    path: "/{id}/role-mappings/realm/available",
    urlParamKeys: ["id"],
  });

  // Get effective realm-level role mappings This will recurse all composite roles to get the result.
  public listCompositeRealmRoleMappings = this.makeRequest<
    { id: string },
    RoleRepresentation[]
  >({
    method: "GET",
    path: "/{id}/role-mappings/realm/composite",
    urlParamKeys: ["id"],
  });

  /**
   * Client role mappings
   * https://www.keycloak.org/docs-api/11.0/rest-api/#_client_role_mappings_resource
   */

  public listClientRoleMappings = this.makeRequest<
    { id: string; clientUniqueId: string },
    RoleRepresentation[]
  >({
    method: "GET",
    path: "/{id}/role-mappings/clients/{clientUniqueId}",
    urlParamKeys: ["id", "clientUniqueId"],
  });

  public addClientRoleMappings = this.makeRequest<
    { id: string; clientUniqueId: string; roles: RoleMappingPayload[] },
    void
  >({
    method: "POST",
    path: "/{id}/role-mappings/clients/{clientUniqueId}",
    urlParamKeys: ["id", "clientUniqueId"],
    payloadKey: "roles",
  });

  public delClientRoleMappings = this.makeRequest<
    { id: string; clientUniqueId: string; roles: RoleMappingPayload[] },
    void
  >({
    method: "DELETE",
    path: "/{id}/role-mappings/clients/{clientUniqueId}",
    urlParamKeys: ["id", "clientUniqueId"],
    payloadKey: "roles",
  });

  public listAvailableClientRoleMappings = this.makeRequest<
    { id: string; clientUniqueId: string },
    RoleRepresentation[]
  >({
    method: "GET",
    path: "/{id}/role-mappings/clients/{clientUniqueId}/available",
    urlParamKeys: ["id", "clientUniqueId"],
  });

  public listCompositeClientRoleMappings = this.makeRequest<
    { id: string; clientUniqueId: string },
    RoleRepresentation[]
  >({
    method: "GET",
    path: "/{id}/role-mappings/clients/{clientUniqueId}/composite",
    urlParamKeys: ["id", "clientUniqueId"],
  });

  /**
   * Authorization permissions
   */
  public updatePermission = this.makeUpdateRequest<
    { id: string },
    ManagementPermissionReference,
    ManagementPermissionReference
  >({
    method: "PUT",
    path: "/{id}/management/permissions",
    urlParamKeys: ["id"],
  });

  public listPermissions = this.makeRequest<
    { id: string },
    ManagementPermissionReference
  >({
    method: "GET",
    path: "/{id}/management/permissions",
    urlParamKeys: ["id"],
  });

  constructor(client: KeycloakAdminClient) {
    super(client, {
      path: "/admin/realms/{realm}/groups",
      getUrlParams: () => ({
        realm: client.realmName,
      }),
      getBaseUrl: () => client.baseUrl,
    });
  }
}
