/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/#_userconsentrepresentation
 */

export default interface UserConsentRepresentation {
  clientId?: string;
  createdDate?: number;
  grantedClientScopes?: string[];
  lastUpdatedDate?: number;
}
