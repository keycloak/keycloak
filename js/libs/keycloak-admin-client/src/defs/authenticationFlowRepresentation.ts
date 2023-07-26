import type AuthenticationExecutionExportRepresentation from "./authenticationExecutionExportRepresentation.js";

/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_authenticationflowrepresentation
 */
export default interface AuthenticationFlowRepresentation {
  id?: string;
  alias?: string;
  description?: string;
  providerId?: string;
  topLevel?: boolean;
  builtIn?: boolean;
  authenticationExecutions?: AuthenticationExecutionExportRepresentation[];
}
