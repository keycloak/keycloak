import { KeycloakAdminClient } from "./client.js";
import { RequiredActionAlias } from "./defs/requiredActionProviderRepresentation.js";

export const requiredAction = RequiredActionAlias;
export default KeycloakAdminClient;
export { NetworkError, fetchWithError } from "./utils/fetchWithError.js";
export type { NetworkErrorOptions } from "./utils/fetchWithError.js";

export type { default as OrganizationInvitationRepresentation } from "./defs/organizationInvitationRepresentation.js";
export { OrganizationInvitationStatus } from "./defs/organizationInvitationRepresentation.js";

export { Groups } from "./resources/groups.js";
// V2 API types and classes
export {
  ClientsV2Api,
  createClientsV2Api,
} from "./resources/clientsV2.js";
export type {
  OIDCClientRepresentation,
  SAMLClientRepresentation,
  ClientRepresentationV2,
  AdminApiRealmNameClientsVersionGetRequest,
  AdminApiRealmNameClientsVersionIdDeleteRequest,
  AdminApiRealmNameClientsVersionIdGetRequest,
  AdminApiRealmNameClientsVersionIdPatchRequest,
  AdminApiRealmNameClientsVersionIdPutRequest,
  AdminApiRealmNameClientsVersionPostRequest,
} from "./resources/clientsV2.js";
