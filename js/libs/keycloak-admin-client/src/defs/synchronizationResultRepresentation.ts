/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_synchronizationresult
 */

export default interface SynchronizationResultRepresentation {
  ignored?: boolean;
  added?: number;
  updated?: number;
  removed?: number;
  failed?: number;
  status?: string;
}
