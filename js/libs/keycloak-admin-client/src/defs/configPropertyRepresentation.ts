/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_configpropertyrepresentation
 */
export interface ConfigPropertyRepresentation {
  name?: string;
  label?: string;
  helpText?: string;
  type?: string;
  defaultValue?: object;
  options?: string[];
  secret?: boolean;
  required?: boolean;
}
