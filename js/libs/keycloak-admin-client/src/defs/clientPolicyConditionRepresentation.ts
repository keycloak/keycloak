/**
 * https://www.keycloak.org/docs-api/15.0/rest-api/#_clientpolicyconditionrepresentation
 */
export default interface ClientPolicyConditionRepresentation {
  condition?: string;
  configuration?: object;
}
