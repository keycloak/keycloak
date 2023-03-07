/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/#_userconsentrepresentation
 */

export default interface UserConsentRepresentation {
  clientId?: string;
  createDate?: string;
  grantedClientScopes?: string[];
  lastUpdatedDate?: number;
}
