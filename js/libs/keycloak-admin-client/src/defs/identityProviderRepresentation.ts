/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_identityproviderrepresentation
 */

export enum IdentityProviderType {
  ANY = "ANY",
  USER_AUTHENTICATION = "USER_AUTHENTICATION",
  CLIENT_ASSERTION = "CLIENT_ASSERTION",
  EXCHANGE_EXTERNAL_TOKEN = "EXCHANGE_EXTERNAL_TOKEN",
  JWT_AUTHORIZATION_GRANT = "JWT_AUTHORIZATION_GRANT",
}

export default interface IdentityProviderRepresentation {
  addReadTokenRoleOnCreate?: boolean;
  alias?: string;
  config?: Record<string, any>;
  displayName?: string;
  enabled?: boolean;
  firstBrokerLoginFlowAlias?: string;
  internalId?: string;
  linkOnly?: boolean;
  hideOnLogin?: boolean;
  postBrokerLoginFlowAlias?: string;
  providerId?: string;
  storeToken?: boolean;
  trustEmail?: boolean;
  organizationId?: string;
  types?: string[];
}
