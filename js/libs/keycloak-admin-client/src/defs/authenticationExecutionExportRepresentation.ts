/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_authenticationexecutionexportrepresentation
 */
export default interface AuthenticationExecutionExportRepresentation {
  flowAlias?: string;
  userSetupAllowed?: boolean;
  authenticatorConfig?: string;
  authenticator?: string;
  requirement?: string;
  priority?: number;
  autheticatorFlow?: boolean;
}
