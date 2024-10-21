/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_authenticatorconfiginforepresentation
 */
export default interface AuthenticatorConfigInfoRepresentation {
  name?: string;
  providerId?: string;
  helpText?: string;
  properties?: ConfigPropertyRepresentation[];
}

export interface ConfigPropertyRepresentation {
  name?: string;
  label?: string;
  helpText?: string;
  type?: string;
  defaultValue?: any;
  options?: string[];
  secret?: boolean;
  required?: boolean;
  placeholder?: string;
}
