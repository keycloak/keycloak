/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_authenticationexecutioninforepresentation
 */
export default interface AuthenticationExecutionInfoRepresentation {
  id?: string;
  requirement?: string;
  displayName?: string;
  alias?: string;
  description?: string;
  requirementChoices?: string[];
  configurable?: boolean;
  authenticationFlow?: boolean;
  providerId?: string;
  authenticationConfig?: string;
  flowId?: string;
  level?: number;
  index?: number;
}
