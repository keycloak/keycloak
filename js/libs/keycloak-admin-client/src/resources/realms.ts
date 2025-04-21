import Resource from "./resource.js";
import type AdminEventRepresentation from "../defs/adminEventRepresentation.js";
import type RealmRepresentation from "../defs/realmRepresentation.js";
import type {
  PartialImportRealmRepresentation,
  PartialImportResponse,
} from "../defs/realmRepresentation.js";
import type EventRepresentation from "../defs/eventRepresentation.js";
import type EventType from "../defs/eventTypes.js";
import type KeysMetadataRepresentation from "../defs/keyMetadataRepresentation.js";
import type ClientInitialAccessPresentation from "../defs/clientInitialAccessPresentation.js";
import type TestLdapConnectionRepresentation from "../defs/testLdapConnection.js";

import type { KeycloakAdminClient } from "../client.js";
import type { RealmEventsConfigRepresentation } from "../defs/realmEventsConfigRepresentation.js";
import type GlobalRequestResult from "../defs/globalRequestResult.js";
import type GroupRepresentation from "../defs/groupRepresentation.js";
import type { ManagementPermissionReference } from "../defs/managementPermissionReference.js";
import type ComponentTypeRepresentation from "../defs/componentTypeRepresentation.js";
import type { ClientSessionStat } from "../defs/clientSessionStat.js";

export class Realms extends Resource {
  /**
   * Realm
   * https://www.keycloak.org/docs-api/11.0/rest-api/#_realms_admin_resource
   */

  public find = this.makeRequest<
    { briefRepresentation?: boolean },
    RealmRepresentation[]
  >({
    method: "GET",
  });

  public create = this.makeRequest<RealmRepresentation, { realmName: string }>({
    method: "POST",
    returnResourceIdInLocationHeader: { field: "realmName" },
  });

  public findOne = this.makeRequest<
    { realm: string },
    RealmRepresentation | undefined
  >({
    method: "GET",
    path: "/{realm}",
    urlParamKeys: ["realm"],
    catchNotFound: true,
  });

  public update = this.makeUpdateRequest<
    { realm: string },
    RealmRepresentation,
    void
  >({
    method: "PUT",
    path: "/{realm}",
    urlParamKeys: ["realm"],
  });

  public del = this.makeRequest<{ realm: string }, void>({
    method: "DELETE",
    path: "/{realm}",
    urlParamKeys: ["realm"],
  });

  public partialImport = this.makeRequest<
    {
      realm: string;
      rep: PartialImportRealmRepresentation;
    },
    PartialImportResponse
  >({
    method: "POST",
    path: "/{realm}/partialImport",
    urlParamKeys: ["realm"],
    payloadKey: "rep",
  });

  public export = this.makeRequest<
    {
      realm: string;
      exportClients?: boolean;
      exportGroupsAndRoles?: boolean;
    },
    RealmRepresentation
  >({
    method: "POST",
    path: "/{realm}/partial-export",
    urlParamKeys: ["realm"],
    queryParamKeys: ["exportClients", "exportGroupsAndRoles"],
  });

  public getDefaultGroups = this.makeRequest<
    { realm: string },
    GroupRepresentation[]
  >({
    method: "GET",
    path: "/{realm}/default-groups",
    urlParamKeys: ["realm"],
  });

  public addDefaultGroup = this.makeRequest<{ realm: string; id: string }>({
    method: "PUT",
    path: "/{realm}/default-groups/{id}",
    urlParamKeys: ["realm", "id"],
  });

  public removeDefaultGroup = this.makeRequest<{ realm: string; id: string }>({
    method: "DELETE",
    path: "/{realm}/default-groups/{id}",
    urlParamKeys: ["realm", "id"],
  });

  public getGroupByPath = this.makeRequest<
    { path: string; realm: string },
    GroupRepresentation
  >({
    method: "GET",
    path: "/{realm}/group-by-path/{path}",
    urlParamKeys: ["realm", "path"],
  });

  /**
   * Get events Returns all events, or filters them based on URL query parameters listed here
   */
  public findEvents = this.makeRequest<
    {
      realm: string;
      client?: string;
      dateFrom?: string;
      dateTo?: string;
      first?: number;
      ipAddress?: string;
      max?: number;
      type?: EventType | EventType[];
      user?: string;
    },
    EventRepresentation[]
  >({
    method: "GET",
    path: "/{realm}/events",
    urlParamKeys: ["realm"],
    queryParamKeys: [
      "client",
      "dateFrom",
      "dateTo",
      "first",
      "ipAddress",
      "max",
      "type",
      "user",
    ],
  });

  public getConfigEvents = this.makeRequest<
    { realm: string },
    RealmEventsConfigRepresentation
  >({
    method: "GET",
    path: "/{realm}/events/config",
    urlParamKeys: ["realm"],
  });

  public updateConfigEvents = this.makeUpdateRequest<
    { realm: string },
    RealmEventsConfigRepresentation,
    void
  >({
    method: "PUT",
    path: "/{realm}/events/config",
    urlParamKeys: ["realm"],
  });

  public clearEvents = this.makeRequest<{ realm: string }, void>({
    method: "DELETE",
    path: "/{realm}/events",
    urlParamKeys: ["realm"],
  });

  public clearAdminEvents = this.makeRequest<{ realm: string }, void>({
    method: "DELETE",
    path: "/{realm}/admin-events",
    urlParamKeys: ["realm"],
  });

  public getClientRegistrationPolicyProviders = this.makeRequest<
    { realm: string },
    ComponentTypeRepresentation[]
  >({
    method: "GET",
    path: "/{realm}/client-registration-policy/providers",
    urlParamKeys: ["realm"],
  });

  public getClientsInitialAccess = this.makeRequest<
    { realm: string },
    ClientInitialAccessPresentation[]
  >({
    method: "GET",
    path: "/{realm}/clients-initial-access",
    urlParamKeys: ["realm"],
  });

  public createClientsInitialAccess = this.makeUpdateRequest<
    { realm: string },
    { count?: number; expiration?: number },
    ClientInitialAccessPresentation
  >({
    method: "POST",
    path: "/{realm}/clients-initial-access",
    urlParamKeys: ["realm"],
  });

  public delClientsInitialAccess = this.makeRequest<
    { realm: string; id: string },
    void
  >({
    method: "DELETE",
    path: "/{realm}/clients-initial-access/{id}",
    urlParamKeys: ["realm", "id"],
  });

  /**
   * Remove a specific user session.
   */
  public removeSession = this.makeRequest<
    { realm: string; sessionId: string },
    void
  >({
    method: "DELETE",
    path: "/{realm}/sessions/{sessionId}",
    urlParamKeys: ["realm", "sessionId"],
    catchNotFound: true,
  });

  /**
   * Get admin events Returns all admin events, or filters events based on URL query parameters listed here
   */
  public findAdminEvents = this.makeRequest<
    {
      realm: string;
      authClient?: string;
      authIpAddress?: string;
      authRealm?: string;
      authUser?: string;
      dateFrom?: Date;
      dateTo?: Date;
      first?: number;
      max?: number;
      operationTypes?: string;
      resourcePath?: string;
      resourceTypes?: string;
    },
    AdminEventRepresentation[]
  >({
    method: "GET",
    path: "/{realm}/admin-events",
    urlParamKeys: ["realm"],
    queryParamKeys: [
      "authClient",
      "authIpAddress",
      "authRealm",
      "authUser",
      "dateFrom",
      "dateTo",
      "max",
      "first",
      "operationTypes",
      "resourcePath",
      "resourceTypes",
    ],
  });

  /**
   * Users management permissions
   */
  public getUsersManagementPermissions = this.makeRequest<
    { realm: string },
    ManagementPermissionReference
  >({
    method: "GET",
    path: "/{realm}/users-management-permissions",
    urlParamKeys: ["realm"],
  });

  public updateUsersManagementPermissions = this.makeRequest<
    { realm: string; enabled: boolean },
    ManagementPermissionReference
  >({
    method: "PUT",
    path: "/{realm}/users-management-permissions",
    urlParamKeys: ["realm"],
  });

  /**
   * Sessions
   */
  public getClientSessionStats = this.makeRequest<
    { realm: string },
    ClientSessionStat[]
  >({
    method: "GET",
    path: "/{realm}/client-session-stats",
    urlParamKeys: ["realm"],
  });

  public logoutAll = this.makeRequest<{ realm: string }, void>({
    method: "POST",
    path: "/{realm}/logout-all",
    urlParamKeys: ["realm"],
  });

  public deleteSession = this.makeRequest<
    { realm: string; session: string; isOffline: boolean },
    void
  >({
    method: "DELETE",
    path: "/{realm}/sessions/{session}",
    urlParamKeys: ["realm", "session"],
    queryParamKeys: ["isOffline"],
  });

  public pushRevocation = this.makeRequest<
    { realm: string },
    GlobalRequestResult
  >({
    method: "POST",
    path: "/{realm}/push-revocation",
    urlParamKeys: ["realm"],
    ignoredKeys: ["realm"],
  });

  public getKeys = this.makeRequest<
    { realm: string },
    KeysMetadataRepresentation
  >({
    method: "GET",
    path: "/{realm}/keys",
    urlParamKeys: ["realm"],
  });

  public testLDAPConnection = this.makeUpdateRequest<
    { realm: string },
    TestLdapConnectionRepresentation
  >({
    method: "POST",
    path: "/{realm}/testLDAPConnection",
    urlParamKeys: ["realm"],
  });

  public testSMTPConnection = this.makeUpdateRequest<
    { realm: string },
    Record<string, string | number>
  >({
    method: "POST",
    path: "/{realm}/testSMTPConnection",
    urlParamKeys: ["realm"],
  });

  public ldapServerCapabilities = this.makeUpdateRequest<
    { realm: string },
    TestLdapConnectionRepresentation
  >({
    method: "POST",
    path: "/{realm}/ldap-server-capabilities",
    urlParamKeys: ["realm"],
  });

  public getRealmSpecificLocales = this.makeRequest<
    { realm: string },
    string[]
  >({
    method: "GET",
    path: "/{realm}/localization",
    urlParamKeys: ["realm"],
  });

  public getRealmLocalizationTexts = this.makeRequest<
    { realm: string; selectedLocale: string; first?: number; max?: number },
    Record<string, string>
  >({
    method: "GET",
    path: "/{realm}/localization/{selectedLocale}",
    urlParamKeys: ["realm", "selectedLocale"],
  });

  public addLocalization = this.makeUpdateRequest<
    { realm: string; selectedLocale: string; key: string },
    string,
    void
  >({
    method: "PUT",
    path: "/{realm}/localization/{selectedLocale}/{key}",
    urlParamKeys: ["realm", "selectedLocale", "key"],
    headers: { "content-type": "text/plain" },
  });

  public deleteRealmLocalizationTexts = this.makeRequest<
    { realm: string; selectedLocale: string; key?: string },
    void
  >({
    method: "DELETE",
    path: "/{realm}/localization/{selectedLocale}/{key}",
    urlParamKeys: ["realm", "selectedLocale", "key"],
  });

  constructor(client: KeycloakAdminClient) {
    super(client, {
      path: "/admin/realms",
      getBaseUrl: () => client.baseUrl,
    });
  }
}
