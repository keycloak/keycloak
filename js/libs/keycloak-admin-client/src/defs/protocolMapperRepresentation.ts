/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_protocolmapperrepresentation
 */

export default interface ProtocolMapperRepresentation {
  config?: Record<string, any>;
  id?: string;
  name?: string;
  protocol?: string;
  protocolMapper?: string;
}
