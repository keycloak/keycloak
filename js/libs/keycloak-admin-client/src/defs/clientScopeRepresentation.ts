/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_clientscoperepresentation
 */
import type ProtocolMapperRepresentation from "./protocolMapperRepresentation.js";

export default interface ClientScopeRepresentation {
  attributes?: Record<string, any>;
  description?: string;
  id?: string;
  name?: string;
  protocol?: string;
  protocolMappers?: ProtocolMapperRepresentation[];
}
