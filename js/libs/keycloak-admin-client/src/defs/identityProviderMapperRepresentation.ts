/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_identityprovidermapperrepresentation
 */

export default interface IdentityProviderMapperRepresentation {
  config?: any;
  id?: string;
  identityProviderAlias?: string;
  identityProviderMapper?: string;
  name?: string;
}
