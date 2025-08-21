// Generated using typescript-generator version 2.37.1128 on 2022-09-16 15:57:05.

export interface AccountLinkUriRepresentation {
  accountLinkUri: string;
  nonce: string;
  hash: string;
}

export interface ClientRepresentation {
  clientId: string;
  clientName: string;
  description: string;
  userConsentRequired: boolean;
  inUse: boolean;
  offlineAccess: boolean;
  rootUrl: string;
  baseUrl: string;
  effectiveUrl: string;
  consent?: ConsentRepresentation;
  logoUri: string;
  policyUri: string;
  tosUri: string;
}

export interface ConsentRepresentation {
  grantedScopes: ConsentScopeRepresentation[];
  createdDate: number;
  lastUpdatedDate: number;
}

export interface ConsentScopeRepresentation {
  id: string;
  name: string;
  displayText: string;
}

export interface CredentialMetadataRepresentationMessage {
  key: string;
  parameters?: string[];
}

export interface CredentialMetadataRepresentation {
  infoMessage: CredentialMetadataRepresentationMessage;
  infoProperties: CredentialMetadataRepresentationMessage[];
  warningMessageTitle: CredentialMetadataRepresentationMessage;
  warningMessageDescription: CredentialMetadataRepresentationMessage;
  credential: CredentialRepresentation;
}

export interface DeviceRepresentation {
  id: string;
  ipAddress: string;
  os: string;
  osVersion: string;
  browser: string;
  device: string;
  lastAccess: number;
  current: boolean;
  sessions: SessionRepresentation[];
  mobile: boolean;
}

export interface LinkedAccountRepresentation {
  connected: boolean;
  providerAlias: string;
  providerName: string;
  displayName: string;
  linkedUsername: string;
  social: boolean;
}

export interface SessionRepresentation {
  id: string;
  ipAddress: string;
  started: number;
  lastAccess: number;
  expires: number;
  clients: ClientRepresentation[];
  browser: string;
  current: boolean;
}

export interface UserProfileAttributeMetadata {
  name: string;
  displayName: string;
  required: boolean;
  readOnly: boolean;
  annotations?: { [index: string]: any };
  validators: { [index: string]: { [index: string]: any } };
  multivalued: boolean;
  defaultValue: string;
}

export interface UserProfileMetadata {
  attributes: UserProfileAttributeMetadata[];
}

export type UserRepresentation = any & {
  userProfileMetadata: UserProfileMetadata;
};

export interface CredentialRepresentation {
  id: string;
  type: string;
  userLabel: string;
  createdDate: number;
  secretData: string;
  credentialData: string;
  priority: number;
  value: string;
  temporary: boolean;
  /**
   * @deprecated
   */
  device: string;
  /**
   * @deprecated
   */
  hashedSaltedValue: string;
  /**
   * @deprecated
   */
  salt: string;
  /**
   * @deprecated
   */
  hashIterations: number;
  /**
   * @deprecated
   */
  counter: number;
  /**
   * @deprecated
   */
  algorithm: string;
  /**
   * @deprecated
   */
  digits: number;
  /**
   * @deprecated
   */
  period: number;
  /**
   * @deprecated
   */
  config: { [index: string]: string[] };
}

export interface CredentialTypeMetadata {
  type: string;
  displayName: string;
  helpText: string;
  iconCssClass: string;
  createAction: string;
  updateAction: string;
  removeable: boolean;
  category: "basic-authentication" | "two-factor" | "passwordless";
}

export interface CredentialContainer {
  type: string;
  category: string;
  displayName: string;
  helptext: string;
  iconCssClass: string;
  createAction: string;
  updateAction: string;
  removeable: boolean;
  userCredentialMetadatas: CredentialMetadataRepresentation[];
  metadata: CredentialTypeMetadata;
}

export interface Client {
  baseUrl: string;
  clientId: string;
  name?: string;
}

export interface Scope {
  name: string;
  displayName?: string;
}

export interface Resource {
  _id: string;
  name: string;
  client: Client;
  scopes: Scope[];
  uris: string[];
  shareRequests?: Permission[];
}

export interface Permission {
  email?: string;
  firstName?: string;
  lastName?: string;
  scopes: Scope[] | string[]; // this should be Scope[] - fix API
  username: string;
}

export interface Permissions {
  permissions: Permission[];
  row?: number;
}

export interface Group {
  id?: string;
  name: string;
  path: string;
}

export interface SupportedCredentialConfiguration {
  id: string;
  format: string;
  scope: string;
}
export interface CredentialsIssuer {
  credential_issuer: string;
  credential_endpoint: string;
  authorization_servers: string[];
  credential_configurations_supported: Record<
    string,
    SupportedCredentialConfiguration
  >;
}
