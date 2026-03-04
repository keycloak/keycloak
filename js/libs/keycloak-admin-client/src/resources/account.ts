import type {
  AccountClientRepresentation,
  AccountConsentRepresentation,
  AccountCredentialContainerRepresentation,
  AccountDeviceRepresentation,
  AccountLinkedAccountRepresentation,
  AccountLinkUriRepresentation,
  AccountOrganizationRepresentation,
  AccountSessionRepresentation,
} from "../defs/accountRepresentation.js";
import type GroupRepresentation from "../defs/groupRepresentation.js";
import type UserRepresentation from "../defs/userRepresentation.js";
import type KeycloakAdminClient from "../index.js";
import Resource from "./resource.js";

interface LinkedAccountQuery {
  linked?: boolean;
  search?: string;
  first?: number;
  max?: number;
}

export class Account extends Resource<{ realm?: string }> {
  constructor(client: KeycloakAdminClient) {
    super(client, {
      path: "/realms/{realm}/account",
      getUrlParams: () => ({
        realm: client.realmName,
      }),
      getBaseUrl: () => client.baseUrl,
    });
  }

  public getProfile = this.makeRequest<
    { userProfileMetadata?: boolean },
    UserRepresentation
  >({
    method: "GET",
    path: "/",
    queryParamKeys: ["userProfileMetadata"],
  });

  public updateProfile = this.makeRequest<UserRepresentation, void>({
    method: "POST",
    path: "/",
  });

  public listSessions = this.makeRequest<{}, AccountSessionRepresentation[]>({
    method: "GET",
    path: "/sessions",
  });

  public listSessionDevices = this.makeRequest<
    {},
    AccountDeviceRepresentation[]
  >({
    method: "GET",
    path: "/sessions/devices",
  });

  public logoutSessions = this.makeRequest<{ current?: boolean }, void>({
    method: "DELETE",
    path: "/sessions",
    queryParamKeys: ["current"],
  });

  public logoutSession = this.makeRequest<{ id: string }, void>({
    method: "DELETE",
    path: "/sessions/{id}",
    urlParamKeys: ["id"],
  });

  public listCredentialTypes = this.makeRequest<
    { type?: string; userCredentials?: boolean },
    AccountCredentialContainerRepresentation[]
  >({
    method: "GET",
    path: "/credentials",
    queryParamKeys: ["type", "userCredentials"],
    keyTransform: {
      userCredentials: "user-credentials",
    },
  });

  public removeCredential = this.makeRequest<{ credentialId: string }, void>({
    method: "DELETE",
    path: "/credentials/{credentialId}",
    urlParamKeys: ["credentialId"],
  });

  public setCredentialLabel = this.makeUpdateRequest<
    { credentialId: string },
    string,
    void
  >({
    method: "PUT",
    path: "/credentials/{credentialId}/label",
    urlParamKeys: ["credentialId"],
  });

  public supportedLocales = this.makeRequest<{}, string[]>({
    method: "GET",
    path: "/supportedLocales",
  });

  public listOrganizations = this.makeRequest<
    {},
    AccountOrganizationRepresentation[]
  >({
    method: "GET",
    path: "/organizations/",
  });

  public listLinkedAccounts = this.makeRequest<
    LinkedAccountQuery,
    AccountLinkedAccountRepresentation[]
  >({
    method: "GET",
    path: "/linked-accounts/",
    queryParamKeys: ["linked", "search", "first", "max"],
  });

  public buildLinkedAccountUri = this.makeRequest<
    { providerAlias: string; redirectUri: string },
    AccountLinkUriRepresentation
  >({
    method: "GET",
    path: "/linked-accounts/{providerAlias}",
    urlParamKeys: ["providerAlias"],
    queryParamKeys: ["redirectUri"],
  });

  public removeLinkedAccount = this.makeRequest<
    { providerAlias: string },
    void
  >({
    method: "DELETE",
    path: "/linked-accounts/{providerAlias}",
    urlParamKeys: ["providerAlias"],
  });

  public listGroups = this.makeRequest<{}, GroupRepresentation[]>({
    method: "GET",
    path: "/groups",
  });

  public listApplications = this.makeRequest<
    { name?: string },
    AccountClientRepresentation[]
  >({
    method: "GET",
    path: "/applications",
    queryParamKeys: ["name"],
  });

  public getConsent = this.makeRequest<
    { clientId: string },
    AccountConsentRepresentation
  >({
    method: "GET",
    path: "/applications/{clientId}/consent",
    urlParamKeys: ["clientId"],
  });

  public revokeConsent = this.makeRequest<{ clientId: string }, void>({
    method: "DELETE",
    path: "/applications/{clientId}/consent",
    urlParamKeys: ["clientId"],
  });

  public grantConsent = this.makeUpdateRequest<
    { clientId: string },
    AccountConsentRepresentation,
    AccountConsentRepresentation
  >({
    method: "POST",
    path: "/applications/{clientId}/consent",
    urlParamKeys: ["clientId"],
  });

  public updateConsent = this.makeUpdateRequest<
    { clientId: string },
    AccountConsentRepresentation,
    AccountConsentRepresentation
  >({
    method: "PUT",
    path: "/applications/{clientId}/consent",
    urlParamKeys: ["clientId"],
  });
}
