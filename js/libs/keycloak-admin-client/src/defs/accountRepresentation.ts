import type CredentialRepresentation from "./credentialRepresentation.js";

export interface AccountConsentScopeRepresentation {
  id?: string;
  name?: string;
  displayText?: string;
  /**
   * @deprecated Use `displayText` instead.
   */
  displayTest?: string;
}

export interface AccountConsentRepresentation {
  grantedScopes?: AccountConsentScopeRepresentation[];
  createdDate?: number;
  lastUpdatedDate?: number;
}

export interface AccountClientRepresentation {
  clientId?: string;
  clientName?: string;
  description?: string;
  userConsentRequired?: boolean;
  inUse?: boolean;
  offlineAccess?: boolean;
  rootUrl?: string;
  baseUrl?: string;
  effectiveUrl?: string;
  consent?: AccountConsentRepresentation;
  logoUri?: string;
  policyUri?: string;
  tosUri?: string;
}

export interface AccountSessionRepresentation {
  id?: string;
  ipAddress?: string;
  started?: number;
  lastAccess?: number;
  expires?: number;
  clients?: AccountClientRepresentation[];
  browser?: string;
  current?: boolean;
}

export interface AccountDeviceRepresentation {
  id?: string;
  ipAddress?: string;
  os?: string;
  osVersion?: string;
  browser?: string;
  device?: string;
  lastAccess?: number;
  current?: boolean;
  sessions?: AccountSessionRepresentation[];
  mobile?: boolean;
}

export interface AccountLinkedAccountRepresentation {
  connected?: boolean;
  social?: boolean;
  providerAlias?: string;
  providerName?: string;
  displayName?: string;
  linkedUsername?: string;
}

export interface AccountLinkUriRepresentation {
  accountLinkUri?: string;
  nonce?: string;
  hash?: string;
}

export interface AccountOrganizationRepresentation {
  id?: string;
  name?: string;
  alias?: string;
  enabled?: boolean;
  description?: string;
  domains?: string[];
}

export interface AccountLocalizedMessageRepresentation {
  key?: string;
  parameters?: string[];
}

export interface AccountCredentialMetadataRepresentation {
  infoMessage?: AccountLocalizedMessageRepresentation;
  infoProperties?: AccountLocalizedMessageRepresentation[];
  warningMessageTitle?: AccountLocalizedMessageRepresentation;
  warningMessageDescription?: AccountLocalizedMessageRepresentation;
  credential?: CredentialRepresentation;
}

export interface AccountCredentialContainerRepresentation {
  type?: string;
  category?: string;
  displayName?: string;
  helptext?: string;
  iconCssClass?: string;
  createAction?: string;
  updateAction?: string;
  removeable?: boolean;
  userCredentialMetadatas?: AccountCredentialMetadataRepresentation[];
}
