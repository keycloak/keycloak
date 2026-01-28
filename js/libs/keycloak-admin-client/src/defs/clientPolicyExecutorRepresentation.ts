/**
 * https://www.keycloak.org/docs-api/15.0/rest-api/#_clientpolicyexecutorrepresentation
 */
export default interface ClientPolicyExecutorRepresentation {
  configuration?: object;
  executor?: string;
}
