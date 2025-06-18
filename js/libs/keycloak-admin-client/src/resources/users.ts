import type { KeycloakAdminClient } from "../client.js";
import type CredentialRepresentation from "../defs/credentialRepresentation.js";
import type FederatedIdentityRepresentation from "../defs/federatedIdentityRepresentation.js";
import type GroupRepresentation from "../defs/groupRepresentation.js";
import type MappingsRepresentation from "../defs/mappingsRepresentation.js";
import type { RequiredActionAlias } from "../defs/requiredActionProviderRepresentation.js";
import type RoleRepresentation from "../defs/roleRepresentation.js";
import type { RoleMappingPayload } from "../defs/roleRepresentation.js";
import type UserConsentRepresentation from "../defs/userConsentRepresentation.js";
import type {
  UserProfileConfig,
  UserProfileMetadata,
} from "../defs/userProfileMetadata.js";
import type UserRepresentation from "../defs/userRepresentation.js";
import type UserSessionRepresentation from "../defs/userSessionRepresentation.js";
import Resource from "./resource.js";

interface SearchQuery {
  search?: string;
}

interface PaginationQuery {
  first?: number;
  max?: number;
}

interface UserBaseQuery {
  email?: string;
  firstName?: string;
  lastName?: string;
  username?: string;
  q?: string;
}

export interface UserQuery extends PaginationQuery, SearchQuery, UserBaseQuery {
  exact?: boolean;
  [key: string]: string | number | undefined | boolean;
}

export class Users extends Resource<{ realm?: string }> {
  public find = this.makeRequest<UserQuery, UserRepresentation[]>({
    method: "GET",
  });

  public create = this.makeRequest<UserRepresentation, { id: string }>({
    method: "POST",
    returnResourceIdInLocationHeader: { field: "id" },
  });

  /**
   * Single user
   */

  public findOne = this.makeRequest<
    { id: string; userProfileMetadata?: boolean },
    UserRepresentation | undefined
  >({
    method: "GET",
    path: "/{id}",
    urlParamKeys: ["id"],
    catchNotFound: true,
  });

  public update = this.makeUpdateRequest<
    { id: string },
    UserRepresentation,
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

  public count = this.makeRequest<UserBaseQuery & SearchQuery, number>({
    method: "GET",
    path: "/count",
  });

  public getProfile = this.makeRequest<{}, UserProfileConfig>({
    method: "GET",
    path: "/profile",
  });

  public updateProfile = this.makeRequest<UserProfileConfig, UserProfileConfig>(
    {
      method: "PUT",
      path: "/profile",
    },
  );

  public getProfileMetadata = this.makeRequest<{}, UserProfileMetadata>({
    method: "GET",
    path: "/profile/metadata",
  });

  /**
   * role mappings
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
   * Send a update account email to the user
   * an email contains a link the user can click to perform a set of required actions.
   */

  public executeActionsEmail = this.makeRequest<
    {
      id: string;
      clientId?: string;
      lifespan?: number;
      redirectUri?: string;
      actions?: (RequiredActionAlias | string)[];
    },
    void
  >({
    method: "PUT",
    path: "/{id}/execute-actions-email",
    urlParamKeys: ["id"],
    payloadKey: "actions",
    queryParamKeys: ["lifespan", "redirectUri", "clientId"],
    keyTransform: {
      clientId: "client_id",
      redirectUri: "redirect_uri",
    },
  });

  /**
   * Group
   */

  public listGroups = this.makeRequest<
    { id: string; briefRepresentation?: boolean } & PaginationQuery &
      SearchQuery,
    GroupRepresentation[]
  >({
    method: "GET",
    path: "/{id}/groups",
    urlParamKeys: ["id"],
  });

  public addToGroup = this.makeRequest<{ id: string; groupId: string }, string>(
    {
      method: "PUT",
      path: "/{id}/groups/{groupId}",
      urlParamKeys: ["id", "groupId"],
    },
  );

  public delFromGroup = this.makeRequest<
    { id: string; groupId: string },
    string
  >({
    method: "DELETE",
    path: "/{id}/groups/{groupId}",
    urlParamKeys: ["id", "groupId"],
  });

  public countGroups = this.makeRequest<
    { id: string; search?: string },
    { count: number }
  >({
    method: "GET",
    path: "/{id}/groups/count",
    urlParamKeys: ["id"],
  });

  /**
   * Federated Identity
   */

  public listFederatedIdentities = this.makeRequest<
    { id: string },
    FederatedIdentityRepresentation[]
  >({
    method: "GET",
    path: "/{id}/federated-identity",
    urlParamKeys: ["id"],
  });

  public addToFederatedIdentity = this.makeRequest<
    {
      id: string;
      federatedIdentityId: string;
      federatedIdentity: FederatedIdentityRepresentation;
    },
    void
  >({
    method: "POST",
    path: "/{id}/federated-identity/{federatedIdentityId}",
    urlParamKeys: ["id", "federatedIdentityId"],
    payloadKey: "federatedIdentity",
  });

  public delFromFederatedIdentity = this.makeRequest<
    { id: string; federatedIdentityId: string },
    void
  >({
    method: "DELETE",
    path: "/{id}/federated-identity/{federatedIdentityId}",
    urlParamKeys: ["id", "federatedIdentityId"],
  });

  /**
   * remove totp
   */
  public removeTotp = this.makeRequest<{ id: string }, void>({
    method: "PUT",
    path: "/{id}/remove-totp",
    urlParamKeys: ["id"],
  });

  /**
   * reset password
   */
  public resetPassword = this.makeRequest<
    { id: string; credential: CredentialRepresentation },
    void
  >({
    method: "PUT",
    path: "/{id}/reset-password",
    urlParamKeys: ["id"],
    payloadKey: "credential",
  });

  public getUserStorageCredentialTypes = this.makeRequest<
    { id: string },
    string[]
  >({
    method: "GET",
    path: "/{id}/configured-user-storage-credential-types",
    urlParamKeys: ["id"],
  });

  /**
   * get user credentials
   */
  public getCredentials = this.makeRequest<
    { id: string },
    CredentialRepresentation[]
  >({
    method: "GET",
    path: "/{id}/credentials",
    urlParamKeys: ["id"],
  });

  /**
   * delete user credentials
   */
  public deleteCredential = this.makeRequest<
    { id: string; credentialId: string },
    void
  >({
    method: "DELETE",
    path: "/{id}/credentials/{credentialId}",
    urlParamKeys: ["id", "credentialId"],
  });

  /**
   * update a credential label for a user
   */
  public updateCredentialLabel = this.makeUpdateRequest<
    { id: string; credentialId: string },
    string,
    void
  >({
    method: "PUT",
    path: "/{id}/credentials/{credentialId}/userLabel",
    urlParamKeys: ["id", "credentialId"],
    headers: { "content-type": "text/plain" },
  });

  // Move a credential to a position behind another credential
  public moveCredentialPositionDown = this.makeRequest<
    {
      id: string;
      credentialId: string;
      newPreviousCredentialId: string;
    },
    void
  >({
    method: "POST",
    path: "/{id}/credentials/{credentialId}/moveAfter/{newPreviousCredentialId}",
    urlParamKeys: ["id", "credentialId", "newPreviousCredentialId"],
  });

  // Move a credential to a first position in the credentials list of the user
  public moveCredentialPositionUp = this.makeRequest<
    {
      id: string;
      credentialId: string;
    },
    void
  >({
    method: "POST",
    path: "/{id}/credentials/{credentialId}/moveToFirst",
    urlParamKeys: ["id", "credentialId"],
  });

  /**
   * send verify email
   */
  public sendVerifyEmail = this.makeRequest<
    { id: string; clientId?: string; redirectUri?: string },
    void
  >({
    method: "PUT",
    path: "/{id}/send-verify-email",
    urlParamKeys: ["id"],
    queryParamKeys: ["clientId", "redirectUri"],
    keyTransform: {
      clientId: "client_id",
      redirectUri: "redirect_uri",
    },
  });

  /**
   * list user sessions
   */
  public listSessions = this.makeRequest<
    { id: string },
    UserSessionRepresentation[]
  >({
    method: "GET",
    path: "/{id}/sessions",
    urlParamKeys: ["id"],
  });

  /**
   * list offline sessions associated with the user and client
   */
  public listOfflineSessions = this.makeRequest<
    { id: string; clientId: string },
    UserSessionRepresentation[]
  >({
    method: "GET",
    path: "/{id}/offline-sessions/{clientId}",
    urlParamKeys: ["id", "clientId"],
  });

  /**
   * logout user from all sessions
   */
  public logout = this.makeRequest<{ id: string }, void>({
    method: "POST",
    path: "/{id}/logout",
    urlParamKeys: ["id"],
  });

  /**
   * list consents granted by the user
   */
  public listConsents = this.makeRequest<
    { id: string },
    UserConsentRepresentation[]
  >({
    method: "GET",
    path: "/{id}/consents",
    urlParamKeys: ["id"],
  });

  public impersonation = this.makeUpdateRequest<
    { id: string },
    { user: string; realm: string },
    Record<string, any>
  >({
    method: "POST",
    path: "/{id}/impersonation",
    urlParamKeys: ["id"],
  });

  /**
   * revoke consent and offline tokens for particular client from user
   */
  public revokeConsent = this.makeRequest<
    { id: string; clientId: string },
    void
  >({
    method: "DELETE",
    path: "/{id}/consents/{clientId}",
    urlParamKeys: ["id", "clientId"],
  });

  public getUnmanagedAttributes = this.makeRequest<
    { id: string },
    Record<string, string[]>
  >({
    method: "GET",
    path: "/{id}/unmanagedAttributes",
    urlParamKeys: ["id"],
  });

  constructor(client: KeycloakAdminClient) {
    super(client, {
      path: "/admin/realms/{realm}/users",
      getUrlParams: () => ({
        realm: client.realmName,
      }),
      getBaseUrl: () => client.baseUrl,
    });
  }
}
