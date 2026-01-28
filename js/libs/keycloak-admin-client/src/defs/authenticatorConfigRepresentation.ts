/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_authenticatorconfigrepresentation
 */
export default interface AuthenticatorConfigRepresentation {
  id?: string;
  alias?: string;
  config?: { [index: string]: string };
}

// we defined this type ourself as the original is just `{[index: string]: any}[]`
// but the admin console does assume these properties are there.
export interface AuthenticationProviderRepresentation {
  id?: string;
  displayName?: string;
  description?: string;
  supportsSecret?: boolean;
}
