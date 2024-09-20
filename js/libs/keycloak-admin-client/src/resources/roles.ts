import Resource from "./resource.js";
import type RoleRepresentation from "../defs/roleRepresentation.js";
import type UserRepresentation from "../defs/userRepresentation.js";
import type { KeycloakAdminClient } from "../client.js";
import type { ManagementPermissionReference } from "../defs/managementPermissionReference.js";

export interface RoleQuery {
  first?: number;
  max?: number;
  search?: string;
  briefRepresentation?: boolean;
}

export class Roles extends Resource<{ realm?: string }> {
  /**
   * Realm roles
   */

  public find = this.makeRequest<RoleQuery, RoleRepresentation[]>({
    method: "GET",
    path: "/roles",
  });

  public create = this.makeRequest<RoleRepresentation, { roleName: string }>({
    method: "POST",
    path: "/roles",
    returnResourceIdInLocationHeader: { field: "roleName" },
  });

  /**
   * Roles by name
   */

  public findOneByName = this.makeRequest<
    { name: string },
    RoleRepresentation | undefined
  >({
    method: "GET",
    path: "/roles/{name}",
    urlParamKeys: ["name"],
    catchNotFound: true,
  });

  public updateByName = this.makeUpdateRequest<
    { name: string },
    RoleRepresentation,
    void
  >({
    method: "PUT",
    path: "/roles/{name}",
    urlParamKeys: ["name"],
  });

  public delByName = this.makeRequest<{ name: string }, void>({
    method: "DELETE",
    path: "/roles/{name}",
    urlParamKeys: ["name"],
  });

  public findUsersWithRole = this.makeRequest<
    {
      name: string;
      briefRepresentation?: boolean;
      first?: number;
      max?: number;
    },
    UserRepresentation[]
  >({
    method: "GET",
    path: "/roles/{name}/users",
    urlParamKeys: ["name"],
    catchNotFound: true,
  });

  /**
   * Roles by id
   */

  public findOneById = this.makeRequest<
    { id: string },
    RoleRepresentation | undefined
  >({
    method: "GET",
    path: "/roles-by-id/{id}",
    urlParamKeys: ["id"],
    catchNotFound: true,
  });

  public createComposite = this.makeUpdateRequest<
    { roleId: string },
    RoleRepresentation[],
    void
  >({
    method: "POST",
    path: "/roles-by-id/{roleId}/composites",
    urlParamKeys: ["roleId"],
  });

  public getCompositeRoles = this.makeRequest<
    { id: string; search?: string; first?: number; max?: number },
    RoleRepresentation[]
  >({
    method: "GET",
    path: "/roles-by-id/{id}/composites",
    urlParamKeys: ["id"],
  });

  public getCompositeRolesForRealm = this.makeRequest<
    { id: string },
    RoleRepresentation[]
  >({
    method: "GET",
    path: "/roles-by-id/{id}/composites/realm",
    urlParamKeys: ["id"],
  });

  public getCompositeRolesForClient = this.makeRequest<
    { id: string; clientId: string },
    RoleRepresentation[]
  >({
    method: "GET",
    path: "/roles-by-id/{id}/composites/clients/{clientId}",
    urlParamKeys: ["id", "clientId"],
  });

  public delCompositeRoles = this.makeUpdateRequest<
    { id: string },
    RoleRepresentation[],
    void
  >({
    method: "DELETE",
    path: "/roles-by-id/{id}/composites",
    urlParamKeys: ["id"],
  });

  public updateById = this.makeUpdateRequest<
    { id: string },
    RoleRepresentation,
    void
  >({
    method: "PUT",
    path: "/roles-by-id/{id}",
    urlParamKeys: ["id"],
  });

  public delById = this.makeRequest<{ id: string }, void>({
    method: "DELETE",
    path: "/roles-by-id/{id}",
    urlParamKeys: ["id"],
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
    path: "/roles-by-id/{id}/management/permissions",
    urlParamKeys: ["id"],
  });

  public listPermissions = this.makeRequest<
    { id: string },
    ManagementPermissionReference
  >({
    method: "GET",
    path: "/roles-by-id/{id}/management/permissions",
    urlParamKeys: ["id"],
  });

  constructor(client: KeycloakAdminClient) {
    super(client, {
      path: "/admin/realms/{realm}",
      getUrlParams: () => ({
        realm: client.realmName,
      }),
      getBaseUrl: () => client.baseUrl,
    });
  }
}
