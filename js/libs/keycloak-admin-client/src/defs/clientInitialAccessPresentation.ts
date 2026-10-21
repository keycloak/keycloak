/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_clientinitialaccesspresentation
 */
export default interface ClientInitialAccessPresentation {
  id?: string;
  token?: string;
  timestamp?: number;
  expiration?: number;
  count?: number;
  remainingCount?: number;
}
