/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/#_certificaterepresentation
 */
export default interface CertificateRepresentation {
  publicKey?: string;
  certificate?: string;
  kid?: string;
  jwks?: string;
}
