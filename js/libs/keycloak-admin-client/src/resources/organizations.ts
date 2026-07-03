import type { KeycloakAdminClient } from "../client.js";
import IdentityProviderRepresentation from "../defs/identityProviderRepresentation.js";
import type OrganizationRepresentation from "../defs/organizationRepresentation.js";
import type OrganizationInvitationRepresentation from "../defs/organizationInvitationRepresentation.js";
import UserRepresentation from "../defs/userRepresentation.js";
import Resource from "./resource.js";
import { Groups } from "./groups.js";
import OrganizationMemberRepresentation from "../defs/organizationMemberRepresentation.js";
import RoleRepresentation from "../defs/roleRepresentation.js";

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

interface InvitationQuery extends PaginatedQuery {
  orgId: string; //Id of the organization to get the invitations of
  status?: string; //Filter by invitation status
  email?: string; //Filter by email
  search?: string; //Search across email, firstName, and lastName
  firstName?: string; //Filter by first name
  lastName?: string; //Filter by last name
}

export interface OrganizationRoleQuery extends PaginatedQuery {
  orgId: string;
  briefRepresentation?: boolean;
}

interface OrganizationRoleParams {
  orgId: string;
  roleId: string;
}

export class Organizations extends Resource<{ realm?: string }> {
  /**
   * Organizations
   */
  #client: KeycloakAdminClient;

  constructor(client: KeycloakAdminClient) {
    super(client, {
      path: "/admin/realms/{realm}/organizations",
      getUrlParams: () => ({
        realm: client.realmName,
      }),
      getBaseUrl: () => client.baseUrl,
    });
    this.#client = client;
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

  public getMember = this.makeRequest<
    { orgId: string; userId: string },
    OrganizationMemberRepresentation
  >({
    method: "GET",
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
    OrganizationRoleQuery,
    RoleRepresentation[]
  >({
    method: "GET",
    path: "/{orgId}/roles",
    urlParamKeys: ["orgId"],
  });

  public countRoles = this.makeRequest<
    Pick<OrganizationRoleQuery, "orgId" | "search">,
    number
  >({
    method: "GET",
    path: "/{orgId}/roles/count",
    urlParamKeys: ["orgId"],
  });

  public createRole = this.makeRequest<
    RoleRepresentation & { orgId: string },
    { id: string }
  >({
    method: "POST",
    path: "/{orgId}/roles",
    urlParamKeys: ["orgId"],
    returnResourceIdInLocationHeader: { field: "id" },
  });

  public findRole = this.makeRequest<
    OrganizationRoleParams,
    RoleRepresentation | null
  >({
    method: "GET",
    path: "/{orgId}/roles/{roleId}",
    urlParamKeys: ["orgId", "roleId"],
    catchNotFound: true,
  });

  public findDefaultRole = this.makeRequest<
    { orgId: string },
    RoleRepresentation | null
  >({
    method: "GET",
    path: "/{orgId}/roles/default",
    urlParamKeys: ["orgId"],
    catchNotFound: true,
  });

  public updateRole = this.makeUpdateRequest<
    OrganizationRoleParams,
    RoleRepresentation,
    void
  >({
    method: "PUT",
    path: "/{orgId}/roles/{roleId}",
    urlParamKeys: ["orgId", "roleId"],
  });

  public delRole = this.makeRequest<OrganizationRoleParams, void>({
    method: "DELETE",
    path: "/{orgId}/roles/{roleId}",
    urlParamKeys: ["orgId", "roleId"],
  });

  public listRoleComposites = this.makeRequest<
    OrganizationRoleParams & PaginatedQuery,
    RoleRepresentation[]
  >({
    method: "GET",
    path: "/{orgId}/roles/{roleId}/composites",
    urlParamKeys: ["orgId", "roleId"],
  });

  public listRealmRoleComposites = this.makeRequest<
    OrganizationRoleParams,
    RoleRepresentation[]
  >({
    method: "GET",
    path: "/{orgId}/roles/{roleId}/composites/realm",
    urlParamKeys: ["orgId", "roleId"],
  });

  public listClientRoleComposites = this.makeRequest<
    OrganizationRoleParams & { clientId: string },
    RoleRepresentation[]
  >({
    method: "GET",
    path: "/{orgId}/roles/{roleId}/composites/clients/{clientId}",
    urlParamKeys: ["orgId", "roleId", "clientId"],
  });

  public addRoleComposites = this.makeUpdateRequest<
    OrganizationRoleParams,
    RoleRepresentation[],
    void
  >({
    method: "POST",
    path: "/{orgId}/roles/{roleId}/composites",
    urlParamKeys: ["orgId", "roleId"],
  });

  public delRoleComposites = this.makeUpdateRequest<
    OrganizationRoleParams,
    RoleRepresentation[],
    void
  >({
    method: "DELETE",
    path: "/{orgId}/roles/{roleId}/composites",
    urlParamKeys: ["orgId", "roleId"],
  });

  public listRoleUsers = this.makeRequest<
    OrganizationRoleParams & {
      briefRepresentation?: boolean;
      first?: number;
      max?: number;
    },
    UserRepresentation[]
  >({
    method: "GET",
    path: "/{orgId}/roles/{roleId}/users",
    urlParamKeys: ["orgId", "roleId"],
  });

  public listAvailableRoleUsers = this.makeRequest<
    OrganizationRoleParams & {
      briefRepresentation?: boolean;
      first?: number;
      max?: number;
      search?: string;
      exact?: boolean;
    },
    UserRepresentation[]
  >({
    method: "GET",
    path: "/{orgId}/roles/{roleId}/users/available",
    urlParamKeys: ["orgId", "roleId"],
  });

  public addRoleUsers = this.makeUpdateRequest<
    OrganizationRoleParams,
    UserRepresentation[],
    void
  >({
    method: "POST",
    path: "/{orgId}/roles/{roleId}/users",
    urlParamKeys: ["orgId", "roleId"],
  });

  public delRoleUsers = this.makeUpdateRequest<
    OrganizationRoleParams,
    UserRepresentation[],
    void
  >({
    method: "DELETE",
    path: "/{orgId}/roles/{roleId}/users",
    urlParamKeys: ["orgId", "roleId"],
  });

  // Organization Invitations Management
  public listInvitations = this.makeRequest<
    InvitationQuery,
    OrganizationInvitationRepresentation[]
  >({
    method: "GET",
    path: "/{orgId}/invitations",
    urlParamKeys: ["orgId"],
  });

  public findInvitation = this.makeRequest<
    { orgId: string; invitationId: string },
    OrganizationInvitationRepresentation
  >({
    method: "GET",
    path: "/{orgId}/invitations/{invitationId}",
    urlParamKeys: ["orgId", "invitationId"],
  });

  public resendInvitation = this.makeRequest<
    { orgId: string; invitationId: string },
    void
  >({
    method: "POST",
    path: "/{orgId}/invitations/{invitationId}/resend",
    urlParamKeys: ["orgId", "invitationId"],
  });

  public deleteInvitation = this.makeRequest<
    { orgId: string; invitationId: string },
    void
  >({
    method: "DELETE",
    path: "/{orgId}/invitations/{invitationId}",
    urlParamKeys: ["orgId", "invitationId"],
  });

  public groups = (orgId: string) => new Groups(this.#client, orgId);
}
