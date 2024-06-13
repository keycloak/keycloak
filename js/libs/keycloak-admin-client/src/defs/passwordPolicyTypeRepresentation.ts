/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_passwordpolicytyperepresentation
 */
export default interface PasswordPolicyTypeRepresentation {
  id?: string;
  displayName?: string;
  configType?: string;
  defaultValue?: string;
  multipleSupported?: boolean;
}
