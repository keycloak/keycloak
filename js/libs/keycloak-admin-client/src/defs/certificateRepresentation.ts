/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/#_certificaterepresentation
 */
export default interface CertificateRepresentation {
  privateKey?: string;
  publicKey?: string;
  certificate?: string;
  kid?: string;
}
