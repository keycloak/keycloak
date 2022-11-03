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
  consent: ConsentRepresentation;
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
  displayTest: string;
}

export interface CredentialMetadataRepresentation {
  infoMessage: string;
  warningMessageTitle: string;
  warningMessageDescription: string;
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
  annotations: { [index: string]: any };
  validators: { [index: string]: { [index: string]: any } };
}

export interface UserProfileMetadata {
  attributes: UserProfileAttributeMetadata[];
}

export interface UserRepresentation {
  id: string;
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  emailVerified: boolean;
  userProfileMetadata: UserProfileMetadata;
  attributes: { [index: string]: string[] };
}

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
