/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_componentrepresentation
 */

export default interface ComponentRepresentation {
  id?: string;
  name?: string;
  providerId?: string;
  providerType?: string;
  parentId?: string;
  subType?: string;
  config?: { [index: string]: string | string[] };
}
