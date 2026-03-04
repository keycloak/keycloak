import { KeycloakAdminClient } from "./client.js";
import { RequiredActionAlias } from "./defs/requiredActionProviderRepresentation.js";

export const requiredAction = RequiredActionAlias;
export default KeycloakAdminClient;
export { NetworkError, fetchWithError } from "./utils/fetchWithError.js";
export type { NetworkErrorOptions } from "./utils/fetchWithError.js";

export type { default as OrganizationInvitationRepresentation } from "./defs/organizationInvitationRepresentation.js";
export { OrganizationInvitationStatus } from "./defs/organizationInvitationRepresentation.js";

export { Groups } from "./resources/groups.js";
export { Account } from "./resources/account.js";
export type {
  AccountClientRepresentation,
  AccountConsentRepresentation,
  AccountConsentScopeRepresentation,
  AccountCredentialContainerRepresentation,
  AccountCredentialMetadataRepresentation,
  AccountDeviceRepresentation,
  AccountLinkUriRepresentation,
  AccountLinkedAccountRepresentation,
  AccountLocalizedMessageRepresentation,
  AccountOrganizationRepresentation,
  AccountSessionRepresentation,
} from "./defs/accountRepresentation.js";
// V2 API types (Kiota-generated)
export type {
  OIDCClientRepresentation,
  SAMLClientRepresentation,
  ClientRepresentationV2,
} from "./resources/clientsV2.js";
