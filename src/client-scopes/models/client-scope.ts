/**
 * https://www.keycloak.org/docs-api/4.1/rest-api/#_protocolmapperrepresentation
 */

export interface ProtocolMapperRepresentation {
  config?: Record<string, any>;
  id?: string;
  name?: string;
  protocol?: string;
  protocolMapper?: string;
}

/**
 * https://www.keycloak.org/docs-api/6.0/rest-api/index.html#_clientscoperepresentation
 */

export interface ClientScopeRepresentation {
  attributes?: Record<string, any>;
  description?: string;
  id?: string;
  name?: string;
  protocol?: string;
  protocolMappers?: ProtocolMapperRepresentation[];
}
