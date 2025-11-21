import type { KeycloakAdminClient } from "../client.js";
import IdentityProviderRepresentation from "../defs/identityProviderRepresentation.js";
import type OrganizationRepresentation from "../defs/organizationRepresentation.js";
import type OrganizationRoleRepresentation from "../defs/organizationRoleRepresentation.js";
import type RoleRepresentation from "../defs/roleRepresentation.js";
import UserRepresentation from "../defs/userRepresentation.js";
import Resource from "./resource.js";

interface PaginatedQuery {
  first?: number; // The position of the first result to be processed (pagination offset)
  max?: number; // The maximum number of results to be returned - defaults to 10
  search?: string;
}
export interface OrganizationQuery extends PaginatedQuery {
  q?: string; // A query to search for custom attributes, in the format 'key1:value2 key2:value2'
  exact?: boolean; // Boolean which defines whether the param 'search' must match exactly or not
}

interface MemberQuery extends PaginatedQuery {
  orgId: string; //Id of the organization to get the members of
  membershipType?: string;
}

interface RoleQuery extends PaginatedQuery {
  orgId: string; //Id of the organization to get the roles of
}

export class Organizations extends Resource<{ realm?: string }> {
  /**
   * Organizations
   */

  constructor(client: KeycloakAdminClient) {
    super(client, {
      path: "/admin/realms/{realm}/organizations",
      getUrlParams: () => ({
        realm: client.realmName,
      }),
      getBaseUrl: () => client.baseUrl,
    });
  }

  public find = this.makeRequest<
    OrganizationQuery,
    OrganizationRepresentation[]
  >({
    method: "GET",
    path: "/",
  });

  public findOne = this.makeRequest<{ id: string }, OrganizationRepresentation>(
    {
      method: "GET",
      path: "/{id}",
      urlParamKeys: ["id"],
    },
  );

  public create = this.makeRequest<OrganizationRepresentation, { id: string }>({
    method: "POST",
    returnResourceIdInLocationHeader: { field: "id" },
  });

  public delById = this.makeRequest<{ id: string }, void>({
    method: "DELETE",
    path: "/{id}",
    urlParamKeys: ["id"],
  });

  public updateById = this.makeUpdateRequest<
    { id: string },
    OrganizationRepresentation,
    void
  >({
    method: "PUT",
    path: "/{id}",
    urlParamKeys: ["id"],
  });

  public listMembers = this.makeRequest<MemberQuery, UserRepresentation[]>({
    method: "GET",
    path: "/{orgId}/members",
    urlParamKeys: ["orgId"],
  });

  public addMember = this.makeRequest<
    { orgId: string; userId: string },
    string
  >({
    method: "POST",
    path: "/{orgId}/members",
    urlParamKeys: ["orgId"],
    payloadKey: "userId",
  });

  public delMember = this.makeRequest<
    { orgId: string; userId: string },
    string
  >({
    method: "DELETE",
    path: "/{orgId}/members/{userId}",
    urlParamKeys: ["orgId", "userId"],
  });

  public memberOrganizations = this.makeRequest<
    { userId: string },
    OrganizationRepresentation[]
  >({
    method: "GET",
    path: "/members/{userId}/organizations",
    urlParamKeys: ["userId"],
  });

  public invite = this.makeUpdateRequest<{ orgId: string }, FormData>({
    method: "POST",
    path: "/{orgId}/members/invite-user",
    urlParamKeys: ["orgId"],
  });

  public inviteExistingUser = this.makeUpdateRequest<
    { orgId: string },
    FormData
  >({
    method: "POST",
    path: "/{orgId}/members/invite-existing-user",
    urlParamKeys: ["orgId"],
  });

  public listIdentityProviders = this.makeRequest<
    { orgId: string },
    IdentityProviderRepresentation[]
  >({
    method: "GET",
    path: "/{orgId}/identity-providers",
    urlParamKeys: ["orgId"],
  });

  public linkIdp = this.makeRequest<{ orgId: string; alias: string }, string>({
    method: "POST",
    path: "/{orgId}/identity-providers",
    urlParamKeys: ["orgId"],
    payloadKey: "alias",
  });

  public unLinkIdp = this.makeRequest<{ orgId: string; alias: string }, string>(
    {
      method: "DELETE",
      path: "/{orgId}/identity-providers/{alias}",
      urlParamKeys: ["orgId", "alias"],
    },
  );

  public listRoles = this.makeRequest<
    RoleQuery,
    OrganizationRoleRepresentation[]
  >({
    method: "GET",
    path: "/{orgId}/roles",
    urlParamKeys: ["orgId"],
  });

  public findRole = this.makeRequest<
    { orgId: string; roleId: string },
    OrganizationRoleRepresentation
  >({
    method: "GET",
    path: "/{orgId}/roles/{roleId}",
    urlParamKeys: ["orgId", "roleId"],
  });

  public createRole = this.makeRequest<
    OrganizationRoleRepresentation & { orgId: string },
    { id: string }
  >({
    method: "POST",
    path: "/{orgId}/roles",
    urlParamKeys: ["orgId"],
    returnResourceIdInLocationHeader: { field: "id" },
  });

  public updateRole = this.makeUpdateRequest<
    { orgId: string; roleId: string },
    OrganizationRoleRepresentation,
    void
  >({
    method: "PUT",
    path: "/{orgId}/roles/{roleId}",
    urlParamKeys: ["orgId", "roleId"],
  });

  public delRole = this.makeRequest<{ orgId: string; roleId: string }, void>({
    method: "DELETE",
    path: "/{orgId}/roles/{roleId}",
    urlParamKeys: ["orgId", "roleId"],
  });

  public grantRole = this.makeRequest<
    { orgId: string; roleId: string; userId: string },
    void
  >({
    method: "POST",
    path: "/{orgId}/roles/{roleId}/members/{userId}",
    urlParamKeys: ["orgId", "roleId", "userId"],
  });

  public revokeRole = this.makeRequest<
    { orgId: string; roleId: string; userId: string },
    void
  >({
    method: "DELETE",
    path: "/{orgId}/roles/{roleId}/members/{userId}",
    urlParamKeys: ["orgId", "roleId", "userId"],
  });

  public listRoleMembers = this.makeRequest<
    { orgId: string; roleId: string },
    UserRepresentation[]
  >({
    method: "GET",
    path: "/{orgId}/roles/{roleId}/members",
    urlParamKeys: ["orgId", "roleId"],
  });

  public addRoleComposites = this.makeUpdateRequest<
    { orgId: string; roleId: string },
    RoleRepresentation[],
    void
  >({
    method: "POST",
    path: "/{orgId}/roles/{roleId}/composites",
    urlParamKeys: ["orgId", "roleId"],
  });

  public delRoleComposites = this.makeUpdateRequest<
    { orgId: string; roleId: string },
    RoleRepresentation[],
    void
  >({
    method: "DELETE",
    path: "/{orgId}/roles/{roleId}/composites",
    urlParamKeys: ["orgId", "roleId"],
  });

  public listRoleComposites = this.makeRequest<
    { orgId: string; roleId: string },
    RoleRepresentation[]
  >({
    method: "GET",
    path: "/{orgId}/roles/{roleId}/composites",
    urlParamKeys: ["orgId", "roleId"],
  });

  public getRoleComposites = this.makeRequest<
    { orgId: string; roleId: string },
    RoleRepresentation[]
  >({
    method: "GET",
    path: "/{orgId}/roles/{roleId}/composites",
    urlParamKeys: ["orgId", "roleId"],
  });

  public getRoleRealmComposites = this.makeRequest<
    { orgId: string; roleId: string },
    RoleRepresentation[]
  >({
    method: "GET",
    path: "/{orgId}/roles/{roleId}/composites/realm",
    urlParamKeys: ["orgId", "roleId"],
  });

  public getRoleClientComposites = this.makeRequest<
    { orgId: string; roleId: string; clientId: string },
    RoleRepresentation[]
  >({
    method: "GET",
    path: "/{orgId}/roles/{roleId}/composites/clients/{clientId}",
    urlParamKeys: ["orgId", "roleId", "clientId"],
  });

  public getUserRoleMappings = this.makeRequest<
    { orgId: string; userId: string },
    OrganizationRoleRepresentation[]
  >({
    method: "GET",
    path: "/{orgId}/members/{userId}/role-mappings/organization",
    urlParamKeys: ["orgId", "userId"],
  });

  public getUserRoleMappingsComposite = this.makeRequest<
    { orgId: string; userId: string },
    OrganizationRoleRepresentation[]
  >({
    method: "GET",
    path: "/{orgId}/members/{userId}/role-mappings/organization/composite",
    urlParamKeys: ["orgId", "userId"],
  });

  public getUserRoleMappingsAvailable = this.makeRequest<
    { orgId: string; userId: string },
    OrganizationRoleRepresentation[]
  >({
    method: "GET",
    path: "/{orgId}/members/{userId}/role-mappings/organization/available",
    urlParamKeys: ["orgId", "userId"],
  });

  public addUserRoleMappings = this.makeUpdateRequest<
    { orgId: string; userId: string },
    OrganizationRoleRepresentation[],
    void
  >({
    method: "POST",
    path: "/{orgId}/members/{userId}/role-mappings/organization",
    urlParamKeys: ["orgId", "userId"],
  });

  public delUserRoleMappings = this.makeUpdateRequest<
    { orgId: string; userId: string },
    OrganizationRoleRepresentation[],
    void
  >({
    method: "DELETE",
    path: "/{orgId}/members/{userId}/role-mappings/organization",
    urlParamKeys: ["orgId", "userId"],
  });
}
