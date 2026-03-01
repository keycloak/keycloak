import type { ConfigPropertyRepresentation } from "./configPropertyRepresentation.js";

/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_componenttyperepresentation
 */
export default interface ComponentTypeRepresentation {
  id: string;
  helpText: string;
  properties: ConfigPropertyRepresentation[];
  clientProperties: ConfigPropertyRepresentation[];
  metadata: { [index: string]: any };
}
