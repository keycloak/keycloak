/**
 * Interface definition for cross-domain trust configuration.
 */
export default interface CrossDomainTrustConfig {
  issuer: string;
  audience: string;
  certificate: string;
}
