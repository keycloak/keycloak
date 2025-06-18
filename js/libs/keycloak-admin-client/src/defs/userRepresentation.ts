import type CredentialRepresentation from "./credentialRepresentation.js";
import type FederatedIdentityRepresentation from "./federatedIdentityRepresentation.js";
import type { RequiredActionAlias } from "./requiredActionProviderRepresentation.js";
import type UserConsentRepresentation from "./userConsentRepresentation.js";
import type { UserProfileMetadata } from "./userProfileMetadata.js";

export default interface UserRepresentation {
  id?: string;
  createdTimestamp?: number;
  username?: string;
  enabled?: boolean;
  totp?: boolean;
  emailVerified?: boolean;
  disableableCredentialTypes?: string[];
  requiredActions?: (RequiredActionAlias | string)[];
  notBefore?: number;
  access?: Record<string, boolean>;

  // optional from response
  attributes?: Record<string, any>;
  clientConsents?: UserConsentRepresentation[];
  clientRoles?: Record<string, any>;
  credentials?: CredentialRepresentation[];
  email?: string;
  federatedIdentities?: FederatedIdentityRepresentation[];
  federationLink?: string;
  firstName?: string;
  groups?: string[];
  lastName?: string;
  realmRoles?: string[];
  self?: string;
  serviceAccountClientId?: string;
  userProfileMetadata?: UserProfileMetadata;
}
