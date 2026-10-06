/**
 * https://www.keycloak.org/docs-api/latest/rest-api/index.html#ClientMappingsRepresentation
 */
import type RoleRepresentation from "./roleRepresentation.js";

export default interface ClientMappingsRepresentation {
  id?: string;
  client?: string;
  mappings?: RoleRepresentation[];
}
